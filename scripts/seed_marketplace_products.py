import urllib.request
import uuid
import hashlib
import os
import subprocess
import json

FILES_DIR = "/var/www/chamba/backend/data/files"
os.makedirs(FILES_DIR, exist_ok=True)

ITEMS = [
    {
        "sku": "TECH-SSD-480",
        "name": "Unidad SSD Kingston 480 GB",
        "description": "Disco sólido de alta velocidad SATA 3 2.5 pulg. Ideal para repotenciar laptops y computadoras de escritorio con arranque ultra rápido.",
        "price_minor": 3500,
        "currency": "USD",
        "stock": 15,
        "category_slug": "informatica",
        "url": "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=600&auto=format&fit=crop&q=80",
        "owner_email": "tecnico@demo.example.com",
        "store_slug": "tecnologia-demo",
        "store_name": "Daniel · Tecnología",
    },
    {
        "sku": "TECH-AUDIO-BT",
        "name": "Audífonos Inalámbricos Bluetooth Pro",
        "description": "Auriculares con cancelación activa de ruido, micrófono HD para llamadas y estuche con carga rápida de hasta 24 horas de batería.",
        "price_minor": 2800,
        "currency": "USD",
        "stock": 12,
        "category_slug": "informatica",
        "url": "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80",
        "owner_email": "tecnico@demo.example.com",
        "store_slug": "tecnologia-demo",
        "store_name": "Daniel · Tecnología",
    },
    {
        "sku": "ELEC-RGB-BULBS",
        "name": "Bombillos LED Inteligentes RGB Pack x4",
        "description": "Bombillos WiFi compatibles con Alexa y Google Home. Control de color desde el celular, programación de encendido y bajo consumo.",
        "price_minor": 1800,
        "currency": "USD",
        "stock": 20,
        "category_slug": "electricidad",
        "url": "https://images.unsplash.com/photo-1550985543-f47f38aeee65?w=600&auto=format&fit=crop&q=80",
        "owner_email": "tecnico@demo.example.com",
        "store_slug": "tecnologia-demo",
        "store_name": "Daniel · Tecnología",
    },
    {
        "sku": "TOOL-DRILL-20V",
        "name": "Taladro Percutor Inalámbrico 20V",
        "description": "Taladro percutor profesional con 2 baterías de litio, cargador rápido, reversa y maletín con 24 accesorios de perforación.",
        "price_minor": 4800,
        "currency": "USD",
        "stock": 8,
        "category_slug": "cerrajeria",
        "url": "https://images.unsplash.com/photo-1504148455328-c376907d081c?w=600&auto=format&fit=crop&q=80",
        "owner_email": "plomero@demo.example.com",
        "store_slug": "mateo-ferreteria",
        "store_name": "Mateo · Ferretería y Hogar",
    },
    {
        "sku": "TOOL-WRENCH-SET",
        "name": "Juego de Llaves Combinadas 12 Piezas",
        "description": "Set de llaves milimétricas de 6mm a 22mm fabricadas en acero cromo vanadio forjado de máxima resistencia con estuche organizador.",
        "price_minor": 2200,
        "currency": "USD",
        "stock": 14,
        "category_slug": "carpinteria",
        "url": "https://images.unsplash.com/photo-1581783342308-f792dbdd27c5?w=600&auto=format&fit=crop&q=80",
        "owner_email": "plomero@demo.example.com",
        "store_slug": "mateo-ferreteria",
        "store_name": "Mateo · Ferretería y Hogar",
    },
    {
        "sku": "CLEAN-KIT-PREM",
        "name": "Kit de Limpieza Profesional Multiuso",
        "description": "Kit completo para desinfección y limpieza profunda. Incluye paños de microfibra, cepillos de cerdas duras y atomizadores industriales.",
        "price_minor": 1200,
        "currency": "USD",
        "stock": 25,
        "category_slug": "limpieza",
        "url": "https://images.unsplash.com/photo-1585421514738-01798e348b17?w=600&auto=format&fit=crop&q=80",
        "owner_email": "plomero@demo.example.com",
        "store_slug": "mateo-ferreteria",
        "store_name": "Mateo · Ferretería y Hogar",
    },
    {
        "sku": "REP-OIL-20W50",
        "name": "Aceite para Motor 20W-50 4T (1 Litro)",
        "description": "Lubricante mineral premium para motores 4 tiempos de moto y autos ligeros. Excelente protección contra el desgaste térmico y fricción.",
        "price_minor": 850,
        "currency": "USD",
        "stock": 30,
        "category_slug": "cerrajeria-automotriz",
        "url": "https://images.unsplash.com/photo-1619642751034-765dfdf7c58e?w=600&auto=format&fit=crop&q=80",
        "owner_email": "plomero@demo.example.com",
        "store_slug": "mateo-ferreteria",
        "store_name": "Mateo · Ferretería y Hogar",
    },
    {
        "sku": "PLUMB-PUMP-HALFHP",
        "name": "Bomba de Agua Periférica 1/2 HP",
        "description": "Electrobomba de 0.5 HP con turbina de latón e impulsor en bronce. Flujo constante de agua para casas, apartamentos y sistemas hidroneumáticos.",
        "price_minor": 4200,
        "currency": "USD",
        "stock": 6,
        "category_slug": "plomeria",
        "url": "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=600&auto=format&fit=crop&q=80",
        "owner_email": "plomero@demo.example.com",
        "store_slug": "mateo-ferreteria",
        "store_name": "Mateo · Ferretería y Hogar",
    },
]

def run_sql(sql):
    cmd = ["docker", "exec", "-i", "chamba-db-1", "psql", "-U", "chamba_owner", "-d", "chamba"]
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    out, err = p.communicate(input=sql)
    if p.returncode != 0:
        raise Exception(f"SQL error: {err}")
    return out

print("Iniciando creación de productos con imágenes reales...")

# Asegurar tiendas y derechos para Mateo y Daniel
run_sql("""
DO $$
DECLARE
  v_daniel uuid;
  v_mateo uuid;
  v_store_daniel uuid;
  v_store_mateo uuid;
BEGIN
  SELECT id INTO v_daniel FROM app.users WHERE email = 'tecnico@demo.example.com';
  SELECT id INTO v_mateo FROM app.users WHERE email = 'plomero@demo.example.com';

  -- Entitlements
  INSERT INTO app.entitlements(professional_id, kind, starts_at, ends_at)
  VALUES (v_daniel, 'marketplace', now(), now() + interval '50 years')
  ON CONFLICT (professional_id, kind) DO UPDATE SET ends_at = now() + interval '50 years', revoked_at = NULL;

  INSERT INTO app.entitlements(professional_id, kind, starts_at, ends_at)
  VALUES (v_mateo, 'marketplace', now(), now() + interval '50 years')
  ON CONFLICT (professional_id, kind) DO UPDATE SET ends_at = now() + interval '50 years', revoked_at = NULL;

  -- Store Daniel
  INSERT INTO app.stores(professional_id, slug, name, description, status)
  VALUES (v_daniel, 'tecnologia-demo', 'Daniel · Tecnología', 'Especialistas en componentes, electrónica y accesorios.', 'active')
  ON CONFLICT (professional_id) DO UPDATE SET status = 'active', name = 'Daniel · Tecnología';

  -- Store Mateo
  INSERT INTO app.stores(professional_id, slug, name, description, status)
  VALUES (v_mateo, 'mateo-ferreteria', 'Mateo · Ferretería y Hogar', 'Herramientas, insumos y repuestos con entrega inmediata.', 'active')
  ON CONFLICT (professional_id) DO UPDATE SET status = 'active', name = 'Mateo · Ferretería y Hogar';
END $$;
""")

headers = {'User-Agent': 'Mozilla/5.0'}
for item in ITEMS:
    print(f"Descargando imagen para: {item['name']}...")
    req = urllib.request.Request(item['url'], headers=headers)
    with urllib.request.urlopen(req, timeout=15) as resp:
        img_data = resp.read()

    file_id = str(uuid.uuid4())
    obj_key = f"{uuid.uuid4()}.jpg"
    file_path = os.path.join(FILES_DIR, obj_key)
    with open(file_path, "wb") as f:
        f.write(img_data)
    os.chmod(file_path, 0o644)

    sha = hashlib.sha256(img_data).hexdigest()
    file_bytes = len(img_data)

    # Insertar en base de datos
    sql = f"""
    DO $$
    DECLARE
      v_user uuid;
      v_store uuid;
      v_cat uuid;
      v_prod uuid;
    BEGIN
      SELECT id INTO v_user FROM app.users WHERE email = '{item['owner_email']}';
      SELECT id INTO v_store FROM app.stores WHERE professional_id = v_user;
      SELECT id INTO v_cat FROM app.categories WHERE slug = '{item['category_slug']}';

      -- Archivo en app.files
      INSERT INTO app.files(id, owner_id, object_key, mime_type, bytes, sha256, purpose, moderation_status, exif_removed)
      VALUES ('{file_id}', v_user, '{obj_key}', 'image/jpeg', {file_bytes}, '{sha}', 'product', 'approved', true);

      -- Producto en app.products (o update si existe sku)
      INSERT INTO app.products(store_id, category_id, sku, name, description, price_minor, currency, stock, status)
      VALUES (v_store, v_cat, '{item['sku']}', '{item['name']}', '{item['description']}', {item['price_minor']}, '{item['currency']}', {item['stock']}, 'active')
      ON CONFLICT (store_id, sku) DO UPDATE
        SET name = EXCLUDED.name,
            description = EXCLUDED.description,
            price_minor = EXCLUDED.price_minor,
            stock = EXCLUDED.stock,
            status = 'active'
      RETURNING id INTO v_prod;

      -- Imagen de producto
      DELETE FROM app.product_images WHERE product_id = v_prod;
      INSERT INTO app.product_images(product_id, file_id, sort_order)
      VALUES (v_prod, '{file_id}', 1);
    END $$;
    """
    run_sql(sql)
    print(f"✓ Registrado producto con foto: {item['name']} (ID archivo: {file_id})")

print("¡Todos los productos e imágenes fueron cargados con éxito!")
