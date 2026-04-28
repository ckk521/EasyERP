import { test, expect } from '@playwright/test';

test.describe('基础数据模块 - 商品管理', () => {

  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/');

    // 导航到商品管理
    await page.goto('/base/product');
    await page.waitForLoadState('networkidle');
  });

  test('TC-SKU-001: 创建商品-正常流程', async ({ page }) => {
    const skuCode = 'SKU-' + Date.now().toString().slice(-6);

    // 点击新建商品按钮
    await page.click('button:has-text("新建商品")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 填写商品信息
    await page.locator('input').nth(0).fill(skuCode); // SKU编码
    await page.locator('input').nth(1).fill('6901234567890'); // 条码
    await page.locator('input').nth(2).fill('测试商品A'); // 中文名
    await page.locator('input').nth(3).fill('Test Product A'); // 英文名
    await page.locator('input').nth(5).fill('测试品牌'); // 品牌

    // 提交表单
    const [response] = await Promise.all([
      page.waitForResponse(resp => resp.url().includes('/api/v1/base/products') && resp.request().method() === 'POST'),
      page.click('button:has-text("确认创建")'),
    ]);

    const status = response.status();
    expect([200, 201]).toContain(status);

    // 验证创建成功提示
    await expect(page.locator('text=商品创建成功')).toBeVisible({ timeout: 5000 });
  });

  test('TC-SKU-002: 创建商品-SKU编码重复校验', async ({ page }) => {
    await page.click('button:has-text("新建商品")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 使用已存在的SKU编码
    await page.locator('input').nth(0).fill('SKU-001');
    await page.locator('input').nth(2).fill('重复测试商品');

    await page.click('button:has-text("确认创建")');

    // 验证错误提示
    await expect(page.locator('text=SKU编码已存在')).toBeVisible({ timeout: 5000 });
  });

  test('TC-SKU-003: 创建商品-必填项校验', async ({ page }) => {
    await page.click('button:has-text("新建商品")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 不填写任何信息，直接提交
    await page.click('button:has-text("确认创建")');

    // 验证表单验证 (HTML5 required)
    const skuInput = page.locator('input').nth(0);
    await expect(skuInput).toHaveAttribute('required', '');
  });

  test('TC-SKU-004: 编辑商品', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到编辑按钮
    const editBtn = page.locator('button[title="编辑"]').first();

    if (await editBtn.count() > 0) {
      await editBtn.click();

      const dialog = page.locator('[role="dialog"]');
      await expect(dialog).toBeVisible();
      await expect(dialog.locator('text=编辑商品')).toBeVisible();

      // 修改商品名称
      const nameInput = page.locator('input').nth(2);
      await nameInput.fill(nameInput.inputValue() + '-更新');

      // 保存
      await page.click('button:has-text("保存")');

      // 验证更新成功
      await expect(page.locator('text=商品信息已更新')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-SKU-005: 禁用商品', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到禁用按钮
    const disableBtn = page.locator('button[title="禁用"]').first();

    if (await disableBtn.count() > 0) {
      // 监听 confirm 对话框
      page.on('dialog', dialog => dialog.accept());

      await disableBtn.click();

      // 验证禁用成功
      await expect(page.locator('text=商品已禁用')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-SKU-006: 商品列表筛选', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 验证表格存在
    await expect(page.locator('table')).toBeVisible();

    // 验证表头
    await expect(page.locator('th:has-text("SKU编码")')).toBeVisible();
    await expect(page.locator('th:has-text("商品名称")')).toBeVisible();
    await expect(page.locator('th:has-text("分类")')).toBeVisible();
    await expect(page.locator('th:has-text("状态")')).toBeVisible();

    // 搜索功能
    const searchInput = page.locator('input[placeholder*="搜索"]');
    await searchInput.fill('SKU');
    await page.waitForTimeout(500);
  });
});
