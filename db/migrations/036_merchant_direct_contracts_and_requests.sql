-- 036_merchant_direct_contracts_and_requests.sql
-- Autonomía completa para comerciantes/profesionales: validación directa de contrataciones de clientes,
-- creación inmediata del contrato digital de servicio y enriquecimiento de contratos y ubicaciones.

-- 1. Permitir que el profesional valide de inmediato la contratación de su servicio sin bloqueos
CREATE OR REPLACE FUNCTION api.accept_direct_request(p_request uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v uuid := internal.actor();
  r app.service_requests;
  c app.contracts;
  price_to_use bigint;
BEGIN
  SELECT * INTO r FROM app.service_requests WHERE id=p_request FOR UPDATE;
  IF NOT FOUND OR r.mode<>'direct' OR r.target_professional_id<>v THEN
    RAISE EXCEPTION 'not_target_professional' USING ERRCODE='42501';
  END IF;

  SELECT * INTO c FROM app.contracts WHERE request_id=p_request;
  IF FOUND THEN RETURN c.id; END IF;

  -- Calcular el precio acordado para el contrato digital:
  -- Prioridad 1: precio del servicio anunciado si proviene de un offering
  SELECT price_min_minor INTO price_to_use FROM app.offerings WHERE id=r.offering_id;
  
  -- Prioridad 2: presupuesto del cliente u oferta
  IF price_to_use IS NULL OR price_to_use <= 0 THEN
    price_to_use := coalesce(r.budget_max_minor, r.offered_price_minor, 0);
  END IF;

  -- Formalizar el contrato digital del servicio
  RETURN internal.make_contract(r.id, v, NULL, price_to_use);
END $$;

-- 2. Enriquecer api.my_contracts con información comercial, nombres y títulos
CREATE OR REPLACE FUNCTION api.my_contracts() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT (to_jsonb(c) || jsonb_build_object(
    'title', coalesce((SELECT title FROM app.service_requests WHERE id=c.request_id), 'Servicio contratado'),
    'customer_name', (SELECT display_name FROM app.users WHERE id=c.customer_id),
    'professional_name', (SELECT coalesce(p.business_name, u.display_name) FROM app.users u LEFT JOIN app.professionals p ON p.user_id=u.id WHERE u.id=c.professional_id),
    'customer_email', (SELECT email FROM app.users WHERE id=c.customer_id),
    'is_customer', (internal.actor() = c.customer_id),
    'is_professional', (internal.actor() = c.professional_id),
    'reviews_count', (SELECT count(*) FROM app.reviews r WHERE r.contract_id=c.id)
  ))
  FROM app.contracts c
  WHERE internal.actor() IN (customer_id, professional_id)
  ORDER BY accepted_at DESC LIMIT 200;
$$;

-- 3. Enriquecer api.contract_detail con datos completos del proyecto y ubicación presencial
CREATE OR REPLACE FUNCTION api.contract_detail(p_contract uuid) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  c app.contracts := internal.participant(p_contract);
  sr app.service_requests;
BEGIN
  SELECT * INTO sr FROM app.service_requests WHERE id=c.request_id;
  RETURN to_jsonb(c) || jsonb_build_object(
    'title', coalesce(sr.title, 'Servicio contratado'),
    'customer_name', (SELECT display_name FROM app.users WHERE id=c.customer_id),
    'customer_email', (SELECT email FROM app.users WHERE id=c.customer_id),
    'professional_name', (SELECT coalesce(p.business_name, u.display_name) FROM app.users u LEFT JOIN app.professionals p ON p.user_id=u.id WHERE u.id=c.professional_id),
    'offering_title', (SELECT o.title FROM app.offerings o WHERE o.id=sr.offering_id),
    'request_description', sr.description,
    'terms', (SELECT jsonb_agg(to_jsonb(t) ORDER BY version DESC) FROM app.contract_terms t WHERE contract_id=c.id),
    'events', (SELECT jsonb_agg(to_jsonb(e) ORDER BY id) FROM app.contract_events e WHERE contract_id=c.id),
    'reference_photos', coalesce((SELECT jsonb_agg(jsonb_build_object('file_id',f.id,'mime_type',f.mime_type) ORDER BY f.created_at,f.id)
      FROM app.request_attachments ra JOIN app.files f ON f.id=ra.file_id
      WHERE ra.request_id=c.request_id AND f.moderation_status='approved' AND f.exif_removed AND f.deleted_at IS NULL),'[]'::jsonb),
    'reviews', coalesce((SELECT jsonb_agg(to_jsonb(r) ORDER BY r.created_at DESC) FROM app.reviews r WHERE r.contract_id=c.id), '[]'::jsonb),
    'onsite_location', (SELECT jsonb_build_object(
      'address_line', rl.address_line,
      'instructions', rl.instructions,
      'city', rl.city,
      'country_code', rl.country_code
    ) FROM app.request_locations rl WHERE rl.request_id=c.request_id)
  );
END $$;

GRANT EXECUTE ON FUNCTION api.accept_direct_request(uuid) TO chamba_api, chamba_auth, chamba_admin;
GRANT EXECUTE ON FUNCTION api.my_contracts() TO chamba_api, chamba_auth, chamba_admin;
GRANT EXECUTE ON FUNCTION api.contract_detail(uuid) TO chamba_api, chamba_auth, chamba_admin;
