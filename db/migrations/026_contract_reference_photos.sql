-- Reference photos remain available to both participants after assignment.
CREATE OR REPLACE FUNCTION api.contract_detail(p_contract uuid) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts:=internal.participant(p_contract);
BEGIN
  RETURN to_jsonb(c)||jsonb_build_object('title',(SELECT title FROM app.service_requests WHERE id=c.request_id),
    'customer_name',(SELECT display_name FROM app.users WHERE id=c.customer_id),
    'professional_name',(SELECT business_name FROM app.professionals WHERE user_id=c.professional_id),
    'terms',(SELECT jsonb_agg(to_jsonb(t) ORDER BY version DESC) FROM app.contract_terms t WHERE contract_id=c.id),
    'events',(SELECT jsonb_agg(to_jsonb(e) ORDER BY id) FROM app.contract_events e WHERE contract_id=c.id),
    'reference_photos',coalesce((SELECT jsonb_agg(jsonb_build_object('file_id',f.id,'mime_type',f.mime_type) ORDER BY f.created_at,f.id)
      FROM app.request_attachments ra JOIN app.files f ON f.id=ra.file_id
      WHERE ra.request_id=c.request_id AND f.moderation_status='approved' AND f.exif_removed AND f.deleted_at IS NULL),'[]'::jsonb));
END $$;
