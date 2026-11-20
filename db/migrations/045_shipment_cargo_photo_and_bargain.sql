-- 045_shipment_cargo_photo_and_bargain.sql
-- Ampliación de envíos: foto de mercancía, cantidad de pasajeros, regateo y tarifa estimada.

ALTER TABLE app.shipments
  ADD COLUMN IF NOT EXISTS cargo_image_file_id uuid REFERENCES app.files(id),
  ADD COLUMN IF NOT EXISTS passenger_count integer NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS is_bargain boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS estimated_fare_minor bigint;

-- 1. Actualizar api.create_shipment con parámetros opcionales
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
  p_currency text DEFAULT 'VES',
  p_cargo_image_file_id uuid DEFAULT NULL,
  p_passenger_count integer DEFAULT 1,
  p_is_bargain boolean DEFAULT false,
  p_estimated_fare_minor bigint DEFAULT NULL
) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v_actor uuid := internal.actor();
  v_origin public.geography(Point, 4326);
  v_destination public.geography(Point, 4326);
  v_dist_km numeric(8,2);
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
    distance_km, fare_minor, currency,
    cargo_image_file_id, passenger_count, is_bargain, estimated_fare_minor
  ) VALUES (
    v_actor, p_shipment_type, p_vehicle_type, 'requested',
    trim(p_title), trim(p_description),
    trim(p_origin_address), v_origin, trim(p_origin_contact_name), trim(p_origin_contact_phone), trim(p_origin_instructions),
    trim(p_destination_address), v_destination, trim(p_destination_contact_name), trim(p_destination_contact_phone), trim(p_destination_instructions),
    v_dist_km, p_fare_minor, p_currency,
    p_cargo_image_file_id, coalesce(p_passenger_count, 1), coalesce(p_is_bargain, false), p_estimated_fare_minor
  ) RETURNING * INTO v_shipment;

  PERFORM internal.audit('shipment.created', 'shipment', v_shipment.id, jsonb_build_object(
    'type', v_shipment.shipment_type,
    'vehicle_type', v_shipment.vehicle_type,
    'fare_minor', v_shipment.fare_minor,
    'currency', v_shipment.currency,
    'is_bargain', v_shipment.is_bargain
  ));

  RETURN jsonb_build_object(
    'id', v_shipment.id,
    'status', v_shipment.status,
    'title', v_shipment.title,
    'distance_km', v_shipment.distance_km,
    'fare_minor', v_shipment.fare_minor,
    'currency', v_shipment.currency,
    'is_bargain', v_shipment.is_bargain,
    'cargo_image_file_id', v_shipment.cargo_image_file_id,
    'passenger_count', v_shipment.passenger_count,
    'created_at', v_shipment.created_at
  );
END $$;

REVOKE ALL ON FUNCTION api.create_shipment(text, text, text, text, double precision, double precision, text, double precision, double precision, bigint, text, text, text, text, text, text, text, text, uuid, integer, boolean, bigint) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.create_shipment(text, text, text, text, double precision, double precision, text, double precision, double precision, bigint, text, text, text, text, text, text, text, text, uuid, integer, boolean, bigint) TO chamba_api;

-- 2. Actualizar api.available_shipments para proyectar foto de carga, pasajeros y regateo
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
      'is_bargain', s.is_bargain,
      'cargo_image_file_id', s.cargo_image_file_id,
      'passenger_count', s.passenger_count,
      'estimated_fare_minor', s.estimated_fare_minor,
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

REVOKE ALL ON FUNCTION api.available_shipments(double precision, double precision, double precision, text, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.available_shipments(double precision, double precision, double precision, text, text) TO chamba_api;

-- 3. Actualizar api.shipment_tracking
CREATE OR REPLACE FUNCTION api.shipment_tracking(
  p_shipment uuid
) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v_actor uuid := internal.actor();
  s app.shipments;
  v_sender record;
  v_driver record;
BEGIN
  SELECT * INTO s FROM app.shipments WHERE id = p_shipment;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'shipment_not_found';
  END IF;

  SELECT id, display_name, avatar_file_id INTO v_sender FROM app.users WHERE id = s.sender_id;
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
    'is_bargain', s.is_bargain,
    'cargo_image_file_id', s.cargo_image_file_id,
    'passenger_count', s.passenger_count,
    'estimated_fare_minor', s.estimated_fare_minor,
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

REVOKE ALL ON FUNCTION api.shipment_tracking(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.shipment_tracking(uuid) TO chamba_api;

-- 4. Actualizar api.my_shipments
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
      'is_bargain', s.is_bargain,
      'cargo_image_file_id', s.cargo_image_file_id,
      'passenger_count', s.passenger_count,
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

REVOKE ALL ON FUNCTION api.my_shipments(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.my_shipments(text) TO chamba_api;
