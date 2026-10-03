-- Private branches use the parish reference point, never their exact coordinates.
CREATE FUNCTION api.professional_map_markers(p_professionals uuid[],p_category uuid DEFAULT NULL,
  p_lon double precision DEFAULT NULL,p_lat double precision DEFAULT NULL,p_radius_m integer DEFAULT 10000)
RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE point public.geography;
BEGIN
  IF p_professionals IS NULL OR cardinality(p_professionals)>100
    OR p_radius_m IS NULL OR p_radius_m NOT BETWEEN 100 AND 200000
    OR (p_lon IS NULL)<>(p_lat IS NULL) THEN RAISE EXCEPTION 'invalid_search_filters'; END IF;
  IF p_lon IS NOT NULL THEN
    point:=internal.point(p_lon,p_lat);
    IF api.locate_parish(p_lon,p_lat) IS NULL THEN RAISE EXCEPTION 'venezuela_only'; END IF;
  END IF;
  RETURN QUERY
  SELECT DISTINCT jsonb_build_object('professional_id',p.user_id,
    'lat',CASE WHEN b.public_address THEN public.ST_Y(b.location::public.geometry) ELSE parish.latitude END,
    'lon',CASE WHEN b.public_address THEN public.ST_X(b.location::public.geometry) ELSE parish.longitude END,
    'approximate',NOT b.public_address,
    'label',CASE WHEN b.public_address THEN b.address_line ELSE parish.name||', '||b.city END)
  FROM app.professionals p JOIN app.users u ON u.id=p.user_id AND u.status='active'
  JOIN app.branches b ON b.professional_id=p.user_id AND b.active AND b.country_code='VE'
  LEFT JOIN app.geo_parishes parish ON parish.code=b.parish_code
  WHERE p.user_id=ANY(p_professionals) AND p.status='active'
    AND (b.public_address OR parish.code IS NOT NULL)
    AND (point IS NULL OR (p.accepts_requests AND public.ST_DWithin(b.location,point,p_radius_m)))
    AND EXISTS(SELECT 1 FROM app.offerings o JOIN app.categories cat ON cat.id=o.category_id AND cat.active
      WHERE o.professional_id=p.user_id AND o.active AND o.delivery_mode='onsite' AND o.currency IN ('VES','USD')
        AND (p_category IS NULL OR o.category_id=p_category)
        AND EXISTS(SELECT 1 FROM app.coverage_areas ca WHERE ca.branch_id=b.id AND ca.professional_id=p.user_id
          AND ca.category_id=o.category_id AND ca.active
          AND (point IS NULL OR (public.ST_DWithin(b.location,point,ca.radius_m)
            AND (ca.polygon IS NULL OR public.ST_Covers(ca.polygon,point))))));
END $$;

REVOKE ALL ON FUNCTION api.professional_map_markers(uuid[],uuid,double precision,double precision,integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.professional_map_markers(uuid[],uuid,double precision,double precision,integer) TO chamba_api;
