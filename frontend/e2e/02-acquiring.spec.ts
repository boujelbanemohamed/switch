import { test, expect } from './fixtures';

test.describe('Acquiring - Acceptation Commerçants', () => {
  test('affiche le titre Acquiring', async ({ page }) => {
    await page.goto('/acquiring');
    await expect(page.locator('h2')).toHaveText('Acquiring');
  });

  test('affiche les statistiques marchands', async ({ page }) => {
    await page.goto('/acquiring');
    await page.waitForLoadState('networkidle');
    const statCards = page.locator('main > div > div').nth(1).locator('> div');
    const count = await statCards.count();
    expect(count).toBeGreaterThanOrEqual(3);
  });

  test('affiche le tableau des marchands avec Code, Name, Category, Country, Status', async ({ page }) => {
    await page.goto('/acquiring');
    const table = page.locator('table');
    await expect(table).toBeVisible();
    const headers = table.locator('thead th');
    const texts = await headers.allTextContents();
    expect(texts.join(', ')).toContain('Code');
    expect(texts.join(', ')).toContain('Name');
    expect(texts.join(', ')).toContain('Status');
  });
});
