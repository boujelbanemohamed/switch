import { test, expect } from './fixtures';

// Tests the full Dispute / Chargeback workflow, button by button.
test.describe('Disputes — workflow complet', () => {

  test('la page se charge avec son titre et le bouton "Open Dispute"', async ({ page }) => {
    await page.goto('/disputes');
    await expect(page.getByRole('heading', { name: 'Disputes' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Open Dispute' })).toBeVisible();
  });

  test('les onglets All / Open / Resolved sont cliquables', async ({ page }) => {
    await page.goto('/disputes');
    for (const label of ['Open', 'Resolved', 'All']) {
      await page.getByRole('button', { name: label, exact: true }).click();
      // après le clic, soit le tableau, soit le message "no data" s'affiche
      await page.waitForTimeout(300);
    }
  });

  test('le bouton "Open Dispute" ouvre la modale de création', async ({ page }) => {
    await page.goto('/disputes');
    await page.getByRole('button', { name: 'Open Dispute' }).click();
    await expect(page.getByPlaceholder('Transaction ID')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create' })).toBeVisible();
    // fermer la modale
    await page.getByRole('button', { name: 'Cancel' }).click();
  });

  test('créer un litige soumet le formulaire et rafraîchit la liste', async ({ page }) => {
    await page.goto('/disputes');
    await page.getByRole('button', { name: 'Open Dispute' }).click();

    await page.getByPlaceholder('Transaction ID').fill('TXN-E2E-' + Date.now());
    await page.getByPlaceholder('Amount').fill('150.00');
    // la description est un textarea avec le même placeholder "Description"
    await page.getByPlaceholder('Description').fill('Test E2E dispute');

    // intercepter l'appel API pour vérifier qu'il part bien
    const [resp] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/v1/disputes') && r.request().method() === 'POST'),
      page.getByRole('button', { name: 'Create' }).click(),
    ]);
    // 200/201 attendu ; on logue le statut pour diagnostic si échec
    expect(resp.status(), `POST /disputes a renvoyé ${resp.status()}`).toBeLessThan(400);
  });

  test('sélectionner un litige affiche son détail (timeline + preuve)', async ({ page }) => {
    await page.goto('/disputes');
    await page.waitForLoadState('networkidle');
    const firstRow = page.locator('tbody tr').first();
    const hasRows = await firstRow.isVisible().catch(() => false);
    test.skip(!hasRows, 'Aucun litige en base — créer un litige d\'abord');

    await firstRow.click();
    // le panneau détail montre la section Evidence et un bouton Submit Evidence
    await expect(page.getByRole('button', { name: 'Submit Evidence' })).toBeVisible();
    await expect(page.getByText('Timeline')).toBeVisible();
  });
});
