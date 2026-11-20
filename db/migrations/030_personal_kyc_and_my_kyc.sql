-- Migración 030: Actualización de api.my_kyc() para devolver documentos adjuntos
CREATE OR REPLACE FUNCTION api.my_kyc() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT (to_jsonb(k)-'document_ciphertext'-'identity_fingerprint') || jsonb_build_object(
    'documents', coalesce((
      SELECT jsonb_agg(jsonb_build_object('kind', d.kind, 'file_id', f.id, 'created_at', f.created_at))
      FROM app.kyc_documents d
      JOIN app.files f ON f.id = d.file_id
      WHERE d.case_id = k.id AND f.deleted_at IS NULL
    ), '[]'::jsonb)
  )
  FROM app.kyc_cases k
  WHERE user_id = internal.actor()
  ORDER BY submitted_at DESC;
$$;
