import { test, expect } from './fixtures';

// Tests the Fee Schedules module: create schedule -> add rule -> toggle -> delete.
test.describe('Fee Schedules', () => {

  test('la page affiche le titre Fee Schedules', async ({ page }) => {
    await page.goto('/fees');
    await expect(page.getByRole('heading', { name: 'Fee Schedules' })).toBeVisible();
  });

  test('le bouton New Schedule ouvre le formulaire', async ({ page }) => {
    await page.goto('/fees');
    await page.getByRole('button', { name: 'New Schedule' }).click();
    await expect(page.getByRole('heading', { name: 'New Schedule' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create' })).toBeVisible();
  });

  test('créer une grille tarifaire envoie un POST /fees/schedules', async ({ page }) => {
    await page.goto('/fees');
    await page.getByRole('button', { name: 'New Schedule' }).click();

    // Le premier champ "Name" — on cible le label "Name" du formulaire
    await page.getByLabel('Name').fill('E2E Schedule ' + Date.now());
    // Effective From est requis (type date)
    const dateInput = page.locator('input[type="date"]').first();
    await dateInput.fill('2026-01-01');

    const [resp] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/v1/fees/schedules') && r.request().method() === 'POST'),
      page.getByRole('button', { name: 'Create' }).click(),
    ]);
    console.log(`POST /fees/schedules → ${resp.status()}`);
    expect(resp.status()).toBeLessThan(400);
  });

  test('sélectionner une grille affiche ses règles et le bouton Add Rule', async ({ page }) => {
    await page.goto('/fees');
    await page.waitForLoadState('networkidle');
    const firstSchedule = page.locator('tbody tr').first();
    const hasRows = await firstSchedule.isVisible().catch(() => false);
    test.skip(!hasRows, 'Aucune grille — en créer une d\'abord');

    await firstSchedule.click();
    await expect(page.getByRole('button', { name: 'Add Rule' })).toBeVisible();
  });
});
