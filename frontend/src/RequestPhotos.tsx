import { useEffect, useState } from "react";
import { ImagePlus, X } from "lucide-react";
import { api, privateFileBlob, type Data } from "./api";
import { Badge, RunButton } from "./components";

function LocalPhoto({ file }: { file: File }) {
  const [url, setUrl] = useState("");
  useEffect(() => {
    const next = URL.createObjectURL(file);
    setUrl(next);
    return () => URL.revokeObjectURL(next);
  }, [file]);
  return url ? <img src={url} alt={`Referencia: ${file.name}`} /> : null;
}

export function RequestPhotoInput({
  files,
  onChange,
  disabled = false,
}: {
  files: File[];
  onChange: (files: File[]) => void;
  disabled?: boolean;
}) {
  const [error, setError] = useState("");
  return (
    <div className="request-photo-input">
      <label className="upload-zone">
        <ImagePlus size={22} />
        <span>
          <strong>Fotos de referencia</strong>
          <small>
            Opcional · hasta 6 fotos JPG, PNG o WEBP de 10 MB cada una.
          </small>
          <small className="link-button">Seleccionar fotos</small>
        </span>
        <input
          type="file"
          aria-label="Fotos de referencia"
          multiple
          accept="image/jpeg,image/png,image/webp"
          disabled={disabled}
          onChange={(event) => {
            const added = Array.from(event.target.files ?? []);
            event.target.value = "";
            if (
              added.some(
                (f) =>
                  !["image/jpeg", "image/png", "image/webp"].includes(f.type) ||
                  f.size > 10 * 1024 * 1024 ||
                  f.size === 0,
              )
            ) {
              setError("Usa fotos JPG, PNG o WEBP de hasta 10 MB cada una.");
              return;
            }
            const next = [...files];
            for (const file of added)
              if (
                !next.some(
                  (f) =>
                    f.name === file.name &&
                    f.size === file.size &&
                    f.lastModified === file.lastModified,
                )
              )
                next.push(file);
            if (next.length > 6) {
              setError("Puedes adjuntar hasta 6 fotos.");
              return;
            }
            setError("");
            onChange(next);
          }}
        />
      </label>
      <p className="field-hint">
        Muestra la falla o el trabajo que necesitas. Evita rostros, documentos y
        datos de contacto.
      </p>
      {error && (
        <p className="field-error" role="alert">
          {error}
        </p>
      )}
      {files.length > 0 && (
        <div className="request-photos">
          {files.map((file, index) => (
            <div
              className="request-photo"
              key={`${file.name}-${file.lastModified}-${index}`}
            >
              <LocalPhoto file={file} />
              <button
                type="button"
                className="photo-remove"
                disabled={disabled}
                aria-label={`Quitar foto ${index + 1}`}
                onClick={() => {
                  onChange(files.filter((_, i) => i !== index));
                  setError("");
                }}
              >
                <X size={16} />
              </button>
              <small>{file.name}</small>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function SavedPhoto({
  photo,
  index,
  requestId,
  after,
}: {
  photo: Data;
  index: number;
  requestId?: string;
  after?: () => void;
}) {
  const [url, setUrl] = useState(""),
    [error, setError] = useState("");
  useEffect(() => {
    let active = true,
      objectUrl = "";
    privateFileBlob(photo.file_id)
      .then((blob) => {
        if (!active) return;
        objectUrl = URL.createObjectURL(blob);
        setUrl(objectUrl);
      })
      .catch((e) => {
        if (active) setError(e.message);
      });
    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [photo.file_id]);
  return (
    <div className="request-photo saved-photo">
      {url ? (
        <a
          href={url}
          target="_blank"
          rel="noreferrer"
          aria-label={`Ver foto de referencia ${index + 1}`}
        >
          <img src={url} alt={`Foto de referencia ${index + 1}`} loading="lazy" decoding="async" />
        </a>
      ) : (
        <small>{error || "Cargando foto…"}</small>
      )}
      {photo.moderation_status && <Badge value={photo.moderation_status} />}
      {requestId && (
        <RunButton
          run={() =>
            api("remove_request_image", {
              request: requestId,
              file: photo.file_id,
            })
          }
          after={after}
        >
          Quitar foto {index + 1}
        </RunButton>
      )}
    </div>
  );
}

export function RequestImages({
  photos,
  requestId,
  after,
}: {
  photos?: Data[];
  requestId?: string;
  after?: () => void;
}) {
  if (!photos?.length) return null;
  return (
    <div className="request-photos" aria-label="Fotos adjuntas">
      {photos.map((photo, index) => (
        <SavedPhoto
          key={photo.file_id}
          photo={photo}
          index={index}
          requestId={requestId}
          after={after}
        />
      ))}
    </div>
  );
}
