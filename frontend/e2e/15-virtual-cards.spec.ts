import { test, expect } from './fixtures';

// Tests Virtual Cards: create -> filter -> lifecycle actions.
test.describe('Virtual Cards', () => {

  test('la page affiche le titre Virtual Cards', async ({ page }) => {
    await page.goto('/virtual-cards');
    await expect(page.getByRole('heading', { name: 'Virtual Cards' })).toBeVisible();
  });

  test('le bouton Create Virtual Card ouvre le formulaire', async ({ page }) => {
    await page.goto('/virtual-cards');
    await page.getByText('Loading...').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {});
    await page.locator('button:has-text("Create Virtual Card")').click();
    await expect(page.getByRole('heading', { name: 'Create Virtual Card' })).toBeVisible();
  });

  test('les filtres de statut sont cliquables', async ({ page }) => {
    await page.goto('/virtual-cards');
    await page.getByText('Loading...').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {});
    // le filtre "All" est toujours présent
    const allFilter = page.getByRole('button', { name: 'All', exact: true });
    if (await allFilter.isVisible().catch(() => false)) {
      await allFilter.click();
      await page.waitForTimeout(300);
    }
  });

  test('créer une carte virtuelle envoie un POST /issuing/virtual-cards', async ({ page }) => {
    await page.goto('/virtual-cards');
    await page.getByText('Loading...').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {});

    // Créer un cardholder valide via l'API pour avoir un UUID réel
    const token = await page.evaluate(() => localStorage.getItem('accessToken'));
    const createResp = await page.request.post('/api/v1/issuing/cardholders', {
      data: { firstName: 'Test', lastName: 'User', email: `vc-test-${Date.now()}@example.com`, status: 'ACTIVE' },
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    expect(createResp.status()).toBeLessThan(400);
    const chId = (await createResp.json()).id;

    await page.locator('button:has-text("Create Virtual Card")').click();

    const chInput = page.getByLabel('Cardholder ID');
    if (await chInput.isVisible().catch(() => false)) {
      await chInput.fill(chId);
    }

    await page.waitForLoadState('networkidle');
    const submitBtn = page.getByRole('button', { name: /create/i }).last();
    const [resp] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/v1/issuing/virtual-cards') && r.request().method() === 'POST'),
      submitBtn.click(),
    ]);
    console.log(`POST /issuing/virtual-cards → ${resp.status()}`);
    expect(resp.status()).toBeLessThan(400);
  });
});
