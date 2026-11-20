-- Migration 046: Wallet payment system for marketplace orders and shipments
-- Allows paying orders directly with money wallet balance (VES / USD)

ALTER TABLE app.orders DROP CONSTRAINT IF EXISTS orders_payment_status_check;
ALTER TABLE app.orders ADD CONSTRAINT orders_payment_status_check CHECK (
  payment_status = ANY (ARRAY['not_reported'::text, 'buyer_reported'::text, 'seller_confirmed'::text, 'paid'::text, 'refunded'::text, 'disputed'::text])
);

CREATE OR REPLACE FUNCTION api.pay_order_with_wallet(p_order uuid, p_key uuid DEFAULT NULL)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public', 'pg_temp'
AS $function$
DECLARE
  v uuid := internal.actor();
  o app.orders;
  s app.stores;
  w_buyer uuid;
  w_seller uuid;
  acc_buyer uuid;
  acc_seller uuid;
  v_balance bigint;
  tx uuid;
  v_key text;
BEGIN
  IF p_order IS NULL THEN RAISE EXCEPTION 'order_id_required'; END IF;
  
  SELECT * INTO o FROM app.orders WHERE id = p_order FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'order_not_found'; END IF;
  
  IF o.customer_id <> v THEN
    RAISE EXCEPTION 'only_buyer_can_pay' USING ERRCODE = '42501';
  END IF;
  
  IF o.payment_status = 'paid' THEN
    RETURN jsonb_build_object(
      'id', o.id,
      'status', o.status,
      'payment_status', o.payment_status,
      'payment_method', o.payment_method,
      'message', 'El pedido ya se encuentra pagado.'
    );
  END IF;
  
  IF o.status IN ('cancelled', 'disputed') THEN
    RAISE EXCEPTION 'order_cannot_be_paid_in_current_status';
  END IF;

  SELECT * INTO s FROM app.stores WHERE id = o.store_id;
  IF NOT FOUND THEN RAISE EXCEPTION 'store_not_found'; END IF;

  -- Wallets
  w_buyer := internal.money_wallet(o.customer_id, o.currency);
  w_seller := internal.money_wallet(s.professional_id, o.currency);

  PERFORM 1 FROM app.money_wallets WHERE id = w_buyer AND status = 'active' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'buyer_wallet_frozen'; END IF;

  acc_buyer := internal.money_account('available', o.currency, w_buyer);
  acc_seller := internal.money_account('available', o.currency, w_seller);

  SELECT balance_minor INTO v_balance FROM app.money_accounts WHERE id = acc_buyer;
  IF coalesce(v_balance, 0) < o.total_minor THEN
    RAISE EXCEPTION 'insufficient_wallet_balance';
  END IF;

  v_key := 'order:wallet_pay:' || coalesce(p_key::text, o.id::text);

  tx := internal.post_money(
    'order_payment',
    o.currency,
    v_key,
    o.id,
    jsonb_build_array(
      jsonb_build_object('account_id', acc_buyer, 'delta', -o.total_minor),
      jsonb_build_object('account_id', acc_seller, 'delta', o.total_minor)
    ),
    jsonb_build_object(
      'order_id', o.id,
      'buyer_id', o.customer_id,
      'seller_id', s.professional_id,
      'total_minor', o.total_minor,
      'payment_method', 'wallet'
    )
  );

  UPDATE app.orders
  SET payment_method = 'wallet',
      payment_status = 'paid'
  WHERE id = o.id;

  INSERT INTO app.order_events(order_id, actor_id, event_type, metadata)
  VALUES(o.id, v, 'wallet_payment', jsonb_build_object('amount_minor', o.total_minor, 'currency', o.currency, 'transaction_id', tx));

  INSERT INTO app.outbox(topic, aggregate_id, recipient_id, deduplication_key)
  VALUES('order.paid', o.id, s.professional_id, 'order_paid:' || o.id);

  PERFORM internal.audit('order.paid_with_wallet', 'order', o.id, jsonb_build_object('transaction_id', tx, 'amount_minor', o.total_minor));

  RETURN jsonb_build_object(
    'id', o.id,
    'status', o.status,
    'payment_status', 'paid',
    'payment_method', 'wallet',
    'transaction_id', tx,
    'total_minor', o.total_minor,
    'currency', o.currency
  );
END $function$;

GRANT EXECUTE ON FUNCTION api.pay_order_with_wallet(uuid, uuid) TO chamba_api;

CREATE OR REPLACE FUNCTION api.advance_order(p_order uuid, p_action text)
 RETURNS text
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'pg_catalog', 'public', 'pg_temp'
AS $function$
DECLARE
  v uuid := internal.actor();
  o app.orders;
  v_seller uuid;
  v_status text;
  item record;
  w_buyer uuid;
  w_seller uuid;
  acc_buyer uuid;
  acc_seller uuid;
  tx uuid;
BEGIN
  SELECT * INTO o FROM app.orders WHERE id = p_order FOR UPDATE;
  SELECT professional_id INTO v_seller FROM app.stores WHERE id = o.store_id;
  IF o.id IS NULL OR v NOT IN(o.customer_id, v_seller) THEN
    RAISE EXCEPTION 'order_access_denied' USING ERRCODE = '42501';
  END IF;

  IF p_action = 'cancel' THEN
    IF o.status = 'cancelled' THEN RETURN o.status; END IF;
    IF o.status NOT IN('pending', 'accepted') THEN RAISE EXCEPTION 'open_dispute_instead'; END IF;

    -- Return items to stock
    FOR item IN SELECT * FROM app.order_items WHERE order_id = o.id ORDER BY product_id LOOP
      UPDATE app.products SET stock = stock + item.quantity, updated_at = now() WHERE id = item.product_id;
      INSERT INTO app.inventory_movements(product_id, order_id, delta, reason, actor_id)
      VALUES(item.product_id, o.id, item.quantity, 'order_cancelled', v);
    END LOOP;

    -- If paid via wallet, refund funds to buyer
    IF o.payment_method = 'wallet' AND o.payment_status = 'paid' THEN
      w_buyer := internal.money_wallet(o.customer_id, o.currency);
      w_seller := internal.money_wallet(v_seller, o.currency);
      acc_buyer := internal.money_account('available', o.currency, w_buyer);
      acc_seller := internal.money_account('available', o.currency, w_seller);

      tx := internal.post_money(
        'order_refund',
        o.currency,
        'order:wallet_refund:' || o.id,
        o.id,
        jsonb_build_array(
          jsonb_build_object('account_id', acc_seller, 'delta', -o.total_minor),
          jsonb_build_object('account_id', acc_buyer, 'delta', o.total_minor)
        ),
        jsonb_build_object('order_id', o.id, 'reason', 'order_cancelled', 'refund_to', o.customer_id)
      );

      UPDATE app.orders SET payment_status = 'refunded' WHERE id = o.id;
      INSERT INTO app.order_events(order_id, actor_id, event_type, metadata)
      VALUES(o.id, v, 'wallet_refund', jsonb_build_object('amount_minor', o.total_minor, 'transaction_id', tx));
    END IF;

    v_status := 'cancelled';
  ELSIF p_action = 'accept' AND o.status = 'pending' AND v = v_seller THEN
    v_status := 'accepted';
  ELSIF p_action = 'dispatch' AND o.status = 'accepted' AND v = v_seller THEN
    v_status := 'dispatched';
  ELSIF p_action = 'receive' AND o.status IN('accepted', 'dispatched') AND v = o.customer_id THEN
    v_status := 'delivered';
  ELSE
    RAISE EXCEPTION 'invalid_order_transition';
  END IF;

  UPDATE app.orders SET status = v_status WHERE id = o.id;
  INSERT INTO app.order_events(order_id, actor_id, event_type, metadata)
  VALUES(o.id, v, p_action, jsonb_build_object('from', o.status, 'to', v_status));
  PERFORM internal.audit('order.' || p_action, 'order', o.id);

  RETURN v_status;
END $function$;

ALTER TABLE app.shipments ADD COLUMN IF NOT EXISTS payment_method text DEFAULT 'cash';
ALTER TABLE app.shipments ADD COLUMN IF NOT EXISTS payment_status text DEFAULT 'pending';

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
  p_estimated_fare_minor bigint DEFAULT NULL,
  p_payment_method text DEFAULT 'cash'
) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v_actor uuid := internal.actor();
  v_origin public.geography(Point, 4326);
  v_destination public.geography(Point, 4326);
  v_dist_km numeric(8,2);
  v_shipment app.shipments;
  v_method text := coalesce(p_payment_method, 'cash');
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
    cargo_image_file_id, passenger_count, is_bargain, estimated_fare_minor, payment_method, payment_status
  ) VALUES (
    v_actor, p_shipment_type, p_vehicle_type, 'requested',
    trim(p_title), trim(p_description),
    trim(p_origin_address), v_origin, trim(p_origin_contact_name), trim(p_origin_contact_phone), trim(p_origin_instructions),
    trim(p_destination_address), v_destination, trim(p_destination_contact_name), trim(p_destination_contact_phone), trim(p_destination_instructions),
    v_dist_km, p_fare_minor, p_currency,
    p_cargo_image_file_id, coalesce(p_passenger_count, 1), coalesce(p_is_bargain, false), p_estimated_fare_minor,
    v_method, 'pending'
  ) RETURNING * INTO v_shipment;

  PERFORM internal.audit('shipment.created', 'shipment', v_shipment.id, jsonb_build_object(
    'type', v_shipment.shipment_type,
    'vehicle_type', v_shipment.vehicle_type,
    'fare_minor', v_shipment.fare_minor,
    'currency', v_shipment.currency,
    'is_bargain', v_shipment.is_bargain,
    'payment_method', v_method
  ));

  RETURN jsonb_build_object(
    'id', v_shipment.id,
    'status', v_shipment.status,
    'title', v_shipment.title,
    'distance_km', v_shipment.distance_km,
    'fare_minor', v_shipment.fare_minor,
    'currency', v_shipment.currency,
    'is_bargain', v_shipment.is_bargain,
    'payment_method', v_shipment.payment_method,
    'created_at', v_shipment.created_at
  );
END $$;

GRANT EXECUTE ON FUNCTION api.create_shipment(text, text, text, text, double precision, double precision, text, double precision, double precision, bigint, text, text, text, text, text, text, text, text, uuid, integer, boolean, bigint, text) TO chamba_api;

