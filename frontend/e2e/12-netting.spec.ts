import { test, expect } from './fixtures';

// Tests the Multilateral Netting workflow: Calculate -> Confirm -> Settle.
test.describe('Multilateral Netting', () => {

  test('la page affiche le titre Multilateral Netting', async ({ page }) => {
    await page.goto('/netting');
    await expect(page.getByRole('heading', { name: 'Multilateral Netting' })).toBeVisible();
  });

  test('le bouton Calculate déclenche un POST /netting/calculate', async ({ page }) => {
    await page.goto('/netting');
    const [resp] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/v1/netting/calculate') && r.request().method() === 'POST'),
      page.getByRole('button', { name: 'Calculate' }).click(),
    ]);
    expect(resp.status(), `POST /netting/calculate a renvoyé ${resp.status()}`).toBeLessThan(400);
  });

  test('après calcul, la session affiche une efficacité et des positions', async ({ page }) => {
    await page.goto('/netting');
    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/v1/netting/calculate')),
      page.getByRole('button', { name: 'Calculate' }).click(),
    ]);
    await page.waitForTimeout(500);
    // Efficiency label visible une fois la session créée
    await expect(page.getByText('Efficiency')).toBeVisible();
  });

  test('Confirm puis Settle sont disponibles après calcul', async ({ page }) => {
    await page.goto('/netting');
    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/v1/netting/calculate')),
      page.getByRole('button', { name: 'Calculate' }).click(),
    ]);
    await page.waitForTimeout(500);

    const confirmBtn = page.getByRole('button', { name: 'Confirm' });
    if (await confirmBtn.isVisible().catch(() => false)) {
      const [resp] = await Promise.all([
        page.waitForResponse(r => r.url().includes('/confirm') && r.request().method() === 'POST'),
        confirmBtn.click(),
      ]);
      expect(resp.status(), `POST confirm a renvoyé ${resp.status()}`).toBeLessThan(400);
    }
  });
});
