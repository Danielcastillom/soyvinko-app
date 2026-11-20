ALTER TABLE app.contracts ADD COLUMN work_requirements text NOT NULL DEFAULT '';
UPDATE app.contracts c SET work_requirements=r.description FROM app.service_requests r WHERE r.id=c.request_id;

CREATE FUNCTION api.set_offering_delivery(p_offering uuid,p_mode text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  UPDATE app.offerings SET delivery_mode=p_mode WHERE id=p_offering AND professional_id=internal.actor();
  IF NOT FOUND THEN RAISE EXCEPTION 'offering_not_owned' USING ERRCODE='42501'; END IF;
END $$;

CREATE FUNCTION api.create_remote_request(p_category uuid,p_mode text,p_target uuid,p_title text,p_requirements text,
  p_currency text,p_offered_price bigint,p_expires_at timestamptz,p_key uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); r app.service_requests; v_id uuid;
BEGIN
  PERFORM 1 FROM app.users WHERE id=v FOR UPDATE;
  SELECT * INTO r FROM app.service_requests WHERE customer_id=v AND idempotency_key=p_key;
  IF FOUND THEN
    IF (r.category_id,r.mode,r.target_professional_id,r.title,r.description,r.currency,r.offered_price_minor,r.expires_at,r.delivery_mode)
      IS DISTINCT FROM(p_category,p_mode,p_target,p_title,p_requirements,p_currency::char(3),p_offered_price,p_expires_at,'remote'::text) THEN
      RAISE EXCEPTION 'idempotency_conflict'; END IF;
    RETURN r.id;
  END IF;
  IF p_offered_price IS NULL OR p_offered_price<=0 THEN RAISE EXCEPTION 'positive_offer_required'; END IF;
  IF NOT EXISTS(SELECT 1 FROM app.categories WHERE id=p_category AND active) THEN RAISE EXCEPTION 'invalid_category'; END IF;
  INSERT INTO app.service_requests(customer_id,category_id,mode,target_professional_id,title,description,currency,
    budget_min_minor,budget_max_minor,search_radius_m,expires_at,idempotency_key,delivery_mode,requires_platform_payment,offered_price_minor)
  VALUES(v,p_category,p_mode,p_target,p_title,p_requirements,p_currency,p_offered_price,p_offered_price,NULL,p_expires_at,p_key,'remote',true,p_offered_price)
  RETURNING id INTO v_id;
  RETURN v_id;
END $$;

CREATE FUNCTION api.protect_service_request(p_request uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  UPDATE app.service_requests SET requires_platform_payment=true
  WHERE id=p_request AND customer_id=internal.actor() AND status='draft';
  IF NOT FOUND THEN RAISE EXCEPTION 'request_not_editable'; END IF;
END $$;

CREATE OR REPLACE FUNCTION internal.eligible(p_professional uuid,p_request uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT EXISTS(SELECT 1 FROM app.service_requests r
    JOIN app.categories cat ON cat.id=r.category_id AND cat.active
    JOIN app.professionals p ON p.user_id=p_professional AND p.status='active' AND p.accepts_requests
    JOIN app.users u ON u.id=p.user_id AND u.status='active'
    WHERE r.id=p_request AND r.customer_id<>p_professional AND (r.mode='broadcast' OR r.target_professional_id=p_professional)
      AND EXISTS(SELECT 1 FROM app.offerings o WHERE o.professional_id=p_professional AND o.category_id=r.category_id
        AND o.active AND o.delivery_mode=r.delivery_mode)
      AND (r.delivery_mode='remote' OR EXISTS(SELECT 1 FROM app.request_locations l
        JOIN app.coverage_areas ca ON ca.professional_id=p_professional AND ca.category_id=r.category_id AND ca.active
        JOIN app.branches b ON b.id=ca.branch_id AND b.active
        WHERE l.request_id=r.id AND public.ST_DWithin(b.location,l.location,least(ca.radius_m,r.search_radius_m))
          AND (ca.polygon IS NULL OR public.ST_Covers(ca.polygon,l.location)))));
$$;

CREATE FUNCTION internal.contract_money_guard() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE r app.service_requests; e app.service_escrows;
BEGIN
  IF TG_OP='INSERT' THEN
    SELECT * INTO r FROM app.service_requests WHERE id=NEW.request_id;
    NEW.delivery_mode:=r.delivery_mode; NEW.work_requirements:=r.description;
    IF r.delivery_mode='remote' OR r.requires_platform_payment OR EXISTS(SELECT 1 FROM app.users
      WHERE id IN(NEW.customer_id,NEW.professional_id) AND requires_platform_payment) THEN
      NEW.payment_protection:='guaranteed'; NEW.payment_method:='platform_wallet';
    END IF;
    IF NEW.proposal_id IS NULL AND r.offered_price_minor IS NOT NULL THEN NEW.agreed_price_minor:=r.offered_price_minor; END IF;
    RETURN NEW;
  END IF;
  IF (NEW.customer_id,NEW.professional_id,NEW.request_id,NEW.delivery_mode,NEW.payment_protection)
    IS DISTINCT FROM(OLD.customer_id,OLD.professional_id,OLD.request_id,OLD.delivery_mode,OLD.payment_protection) THEN
    RAISE EXCEPTION 'contract_identity_and_payment_mode_immutable'; END IF;
  IF OLD.payment_protection='guaranteed' THEN
    SELECT * INTO e FROM app.service_escrows WHERE contract_id=OLD.id;
    IF e.status<>'unfunded' AND (NEW.agreed_price_minor,NEW.currency,NEW.terms_version,NEW.payment_terms,NEW.payment_method,NEW.work_requirements)
      IS DISTINCT FROM(OLD.agreed_price_minor,OLD.currency,OLD.terms_version,OLD.payment_terms,OLD.payment_method,OLD.work_requirements) THEN
      RAISE EXCEPTION 'funded_terms_immutable'; END IF;
    IF NEW.payment_method IS DISTINCT FROM 'platform_wallet' THEN RAISE EXCEPTION 'platform_payment_required'; END IF;
    IF NEW.status IS DISTINCT FROM OLD.status AND NEW.status IN('en_route','arrived','in_progress','awaiting_confirmation') THEN
      IF e.status IS DISTINCT FROM 'funded' OR EXISTS(SELECT 1 FROM app.money_wallets w WHERE w.user_id=OLD.customer_id AND w.currency=OLD.currency AND w.status<>'active') THEN
        RAISE EXCEPTION 'funds_not_guaranteed'; END IF;
    END IF;
    IF NEW.status='completed' AND e.status IS DISTINCT FROM 'released' THEN RAISE EXCEPTION 'customer_release_required'; END IF;
    IF NEW.status='cancelled' AND e.status IN('funded','disputed') THEN RAISE EXCEPTION 'escrow_refund_required'; END IF;
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER contract_money_guard BEFORE INSERT OR UPDATE ON app.contracts FOR EACH ROW EXECUTE FUNCTION internal.contract_money_guard();

CREATE FUNCTION internal.sync_contract_escrow() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  IF NEW.payment_protection='guaranteed' THEN
    IF TG_OP='INSERT' THEN INSERT INTO app.service_escrows(contract_id) VALUES(NEW.id);
    ELSIF NEW.status='disputed' THEN UPDATE app.service_escrows SET status='disputed' WHERE contract_id=NEW.id AND status='funded'; END IF;
  END IF;
  RETURN NULL;
END $$;
CREATE TRIGGER contract_escrow AFTER INSERT OR UPDATE ON app.contracts FOR EACH ROW EXECUTE FUNCTION internal.sync_contract_escrow();

CREATE FUNCTION internal.prevent_remote_location() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  IF (TG_TABLE_NAME='request_locations' AND EXISTS(SELECT 1 FROM app.service_requests WHERE id=NEW.request_id AND delivery_mode='remote')) THEN
    RAISE EXCEPTION 'remote_service_has_no_location'; END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER onsite_location_only BEFORE INSERT OR UPDATE ON app.request_locations FOR EACH ROW EXECUTE FUNCTION internal.prevent_remote_location();

CREATE FUNCTION internal.prevent_remote_share() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  IF EXISTS(SELECT 1 FROM app.contracts WHERE id=NEW.contract_id AND delivery_mode='remote') THEN RAISE EXCEPTION 'remote_service_has_no_location'; END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER onsite_share_only BEFORE INSERT ON app.location_shares FOR EACH ROW EXECUTE FUNCTION internal.prevent_remote_share();

CREATE FUNCTION internal.external_report_guard() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  IF EXISTS(SELECT 1 FROM app.contracts WHERE id=NEW.contract_id AND payment_protection='guaranteed') THEN RAISE EXCEPTION 'platform_payment_required'; END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER external_report_only BEFORE INSERT ON app.external_payment_reports FOR EACH ROW EXECUTE FUNCTION internal.external_report_guard();

-- Se conserva el comportamiento de los servicios presenciales con pago externo.
ALTER FUNCTION api.advance_contract(uuid,text) SET SCHEMA internal;
ALTER FUNCTION internal.advance_contract(uuid,text) RENAME TO advance_onsite_contract;
REVOKE ALL ON FUNCTION internal.advance_onsite_contract(uuid,text) FROM chamba_api;
CREATE FUNCTION api.advance_contract(p_contract uuid,p_action text) RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts; v uuid:=internal.actor();
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract AND v IN(customer_id,professional_id) FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'contract_access_denied' USING ERRCODE='42501'; END IF;
  IF c.payment_protection='guaranteed' AND p_action='finish' THEN RAISE EXCEPTION 'use_delivery_and_customer_release'; END IF;
  IF c.delivery_mode='onsite' THEN RETURN internal.advance_onsite_contract(p_contract,p_action); END IF;
  IF v<>c.professional_id THEN RAISE EXCEPTION 'only_professional_can_start' USING ERRCODE='42501'; END IF;
  IF p_action='start' AND c.status='in_progress' THEN RETURN c.status; END IF;
  IF p_action IS DISTINCT FROM 'start' OR c.status<>'accepted' THEN RAISE EXCEPTION 'invalid_remote_transition'; END IF;
  UPDATE app.contracts SET status='in_progress',started_at=now() WHERE id=c.id;
  INSERT INTO app.contract_events(contract_id,actor_id,event_type,from_status,to_status) VALUES(c.id,v,'remote_start',c.status,'in_progress');
  RETURN 'in_progress';
END $$;

ALTER FUNCTION api.professional_profile(uuid) SET SCHEMA internal;
ALTER FUNCTION internal.professional_profile(uuid) RENAME TO base_professional_profile;
REVOKE ALL ON FUNCTION internal.base_professional_profile(uuid) FROM chamba_api;
CREATE FUNCTION api.professional_profile(p_professional uuid) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE result jsonb;
BEGIN
  result:=internal.base_professional_profile(p_professional);
  IF result IS NULL THEN RETURN NULL; END IF;
  RETURN result||jsonb_build_object('requires_platform_payment',(SELECT requires_platform_payment FROM app.users WHERE id=p_professional),
    'offerings',coalesce((SELECT jsonb_agg(jsonb_build_object('id',o.id,'category_id',o.category_id,'title',o.title,'description',o.description,
      'delivery_mode',o.delivery_mode,'pricing_type',o.pricing_type,'price_min_minor',o.price_min_minor,'price_max_minor',o.price_max_minor,'currency',o.currency))
    FROM app.offerings o WHERE o.professional_id=p_professional AND o.active),'[]'::jsonb));
END $$;

CREATE FUNCTION api.search_remote_professionals(p_category uuid,p_currency text DEFAULT NULL,p_max_price bigint DEFAULT NULL,
  p_verified_only boolean DEFAULT false,p_limit integer DEFAULT 30,p_offset integer DEFAULT 0) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor();
BEGIN
  IF p_limit IS NULL OR p_limit NOT BETWEEN 1 AND 100 OR p_offset IS NULL OR p_offset<0
    OR (p_max_price IS NOT NULL AND (p_currency IS NULL OR p_max_price<0)) THEN RAISE EXCEPTION 'invalid_search_filters'; END IF;
  RETURN QUERY WITH candidates AS (
    SELECT p.user_id,
      EXISTS(SELECT 1 FROM app.kyc_cases k WHERE k.user_id=p.user_id AND k.status='approved' AND k.expires_at>now()) AS verified,
      coalesce((SELECT avg(r.rating) FROM app.reviews r WHERE r.subject_id=p.user_id AND r.moderation_status='published'),0) AS rating,
      coalesce((SELECT max(e.weight) FROM app.entitlements e WHERE e.professional_id=p.user_id AND e.kind='positioning' AND e.revoked_at IS NULL
        AND e.starts_at<=now() AND e.ends_at>now()),0) AS boost
    FROM app.professionals p JOIN app.users u ON u.id=p.user_id AND u.status='active'
    WHERE p.status='active' AND p.accepts_requests AND p.user_id<>v
      AND EXISTS(SELECT 1 FROM app.offerings o JOIN app.categories cat ON cat.id=o.category_id AND cat.active
        WHERE o.professional_id=p.user_id AND o.active AND o.delivery_mode='remote' AND o.category_id=p_category
        AND (p_currency IS NULL OR o.currency=p_currency) AND (p_max_price IS NULL OR o.price_min_minor<=p_max_price))
  ) SELECT api.professional_profile(c.user_id)||jsonb_build_object('sponsored',c.boost>0) FROM candidates c
    WHERE NOT p_verified_only OR c.verified ORDER BY c.verified DESC,c.boost DESC,c.rating DESC,c.user_id LIMIT p_limit OFFSET p_offset;
END $$;

CREATE FUNCTION api.service_feed(p_delivery_mode text,p_limit integer DEFAULT 30,p_offset integer DEFAULT 0) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor();
BEGIN
  IF p_delivery_mode IS NULL OR p_delivery_mode NOT IN('onsite','remote') OR p_limit IS NULL OR p_limit NOT BETWEEN 1 AND 100
    OR p_offset IS NULL OR p_offset<0 THEN RAISE EXCEPTION 'invalid_feed_filters'; END IF;
  RETURN QUERY SELECT jsonb_build_object('request_id',r.id,'category_id',r.category_id,'title',r.title,'description',r.description,
    'delivery_mode',r.delivery_mode,'mode',r.mode,'currency',r.currency,'offered_price_minor',r.offered_price_minor,
    'budget_min_minor',r.budget_min_minor,'budget_max_minor',r.budget_max_minor,'expires_at',r.expires_at,
    'requires_platform_payment',r.requires_platform_payment OR EXISTS(SELECT 1 FROM app.users u WHERE u.id=r.customer_id AND u.requires_platform_payment),
    'attachments',coalesce((SELECT jsonb_agg(jsonb_build_object('file_id',f.id,'mime_type',f.mime_type)) FROM app.request_attachments ra
      JOIN app.files f ON f.id=ra.file_id WHERE ra.request_id=r.id AND f.moderation_status='approved' AND f.exif_removed AND f.deleted_at IS NULL),'[]'::jsonb))
  FROM app.service_requests r WHERE r.delivery_mode=p_delivery_mode AND r.status='open' AND r.moderation_status='approved'
    AND r.expires_at>now() AND internal.eligible(v,r.id) ORDER BY r.created_at DESC,r.id LIMIT p_limit OFFSET p_offset;
END $$;

GRANT EXECUTE ON FUNCTION api.set_offering_delivery(uuid,text),api.create_remote_request(uuid,text,uuid,text,text,text,bigint,timestamptz,uuid),
  api.protect_service_request(uuid),api.advance_contract(uuid,text),api.professional_profile(uuid),
  api.search_remote_professionals(uuid,text,bigint,boolean,integer,integer),api.service_feed(text,integer,integer) TO chamba_api;


-- La búsqueda por distancia corresponde solo a servicios presenciales.
CREATE OR REPLACE FUNCTION api.search_professionals(p_address uuid,p_category uuid,p_radius_m integer DEFAULT 10000,
  p_sort text DEFAULT 'distance',p_verified_only boolean DEFAULT false,p_currency text DEFAULT NULL,
  p_max_price bigint DEFAULT NULL,p_min_rating numeric DEFAULT 0,p_limit integer DEFAULT 30,p_offset integer DEFAULT 0)
RETURNS TABLE(professional_id uuid,profile jsonb,distance_m bigint,verified boolean,sponsored boolean)
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); a app.addresses;
BEGIN
  SELECT * INTO a FROM app.addresses WHERE id=p_address AND user_id=v AND archived_at IS NULL;
  IF NOT FOUND THEN RAISE EXCEPTION 'address_not_owned' USING ERRCODE='42501'; END IF;
  IF p_sort IS NULL OR p_sort NOT IN('distance','recommended') OR p_radius_m IS NULL OR p_radius_m NOT BETWEEN 100 AND 200000
    OR p_limit IS NULL OR p_limit NOT BETWEEN 1 AND 100 OR p_offset IS NULL OR p_offset<0 OR p_min_rating NOT BETWEEN 0 AND 5
    OR (p_max_price IS NOT NULL AND (p_max_price<0 OR p_currency IS NULL)) THEN RAISE EXCEPTION 'invalid_search_filters'; END IF;
  RETURN QUERY
  WITH candidates AS (
    SELECT p.user_id,min(public.ST_Distance(b.location,a.location)) AS meters,
      EXISTS(SELECT 1 FROM app.kyc_cases k WHERE k.user_id=p.user_id AND k.status='approved' AND k.expires_at>now()) AS is_verified,
      coalesce((SELECT max(e.weight) FROM app.entitlements e WHERE e.professional_id=p.user_id AND e.kind='positioning'
        AND e.revoked_at IS NULL AND e.starts_at<=now() AND e.ends_at>now()),0) AS boost,
      coalesce((SELECT avg(r.rating) FROM app.reviews r WHERE r.subject_id=p.user_id AND r.moderation_status='published'),0) AS rating
    FROM app.professionals p JOIN app.users u ON u.id=p.user_id AND u.status='active'
    JOIN app.coverage_areas ca ON ca.professional_id=p.user_id AND ca.category_id=p_category AND ca.active
    JOIN app.categories cat ON cat.id=ca.category_id AND cat.active
    JOIN app.branches b ON b.id=ca.branch_id AND b.active
    WHERE p.status='active' AND p.accepts_requests AND p.user_id<>v
      AND public.ST_DWithin(b.location,a.location,p_radius_m)
      AND public.ST_DWithin(b.location,a.location,ca.radius_m)
      AND (ca.polygon IS NULL OR public.ST_Covers(ca.polygon,a.location))
      AND EXISTS(SELECT 1 FROM app.offerings o WHERE o.professional_id=p.user_id AND o.category_id=p_category AND o.active AND o.delivery_mode='onsite'
        AND (p_currency IS NULL OR o.currency=p_currency) AND (p_max_price IS NULL OR o.price_min_minor<=p_max_price))
    GROUP BY p.user_id
  )
  SELECT c.user_id,api.professional_profile(c.user_id),round(c.meters)::bigint,c.is_verified,c.boost>0
  FROM candidates c WHERE (NOT p_verified_only OR c.is_verified) AND c.rating>=p_min_rating
  ORDER BY CASE WHEN p_sort='distance' THEN c.meters END ASC,
    CASE WHEN p_sort='recommended' THEN c.is_verified::integer END DESC,
    CASE WHEN p_sort='recommended' THEN c.boost END DESC,
    CASE WHEN p_sort='recommended' THEN c.rating END DESC,
    c.meters ASC,c.user_id ASC LIMIT p_limit OFFSET p_offset;
END $$;
