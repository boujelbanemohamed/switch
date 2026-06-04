import { test, expect } from './fixtures';

test.describe('Switching - Routage & BIN Tables', () => {
  test('affiche le titre Routing Rules', async ({ page }) => {
    await page.goto('/routing');
    await expect(page.locator('h2')).toHaveText('Routing Rules');
  });

  test('affiche le titre BIN Tables', async ({ page }) => {
    await page.goto('/bin-tables');
    await expect(page.locator('h2')).toHaveText('BIN Tables');
  });

  test('affiche la page BIN Tables avec les en-têtes', async ({ page }) => {
    await page.goto('/bin-tables');
    await expect(page.locator('h2')).toHaveText('BIN Tables');
    const contentArea = page.locator('main > div');
    await expect(contentArea).toBeVisible();
  });

  test('navigation: retour au Dashboard depuis Switching', async ({ page }) => {
    await page.goto('/routing');
    await page.click('a[href="/"]');
    await expect(page.locator('h2')).toHaveText('Dashboard');
  });
});
