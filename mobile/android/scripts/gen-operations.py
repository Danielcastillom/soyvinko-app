#!/usr/bin/env python3
"""Regenera shared/.../OperationsRegistry.kt desde frontend/src/operations.json (tras scripts/export_http_registry.py)."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
ops = json.loads((root / "../../frontend/src/operations.json").read_text())
lines = [f'    "{k}" to ("{v["path"]}" to "{v["method"]}"),' for k, v in sorted(ops.items())]
out = root / "shared/src/commonMain/kotlin/com/soyvinko/app/data/OperationsRegistry.kt"
out.write_text(
    "package com.soyvinko.app.data\n\n"
    "// GENERADO por scripts/gen-operations.py desde frontend/src/operations.json. No editar a mano.\n"
    "// Nombre de operación -> (ruta, método). Mismo registro que usa api() en la web.\n"
    "internal val OPERATIONS: Map<String, Pair<String, String>> = mapOf(\n" + "\n".join(lines) + "\n)\n"
)
print(f"{len(ops)} operaciones -> {out}")
