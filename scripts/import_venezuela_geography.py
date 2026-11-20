#!/usr/bin/env python3
"""Build the pinned INE/OCHA catalog migration; development-only dependency: shapely==2.1.2.

Usage: backend/.venv/bin/python scripts/import_venezuela_geography.py /path/to/ven_admin_boundaries.geojson.zip
Never overwrites an existing migration. No external geocoding or invented subdivisions.
"""
import hashlib
import json
from pathlib import Path
import sys
import zipfile
from shapely import make_valid, to_geojson
from shapely.geometry import shape

ROOT = Path(__file__).resolve().parents[1]
archive = Path(sys.argv[1])
target = ROOT / "db/migrations/023_venezuela_catalog.sql"
if target.exists():
    raise SystemExit("La migración existe; genera una migración nueva para actualizar el catálogo.")


def quote(value):
    return "'" + str(value).replace("'", "''") + "'"


def geometry(feature):
    area = make_valid(shape(feature["geometry"])).simplify(0.00015, preserve_topology=True)
    payload = json.loads(to_geojson(area))
    def rounded(value):
        return [rounded(v) for v in value] if isinstance(value, list) else round(value, 6)
    payload["coordinates"] = rounded(payload["coordinates"])
    return area, "public.ST_Multi(public.ST_CollectionExtract(public.ST_MakeValid(public.ST_SetSRID(public.ST_GeomFromGeoJSON(" + quote(json.dumps(payload, separators=(",", ":"))) + "),4326)),3))"


lines = ["-- INE / OCHA Venezuela COD-AB v01, downloaded 2026-09-11.",
    "-- https://data.humdata.org/dataset/cod-ab-ven ; CC BY-IGO 3.0.",
    "-- Archive SHA-256: " + hashlib.sha256(archive.read_bytes()).hexdigest(),
    "-- Boundary simplification: 0.00015 degrees, topology preserved; point checks allow 100m at boundaries."]
counts = {}
with zipfile.ZipFile(archive) as bundle:
    for level, table in enumerate(("geo_countries", "geo_states", "geo_municipalities", "geo_parishes")):
        features = json.loads(bundle.read(f"ven_admin{level}.geojson"))["features"]
        counts[table] = len(features)
        for feature in features:
            props = feature["properties"]
            code = props[f"adm{level}_pcode"]
            name = props[f"adm{level}_name"]
            if level == 0:
                _, geom = geometry(feature)
                lines.append(f"INSERT INTO app.{table}(code,name,active,boundary) VALUES ('VE','Venezuela',true,{geom});")
                continue
            if level == 1:
                kind = "federal_dependencies" if code == "VE25" else "capital_district" if code == "VE01" else "state"
                values = [quote(code), "'VE'", quote(name), quote(kind), str(props['center_lat']), str(props['center_lon'])]
                columns = "code,country_code,name,kind,latitude,longitude"
            else:
                parent = props[f"adm{level-1}_pcode"]
                kind = "federal_dependencies" if code.startswith("VE25") else "parish" if level == 3 else "municipality"
                values = [quote(code),quote(parent),quote(name),quote(kind)]
                columns = "code," + ("state_code" if level == 2 else "municipality_code") + ",name,kind,latitude,longitude"
                if level == 3:
                    area, geom = geometry(feature)
                    point = area.representative_point()
                    values += [str(round(point.y,6)),str(round(point.x,6)),geom]
                    columns += ",boundary"
                else:
                    values += [str(props['center_lat']),str(props['center_lon'])]
            lines.append(f"INSERT INTO app.{table}({columns}) VALUES ({','.join(values)});")
assert list(counts.values()) == [1,25,336,1135], counts
lines += ["ANALYZE app.geo_parishes;", "ANALYZE app.geo_municipalities;"]
target.write_text("\n".join(lines)+"\n")
print(json.dumps({"migration":str(target),"counts":counts,"bytes":target.stat().st_size}))
