-- 043_marketplace_open_seller.sql
--   1. Habilitar modo vendedor en Marketplace para cualquier usuario (no viene por defecto).
--   2. Manejo de inventario (stock) en publicaciones y movimientos.
--   3. api.activate_store permite a usuarios activar su tienda/perfil vendedor sin paywall.
--   4. api.my_store_info para consultar el estado del vendedor.

-- 1. api.activate_store
CREATE OR REPLACE FUNCTION api.activate_store(p_slug text, p_name text, p_description text DEFAULT '') RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v uuid := internal.actor();
  v_id uuid;
  v_u app.users;
  v_clean_slug text := lower(regexp_replace(trim(p_slug), '[^a-zA-Z0-9\-]+', '-', 'g'));
BEGIN
  SELECT * INTO v_u FROM app.users WHERE id = v;
  IF NOT FOUND OR v_u.status <> 'active' THEN
    RAISE EXCEPTION 'user_not_active';
  END IF;

  IF v_clean_slug IS NULL OR length(v_clean_slug) < 3 THEN
    v_clean_slug := 'vendedor-' || substring(replace(v::text, '-', ''), 1, 8);
  END IF;

  -- Si el usuario aún no tiene perfil profesional/vendedor, se le crea automáticamente
  IF NOT EXISTS(SELECT 1 FROM app.professionals WHERE user_id = v) THEN
    INSERT INTO app.professionals(user_id, slug, business_name, bio, status)
    VALUES (v, v_clean_slug, coalesce(nullif(trim(p_name), ''), v_u.display_name), p_description, 'active')
    ON CONFLICT (user_id) DO UPDATE SET status = 'active';
  END IF;

  INSERT INTO app.user_roles(user_id, role_code)
  VALUES (v, 'professional')
  ON CONFLICT DO NOTHING;

  -- Asegurar el derecho a vender en marketplace (sin bloquear por planes previos)
  INSERT INTO app.entitlements(professional_id, kind, starts_at, ends_at)
  VALUES (v, 'marketplace', now(), now() + interval '50 years')
  ON CONFLICT (professional_id, kind)
  DO UPDATE SET ends_at = now() + interval '50 years', revoked_at = NULL;

  IF EXISTS(SELECT 1 FROM app.stores WHERE professional_id = v AND status = 'suspended') THEN
    RAISE EXCEPTION 'store_suspended';
  END IF;

  INSERT INTO app.stores(professional_id, slug, name, description, status)
  VALUES(v, v_clean_slug, coalesce(nullif(trim(p_name), ''), v_u.display_name), p_description, 'active')
  ON CONFLICT(professional_id) DO UPDATE
    SET status = 'active',
        name = coalesce(nullif(trim(EXCLUDED.name), ''), app.stores.name),
        description = EXCLUDED.description,
        slug = coalesce(nullif(EXCLUDED.slug, ''), app.stores.slug)
  RETURNING id INTO v_id;

  PERFORM internal.audit('store.activated', 'store', v_id);
  RETURN v_id;
END $$;

REVOKE ALL ON FUNCTION api.activate_store(text, text, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.activate_store(text, text, text) TO chamba_api;

-- 2. api.add_product con stock / inventario
CREATE OR REPLACE FUNCTION api.add_product(
  p_store uuid,
  p_sku text,
  p_name text,
  p_description text,
  p_price bigint,
  p_currency text,
  p_stock integer
) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v uuid := internal.actor();
  v_id uuid;
  v_store app.stores;
  v_sku text := coalesce(nullif(trim(p_sku), ''), 'SKU-' || upper(substring(replace(gen_random_uuid()::text, '-', ''), 1, 8)));
BEGIN
  SELECT * INTO v_store FROM app.stores WHERE id = p_store AND professional_id = v AND status = 'active';
  IF NOT FOUND THEN
    RAISE EXCEPTION 'store_access_denied' USING ERRCODE = '42501';
  END IF;

  -- Asegurar entitlement
  INSERT INTO app.entitlements(professional_id, kind, starts_at, ends_at)
  VALUES (v, 'marketplace', now(), now() + interval '50 years')
  ON CONFLICT (professional_id, kind)
  DO UPDATE SET ends_at = now() + interval '50 years', revoked_at = NULL;

  INSERT INTO app.products(store_id, sku, name, description, price_minor, currency, stock, status)
  VALUES(p_store, v_sku, trim(p_name), trim(p_description), p_price, upper(trim(p_currency)), greatest(coalesce(p_stock, 0), 0), 'active')
  RETURNING id INTO v_id;

  IF p_stock > 0 THEN
    INSERT INTO app.inventory_movements(product_id, delta, reason, actor_id)
    VALUES(v_id, p_stock, 'initial_stock', v);
  END IF;

  PERFORM internal.audit('product.created', 'product', v_id);
  RETURN v_id;
END $$;

REVOKE ALL ON FUNCTION api.add_product(uuid, text, text, text, bigint, text, integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.add_product(uuid, text, text, text, bigint, text, integer) TO chamba_api;

-- 3. api.marketplace feed ordenado por más recientes y con stock
CREATE OR REPLACE FUNCTION api.marketplace(p_limit integer DEFAULT 50, p_offset integer DEFAULT 0)
RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  IF p_limit NOT BETWEEN 1 AND 100 OR p_offset < 0 THEN
    RAISE EXCEPTION 'invalid_pagination';
  END IF;

  RETURN QUERY
  SELECT to_jsonb(p) || jsonb_build_object(
    'store_name', s.name,
    'professional_id', s.professional_id,
    'images', coalesce((
      SELECT jsonb_agg(i.file_id ORDER BY i.sort_order, i.file_id)
      FROM app.product_images i
      JOIN app.files f ON f.id = i.file_id
      WHERE i.product_id = p.id AND f.moderation_status = 'approved' AND f.deleted_at IS NULL
    ), '[]'::jsonb)
  )
  FROM app.products p
  JOIN app.stores s ON s.id = p.store_id
  JOIN app.professionals pro ON pro.user_id = s.professional_id
  JOIN app.users u ON u.id = pro.user_id
  WHERE p.status = 'active'
    AND s.status = 'active'
    AND pro.status = 'active'
    AND u.status = 'active'
  ORDER BY p.created_at DESC, p.id
  LIMIT p_limit OFFSET p_offset;
END $$;

REVOKE ALL ON FUNCTION api.marketplace(integer, integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.marketplace(integer, integer) TO chamba_api;
