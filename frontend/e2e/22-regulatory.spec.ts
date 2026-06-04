import { test, expect } from './fixtures';

test.describe('P6 — Regulatory Reports (BCT / SIBTEL)', () => {

  test('affiche le titre Regulatory Reports', async ({ page }) => {
    await page.goto('/regulatory-reports');
    await expect(page.getByRole('heading', { name: /regulatory|réglementaire/i })).toBeVisible();
  });

  test('affiche les templates de rapport', async ({ page }) => {
    await page.goto('/regulatory-reports');
    await page.waitForLoadState('networkidle');
    const cards = page.locator('main > div > div > div > div > div');
    const count = await cards.count();
    expect(count).toBeGreaterThanOrEqual(1);
  });

  test('affiche le panneau de génération avec date range', async ({ page }) => {
    await page.goto('/regulatory-reports');
    await page.waitForLoadState('networkidle');
    const generateTitle = page.getByText(/Generate Report/i);
    await expect(generateTitle).toBeVisible();
    const dateInputs = page.locator('input[type="date"]');
    expect(await dateInputs.count()).toBeGreaterThanOrEqual(2);
    const generateBtn = page.getByRole('button', { name: /generate|générer/i });
    await expect(generateBtn).toBeVisible();
  });

  test('le select format offre CSV', async ({ page }) => {
    await page.goto('/regulatory-reports');
    await page.waitForLoadState('networkidle');
    const select = page.locator('main select');
    await expect(select).toBeVisible();
    const options = await select.locator('option').allTextContents();
    expect(options.join(', ')).toContain('CSV');
  });
});
