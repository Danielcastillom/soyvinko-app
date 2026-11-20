-- 047_marketplace_entitlements_fix.sql
-- Permite que los derechos (entitlements) de marketplace no dependan obligatoriamente de compras previas de créditos
-- y añade restricción única (professional_id, kind) para permitir ON CONFLICT en activate_store y add_product.

ALTER TABLE app.entitlements ALTER COLUMN purchase_id DROP NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'entitlements_professional_kind_uniq'
  ) THEN
    ALTER TABLE app.entitlements ADD CONSTRAINT entitlements_professional_kind_uniq UNIQUE (professional_id, kind);
  END IF;
END $$;
