-- 039_address_names_and_rls.sql
--   1. api.my_account vuelve a devolver estado, municipio y parroquia de cada dirección
--      (029 los había perdido al redefinir la función).
--   2. app.vinko_exchange_rates (034) era la única tabla de app sin RLS (denegación por defecto).

ALTER TABLE app.vinko_exchange_rates ENABLE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION api.my_account() RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT jsonb_build_object(
    'id', u.id,
    'email', u.email,
    'name', u.display_name,
    'status', u.status,
    'account_type', coalesce(u.account_type, 'natural'),
    'business_name', u.business_name,
    'rif', u.rif,
    'contact_name', u.contact_name,
    'roles', (SELECT jsonb_agg(role_code) FROM app.user_roles WHERE user_id=u.id),
    'contacts', (SELECT jsonb_agg(to_jsonb(c)) FROM app.user_contacts c WHERE c.user_id=u.id),
    'addresses', (
      SELECT jsonb_agg((to_jsonb(a)-'location') || jsonb_build_object(
        'longitude', public.ST_X(a.location::public.geometry),
        'latitude', public.ST_Y(a.location::public.geometry),
        'state_name', s.name, 'municipality_name', m.name, 'parish_name', p.name) ORDER BY a.created_at, a.id)
      FROM app.addresses a
      LEFT JOIN app.geo_parishes p ON p.code=a.parish_code
      LEFT JOIN app.geo_municipalities m ON m.code=a.municipality_code
      LEFT JOIN app.geo_states s ON s.code=a.state_code
      WHERE a.user_id=u.id AND a.archived_at IS NULL)
  )
  FROM app.users u WHERE u.id=internal.actor();
$$;
