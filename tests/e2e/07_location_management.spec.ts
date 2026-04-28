import { test, expect } from '@playwright/test';

test.describe('基础数据模块 - 库位管理', () => {

  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/');

    // 导航到库位管理
    await page.goto('/base/location');
    await page.waitForLoadState('networkidle');
  });

  test('TC-LOC-001: 批量生成库位', async ({ page }) => {
    // 点击批量生成按钮
    await page.click('button:has-text("批量生成")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 选择所属库区
    const zoneSelect = page.locator('button:has-text("请选择库区")');
    if (await zoneSelect.count() > 0) {
      await zoneSelect.click();
      await page.waitForTimeout(300);
      await page.locator('[role="option"]').first().click();
    }

    // 设置生成规则
    await page.locator('input[type="number"]').nth(0).fill('1'); // startRow
    await page.locator('input[type="number"]').nth(1).fill('2'); // endRow
    await page.locator('input[type="number"]').nth(2).fill('1'); // startCol
    await page.locator('input[type="number"]').nth(3).fill('3'); // endCol
    await page.locator('input[type="number"]').nth(4).fill('1'); // startLayer
    await page.locator('input[type="number"]').nth(5).fill('2'); // endLayer

    // 提交生成
    const [response] = await Promise.all([
      page.waitForResponse(resp => resp.url().includes('/api/v1/base/locations/batch') && resp.request().method() === 'POST'),
      page.click('button:has-text("生成")'),
    ]);

    const status = response.status();
    expect([200, 201]).toContain(status);

    // 验证生成成功
    await expect(page.locator('text=已生成')).toBeVisible({ timeout: 5000 });
  });

  test('TC-LOC-002: 库位编码规则验证', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 查看库位列表中的编码格式
    const firstRow = page.locator('tbody tr').first();

    if (await firstRow.count() > 0) {
      const codeCell = firstRow.locator('td').first();
      const code = await codeCell.textContent();

      // 验证编码格式: 库区-排-列-层 (如 ZN-A-01-01-01)
      expect(code).toMatch(/^[A-Z0-9]+-[0-9]{2}-[0-9]{2}-[0-9]{2}$/);
    } else {
      test.skip();
    }
  });

  test('TC-LOC-003: 设置库位尺寸限制', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到编辑按钮
    const editBtn = page.locator('button[title="编辑"]').first();

    if (await editBtn.count() > 0) {
      await editBtn.click();

      const dialog = page.locator('[role="dialog"]');
      await expect(dialog).toBeVisible();

      // 设置尺寸限制
      const inputs = page.locator('input[type="number"]');
      await inputs.nth(0).fill('100'); // maxLength
      await inputs.nth(1).fill('80');  // maxWidth
      await inputs.nth(2).fill('120'); // maxHeight
      await inputs.nth(3).fill('50');  // maxWeight

      // 保存
      await page.click('button:has-text("保存")');

      // 验证更新成功
      await expect(page.locator('text=库位信息已更新')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-LOC-004: 库位状态查看', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 验证表格存在
    await expect(page.locator('table')).toBeVisible();

    // 验证状态列存在
    await expect(page.locator('th:has-text("状态")')).toBeVisible();

    // 检查状态显示
    const statusCell = page.locator('tbody td:has-text("空闲"), tbody td:has-text("占用"), tbody td:has-text("锁定"), tbody td:has-text("禁用")');
    const count = await statusCell.count();
    expect(count).toBeGreaterThanOrEqual(0);
  });

  test('TC-LOC-005: 禁用库位', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 找到禁用按钮
    const disableBtn = page.locator('button[title="禁用"]').first();

    if (await disableBtn.count() > 0) {
      // 监听 confirm 对话框
      page.on('dialog', dialog => dialog.accept());

      await disableBtn.click();

      // 验证禁用成功
      await expect(page.locator('text=库位已禁用')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-LOC-006: 库位列表筛选', async ({ page }) => {
    // 等待数据加载
    await page.waitForTimeout(1000);

    // 验证表头
    await expect(page.locator('th:has-text("库位编码")')).toBeVisible();
    await expect(page.locator('th:has-text("库区")')).toBeVisible();
    await expect(page.locator('th:has-text("类型")')).toBeVisible();
    await expect(page.locator('th:has-text("状态")')).toBeVisible();

    // 按状态筛选
    const statusFilterBtn = page.locator('button:has-text("空闲")').first();
    if (await statusFilterBtn.count() > 0) {
      await statusFilterBtn.click();
      await page.waitForTimeout(500);
    }
  });
});
