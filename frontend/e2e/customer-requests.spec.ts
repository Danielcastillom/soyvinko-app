import { test, expect, type BrowserContext } from "@playwright/test";
import { readFileSync } from "node:fs";
const access = readFileSync(
  process.env.CHAMBA_TEST_ACCESS_FILE ||
    new URL("../../.local-access.md", import.meta.url),
  "utf8",
);
function credentials(role: string) {
  const row = access.split("\n").find((l) => l.startsWith(`- ${role}:`))!;
  const matches = [...row.matchAll(/`([^`]+)`/g)].map((m) => m[1]);
  return { email: matches[0], password: matches[1] };
}
async function login(context: BrowserContext, role = "cliente") {
  const config = (await (await context.request.get("/api/v1/config")).json())
    .data;
  expect(config.payment_mode).toBe("sandbox");
  expect(["test", "staging", "development"]).toContain(config.environment);
  const response = await context.request.post("/api/v1/auth/login", {
    data: { ...credentials(role), client: "web" },
  });
  expect(response.ok()).toBe(true);
  const apiSession = await context.request.post("/api/v1/auth/login", {
    data: { ...credentials(role), client: "native" },
  });
  expect(apiSession.ok()).toBe(true);
  return (await apiSession.json()).data.access_token as string;
}
async function call(
  context: BrowserContext,
  token: string,
  path: string,
  body?: any,
) {
  const response = await context.request[body === undefined ? "get" : "post"](
    `/api/v1/${path}`,
    {
      headers: { Authorization: `Bearer ${token}` },
      ...(body === undefined ? {} : { data: body }),
    },
  );
  expect(response.ok(), await response.text()).toBe(true);
  return (await response.json()).data;
}
const photo = {
  name: "falla-de-ejemplo.webp",
  mimeType: "image/webp",
  buffer: readFileSync(
    new URL("../public/images/services/electricidad-v1.webp", import.meta.url),
  ),
};

test("contratación presencial conserva el servicio, fotos y solicitud única al reintentar", async ({
  page,
  context,
  browser,
}) => {
  const token = await login(context);
  const profiles = (
    await (
      await context.request.get("/api/v1/public/discover?limit=100")
    ).json()
  ).data;
  const pro = profiles.find((p: any) => p.slug === "plomero-demo");
  const offering = pro.offerings[0];
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(`/professionals/${pro.id}`);
  await page.getByRole("link", { name: "Solicitar servicio" }).first().click();
  await expect(page).toHaveURL(new RegExp(`offering=${offering.id}`));
  await expect(page.locator(".selected-service-summary")).toContainText(
    offering.title,
  );
  for (const label of [
    /^Categoría/,
    /Título de tu solicitud/,
    /Radio de búsqueda/,
    /Presupuesto/,
  ])
    await expect(page.getByLabel(label)).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: "Remoto", exact: true }),
  ).toHaveCount(0);
  const description = `Prueba ficticia ${crypto.randomUUID().slice(0, 8)}: hay una fuga en el grifo de la cocina.`;
  await page.getByLabel(/Explica la falla/).fill(description);
  await page.getByLabel(/Moneda de pago/).selectOption("VES");
  const addresses = await call(context, token, "account/my-account");
  const address = addresses.addresses.find(
    (a: any) => a.country_code === "VE" && a.label === "Casa de ejemplo",
  );
  await page.getByLabel(/Dirección del servicio/).selectOption(address.id);
  await page
    .getByLabel("Fotos de referencia", { exact: true })
    .setInputFiles([photo, { ...photo, name: "detalle-de-la-falla.webp" }]);
  await expect(page.locator(".request-photo-input img")).toHaveCount(2);
  await page
    .getByRole("button", { name: "Quitar foto 2", exact: true })
    .click();
  await expect(page.locator(".request-photo-input img")).toHaveCount(1);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: "../docs/screenshots/request-offering-mobile.png",
    fullPage: true,
  });
  const before = await call(context, token, "requests/my-requests");
  const professionalContext = await browser.newContext({
    baseURL: new URL(page.url()).origin,
  });
  const professionalToken = await login(professionalContext, "plomero");
  const professionalPage = await professionalContext.newPage();
  await professionalPage.goto("/opportunities");
  await expect(
    professionalPage.getByRole("heading", { name: "Solicitudes de clientes" }),
  ).toBeVisible();
  await expect(
    professionalPage.getByText(description, { exact: true }),
  ).toHaveCount(0);
  let attempts = 0;
  await page.route("**/api/v1/requests/request-offering", async (route) => {
    attempts += 1;
    if (attempts === 1) {
      await route.fetch();
      await route.abort("failed");
    } else await route.continue();
  });
  await page
    .getByRole("button", { name: "Enviar solicitud", exact: true })
    .click();
  await expect(page.locator(".fields-form .error-box")).toBeVisible();
  await expect(page.getByLabel(/Explica la falla/)).toHaveValue(
    /fuga en el grifo/,
  );
  await page
    .getByRole("button", { name: "Enviar solicitud", exact: true })
    .click();
  await expect(page).toHaveURL(/\/requests\/[a-f0-9-]+$/);
  const ident = page.url().split("/").pop()!;
  const rows = await call(context, token, "requests/my-requests");
  expect(rows.length).toBe(before.length + 1);
  const saved = rows.find((r: any) => r.id === ident);
  expect(saved.offering_id).toBe(offering.id);
  expect(saved.currency).toBe("VES");
  expect(saved.budget_max_minor).toBeNull();
  expect(saved.attachments).toHaveLength(1);
  expect(saved.status).toBe("open");
  expect(saved.moderation_status).toBe("approved");
  expect(saved.attachments[0].moderation_status).toBe("approved");
  await expect(
    page
      .getByRole("status")
      .filter({ hasText: "Solicitud enviada al profesional" }),
  ).toBeVisible();
  const feed = await call(
    professionalContext,
    professionalToken,
    "requests/service-feed?delivery_mode=onsite",
  );
  expect(feed.filter((r: any) => r.request_id === ident)).toHaveLength(1);
  const image = await professionalContext.request.get(
    `/api/v1/files/${saved.attachments[0].file_id}`,
    {
      headers: { Authorization: `Bearer ${professionalToken}` },
    },
  );
  expect(image.ok()).toBe(true);
  await expect(
    professionalPage.getByText(description, { exact: true }),
  ).toBeVisible({ timeout: 20000 });
  await professionalPage.screenshot({
    path: "../docs/screenshots/request-automatic-professional.png",
    fullPage: true,
  });
  await expect(page.locator(".request-photos img")).toHaveCount(1);
  await expect(page.locator(".request-photos img").first()).toHaveJSProperty(
    "naturalWidth",
    1280,
  );
  await page.reload();
  await expect(page.locator(".request-photos img")).toHaveCount(1);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: "../docs/screenshots/request-automatic-customer-mobile.png",
    fullPage: true,
  });
  await call(context, token, "requests/cancel-request", {
    request: ident,
    reason: "Solicitud ficticia del recorrido de prueba finalizada.",
  });
  await professionalContext.close();
});

test("una necesidad remota se publica automáticamente y el administrador puede desautorizarla", async ({
  page,
  context,
  browser,
}) => {
  const token = await login(context);
  const cats = (
    await (await context.request.get("/api/v1/public/catalog")).json()
  ).data;
  await page.goto("/requests/new?mode=remote");
  await expect(
    page.getByRole("heading", { name: "¿Qué necesitas resolver?" }),
  ).toBeVisible();
  await page
    .getByLabel(/^Categoría/)
    .selectOption(cats.find((c: any) => c.slug === "diseno-grafico").id);
  const title = `Necesidad remota ficticia ${crypto.randomUUID().slice(0, 8)}`;
  await page.getByLabel(/Título de tu solicitud/).fill(title);
  await page
    .getByLabel(/Requisitos y resultado esperado/)
    .fill(
      "Necesito adaptar esta referencia visual para una presentación de ejemplo.",
    );
  await page.getByLabel(/Moneda de pago/).selectOption("USD");
  await expect(
    page.getByLabel(/Dirección del servicio|Radio de búsqueda/),
  ).toHaveCount(0);
  await expect(page.getByLabel(/Presupuesto ofrecido/)).not.toHaveAttribute(
    "required",
    "",
  );
  await page
    .getByLabel("Fotos de referencia", { exact: true })
    .setInputFiles(photo);
  await page
    .getByRole("button", { name: "Publicar solicitud", exact: true })
    .click();
  await expect(page).toHaveURL(/\/requests\/[a-f0-9-]+$/);
  const ident = page.url().split("/").pop()!;
  const saved = (await call(context, token, "requests/my-requests")).find(
    (r: any) => r.id === ident,
  );
  expect(saved.mode).toBe("broadcast");
  expect(saved.budget_max_minor).toBeNull();
  expect(saved.attachments).toHaveLength(1);
  expect(saved.status).toBe("open");
  expect(saved.moderation_status).toBe("approved");
  expect(saved.attachments[0].moderation_status).toBe("approved");
  await expect(page.locator(".request-photos img")).toHaveCount(1);
  await expect(
    page.getByRole("button", { name: "Quitar foto 1", exact: true }),
  ).toHaveCount(0);
  const baseURL = new URL(page.url()).origin;
  const adminContext = await browser.newContext({ baseURL });
  const professionalContext = await browser.newContext({ baseURL });
  try {
    await login(adminContext, "admin");
    const professionalToken = await login(professionalContext, "disenadora");
    const feed = () =>
      call(
        professionalContext,
        professionalToken,
        "requests/service-feed?delivery_mode=remote",
      );
    expect((await feed()).some((r: any) => r.request_id === ident)).toBe(true);
    const adminPage = await adminContext.newPage();
    await adminPage.goto("/admin");
    await expect(adminPage.locator(".admin-table")).toBeVisible();
    const row = adminPage.getByRole("row").filter({ hasText: title });
    for (
      let pageNumber = 0;
      pageNumber < 20 && !(await row.count());
      pageNumber += 1
    ) {
      const next = adminPage.getByRole("button", {
        name: "Siguiente",
        exact: true,
      });
      if (!(await next.isEnabled())) break;
      await next.click();
      await expect(adminPage.locator(".pagination")).toContainText(
        `Página ${pageNumber + 2}`,
      );
      await expect(adminPage.locator(".admin-table")).toBeVisible();
    }
    await row
      .getByRole("button", { name: "Desautorizar", exact: true })
      .click();
    const dialog = adminPage.getByRole("dialog");
    await dialog
      .getByLabel(/Motivo/)
      .fill(
        "Desautorización de publicación ficticia para validar el control administrativo.",
      );
    await dialog
      .getByRole("button", { name: "Desautorizar", exact: true })
      .click();
    await expect(dialog).toHaveCount(0);
    await expect(
      row.getByRole("button", { name: "Autorizar publicación", exact: true }),
    ).toBeVisible();
    expect((await feed()).some((r: any) => r.request_id === ident)).toBe(false);
    await page.reload();
    await expect(
      page.getByText("La administración desautorizó esta solicitud.", {
        exact: false,
      }),
    ).toBeVisible();
    await page.screenshot({
      path: "../docs/screenshots/request-deauthorized.png",
      fullPage: true,
    });
    await adminPage.screenshot({
      path: "../docs/screenshots/admin-request-moderation.png",
      fullPage: true,
    });
  } finally {
    await call(context, token, "requests/cancel-request", {
      request: ident,
      reason: "Necesidad ficticia de prueba finalizada.",
    });
    await professionalContext.close();
    await adminContext.close();
  }
});
