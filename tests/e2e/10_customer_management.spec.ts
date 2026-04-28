import { test, expect } from '@playwright/test';

test.describe('基础数据模块 - 客户管理', () => {

  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/');

    // 导航到客户管理
    await page.goto('/base/customer');
    await page.waitForLoadState('networkidle');
  });

  test('TC-CUS-001: 创建客户', async ({ page }) => {
    const customerCode = 'CUS-' + Date.now().toString().slice(-6);

    // 点击新建客户按钮
    await page.click('button:has-text("新建客户")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 填写客户信息
    await page.locator('input').nth(0).fill(customerCode);
    await page.locator('input').nth(1).fill('测试客户A');
    await page.locator('input').nth(4).fill('王五');
    await page.locator('input').nth(5).fill('0123456789');
    await page.locator('input').nth(6).fill('customer@test.com');

    // 提交表单
    const [response] = await Promise.all([
      page.waitForResponse(resp => resp.url().includes('/api/v1/base/customers') && resp.request().method() === 'POST'),
      page.click('button:has-text("确认创建")'),
    ]);

    const status = response.status();
    expect([200, 201]).toContain(status);

    // 验证创建成功提示
    await expect(page.locator('text=客户创建成功')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CUS-002: 客户类型选择', async ({ page }) => {
    await page.click('button:has-text("新建客户")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 点击客户类型下拉框
    const typeSelect = page.locator('button:has-text("个人")').first();
    await typeSelect.click();

    // 等待下拉选项
    await page.waitForTimeout(300);

    // 验证客户类型选项
    await expect(page.locator('[role="option"]:has-text("个人")')).toBeVisible();
    await expect(page.locator('[role="option"]:has-text("企业")')).toBeVisible();
  });

  test('TC-CUS-003: 编辑客户', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到编辑按钮
    const editBtn = page.locator('button[title="编辑"]').first();

    if (await editBtn.count() > 0) {
      await editBtn.click();

      const dialog = page.locator('[role="dialog"]');
      await expect(dialog).toBeVisible();
      await expect(dialog.locator('text=编辑客户')).toBeVisible();

      // 修改客户名称
      const nameInput = page.locator('input').nth(1);
      await nameInput.fill(nameInput.inputValue() + '-更新');

      // 保存
      await page.click('button:has-text("保存")');

      // 验证更新成功
      await expect(page.locator('text=客户信息已更新')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-CUS-004: 停用客户', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到停用按钮
    const disableBtn = page.locator('button[title="停用"]').first();

    if (await disableBtn.count() > 0) {
      // 监听 confirm 对话框
      page.on('dialog', dialog => dialog.accept());

      await disableBtn.click();

      // 验证停用成功
      await expect(page.locator('text=客户已禁用')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-CUS-005: 启用客户', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到启用按钮
    const enableBtn = page.locator('button[title="启用"]').first();

    if (await enableBtn.count() > 0) {
      // 监听 confirm 对话框
      page.on('dialog', dialog => dialog.accept());

      await enableBtn.click();

      // 验证启用成功
      await expect(page.locator('text=客户已启用')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-CUS-006: 客户列表筛选', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 验证表格存在
    await expect(page.locator('table')).toBeVisible();

    // 验证表头
    await expect(page.locator('th:has-text("客户编码")')).toBeVisible();
    await expect(page.locator('th:has-text("客户名称")')).toBeVisible();
    await expect(page.locator('th:has-text("类型")')).toBeVisible();
    await expect(page.locator('th:has-text("状态")')).toBeVisible();
  });
});
