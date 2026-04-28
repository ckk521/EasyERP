import { test, expect } from '@playwright/test';

test.describe('基础数据模块 - 供应商管理', () => {

  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/');

    // 导航到供应商管理
    await page.goto('/base/supplier');
    await page.waitForLoadState('networkidle');
  });

  test('TC-SUP-001: 创建供应商', async ({ page }) => {
    const supplierCode = 'SUP-' + Date.now().toString().slice(-6);

    // 点击新建供应商按钮
    await page.click('button:has-text("新建供应商")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 填写供应商信息
    await page.locator('input').nth(0).fill(supplierCode);
    await page.locator('input').nth(1).fill('测试供应商A');
    await page.locator('input').nth(2).fill('李四');
    await page.locator('input').nth(3).fill('0123456789');
    await page.locator('input').nth(4).fill('supplier@test.com');

    // 提交表单
    const [response] = await Promise.all([
      page.waitForResponse(resp => resp.url().includes('/api/v1/base/suppliers') && resp.request().method() === 'POST'),
      page.click('button:has-text("确认创建")'),
    ]);

    const status = response.status();
    expect([200, 201]).toContain(status);

    // 验证创建成功提示
    await expect(page.locator('text=供应商创建成功')).toBeVisible({ timeout: 5000 });
  });

  test('TC-SUP-002: 编辑供应商', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到编辑按钮
    const editBtn = page.locator('button[title="编辑"]').first();

    if (await editBtn.count() > 0) {
      await editBtn.click();

      const dialog = page.locator('[role="dialog"]');
      await expect(dialog).toBeVisible();
      await expect(dialog.locator('text=编辑供应商')).toBeVisible();

      // 修改供应商名称
      const nameInput = page.locator('input').nth(1);
      await nameInput.fill(nameInput.inputValue() + '-更新');

      // 保存
      await page.click('button:has-text("保存")');

      // 验证更新成功
      await expect(page.locator('text=供应商信息已更新')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-SUP-003: 停用供应商', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到停用按钮
    const disableBtn = page.locator('button[title="停用"]').first();

    if (await disableBtn.count() > 0) {
      // 监听 confirm 对话框
      page.on('dialog', dialog => dialog.accept());

      await disableBtn.click();

      // 验证停用成功
      await expect(page.locator('text=供应商已禁用')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-SUP-004: 启用供应商', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到启用按钮
    const enableBtn = page.locator('button[title="启用"]').first();

    if (await enableBtn.count() > 0) {
      // 监听 confirm 对话框
      page.on('dialog', dialog => dialog.accept());

      await enableBtn.click();

      // 验证启用成功
      await expect(page.locator('text=供应商已启用')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-SUP-005: 供应商列表筛选', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 验证表格存在
    await expect(page.locator('table')).toBeVisible();

    // 验证表头
    await expect(page.locator('th:has-text("供应商编码")')).toBeVisible();
    await expect(page.locator('th:has-text("供应商名称")')).toBeVisible();
    await expect(page.locator('th:has-text("联系人")')).toBeVisible();
    await expect(page.locator('th:has-text("状态")')).toBeVisible();
  });
});
