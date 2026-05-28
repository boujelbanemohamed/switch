import { test as setup } from '@playwright/test';

setup('authenticate', async ({ page }) => {
  const res = await page.request.post('http://localhost:8085/api/v1/auth/login', {
    data: { username: 'admin', password: 'admin123' },
  });
  const body = await res.json();
  await page.goto('/');
  await page.evaluate((token) => {
    localStorage.setItem('accessToken', token);
  }, body.accessToken);
  await page.context().storageState({ path: 'e2e/.auth.json' });
});
