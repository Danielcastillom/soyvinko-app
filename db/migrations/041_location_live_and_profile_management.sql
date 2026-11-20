-- 041_location_live_and_profile_management.sql
--   1. Ubicación del cliente en vivo: share_location acepta coordenadas GPS (punto de encuentro) y
--      update_shared_location las actualiza mientras dure el permiso. navigate las entrega al profesional.
--   2. location_status: estado del permiso (activo, desde/hasta, origen) sin exponer coordenadas ni
--      escribir auditoría, para consultarlo con frecuencia.
--   3. Gestión del perfil profesional: quitar servicios (se archivan si tienen historial), sedes,
--      portafolio, credenciales y contactos.
--   4. Productos: store_products y my_products devuelven fotos y nombre de tienda; remove_product_image.

-- 1. Ubicación en vivo del cliente --------------------------------------------------------------
ALTER TABLE app.location_shares
  ADD COLUMN IF NOT EXISTS source text NOT NULL DEFAULT 'address',
  ADD COLUMN IF NOT EXISTS location public.geography(Point, 4326),
  ADD COLUMN IF NOT EXISTS accuracy_m integer,
  ADD COLUMN IF NOT EXISTS location_updated_at timestamptz;

DO $$BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'location_shares_source_check') THEN
    ALTER TABLE app.location_shares ADD CONSTRAINT location_shares_source_check
      CHECK ((source = 'address' AND location IS NULL) OR (source = 'gps' AND location IS NOT NULL));
  END IF;
END$$;

DROP FUNCTION IF EXISTS api.share_location(uuid, integer);
CREATE FUNCTION api.share_location(
  p_contract uuid,
  p_hours integer DEFAULT 8,
  p_lon double precision DEFAULT NULL,
  p_lat double precision DEFAULT NULL,
  p_accuracy integer DEFAULT NULL
) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); c app.contracts; v_id uuid; v_chat uuid; v_gps boolean := p_lon IS NOT NULL AND p_lat IS NOT NULL;
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract FOR UPDATE;
  IF NOT FOUND OR c.customer_id<>v THEN RAISE EXCEPTION 'only_customer_can_share' USING ERRCODE='42501'; END IF;
  IF c.status NOT IN ('accepted','en_route','arrived','in_progress','awaiting_confirmation') OR p_hours IS NULL OR p_hours NOT BETWEEN 1 AND 24 THEN
    RAISE EXCEPTION 'location_share_not_allowed';
  END IF;
  IF (p_lon IS NULL) <> (p_lat IS NULL) OR (v_gps AND (p_lat NOT BETWEEN -90 AND 90 OR p_lon NOT BETWEEN -180 AND 180))
     OR (p_accuracy IS NOT NULL AND p_accuracy NOT BETWEEN 0 AND 100000) THEN
    RAISE EXCEPTION 'invalid_coordinates';
  END IF;
  UPDATE app.location_shares SET revoked_at=now() WHERE contract_id=c.id AND revoked_at IS NULL;
  INSERT INTO app.location_shares(contract_id,customer_id,expires_at,source,location,accuracy_m,location_updated_at)
  VALUES(c.id,v,now()+make_interval(hours=>p_hours),
    CASE WHEN v_gps THEN 'gps' ELSE 'address' END,
    CASE WHEN v_gps THEN internal.point(p_lon,p_lat) END,
    CASE WHEN v_gps THEN p_accuracy END,
    CASE WHEN v_gps THEN now() END)
  RETURNING id INTO v_id;
  SELECT id INTO v_chat FROM app.conversations WHERE contract_id=c.id;
  INSERT INTO app.messages(conversation_id,sender_id,client_message_id,kind,body)
  VALUES(v_chat,v,v_id,'location_shared',
    CASE WHEN v_gps THEN 'El cliente compartió su ubicación en vivo.' ELSE 'El cliente ha compartido la ubicación del servicio.' END);
  PERFORM internal.audit('location.shared','contract',c.id,jsonb_build_object('share_id',v_id,'source',CASE WHEN v_gps THEN 'gps' ELSE 'address' END));
  RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION api.update_shared_location(p_contract uuid, p_lon double precision, p_lat double precision, p_accuracy integer DEFAULT NULL)
RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor();
BEGIN
  IF p_lon IS NULL OR p_lat IS NULL OR p_lat NOT BETWEEN -90 AND 90 OR p_lon NOT BETWEEN -180 AND 180
     OR (p_accuracy IS NOT NULL AND p_accuracy NOT BETWEEN 0 AND 100000) THEN
    RAISE EXCEPTION 'invalid_coordinates';
  END IF;
  UPDATE app.location_shares s SET location=internal.point(p_lon,p_lat), accuracy_m=p_accuracy, location_updated_at=now()
  FROM app.contracts c
  WHERE s.contract_id=p_contract AND c.id=s.contract_id AND c.customer_id=v AND s.source='gps'
    AND s.revoked_at IS NULL AND s.expires_at>now()
    AND c.status IN ('accepted','en_route','arrived','in_progress','awaiting_confirmation');
  IF NOT FOUND THEN RAISE EXCEPTION 'location_not_shared' USING ERRCODE='42501'; END IF;
END $$;

CREATE OR REPLACE FUNCTION api.navigate(p_contract uuid) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts := internal.participant(p_contract); s app.location_shares; v_result jsonb;
BEGIN
  SELECT * INTO s FROM app.location_shares WHERE contract_id=c.id AND revoked_at IS NULL AND expires_at>now()
  ORDER BY shared_at DESC LIMIT 1;
  IF c.status NOT IN ('accepted','en_route','arrived','in_progress','awaiting_confirmation') OR s.id IS NULL THEN
    RAISE EXCEPTION 'location_not_shared' USING ERRCODE='42501';
  END IF;
  SELECT jsonb_build_object(
    -- Destino: el GPS compartido por el cliente, o la dirección registrada en la solicitud.
    'destination', CASE WHEN s.source='gps' THEN jsonb_build_object(
        'latitude',public.ST_Y(s.location::public.geometry),'longitude',public.ST_X(s.location::public.geometry),
        'address',l.address_line,'instructions',l.instructions,'source','gps',
        'accuracy_m',s.accuracy_m,'updated_at',s.location_updated_at)
      ELSE jsonb_build_object('latitude',public.ST_Y(l.location::public.geometry),
        'longitude',public.ST_X(l.location::public.geometry),'address',l.address_line,'instructions',l.instructions,'source','address')
      END,
    'share', jsonb_build_object('shared_at',s.shared_at,'expires_at',s.expires_at,'source',s.source),
    'professional_position',(SELECT jsonb_build_object('latitude',public.ST_Y(p.location::public.geometry),
      'longitude',public.ST_X(p.location::public.geometry),'recorded_at',p.recorded_at,'accuracy_m',p.accuracy_m)
      FROM app.professional_positions p WHERE p.contract_id=c.id AND p.expires_at>now()))
  INTO v_result FROM app.request_locations l WHERE l.request_id=c.request_id;
  PERFORM internal.audit('location.read','contract',c.id);
  RETURN v_result;
END $$;

CREATE OR REPLACE FUNCTION api.location_status(p_contract uuid) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts := internal.participant(p_contract); s app.location_shares;
BEGIN
  SELECT * INTO s FROM app.location_shares WHERE contract_id=c.id ORDER BY shared_at DESC LIMIT 1;
  RETURN jsonb_build_object(
    'active', s.id IS NOT NULL AND s.revoked_at IS NULL AND s.expires_at>now()
              AND c.status IN ('accepted','en_route','arrived','in_progress','awaiting_confirmation'),
    'shared_at', s.shared_at, 'expires_at', s.expires_at, 'revoked_at', s.revoked_at,
    'source', s.source, 'location_updated_at', s.location_updated_at,
    'professional_position_at', (SELECT p.recorded_at FROM app.professional_positions p WHERE p.contract_id=c.id AND p.expires_at>now()),
    'remote', c.delivery_mode='remote');
END $$;

-- 3. Gestión del perfil profesional -------------------------------------------------------------
ALTER TABLE app.offerings ADD COLUMN IF NOT EXISTS archived_at timestamptz;

-- Un servicio archivado no se puede reactivar ni editar (conserva el historial de solicitudes).
CREATE OR REPLACE FUNCTION internal.guard_archived_offering() RETURNS trigger
LANGUAGE plpgsql SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  IF OLD.archived_at IS NOT NULL THEN RAISE EXCEPTION 'offering_archived'; END IF;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS offering_archived_guard ON app.offerings;
CREATE TRIGGER offering_archived_guard BEFORE UPDATE ON app.offerings
  FOR EACH ROW WHEN (OLD.archived_at IS NOT NULL) EXECUTE FUNCTION internal.guard_archived_offering();

CREATE OR REPLACE FUNCTION api.remove_offering(p_offering uuid) RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); o app.offerings;
BEGIN
  SELECT * INTO o FROM app.offerings WHERE id=p_offering AND professional_id=v FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'offering_not_owned' USING ERRCODE='42501'; END IF;
  IF o.archived_at IS NOT NULL THEN RETURN 'archived'; END IF;
  IF EXISTS (SELECT 1 FROM app.service_requests WHERE offering_id=o.id) OR EXISTS (SELECT 1 FROM app.proposals WHERE offering_id=o.id) THEN
    UPDATE app.offerings SET active=false, archived_at=now() WHERE id=o.id;
    PERFORM internal.audit('offering.archived','offering',o.id);
    RETURN 'archived';
  END IF;
  DELETE FROM app.offerings WHERE id=o.id;
  PERFORM internal.audit('offering.removed','offering',o.id);
  RETURN 'deleted';
END $$;

CREATE OR REPLACE FUNCTION api.remove_branch(p_branch uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor();
BEGIN
  PERFORM 1 FROM app.branches WHERE id=p_branch AND professional_id=v FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'branch_not_owned' USING ERRCODE='42501'; END IF;
  DELETE FROM app.coverage_areas WHERE branch_id=p_branch;
  DELETE FROM app.branch_hours WHERE branch_id=p_branch;
  UPDATE app.professional_contacts SET branch_id=NULL WHERE branch_id=p_branch;
  DELETE FROM app.branches WHERE id=p_branch;
  PERFORM internal.audit('branch.removed','branch',p_branch);
END $$;

CREATE OR REPLACE FUNCTION api.remove_portfolio_item(p_item uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); v_file uuid;
BEGIN
  DELETE FROM app.portfolio_items WHERE id=p_item AND professional_id=v RETURNING file_id INTO v_file;
  IF NOT FOUND THEN RAISE EXCEPTION 'resource_not_owned' USING ERRCODE='42501'; END IF;
  -- El worker borra el archivo del disco (purga de archivos eliminados).
  UPDATE app.files SET deleted_at=now() WHERE id=v_file AND owner_id=v AND deleted_at IS NULL;
END $$;

CREATE OR REPLACE FUNCTION api.remove_credential(p_credential uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); v_file uuid;
BEGIN
  DELETE FROM app.professional_credentials WHERE id=p_credential AND professional_id=v RETURNING evidence_file_id INTO v_file;
  IF NOT FOUND THEN RAISE EXCEPTION 'resource_not_owned' USING ERRCODE='42501'; END IF;
  UPDATE app.files SET deleted_at=now() WHERE id=v_file AND owner_id=v AND deleted_at IS NULL;
END $$;

CREATE OR REPLACE FUNCTION api.remove_professional_contact(p_contact uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  DELETE FROM app.professional_contacts WHERE id=p_contact AND professional_id=internal.actor();
  IF NOT FOUND THEN RAISE EXCEPTION 'contact_not_owned' USING ERRCODE='42501'; END IF;
END $$;

-- El panel del profesional ya no muestra servicios archivados.
CREATE OR REPLACE FUNCTION api.my_professional() RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(p)||jsonb_build_object('branches',(SELECT jsonb_agg((to_jsonb(b)-'location')||jsonb_build_object(
    'longitude',public.ST_X(b.location::public.geometry),'latitude',public.ST_Y(b.location::public.geometry),
    'state_name',s.name,'municipality_name',m.name,'parish_name',par.name)) FROM app.branches b
    LEFT JOIN app.geo_parishes par ON par.code=b.parish_code LEFT JOIN app.geo_municipalities m ON m.code=b.municipality_code
    LEFT JOIN app.geo_states s ON s.code=b.state_code WHERE b.professional_id=p.user_id),
    'contacts',(SELECT jsonb_agg(to_jsonb(c)) FROM app.professional_contacts c WHERE c.professional_id=p.user_id),
    'coverage',(SELECT jsonb_agg(to_jsonb(c)-'polygon') FROM app.coverage_areas c WHERE c.professional_id=p.user_id),
    'offerings',(SELECT jsonb_agg(to_jsonb(o)) FROM app.offerings o WHERE o.professional_id=p.user_id AND o.archived_at IS NULL),
    'store',(SELECT to_jsonb(s) FROM app.stores s WHERE s.professional_id=p.user_id))
  FROM app.professionals p WHERE p.user_id=internal.actor();
$$;

-- 4. Productos con fotos ------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION api.store_products(p_store uuid) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  PERFORM internal.actor();
  RETURN QUERY SELECT to_jsonb(p)||jsonb_build_object('store_name',s.name,'professional_id',s.professional_id,
      'images',coalesce((SELECT jsonb_agg(i.file_id ORDER BY i.sort_order, i.file_id) FROM app.product_images i JOIN app.files f ON f.id=i.file_id
        WHERE i.product_id=p.id AND f.moderation_status='approved' AND f.deleted_at IS NULL),'[]'::jsonb))
    FROM app.products p JOIN app.stores s ON s.id=p.store_id
    JOIN app.professionals pro ON pro.user_id=s.professional_id JOIN app.users u ON u.id=pro.user_id
    WHERE s.id=p_store AND s.status='active' AND pro.status='active' AND u.status='active'
      AND p.status='active' AND internal.has_entitlement(s.professional_id,'marketplace') ORDER BY p.name,p.id LIMIT 200;
END $$;

-- El vendedor ve todas sus fotos, también las que esperan moderación.
CREATE OR REPLACE FUNCTION api.my_products() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(p)||jsonb_build_object('store_name',s.name,
    'images',coalesce((SELECT jsonb_agg(jsonb_build_object('file_id',i.file_id,'moderation_status',f.moderation_status) ORDER BY i.sort_order, i.file_id)
      FROM app.product_images i JOIN app.files f ON f.id=i.file_id WHERE i.product_id=p.id AND f.deleted_at IS NULL),'[]'::jsonb))
  FROM app.products p JOIN app.stores s ON s.id=p.store_id WHERE s.professional_id=internal.actor() ORDER BY p.created_at DESC LIMIT 200;
$$;

CREATE OR REPLACE FUNCTION api.remove_product_image(p_product uuid, p_file uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor();
BEGIN
  DELETE FROM app.product_images i USING app.products p, app.stores s
  WHERE i.product_id=p_product AND i.file_id=p_file AND p.id=i.product_id AND s.id=p.store_id AND s.professional_id=v;
  IF NOT FOUND THEN RAISE EXCEPTION 'product_not_owned' USING ERRCODE='42501'; END IF;
  UPDATE app.files SET deleted_at=now() WHERE id=p_file AND owner_id=v AND deleted_at IS NULL
    AND NOT EXISTS (SELECT 1 FROM app.product_images WHERE file_id=p_file);
END $$;

-- Permisos: solo la API de usuarios.
REVOKE ALL ON FUNCTION
  api.share_location(uuid, integer, double precision, double precision, integer),
  api.update_shared_location(uuid, double precision, double precision, integer),
  api.location_status(uuid),
  api.remove_offering(uuid), api.remove_branch(uuid), api.remove_portfolio_item(uuid),
  api.remove_credential(uuid), api.remove_professional_contact(uuid), api.remove_product_image(uuid, uuid)
FROM PUBLIC;
GRANT EXECUTE ON FUNCTION
  api.share_location(uuid, integer, double precision, double precision, integer),
  api.update_shared_location(uuid, double precision, double precision, integer),
  api.location_status(uuid),
  api.remove_offering(uuid), api.remove_branch(uuid), api.remove_portfolio_item(uuid),
  api.remove_credential(uuid), api.remove_professional_contact(uuid), api.remove_product_image(uuid, uuid)
TO chamba_api;
