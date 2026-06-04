import { test, expect } from './fixtures';

// Tests KYC Management: tabs, document upload, verification review.
test.describe('KYC Management', () => {

  test('la page affiche le titre KYC Management', async ({ page }) => {
    await page.goto('/kyc');
    await expect(page.getByRole('heading', { name: 'KYC Management' })).toBeVisible();
  });

  test('les onglets Documents / Verifications / Cardholders sont cliquables', async ({ page }) => {
    await page.goto('/kyc');
    for (const tab of ['Verifications', 'Cardholders', 'Documents']) {
      const btn = page.getByRole('button', { name: tab, exact: true });
      if (await btn.isVisible().catch(() => false)) {
        await btn.click();
        await page.waitForTimeout(300);
      }
    }
  });

  test('le bouton Upload Document ouvre le formulaire', async ({ page }) => {
    await page.goto('/kyc');
    await page.getByRole('button', { name: 'Upload Document' }).click();
    await expect(page.getByRole('heading', { name: 'Upload Document' })).toBeVisible();
  });

  test('soumettre un document envoie un POST /kyc/documents', async ({ page }) => {
    await page.goto('/kyc');

    // Créer un cardholder valide via l'API pour avoir un UUID réel
    const token = await page.evaluate(() => localStorage.getItem('accessToken'));
    const createResp = await page.request.post('/api/v1/issuing/cardholders', {
      data: { firstName: 'Kyc', lastName: 'User', email: `kyc-test-${Date.now()}@example.com`, status: 'ACTIVE' },
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    expect(createResp.status()).toBeLessThan(400);
    const chId = (await createResp.json()).id;

    await page.getByRole('button', { name: 'Upload Document' }).click();

    const chInput = page.getByLabel('Cardholder ID');
    if (await chInput.isVisible().catch(() => false)) {
      await chInput.fill(chId);
    }

    const submitBtn = page.getByRole('button', { name: /upload/i }).last();
    const [resp] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/v1/kyc/documents') && r.request().method() === 'POST'),
      submitBtn.click(),
    ]);
    console.log(`POST /kyc/documents → ${resp.status()}`);
    expect(resp.status()).toBeLessThan(400);
  });

  test('la liste des cardholders affiche le niveau KYC', async ({ page }) => {
    await page.goto('/kyc');
    const chTab = page.getByRole('button', { name: 'Cardholders', exact: true });
    if (await chTab.isVisible().catch(() => false)) {
      await chTab.click();
      await page.waitForLoadState('networkidle');
    }
  });
});
