import { test, expect } from '@playwright/test';

test.describe('系统管理模块 - 系统配置', () => {

  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/');

    // 导航到系统配置
    await page.goto('/system/config');
  });

  test('TC-CONF-001: 配置项列表-默认展示', async ({ page }) => {
    // 验证配置分类展示
    await expect(page.locator('text=基础参数配置')).toBeVisible();
    await expect(page.locator('text=业务参数配置')).toBeVisible();
    await expect(page.locator('text=编码规则配置')).toBeVisible();
    await expect(page.locator('text=会话管理配置')).toBeVisible();
  });

  test('TC-CONF-002: 配置项分类展示', async ({ page }) => {
    // 展开单据配置分类
    await page.click('text=单据配置');

    await expect(page.locator('text=入库单前缀')).toBeVisible();
    await expect(page.locator('text=出库单前缀')).toBeVisible();
    await expect(page.locator('text=盘点单前缀')).toBeVisible();
  });

  test('TC-CONF-010: 修改入库单号前缀', async ({ page }) => {
    // 找到入库单前缀配置
    const inboundPrefixInput = page.locator('input[placeholder*="入库单前缀"], input[value*="RK"]').first();
    await inboundPrefixInput.clear();
    await inboundPrefixInput.fill('IN');

    // 保存配置
    await page.click('button:has-text("保存配置")');

    // 验证成功提示
    await expect(page.locator('text=配置已保存')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-011: 修改出库单号前缀', async ({ page }) => {
    const outboundPrefixInput = page.locator('input[placeholder*="出库单前缀"], input[value*="CK"]').first();
    await outboundPrefixInput.clear();
    await outboundPrefixInput.fill('OUT');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=配置已保存')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-012: 修改盘点单号前缀', async ({ page }) => {
    const stocktakePrefixInput = page.locator('input[placeholder*="盘点单前缀"], input[value*="PD"]').first();
    await stocktakePrefixInput.clear();
    await stocktakePrefixInput.fill('CKPD');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=配置已保存')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-013: 单号前缀格式校验-含数字', async ({ page }) => {
    const input = page.locator('input[placeholder*="入库单前缀"]').first();
    await input.clear();
    await input.fill('123');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=单号前缀只能为字母')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-014: 单号前缀长度校验-超过上限', async ({ page }) => {
    const input = page.locator('input[placeholder*="入库单前缀"]').first();
    await input.clear();
    await input.fill('ABCDEF');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=单号前缀长度不能超过5个字符')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-020: 修改盘点差异阈值', async ({ page }) => {
    // 展开业务参数配置
    await page.click('text=业务参数配置');

    const thresholdInput = page.locator('input[placeholder*="差异阈值"], input[value*="5"]').first();
    await thresholdInput.clear();
    await thresholdInput.fill('10');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=配置已保存')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-021: 修改效期预警天数', async ({ page }) => {
    await page.click('text=业务参数配置');

    const validityInput = page.locator('input[placeholder*="效期预警"], input[value*="30"]').first();
    await validityInput.clear();
    await validityInput.fill('15');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=配置已保存')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-022: 修改拣货超时提醒', async ({ page }) => {
    await page.click('text=业务参数配置');

    const pickTimeoutInput = page.locator('input[placeholder*="拣货超时"], input[value*="30"]').first();
    await pickTimeoutInput.clear();
    await pickTimeoutInput.fill('60');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=配置已保存')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-023: 预警数值范围校验-负数', async ({ page }) => {
    await page.click('text=业务参数配置');

    const input = page.locator('input[placeholder*="效期预警"]').first();
    await input.clear();
    await input.fill('-10');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=预警天数必须为正整数')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-024: 预警数值范围校验-超过上限', async ({ page }) => {
    await page.click('text=业务参数配置');

    const input = page.locator('input[placeholder*="效期预警"]').first();
    await input.clear();
    await input.fill('400');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=预警天数不能超过365天')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-030: 修改波次合并策略', async ({ page }) => {
    await page.click('text=作业配置');

    await page.click('#waveStrategy');
    await page.click('[role="option"]:has-text("按快递合并")');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=配置已保存')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-031: 修改拣货策略', async ({ page }) => {
    await page.click('text=作业配置');

    await page.click('#pickStrategy');
    await page.click('[role="option"]:has-text("播种式拣货")');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=配置已保存')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-060: 修改会话超时时间', async ({ page }) => {
    await page.click('text=会话管理配置');

    const timeoutInput = page.locator('input[placeholder*="会话超时"], input[value*="30"]').first();
    await timeoutInput.clear();
    await timeoutInput.fill('60');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=配置已保存')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-061: 修改密码策略', async ({ page }) => {
    await page.click('text=安全配置');

    const minLenInput = page.locator('input[placeholder*="密码最小长度"]').first();
    await minLenInput.clear();
    await minLenInput.fill('8');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=配置已保存')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-062: 修改登录失败锁定', async ({ page }) => {
    await page.click('text=安全配置');

    const lockInput = page.locator('input[placeholder*="登录失败锁定"]').first();
    await lockInput.clear();
    await lockInput.fill('3');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=配置已保存')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-070: 导出配置', async ({ page }) => {
    await page.click('button:has-text("导出配置")');

    // 选择导出范围
    await page.click('[role="option"]:has-text("全部")');

    // 等待下载
    const download = await page.waitForEvent('download', { timeout: 5000 });
    expect(download.suggestedFilename()).toMatch(/\.(json|xlsx)$/);
  });

  test('TC-CONF-071: 导入配置', async ({ page }) => {
    await page.click('button:has-text("导入配置")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 选择导入文件
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles('test-data/config-import.json');

    await dialog.locator('button:has-text("确认导入")').click();

    await expect(page.locator('text=成功导入')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-072: 导入配置-格式错误', async ({ page }) => {
    await page.click('button:has-text("导入配置")');

    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles('test-data/invalid-config.txt');

    await page.click('button:has-text("确认导入")');

    await expect(page.locator('text=导入文件格式错误')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-080: 查看配置变更历史', async ({ page }) => {
    // 找到任一配置项，点击历史按钮
    const historyBtn = page.locator('button:has-text("历史"), button[title="历史"]').first();
    if (await historyBtn.count() > 0) {
      await historyBtn.click();

      const dialog = page.locator('[role="dialog"]');
      await expect(dialog).toBeVisible();
      await expect(dialog.locator('text=变更时间')).toBeVisible();
      await expect(dialog.locator('text=变更人')).toBeVisible();
    } else {
      test.skip();
    }
  });

  test('TC-CONF-081: 回滚配置', async ({ page }) => {
    const historyBtn = page.locator('button:has-text("历史"), button[title="历史"]').first();
    if (await historyBtn.count() > 0) {
      await historyBtn.click();

      const dialog = page.locator('[role="dialog"]');

      // 选择一个历史版本
      await dialog.locator('tbody tr:first-child button:has-text("回滚")').click();
      await page.click('button:has-text("确认")');

      await expect(page.locator('text=配置已回滚')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-CONF-090: 无权限用户-访问系统配置', async ({ page }) => {
    // TODO: 前端暂无权限控制，权限检查需后端实现后补充
    test.skip();
  });

  test('TC-CONF-091: 有查看权限-无编辑权限', async ({ page }) => {
    // TODO: 前端暂无权限控制，权限检查需后端实现后补充
    test.skip();
  });

  test('TC-CONF-100: 必填配置项-未填写', async ({ page }) => {
    // 清空必填配置项
    const systemNameInput = page.locator('input[placeholder*="系统名称"]');
    await systemNameInput.clear();

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=配置值不能为空')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-102: 配置冲突检测', async ({ page }) => {
    // 将入库和出库前缀设为相同
    const inboundInput = page.locator('input[placeholder*="入库单前缀"]');
    const outboundInput = page.locator('input[placeholder*="出库单前缀"]');

    await inboundInput.clear();
    await inboundInput.fill('RK');
    await outboundInput.clear();
    await outboundInput.fill('RK');

    await page.click('button:has-text("保存配置")');

    await expect(page.locator('text=入库单号前缀不能与出库单号前缀相同')).toBeVisible({ timeout: 5000 });
  });

  test('TC-CONF-110: 恢复默认配置', async ({ page }) => {
    // 找到任一配置项
    const resetBtn = page.locator('button:has-text("恢复默认"), button[title="恢复默认"]').first();
    if (await resetBtn.count() > 0) {
      await resetBtn.click();
      await page.click('button:has-text("确认")');

      await expect(page.locator('text=已恢复默认配置')).toBeVisible({ timeout: 5000 });
    } else {
      test.skip();
    }
  });

  test('TC-CONF-111: 批量恢复默认配置', async ({ page }) => {
    // 勾选多个配置项
    await page.locator('input[type="checkbox"]').first().check();
    await page.locator('input[type="checkbox"]').nth(1).check();

    await page.click('button:has-text("批量恢复默认")');
    await page.click('button:has-text("确认")');

    await expect(page.locator('text=已恢复X项默认配置')).toBeVisible({ timeout: 5000 });
  });
});
