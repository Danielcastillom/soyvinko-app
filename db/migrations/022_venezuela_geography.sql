-- Administrative reference data is loaded by 023 from the INE/OCHA COD-AB snapshot.
CREATE TABLE app.geo_countries (
  code text PRIMARY KEY CHECK (code ~ '^[A-Z]{2}$'), name text NOT NULL,
  active boolean NOT NULL DEFAULT false, boundary public.geometry(MultiPolygon,4326) NOT NULL
);
CREATE TABLE app.geo_states (
  code text PRIMARY KEY, country_code text NOT NULL REFERENCES app.geo_countries,
  name text NOT NULL, kind text NOT NULL DEFAULT 'state',
  latitude double precision NOT NULL, longitude double precision NOT NULL
);
CREATE TABLE app.geo_municipalities (
  code text PRIMARY KEY, state_code text NOT NULL REFERENCES app.geo_states,
  name text NOT NULL, kind text NOT NULL DEFAULT 'municipality',
  latitude double precision NOT NULL, longitude double precision NOT NULL,
  UNIQUE(code,state_code)
);
CREATE TABLE app.geo_parishes (
  code text PRIMARY KEY, municipality_code text NOT NULL REFERENCES app.geo_municipalities,
  name text NOT NULL, kind text NOT NULL DEFAULT 'parish',
  latitude double precision NOT NULL, longitude double precision NOT NULL,
  boundary public.geometry(MultiPolygon,4326) NOT NULL,
  UNIQUE(code,municipality_code)
);
CREATE INDEX geo_municipality_parent_idx ON app.geo_municipalities(state_code);
CREATE INDEX geo_parish_parent_idx ON app.geo_parishes(municipality_code);
CREATE INDEX geo_parish_boundary_idx ON app.geo_parishes USING gist(boundary);
CREATE INDEX geo_parish_geography_idx ON app.geo_parishes USING gist((boundary::public.geography));
ALTER TABLE app.geo_countries ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.geo_states ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.geo_municipalities ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.geo_parishes ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.addresses ADD COLUMN state_code text REFERENCES app.geo_states,
  ADD COLUMN municipality_code text, ADD COLUMN parish_code text,
  ADD FOREIGN KEY(municipality_code,state_code) REFERENCES app.geo_municipalities(code,state_code),
  ADD FOREIGN KEY(parish_code,municipality_code) REFERENCES app.geo_parishes(code,municipality_code),
  ADD CHECK ((state_code IS NULL AND municipality_code IS NULL AND parish_code IS NULL)
    OR (country_code='VE' AND state_code IS NOT NULL AND municipality_code IS NOT NULL AND parish_code IS NOT NULL));
ALTER TABLE app.branches ADD COLUMN state_code text REFERENCES app.geo_states,
  ADD COLUMN municipality_code text, ADD COLUMN parish_code text,
  ADD FOREIGN KEY(municipality_code,state_code) REFERENCES app.geo_municipalities(code,state_code),
  ADD FOREIGN KEY(parish_code,municipality_code) REFERENCES app.geo_parishes(code,municipality_code),
  ADD CHECK ((state_code IS NULL AND municipality_code IS NULL AND parish_code IS NULL)
    OR (country_code='VE' AND state_code IS NOT NULL AND municipality_code IS NOT NULL AND parish_code IS NOT NULL));

CREATE FUNCTION api.geo_states() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(s) FROM app.geo_states s JOIN app.geo_countries c ON c.code=s.country_code
  WHERE c.active AND c.code='VE' ORDER BY s.name;
$$;
CREATE FUNCTION api.geo_municipalities(p_state text) RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(m) FROM app.geo_municipalities m WHERE m.state_code=p_state ORDER BY m.name;
$$;
CREATE FUNCTION api.geo_parishes(p_municipality text) RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(p)-'boundary' FROM app.geo_parishes p WHERE p.municipality_code=p_municipality ORDER BY p.name;
$$;
CREATE FUNCTION api.geo_parish(p_parish text) RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT (to_jsonb(p)-'boundary')||jsonb_build_object('state_code',s.code,'state_name',s.name,
    'municipality_name',m.name,'country_code',s.country_code,
    'bbox',jsonb_build_array(public.ST_XMin(p.boundary),public.ST_YMin(p.boundary),public.ST_XMax(p.boundary),public.ST_YMax(p.boundary)),
    'geometry',public.ST_AsGeoJSON(p.boundary,5)::jsonb)
  FROM app.geo_parishes p JOIN app.geo_municipalities m ON m.code=p.municipality_code
  JOIN app.geo_states s ON s.code=m.state_code WHERE p.code=p_parish;
$$;
CREATE FUNCTION api.locate_parish(p_lon double precision,p_lat double precision) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE point public.geography:=internal.point(p_lon,p_lat); result jsonb;
BEGIN
  SELECT jsonb_build_object('country_code',s.country_code,'state_code',s.code,'state_name',s.name,
    'municipality_code',m.code,'municipality_name',m.name,'parish_code',p.code,'parish_name',p.name)
    INTO result FROM app.geo_parishes p JOIN app.geo_municipalities m ON m.code=p.municipality_code
    JOIN app.geo_states s ON s.code=m.state_code JOIN app.geo_countries c ON c.code=s.country_code AND c.active
    WHERE public.ST_DWithin(p.boundary::public.geography,point,100)
    ORDER BY public.ST_Covers(p.boundary,point::public.geometry) DESC,
      public.ST_Distance(p.boundary::public.geography,point),p.code LIMIT 1;
  RETURN result;
END $$;
CREATE FUNCTION internal.venezuela_location(p_country text,p_state text,p_municipality text,p_parish text,
  p_lon double precision,p_lat double precision) RETURNS public.geography
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE point public.geography:=internal.point(p_lon,p_lat); boundary public.geometry;
BEGIN
  IF p_country IS DISTINCT FROM 'VE' THEN RAISE EXCEPTION 'venezuela_only'; END IF;
  SELECT p.boundary INTO boundary FROM app.geo_parishes p
    JOIN app.geo_municipalities m ON m.code=p.municipality_code JOIN app.geo_states s ON s.code=m.state_code
    JOIN app.geo_countries c ON c.code=s.country_code
    WHERE p.code=p_parish AND m.code=p_municipality AND s.code=p_state AND c.code=p_country AND c.active;
  IF NOT FOUND THEN RAISE EXCEPTION 'invalid_address_hierarchy'; END IF;
  IF NOT public.ST_DWithin(boundary::public.geography,point,100) THEN RAISE EXCEPTION 'point_outside_parish'; END IF;
  RETURN point;
END $$;
CREATE FUNCTION api.add_address_ve(p_label text,p_country text,p_state text,p_municipality text,p_parish text,
  p_line text,p_lon double precision,p_lat double precision,p_instructions text DEFAULT NULL) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE actor uuid:=internal.actor(); ident uuid; point public.geography;
BEGIN
  IF p_label IS NULL OR length(btrim(p_label)) NOT BETWEEN 1 AND 80 OR p_line IS NULL
    OR length(btrim(p_line)) NOT BETWEEN 3 AND 300 OR length(coalesce(p_instructions,''))>1000 THEN RAISE EXCEPTION 'invalid_address_details'; END IF;
  point:=internal.venezuela_location(p_country,p_state,p_municipality,p_parish,p_lon,p_lat);
  INSERT INTO app.addresses(user_id,label,country_code,region,city,address_line,location,instructions,state_code,municipality_code,parish_code)
  SELECT actor,btrim(p_label),'VE',s.name,m.name,btrim(p_line),point,nullif(btrim(p_instructions),''),s.code,m.code,p_parish
    FROM app.geo_states s JOIN app.geo_municipalities m ON m.state_code=s.code WHERE m.code=p_municipality RETURNING id INTO ident;
  RETURN ident;
END $$;
CREATE FUNCTION api.add_branch_ve(p_name text,p_country text,p_state text,p_municipality text,p_parish text,
  p_line text,p_lon double precision,p_lat double precision,p_public_address boolean DEFAULT false) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE actor uuid:=internal.actor(); ident uuid; point public.geography;
BEGIN
  IF p_name IS NULL OR length(btrim(p_name)) NOT BETWEEN 1 AND 80 OR p_line IS NULL
    OR length(btrim(p_line)) NOT BETWEEN 3 AND 300 THEN RAISE EXCEPTION 'invalid_address_details'; END IF;
  point:=internal.venezuela_location(p_country,p_state,p_municipality,p_parish,p_lon,p_lat);
  INSERT INTO app.branches(professional_id,name,country_code,city,address_line,location,public_address,timezone,state_code,municipality_code,parish_code)
  SELECT actor,btrim(p_name),'VE',m.name,btrim(p_line),point,coalesce(p_public_address,false),'America/Caracas',m.state_code,m.code,p_parish
    FROM app.geo_municipalities m WHERE m.code=p_municipality RETURNING id INTO ident;
  RETURN ident;
END $$;
CREATE FUNCTION internal.public_listing(p_professional uuid) RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT jsonb_build_object('id',p.user_id,'slug',p.slug,'name',p.business_name,'bio',p.bio,'years_experience',p.years_experience,
    'requires_platform_payment',u.requires_platform_payment,
    'verified',EXISTS(SELECT 1 FROM app.kyc_cases k WHERE k.user_id=p.user_id AND k.status='approved' AND k.expires_at>now()),
    'rating',(SELECT round(avg(rating),2) FROM app.reviews WHERE subject_id=p.user_id AND moderation_status='published'),
    'review_count',(SELECT count(*) FROM app.reviews WHERE subject_id=p.user_id AND moderation_status='published'),
    'city',(SELECT city FROM app.branches WHERE professional_id=p.user_id AND active AND country_code='VE' ORDER BY created_at LIMIT 1),
    'offerings',coalesce((SELECT jsonb_agg(jsonb_build_object('id',o.id,'category_id',o.category_id,'title',o.title,'description',o.description,
      'delivery_mode',o.delivery_mode,'currency',o.currency,'price_min_minor',o.price_min_minor,'pricing_type',o.pricing_type) ORDER BY o.created_at,o.id)
      FROM app.offerings o WHERE o.professional_id=p.user_id AND o.active AND o.currency IN ('VES','USD')),'[]'::jsonb))
  FROM app.professionals p JOIN app.users u ON u.id=p.user_id WHERE p.user_id=p_professional AND p.status='active' AND u.status='active';
$$;
CREATE OR REPLACE FUNCTION api.discover(p_category uuid DEFAULT NULL,p_mode text DEFAULT NULL,p_limit integer DEFAULT 30,p_offset integer DEFAULT 0) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  IF p_limit IS NULL OR p_limit NOT BETWEEN 1 AND 100 OR p_offset IS NULL OR p_offset<0 THEN RAISE EXCEPTION 'invalid_pagination'; END IF;
  RETURN QUERY SELECT internal.public_listing(p.user_id)
    FROM app.professionals p JOIN app.users u ON u.id=p.user_id WHERE p.status='active' AND u.status='active'
    AND EXISTS(SELECT 1 FROM app.offerings o WHERE o.professional_id=p.user_id AND o.active AND o.currency IN ('VES','USD')
      AND (p_category IS NULL OR o.category_id=p_category) AND (p_mode IS NULL OR o.delivery_mode=p_mode)
      AND (o.delivery_mode='remote' OR EXISTS(SELECT 1 FROM app.branches b WHERE b.professional_id=p.user_id AND b.active AND b.country_code='VE')))
    ORDER BY p.created_at,p.user_id LIMIT p_limit OFFSET p_offset;
END $$;
CREATE FUNCTION api.nearby_professionals(p_lon double precision,p_lat double precision,p_category uuid DEFAULT NULL,
  p_radius_m integer DEFAULT 10000,p_verified_only boolean DEFAULT false,p_limit integer DEFAULT 100) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE point public.geography:=internal.point(p_lon,p_lat);
BEGIN
  IF p_radius_m IS NULL OR p_radius_m NOT BETWEEN 100 AND 200000 OR p_limit IS NULL OR p_limit NOT BETWEEN 1 AND 100
    THEN RAISE EXCEPTION 'invalid_search_filters'; END IF;
  IF api.locate_parish(p_lon,p_lat) IS NULL THEN RAISE EXCEPTION 'venezuela_only'; END IF;
  RETURN QUERY WITH matches AS (
    SELECT p.user_id,min(public.ST_Distance(b.location,point)) AS meters
    FROM app.professionals p JOIN app.users u ON u.id=p.user_id AND u.status='active'
    JOIN app.coverage_areas ca ON ca.professional_id=p.user_id AND ca.active
    JOIN app.categories cat ON cat.id=ca.category_id AND cat.active
    JOIN app.branches b ON b.id=ca.branch_id AND b.active AND b.country_code='VE'
    WHERE p.status='active' AND p.accepts_requests AND (p_category IS NULL OR ca.category_id=p_category)
      AND public.ST_DWithin(b.location,point,p_radius_m) AND public.ST_DWithin(b.location,point,ca.radius_m)
      AND (ca.polygon IS NULL OR public.ST_Covers(ca.polygon,point))
      AND EXISTS(SELECT 1 FROM app.offerings o WHERE o.professional_id=p.user_id AND o.category_id=ca.category_id
        AND o.active AND o.delivery_mode='onsite' AND o.currency IN ('VES','USD'))
      AND (NOT p_verified_only OR EXISTS(SELECT 1 FROM app.kyc_cases k WHERE k.user_id=p.user_id AND k.status='approved' AND k.expires_at>now()))
    GROUP BY p.user_id ORDER BY meters,p.user_id LIMIT p_limit
  ) SELECT internal.public_listing(m.user_id)||jsonb_build_object('distance_m',(round(m.meters/100)*100)::bigint)
    FROM matches m ORDER BY m.meters,m.user_id;
END $$;

GRANT EXECUTE ON FUNCTION api.geo_states(),api.geo_municipalities(text),api.geo_parishes(text),api.geo_parish(text),
  api.locate_parish(double precision,double precision),
  api.add_address_ve(text,text,text,text,text,text,double precision,double precision,text),
  api.add_branch_ve(text,text,text,text,text,text,double precision,double precision,boolean),
  api.nearby_professionals(double precision,double precision,uuid,integer,boolean,integer) TO chamba_api;
