import { test, expect, type APIRequestContext } from "@playwright/test";

const API_NEWS = process.env.E2E_API_NEWS_URL ?? "http://localhost:8080/api/news";

async function deleteAllNews(request: APIRequestContext): Promise<void> {
  const res = await request.get(API_NEWS, { failOnStatusCode: false });
  if (!res.ok()) {
    return;
  }
  const items: { id: number }[] = await res.json();
  for (const item of items) {
    await request.delete(`${API_NEWS}/${item.id}`, { failOnStatusCode: false });
  }
}

test.describe("Student News — UI E2E", () => {
  test.beforeEach(async ({ request }) => {
    await deleteAllNews(request);
  });

  test("shows title and empty list after cleanup", async ({ page }) => {
    await page.goto("/");
    await expect(page.getByRole("heading", { name: "Student News" })).toBeVisible();
    // Empty <section class="list"> may have no layout box; assert attachment + no cards.
    await expect(page.getByTestId("news-list")).toBeAttached();
    await expect(page.locator('[data-testid^="news-card-"]')).toHaveCount(0);
  });

  test("creates a draft via form and shows it in the list", async ({ page }) => {
    await page.goto("/");
    await page.getByTestId("input-title").fill("E2E draft title");
    await page.getByTestId("input-content").fill("E2E draft body");
    await page.getByTestId("btn-submit").click();

    const card = page.locator('[data-testid^="news-card-"]').first();
    await expect(card).toBeVisible();
    await expect(card.getByRole("heading", { level: 3, name: "E2E draft title" })).toBeVisible();
    await expect(card.getByText("E2E draft body")).toBeVisible();
    await expect(card.locator('[data-testid^="news-status-"]')).toHaveText("DRAFT");
  });

  test("shows validation error when title and content are empty", async ({ page }) => {
    await page.goto("/");
    await page.getByTestId("input-title").fill("");
    await page.getByTestId("input-content").fill("");
    await page.getByTestId("btn-submit").click();
    await expect(page.getByTestId("error-message")).toContainText("Title and content are required");
  });

  test("publishes from UI and hides Publish button", async ({ page }) => {
    await page.goto("/");
    await page.getByTestId("input-title").fill("Publish me");
    await page.getByTestId("input-content").fill("Content");
    await page.getByTestId("btn-submit").click();

    const card = page.locator('[data-testid^="news-card-"]').first();
    const idAttr = await card.getAttribute("data-testid");
    const id = idAttr?.replace("news-card-", "") ?? "";
    expect(id).toMatch(/^\d+$/);

    await page.getByTestId(`btn-publish-${id}`).click();
    await expect(page.getByTestId(`news-status-${id}`)).toHaveText("PUBLISHED");
    await expect(page.getByTestId(`btn-publish-${id}`)).toHaveCount(0);
  });

  test("archives from UI and hides Archive button", async ({ page }) => {
    await page.goto("/");
    await page.getByTestId("input-title").fill("Archive me");
    await page.getByTestId("input-content").fill("Body");
    await page.getByTestId("btn-submit").click();

    const card = page.locator('[data-testid^="news-card-"]').first();
    const id = (await card.getAttribute("data-testid"))!.replace("news-card-", "");

    await page.getByTestId(`btn-archive-${id}`).click();
    await expect(page.getByTestId(`news-status-${id}`)).toHaveText("ARCHIVED");
    await expect(page.getByTestId(`btn-archive-${id}`)).toHaveCount(0);
  });

  test("edits item and saves changes", async ({ page }) => {
    await page.goto("/");
    await page.getByTestId("input-title").fill("Original");
    await page.getByTestId("input-content").fill("Original body");
    await page.getByTestId("btn-submit").click();

    const card = page.locator('[data-testid^="news-card-"]').first();
    const id = (await card.getAttribute("data-testid"))!.replace("news-card-", "");

    await page.getByTestId(`btn-edit-${id}`).click();
    await expect(page.getByRole("heading", { name: "Edit news" })).toBeVisible();
    await page.getByTestId("input-title").fill("Updated title");
    await page.getByTestId("input-content").fill("Updated body");
    await page.getByTestId("btn-submit").click();

    await expect(page.getByRole("heading", { name: "Create news" })).toBeVisible();
    await expect(
      page.locator(`[data-testid="news-card-${id}"]`).getByRole("heading", { level: 3 })
    ).toHaveText("Updated title");
    await expect(page.getByTestId(`news-content-text-${id}`)).toHaveText("Updated body");
  });

  test("cancels edit without saving", async ({ page }) => {
    await page.goto("/");
    await page.getByTestId("input-title").fill("Keep title");
    await page.getByTestId("input-content").fill("Keep body");
    await page.getByTestId("btn-submit").click();

    const id = (await page.locator('[data-testid^="news-card-"]').first().getAttribute("data-testid"))!.replace(
      "news-card-",
      ""
    );

    await page.getByTestId(`btn-edit-${id}`).click();
    await page.getByTestId("input-title").fill("Should not save");
    await page.getByTestId("btn-cancel-edit").click();

    await expect(page.getByRole("heading", { name: "Create news" })).toBeVisible();
    await expect(
      page.locator(`[data-testid="news-card-${id}"]`).getByRole("heading", { level: 3 })
    ).toHaveText("Keep title");
  });

  test("filters list by Published status", async ({ page }) => {
    await page.goto("/");
    await page.getByTestId("input-title").fill("Only draft");
    await page.getByTestId("input-content").fill("A");
    await page.getByTestId("btn-submit").click();
    await expect(page.getByRole("heading", { level: 3, name: "Only draft" })).toBeVisible();

    await page.getByTestId("input-title").fill("Will publish");
    await page.getByTestId("input-content").fill("B");
    await page.getByTestId("btn-submit").click();
    await expect(page.getByRole("heading", { level: 3, name: "Will publish" })).toBeVisible();

    await expect(page.locator('[data-testid^="news-card-"]')).toHaveCount(2);

    const toPublish = page.locator('[data-testid^="news-card-"]').filter({ hasText: "Will publish" });
    await toPublish.getByRole("button", { name: "Publish" }).click();
    await expect(toPublish.locator('[data-testid^="news-status-"]')).toHaveText("PUBLISHED");

    await page.getByTestId("select-status").selectOption("PUBLISHED");
    await expect(page.locator('[data-testid^="news-card-"]')).toHaveCount(1);
    await expect(page.getByRole("heading", { level: 3, name: "Will publish" })).toBeVisible();

    await page.getByTestId("select-status").selectOption("ALL");
    await expect(page.locator('[data-testid^="news-card-"]')).toHaveCount(2);
  });

  test("deletes item from UI", async ({ page }) => {
    await page.goto("/");
    await page.getByTestId("input-title").fill("Delete me");
    await page.getByTestId("input-content").fill("X");
    await page.getByTestId("btn-submit").click();

    const id = (await page.locator('[data-testid^="news-card-"]').first().getAttribute("data-testid"))!.replace(
      "news-card-",
      ""
    );
    await page.getByTestId(`btn-delete-${id}`).click();
    await expect(page.locator('[data-testid^="news-card-"]')).toHaveCount(0);
  });

  test("refresh reloads list", async ({ page }) => {
    await page.goto("/");
    await page.getByTestId("input-title").fill("Refresh test");
    await page.getByTestId("input-content").fill("Y");
    await page.getByTestId("btn-submit").click();
    await expect(page.locator('[data-testid^="news-card-"]')).toHaveCount(1);

    await page.getByTestId("btn-refresh").click();
    await expect(page.getByRole("heading", { level: 3, name: "Refresh test" })).toBeVisible();
  });
});
