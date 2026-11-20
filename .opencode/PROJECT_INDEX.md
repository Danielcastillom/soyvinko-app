# 🗺️ Índice & Caché del Proyecto (.opencode)
> **Generado automáticamente por OpenCode Agent.**  
> Este archivo optimiza las búsquedas del agente para que no requiera re-analizar todo el workspace en cada consulta.  
> **Última actualización:** 2026-10-30 23:24:29  
> **Archivos indexados:** 285 | **Tamaño total:** 5035.9 KB  
> **Tecnologías:** .py (61), .sql (48), .tsx (29), .json (24), .md (21), .js (19), .ts (18), .xml (13)

---

## 🛠️ Stack Tecnológico & Entorno
- **Plataforma / Framework:** `Proyecto General`

---

## 📂 Mapa de Rutas & Estructura de Directorios

### 📁 [Raíz del Proyecto]
- `.env.example`
- `.gitignore`
- `README.md` ⭐ — `# Chamba · Backend Python y portal React Portal para conectar clientes con profesionales, con API modular FastAPI, web R`
- `builder.json` — `{ "project": "SoyVinko", "platform": "ios",`
- `compose.yaml`
- `roles_dump.sql`

### 📁 backend/
- `.env.example`
- `Dockerfile` ⭐
- `pyproject.toml` ⭐
- `requirements.in`
- `requirements.txt` ⭐

### 📁 backend/app/
- `__init__.py` — `"""API modular de Chamba."""`
- `main.py` — `import importlib import logging import time`
- `operations.json` — `[ { "set": false,`
- `worker.py` — `"""Durable in-app delivery; no automatic bank transfers or service releases.""" import logging import signal`

### 📁 backend/app/core/
- `__init__.py`
- `db.py` — `import json from contextvars import ContextVar from pathlib import Path`
- `errors.py` — `import logging import re from fastapi import Request, HTTPException`
- `profile.py` — `import re import unicodedata def normalize_professional_slug(value):`
- `routing.py` — `from datetime import datetime, date, time from typing import Any, Annotated from uuid import UUID`
- `security.py` — `import hashlib import threading from datetime import datetime, timedelta, timezone`
- `settings.py` — `from pathlib import Path from typing import Literal from pydantic_settings import BaseSettings, SettingsConfigDict`

### 📁 backend/app/modules/
- `__init__.py`

### 📁 backend/app/modules/account/
- `__init__.py`
- `router.py` — `from app.core.routing import mount router = mount("account")`

### 📁 backend/app/modules/admin/
- `__init__.py`
- `router.py` — `from app.core.routing import mount router = mount("admin")`

### 📁 backend/app/modules/auth/
- `__init__.py`
- `router.py` — `import logging import base64 import json`

### 📁 backend/app/modules/contracts/
- `__init__.py`
- `router.py` — `from app.core.routing import mount router = mount("contracts")`

### 📁 backend/app/modules/files/
- `__init__.py`
- `router.py` — `import hashlib import io from pathlib import Path`

### 📁 backend/app/modules/integrations/
- `__init__.py`
- `router.py` — `"""Boundary for trusted payment results. Sandbox routes never exist in production.""" import hashlib import hmac`

### 📁 backend/app/modules/marketplace/
- `__init__.py`
- `router.py` — `from app.core.routing import mount router = mount("marketplace")`

### 📁 backend/app/modules/professionals/
- `__init__.py`
- `router.py` — `from app.core.routing import mount router = mount("professionals")`

### 📁 backend/app/modules/public/
- `__init__.py`
- `router.py` — `from app.core.routing import mount router = mount("public")`

### 📁 backend/app/modules/requests/
- `__init__.py`
- `router.py` — `from app.core.routing import mount router = mount("requests")`

### 📁 backend/app/modules/support/
- `__init__.py`
- `kyc_ocr.py` — `import io import re import calendar`
- `router.py` — `import base64 from datetime import datetime, timedelta, timezone import hashlib`

### 📁 backend/app/modules/transport/
- `__init__.py` — `# transport module`
- `router.py` — `from app.core.routing import mount router = mount("transport")`

### 📁 backend/app/modules/wallet/
- `__init__.py`
- `router.py` — `from app.core.routing import mount router = mount("wallet")`

### 📁 backend/data/mail/
- `14397c0d-d06f-4bdf-ae41-10d231094e9c.eml`
- `534b5783-59cf-4451-81b0-185b37f85dff.eml`
- `94249062-815b-44d9-baf3-1b5cd4522932.eml`
- `a20b6712-b24e-4907-89f8-6974ac20cecb.eml`
- `b9eb7fd7-5458-4035-821e-009907bfafa2.eml`
- `de00582d-8dbe-4caa-8370-82cd3832b0de.eml`
- `f69fd33b-6b8d-445d-a23c-b5aa5bb444e3.eml`

### 📁 backend/tests/
- `conftest.py` — `import os import sys from pathlib import Path`
- `test_api.py` — `import hashlib import hmac import io`
- `test_customer_requests.py` — `"""Selected offerings, atomic reference photos, quotes and one assignee per customer need.""" from datetime import datet`
- `test_deployment.py` — `"""Public deployments keep HTTPS sessions without enabling real payments.""" import pytest from fastapi import Response`
- `test_explore_map.py` — `"""Map markers preserve branch privacy and the current search scope.""" import json from uuid import uuid4`
- `test_professional_activation.py` — `"""Normalize profile identifiers without weakening uniqueness or welcome-credit rules.""" from concurrent.futures import`
- `test_registration.py` — `"""Registration explains rejected fields without exposing values or creating users.""" from uuid import uuid4 import pyt`
- `test_venezuela.py` — `"""Country hierarchy, private address storage, geographic matching and two independent currencies.""" import json from u`

### 📁 backups/
- `auto-publication-history-20260911.json` — `{ "migration": "028_automatic_request_publication.sql", "scope": "Before and after migration, before public sandbox test`
- `auto-publication-nginx-before-20260911T142100Z.conf`
- `chamba-20260911T022906Z.dump`
- `chamba-20260911T024805Z.dump`
- `chamba-20260911T035102Z.dump`
- `customer-requests-code-20260911T043740Z.json` — `[ { "service": "api",`
- `customer-requests-history-after-20260911T043740Z.json` — `{ "contracts": { "count": 5,`
- `customer-requests-history-before-20260911T043740Z.json` — `{ "contracts": { "count": 5,`
- `demo-relocation-20260911T045343Z.json` — `{ "branches": [ {`
- `demo-relocation-protected-after-20260911T045343Z.json` — `{ "contracts": { "count": 6,`
- `demo-relocation-protected-before-20260911T045343Z.json` — `{ "contracts": { "count": 6,`
- `registration-images-20260911T030840Z.json` — `{ "api": { "image": "sha256:f3dc2cea96ce59d5e6db6efa50d854504b0b49b25412996a56773ad7fe654232",`
- `venezuela-history-after-20260911T035145Z.json` — `[ { "name": "money_entries",`
- `venezuela-history-before-20260911T035145Z.json` — `[ { "name": "money_entries",`
- `venezuela-images-20260911T035145Z.json` — `{ "api": { "image": "sha256:d3fa9aa0edf57032c1b6f159ca9809d64d66b1fcd295ecbc0e907ff7d76be02c",`

### 📁 backups/auto-publication-web-before-20260911T142100Z/
- `50x.html`
- `index.html`

### 📁 backups/auto-publication-web-before-20260911T142100Z/assets/
- `AddressWizard-Bv15CceJ.js` — `import{n as e}from"./rolldown-runtime-CbXtAM7H.js";import{t}from"./arrow-left-DywddBM7.js";import{t as n}from"./arrow-ri`
- `Admin-DuC58MqR.js` — `import{n as e}from"./rolldown-runtime-CbXtAM7H.js";import{t}from"./download-CrExgigu.js";import{O as n,S as r,T as i,X a`
- `Auth-DAAt7LRF.js` — `import{n as e}from"./rolldown-runtime-CbXtAM7H.js";import{t}from"./arrow-left-DywddBM7.js";import{K as n,N as r,O as i,P`
- `Contracts-CEXvYTsd.js` — `import{n as e}from"./rolldown-runtime-CbXtAM7H.js";import{t}from"./arrow-right-laXf01mU.js";import{t as n}from"./Request`
- `Dashboard-DhOOWJYc.js` — `import{t as e}from"./arrow-right-laXf01mU.js";import{B as t,G as n,H as r,K as i,N as a,S as o,T as s,U as c,W as l,_ as`
- `Explore-nUoa_MT-.js` — `import{n as e}from"./rolldown-runtime-CbXtAM7H.js";import{t}from"./arrow-right-laXf01mU.js";import{n}from"./AddressWizar`
- `Marketplace-BJ36Jiga.js` — `import{n as e}from"./rolldown-runtime-CbXtAM7H.js";import{E as t,G as n,K as r,O as i,S as a,T as o,X as s,Y as c,_ as l`
- `Profile-C0SLHuM1.js` — `import{n as e}from"./rolldown-runtime-CbXtAM7H.js";import{t}from"./download-CrExgigu.js";import{t as n}from"./AddressWiz`
- `RequestPhotos-Ds8cttvZ.js` — `import{n as e}from"./rolldown-runtime-CbXtAM7H.js";import{A as t,D as n,G as r,X as i,b as a,i as o,p as s,x as c}from".`
- `Requests-Bxa8wyZF.js` — `import{n as e}from"./rolldown-runtime-CbXtAM7H.js";import{t}from"./arrow-right-laXf01mU.js";import{t as n}from"./Address`
- `Support-D3FF0QiI.js` — `import{n as e}from"./rolldown-runtime-CbXtAM7H.js";import{S as t,X as n,_ as r,a as i,b as a,c as o,d as s,i as c,m as l`
- `Wallet-CPoVoRCn.js` — `import{n as e}from"./rolldown-runtime-CbXtAM7H.js";import{E as t,F as n,G as r,O as i,P as a,S as o,T as s,W as c,X as l`
- `arrow-left-DywddBM7.js` — `import{G as e}from"./index-BYIMTSmP.js";var t=e(`ArrowLeft`,[[`path`,{d:`m12 19-7-7 7-7`,key:`1l729n`}],[`path`,{d:`M19 `
- `arrow-right-laXf01mU.js` — `import{G as e}from"./index-BYIMTSmP.js";var t=e(`ArrowRight`,[[`path`,{d:`M5 12h14`,key:`1ays0h`}],[`path`,{d:`m12 5 7 7`
- `download-CrExgigu.js` — `import{G as e}from"./index-BYIMTSmP.js";var t=e(`Download`,[[`path`,{d:`M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4`,key:``
- `index-BYIMTSmP.js`
- `index-CsUWseO_.css`
- `mapbox-qyXVN1sI.js` — `import{n as e,t}from"./rolldown-runtime-CbXtAM7H.js";var n=t(((e,t)=>{(function(n,r){typeof e==`object`&&t!==void 0?r(e)`
- `mapbox-vh-t_kPv.css`
- `paperclip-BRMr60UH.js` — `import{G as e}from"./index-BYIMTSmP.js";var t=e(`Paperclip`,[[`path`,{d:`M13.234 20.252 21 12.3`,key:`1cbrk9`}],[`path`,`
  *(y 1 archivos más...)*

### 📁 db/migrations/
- `001_model.sql`
- `002_contract_workflows.sql`
- `003_billing_marketplace.sql`
- `004_queries_admin_security.sql`
- `005_catalog.sql`
- `006_media_operations.sql`
- `007_consistency_and_payments.sql`
- `008_default_privileges.sql`
- `009_remote_and_money_model.sql`
- `010_money_wallet_api.sql`
- `011_remote_service_api.sql`
- `012_escrow_and_deliveries.sql`
- `013_money_trigger_integrity.sql`
- `014_delivery_ownership_and_quotes.sql`
- `015_visible_funding_status.sql`
- `016_http_auth_and_portal.sql`
- `017_portal_management.sql`
- `018_admin_withdrawal_queue.sql`
- `019_portal_audit_and_media.sql`
- `020_external_payment_history.sql`
  *(y 27 archivos más...)*

### 📁 deploy/
- `contrata-cert-renew.service`
- `contrata-cert-renew.timer`
- `contrata.http.conf`
- `contrata.nginx.conf`
- `reload-nginx.sh`

### 📁 docs/
- `IMPLEMENTATION_PLAN.md` — `# Plan de implementación · backend Python y portal React ## Objetivo y orden Entregar primero una API modular sobre Post`
- `api-reference.md` — `# Referencia SQL del backend 167 funciones del esquema `api`, generadas desde PostgreSQL. Las funciones no son endpoints`
- `backend-web.md` — `# Backend Python y portal React Implementación local del 11 de septiembre de 2026. El plan está en [IMPLEMENTATION_PLAN.`
- `customer-requests.md` — `# Contratar servicios y publicar necesidades Actualización publicada el 11 de septiembre de 2026 en [contrata.contupana.`
- `data-dictionary.md` — `# Diccionario de datos 85 tablas de negocio del esquema `app`. Generado con `python3 scripts/export_schema_docs.py`. Los`
- `deployment.md` — `# Publicación en contrata.contupana.pro El portal de pruebas se publica en **https://contrata.contupana.pro** desde el 1`
- `explore-map.md` — `# Marcadores en Explorar Explorar abre en la pestana `Listado`. La pestana independiente `Ver mapa` muestra los profesio`
- `integration.md` — `# Integración del portal y las aplicaciones La API HTTP y el portal ya están implementados. Ver [backend y web](backend-`
- `mi-chamba-descripcion-funcional-tecnica.md` — `# Mi Chamba: descripción funcional y técnica ## 1. Qué es Mi Chamba y para qué sirve Mi Chamba es una plataforma web que`
- `model.md` — `# Modelo de Chamba ## Relaciones principales ```mermaid`
- `openapi.json`
- `payments.md` — `# Servicios remotos, pagos protegidos y wallet La regla comercial confirmada es: **se cobra comisión únicamente al retir`
- `profile-activation-and-demo-area.md` — `# Activación de perfiles y muestras cercanas Actualización del 11 de septiembre de 2026 para `contrata.contupana.pro`. #`
- `route-coverage.md` — `# Cobertura de operaciones SQL y HTTP La lista estática del servidor se genera con `python3 scripts/export_http_registry`
- `service-samples.md` — `# Muestras de servicios por categoría El portal sandbox contiene tres muestras por cada una de las 13 categorías: **39 p`
- `venezuela-geolocation.md` — `# Venezuela, direcciones y búsqueda por ubicación Publicado el 11 de septiembre de 2026 en [contrata.contupana.pro](http`
- `verification.md` — `# Verificación de backend, web y base de datos ## Nombre de perfil y muestras cerca de Valera: 11 de septiembre de 2026 `

### 📁 frontend/
- `Dockerfile` ⭐
- `index.html`
- `nginx.conf`
- `package-lock.json` — `{ "name": "chamba-web", "version": "1.0.0",`
- `package.json` ⭐ — `{ "name": "chamba-web", "private": true,`
- `playwright.config.ts` — `import { defineConfig, devices } from "@playwright/test"; export default defineConfig({ testDir: "./e2e",`
- `tsconfig.json` ⭐ — `{ "compilerOptions": { "target": "ES2022",`
- `tsconfig.tsbuildinfo`
- `vite.config.ts` — `import { defineConfig } from "vite"; import react from "@vitejs/plugin-react"; export default defineConfig({`

### 📁 frontend/e2e/
- `customer-requests.spec.ts` — `import { test, expect, type BrowserContext } from "@playwright/test"; import { readFileSync } from "node:fs"; const acce`
- `demo-nearby.spec.ts` — `import { test, expect } from "@playwright/test"; import { readFileSync } from "node:fs"; test("las muestras aparecen a 5`
- `deployment.spec.ts` — `import { test, expect } from "@playwright/test"; import { readFileSync } from "node:fs"; test("HTTPS conserva la sesión,`
- `explore-map-live.spec.ts` — `import { test, expect } from "@playwright/test"; test("mapa publicado usa sedes reales del catalogo y posicion del naveg`
- `explore-map.spec.ts` — `import { test, expect, type Page } from "@playwright/test"; const category = "00000000-0000-4000-8000-000000000001"; con`
- `portal.spec.ts` — `import { test, expect, type Page, type BrowserContext } from "@playwright/test"; import { readFileSync } from "node:fs";`
- `professional-activation.spec.ts` — `import { test, expect } from "@playwright/test"; import { randomBytes, randomUUID } from "node:crypto"; test("activación`
- `registration-live.spec.ts` — `import { test, expect } from "@playwright/test"; import { randomBytes, randomUUID } from "node:crypto"; test("registro H`
- `registration.spec.ts` — `import { test, expect } from "@playwright/test"; test("registro valida la contraseña antes de enviar y muestra el motivo`
- `service-samples.spec.ts` — `import { test, expect } from "@playwright/test"; import { readFileSync } from "node:fs"; const samples = JSON.parse(`
- `venezuela.spec.ts` — `import { test, expect, type Page, type BrowserContext } from "@playwright/test"; import { randomBytes, randomUUID } from`

### 📁 frontend/public/downloads/android/
- `latest.json` — `{ "versionCode": 36, "versionName": "1.3.32",`

### 📁 frontend/public/
- `robots.txt`
- `site.webmanifest`
- `sitemap.xml`

### 📁 frontend/src/
- `AddPayoutMethodModal.tsx` — `import { useState, type FormEvent } from "react"; import { Landmark, Smartphone, CheckCircle2, ShieldCheck, ArrowRight, `
- `AddressWizard.tsx` — `import { useEffect, useRef, useState } from "react"; import L from "leaflet"; import "leaflet/dist/leaflet.css";`
- `ExploreMap.tsx` — `import { useEffect, useRef, useState } from "react"; import L from "leaflet"; import "leaflet/dist/leaflet.css";`
- `PersonalKycWizard.tsx` — `import { useState, useRef, useEffect, type FormEvent } from "react"; import { ShieldCheck,`
- `RequestPhotos.tsx` — `import { useEffect, useState } from "react"; import { ImagePlus, X } from "lucide-react"; import { api, privateFileBlob,`
- `VinkoIcons.tsx` — `import type { SVGProps, ComponentType } from "react"; export interface VinkoIconProps extends SVGProps<SVGSVGElement> { `
- `api.ts` — `import operations from "./operations.json"; export type Data = Record<string, any>; export type User = {`
- `components.tsx` — `import { useEffect, useId, useRef, useState, type ReactNode } from "react"; import { LoaderCircle,`
- `context.tsx` — `import { createContext, useContext,`
- `location.tsx` — `import { createContext, useCallback,`
- `main.tsx` — `import { lazy, Suspense, useEffect, useState } from "react"; import { createRoot } from "react-dom/client"; import {`
- `mapbox.ts` — `import L from "leaflet"; export function mapboxTiles(token: string) { return L.tileLayer(`
- `operations.json` — `{ "accept_direct_request": { "path": "/api/v1/requests/accept-direct-request",`
- `professionalSlug.ts` — `export const professionalSlug = (value: string) => value .toLowerCase()`
- `serviceImages.ts` — `export type ServiceImage = { src: string; alt: string;`
- `styles.css`
- `venezuelanBanks.ts` — `export interface VenezuelanBank { code: string; name: string;`

### 📁 frontend/src/components/
- `AuthShowcase.tsx` — `import React, { useState, useEffect } from "react"; import { Wrench,`
- `CookieConsent.tsx` — `import { useState, useEffect } from "react"; import { Link } from "react-router-dom"; import {`
- `ErrorBoundary.tsx` — `import { Component, type ReactNode } from "react"; const RELOAD_KEY = "vinko_chunk_reload"; function isChunkError(error:`
- `MoneyWallet.tsx` — `import { useMemo, useState } from "react"; import { ArrowDownLeft,`

### 📁 frontend/src/pages/
- `Admin.tsx` — `import { useState } from "react"; import { ShieldCheck,`
- `Auth.tsx` — `import { useState, useEffect, type FormEvent } from "react"; import { Link, useNavigate, useSearchParams } from "react-r`
- `Contracts.tsx` — `import { useEffect, useRef, useState } from "react"; import { Link, useParams } from "react-router-dom"; import {`
- `CookiePolicy.tsx` — `import { Link } from "react-router-dom"; import { Cookie,`
- `Dashboard.tsx` — `import { useMemo, useState, type ComponentType } from "react"; import { Link } from "react-router-dom"; import {`
- `Explore.tsx` — `import { useMemo, useState } from "react"; import { Link,`
- `LegalNotice.tsx` — `import { Link } from "react-router-dom"; import { Scale, ShieldCheck, Mail, ArrowLeft, Building2 } from "lucide-react"; `
- `Marketplace.tsx` — `import { useState, useMemo } from "react"; import { Link, useNavigate, useSearchParams } from "react-router-dom"; import`
- `MobileKycPage.tsx` — `import { useState, useRef, useEffect, type FormEvent, type ChangeEvent } from "react"; import { useSearchParams, Link } `
- `PrivacyPolicy.tsx` — `import { Link } from "react-router-dom"; import { ShieldCheck,`
- `Profile.tsx` — `import { useState } from "react"; import { Link, useNavigate, useSearchParams } from "react-router-dom"; import {`
- `Requests.tsx` — `import { useRef, useState } from "react"; import { Link,`
- `Support.tsx` — `import { useState } from "react"; import { MessageCircle, ArrowRight, LifeBuoy } from "lucide-react"; import { api, date`
- `TermsOfService.tsx` — `import { Link } from "react-router-dom"; import { FileText,`
- `Wallet.tsx` — `import { useState } from "react"; import { Sparkles,`

### 📁 mobile/android/
- `.gitignore`
- `README.md` ⭐ — `# SoyVinko · App Android nativa App Android en **Kotlin + Jetpack Compose (Material 3)** con el diseño del portal: morad`
- `build.gradle.kts`
- `gradle.properties`
- `gradlew`
- `gradlew.bat`
- `local.properties`
- `settings.gradle.kts`

### 📁 mobile/android/app/
- `build.gradle.kts`
- `proguard-rules.pro`

### 📁 mobile/android/app/src/main/
- `AndroidManifest.xml`

### 📁 mobile/android/app/src/main/assets/
- `operations.json` — `[ { "set": false,`

### 📁 mobile/android/app/src/main/res/drawable/
- `ic_launcher_background.xml`
- `ic_launcher_foreground.xml`
- `ic_logo_mark.xml`

### 📁 mobile/android/app/src/main/res/mipmap-anydpi-v26/
- `ic_launcher.xml`

### 📁 mobile/android/app/src/main/res/values/
- `colors.xml`
- `strings.xml`
- `themes.xml`

### 📁 mobile/android/app/src/main/res/xml/
- `data_extraction_rules.xml`
- `file_paths.xml`
- `network_security_config.xml`

### 📁 mobile/android/gradle/
- `libs.versions.toml`

### 📁 mobile/android/gradle/wrapper/
- `gradle-wrapper.properties`

### 📁 mobile/android/iosApp/
- `.gitignore`
- `README.md` ⭐ — `# SoyVinko para iPhone La app de iPhone es la misma que la de Android: todas las pantallas, la API, la sesión, los mapas`
- `project.yml`

### 📁 mobile/android/iosApp/SoyVinko/Assets.xcassets/AccentColor.colorset/
- `Contents.json` — `{ "colors" : [ { "color" : { "color-space" : "srgb", "components" : { "alpha" : "1.000", "red" : "0x6F", "green" : "0x3D`

### 📁 mobile/android/iosApp/SoyVinko/Assets.xcassets/AppIcon.appiconset/
- `Contents.json` — `{ "images" : [ { "filename" : "AppIcon-1024.png", "idiom" : "universal", "platform" : "ios", "size" : "1024x1024" }`

### 📁 mobile/android/iosApp/SoyVinko/Assets.xcassets/
- `Contents.json` — `{ "info" : { "author" : "xcode", "version" : 1 } }`

### 📁 mobile/android/iosApp/SoyVinko/
- `ContentView.swift`
- `SoyVinkoApp.swift`

### 📁 mobile/android/scripts/
- `gen-operations.py` — `#!/usr/bin/env python3 """Regenera shared/.../OperationsRegistry.kt desde frontend/src/operations.json (tras scripts/exp`
- `publish-release.sh`

### 📁 mobile/android/shared/
- `build.gradle.kts`

### 📁 mobile/android/shared/src/commonMain/composeResources/drawable/
- `ic_logo_mark.xml`

### 📁 output/imagegen/services/
- `category-prompts.md` — `# Portadas para completar las 13 categorías Generadas con la herramienta integrada Imagegen el 11 de septiembre de 2026.`
- `prompts.md` — `# Imágenes de ejemplo para las tarjetas de servicios Generadas con la herramienta integrada Imagegen el 11 de septiembre`

### 📁 scripts/
- `bootstrap_backend.py` — `#!/usr/bin/env python3 """Aprovisiona cuentas técnicas restringidas; nunca entrega el propietario al servidor.""" import`
- `db.py` — `#!/usr/bin/env python3 """Administración local reproducible; Python estándar + Docker Compose.""" import argparse`
- `demo_services.json` — `{ "plomeria": [ {`
- `dev.py` — `#!/usr/bin/env python3 """Instalación y operación del entorno local completo de Chamba.""" import argparse`
- `export_http_registry.py` — `#!/usr/bin/env python3 """Generación de contratos revisables; no introspección ni SQL arbitrario en runtime.""" import j`
- `export_schema_docs.py` — `#!/usr/bin/env python3 """Regenera el diccionario y la referencia SQL a partir de la instancia local.""" import json`
- `import_venezuela_geography.py` — `#!/usr/bin/env python3 """Build the pinned INE/OCHA catalog migration; development-only dependency: shapely==2.1.2. Usag`
- `localize_venezuela_samples.py` — `#!/usr/bin/env python3 """Localiza únicamente las muestras conocidas. No cambia usuarios reales ni historiales monetario`
- `relocate_demo_services.py` — `#!/usr/bin/env python3 """Place only recognized sandbox service samples within 50 km of an owned saved address. Dry run `
- `seed_demo.py` — `#!/usr/bin/env python3 """Datos ficticios únicamente en development/sandbox; contraseñas privadas aleatorias.""" import `
- `seed_marketplace_products.py` — `import urllib.request import uuid import hashlib`
- `seed_service_samples.py` — `#!/usr/bin/env python3 """Añade muestras sandbox sin duplicar ni alterar perfiles existentes. Por defecto solo informa d`
- `verify_ve_payouts.py` — `import urllib.request import urllib.error import json`

### 📁 tests/
- `run.py` — `#!/usr/bin/env python3 """Pruebas reales contra una base desechable propia; nunca vacían chamba.""" from concurrent.futu`
- `test_money.py` — `"""Servicios remotos, custodia contable y comisión exclusivamente al retirar.""" from concurrent.futures import ThreadPo`

---

## 🧭 Archivos Clave & Puntos de Entrada
- **`README.md`** (10.6 KB): # Chamba · Backend Python y portal React Portal para conectar clientes con profesionales, con API modular FastAPI, web R
- **`backend/Dockerfile`** (0.6 KB)
- **`backend/app/__init__.py`** (0.0 KB): """API modular de Chamba."""
- **`backend/app/core/__init__.py`** (0.0 KB)
- **`backend/app/core/db.py`** (2.7 KB): import json from contextvars import ContextVar from pathlib import Path
- **`backend/app/core/errors.py`** (16.9 KB): import logging import re from fastapi import Request, HTTPException
- **`backend/app/core/profile.py`** (0.4 KB): import re import unicodedata def normalize_professional_slug(value):
- **`backend/app/core/routing.py`** (3.8 KB): from datetime import datetime, date, time from typing import Any, Annotated from uuid import UUID
- **`backend/app/core/security.py`** (3.9 KB): import hashlib import threading from datetime import datetime, timedelta, timezone
- **`backend/app/core/settings.py`** (2.7 KB): from pathlib import Path from typing import Literal from pydantic_settings import BaseSettings, SettingsConfigDict
- **`backend/app/main.py`** (4.4 KB): import importlib import logging import time
- **`backend/app/modules/__init__.py`** (0.0 KB)
- **`backend/app/modules/account/__init__.py`** (0.0 KB)
- **`backend/app/modules/account/router.py`** (0.1 KB): from app.core.routing import mount router = mount("account")
- **`backend/app/modules/admin/__init__.py`** (0.0 KB)
- **`backend/app/modules/admin/router.py`** (0.1 KB): from app.core.routing import mount router = mount("admin")
- **`backend/app/modules/auth/__init__.py`** (0.0 KB)
- **`backend/app/modules/auth/router.py`** (27.6 KB): import logging import base64 import json
- **`backend/app/modules/contracts/__init__.py`** (0.0 KB)
- **`backend/app/modules/contracts/router.py`** (0.1 KB): from app.core.routing import mount router = mount("contracts")
- **`backend/app/modules/files/__init__.py`** (0.0 KB)
- **`backend/app/modules/files/router.py`** (4.2 KB): import hashlib import io from pathlib import Path
- **`backend/app/modules/integrations/__init__.py`** (0.0 KB)
- **`backend/app/modules/integrations/router.py`** (16.5 KB): """Boundary for trusted payment results. Sandbox routes never exist in production.""" import hashlib import hmac
- **`backend/app/modules/marketplace/__init__.py`** (0.0 KB)
- **`backend/app/modules/marketplace/router.py`** (0.1 KB): from app.core.routing import mount router = mount("marketplace")
- **`backend/app/modules/professionals/__init__.py`** (0.0 KB)
- **`backend/app/modules/professionals/router.py`** (0.1 KB): from app.core.routing import mount router = mount("professionals")
- **`backend/app/modules/public/__init__.py`** (0.0 KB)
- **`backend/app/modules/public/router.py`** (0.1 KB): from app.core.routing import mount router = mount("public")
- **`backend/app/modules/requests/__init__.py`** (0.0 KB)
- **`backend/app/modules/requests/router.py`** (0.1 KB): from app.core.routing import mount router = mount("requests")
- **`backend/app/modules/support/__init__.py`** (0.0 KB)
- **`backend/app/modules/support/kyc_ocr.py`** (7.1 KB): import io import re import calendar
- **`backend/app/modules/support/router.py`** (9.2 KB): import base64 from datetime import datetime, timedelta, timezone import hashlib
- **`backend/app/modules/transport/__init__.py`** (0.0 KB): # transport module
- **`backend/app/modules/transport/router.py`** (0.1 KB): from app.core.routing import mount router = mount("transport")
- **`backend/app/modules/wallet/__init__.py`** (0.0 KB)
- **`backend/app/modules/wallet/router.py`** (0.1 KB): from app.core.routing import mount router = mount("wallet")
- **`backend/app/operations.json`** (104.5 KB): [ { "set": false,
- **`backend/app/worker.py`** (2.6 KB): """Durable in-app delivery; no automatic bank transfers or service releases.""" import logging import signal
- **`backend/pyproject.toml`** (0.1 KB)
- **`backend/requirements.txt`** (0.8 KB)
- **`backups/auto-publication-web-before-20260911T142100Z/assets/index-BYIMTSmP.js`** (294.4 KB)
- **`backups/auto-publication-web-before-20260911T142100Z/assets/index-CsUWseO_.css`** (43.3 KB)
- **`backups/auto-publication-web-before-20260911T142100Z/index.html`** (0.6 KB)
- **`db/migrations/040_upload_auto_approval.sql`** (1.4 KB)
- **`frontend/Dockerfile`** (0.3 KB)
- **`frontend/index.html`** (4.5 KB)
- **`frontend/package.json`** (0.7 KB): { "name": "chamba-web", "private": true,
- **`frontend/src/main.tsx`** (26.5 KB): import { lazy, Suspense, useEffect, useState } from "react"; import { createRoot } from "react-dom/client"; import {
- **`frontend/tsconfig.json`** (0.5 KB): { "compilerOptions": { "target": "ES2022",
- **`mobile/android/README.md`** (7.1 KB): # SoyVinko · App Android nativa App Android en **Kotlin + Jetpack Compose (Material 3)** con el diseño del portal: morad
- **`mobile/android/app/build.gradle.kts`** (4.4 KB)
- **`mobile/android/app/proguard-rules.pro`** (0.4 KB)
- **`mobile/android/app/src/main/AndroidManifest.xml`** (3.1 KB)
- **`mobile/android/app/src/main/assets/operations.json`** (101.4 KB): [ { "set": false,
- **`mobile/android/app/src/main/res/drawable/ic_launcher_background.xml`** (0.2 KB)
- **`mobile/android/app/src/main/res/drawable/ic_launcher_foreground.xml`** (0.5 KB)
- **`mobile/android/app/src/main/res/drawable/ic_logo_mark.xml`** (0.6 KB)
- **`mobile/android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`** (0.3 KB)
- **`mobile/android/app/src/main/res/values/colors.xml`** (0.2 KB)
- **`mobile/android/app/src/main/res/values/strings.xml`** (0.1 KB)
- **`mobile/android/app/src/main/res/values/themes.xml`** (0.7 KB)
- **`mobile/android/app/src/main/res/xml/data_extraction_rules.xml`** (0.3 KB)
- **`mobile/android/app/src/main/res/xml/file_paths.xml`** (0.2 KB)
- **`mobile/android/app/src/main/res/xml/network_security_config.xml`** (0.3 KB)
- **`mobile/android/gradle/wrapper/gradle-wrapper.properties`** (0.2 KB)
- **`mobile/android/iosApp/README.md`** (1.9 KB): # SoyVinko para iPhone La app de iPhone es la misma que la de Android: todas las pantallas, la API, la sesión, los mapas
- **`mobile/android/iosApp/SoyVinko/Assets.xcassets/AppIcon.appiconset/Contents.json`** (0.2 KB): { "images" : [ { "filename" : "AppIcon-1024.png", "idiom" : "universal", "platform" : "ios", "size" : "1024x1024" }

---
*💡 Nota para el modelo: Usa este mapa para ubicar archivos directamente sin gastar pasos en listar directorios.*
