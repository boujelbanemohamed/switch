import { test, expect } from '@playwright/test';

test.describe('Back-office & Reporting', () => {
  test('affiche le titre Back Office', async ({ page }) => {
    await page.goto('/backoffice');
    await expect(page.locator('h2')).toHaveText('Back Office');
  });

  test('affiche les statistiques (Audit, Critical, Warnings, Events)', async ({ page }) => {
    await page.goto('/backoffice');
    await page.waitForLoadState('networkidle');
    const statCards = page.locator('main > div > div').nth(1).locator('> div');
    const count = await statCards.count();
    expect(count).toBeGreaterThanOrEqual(3);
  });

  test('affiche les sections Audit Log et Monitoring Events', async ({ page }) => {
    await page.goto('/backoffice');
    await page.waitForLoadState('networkidle');
    const subHeadings = page.locator('h3');
    const texts = await subHeadings.allTextContents();
    const joined = texts.join('|');
    expect(joined).toContain('Audit');
    expect(joined).toContain('Monitoring');
  });
});
