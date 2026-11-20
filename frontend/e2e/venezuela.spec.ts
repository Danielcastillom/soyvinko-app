import { test, expect, type Page, type BrowserContext } from "@playwright/test";
import { randomBytes, randomUUID } from "node:crypto";

async function chooseCaracas(page: Page) {
  await page.getByRole("button", { name: "Continuar", exact: true }).click();
  await page.getByLabel(/Estado o entidad federal/).selectOption("VE01");
  await page.getByRole("button", { name: "Continuar", exact: true }).click();
  await page.getByLabel(/^Municipio/).selectOption("VE0101");
  await page.getByRole("button", { name: "Continuar", exact: true }).click();
  await page.getByLabel(/^Parroquia/).selectOption("VE010109");
  await page.getByRole("button", { name: "Continuar", exact: true }).click();
  await expect(
    page.getByRole("region", { name: "Mapa para elegir la dirección" }),
  ).toBeVisible();
  await expect(page.locator(".leaflet-tile-loaded").first()).toBeAttached();
}

async function newTestAccount(context: BrowserContext) {
  const config = (await (await context.request.get("/api/v1/config")).json())
    .data;
  test.skip(
    config.environment !== "test" && process.env.CHAMBA_GEO_MUTATION !== "1",
    "El alta de la cuenta ficticia requiere habilitar el recorrido de pruebas.",
  );
  expect(config.payment_mode).toBe("sandbox");
  const result = await context.request.post("/api/v1/auth/register", {
    data: {
      name: "Prueba direcciones Venezuela",
      email: `geo-${randomUUID()}@example.com`,
      password: randomBytes(24).toString("base64url"),
      client: "web",
    },
  });
  expect(result.status()).toBe(201);
}

test("selector encadenado y mapa Mapbox permiten elegir zona sin permiso de ubicación", async ({
  page,
}) => {
  await page.addInitScript(() => {
    navigator.geolocation.watchPosition = (_ok, fail) => {
      queueMicrotask(() =>
        fail?.({ code: 1, message: "denied" } as GeolocationPositionError),
      );
      return 1;
    };
  });
  await page.goto("/explore");
  await expect(page.locator(".nearby-location-copy")).toContainText(
    "Puedes permitir la ubicación",
  );
  await page.getByRole("button", { name: "Elegir zona en el mapa" }).click();
  await expect(page.getByLabel("País", { exact: true })).toBeDisabled();
  await chooseCaracas(page);
  for (let i = 0; i < 3; i++)
    await page.getByRole("button", { name: "Anterior", exact: true }).click();
  await page.getByLabel(/Estado o entidad federal/).selectOption("VE23");
  await page.getByRole("button", { name: "Continuar", exact: true }).click();
  await expect(page.getByLabel(/^Municipio/)).toHaveValue("");
  await page.getByLabel(/^Municipio/).selectOption("VE2313");
  await page.getByRole("button", { name: "Continuar", exact: true }).click();
  await expect(page.getByLabel(/^Parroquia/)).toHaveValue("");
  await expect(
    page.getByLabel(/^Parroquia/).locator('option[value="VE010109"]'),
  ).toHaveCount(0);
  await page.getByLabel(/^Parroquia/).selectOption("VE231314");
  await page.getByRole("button", { name: "Continuar", exact: true }).click();
  await expect(page.locator(".address-summary")).toContainText(
    "Olegario Villalobos",
  );
  await expect(
    page.getByRole("button", { name: "Buscar aquí", exact: true }),
  ).toBeDisabled();
  await page
    .getByRole("button", { name: "Fijar en el centro del mapa" })
    .click();
  await expect(page.locator(".address-map-pin")).toBeVisible();
  await page.getByRole("button", { name: "Buscar aquí", exact: true }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(page.locator(".nearby-location-copy")).toContainText(
    "Olegario Villalobos, Zulia",
  );
});

test("dirección móvil se guarda con nombre y jerarquía; wallet y solicitudes usan Bs y USD", async ({
  page,
  context,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await newTestAccount(context);
  await page.goto("/profile");
  await page
    .getByRole("button", { name: "Añadir dirección", exact: true })
    .click();
  await page.getByLabel(/Nombre para esta dirección/).fill("Trabajo de prueba");
  await chooseCaracas(page);
  await expect(page.getByLabel("Latitud", { exact: true })).toHaveCount(0);
  await page
    .getByRole("button", { name: "Fijar en el centro del mapa" })
    .click();
  await page
    .getByLabel(/Dirección y número/)
    .fill("Dirección ficticia de prueba, oficina 2");
  await page.getByLabel(/Indicaciones para llegar/).fill("Entrada de ejemplo");
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: "../docs/screenshots/venezuela-address-mobile.png",
    fullPage: true,
  });
  await page
    .getByRole("button", { name: "Guardar dirección", exact: true })
    .click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.reload();
  const card = page
    .locator(".list-row")
    .filter({ has: page.getByRole("heading", { name: "Trabajo de prueba" }) });
  await expect(card).toContainText("El Recreo, Libertador, Distrito Capital");
  await page.goto("/requests/new?mode=remote&currency=USD&amount=25.5");
  await expect(page.getByLabel(/^Moneda/)).toHaveValue("USD");
  await expect(page.getByLabel(/Presupuesto ofrecido/)).toHaveValue("25.5");
  await page.goto("/wallet");
  await expect(page.getByLabel("Moneda de wallet")).toHaveValue("VES");
  await expect(page.locator(".wallet-amount")).toContainText("Bs");
  await expect(
    page.getByLabel("Moneda de wallet").locator("option"),
  ).toHaveCount(2);
  await page.getByLabel("Moneda de wallet").selectOption("USD");
  await expect(page.locator(".wallet-amount")).toContainText("US$");
  await page.goto("/profile");
  await card.getByRole("button", { name: "Archivar", exact: true }).click();
  await expect(card).toHaveCount(0);
});

test("ubicación del navegador actualiza la zona y envía coordenadas por POST", async ({
  page,
  context,
}) => {
  await context.grantPermissions(["geolocation"]);
  await context.setGeolocation({
    latitude: 10.516828,
    longitude: -66.877321,
    accuracy: 20,
  });
  const searches: { method: string; url: string; lat: number; lon: number }[] =
    [];
  page.on("request", (req) => {
    if (req.url().endsWith("/public/nearby-professionals"))
      searches.push({
        method: req.method(),
        url: req.url(),
        ...req.postDataJSON(),
      });
  });
  await page.goto("/explore");
  await expect(page.locator(".location-status")).toContainText(
    "El Recreo, Distrito Capital",
  );
  await expect.poll(() => searches.length).toBeGreaterThan(0);
  await context.setGeolocation({
    latitude: 10.677139,
    longitude: -71.606321,
    accuracy: 20,
  });
  await expect(page.locator(".location-status")).toContainText(
    "Olegario Villalobos, Zulia",
  );
  await expect.poll(() => searches.at(-1)?.lon).toBe(-71.606321);
  expect(
    searches.every((r) => r.method === "POST" && !r.url.includes("?")),
  ).toBe(true);
  await context.setGeolocation({
    latitude: 4.65,
    longitude: -74.06,
    accuracy: 20,
  });
  await expect(page.locator(".nearby-location-copy")).toContainText(
    "disponibles actualmente en Venezuela",
  );
});

test("reintentar ubicación recupera la zona tras un fallo con las mismas coordenadas", async ({
  page,
  context,
}) => {
  await context.grantPermissions(["geolocation"]);
  await context.setGeolocation({
    latitude: 10.516828,
    longitude: -66.877321,
    accuracy: 20,
  });
  let attempts = 0;
  await page.route("**/api/v1/public/locate-parish", async (route) => {
    attempts += 1;
    if (attempts === 1) {
      await route.fulfill({
        status: 503,
        contentType: "application/json",
        body: JSON.stringify({
          error: { code: "service_unavailable", message: "Prueba de conexión" },
        }),
      });
    } else {
      await route.continue();
    }
  });
  await page.goto("/explore");
  await expect(page.locator(".nearby-location-copy")).toContainText(
    "No pudimos identificar tu zona",
  );
  await page
    .locator(".location-status")
    .getByRole("button", { name: "Usar mi ubicación", exact: true })
    .click();
  await expect(page.locator(".location-status")).toContainText(
    "El Recreo, Distrito Capital",
  );
  expect(attempts).toBeGreaterThanOrEqual(2);
});
