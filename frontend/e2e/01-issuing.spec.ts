import { test, expect } from '@playwright/test';

test.describe('Issuing - Émission Cartes & Wallets', () => {
  test('affiche le titre Issuing', async ({ page }) => {
    await page.goto('/issuing');
    await expect(page.locator('h2')).toHaveText('Issuing');
  });

  test('affiche les cartes statistiques (Total Cardholders, Active Cards, KYC)', async ({ page }) => {
    await page.goto('/issuing');
    await page.waitForLoadState('networkidle');
    const statCards = page.locator('main > div > div').first().locator('> div');
    const count = await statCards.count();
    expect(count).toBeGreaterThanOrEqual(3);
  });

  test('affiche le tableau des cardholders', async ({ page }) => {
    await page.goto('/issuing');
    const table = page.locator('table');
    await expect(table).toBeVisible();
    const headers = table.locator('thead th');
    const texts = await headers.allTextContents();
    expect(texts.join(', ')).toContain('Name');
    expect(texts.join(', ')).toContain('Status');
    expect(texts.join(', ')).toContain('KYC');
  });
});
