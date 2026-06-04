import { test, expect } from './fixtures';

// Tests the Batch Jobs (EOD/BOD) module button by button.
test.describe('Batch Jobs — EOD / BOD', () => {

  test('la page affiche le titre Batch Jobs', async ({ page }) => {
    await page.goto('/batch');
    await expect(page.getByRole('heading', { name: 'Batch Jobs' })).toBeVisible();
  });

  test('déclencher EOD envoie un POST /batch/eod', async ({ page }) => {
    await page.goto('/batch');
    const [resp] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/v1/batch/eod') && r.request().method() === 'POST'),
      page.getByRole('button', { name: /eod/i }).first().click(),
    ]);
    expect(resp.status(), `POST /batch/eod a renvoyé ${resp.status()}`).toBeLessThan(400);
  });

  test('déclencher BOD envoie un POST /batch/bod', async ({ page }) => {
    await page.goto('/batch');
    const [resp] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/v1/batch/bod') && r.request().method() === 'POST'),
      page.getByRole('button', { name: /bod/i }).first().click(),
    ]);
    expect(resp.status(), `POST /batch/bod a renvoyé ${resp.status()}`).toBeLessThan(400);
  });

  test('après EOD, un job apparaît dans l\'historique', async ({ page }) => {
    await page.goto('/batch');
    await page.getByRole('button', { name: /eod/i }).first().click();
    await page.waitForResponse(r => r.url().includes('/api/v1/batch/history'));
    await page.waitForTimeout(500);
    // au moins une ligne dans le tableau des jobs
    const rows = page.locator('tbody tr');
    expect(await rows.count()).toBeGreaterThan(0);
  });
});
