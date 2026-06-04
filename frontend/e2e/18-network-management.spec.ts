import { test, expect } from './fixtures';

test.describe('P2 — ISO 8583 Network Management', () => {

  test('la page Transactions se charge (MTI routing actif)', async ({ page }) => {
    await page.goto('/transactions');
    await expect(page.getByRole('heading', { name: /transactions/i })).toBeVisible();
  });

  test('la page Clearing se charge (clearing records dépend du pipeline ISO)', async ({ page }) => {
    await page.goto('/clearing');
    await expect(page.getByRole('heading', { name: 'Clearing & Settlement' })).toBeVisible();
  });

  test('le statut Clearing affiche bien les cartes de stats', async ({ page }) => {
    await page.goto('/clearing');
    await page.waitForLoadState('networkidle');
    const statCards = page.locator('main > div > div').nth(1).locator('> div');
    const count = await statCards.count();
    expect(count).toBeGreaterThanOrEqual(3);
  });
});
