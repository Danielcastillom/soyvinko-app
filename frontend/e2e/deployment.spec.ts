import { test, expect } from "@playwright/test";
import { readFileSync } from "node:fs";

test("HTTPS conserva la sesión, protege cookies y permite cerrar sesión", async ({
  page,
  context,
  baseURL,
}) => {
  test.skip(!baseURL?.startsWith("https://"), "Requiere el despliegue HTTPS de pruebas");
  const config = await context.request.get("/api/v1/config");
  expect((await config.json()).data).toMatchObject({
    environment: "staging",
    payment_mode: "sandbox",
  });
  const access = readFileSync(new URL("../../.local-access.md", import.meta.url), "utf8");
  const row = access.split("\n").find((line) => line.startsWith("- cliente:"))!;
  const [email, password] = [...row.matchAll(/`([^`]+)`/g)].map((m) => m[1]);
  await page.goto("/login");
  await page.getByLabel(/Correo electrónico/).fill(email);
  await page.getByLabel(/^Contraseña/).fill(password);
  await page.getByRole("button", { name: "Iniciar sesión", exact: true }).click();
  await expect(page).toHaveURL(/dashboard/);
  const cookies = await context.cookies();
  expect(cookies.find((cookie) => cookie.name === "chamba_refresh")).toMatchObject({
    secure: true,
    httpOnly: true,
    sameSite: "Strict",
    path: "/api/v1/auth",
  });
  expect(cookies.find((cookie) => cookie.name === "chamba_csrf")).toMatchObject({
    secure: true,
    httpOnly: false,
    sameSite: "Strict",
  });
  await page.reload();
  await expect(page.getByRole("heading", { name: /Hola,/ })).toBeVisible();
  await expect(page.locator(".sandbox-banner")).toContainText("No se mueve dinero real");
  await page.getByRole("button", { name: "Cerrar sesión", exact: true }).click();
  await expect(page).toHaveURL(/login/);
  await page.goto("/dashboard");
  await expect(page).toHaveURL(/login/);
});

test("el proxy sirve la API y rechaza archivos privados y orígenes ajenos", async ({ request }) => {
  const missing = await request.get("/api/v1/does-not-exist");
  expect(missing.status()).toBe(404);
  expect(missing.headers()["content-type"]).toContain("application/json");
  const privateRoute = await request.get("/api/v1/account/my-account");
  expect(privateRoute.status()).toBe(401);
  const secret = await request.get("/.env");
  expect(secret.status()).toBe(404);
  const origin = await request.post("/api/v1/auth/refresh", {
    headers: { Origin: "https://untrusted.invalid" },
    data: { client: "web" },
  });
  expect(origin.status()).toBe(403);
  expect(origin.headers()["access-control-allow-origin"]).toBeUndefined();
});
