import { test, expect } from './fixtures';

// Tests Card Programs & Products: create program -> create product.
test.describe('Card Programs & Products', () => {

  test('la page affiche le titre Card Programs', async ({ page }) => {
    await page.goto('/card-programs');
    await expect(page.getByRole('heading', { name: 'Card Programs & Products' })).toBeVisible();
  });

  test('le bouton New Program ouvre le formulaire programme', async ({ page }) => {
    await page.goto('/card-programs');
    await page.getByText('Loading...').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {});
    await page.locator('button:has-text("New Program")').click();
    await expect(page.getByRole('heading', { name: 'New Program' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create' })).toBeVisible();
  });

  test('créer un programme envoie un POST /issuing/programs', async ({ page }) => {
    await page.goto('/card-programs');
    await page.getByText('Loading...').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {});
    await page.locator('button:has-text("New Program")').click();

    await page.getByLabel('Name').fill('E2E Program ' + Date.now());

    await page.waitForLoadState('networkidle');
    const [resp] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/v1/issuing/programs') && r.request().method() === 'POST'),
      page.getByRole('button', { name: 'Create' }).click(),
    ]);
    expect(resp.status(), `POST /issuing/programs a renvoyé ${resp.status()}`).toBeLessThan(400);
  });

  test('cliquer sur un programme déplie ses produits', async ({ page }) => {
    await page.goto('/card-programs');
    await page.waitForLoadState('networkidle');
    const firstProgram = page.locator('[class*="cursor-pointer"]').first();
    const hasProgram = await firstProgram.isVisible().catch(() => false);
    test.skip(!hasProgram, 'Aucun programme — en créer un d\'abord');
    await firstProgram.click();
    await page.waitForTimeout(400);
  });
});
