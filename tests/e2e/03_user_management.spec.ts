import { test, expect } from '@playwright/test';

test.describe('系统管理模块 - 用户管理', () => {

  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('#username', { timeout: 10000 });

    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/');

    // 导航到员工管理
    await page.goto('/system/user');
    await page.waitForLoadState('networkidle');
  });

  test('TC-UM-001: 创建员工-正常流程', async ({ page }) => {
    await page.waitForSelector('table', { timeout: 10000 });

    // 点击创建员工按钮
    await page.click('button:has-text("创建员工")');

    // 等待弹窗打开
    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 填写员工信息
    const uniqueId = Date.now().toString().slice(-6);
    await dialog.locator('#employeeNo').fill(`TEST${uniqueId}`);
    await dialog.locator('#name').fill('Test Employee');
    await dialog.locator('#phone').fill('13800138000');
    await dialog.locator('#email').fill('test@wms.com');
    await dialog.locator('#password').fill('test123');
    await dialog.locator('#confirmPassword').fill('test123');

    // 选择部门 - 点击下拉框
    await dialog.locator('button:has-text("请选择部门")').click();
    await page.waitForTimeout(500);
    // 点击入库组选项
    await page.locator('[role="option"]:has-text("入库组")').click();

    // 选择岗位
    await dialog.locator('button:has-text("请选择岗位")').click();
    await page.waitForTimeout(500);
    await page.locator('[role="option"]:has-text("拣货员")').click();

    // 选择角色（多选）- 点击复选框
    const roleCheckboxes = dialog.locator('input[type="checkbox"]');
    await roleCheckboxes.first().check();

    // 选择仓库（多选）
    const warehouseCheckboxes = dialog.locator('input[type="checkbox"]');
    await warehouseCheckboxes.nth(1).check();

    // 提交
    await dialog.locator('button:has-text("确认创建")').click();

    // 验证成功提示
    await expect(page.locator('text=创建成功').or(page.locator('text=成功'))).toBeVisible({ timeout: 5000 });
  });

  test('TC-UM-002: 创建员工-工号唯一性校验', async ({ page }) => {
    await page.waitForSelector('table', { timeout: 10000 });
    await page.click('button:has-text("创建员工")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 使用已存在的工号
    await dialog.locator('#employeeNo').fill('EMP005');
    await dialog.locator('#name').fill('Duplicate Test');
    await dialog.locator('#password').fill('test123');
    await dialog.locator('#confirmPassword').fill('test123');

    // 选择部门和岗位
    await dialog.locator('button:has-text("请选择部门")').click();
    await page.waitForTimeout(300);
    await page.locator('[role="option"]:has-text("入库组")').click();

    await dialog.locator('button:has-text("请选择岗位")').click();
    await page.waitForTimeout(300);
    await page.locator('[role="option"]:has-text("拣货员")').click();

    // 选择角色和仓库
    const checkboxes = dialog.locator('input[type="checkbox"]');
    await checkboxes.first().check();
    await checkboxes.nth(1).check();

    // 提交
    await dialog.locator('button:has-text("确认创建")').click();

    // 验证错误提示
    await expect(page.locator('text=工号已存在').or(page.locator('text=请使用其他工号'))).toBeVisible({ timeout: 5000 });
  });

  test('TC-UM-009: 员工列表查询', async ({ page }) => {
    await page.waitForSelector('table', { timeout: 10000 });

    // 验证表格显示
    const table = page.locator('table');
    await expect(table).toBeVisible();

    // 验证列表字段显示
    await expect(table.locator('th')).toContainText(['工号', '姓名', '部门', '岗位']);
  });

  test('TC-UM-010: 员工列表筛选', async ({ page }) => {
    await page.waitForSelector('table', { timeout: 10000 });

    // 按部门筛选
    await page.locator('button:has-text("全部部门")').click();
    await page.waitForTimeout(300);
    await page.locator('[role="option"]:has-text("入库组")').click();
    await page.click('button:has-text("查询")');

    await page.waitForTimeout(1000);

    // 验证列表只显示入库组员工
    const rows = page.locator('table tbody tr');
    const count = await rows.count();

    if (count > 0) {
      // 验证第一行的部门是入库组
      const deptCell = rows.first().locator('td').nth(2);
      const text = await deptCell.textContent();
      expect(text).toContain('入库组');
    }
  });

  test('TC-UM-011: 员工详情查看', async ({ page }) => {
    await page.waitForSelector('table', { timeout: 10000 });

    // 点击第一个员工的查看详情按钮（Eye图标）
    const firstRow = page.locator('table tbody tr').first();
    await firstRow.locator('button').first().click();

    // 等待详情弹窗
    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 验证基本信息区域
    await expect(dialog.locator('text=基本信息')).toBeVisible();
    await expect(dialog.locator('text=员工工号').or(dialog.locator('text=工号'))).toBeVisible();
    await expect(dialog.locator('text=姓名')).toBeVisible();

    // 验证工作信息区域
    await expect(dialog.locator('text=工作信息')).toBeVisible();
    await expect(dialog.locator('text=部门')).toBeVisible();
    await expect(dialog.locator('text=岗位')).toBeVisible();

    // 验证权限信息区域
    await expect(dialog.locator('text=分配角色').or(dialog.locator('text=角色'))).toBeVisible();
    await expect(dialog.locator('text=可操作仓库').or(dialog.locator('text=仓库'))).toBeVisible();
  });

  test('TC-UM-012: 启用/禁用账号', async ({ page }) => {
    await page.waitForSelector('table', { timeout: 10000 });

    // 找到启用状态的员工行
    const enabledRow = page.locator('table tbody tr').filter({ hasText: '启用' }).first();

    if (await enabledRow.count() > 0) {
      // 点击禁用按钮
      const disableBtn = enabledRow.locator('button:has(svg)').nth(1);
      await disableBtn.click();

      // 等待确认对话框
      await expect(page.locator('text=确定要禁用')).toBeVisible({ timeout: 3000 });

      // 取消（不实际禁用，避免影响其他测试）
      await page.click('button:has-text("取消")');
    }
  });

  test('TC-UM-020: 字典查询', async ({ page }) => {
    await page.waitForSelector('table', { timeout: 10000 });

    // 点击创建员工按钮来查看下拉选项
    await page.click('button:has-text("创建员工")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 检查部门下拉选项
    await dialog.locator('button:has-text("请选择部门")').click();
    await page.waitForTimeout(300);

    // 验证预置选项
    await expect(page.locator('[role="option"]:has-text("入库组")')).toBeVisible();
    await expect(page.locator('[role="option"]:has-text("出库组")')).toBeVisible();
    await expect(page.locator('[role="option"]:has-text("库存组")')).toBeVisible();
    await expect(page.locator('[role="option"]:has-text("管理组")')).toBeVisible();

    // 关闭下拉
    await page.keyboard.press('Escape');

    // 检查岗位下拉选项
    await dialog.locator('button:has-text("请选择岗位")').click();
    await page.waitForTimeout(300);

    await expect(page.locator('[role="option"]:has-text("拣货员")')).toBeVisible();
    await expect(page.locator('[role="option"]:has-text("打包员")')).toBeVisible();
    await expect(page.locator('[role="option"]:has-text("发货员")')).toBeVisible();
    await expect(page.locator('[role="option"]:has-text("收货员")')).toBeVisible();
  });

  test('TC-UM-EX-001: 创建员工-必填校验', async ({ page }) => {
    await page.waitForSelector('table', { timeout: 10000 });
    await page.click('button:has-text("创建员工")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 不填写任何信息，直接提交
    await dialog.locator('button:has-text("确认创建")').click();

    // 验证必填提示（前端验证）
    await expect(page.locator('text=员工工号不能为空').or(page.locator('text=请输入'))).toBeVisible({ timeout: 3000 });
  });

  test('TC-UM-030: 边界值测试-字段最大长度', async ({ page }) => {
    await page.waitForSelector('table', { timeout: 10000 });
    await page.click('button:has-text("创建员工")');

    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();

    // 输入超过20字符的工号
    await dialog.locator('#employeeNo').fill('EMP12345678901234567890'); // 22字符

    // 触发验证（失去焦点）
    await dialog.locator('#name').fill('Test');

    // 验证错误提示
    await expect(page.locator('text=不能超过20').or(page.locator('text=20个字符'))).toBeVisible({ timeout: 3000 });
  });
});