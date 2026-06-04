import { test, expect } from './fixtures';

test.describe('P4 — COF / Recurring Payments', () => {

  test('affiche le titre COF / Recurring', async ({ page }) => {
    await page.goto('/cof');
    await expect(page.getByRole('heading', { name: 'COF / Recurring Payments' })).toBeVisible();
  });

  test('affiche les deux tableaux: Tokens et Schedules', async ({ page }) => {
    await page.goto('/cof');
    await page.waitForLoadState('networkidle');
    const tables = page.locator('table');
    const count = await tables.count();
    expect(count).toBeGreaterThanOrEqual(2);
  });

  test('les en-têtes des tableaux contiennent PAN, Type, Status', async ({ page }) => {
    await page.goto('/cof');
    await page.waitForLoadState('networkidle');
    const headers = page.locator('table').first().locator('thead th');
    const texts = await headers.allTextContents();
    const joined = texts.join('|');
    expect(joined).toContain('PAN');
    expect(joined).toContain('Type');
    expect(joined).toContain('Status');
  });

  test('le bouton Add ouvre le formulaire Token', async ({ page }) => {
    await page.goto('/cof');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /add/i }).first().click();
    await expect(page.getByRole('button', { name: /save|enregistrer/i }).first()).toBeVisible();
  });

  test('le bouton Add Schedule ouvre le formulaire Schedule', async ({ page }) => {
    await page.goto('/cof');
    await page.waitForLoadState('networkidle');
    const gridContainer = page.locator('main > div > div').nth(1);
    const scheduleSection = gridContainer.locator('> div').nth(1);
    await scheduleSection.getByRole('button', { name: 'Add' }).click();
    const scheduleSave = scheduleSection.getByRole('button', { name: /save|enregistrer/i });
    await expect(scheduleSave).toBeVisible();
  });
});
