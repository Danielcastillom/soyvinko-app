-- 040_upload_auto_approval.sql
-- 035 quiso aprobar automáticamente los archivos de portafolio, evidencia, productos y solicitudes,
-- pero solo actualizó los existentes: cada archivo nuevo quedaba 'pending' y send_attachment /
-- submit_delivery / add_product_image lo rechazaban. Además el chat no estaba incluido.
-- Ahora se aprueban al registrarse cuando el servidor ya limpió el archivo (imagen re-codificada sin
-- EXIF, o PDF validado). KYC y avatar siguen pasando por moderación.

CREATE OR REPLACE FUNCTION api.register_upload(p_owner uuid, p_object_key text, p_mime text, p_bytes bigint, p_sha256 text, p_purpose text, p_exif_removed boolean)
RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_id uuid;
BEGIN
  INSERT INTO app.files(owner_id,object_key,mime_type,bytes,sha256,purpose,exif_removed,moderation_status)
  VALUES(p_owner,p_object_key,p_mime,p_bytes,p_sha256,p_purpose,p_exif_removed,
    CASE WHEN p_purpose IN ('portfolio','evidence','product','request','chat') AND p_exif_removed
         THEN 'approved' ELSE 'pending' END)
  RETURNING id INTO v_id;
  RETURN v_id;
END $$;

-- Archivos subidos después de 035 que quedaron pendientes por el error.
UPDATE app.files SET moderation_status='approved'
WHERE purpose IN ('portfolio','evidence','product','request','chat')
  AND moderation_status='pending' AND exif_removed AND deleted_at IS NULL;
