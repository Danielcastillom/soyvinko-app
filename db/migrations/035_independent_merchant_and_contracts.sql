-- 035_independent_merchant_and_contracts.sql
-- Independencia total de comerciantes y clientes, publicación directa y contrato digital transparente

-- 1. Los reviews y calificaciones se publican de inmediato sin intervención administrativa
ALTER TABLE app.reviews ALTER COLUMN moderation_status SET DEFAULT 'published';
UPDATE app.reviews SET moderation_status='published' WHERE moderation_status='pending';

-- 2. Las solicitudes y necesidades de clientes se aprueban por defecto
ALTER TABLE app.service_requests ALTER COLUMN moderation_status SET DEFAULT 'approved';
UPDATE app.service_requests SET moderation_status='approved' WHERE moderation_status='pending';

-- 3. Archivos de portafolio, evidencia y solicitudes se aprueban automáticamente
UPDATE app.files SET moderation_status='approved'
WHERE purpose IN ('portfolio','evidence','product','request') AND moderation_status='pending';

-- 4. Actualizar api.rate_contract para publicar de inmediato y soportar edición
CREATE OR REPLACE FUNCTION api.rate_contract(p_contract uuid, p_rating smallint, p_recommends boolean, p_comment text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  c app.contracts := internal.participant(p_contract);
  v uuid := internal.actor();
  v_id uuid;
  v_subject uuid;
BEGIN
  IF c.status<>'completed' THEN RAISE EXCEPTION 'contract_not_completed'; END IF;
  v_subject := CASE WHEN v=c.customer_id THEN c.professional_id ELSE c.customer_id END;
  
  SELECT id INTO v_id FROM app.reviews WHERE contract_id=c.id AND author_id=v;
  IF FOUND THEN
    UPDATE app.reviews
    SET rating=p_rating, recommends=p_recommends, comment=p_comment, moderation_status='published'
    WHERE id=v_id;
  ELSE
    INSERT INTO app.reviews(contract_id, author_id, subject_id, rating, recommends, comment, moderation_status)
    VALUES(c.id, v, v_subject, p_rating, p_recommends, p_comment, 'published')
    RETURNING id INTO v_id;
  END IF;

  PERFORM internal.audit('review.created', 'review', v_id, jsonb_build_object('moderation', 'published_instant'));
  RETURN v_id;
END $$;

-- 5. Contactos mutuos desbloqueados para el contrato digital
CREATE OR REPLACE FUNCTION api.contract_contacts(p_contract uuid) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  c app.contracts := internal.participant(p_contract);
  v uuid := internal.actor();
  v_other uuid;
  u app.users;
  p app.professionals;
  v_phone text;
  v_whatsapp text;
  result jsonb;
BEGIN
  IF c.status='cancelled' THEN RAISE EXCEPTION 'contract_cancelled'; END IF;
  v_other := CASE WHEN v=c.customer_id THEN c.professional_id ELSE c.customer_id END;

  SELECT * INTO u FROM app.users WHERE id=v_other;
  SELECT * INTO p FROM app.professionals WHERE user_id=v_other;

  -- Buscar whatsapp prioritario
  SELECT value INTO v_whatsapp FROM app.user_contacts WHERE user_id=v_other AND kind='whatsapp' LIMIT 1;
  IF v_whatsapp IS NULL THEN
    SELECT value INTO v_whatsapp FROM app.professional_contacts WHERE professional_id=v_other AND kind='whatsapp' LIMIT 1;
  END IF;

  -- Buscar teléfono prioritario
  SELECT value INTO v_phone FROM app.user_contacts WHERE user_id=v_other AND kind='phone' LIMIT 1;
  IF v_phone IS NULL THEN
    SELECT value INTO v_phone FROM app.professional_contacts WHERE professional_id=v_other AND kind='phone' LIMIT 1;
  END IF;
  IF v_phone IS NULL THEN
    v_phone := v_whatsapp;
  END IF;

  SELECT jsonb_build_object(
    'user_id', u.id,
    'name', coalesce(p.business_name, u.display_name),
    'display_name', u.display_name,
    'business_name', p.business_name,
    'email', u.email,
    'primary_phone', v_phone,
    'primary_whatsapp', v_whatsapp,
    'contacts', coalesce((SELECT jsonb_agg(jsonb_build_object('id', ct.id, 'kind', ct.kind, 'label', ct.label, 'value', ct.value))
       FROM app.user_contacts ct WHERE ct.user_id=u.id), '[]'::jsonb),
    'professional_contacts', CASE WHEN v_other=c.professional_id THEN
       coalesce((SELECT jsonb_agg(jsonb_build_object('id', pc.id, 'kind', pc.kind, 'label', pc.label, 'value', pc.value, 'branch_id', pc.branch_id))
         FROM app.professional_contacts pc WHERE pc.professional_id=v_other), '[]'::jsonb)
       ELSE '[]'::jsonb END
  ) INTO result;

  PERFORM internal.audit('contacts.read', 'contract', c.id);
  RETURN result;
END $$;

-- 6. Detalle del contrato digital incluyendo reviews y estado completo
CREATE OR REPLACE FUNCTION api.contract_detail(p_contract uuid) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts := internal.participant(p_contract);
BEGIN
  RETURN to_jsonb(c)||jsonb_build_object(
    'title', (SELECT title FROM app.service_requests WHERE id=c.request_id),
    'customer_name', (SELECT display_name FROM app.users WHERE id=c.customer_id),
    'professional_name', (SELECT business_name FROM app.professionals WHERE user_id=c.professional_id),
    'terms', (SELECT jsonb_agg(to_jsonb(t) ORDER BY version DESC) FROM app.contract_terms t WHERE contract_id=c.id),
    'events', (SELECT jsonb_agg(to_jsonb(e) ORDER BY id) FROM app.contract_events e WHERE contract_id=c.id),
    'reference_photos', coalesce((SELECT jsonb_agg(jsonb_build_object('file_id',f.id,'mime_type',f.mime_type) ORDER BY f.created_at,f.id)
      FROM app.request_attachments ra JOIN app.files f ON f.id=ra.file_id
      WHERE ra.request_id=c.request_id AND f.moderation_status='approved' AND f.exif_removed AND f.deleted_at IS NULL),'[]'::jsonb),
    'reviews', coalesce((SELECT jsonb_agg(to_jsonb(r) ORDER BY r.created_at DESC) FROM app.reviews r WHERE r.contract_id=c.id), '[]'::jsonb)
  );
END $$;

GRANT EXECUTE ON FUNCTION api.rate_contract(uuid, smallint, boolean, text) TO chamba_api, chamba_auth, chamba_admin;
GRANT EXECUTE ON FUNCTION api.contract_contacts(uuid) TO chamba_api, chamba_auth, chamba_admin;
GRANT EXECUTE ON FUNCTION api.contract_detail(uuid) TO chamba_api, chamba_auth, chamba_admin;
