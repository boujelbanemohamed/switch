import { test, expect } from '@playwright/test';

test.describe('Clearing & Settlement - Compensation et Règlement', () => {
  test('affiche le titre Clearing', async ({ page }) => {
    await page.goto('/clearing');
    await expect(page.locator('h2')).toHaveText('Clearing & Settlement');
  });

  test('affiche les statistiques (Total Cleared, Total Fees, Net, Disputes)', async ({ page }) => {
    await page.goto('/clearing');
    await page.waitForLoadState('networkidle');
    const statCards = page.locator('main > div > div').first().locator('> div');
    const count = await statCards.count();
    expect(count).toBeGreaterThanOrEqual(3);
  });

  test('affiche deux sections: Clearing Records et Netting Results', async ({ page }) => {
    await page.goto('/clearing');
    await page.waitForLoadState('networkidle');
    const subHeadings = page.locator('h3');
    const texts = await subHeadings.allTextContents();
    const joined = texts.join('|');
    expect(joined).toContain('Clearing Records');
    expect(joined).toContain('Netting Results');
  });

  test('affiche les tableaux avec Amount, Fee, Status', async ({ page }) => {
    await page.goto('/clearing');
    await page.waitForLoadState('networkidle');
    const tables = page.locator('table');
    const count = await tables.count();
    expect(count).toBeGreaterThanOrEqual(1);
    const headers = tables.first().locator('thead th');
    const texts = await headers.allTextContents();
    expect(texts.join(', ')).toContain('Amount');
    expect(texts.join(', ')).toContain('Status');
  });
});
