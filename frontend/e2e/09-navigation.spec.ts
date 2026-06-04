import { test, expect } from './fixtures';

test.describe('Navigation générale', () => {
  const sections = [
    { path: '/', title: 'Dashboard' },
    { path: '/issuing', title: 'Issuing' },
    { path: '/acquiring', title: 'Acquiring' },
    { path: '/authorization', title: 'Authorization Engine' },
    { path: '/fraud', title: 'Fraud Detection' },
    { path: '/clearing', title: 'Clearing & Settlement' },
    { path: '/backoffice', title: 'Back Office' },
  ];

  for (const section of sections) {
    test(`navigation et titre: ${section.title}`, async ({ page }) => {
      await page.goto(section.path);
      await expect(page.locator('h2')).toHaveText(section.title);
    });
  }

  test('la sidebar contient tous les liens de navigation', async ({ page }) => {
    await page.goto('/');
    const navLinks = page.locator('nav a');
    const texts = await navLinks.allTextContents();
    const joined = texts.join('|');
    expect(joined).toContain('Dashboard');
    expect(joined).toContain('Issuing');
    expect(joined).toContain('Acquiring');
    expect(joined).toContain('Authorization');
    expect(joined).toContain('Fraud');
    expect(joined).toContain('Clearing');
    expect(joined).toContain('Back Office');
    expect(joined).toContain('E-Commerce');
    expect(joined).toContain('Transactions');
    expect(joined).toContain('Participants');
    expect(joined).toContain('Routing Rules');
    expect(joined).toContain('BIN Tables');
  });

  test('le lien actif est mis en évidence', async ({ page }) => {
    await page.goto('/acquiring');
    const activeLink = page.locator('nav a[href="/acquiring"]');
    await expect(activeLink).toBeVisible();
    const color = await activeLink.evaluate(el => getComputedStyle(el).color);
    expect(color).toBe('rgb(96, 165, 250)');
  });
});
