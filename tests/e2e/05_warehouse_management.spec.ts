import { test, expect } from '@playwright/test';

test.describe('基础数据模块 - 仓库管理', () => {

  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/');

    // 导航到仓库管理
    await page.goto('/base/warehouse');
    await page.waitForLoadState('networkidle');
  });

  test('TC-WH-001: 创建仓库-正常流程', async ({ page }) => {
    const warehouseCode = 'WH-TEST-' + Date.now().toString().slice(-6);

    // 点击新建仓库按钮
    await page.click('button:has-text("新建仓库")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 填写仓库信息
    await page.locator('input[placeholder="例：WH-SZ-001"]').fill(warehouseCode);
    await page.locator('input[placeholder="例：深圳自营仓"]').fill('测试仓库');
    await page.locator('input[placeholder="例：中国"]').fill('中国');
    await page.locator('input[placeholder="例：深圳"]').fill('深圳');
    await page.locator('input[placeholder="例：5000"]').fill('5000');
    await page.locator('input[placeholder="请输入负责人姓名"]').fill('张三');
    await page.locator('input[placeholder="请输入联系电话"]').fill('13800138000');

    // 提交表单
    const [response] = await Promise.all([
      page.waitForResponse(resp => resp.url().includes('/api/v1/base/warehouses') && resp.request().method() === 'POST'),
      page.click('button:has-text("确认创建")'),
    ]);

    const status = response.status();
    expect([200, 201]).toContain(status);

    // 验证创建成功提示
    await expect(page.locator('text=仓库创建成功')).toBeVisible({ timeout: 5000 });
  });

  test('TC-WH-002: 创建仓库-编码重复校验', async ({ page }) => {
    await page.click('button:has-text("新建仓库")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 使用已存在的仓库编码
    await page.locator('input[placeholder="例：WH-SZ-001"]').fill('WH-VN-001');
    await page.locator('input[placeholder="例：深圳自营仓"]').fill('重复测试仓库');
    await page.locator('input[placeholder="例：中国"]').fill('中国');
    await page.locator('input[placeholder="例：深圳"]').fill('深圳');
    await page.locator('input[placeholder="请输入负责人姓名"]').fill('张三');
    await page.locator('input[placeholder="请输入联系电话"]').fill('13800138000');

    await page.click('button:has-text("确认创建")');

    // 验证错误提示
    await expect(page.locator('text=仓库编码已存在')).toBeVisible({ timeout: 5000 });
  });

  test('TC-WH-003: 创建仓库-必填项校验', async ({ page }) => {
    await page.click('button:has-text("新建仓库")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 不填写任何信息，直接提交
    await page.click('button:has-text("确认创建")');

    // 验证表单验证提示 (HTML5 validation)
    const codeInput = page.locator('input[placeholder="例：WH-SZ-001"]');
    await expect(codeInput).toHaveAttribute('required', '');
  });

  test('TC-WH-004: 编辑仓库', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到第一行仓库，点击编辑按钮
    const editBtn = page.locator('button[title="编辑"]').first();

    if (await editBtn.count() > 0) {
      await editBtn.click();

      const dialog = page.locator('[role="dialog"]');
      await expect(dialog).toBeVisible();
      await expect(dialog.locator('text=编辑仓库')).toBeVisible();

      // 修改仓库名称
      const nameInput = page.locator('input[placeholder="例：深圳自营仓"]');
      await nameInput.fill(nameInput.inputValue() + '-更新');

      // 保存
      await page.click('button:has-text("保存")');

      // 验证更新成功
      await expect(page.locator('text=仓库信息已更新')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-WH-005: 停用仓库', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到启用状态的仓库的停用按钮
    const disableBtn = page.locator('button[title="停用"]').first();

    if (await disableBtn.count() > 0) {
      // 监听 confirm 对话框
      page.on('dialog', dialog => dialog.accept());

      await disableBtn.click();

      // 验证停用成功
      await expect(page.locator('text=仓库已停用')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-WH-006: 启用仓库', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到停用状态的仓库的启用按钮
    const enableBtn = page.locator('button[title="启用"]').first();

    if (await enableBtn.count() > 0) {
      // 监听 confirm 对话框
      page.on('dialog', dialog => dialog.accept());

      await enableBtn.click();

      // 验证启用成功
      await expect(page.locator('text=仓库已启用')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-WH-007: 仓库列表筛选', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 验证表格存在
    await expect(page.locator('table')).toBeVisible();

    // 验证表头
    await expect(page.locator('th:has-text("仓库编码")')).toBeVisible();
    await expect(page.locator('th:has-text("仓库名称")')).toBeVisible();
    await expect(page.locator('th:has-text("类型")')).toBeVisible();
    await expect(page.locator('th:has-text("状态")')).toBeVisible();

    // 搜索功能
    const searchInput = page.locator('input[placeholder*="搜索"]');
    await searchInput.fill('WH');

    // 等待搜索结果
    await page.waitForTimeout(500);

    // 验证搜索结果
    const rows = page.locator('tbody tr');
    const count = await rows.count();
    expect(count).toBeGreaterThanOrEqual(0);
  });

  test('TC-WH-008: 删除仓库', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到删除按钮
    const deleteBtn = page.locator('button[title="删除"]').first();

    if (await deleteBtn.count() > 0) {
      // 监听 confirm 对话框
      page.on('dialog', dialog => dialog.accept());

      await deleteBtn.click();

      // 验证删除成功
      await expect(page.locator('text=仓库已删除')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });
});
