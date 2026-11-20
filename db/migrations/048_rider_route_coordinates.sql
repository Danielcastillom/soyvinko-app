-- 048_rider_route_coordinates.sql
-- Incluye coordenadas geográficas de origen y destino, contactos y datos del remitente
-- en api.my_shipments para que el rider pueda visualizar la ruta completa en el mapa interactivo.

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
      'description', s.description,
      'origin_address', s.origin_address,
      'origin_lat', public.ST_Y(s.origin_location::public.geometry),
      'origin_lon', public.ST_X(s.origin_location::public.geometry),
      'origin_contact_name', s.origin_contact_name,
      'origin_contact_phone', s.origin_contact_phone,
      'origin_instructions', s.origin_instructions,
      'destination_address', s.destination_address,
      'destination_lat', public.ST_Y(s.destination_location::public.geometry),
      'destination_lon', public.ST_X(s.destination_location::public.geometry),
      'destination_contact_name', s.destination_contact_name,
      'destination_contact_phone', s.destination_contact_phone,
      'destination_instructions', s.destination_instructions,
      'distance_km', s.distance_km,
      'fare_minor', s.fare_minor,
      'currency', s.currency,
      'payment_method', coalesce(s.payment_method, 'wallet'),
      'is_bargain', s.is_bargain,
      'cargo_image_file_id', s.cargo_image_file_id,
      'passenger_count', s.passenger_count,
      'created_at', s.created_at,
      'accepted_at', s.accepted_at,
      'delivered_at', s.delivered_at,
      'is_sender', (s.sender_id = v_actor),
      'is_driver', (s.driver_id IS NOT NULL AND s.driver_id = v_actor),
      'sender_name', u.display_name,
      'sender_avatar_file_id', u.avatar_file_id
    ) AS item
    FROM app.shipments s
    LEFT JOIN app.users u ON u.id = s.sender_id
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

