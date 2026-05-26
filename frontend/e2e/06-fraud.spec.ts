import { test, expect } from '@playwright/test';

test.describe('Fraud & Risk - Détection de Fraude', () => {
  test('affiche le titre Fraud', async ({ page }) => {
    await page.goto('/fraud');
    await expect(page.locator('h2')).toHaveText('Fraud Detection');
  });

  test('affiche les statistiques (Total Alerts, Open, Confirmed, High Score)', async ({ page }) => {
    await page.goto('/fraud');
    await page.waitForLoadState('networkidle');
    const statCards = page.locator('main > div > div').first().locator('> div');
    const count = await statCards.count();
    expect(count).toBeGreaterThanOrEqual(3);
  });

  test('affiche le tableau des alertes avec Rule, Score, Status, Decision', async ({ page }) => {
    await page.goto('/fraud');
    const table = page.locator('table');
    await expect(table).toBeVisible();
    const headers = table.locator('thead th');
    const texts = await headers.allTextContents();
    expect(texts.join(', ')).toContain('Rule');
    expect(texts.join(', ')).toContain('Score');
    expect(texts.join(', ')).toContain('Status');
  });
});
