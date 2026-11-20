import { test, expect, type Page } from "@playwright/test";

const category = "00000000-0000-4000-8000-000000000001";
const profiles = [
  {
    id: "00000000-0000-4000-8000-000000000011",
    name: "Laura Navarro",
    bio: "Electricidad",
    verified: true,
    offerings: [
      {
        category_id: category,
        title: "Instalaciones eléctricas",
        delivery_mode: "onsite",
        currency: "USD",
        price_min_minor: 2500,
      },
    ],
  },
  {
    id: "00000000-0000-4000-8000-000000000012",
    name: "Pedro Rivas",
    bio: "Reparaciones",
    verified: false,
    offerings: [
      {
        category_id: category,
        title: "Revisión eléctrica",
        delivery_mode: "onsite",
        currency: "USD",
        price_min_minor: 1500,
      },
    ],
  },
  {
    id: "00000000-0000-4000-8000-000000000013",
    name: "Lucía Martínez",
    bio: "Diseño",
    verified: false,
    offerings: [
      {
        category_id: category,
        title: "Diseño remoto",
        delivery_mode: "remote",
        currency: "USD",
        price_min_minor: 5000,
      },
    ],
  },
];

async function listings(page: Page) {
  await page.route("**/api/v1/public/discover**", (route) =>
    route.fulfill({
      json: {
        data: profiles.filter(
          (p) =>
            !new URL(route.request().url()).searchParams.get("mode") ||
            p.offerings[0].delivery_mode ===
              new URL(route.request().url()).searchParams.get("mode"),
        ),
      },
    }),
  );
  await page.route("**/api/v1/public/nearby-professionals", (route) =>
    route.fulfill({
      json: {
        data: profiles.filter((p) => p.offerings[0].delivery_mode === "onsite"),
      },
    }),
  );
  await page.route("**/api/v1/public/professional-map-markers", (route) =>
    route.fulfill({
      json: {
        data: profiles
          .filter(
            (p) =>
              p.offerings[0].delivery_mode === "onsite" &&
              route.request().postDataJSON().professionals.includes(p.id),
          )
          .map((p) => ({
            professional_id: p.id,
            lat: 10.52,
            lon: -66.88,
            approximate: true,
            label: "El Recreo, Libertador",
          })),
      },
    }),
  );
}

test("mapa agrupa profesionales, abre perfiles y responde a filtros y ubicación", async ({
  page,
  context,
}) => {
  await context.grantPermissions(["geolocation"]);
  await context.setGeolocation({
    latitude: 10.516828,
    longitude: -66.877321,
    accuracy: 20,
  });
  await listings(page);
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/explore");
  const map = page.getByRole("region", { name: "Mapa de servicios cercanos" });
  await expect(
    page.getByRole("tab", { name: "Listado", exact: true }),
  ).toHaveAttribute("aria-selected", "true");
  await expect(page.locator(".professional-card")).toHaveCount(3);
  await expect(map).toHaveCount(0);
  await page.getByRole("tab", { name: "Ver mapa", exact: true }).click();
  await expect(map).toBeVisible();
  await expect(page.locator(".professional-card")).toHaveCount(0);
  await expect(page.locator(".explore-map-status")).toContainText(
    "2 profesionales",
  );
  await expect(map.locator(".explore-service-pin")).toHaveCount(1);
  await expect(map.locator(".explore-location-pin")).toHaveCount(1);
  await expect(map.locator(".explore-search-pin")).toHaveCount(0);
  await expect(map.locator(".leaflet-tile-loaded").first()).toBeVisible();
  await map
    .getByRole("button", { name: "2 profesionales en esta zona" })
    .click();
  await expect(map.locator(".leaflet-popup")).toContainText("Laura Navarro");
  await expect(map.locator(".leaflet-popup")).toContainText("Pedro Rivas");
  await expect(
    map.getByRole("link", { name: "Ver perfil" }).first(),
  ).toHaveAttribute("href", `/professionals/${profiles[0].id}`);
  await page
    .locator(".explore-map-section")
    .screenshot({ path: "../output/explore-map-desktop.png" });
  await page.getByLabel("Solo verificados").check();
  await expect(page.locator(".explore-map-status")).toContainText(
    "1 profesional en el mapa",
  );
  await page.getByLabel("Buscar profesionales").fill("No existe");
  await expect(map.locator(".explore-service-pin")).toHaveCount(0);
  await expect(map.locator(".explore-location-pin")).toHaveCount(1);
  await page.getByLabel("Buscar profesionales").fill("");
  await context.setGeolocation({
    latitude: 10.5173,
    longitude: -66.876,
    accuracy: 15,
  });
  await page.getByRole("button", { name: "Centrar en mi ubicación" }).click();
  await expect(map.locator(".explore-location-pin")).toBeVisible();
  await page.getByRole("button", { name: "Remotos", exact: true }).click();
  await expect(map).toHaveCount(0);
  await expect(
    page.getByRole("tab", { name: "Ver mapa", exact: true }),
  ).toBeDisabled();
  await page.getByRole("button", { name: "Todos", exact: true }).click();
  await expect(map).toHaveCount(0);
  await page.getByRole("tab", { name: "Ver mapa", exact: true }).click();
  await expect(map).toBeVisible();
  await page
    .getByRole("tab", { name: "Ver mapa", exact: true })
    .press("ArrowLeft");
  await expect(map).toHaveCount(0);
  await expect(page.locator(".professional-card")).toHaveCount(1);
  await expect(page.getByLabel("Solo verificados")).toBeChecked();
  await page
    .getByRole("tab", { name: "Listado", exact: true })
    .press("ArrowRight");
  await expect(map).toBeVisible();
  expect(errors).toEqual([]);
});

test("móvil conserva zona manual y ubicación actual como marcadores distintos", async ({
  page,
  context,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await context.grantPermissions(["geolocation"]);
  await context.setGeolocation({
    latitude: 10.516828,
    longitude: -66.877321,
    accuracy: 20,
  });
  await listings(page);
  await page.goto("/explore");
  await expect(page.locator(".professional-card")).toHaveCount(3);
  await expect(page.locator(".explore-map-section")).toHaveCount(0);
  await page.getByRole("button", { name: "Elegir zona en el mapa" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByRole("button", { name: "Continuar", exact: true }).click();
  await dialog.getByLabel(/Estado o entidad federal/).selectOption("VE01");
  await dialog.getByRole("button", { name: "Continuar", exact: true }).click();
  await dialog.getByLabel(/^Municipio/).selectOption("VE0101");
  await dialog.getByRole("button", { name: "Continuar", exact: true }).click();
  await dialog.getByLabel(/^Parroquia/).selectOption("VE010109");
  await dialog.getByRole("button", { name: "Continuar", exact: true }).click();
  await dialog
    .getByRole("button", { name: "Fijar en el centro del mapa" })
    .click();
  await dialog
    .getByRole("button", { name: "Buscar aquí", exact: true })
    .click();
  await expect(dialog).toHaveCount(0);
  await page.getByRole("tab", { name: "Ver mapa", exact: true }).click();
  const map = page.getByRole("region", { name: "Mapa de servicios cercanos" });
  await expect(map.locator(".explore-search-pin")).toHaveCount(1);
  await expect(map.locator(".explore-location-pin")).toHaveCount(1);
  await map
    .getByRole("button", { name: "2 profesionales en esta zona" })
    .click();
  await expect(map.locator(".leaflet-popup")).toBeVisible();
  await expect(map.locator(".leaflet-tile-loaded").first()).toBeAttached();
  await map.locator(".leaflet-popup-close-button").click();
  await page.getByRole("button", { name: "Ver todos los marcadores" }).click();
  await expect
    .poll(async () => {
      const box = await map.boundingBox();
      const pins = await map
        .locator(".leaflet-marker-icon")
        .evaluateAll((elements) =>
          elements.map((element) => {
            const rect = element.getBoundingClientRect();
            return {
              x: rect.x,
              y: rect.y,
              right: rect.right,
              bottom: rect.bottom,
            };
          }),
        );
      return (
        !!box &&
        pins.length >= 3 &&
        pins.every(
          (pin) =>
            pin.x >= box.x &&
            pin.y >= box.y &&
            pin.right <= box.x + box.width &&
            pin.bottom <= box.y + box.height,
        )
      );
    })
    .toBe(true);
  await page
    .locator(".explore-map-section")
    .screenshot({ path: "../output/explore-map-mobile.png" });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.getByRole("button", { name: "Centrar en mi ubicación" }).click();
  await expect(page.getByLabel("Dirección para buscar")).toHaveValue("manual");
});

test("ubicación denegada no inventa posición y un fallo de marcadores permite reintentar", async ({
  page,
}) => {
  await page.addInitScript(() => {
    navigator.geolocation.watchPosition = (_ok, fail) => {
      queueMicrotask(() =>
        fail?.({ code: 1, message: "denied" } as GeolocationPositionError),
      );
      return 1;
    };
    navigator.geolocation.getCurrentPosition = (_ok, fail) =>
      fail?.({ code: 1, message: "denied" } as GeolocationPositionError);
  });
  await listings(page);
  let failed = false;
  await page.route(
    "**/api/v1/public/professional-map-markers",
    async (route) => {
      if (!failed) {
        failed = true;
        await route.fulfill({
          status: 503,
          json: { error: { message: "No se pudieron cargar los marcadores" } },
        });
      } else await route.fallback();
    },
  );
  await page.goto("/explore");
  await page.getByRole("tab", { name: "Ver mapa", exact: true }).click();
  const section = page.locator(".explore-map-section");
  await expect(section).toContainText("No se pudieron cargar los marcadores");
  await expect(section.locator(".explore-location-pin")).toHaveCount(0);
  await section.getByRole("button", { name: /Reintentar/ }).click();
  await expect(section).toContainText("2 profesionales en el mapa");
  await section
    .getByRole("button", { name: "Centrar en mi ubicación" })
    .click();
  await expect(section).toContainText("Permite la ubicación en tu navegador");
  await expect(section.locator(".explore-location-pin")).toHaveCount(0);
});
