import { test, expect, type Page, type BrowserContext } from "@playwright/test";
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
async function login(page: Page, role: string) {
  const c = credentials(role);
  await page.goto("/login");
  await page.getByLabel(/Correo electrónico/).fill(c.email);
  await page.getByLabel(/^Contraseña/).fill(c.password);
  await page
    .getByRole("button", { name: "Iniciar sesión", exact: true })
    .click();
  await expect(page).toHaveURL(/dashboard/);
  await expect(page.getByRole("heading", { name: /Hola,/ })).toBeVisible();
}
async function native(context: BrowserContext, role: string) {
  const response = await context.request.post("/api/v1/auth/login", {
    data: { ...credentials(role), client: "native" },
  });
  expect(response.ok()).toBeTruthy();
  return (await response.json()).data;
}
async function call(
  context: BrowserContext,
  token: string,
  path: string,
  body?: any,
) {
  const response = await context.request[body === undefined ? "get" : "post"](
    "/api/v1/" + path,
    {
      headers: { Authorization: `Bearer ${token}` },
      ...(body === undefined ? {} : { data: body }),
    },
  );
  expect(response.ok(), await response.text()).toBeTruthy();
  return (await response.json()).data;
}

test("exploración pública en escritorio y móvil", async ({ page }) => {
  const response = await page.request.get("/api/v1/public/discover?limit=100");
  const profiles = (await response.json()).data;
  const remoteCount = profiles.filter((p: any) =>
    p.offerings.some((o: any) => o.delivery_mode === "remote"),
  ).length;
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/explore");
  await expect(
    page.getByRole("heading", { name: /Un experto para/ }),
  ).toBeVisible();
  await expect(page.locator(".professional-card")).toHaveCount(profiles.length);
  await page.screenshot({
    path: "../docs/screenshots/explore-desktop.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "Remotos", exact: true }).click();
  await expect(page.locator(".professional-card")).toHaveCount(remoteCount);
  await expect(page.getByText("Lucía Martínez", { exact: true })).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole("button", { name: "Todos", exact: true }).click();
  await expect(page.locator(".professional-card")).toHaveCount(profiles.length);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
  await page.screenshot({
    path: "../docs/screenshots/explore-mobile.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "Abrir menú" }).click();
  await expect(page.getByRole("navigation")).toBeVisible();
  await page
    .locator(".sidebar")
    .getByRole("button", { name: "Cerrar menú", exact: true })
    .click();
  expect(errors).toEqual([]);
});

test("paneles de cliente, perfil, soporte y saldo con persistencia", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await login(page, "cliente");
  for (const path of [
    "/profile",
    "/profile?tab=professional",
    "/profile?tab=verification",
    "/profile?tab=files",
    "/profile?tab=security",
    "/support",
    "/requests",
    "/contracts",
    "/marketplace?tab=orders",
  ]) {
    await page.goto(path);
    await expect(page.locator("h1")).toBeVisible();
    await expect(page.locator(".error-box")).toHaveCount(0);
  }
  await page.goto("/wallet");
  await expect(
    page.getByRole("heading", { name: "Mi wallet", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Recargar", exact: true }).click();
  await page.getByLabel(/Importe en Bs/).fill("300000");
  await page.getByRole("button", { name: "Crear y simular recarga" }).click();
  await expect(page.locator("dialog")).toHaveCount(0);
  await page.reload();
  await expect(page.locator(".wallet-amount")).not.toHaveText("Bs 0,00");
  await page.screenshot({
    path: "../docs/screenshots/wallet.png",
    fullPage: true,
  });
  await page.goto("/support");
  await page.getByRole("button", { name: "Crear caso", exact: true }).click();
  await page.getByLabel(/Tema/).selectOption("account");
  await page.getByLabel(/Asunto/).fill("Prueba del portal web");
  await page
    .getByLabel(/Cómo podemos ayudarte/)
    .fill(
      "Caso ficticio para comprobar que el soporte conserva la conversación.",
    );
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Crear caso", exact: true })
    .click();
  await expect(page.locator("dialog")).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: /Prueba del portal web/ }).first(),
  ).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/profile");
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
  expect(errors).toEqual([]);
});

test("administración y panel profesional consultan colas reales", async ({
  page,
}) => {
  await login(page, "admin");
  await page.goto("/admin");
  await expect(
    page.getByRole("heading", { name: "Administración", exact: true }),
  ).toBeVisible();
  for (const name of [
    "Archivos",
    "Reseñas",
    "Identidad",
    "Usuarios",
    "Soporte",
    "Planes",
    "Comisión de retiro",
    "Retiros",
    "Auditoría",
  ]) {
    await page.getByRole("button", { name, exact: true }).click();
    await expect(page.locator(".loading")).toHaveCount(0);
    await expect(page.locator(".error-box")).toHaveCount(0);
  }
  await page.screenshot({
    path: "../docs/screenshots/admin.png",
    fullPage: true,
  });
  await login(page, "tecnico");
  for (const path of [
    "/profile?tab=professional",
    "/opportunities",
    "/marketplace?tab=store",
  ]) {
    await page.goto(path);
    await expect(page.locator("h1")).toBeVisible();
    await expect(page.locator(".error-box")).toHaveCount(0);
  }
});

test("contratación remota, reserva, chat, entrega y liberación desde la web", async ({
  browser,
}) => {
  const customerContext = await browser.newContext(),
    proContext = await browser.newContext();
  const customer = await customerContext.newPage(),
    pro = await proContext.newPage();
  try {
    const customerApi = await native(customerContext, "cliente"),
      professional = await native(proContext, "disenadora");
    const cats = (
      await (await customerContext.request.get("/api/v1/public/catalog")).json()
    ).data;
    const category = cats.find((c: any) => c.slug === "diseno-grafico").id;
    const funds = await call(customerContext, customerApi.access_token, "wallet/money-wallet?currency=VES");
    if (funds.available_minor < 1000000) {
      const topup = await call(customerContext, customerApi.access_token, "wallet/create-topup", { currency: "VES", amount: 1000000, key: crypto.randomUUID() });
      await call(customerContext, customerApi.access_token, `integrations/sandbox/topup/${topup}/settle`, {});
    }
    const balance = await call(
      proContext,
      professional.access_token,
      "wallet/my-wallet",
    );
    if (balance.balance < 1) {
      const plans = await call(
        proContext,
        professional.access_token,
        "wallet/active-plans",
      );
      const purchase = await call(
        proContext,
        professional.access_token,
        "wallet/start-purchase",
        { plan: plans[0].id, key: crypto.randomUUID() },
      );
      await call(
        proContext,
        professional.access_token,
        `integrations/sandbox/purchase/${purchase}/settle`,
        {},
      );
    }
    await login(customer, "cliente");
    await login(pro, "disenadora");
    const profile = await call(
      proContext,
      professional.access_token,
      "professionals/my-professional",
    );
    const offering = profile.offerings.find(
      (o: any) =>
        o.category_id === category && o.delivery_mode === "remote" && o.active,
    );
    await customer.goto(
      `/requests/new?professional=${professional.user.id}&offering=${offering.id}`,
    );
    await expect(customer.getByLabel(/Título de tu solicitud/)).toHaveCount(0);
    await customer.getByLabel(/Moneda de pago/).selectOption("VES");
    await customer
      .getByLabel(/Explica la falla/)
      .fill(
        "Entregar un logotipo con dos variantes y comprobar la aceptación del cliente.",
      );
    await customer.getByLabel("Fotos de referencia", { exact: true }).setInputFiles({
      name: "referencia-de-prueba.webp", mimeType: "image/webp",
      buffer: readFileSync(new URL("../public/images/services/electricidad-v1.webp", import.meta.url)),
    });
    await customer
      .getByRole("button", { name: "Enviar solicitud", exact: true })
      .click();
    await expect(customer).toHaveURL(/\/requests\/[a-f0-9-]+$/);
    const id = customer.url().split("/").pop()!;
    const customerRequests = await call(customerContext, customerApi.access_token, "requests/my-requests");
    const saved = customerRequests.find((r: any) => r.id === id);
    expect(saved.status).toBe("open");
    expect(saved.moderation_status).toBe("approved");
    expect(saved.attachments[0].moderation_status).toBe("approved");
    await expect(customer.locator(".status-open")).toBeVisible();
    await pro.goto("/opportunities");
    await pro.getByRole("button", { name: "Proyectos remotos" }).click();
    const card = pro
      .locator("section")
      .filter({
        has: pro.getByRole("heading", {
          name: offering.title,
          exact: true,
        }),
      })
      .first();
    await card
      .getByRole("button", { name: "Enviar cotización", exact: true })
      .click();
    await pro
      .getByLabel(/Cuéntale cómo puedes ayudar/)
      .fill("Entregaré las dos variantes y el logotipo acordados.");
    await pro.getByLabel(/Precio ofrecido en Bs/).fill("10000");
    await pro
      .getByRole("dialog")
      .getByRole("button", { name: "Enviar cotización", exact: true })
      .click();
    await expect(pro.getByRole("dialog")).toHaveCount(0);
    await customer.reload();
    await customer
      .getByRole("button", { name: "Elegir profesional", exact: true })
      .click();
    await customer
      .getByRole("button", { name: "Confirmar", exact: true })
      .click();
    await expect(customer).toHaveURL(/\/contracts\/[a-f0-9-]+$/);
    const contractId = customer.url().split("/").pop()!;
    await pro.goto(`/contracts/${contractId}`);
    await expect(pro.locator(".request-photos img")).toHaveCount(1);
    await expect(customer.locator(".request-photos img")).toHaveCount(1);
    await customer
      .getByRole("button", { name: "Reservar saldo", exact: true })
      .click();
    await customer
      .getByRole("button", { name: "Confirmar reserva", exact: true })
      .click();
    await expect(customer.locator("dialog")).toHaveCount(0);
    await customer
      .getByRole("textbox", { name: "Mensaje", exact: true })
      .fill("El alcance está confirmado. ¡Podemos empezar!");
    await customer.getByRole("button", { name: "Enviar mensaje" }).click();
    await expect(
      pro.getByText("El alcance está confirmado. ¡Podemos empezar!", {
        exact: true,
      }),
    ).toBeVisible();
    await pro
      .getByRole("button", { name: "Iniciar actividad", exact: true })
      .click();
    await expect(
      pro.locator(".page-actions .status-in_progress"),
    ).toBeVisible();
    await pro
      .getByRole("button", { name: "Presentar entrega", exact: true })
      .click();
    await pro
      .getByLabel(/Describe lo que entregas/)
      .fill("Logotipo y dos variantes completadas para validar el recorrido.");
    await pro
      .getByRole("dialog")
      .getByRole("button", { name: "Presentar entrega", exact: true })
      .click();
    await expect(
      customer.getByRole("button", {
        name: "Validar y liberar pago",
        exact: true,
      }),
    ).toBeVisible();
    await customer
      .getByRole("button", { name: "Validar y liberar pago", exact: true })
      .click();
    await customer
      .getByRole("button", { name: "Confirmar", exact: true })
      .click();
    await expect(
      customer.locator(".page-actions .status-completed"),
    ).toBeVisible();
    await customer.screenshot({
      path: "../docs/screenshots/contract-completed.png",
      fullPage: true,
    });
  } finally {
    await customerContext.close();
    await proContext.close();
  }
});
