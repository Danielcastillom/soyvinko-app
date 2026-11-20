-- 044_transport_and_deliveries.sql
--   1. Módulo completo para Transportes, Envíos, Fletes y Delivery tipo Uber.
--   2. app.shipments: registro del envío, origen ('desde') y destino ('hasta'),
--      datos de carga, vehículo, tarifa y estado del ciclo de vida.
--   3. Toma de pedido tipo Uber: el despacho se inicia estrictamente cuando el conductor
--      acepta el pedido (status pasa de 'requested' a 'accepted' y accepted_at = now()).
--   4. Rastrear en tiempo real la ubicación de la persona que realiza el envío
--      (app.shipment_positions y current_driver_location en app.shipments).
--   5. Operaciones API: api.create_shipment, api.available_shipments, api.take_shipment,
--      api.update_shipment_location, api.advance_shipment, api.shipment_tracking, api.my_shipments.

-- 1. Tablas de Transportes y Envíos -------------------------------------------------------------

CREATE TABLE IF NOT EXISTS app.shipments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  sender_id uuid NOT NULL REFERENCES app.users(id),
  driver_id uuid REFERENCES app.users(id),
  shipment_type text NOT NULL CHECK (shipment_type IN ('ride', 'package', 'freight', 'food')),
  vehicle_type text NOT NULL CHECK (vehicle_type IN ('moto', 'carro', 'pickup', 'camion')),
  status text NOT NULL DEFAULT 'requested' CHECK (status IN ('requested', 'accepted', 'picking_up', 'in_transit', 'delivered', 'cancelled')),
  title text NOT NULL CHECK (length(trim(title)) >= 3 AND length(title) <= 200),
  description text,
  -- Origen: "Desde"
  origin_address text NOT NULL CHECK (length(trim(origin_address)) >= 3),
  origin_location public.geography(Point, 4326) NOT NULL,
  origin_contact_name text,
  origin_contact_phone text,
  origin_instructions text,
  -- Destino: "Hasta"
  destination_address text NOT NULL CHECK (length(trim(destination_address)) >= 3),
  destination_location public.geography(Point, 4326) NOT NULL,
  destination_contact_name text,
  destination_contact_phone text,
  destination_instructions text,
  -- Distancia y Tarifa
  distance_km numeric(8,2) NOT NULL DEFAULT 0.00,
  fare_minor bigint NOT NULL CHECK (fare_minor > 0),
  currency text NOT NULL DEFAULT 'VES' CHECK (currency IN ('VES', 'USD')),
  -- Ubicación en tiempo real del transportista
  current_driver_location public.geography(Point, 4326),
  last_position_updated_at timestamptz,
  -- Tiempos y Ciclo de Vida (El despacho arranca con accepted_at al tomar el pedido)
  created_at timestamptz NOT NULL DEFAULT now(),
  accepted_at timestamptz,
  picked_up_at timestamptz,
  delivered_at timestamptz,
  cancelled_at timestamptz,
  cancel_reason text
);

CREATE INDEX IF NOT EXISTS shipments_status_idx ON app.shipments(status);
CREATE INDEX IF NOT EXISTS shipments_sender_idx ON app.shipments(sender_id);
CREATE INDEX IF NOT EXISTS shipments_driver_idx ON app.shipments(driver_id);
CREATE INDEX IF NOT EXISTS shipments_origin_gix ON app.shipments USING gist(origin_location);
CREATE INDEX IF NOT EXISTS shipments_destination_gix ON app.shipments USING gist(destination_location);

CREATE TABLE IF NOT EXISTS app.shipment_positions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  shipment_id uuid NOT NULL REFERENCES app.shipments(id) ON DELETE CASCADE,
  driver_id uuid NOT NULL REFERENCES app.users(id),
  location public.geography(Point, 4326) NOT NULL,
  heading double precision,
  speed double precision,
  accuracy_m integer,
  recorded_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS shipment_positions_lookup_idx ON app.shipment_positions(shipment_id, recorded_at DESC);

-- Permisos sobre las tablas a chamba_owner y chamba_api
GRANT SELECT, INSERT, UPDATE, DELETE ON app.shipments TO chamba_api;
GRANT SELECT, INSERT, UPDATE, DELETE ON app.shipment_positions TO chamba_api;

-- 2. Operación api.create_shipment -------------------------------------------------------------

CREATE OR REPLACE FUNCTION api.create_shipment(
  p_shipment_type text,
  p_vehicle_type text,
  p_title text,
  p_origin_address text,
  p_origin_lon double precision,
  p_origin_lat double precision,
  p_destination_address text,
  p_destination_lon double precision,
  p_destination_lat double precision,
  p_fare_minor bigint,
  p_description text DEFAULT NULL,
  p_origin_contact_name text DEFAULT NULL,
  p_origin_contact_phone text DEFAULT NULL,
  p_origin_instructions text DEFAULT NULL,
  p_destination_contact_name text DEFAULT NULL,
  p_destination_contact_phone text DEFAULT NULL,
  p_destination_instructions text DEFAULT NULL,
  p_currency text DEFAULT 'VES'
) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v_actor uuid := internal.actor();
  v_origin public.geography(Point, 4326);
  v_destination public.geography(Point, 4326);
  v_dist_km numeric(8,2);
  v_id uuid;
  v_shipment app.shipments;
BEGIN
  IF p_shipment_type NOT IN ('ride', 'package', 'freight', 'food') THEN
    RAISE EXCEPTION 'invalid_shipment_type';
  END IF;
  IF p_vehicle_type NOT IN ('moto', 'carro', 'pickup', 'camion') THEN
    RAISE EXCEPTION 'invalid_vehicle_type';
  END IF;
  IF p_currency NOT IN ('VES', 'USD') THEN
    RAISE EXCEPTION 'invalid_currency';
  END IF;
  IF p_fare_minor IS NULL OR p_fare_minor <= 0 THEN
    RAISE EXCEPTION 'invalid_fare';
  END IF;

  v_origin := internal.point(p_origin_lon, p_origin_lat);
  v_destination := internal.point(p_destination_lon, p_destination_lat);
  v_dist_km := round((public.ST_Distance(v_origin, v_destination) / 1000.0)::numeric, 2);

  INSERT INTO app.shipments (
    sender_id, shipment_type, vehicle_type, status,
    title, description,
    origin_address, origin_location, origin_contact_name, origin_contact_phone, origin_instructions,
    destination_address, destination_location, destination_contact_name, destination_contact_phone, destination_instructions,
    distance_km, fare_minor, currency
  ) VALUES (
    v_actor, p_shipment_type, p_vehicle_type, 'requested',
    trim(p_title), trim(p_description),
    trim(p_origin_address), v_origin, trim(p_origin_contact_name), trim(p_origin_contact_phone), trim(p_origin_instructions),
    trim(p_destination_address), v_destination, trim(p_destination_contact_name), trim(p_destination_contact_phone), trim(p_destination_instructions),
    v_dist_km, p_fare_minor, p_currency
  ) RETURNING * INTO v_shipment;

  PERFORM internal.audit('shipment.created', 'shipment', v_shipment.id, jsonb_build_object(
    'type', v_shipment.shipment_type,
    'fare_minor', v_shipment.fare_minor,
    'currency', v_shipment.currency
  ));

  RETURN jsonb_build_object(
    'id', v_shipment.id,
    'status', v_shipment.status,
    'title', v_shipment.title,
    'distance_km', v_shipment.distance_km,
    'fare_minor', v_shipment.fare_minor,
    'currency', v_shipment.currency,
    'created_at', v_shipment.created_at
  );
END $$;

-- 3. Operación api.available_shipments (Bolsa de envíos para conductores tipo Uber) --------------

CREATE OR REPLACE FUNCTION api.available_shipments(
  p_lon double precision DEFAULT NULL,
  p_lat double precision DEFAULT NULL,
  p_radius_km double precision DEFAULT 100.0,
  p_vehicle_type text DEFAULT NULL,
  p_shipment_type text DEFAULT NULL
) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v_driver_pt public.geography(Point, 4326) := NULL;
  v_res jsonb;
BEGIN
  IF p_lon IS NOT NULL AND p_lat IS NOT NULL THEN
    v_driver_pt := internal.point(p_lon, p_lat);
  END IF;

  SELECT coalesce(jsonb_agg(item), '[]'::jsonb) INTO v_res FROM (
    SELECT jsonb_build_object(
      'id', s.id,
      'status', s.status,
      'shipment_type', s.shipment_type,
      'vehicle_type', s.vehicle_type,
      'title', s.title,
      'description', s.description,
      'origin_address', s.origin_address,
      'origin_lat', public.ST_Y(s.origin_location::public.geometry),
      'origin_lon', public.ST_X(s.origin_location::public.geometry),
      'destination_address', s.destination_address,
      'destination_lat', public.ST_Y(s.destination_location::public.geometry),
      'destination_lon', public.ST_X(s.destination_location::public.geometry),
      'distance_km', s.distance_km,
      'distance_to_pickup_km', CASE WHEN v_driver_pt IS NOT NULL
        THEN round((public.ST_Distance(s.origin_location, v_driver_pt) / 1000.0)::numeric, 1)
        ELSE NULL END,
      'fare_minor', s.fare_minor,
      'currency', s.currency,
      'created_at', s.created_at,
      'sender_name', u.display_name,
      'sender_avatar_file_id', u.avatar_file_id
    ) AS item
    FROM app.shipments s
    JOIN app.users u ON u.id = s.sender_id
    WHERE s.status = 'requested'
      AND s.driver_id IS NULL
      AND (p_vehicle_type IS NULL OR s.vehicle_type = p_vehicle_type)
      AND (p_shipment_type IS NULL OR s.shipment_type = p_shipment_type)
      AND (v_driver_pt IS NULL OR public.ST_DWithin(s.origin_location, v_driver_pt, coalesce(p_radius_km, 100.0) * 1000.0))
    ORDER BY
      CASE WHEN v_driver_pt IS NOT NULL THEN public.ST_Distance(s.origin_location, v_driver_pt) ELSE 0 END ASC,
      s.created_at DESC
    LIMIT 50
  ) q;

  RETURN v_res;
END $$;

-- 4. Operación api.take_shipment (Tomar envío / Inicia el despacho) -----------------------------

CREATE OR REPLACE FUNCTION api.take_shipment(
  p_shipment uuid
) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v_driver uuid := internal.actor();
  s app.shipments;
  v_sender_name text;
BEGIN
  SELECT * INTO s FROM app.shipments WHERE id = p_shipment FOR UPDATE;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'shipment_not_found';
  END IF;

  IF s.sender_id = v_driver THEN
    RAISE EXCEPTION 'cannot_take_own_shipment';
  END IF;

  IF s.status <> 'requested' OR s.driver_id IS NOT NULL THEN
    RAISE EXCEPTION 'shipment_already_taken';
  END IF;

  -- INICIA EL DESPACHO AL TOMAR EL PEDIDO:
  UPDATE app.shipments
  SET driver_id = v_driver,
      status = 'accepted',
      accepted_at = now()
  WHERE id = s.id
  RETURNING * INTO s;

  SELECT display_name INTO v_sender_name FROM app.users WHERE id = s.sender_id;

  PERFORM internal.audit('shipment.accepted', 'shipment', s.id, jsonb_build_object(
    'driver_id', v_driver,
    'accepted_at', s.accepted_at
  ));

  RETURN jsonb_build_object(
    'id', s.id,
    'status', s.status,
    'driver_id', s.driver_id,
    'accepted_at', s.accepted_at,
    'message', 'Despacho iniciado con éxito.'
  );
END $$;

-- 5. Operación api.update_shipment_location (Reportar GPS en vivo por el conductor) -------------

CREATE OR REPLACE FUNCTION api.update_shipment_location(
  p_shipment uuid,
  p_lon double precision,
  p_lat double precision,
  p_accuracy integer DEFAULT NULL,
  p_heading double precision DEFAULT NULL,
  p_speed double precision DEFAULT NULL
) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v_driver uuid := internal.actor();
  v_pt public.geography(Point, 4326);
  s app.shipments;
BEGIN
  SELECT * INTO s FROM app.shipments WHERE id = p_shipment FOR UPDATE;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'shipment_not_found';
  END IF;

  IF s.driver_id <> v_driver THEN
    RAISE EXCEPTION 'not_authorized_driver' USING ERRCODE='42501';
  END IF;

  IF s.status NOT IN ('accepted', 'picking_up', 'in_transit') THEN
    RAISE EXCEPTION 'shipment_not_active';
  END IF;

  v_pt := internal.point(p_lon, p_lat);

  -- Actualizar última ubicación en el envío
  UPDATE app.shipments
  SET current_driver_location = v_pt,
      last_position_updated_at = now()
  WHERE id = s.id;

  -- Registrar en histórico de posiciones
  INSERT INTO app.shipment_positions(
    shipment_id, driver_id, location, heading, speed, accuracy_m, recorded_at
  ) VALUES (
    s.id, v_driver, v_pt, p_heading, p_speed, p_accuracy, now()
  );

  RETURN jsonb_build_object('success', true, 'updated_at', now());
END $$;

-- 6. Operación api.advance_shipment (Cambiar estado: llegar, recoger, entregar, cancelar) -------

CREATE OR REPLACE FUNCTION api.advance_shipment(
  p_shipment uuid,
  p_action text
) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v_actor uuid := internal.actor();
  s app.shipments;
BEGIN
  SELECT * INTO s FROM app.shipments WHERE id = p_shipment FOR UPDATE;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'shipment_not_found';
  END IF;

  IF p_action = 'arrive_origin' THEN
    IF s.driver_id <> v_actor THEN RAISE EXCEPTION 'not_authorized_driver'; END IF;
    IF s.status <> 'accepted' THEN RAISE EXCEPTION 'invalid_status_transition'; END IF;
    UPDATE app.shipments SET status = 'picking_up' WHERE id = s.id RETURNING * INTO s;

  ELSIF p_action = 'picked_up' THEN
    IF s.driver_id <> v_actor THEN RAISE EXCEPTION 'not_authorized_driver'; END IF;
    IF s.status NOT IN ('accepted', 'picking_up') THEN RAISE EXCEPTION 'invalid_status_transition'; END IF;
    UPDATE app.shipments SET status = 'in_transit', picked_up_at = now() WHERE id = s.id RETURNING * INTO s;

  ELSIF p_action = 'delivered' THEN
    IF s.driver_id <> v_actor THEN RAISE EXCEPTION 'not_authorized_driver'; END IF;
    IF s.status <> 'in_transit' THEN RAISE EXCEPTION 'invalid_status_transition'; END IF;
    UPDATE app.shipments SET status = 'delivered', delivered_at = now() WHERE id = s.id RETURNING * INTO s;

  ELSIF p_action = 'cancel' THEN
    IF s.sender_id <> v_actor AND s.driver_id <> v_actor THEN RAISE EXCEPTION 'not_authorized'; END IF;
    IF s.status = 'delivered' THEN RAISE EXCEPTION 'already_delivered'; END IF;
    UPDATE app.shipments SET status = 'cancelled', cancelled_at = now() WHERE id = s.id RETURNING * INTO s;

  ELSE
    RAISE EXCEPTION 'unknown_action';
  END IF;

  PERFORM internal.audit('shipment.advanced', 'shipment', s.id, jsonb_build_object('status', s.status, 'action', p_action));

  RETURN jsonb_build_object(
    'id', s.id,
    'status', s.status,
    'action', p_action,
    'picked_up_at', s.picked_up_at,
    'delivered_at', s.delivered_at,
    'cancelled_at', s.cancelled_at
  );
END $$;

-- 7. Operación api.shipment_tracking (Consultar mapa de ruta y rastreo en vivo) -----------------

CREATE OR REPLACE FUNCTION api.shipment_tracking(
  p_shipment uuid
) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v_actor uuid := internal.actor();
  s app.shipments;
  v_sender record;
  v_driver record;
  v_driver_pos record;
BEGIN
  SELECT * INTO s FROM app.shipments WHERE id = p_shipment;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'shipment_not_found';
  END IF;

  -- Datos del remitente
  SELECT id, display_name, avatar_file_id INTO v_sender FROM app.users WHERE id = s.sender_id;

  -- Datos del conductor (si ya fue tomado)
  IF s.driver_id IS NOT NULL THEN
    SELECT id, display_name, avatar_file_id INTO v_driver FROM app.users WHERE id = s.driver_id;
  END IF;

  RETURN jsonb_build_object(
    'id', s.id,
    'status', s.status,
    'shipment_type', s.shipment_type,
    'vehicle_type', s.vehicle_type,
    'title', s.title,
    'description', s.description,
    'origin', jsonb_build_object(
      'address', s.origin_address,
      'latitude', public.ST_Y(s.origin_location::public.geometry),
      'longitude', public.ST_X(s.origin_location::public.geometry),
      'contact_name', s.origin_contact_name,
      'contact_phone', s.origin_contact_phone,
      'instructions', s.origin_instructions
    ),
    'destination', jsonb_build_object(
      'address', s.destination_address,
      'latitude', public.ST_Y(s.destination_location::public.geometry),
      'longitude', public.ST_X(s.destination_location::public.geometry),
      'contact_name', s.destination_contact_name,
      'contact_phone', s.destination_contact_phone,
      'instructions', s.destination_instructions
    ),
    'driver_location', CASE WHEN s.current_driver_location IS NOT NULL THEN jsonb_build_object(
      'latitude', public.ST_Y(s.current_driver_location::public.geometry),
      'longitude', public.ST_X(s.current_driver_location::public.geometry),
      'updated_at', s.last_position_updated_at
    ) ELSE NULL END,
    'distance_km', s.distance_km,
    'fare_minor', s.fare_minor,
    'currency', s.currency,
    'created_at', s.created_at,
    'accepted_at', s.accepted_at,
    'picked_up_at', s.picked_up_at,
    'delivered_at', s.delivered_at,
    'cancelled_at', s.cancelled_at,
    'is_sender', (s.sender_id = v_actor),
    'is_driver', (s.driver_id IS NOT NULL AND s.driver_id = v_actor),
    'can_take', (s.status = 'requested' AND s.driver_id IS NULL AND s.sender_id <> v_actor),
    'sender', jsonb_build_object(
      'id', v_sender.id,
      'name', v_sender.display_name,
      'avatar_file_id', v_sender.avatar_file_id
    ),
    'driver', CASE WHEN v_driver.id IS NOT NULL THEN jsonb_build_object(
      'id', v_driver.id,
      'name', v_driver.display_name,
      'avatar_file_id', v_driver.avatar_file_id
    ) ELSE NULL END
  );
END $$;

-- 8. Operación api.my_shipments (Historial y activos del usuario) ------------------------------

CREATE OR REPLACE FUNCTION api.my_shipments(
  p_role text DEFAULT 'all'
) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v_actor uuid := internal.actor();
  v_res jsonb;
BEGIN
  SELECT coalesce(jsonb_agg(item), '[]'::jsonb) INTO v_res FROM (
    SELECT jsonb_build_object(
      'id', s.id,
      'status', s.status,
      'shipment_type', s.shipment_type,
      'vehicle_type', s.vehicle_type,
      'title', s.title,
      'origin_address', s.origin_address,
      'destination_address', s.destination_address,
      'distance_km', s.distance_km,
      'fare_minor', s.fare_minor,
      'currency', s.currency,
      'created_at', s.created_at,
      'accepted_at', s.accepted_at,
      'delivered_at', s.delivered_at,
      'is_sender', (s.sender_id = v_actor),
      'is_driver', (s.driver_id IS NOT NULL AND s.driver_id = v_actor)
    ) AS item
    FROM app.shipments s
    WHERE (
      (p_role = 'sender' AND s.sender_id = v_actor) OR
      (p_role = 'driver' AND s.driver_id = v_actor) OR
      (p_role = 'all' AND (s.sender_id = v_actor OR s.driver_id = v_actor))
    )
    ORDER BY s.created_at DESC
    LIMIT 100
  ) q;

  RETURN v_res;
END $$;

-- Otorgar permisos a chamba_api
REVOKE ALL ON FUNCTION api.create_shipment(text, text, text, text, double precision, double precision, text, double precision, double precision, bigint, text, text, text, text, text, text, text, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.create_shipment(text, text, text, text, double precision, double precision, text, double precision, double precision, bigint, text, text, text, text, text, text, text, text) TO chamba_api;

REVOKE ALL ON FUNCTION api.available_shipments(double precision, double precision, double precision, text, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.available_shipments(double precision, double precision, double precision, text, text) TO chamba_api;

REVOKE ALL ON FUNCTION api.take_shipment(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.take_shipment(uuid) TO chamba_api;

REVOKE ALL ON FUNCTION api.update_shipment_location(uuid, double precision, double precision, integer, double precision, double precision) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.update_shipment_location(uuid, double precision, double precision, integer, double precision, double precision) TO chamba_api;

REVOKE ALL ON FUNCTION api.advance_shipment(uuid, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.advance_shipment(uuid, text) TO chamba_api;

REVOKE ALL ON FUNCTION api.shipment_tracking(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.shipment_tracking(uuid) TO chamba_api;

REVOKE ALL ON FUNCTION api.my_shipments(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.my_shipments(text) TO chamba_api;
