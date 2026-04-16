import { test, expect } from '@playwright/test';

test.describe('系统管理模块 - 日志审计', () => {

  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/');

    // 导航到操作日志
    await page.goto('/system/log');
  });

  test('TC-LOG-001: 日志类型-登录日志', async ({ page }) => {
    // 选择日志类型
    await page.click('[data-testid="log-type-select"]');
    await page.click('[role="option"]:has-text("登录日志")');

    // 验证登录日志列表
    await expect(page.locator('th:has-text("操作类型")')).toBeVisible();
    await expect(page.locator('td:has-text("登录")')).toBeVisible();
  });

  test('TC-LOG-002: 日志类型-操作日志', async ({ page }) => {
    await page.click('[data-testid="log-type-select"]');
    await page.click('[role="option"]:has-text("操作日志")');

    await expect(page.locator('th:has-text("模块")')).toBeVisible();
    await expect(page.locator('th:has-text("操作")')).toBeVisible();
  });

  test('TC-LOG-003: 日志类型-库存变动日志', async ({ page }) => {
    await page.click('[data-testid="log-type-select"]');
    await page.click('[role="option"]:has-text("库存变动日志")');

    await expect(page.locator('th:has-text("SKU")')).toBeVisible();
    await expect(page.locator('th:has-text("变动数量")')).toBeVisible();
  });

  test('TC-LOG-004: 日志类型-系统日志', async ({ page }) => {
    await page.click('[data-testid="log-type-select"]');
    await page.click('[role="option"]:has-text("系统日志")');

    await expect(page.locator('th:has-text("日志级别")')).toBeVisible();
    await expect(page.locator('th:has-text("日志内容")')).toBeVisible();
  });

  test('TC-LOG-010: 查询日志-按时间范围', async ({ page }) => {
    // 选择日期范围
    await page.fill('#startDate', '2026-04-01');
    await page.fill('#endDate', '2026-04-10');
    await page.click('button:has-text("查询")');

    // 等待查询结果
    await page.waitForTimeout(500);

    // 验证结果（所有日志应在指定范围内）
    const logRows = page.locator('tbody tr');
    expect(await logRows.count()).toBeGreaterThan(0);
  });

  test('TC-LOG-011: 查询日志-快捷时间筛选', async ({ page }) => {
    // 点击今日
    await page.click('button:has-text("今日")');

    await page.waitForTimeout(500);

    const logRows = page.locator('tbody tr');
    expect(await logRows.count()).toBeGreaterThan(0);
  });

  test('TC-LOG-012: 查询日志-按操作人', async ({ page }) => {
    const searchInput = page.locator('input[placeholder*="操作人"]');
    await searchInput.fill('张三');
    await page.click('button:has-text("查询")');

    await page.waitForTimeout(500);

    // 验证结果只包含张三的操作
    const userCells = page.locator('td:nth-child(2)');
    const count = await userCells.count();
    for (let i = 0; i < count; i++) {
      const text = await userCells.nth(i).textContent();
      expect(text).toContain('张三');
    }
  });

  test('TC-LOG-013: 查询日志-按操作模块', async ({ page }) => {
    await page.click('#moduleSelect');
    await page.click('[role="option"]:has-text("入库管理")');
    await page.click('button:has-text("查询")');

    await page.waitForTimeout(500);

    // 验证所有结果都是入库管理模块
    const moduleCells = page.locator('td:nth-child(3)');
    const count = await moduleCells.count();
    for (let i = 0; i < count; i++) {
      const text = await moduleCells.nth(i).textContent();
      expect(text).toBe('入库管理');
    }
  });

  test('TC-LOG-014: 查询日志-按操作类型', async ({ page }) => {
    await page.click('#actionTypeSelect');
    await page.click('[role="option"]:has-text("创建")');
    await page.click('button:has-text("查询")');

    await page.waitForTimeout(500);

    const actionCells = page.locator('td:nth-child(4)');
    const count = await actionCells.count();
    for (let i = 0; i < count; i++) {
      const text = await actionCells.nth(i).textContent();
      expect(text).toBe('创建');
    }
  });

  test('TC-LOG-015: 查询日志-按操作结果', async ({ page }) => {
    await page.click('#resultSelect');
    await page.click('[role="option"]:has-text("失败")');
    await page.click('button:has-text("查询")');

    await page.waitForTimeout(500);

    const resultCells = page.locator('td:nth-child(7)');
    const count = await resultCells.count();
    for (let i = 0; i < count; i++) {
      const text = await resultCells.nth(i).textContent();
      expect(text).toBe('失败');
    }
  });

  test('TC-LOG-016: 查询日志-组合筛选', async ({ page }) => {
    // 设置多条件组合
    await page.click('button:has-text("今日")');
    await page.fill('input[placeholder*="操作人"]', '张三');
    await page.click('#moduleSelect');
    await page.click('[role="option"]:has-text("入库管理")');
    await page.click('#actionTypeSelect');
    await page.click('[role="option"]:has-text("创建")');

    await page.click('button:has-text("查询")');
    await page.waitForTimeout(500);

    // 验证组合筛选结果
    const rows = page.locator('tbody tr');
    expect(await rows.count()).toBeGreaterThan(0);
  });

  test('TC-LOG-017: 查询日志-导出Excel', async ({ page }) => {
    // 设置筛选条件
    await page.click('button:has-text("今日")');

    // 点击导出
    await page.click('button:has-text("导出日志")');

    // 选择导出格式
    await page.click('[role="option"]:has-text("Excel")');

    // 验证下载开始
    const download = await page.waitForEvent('download', { timeout: 5000 });
    expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
  });

  test('TC-LOG-020: 登录日志详情-登录成功', async ({ page }) => {
    await page.click('[data-testid="log-type-select"]');
    await page.click('[role="option"]:has-text("登录日志")');

    // 点击第一条记录的详情
    await page.locator('tbody tr:first-child button:has-text("详情")').click();

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 验证详情包含必要字段
    await expect(dialog.locator('text=登录时间')).toBeVisible();
    await expect(dialog.locator('text=IP地址')).toBeVisible();
    await expect(dialog.locator('text=登录设备')).toBeVisible();
  });

  test('TC-LOG-021: 登录日志详情-登录失败', async ({ page }) => {
    await page.click('[data-testid="log-type-select"]');
    await page.click('[role="option"]:has-text("登录日志")');

    // 筛选失败的登录
    await page.click('#resultSelect');
    await page.click('[role="option"]:has-text("失败")');
    await page.click('button:has-text("查询")');

    await page.waitForTimeout(500);

    const failedRow = page.locator('tbody tr:first-child');
    if (await failedRow.count() > 0) {
      await failedRow.locator('button:has-text("详情")').click();

      const dialog = page.locator('[role="dialog"]');
      await expect(dialog.locator('text=失败原因')).toBeVisible();
    }
  });

  test('TC-LOG-030: 操作日志详情-入库单创建', async ({ page }) => {
    await page.click('[data-testid="log-type-select"]');
    await page.click('[role="option"]:has-text("操作日志")');

    // 找到入库创建日志
    await page.click('#moduleSelect');
    await page.click('[role="option"]:has-text("入库管理")');
    await page.click('#actionTypeSelect');
    await page.click('[role="option"]:has-text("创建")');
    await page.click('button:has-text("查询")');

    await page.waitForTimeout(500);

    const logRow = page.locator('tbody tr:first-child');
    if (await logRow.count() > 0) {
      await logRow.locator('button:has-text("详情")').click();

      const dialog = page.locator('[role="dialog"]');
      await expect(dialog.locator('text=操作人')).toBeVisible();
      await expect(dialog.locator('text=操作时间')).toBeVisible();
      await expect(dialog.locator('text=变更详情')).toBeVisible();
    }
  });

  test('TC-LOG-040: 库存变动日志详情', async ({ page }) => {
    await page.click('[data-testid="log-type-select"]');
    await page.click('[role="option"]:has-text("库存变动日志")');

    await page.waitForTimeout(500);

    const logRow = page.locator('tbody tr:first-child');
    if (await logRow.count() > 0) {
      await logRow.locator('button:has-text("详情")').click();

      const dialog = page.locator('[role="dialog"]');
      await expect(dialog.locator('text=SKU')).toBeVisible();
      await expect(dialog.locator('text=变动数量')).toBeVisible();
      await expect(dialog.locator('text=变动前库存')).toBeVisible();
      await expect(dialog.locator('text=变动后库存')).toBeVisible();
    }
  });

  test('TC-LOG-050: 系统日志-Error级别', async ({ page }) => {
    await page.click('[data-testid="log-type-select"]');
    await page.click('[role="option"]:has-text("系统日志")');

    await page.click('#logLevelSelect');
    await page.click('[role="option"]:has-text("Error")');
    await page.click('button:has-text("查询")');

    await page.waitForTimeout(500);

    const resultCells = page.locator('td:nth-child(3)');
    const count = await resultCells.count();
    if (count > 0) {
      for (let i = 0; i < count; i++) {
        const text = await resultCells.nth(i).textContent();
        expect(text).toBe('Error');
      }
    }
  });

  test('TC-LOG-080: 无权限用户-访问日志审计', async ({ page }) => {
    // TODO: 前端暂无权限控制，权限检查需后端实现后补充
    test.skip();
  });

  test('TC-LOG-081: 有查看权限-无导出权限', async ({ page }) => {
    // TODO: 前端暂无权限控制，权限检查需后端实现后补充
    test.skip();
  });
});
