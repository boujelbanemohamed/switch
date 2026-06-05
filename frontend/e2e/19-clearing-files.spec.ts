import { test, expect } from './fixtures';

test.describe('P3 — Clearing Files (Generate & Upload)', () => {

  test('affiche le titre Clearing', async ({ page }) => {
    await page.goto('/clearing');
    await expect(page.getByRole('heading', { name: 'Clearing & Settlement' })).toBeVisible();
  });

  test('affiche la section generate avec', async ({ page }) => {
    await page.goto('/clearing');
    await page.waitForLoadState('networkidle');
    const generateTitle = page.getByText(/Generate Clearing File/i);
    await expect(generateTitle).toBeVisible();
    const dateInput = page.locator('input[type="date"]').first();
    await expect(dateInput).toBeVisible();
  });

  test('affiche la section upload avec textarea', async ({ page }) => {
    await page.goto('/clearing');
    await page.waitForLoadState('networkidle');
    const uploadTitle = page.getByText(/Upload Incoming File/i);
    await expect(uploadTitle).toBeVisible();
    const textarea = page.locator('textarea');
    await expect(textarea).toBeVisible();
  });

  test('le bouton generate est désactivé sans participantId', async ({ page }) => {
    await page.goto('/clearing');
    await page.waitForLoadState('networkidle');
    const generateBtn = page.getByRole('button', { name: /generate|générer/i }).first();
    await expect(generateBtn).toBeDisabled();
  });

  test('le select CSV / ISO 20022 est présent', async ({ page }) => {
    await page.goto('/clearing');
    await page.waitForLoadState('networkidle');
    const select = page.locator('main select');
    await expect(select).toBeVisible();
    const options = await select.locator('option').allTextContents();
    expect(options.join(', ')).toContain('CSV');
  });

  test('le select contient COMPCONF et CP50', async ({ page }) => {
    await page.goto('/clearing');
    await page.waitForLoadState('networkidle');
    const select = page.locator('main select');
    const options = await select.locator('option').allTextContents();
    const joined = options.join(', ');
    expect(joined).toContain('COMPCONF');
    expect(joined).toContain('CP50');
  });
});
