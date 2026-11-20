-- Two independent currencies. Never relabel or convert historical ledger entries.
CREATE TABLE app.operating_currencies(code text PRIMARY KEY,label text NOT NULL,symbol text NOT NULL);
INSERT INTO app.operating_currencies VALUES('VES','Bolívares','Bs'),('USD','Dólares estadounidenses','$');
ALTER TABLE app.operating_currencies ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.users ALTER COLUMN locale SET DEFAULT 'es-VE', ALTER COLUMN timezone SET DEFAULT 'America/Caracas';
ALTER TABLE app.branches ALTER COLUMN timezone SET DEFAULT 'America/Caracas';
CREATE OR REPLACE FUNCTION internal.money_wallet(p_user uuid,p_currency text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE ident uuid;
BEGIN
  IF p_currency IS NULL OR p_currency !~ '^[A-Z]{3}$' THEN RAISE EXCEPTION 'invalid_currency'; END IF;
  SELECT id INTO ident FROM app.money_wallets WHERE user_id=p_user AND currency=p_currency;
  IF FOUND THEN RETURN ident; END IF; -- Previously created wallets remain readable in their original currency.
  IF p_currency NOT IN ('VES','USD') THEN RAISE EXCEPTION 'unsupported_currency'; END IF;
  INSERT INTO app.money_wallets(user_id,currency) VALUES(p_user,p_currency) ON CONFLICT(user_id,currency) DO NOTHING;
  SELECT id INTO ident FROM app.money_wallets WHERE user_id=p_user AND currency=p_currency;
  RETURN ident;
END $$;
CREATE FUNCTION internal.operating_currency_guard() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  IF NEW.currency NOT IN ('VES','USD') THEN RAISE EXCEPTION 'unsupported_currency'; END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER offering_operating_currency BEFORE INSERT OR UPDATE OF currency ON app.offerings
  FOR EACH ROW EXECUTE FUNCTION internal.operating_currency_guard();
CREATE TRIGGER request_operating_currency BEFORE INSERT OR UPDATE OF currency ON app.service_requests
  FOR EACH ROW EXECUTE FUNCTION internal.operating_currency_guard();
CREATE TRIGGER product_operating_currency BEFORE INSERT OR UPDATE OF currency ON app.products
  FOR EACH ROW EXECUTE FUNCTION internal.operating_currency_guard();
CREATE TRIGGER plan_operating_currency BEFORE INSERT OR UPDATE OF currency ON app.plans
  FOR EACH ROW EXECUTE FUNCTION internal.operating_currency_guard();
CREATE TRIGGER fee_operating_currency BEFORE INSERT OR UPDATE OF currency ON app.withdrawal_fee_policies
  FOR EACH ROW EXECUTE FUNCTION internal.operating_currency_guard();

CREATE FUNCTION internal.venezuela_request_guard() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  IF NEW.country_code IS DISTINCT FROM 'VE' THEN RAISE EXCEPTION 'venezuela_only'; END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER venezuela_new_request_location BEFORE INSERT ON app.request_locations
  FOR EACH ROW EXECUTE FUNCTION internal.venezuela_request_guard();

CREATE OR REPLACE FUNCTION api.my_account() RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT jsonb_build_object('id',u.id,'email',u.email,'name',u.display_name,'status',u.status,
    'roles',(SELECT jsonb_agg(role_code) FROM app.user_roles WHERE user_id=u.id),
    'contacts',(SELECT jsonb_agg(to_jsonb(c)) FROM app.user_contacts c WHERE c.user_id=u.id),
    'addresses',(SELECT jsonb_agg((to_jsonb(a)-'location')||jsonb_build_object('longitude',public.ST_X(a.location::public.geometry),
      'latitude',public.ST_Y(a.location::public.geometry),'state_name',s.name,'municipality_name',m.name,'parish_name',p.name) ORDER BY a.created_at,a.id)
      FROM app.addresses a LEFT JOIN app.geo_parishes p ON p.code=a.parish_code
      LEFT JOIN app.geo_municipalities m ON m.code=a.municipality_code LEFT JOIN app.geo_states s ON s.code=a.state_code
      WHERE a.user_id=u.id AND a.archived_at IS NULL))
  FROM app.users u WHERE u.id=internal.actor();
$$;
CREATE OR REPLACE FUNCTION api.my_professional() RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(p)||jsonb_build_object('branches',(SELECT jsonb_agg((to_jsonb(b)-'location')||jsonb_build_object(
    'longitude',public.ST_X(b.location::public.geometry),'latitude',public.ST_Y(b.location::public.geometry),
    'state_name',s.name,'municipality_name',m.name,'parish_name',par.name)) FROM app.branches b
    LEFT JOIN app.geo_parishes par ON par.code=b.parish_code LEFT JOIN app.geo_municipalities m ON m.code=b.municipality_code
    LEFT JOIN app.geo_states s ON s.code=b.state_code WHERE b.professional_id=p.user_id),
    'contacts',(SELECT jsonb_agg(to_jsonb(c)) FROM app.professional_contacts c WHERE c.professional_id=p.user_id),
    'coverage',(SELECT jsonb_agg(to_jsonb(c)-'polygon') FROM app.coverage_areas c WHERE c.professional_id=p.user_id),
    'offerings',(SELECT jsonb_agg(to_jsonb(o)) FROM app.offerings o WHERE o.professional_id=p.user_id),
    'store',(SELECT to_jsonb(s) FROM app.stores s WHERE s.professional_id=p.user_id))
  FROM app.professionals p WHERE p.user_id=internal.actor();
$$;
