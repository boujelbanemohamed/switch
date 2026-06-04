import { test as base, expect } from '@playwright/test';

export const test = base.extend({
  page: async ({ page }, use) => {
    await page.addInitScript(() => {
      const token = localStorage.getItem('accessToken');
      if (token) {
        sessionStorage.setItem('accessToken', token);
      }
    });
    await use(page);
  },
});

export { expect };
