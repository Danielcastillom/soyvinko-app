-- Migración 031: Modelo de negocio, gestión de Marketplace y supervisión de Servicios Contratados

-- 1. Permitir planes gratuitos ($0.00) en app.plans
ALTER TABLE app.plans DROP CONSTRAINT IF EXISTS plans_price_minor_check;
ALTER TABLE app.plans ADD CONSTRAINT plans_price_minor_check CHECK (price_minor >= 0);

-- 2. Metadatos del modelo de negocio en app.plans
ALTER TABLE app.plans ADD COLUMN IF NOT EXISTS affiliate_type text NOT NULL DEFAULT 'provider';
ALTER TABLE app.plans ADD COLUMN IF NOT EXISTS fee_summary text NOT NULL DEFAULT '';
ALTER TABLE app.plans ADD COLUMN IF NOT EXISTS benefits_summary text NOT NULL DEFAULT '';
ALTER TABLE app.plans ADD COLUMN IF NOT EXISTS max_products integer NOT NULL DEFAULT 0;
ALTER TABLE app.plans ADD COLUMN IF NOT EXISTS presential_fee_percent numeric NOT NULL DEFAULT 0;
ALTER TABLE app.plans ADD COLUMN IF NOT EXISTS remote_fee_percent numeric NOT NULL DEFAULT 0;

-- 3. Asignación de planes en app.stores y app.professionals
ALTER TABLE app.stores ADD COLUMN IF NOT EXISTS plan_code text NOT NULL DEFAULT 'STORE-PROMO';
ALTER TABLE app.professionals ADD COLUMN IF NOT EXISTS plan_code text NOT NULL DEFAULT 'PRO-PROMO';

-- 4. Inserción / actualización de los planes del modelo de negocio oficial
INSERT INTO app.plans (
  code, name, affiliate_type, price_minor, currency, credits,
  positioning_weight, positioning_days, marketplace_days, active,
  fee_summary, benefits_summary, max_products, presential_fee_percent, remote_fee_percent
)
VALUES
  -- Comercios (Tiendas / Repuestos)
  ('STORE-PROMO', 'Plan Promocional Entrada (Meses 1 al 3)', 'store', 0, 'USD', 0,
   10, 90, 90, true,
   '0% (Primer mes) / 3% (Meses 2 y 3)',
   '• Registro gratis de la tienda.\n• Indexación en el motor de búsqueda instantánea.\n• Carga de inventario (hasta 15 productos).',
   15, 0, 0),

  ('STORE-STANDARD-30', 'Membresía Comercio Estándar (Desde Mes 4)', 'store', 3000, 'USD', 0,
   25, 30, 30, true,
   'Tasa Mínima 4%',
   '• Mayor visibilidad en búsquedas.\n• Botón de compra exprés de repuestos para técnicos.\n• Acceso a estadísticas de ventas.\n• Carga de inventario (hasta 30 productos).',
   30, 4, 4),

  ('STORE-PLUS-50', 'Membresía Comercio Plus (Desde Mes 4)', 'store', 5000, 'USD', 0,
   50, 30, 30, true,
   'Tasa Mínima 4%',
   '• Mayor visibilidad en búsquedas.\n• Botón de compra exprés de repuestos para técnicos.\n• Acceso a estadísticas de ventas.\n• Carga de inventario (hasta 70 productos).',
   70, 4, 4),

  ('STORE-BASE', 'Modalidad Base Sin Suscripción (Desde Mes 4)', 'store', 0, 'USD', 0,
   0, 0, 30, true,
   '10% por venta',
   '• Permanencia en la app sin costo fijo.\n• Ideal para comercios con muy bajo volumen inicial.\n• Carga de inventario (hasta 10 productos).',
   10, 10, 10),

  -- Proveedores (Servicios Contratados)
  ('PRO-PROMO', 'Plan Promocional Entrada (Meses 1 al 3)', 'provider', 0, 'USD', 15,
   10, 90, 0, true,
   '0% (Primer mes) / 3% (Meses 2 y 3)',
   '• Certificación de perfil gratis.\n• Acceso completo a la cartelera de anuncios.',
   0, 0, 0),

  ('PRO-MEMBERSHIP-15', 'Membresía Profesional (Desde Mes 4)', 'provider', 1500, 'USD', 30,
   30, 30, 0, true,
   'Tasa Mínima: 3% Presencial / 6% Remoto',
   '• Prioridad para recibir solicitudes directas.\n• Descuento en comisión para mitigar gastos presenciales (gasolina, viáticos).',
   0, 3, 6),

  ('PRO-BASE', 'Modalidad Base Sin Suscripción (Desde Mes 4)', 'provider', 0, 'USD', 5,
   0, 0, 0, true,
   '8% Presencial / 12% Remoto',
   '• No paga mensualidad.\n• Ideal para trabajadores ocasionales o remotos con menos costos físicos.',
   0, 8, 12)
ON CONFLICT (code) DO UPDATE SET
  name = EXCLUDED.name,
  affiliate_type = EXCLUDED.affiliate_type,
  price_minor = EXCLUDED.price_minor,
  currency = EXCLUDED.currency,
  fee_summary = EXCLUDED.fee_summary,
  benefits_summary = EXCLUDED.benefits_summary,
  max_products = EXCLUDED.max_products,
  presential_fee_percent = EXCLUDED.presential_fee_percent,
  remote_fee_percent = EXCLUDED.remote_fee_percent,
  active = EXCLUDED.active;

-- 5. Métricas operativas completas para el Administrador
CREATE OR REPLACE FUNCTION api.admin_overview()
RETURNS jsonb
LANGUAGE plpgsql
STABLE SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public', 'pg_temp'
AS $$
BEGIN
  PERFORM internal.require_permission('platform.read');
  RETURN jsonb_build_object(
    'users', (SELECT count(*) FROM app.users),
    'professionals', (SELECT count(*) FROM app.professionals),
    'contracts', (SELECT count(*) FROM app.contracts),
    'active_contracts', (SELECT count(*) FROM app.contracts WHERE status IN ('accepted','en_route','arrived','in_progress','awaiting_confirmation')),
    'pending_requests', (SELECT count(*) FROM app.service_requests WHERE moderation_status='pending'),
    'pending_kyc', (SELECT count(*) FROM app.kyc_cases WHERE status IN('pending','in_review')),
    'open_tickets', (SELECT count(*) FROM app.support_tickets WHERE status NOT IN('closed','resolved')),
    'stores', (SELECT count(*) FROM app.stores),
    'active_stores', (SELECT count(*) FROM app.stores WHERE status='active'),
    'products', (SELECT count(*) FROM app.products WHERE status='active'),
    'orders', (SELECT count(*) FROM app.orders),
    'marketplace_sales_minor', coalesce((SELECT sum(total_minor) FROM app.orders WHERE status IN ('accepted','dispatched','delivered')), 0),
    'contracts_volume_minor', coalesce((SELECT sum(agreed_price_minor) FROM app.contracts WHERE status IN ('accepted','in_progress','completed')), 0)
  );
END $$;

-- 6. Lista extendida para el panel de administración
CREATE OR REPLACE FUNCTION api.admin_list(p_resource text, p_limit integer DEFAULT 50, p_offset integer DEFAULT 0)
RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE tbl text; perm text;
BEGIN
  IF p_limit NOT BETWEEN 1 AND 100 OR p_offset < 0 THEN RAISE EXCEPTION 'invalid_pagination'; END IF;

  SELECT t, p INTO tbl, perm FROM (VALUES
    ('users', 'users', 'users.manage'),
    ('requests', 'service_requests', 'content.moderate'),
    ('reviews', 'reviews', 'content.moderate'),
    ('files', 'files', 'content.moderate'),
    ('kyc', 'kyc_cases', 'kyc.review'),
    ('plans', 'plans', 'plans.manage'),
    ('fees', 'withdrawal_fee_policies', 'money.configure'),
    ('tickets', 'support_tickets', 'support.read'),
    ('withdrawals', 'money_withdrawals', 'money.configure'),
    ('audit', 'audit_events', 'platform.read'),
    ('contracts', 'contracts', 'platform.read'),
    ('stores', 'stores', 'platform.read'),
    ('orders', 'orders', 'platform.read'),
    ('products', 'products', 'platform.read')
  ) x(k, t, p) WHERE k = p_resource;

  IF tbl IS NULL THEN RAISE EXCEPTION 'unknown_resource'; END IF;
  PERFORM internal.require_permission(perm);

  IF p_resource = 'withdrawals' THEN
    RETURN QUERY
    SELECT (to_jsonb(wd)-'destination_token') || jsonb_build_object('currency', w.currency, 'owner_name', u.display_name)
    FROM app.money_withdrawals wd
    JOIN app.money_wallets w ON w.id = wd.wallet_id
    JOIN app.users u ON u.id = w.user_id
    ORDER BY wd.created_at DESC, wd.id
    LIMIT p_limit OFFSET p_offset;
    RETURN;
  END IF;

  IF p_resource = 'contracts' THEN
    RETURN QUERY
    SELECT jsonb_build_object(
      'id', c.id,
      'request_id', c.request_id,
      'request_title', coalesce(r.title, 'Servicio acordado'),
      'category_name', coalesce(cat.name, 'Servicios Generales'),
      'customer_id', c.customer_id,
      'customer_name', cu.display_name,
      'customer_email', cu.email,
      'professional_id', c.professional_id,
      'professional_name', pu.display_name,
      'professional_email', pu.email,
      'status', c.status,
      'delivery_mode', c.delivery_mode,
      'agreed_price_minor', c.agreed_price_minor,
      'currency', c.currency,
      'platform_fee_minor', c.platform_fee_minor,
      'payment_protection', c.payment_protection,
      'plan_code', coalesce(p.plan_code, 'PRO-PROMO'),
      'accepted_at', c.accepted_at,
      'completed_at', c.completed_at,
      'cancelled_at', c.cancelled_at,
      'cancellation_reason', c.cancellation_reason
    )
    FROM app.contracts c
    JOIN app.users cu ON cu.id = c.customer_id
    JOIN app.users pu ON pu.id = c.professional_id
    LEFT JOIN app.professionals p ON p.user_id = c.professional_id
    LEFT JOIN app.service_requests r ON r.id = c.request_id
    LEFT JOIN app.categories cat ON cat.id = r.category_id
    ORDER BY coalesce(c.accepted_at, c.customer_accepted_at, now()) DESC, c.id
    LIMIT p_limit OFFSET p_offset;
    RETURN;
  END IF;

  IF p_resource = 'stores' THEN
    RETURN QUERY
    SELECT jsonb_build_object(
      'id', s.id,
      'professional_id', s.professional_id,
      'owner_name', u.display_name,
      'owner_email', u.email,
      'slug', s.slug,
      'name', s.name,
      'description', s.description,
      'status', s.status,
      'plan_code', s.plan_code,
      'created_at', s.created_at,
      'products_count', (SELECT count(*) FROM app.products p WHERE p.store_id = s.id),
      'orders_count', (SELECT count(*) FROM app.orders o WHERE o.store_id = s.id)
    )
    FROM app.stores s
    JOIN app.users u ON u.id = s.professional_id
    ORDER BY s.created_at DESC
    LIMIT p_limit OFFSET p_offset;
    RETURN;
  END IF;

  IF p_resource = 'orders' THEN
    RETURN QUERY
    SELECT jsonb_build_object(
      'id', o.id,
      'customer_id', o.customer_id,
      'customer_name', u.display_name,
      'customer_email', u.email,
      'store_id', o.store_id,
      'store_name', s.name,
      'seller_name', su.display_name,
      'seller_email', su.email,
      'status', o.status,
      'total_minor', o.total_minor,
      'currency', o.currency,
      'payment_method', o.payment_method,
      'payment_status', o.payment_status,
      'platform_fee_minor', o.platform_fee_minor,
      'created_at', o.created_at,
      'items_count', (SELECT count(*) FROM app.order_items oi WHERE oi.order_id = o.id),
      'delivery_contact', (SELECT recipient_name || ' (' || phone || ')' FROM app.order_delivery od WHERE od.order_id = o.id)
    )
    FROM app.orders o
    JOIN app.users u ON u.id = o.customer_id
    JOIN app.stores s ON s.id = o.store_id
    JOIN app.users su ON su.id = s.professional_id
    ORDER BY o.created_at DESC
    LIMIT p_limit OFFSET p_offset;
    RETURN;
  END IF;

  IF p_resource = 'products' THEN
    RETURN QUERY
    SELECT jsonb_build_object(
      'id', p.id,
      'store_id', p.store_id,
      'store_name', s.name,
      'seller_name', su.display_name,
      'name', p.name,
      'sku', p.sku,
      'description', p.description,
      'price_minor', p.price_minor,
      'currency', p.currency,
      'stock', p.stock,
      'status', p.status,
      'created_at', p.created_at
    )
    FROM app.products p
    JOIN app.stores s ON s.id = p.store_id
    JOIN app.users su ON su.id = s.professional_id
    ORDER BY p.created_at DESC
    LIMIT p_limit OFFSET p_offset;
    RETURN;
  END IF;

  IF p_resource = 'plans' THEN
    RETURN QUERY
    SELECT jsonb_build_object(
      'id', pl.id,
      'code', pl.code,
      'name', pl.name,
      'affiliate_type', pl.affiliate_type,
      'price_minor', pl.price_minor,
      'currency', pl.currency,
      'credits', pl.credits,
      'fee_summary', pl.fee_summary,
      'benefits_summary', pl.benefits_summary,
      'max_products', pl.max_products,
      'presential_fee_percent', pl.presential_fee_percent,
      'remote_fee_percent', pl.remote_fee_percent,
      'positioning_weight', pl.positioning_weight,
      'active', pl.active,
      'created_at', pl.created_at
    )
    FROM app.plans pl
    ORDER BY pl.affiliate_type, pl.price_minor ASC, pl.code;
    RETURN;
  END IF;

  RETURN QUERY EXECUTE format('SELECT to_jsonb(t)-ARRAY[''document_ciphertext'',''identity_fingerprint'',''encryption_key_id'',''object_key'',''destination_token''] FROM app.%I t ORDER BY 1 LIMIT $1 OFFSET $2', tbl) USING p_limit, p_offset;
END $$;

-- 7. Funciones de gestión administrativa para Tiendas y Órdenes
CREATE OR REPLACE FUNCTION api.set_store_status(p_store uuid, p_status text, p_reason text)
RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.require_permission('users.manage');
BEGIN
  IF p_status NOT IN ('active', 'suspended', 'closed') THEN RAISE EXCEPTION 'invalid_store_status'; END IF;
  IF length(trim(coalesce(p_reason, ''))) < 5 THEN RAISE EXCEPTION 'reason_required'; END IF;
  UPDATE app.stores SET status = p_status WHERE id = p_store;
  PERFORM internal.audit('store.status_changed', 'store', p_store, jsonb_build_object('status', p_status, 'reason', p_reason));
  RETURN p_status;
END $$;

CREATE OR REPLACE FUNCTION api.set_store_plan(p_store uuid, p_plan text, p_reason text)
RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.require_permission('plans.manage');
BEGIN
  IF NOT EXISTS (SELECT 1 FROM app.plans WHERE code = p_plan AND active) THEN RAISE EXCEPTION 'invalid_plan_code'; END IF;
  IF length(trim(coalesce(p_reason, ''))) < 5 THEN RAISE EXCEPTION 'reason_required'; END IF;
  UPDATE app.stores SET plan_code = p_plan WHERE id = p_store;
  PERFORM internal.audit('store.plan_changed', 'store', p_store, jsonb_build_object('plan_code', p_plan, 'reason', p_reason));
  RETURN p_plan;
END $$;

CREATE OR REPLACE FUNCTION api.set_professional_plan(p_professional uuid, p_plan text, p_reason text)
RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.require_permission('plans.manage');
BEGIN
  IF NOT EXISTS (SELECT 1 FROM app.plans WHERE code = p_plan AND active) THEN RAISE EXCEPTION 'invalid_plan_code'; END IF;
  IF length(trim(coalesce(p_reason, ''))) < 5 THEN RAISE EXCEPTION 'reason_required'; END IF;
  UPDATE app.professionals SET plan_code = p_plan WHERE user_id = p_professional;
  PERFORM internal.audit('professional.plan_changed', 'professional', p_professional, jsonb_build_object('plan_code', p_plan, 'reason', p_reason));
  RETURN p_plan;
END $$;

CREATE OR REPLACE FUNCTION api.admin_advance_order(p_order uuid, p_action text, p_reason text)
RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.require_permission('support.manage');
  o app.orders; v_status text; item record;
BEGIN
  SELECT * INTO o FROM app.orders WHERE id = p_order FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'order_not_found'; END IF;
  IF length(trim(coalesce(p_reason, ''))) < 5 THEN RAISE EXCEPTION 'reason_required'; END IF;

  IF p_action = 'cancel' THEN
    IF o.status = 'cancelled' THEN RETURN o.status; END IF;
    FOR item IN SELECT * FROM app.order_items WHERE order_id = o.id ORDER BY product_id LOOP
      UPDATE app.products SET stock = stock + item.quantity, updated_at = now() WHERE id = item.product_id;
      INSERT INTO app.inventory_movements(product_id, order_id, delta, reason, actor_id) VALUES(item.product_id, o.id, item.quantity, 'admin_order_cancelled', v);
    END LOOP;
    v_status := 'cancelled';
  ELSIF p_action = 'accept' THEN v_status := 'accepted';
  ELSIF p_action = 'dispatch' THEN v_status := 'dispatched';
  ELSIF p_action = 'receive' THEN v_status := 'delivered';
  ELSE RAISE EXCEPTION 'invalid_order_transition'; END IF;

  UPDATE app.orders SET status = v_status WHERE id = o.id;
  INSERT INTO app.order_events(order_id, actor_id, event_type, metadata)
  VALUES(o.id, v, 'admin_' || p_action, jsonb_build_object('from', o.status, 'to', v_status, 'reason', p_reason));
  PERFORM internal.audit('order.admin_' || p_action, 'order', o.id, jsonb_build_object('reason', p_reason));
  RETURN v_status;
END $$;

-- 8. Permisos de ejecución
GRANT EXECUTE ON FUNCTION
  api.admin_overview(),
  api.admin_list(text, integer, integer),
  api.set_store_status(uuid, text, text),
  api.set_store_plan(uuid, text, text),
  api.set_professional_plan(uuid, text, text),
  api.admin_advance_order(uuid, text, text)
TO chamba_admin;
