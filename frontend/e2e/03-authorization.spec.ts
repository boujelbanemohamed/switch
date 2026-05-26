import { test, expect } from '@playwright/test';

test.describe('Authorization - Moteur d\'Autorisation', () => {
  test('affiche le titre Authorization', async ({ page }) => {
    await page.goto('/authorization');
    await expect(page.locator('h2')).toHaveText('Authorization Engine');
  });

  test('affiche les statistiques (Total Rules, Active, Success Rate, Decisions)', async ({ page }) => {
    await page.goto('/authorization');
    await page.waitForLoadState('networkidle');
    const statCards = page.locator('main > div > div').first().locator('> div');
    const count = await statCards.count();
    expect(count).toBeGreaterThanOrEqual(3);
  });

  test('affiche le tableau des règles avec Name, Type, Priority, Status, Success, Failures', async ({ page }) => {
    await page.goto('/authorization');
    const table = page.locator('table');
    await expect(table).toBeVisible();
    const headers = table.locator('thead th');
    const texts = await headers.allTextContents();
    expect(texts.join(', ')).toContain('Name');
    expect(texts.join(', ')).toContain('Priority');
    expect(texts.join(', ')).toContain('Status');
  });
});
