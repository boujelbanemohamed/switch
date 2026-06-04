import { test, expect } from './fixtures';

test.describe('Stand-in / STIP', () => {

  test('la page affiche le titre Stand-in / STIP', async ({ page }) => {
    await page.goto('/stand-in');
    await expect(page.getByRole('heading', { name: 'Stand-in / STIP' })).toBeVisible();
  });

  test('le bouton New Rule ouvre le formulaire', async ({ page }) => {
    await page.goto('/stand-in');
    await page.getByRole('button', { name: 'New Rule' }).click();
    await expect(page.getByRole('heading', { name: 'New Rule' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create' })).toBeVisible();
  });

  test('créer une règle stand-in envoie un POST /standin/rules', async ({ page }) => {
    await page.goto('/stand-in');
    await page.getByRole('button', { name: 'New Rule' }).click();

    await page.locator('input[type="number"]').first().fill('2000');

    const [resp] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/v1/standin/rules') && r.request().method() === 'POST'),
      page.getByRole('button', { name: 'Create' }).click(),
    ]);
    console.log(`POST /standin/rules → ${resp.status()}`);
    expect(resp.status()).toBeLessThan(400);
  });
});
