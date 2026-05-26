import { test, expect } from '@playwright/test';

test.describe('E-Commerce (ACS, EPG, 3DSS)', () => {
  test('affiche le titre E-Commerce', async ({ page }) => {
    await page.goto('/ecommerce');
    await expect(page.locator('h2')).toHaveText('E-Commerce');
  });

  test('affiche les trois onglets: ACS, EPG, 3DSS', async ({ page }) => {
    await page.goto('/ecommerce');
    const tabs = page.locator('button');
    const texts = await tabs.allTextContents();
    const joined = texts.join('|');
    expect(joined).toContain('ACS');
    expect(joined).toContain('EPG');
    expect(joined).toContain('3DSS');
  });

  test('bascule entre les onglets ACS, EPG, 3DSS', async ({ page }) => {
    await page.goto('/ecommerce');

    // Vérifie l'onglet ACS par défaut
    await expect(page.locator('h3').first()).toHaveText('Create Authentication');

    // Clique sur EPG
    await page.locator('button', { hasText: 'EPG' }).click();
    await expect(page.locator('h3').first()).toHaveText('Initiate Payment');

    // Clique sur 3DSS
    await page.locator('button', { hasText: '3DSS' }).click();
    await expect(page.locator('h3').first()).toHaveText('Create 3DS Session');

    // Retour sur ACS
    await page.locator('button', { hasText: 'ACS' }).click();
    await expect(page.locator('h3').first()).toHaveText('Create Authentication');
  });

  test('affiche les champs du formulaire ACS', async ({ page }) => {
    await page.goto('/ecommerce');
    const inputs = page.locator('input');
    const inputCount = await inputs.count();
    expect(inputCount).toBeGreaterThanOrEqual(3);
  });
});
