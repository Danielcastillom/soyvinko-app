import { test, expect } from "@playwright/test";
import { randomBytes, randomUUID } from "node:crypto";

test("registro HTTPS real: corrige un 422, crea una cuenta ficticia y conserva la sesión", async ({
  page,
  context,
  request,
  baseURL,
}) => {
  test.skip(
    process.env.CHAMBA_TEST_REGISTRATION !== "1" ||
      !baseURL?.startsWith("https://"),
    "Requiere autorización explícita para crear una cuenta ficticia en staging.",
  );
  const config = await request.get("/api/v1/config");
  expect((await config.json()).data).toMatchObject({
    environment: "staging",
    payment_mode: "sandbox",
  });
  const errors: string[] = [];
  const routerWarnings: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("console", (message) => {
    if (message.text().includes("React Router Future Flag Warning"))
      routerWarnings.push(message.text());
  });
  await page.goto("/register");
  const name = page.getByLabel(/Tu nombre/);
  const password = page.getByLabel(/^Contraseña/);
  const testPassword = randomBytes(24).toString("base64url");
  await name.fill("   ");
  await page
    .getByLabel(/Correo electrónico/)
    .fill(`registro-smoke-${randomUUID()}@example.com`);
  await password.fill(testPassword);
  const [invalid] = await Promise.all([
    page.waitForResponse((response) =>
      response.url().endsWith("/api/v1/auth/register"),
    ),
    page.getByRole("button", { name: "Crear mi cuenta" }).click(),
  ]);
  expect(invalid.status()).toBe(422);
  await expect(name).toHaveAttribute("aria-invalid", "true");
  await expect(name).toHaveAccessibleDescription(
    "Escribe al menos 2 caracteres.",
  );
  await expect(password).toHaveValue(testPassword);
  await name.fill("Prueba de registro HTTPS");
  const [created] = await Promise.all([
    page.waitForResponse((response) =>
      response.url().endsWith("/api/v1/auth/register"),
    ),
    page.getByRole("button", { name: "Crear mi cuenta" }).click(),
  ]);
  expect(created.status()).toBe(201);
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole("heading", { name: /Hola,/ })).toBeVisible();
  expect(
    (await context.cookies()).find(
      (cookie) => cookie.name === "chamba_refresh",
    ),
  ).toMatchObject({ secure: true, httpOnly: true, sameSite: "Strict" });
  await page.reload();
  await expect(page.getByRole("heading", { name: /Hola,/ })).toBeVisible();
  await page
    .getByRole("button", { name: "Cerrar sesión", exact: true })
    .click();
  await expect(page).toHaveURL(/\/login$/);
  expect(errors).toEqual([]);
  expect(routerWarnings).toEqual([]);
});
