import { test, expect } from "@playwright/test";

test("mapa publicado usa sedes reales del catalogo y posicion del navegador", async ({
  page,
  context,
}) => {
  test.skip(
    process.env.CHAMBA_MAP_LIVE !== "1",
    "Requiere el catalogo de demostracion publicado.",
  );
  const config = (await (await context.request.get("/api/v1/config")).json())
    .data;
  expect(config.payment_mode).toBe("sandbox");
  await context.grantPermissions(["geolocation"]);
  await context.setGeolocation({
    latitude: 9.308468,
    longitude: -70.604217,
    accuracy: 20,
  });
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  const responses: { status: number; method: string; count: number }[] = [];
  page.on("response", async (response) => {
    if (response.url().endsWith("/professional-map-markers"))
      responses.push({
        status: response.status(),
        method: response.request().method(),
        count: (await response.json()).data?.length ?? 0,
      });
  });
  await page.goto("/explore");
  await expect(page.locator(".location-status")).toContainText(
    "Juan Ignacio Montilla",
  );
  await page.getByRole("button", { name: "Presenciales", exact: true }).click();
  await page.getByLabel("Radio de búsqueda").selectOption("50000");
  const search = { lat: 9.308468, lon: -70.604217, radius_m: 50000 };
  const nearby = (
    await (
      await context.request.post("/api/v1/public/nearby-professionals", {
        data: search,
      })
    ).json()
  ).data;
  expect(nearby.length).toBeGreaterThan(0);
  await expect(page.locator(".professional-card")).toHaveCount(nearby.length);
  await expect(page.locator(".explore-map-section")).toHaveCount(0);
  await page.getByRole("tab", { name: "Ver mapa", exact: true }).click();
  await expect(page.locator(".professional-card")).toHaveCount(0);
  await expect(page.locator(".explore-map-status")).toContainText(
    `${nearby.length} profesionales en el mapa`,
  );
  const map = page.getByRole("region", { name: "Mapa de servicios cercanos" });
  await expect(map.locator(".explore-location-pin")).toBeVisible();
  await expect(map.locator(".leaflet-tile-loaded").first()).toBeAttached();
  await page.getByRole("button", { name: "Ver todos los marcadores" }).click();
  await page
    .locator(".explore-map-section")
    .screenshot({ path: "../output/explore-map-live-desktop.png" });
  const categories = (
    await (await context.request.get("/api/v1/public/catalog")).json()
  ).data;
  const plumbing = categories.find(
    (c: { slug: string }) => c.slug === "plomeria",
  ).id;
  const filtered = (
    await (
      await context.request.post("/api/v1/public/nearby-professionals", {
        data: { ...search, category: plumbing },
      })
    ).json()
  ).data;
  expect(filtered.length).toBeGreaterThan(0);
  await page.getByLabel("Categoría", { exact: true }).selectOption(plumbing);
  await expect(page.locator(".explore-map-status")).toContainText(
    `${filtered.length} profesionales en el mapa`,
  );
  await page.getByRole("tab", { name: "Listado", exact: true }).click();
  await expect(page.locator(".professional-card")).toHaveCount(filtered.length);
  await expect(page.getByLabel("Categoría", { exact: true })).toHaveValue(
    plumbing,
  );
  await page.getByRole("tab", { name: "Ver mapa", exact: true }).click();
  await expect(page.locator(".professional-card")).toHaveCount(0);
  await map.locator(".explore-service-pin").first().click();
  await expect(
    map.getByRole("link", { name: "Ver perfil" }).first(),
  ).toBeVisible();
  await map.locator(".leaflet-popup-close-button").click();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole("button", { name: "Ver todos los marcadores" }).click();
  await expect
    .poll(() => map.locator("img.leaflet-tile").count())
    .toBeGreaterThan(0);
  await expect(
    map.locator("img.leaflet-tile:not(.leaflet-tile-loaded)"),
  ).toHaveCount(0);
  await page
    .locator(".explore-map-section")
    .screenshot({ path: "../output/explore-map-live-mobile.png" });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  expect(responses.length).toBeGreaterThan(0);
  expect(
    responses.every(
      (response) => response.status === 200 && response.method === "POST",
    ),
  ).toBe(true);
  expect(errors).toEqual([]);
});
