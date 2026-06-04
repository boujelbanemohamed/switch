import { test, expect } from './fixtures';

test.describe('Dashboard - Vue d\'ensemble', () => {
  test('affiche le titre et les statistiques', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('h2')).toHaveText('Dashboard');
    const cards = page.locator('main > div > div').nth(2).locator('> div');
    const count = await cards.count();
    expect(count).toBeGreaterThanOrEqual(3);
  });

  test('affiche les graphiques', async ({ page }) => {
    await page.goto('/');
    await page.waitForSelector('.recharts-responsive-container', { timeout: 5000 }).catch(() => {});
    const charts = page.locator('.recharts-responsive-container');
    expect(await charts.count()).toBeGreaterThanOrEqual(1);
  });
});
