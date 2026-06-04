import { test, expect } from './fixtures';

test.describe('P5 — FX / Multi-Currency', () => {

  test('affiche le titre FX / Multi-Currency', async ({ page }) => {
    await page.goto('/fx-rates');
    await expect(page.getByRole('heading', { name: /fx|multi-currency|multi-dévise/i })).toBeVisible();
  });

  test('affiche le tableau des taux de change', async ({ page }) => {
    await page.goto('/fx-rates');
    await page.waitForLoadState('networkidle');
    const table = page.locator('table');
    await expect(table).toBeVisible();
    const headers = await table.locator('thead th').allTextContents();
    const joined = headers.join('|');
    expect(joined).toContain('Pair');
    expect(joined).toContain('Rate');
    expect(joined).toContain('Margin');
  });

  test('affiche le panneau Currency Converter', async ({ page }) => {
    await page.goto('/fx-rates');
    await page.waitForLoadState('networkidle');
    const converterTitle = page.getByText(/converter|convertisseur/i);
    await expect(converterTitle).toBeVisible();
    const inputs = page.locator('input[type="number"]');
    expect(await inputs.count()).toBeGreaterThanOrEqual(1);
    const convertBtn = page.getByRole('button', { name: /convert|convertir/i });
    await expect(convertBtn).toBeVisible();
  });

  test('le bouton Add ouvre le formulaire', async ({ page }) => {
    await page.goto('/fx-rates');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /add|ajouter/i }).click();
    await expect(page.getByRole('button', { name: /save|enregistrer/i })).toBeVisible();
  });
});
