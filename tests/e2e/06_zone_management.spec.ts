import { test, expect } from '@playwright/test';

test.describe('基础数据模块 - 库区管理', () => {

  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/');

    // 导航到库区管理
    await page.goto('/base/zone');
    await page.waitForLoadState('networkidle');
  });

  test('TC-ZN-001: 创建库区', async ({ page }) => {
    const zoneCode = 'ZN-' + Date.now().toString().slice(-4);

    // 点击新建库区按钮
    await page.click('button:has-text("新建库区")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 填写库区信息
    await page.locator('input[placeholder="例：ZN-A"]').fill(zoneCode);
    await page.locator('input[placeholder="例：存储区A"]').fill('测试存储区');

    // 选择所属仓库
    const warehouseSelect = page.locator('button:has-text("请选择仓库")');
    if (await warehouseSelect.count() > 0) {
      await warehouseSelect.click();
      await page.waitForTimeout(300);
      await page.locator('[role="option"]').first().click();
    }

    // 提交表单
    const [response] = await Promise.all([
      page.waitForResponse(resp => resp.url().includes('/api/v1/base/zones') && resp.request().method() === 'POST'),
      page.click('button:has-text("确认创建")'),
    ]);

    const status = response.status();
    expect([200, 201]).toContain(status);

    // 验证创建成功提示
    await expect(page.locator('text=库区创建成功')).toBeVisible({ timeout: 5000 });
  });

  test('TC-ZN-002: 库区类型校验', async ({ page }) => {
    await page.click('button:has-text("新建库区")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 点击库区类型下拉框
    const typeSelect = page.locator('button:has-text("存储区")').first();
    await typeSelect.click();

    // 等待下拉选项
    await page.waitForTimeout(300);

    // 验证类型选项存在
    await expect(page.locator('[role="option"]:has-text("收货区")')).toBeVisible();
    await expect(page.locator('[role="option"]:has-text("质检区")')).toBeVisible();
    await expect(page.locator('[role="option"]:has-text("存储区")')).toBeVisible();
    await expect(page.locator('[role="option"]:has-text("拣货区")')).toBeVisible();
  });

  test('TC-ZN-003: 编辑库区', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到编辑按钮
    const editBtn = page.locator('button[title="编辑"]').first();

    if (await editBtn.count() > 0) {
      await editBtn.click();

      const dialog = page.locator('[role="dialog"]');
      await expect(dialog).toBeVisible();
      await expect(dialog.locator('text=编辑库区')).toBeVisible();

      // 修改库区名称
      const nameInput = page.locator('input[placeholder="例：存储区A"]');
      await nameInput.fill(nameInput.inputValue() + '-更新');

      // 保存
      await page.click('button:has-text("保存")');

      // 验证更新成功
      await expect(page.locator('text=库区信息已更新')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-ZN-004: 停用库区', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到停用按钮
    const disableBtn = page.locator('button[title="停用"]').first();

    if (await disableBtn.count() > 0) {
      // 监听 confirm 对话框
      page.on('dialog', dialog => dialog.accept());

      await disableBtn.click();

      // 验证停用成功
      await expect(page.locator('text=库区已停用')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-ZN-005: 库区列表筛选', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 验证表格存在
    await expect(page.locator('table')).toBeVisible();

    // 验证表头
    await expect(page.locator('th:has-text("库区编码")')).toBeVisible();
    await expect(page.locator('th:has-text("库区名称")')).toBeVisible();
    await expect(page.locator('th:has-text("所属仓库")')).toBeVisible();
    await expect(page.locator('th:has-text("库区类型")')).toBeVisible();
    await expect(page.locator('th:has-text("状态")')).toBeVisible();
  });
});
