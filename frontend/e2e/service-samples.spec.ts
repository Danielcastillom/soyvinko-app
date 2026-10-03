import { test, expect } from "@playwright/test";
import { readFileSync } from "node:fs";

const samples = JSON.parse(
  readFileSync(
    new URL("../../scripts/demo_services.json", import.meta.url),
    "utf8",
  ),
);

test("cada categoría ofrece tres muestras con imagen y perfil accesible", async ({
  page,
  context,
}) => {
  test.setTimeout(120000);
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  const response = await page.request.get("/api/v1/public/catalog");
  const categories = (await response.json()).data;
  await page.goto("/explore");
  const selector = page.getByRole("combobox", {
    name: "Categoría",
    exact: true,
  });
  for (const category of categories) {
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes("/public/discover?") &&
          r.url().includes(category.id) &&
          r.ok(),
      ),
      selector.selectOption(category.id),
    ]);
    for (const sample of samples[category.slug]) {
      const card = page.locator(".professional-card").filter({
        has: page.getByRole("heading", { name: sample.name, exact: true }),
      });
      await expect(card).toBeVisible();
      await expect(card).toContainText(sample.title);
      await card.scrollIntoViewIfNeeded();
      const image = card.getByRole("img", { name: /^Imagen de ejemplo/ });
      await expect(image).toHaveJSProperty("naturalWidth", 1280);
      await expect(image).toHaveAttribute(
        "src",
        `/images/services/${category.slug}-v1.webp`,
      );
    }
    const first = samples[category.slug][0];
    const href = await page
      .getByRole("link", { name: `Ver perfil de ${first.name}`, exact: true })
      .getAttribute("href");
    const profilePage = await context.newPage();
    await profilePage.goto(href!);
    await expect(
      profilePage.getByRole("heading", { name: first.name, exact: true }),
    ).toBeVisible();
    await expect(
      profilePage.getByRole("heading", { name: first.title, exact: true }),
    ).toBeVisible();
    await profilePage.close();
  }
  const design = categories.find((c: any) => c.slug === "diseno-grafico");
  await selector.selectOption(design.id);
  await expect(
    page.getByRole("heading", { name: "Lucía Martínez", exact: true }),
  ).toBeVisible();
  await page.locator(".professional-grid").screenshot({
    path: "../docs/screenshots/three-design-services-desktop.png",
  });
  const mobilePage = await context.newPage();
  await mobilePage.setViewportSize({ width: 390, height: 844 });
  await mobilePage.goto("/explore");
  await mobilePage
    .getByRole("combobox", { name: "Categoría", exact: true })
    .selectOption(design.id);
  await expect(
    mobilePage.getByRole("heading", { name: "Lucía Martínez", exact: true }),
  ).toBeVisible();
  for (const card of await mobilePage.locator(".professional-card").all())
    await card.scrollIntoViewIfNeeded();
  expect(
    await mobilePage.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await mobilePage.locator(".professional-grid").screenshot({
    path: "../docs/screenshots/three-design-services-mobile.png",
  });
  await mobilePage.close();
  expect(errors).toEqual([]);
});
