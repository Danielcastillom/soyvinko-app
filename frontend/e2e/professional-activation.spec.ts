import { test, expect } from "@playwright/test";
import { randomBytes, randomUUID } from "node:crypto";

test("activación convierte el nombre, explica duplicados y entrega créditos una sola vez", async ({
  page,
  context,
}) => {
  const config = (await (await context.request.get("/api/v1/config")).json())
    .data;
  test.skip(
    config.environment !== "test" && process.env.CHAMBA_TEST_ACTIVATION !== "1",
    "Requiere habilitar una cuenta ficticia para verificar activación.",
  );
  expect(config.payment_mode).toBe("sandbox");
  const email = `activation-${randomUUID()}@example.com`,
    password = randomBytes(24).toString("base64url");
  const registered = await context.request.post("/api/v1/auth/register", {
    data: { email, password, name: "Prueba de activación", client: "web" },
  });
  expect(registered.status()).toBe(201);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/profile?tab=professional");
  await page
    .getByLabel(/Nombre profesional o de tu negocio/)
    .fill("Develope Academy · prueba");
  const slug = page.getByLabel(/Nombre único para tu perfil/);
  await slug.fill("Plomero Demo");
  await page
    .getByLabel(/Cuéntanos qué haces/)
    .fill("Desarrollo Full Stack de aplicaciones de ejemplo.");
  await expect(slug).toHaveValue("plomero-demo");
  await page
    .getByRole("button", { name: "Activar perfil profesional", exact: true })
    .click();
  await expect(slug).toHaveAttribute("aria-invalid", "true");
  await expect(page.locator(".field-error")).toContainText(
    "nombre de perfil ya está ocupado",
  );
  await expect(page.getByLabel(/Cuéntanos qué haces/)).toHaveValue(
    "Desarrollo Full Stack de aplicaciones de ejemplo.",
  );
  const suffix = randomUUID().slice(0, 8),
    raw = `Develope Academy ${suffix}`,
    canonical = `develope-academy-${suffix}`;
  await slug.fill(raw);
  await slug.press("Tab");
  await expect(slug).toHaveValue(canonical);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: "../docs/screenshots/profile-activation-mobile.png",
    fullPage: true,
  });
  await page
    .getByRole("button", { name: "Activar perfil profesional", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Tu presentación", exact: true }),
  ).toBeVisible();
  const session = (
    await (
      await context.request.post("/api/v1/auth/login", {
        data: { email, password, client: "native" },
      })
    ).json()
  ).data;
  const headers = { Authorization: `Bearer ${session.access_token}` };
  const profile = (
    await (
      await context.request.get("/api/v1/professionals/my-professional", {
        headers,
      })
    ).json()
  ).data;
  expect(profile.slug).toBe(canonical);
  const repeated = await context.request.post(
    "/api/v1/professionals/activate-professional",
    {
      headers,
      data: { slug: raw, business_name: "Develope Academy · prueba" },
    },
  );
  expect(repeated.ok()).toBe(true);
  const wallet = (
    await (
      await context.request.get("/api/v1/wallet/my-wallet", { headers })
    ).json()
  ).data;
  expect(wallet.balance).toBe(3);
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "Tu presentación", exact: true }),
  ).toBeVisible();
});
