import { test, expect } from "@playwright/test";

test("registro valida la contraseña antes de enviar y muestra el motivo en móvil", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  let submissions = 0;
  await page.route("**/api/v1/auth/register", async (route) => {
    submissions++;
    await route.abort();
  });
  await page.goto("/register");
  await page.getByLabel(/Tu nombre/).fill("Prueba Registro");
  await page.getByLabel(/Correo electrónico/).fill("registro@example.com");
  const password = page.getByLabel(/^Contraseña/);
  await password.fill("corta");
  await page.getByRole("button", { name: "Crear mi cuenta" }).click();
  await expect(password).toHaveAttribute("aria-invalid", "true");
  await expect(page.getByRole("alert")).toHaveText(
    "Escribe al menos 12 caracteres.",
  );
  await expect(page).toHaveURL(/\/register$/);
  expect(submissions).toBe(0);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  await password.fill("Valida-ahora-2026!");
  await expect(password).not.toHaveAttribute("aria-invalid", "true");
  await expect(page.getByRole("alert")).toHaveCount(0);
});

test("registro conserva los datos, señala el campo rechazado por la API y permite corregirlo", async ({
  page,
}) => {
  let submissions = 0;
  await page.route("**/api/v1/auth/register", async (route) => {
    submissions++;
    const body = route.request().postDataJSON();
    expect(body.password).toBe("  Clave-de-prueba-2026  ");
    expect(body.email).toBe("registro@example.com");
    expect(body.client).toBe("web");
    expect(body.name).toBe(
      submissions === 1 ? "Prueba Registro" : "Prueba Corregida",
    );
    await route.fulfill({
      status: submissions === 1 ? 422 : 409,
      contentType: "application/json",
      body: JSON.stringify({
        error:
          submissions === 1
            ? {
                code: "validation_error",
                message: "Revisa los campos del formulario.",
                request_id: "registration-validation-test",
                details: [
                  {
                    field: "body.name",
                    message: "Escribe al menos 2 caracteres.",
                  },
                ],
              }
            : {
                code: "already_exists",
                message:
                  "Ya existe un registro con estos datos. Revisa la información o inicia sesión.",
                details: null,
              },
      }),
    });
  });
  await page.goto("/register");
  const name = page.getByLabel(/Tu nombre/);
  const email = page.getByLabel(/Correo electrónico/);
  const password = page.getByLabel(/^Contraseña/);
  await name.fill("  Prueba Registro  ");
  await email.fill("registro@example.com");
  await password.fill("  Clave-de-prueba-2026  ");
  await page.getByRole("button", { name: "Crear mi cuenta" }).click();
  await expect(name).toHaveAttribute("aria-invalid", "true");
  await expect(name).toHaveAccessibleDescription(
    "Escribe al menos 2 caracteres.",
  );
  await expect(name).toBeFocused();
  await expect(email).toHaveValue("registro@example.com");
  await expect(password).toHaveValue("  Clave-de-prueba-2026  ");
  await name.fill("Prueba Corregida");
  await expect(name).not.toHaveAttribute("aria-invalid", "true");
  await page.getByRole("button", { name: "Crear mi cuenta" }).click();
  await expect(page.getByRole("alert")).toContainText("Ya existe un registro");
  await expect(
    page.getByRole("button", { name: "Crear mi cuenta" }),
  ).toBeEnabled();
  expect(submissions).toBe(2);
});
