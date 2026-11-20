-- PostgreSQL concede EXECUTE a PUBLIC globalmente por defecto. Revocar solo
-- IN SCHEMA no retira ese privilegio global. Cubrimos funciones ya existentes
-- y las que cree este propietario en migraciones posteriores.
ALTER DEFAULT PRIVILEGES REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA app,api,internal FROM PUBLIC;

CREATE OR REPLACE FUNCTION api.expire_requests() RETURNS integer
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_count integer; v_ids uuid[];
BEGIN
  WITH expired AS(UPDATE app.service_requests SET status='expired' WHERE status IN('draft','open') AND expires_at<=now() RETURNING id)
  SELECT coalesce(array_agg(id),'{}'::uuid[]),count(*)::integer INTO v_ids,v_count FROM expired;
  UPDATE app.proposals SET status='rejected' WHERE request_id=ANY(v_ids) AND status='pending';
  DELETE FROM app.professional_positions WHERE expires_at<=now();
  UPDATE app.location_shares SET revoked_at=now() WHERE revoked_at IS NULL AND expires_at<=now();
  UPDATE app.kyc_cases SET status='expired' WHERE status='approved' AND expires_at<=now();
  RETURN v_count;
END $$;
