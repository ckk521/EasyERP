import { useState, useEffect } from "react";
import { Plus, Search, Pencil, Trash2, Key, UserX, UserCheck, Eye } from "lucide-react";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Checkbox } from "../../components/ui/checkbox";
import { toast } from "sonner";

interface User {
  id: number;
  username: string;
  employeeNo: string | null;
  name: string;
  phone: string;
  email: string;
  department: number | null;
  departmentName: string | null;
  position: number | null;
  positionName: string | null;
  workStatus: number;
  workStatusName: string;
  skillLevel: number | null;
  skillLevelName: string | null;
  shiftType: number | null;
  shiftTypeName: string | null;
  hireDate: string | null;
  status: number;
  statusName: string;
  lastLoginTime: string | null;
  lastLoginIp: string | null;
  loginCount: number;
  createTime: string;
  roles: { id: number; code: string; name: string }[];
  warehouses: { id: number; code: string; name: string }[];
}

interface DictItem {
  code: number;
  name: string;
  isSystem: number;
}

interface Role {
  id: number;
  code: string;
  name: string;
  status: number;
}

interface Warehouse {
  id: number;
  code: string;
  name: string;
}

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  code?: number;
}

const API_BASE = "/api/v1";

async function fetchApi<T>(url: string, options?: RequestInit): Promise<T> {
  const token = localStorage.getItem("token");
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options?.headers as Record<string, string> || {}),
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  let data: ApiResponse<T>;
  try {
    data = await response.json();
  } catch (e) {
    throw new Error(`响应解析失败: ${response.status} ${response.statusText}`);
  }

  if (!data.success) {
    if (data.code === 400 && data.data && typeof data.data === 'object') {
      const errors = data.data as Record<string, string>;
      const errorMessages = Object.entries(errors)
        .map(([field, msg]) => `${field}: ${msg}`)
        .join('; ');
      throw new Error(errorMessages || data.message || "参数校验失败");
    }
    throw new Error(data.message || "API Error");
  }

  return data.data as T;
}

export default function UserList() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [viewingUser, setViewingUser] = useState<User | null>(null);
  const [formData, setFormData] = useState({
    employeeNo: "",
    name: "",
    password: "",
    confirmPassword: "",
    phone: "",
    email: "",
    department: "",
    position: "",
    workStatus: "1",
    skillLevel: "",
    shiftType: "",
    hireDate: "",
    roleIds: [] as number[],
    warehouseIds: [] as number[],
  });
  const [departments, setDepartments] = useState<DictItem[]>([]);
  const [positions, setPositions] = useState<DictItem[]>([]);
  const [workStatuses, setWorkStatuses] = useState<DictItem[]>([]);
  const [skillLevels, setSkillLevels] = useState<DictItem[]>([]);
  const [shiftTypes, setShiftTypes] = useState<DictItem[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [filterDepartment, setFilterDepartment] = useState<string>("all");
  const [filterWorkStatus, setFilterWorkStatus] = useState<string>("all");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    fetchUsers();
    fetchDicts();
    fetchRoles();
    fetchWarehouses();
  }, []);

  async function fetchUsers(keyword?: string, dept?: string, ws?: string) {
    try {
      setLoading(true);
      const params = new URLSearchParams({ page: "1", limit: "100" });
      if (keyword) params.append("keyword", keyword);
      if (dept && dept !== "all") params.append("department", dept);
      if (ws && ws !== "all") params.append("workStatus", ws);

      const data = await fetchApi<{ list: User[] }>(`${API_BASE}/system/users?${params}`);
      setUsers(data.list || []);
    } catch (error: any) {
      toast.error("获取员工列表失败");
      setUsers([]);
    } finally {
      setLoading(false);
    }
  }

  async function fetchDicts() {
    try {
      const [deptData, posData, wsData, skillData, shiftData] = await Promise.all([
        fetchApi<DictItem[]>(`${API_BASE}/system/dict/department`),
        fetchApi<DictItem[]>(`${API_BASE}/system/dict/position`),
        fetchApi<DictItem[]>(`${API_BASE}/system/dict/work_status`),
        fetchApi<DictItem[]>(`${API_BASE}/system/dict/skill_level`),
        fetchApi<DictItem[]>(`${API_BASE}/system/dict/shift_type`),
      ]);
      setDepartments(deptData);
      setPositions(posData);
      setWorkStatuses(wsData);
      setSkillLevels(skillData);
      setShiftTypes(shiftData);
    } catch (error) {
      console.error("Failed to fetch dicts", error);
    }
  }

  async function fetchRoles() {
    try {
      const data = await fetchApi<{ list: Role[] }>(`${API_BASE}/system/roles?limit=100`);
      setRoles(data.list || []);
    } catch (error) {
      console.error("Failed to fetch roles", error);
    }
  }

  async function fetchWarehouses() {
    try {
      const data = await fetchApi<{ list: Warehouse[] }>(`${API_BASE}/base/warehouses?limit=100`);
      setWarehouses(data.list || []);
    } catch (error) {
      console.error("Failed to fetch warehouses", error);
    }
  }

  async function fetchUserDetail(userId: number) {
    try {
      const data = await fetchApi<User>(`${API_BASE}/system/users/${userId}`);
      setViewingUser(data);
      setIsDetailOpen(true);
    } catch (error: any) {
      toast.error("获取员工详情失败");
    }
  }

  const handleSearch = () => {
    fetchUsers(searchKeyword, filterDepartment, filterWorkStatus);
  };

  const handleOpenCreate = () => {
    setIsEditMode(false);
    setEditingUser(null);
    setFormData({
      employeeNo: "",
      name: "",
      password: "",
      confirmPassword: "",
      phone: "",
      email: "",
      department: "",
      position: "",
      workStatus: "1",
      skillLevel: "",
      shiftType: "",
      hireDate: "",
      roleIds: [],
      warehouseIds: [],
    });
    setFieldErrors({});
    setIsFormOpen(true);
  };

  const handleOpenEdit = async (user: User) => {
    setIsEditMode(true);
    setEditingUser(user);
    setFormData({
      employeeNo: user.employeeNo || "",
      name: user.name,
      password: "",
      confirmPassword: "",
      phone: user.phone || "",
      email: user.email || "",
      department: user.department?.toString() || "",
      position: user.position?.toString() || "",
      workStatus: user.workStatus?.toString() || "1",
      skillLevel: user.skillLevel?.toString() || "",
      shiftType: user.shiftType?.toString() || "",
      hireDate: user.hireDate || "",
      roleIds: user.roles?.map(r => r.id) || [],
      warehouseIds: user.warehouses?.map(w => w.id) || [],
    });
    setFieldErrors({});
    setIsFormOpen(true);
  };

  const validateField = (field: string): string | null => {
    switch (field) {
      // employeeNo 校验移除，因为现在是自动生成的
      case "name":
        if (!formData.name) return "姓名不能为空";
        if (formData.name.length > 100) return "姓名不能超过100个字符";
        break;
      case "phone":
        if (formData.phone && !/^1[3-9]\d{9}$/.test(formData.phone)) return "手机号格式不正确";
        break;
      case "email":
        if (formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) return "邮箱格式不正确";
        break;
      case "department":
        if (!formData.department) return "请选择部门";
        break;
      case "position":
        if (!formData.position) return "请选择岗位";
        break;
      case "roleIds":
        if (formData.roleIds.length === 0) return "请至少选择一个角色";
        break;
      case "warehouseIds":
        if (formData.warehouseIds.length === 0) return "请至少选择一个仓库";
        break;
      case "password":
        if (!isEditMode && (!formData.password || formData.password.length < 6)) return "密码长度不能少于6个字符";
        break;
      case "confirmPassword":
        if (!isEditMode && formData.password !== formData.confirmPassword) return "两次输入的密码不一致";
        break;
    }
    return null;
  };

  const handleBlur = (field: string) => {
    const error = validateField(field);
    setFieldErrors(prev => ({ ...prev, [field]: error || "" }));
  };

  const validateForm = (): string | null => {
    const errors: string[] = [];

    if (!isEditMode) {
      // employeeNo 校验移除，因为现在是自动生成的
      const pwdError = validateField("password");
      if (pwdError) errors.push(pwdError);

      const confirmPwdError = validateField("confirmPassword");
      if (confirmPwdError) errors.push(confirmPwdError);
    }

    const nameError = validateField("name");
    if (nameError) errors.push(nameError);

    const deptError = validateField("department");
    if (deptError) errors.push(deptError);

    const posError = validateField("position");
    if (posError) errors.push(posError);

    const roleError = validateField("roleIds");
    if (roleError) errors.push(roleError);

    const whError = validateField("warehouseIds");
    if (whError) errors.push(whError);

    if (formData.phone) {
      const phoneError = validateField("phone");
      if (phoneError) errors.push(phoneError);
    }

    if (formData.email) {
      const emailError = validateField("email");
      if (emailError) errors.push(emailError);
    }

    return errors.length > 0 ? errors[0] : null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const validationError = validateForm();
    if (validationError) {
      toast.error(validationError);
      return;
    }

    try {
      if (isEditMode && editingUser) {
        await fetchApi(`${API_BASE}/system/users/${editingUser.id}`, {
          method: "PUT",
          body: JSON.stringify({
            name: formData.name,
            phone: formData.phone || null,
            email: formData.email || null,
            department: parseInt(formData.department),
            position: parseInt(formData.position),
            workStatus: parseInt(formData.workStatus),
            skillLevel: formData.skillLevel ? parseInt(formData.skillLevel) : null,
            shiftType: formData.shiftType ? parseInt(formData.shiftType) : null,
            hireDate: formData.hireDate || null,
            roleIds: formData.roleIds,
            warehouseIds: formData.warehouseIds,
          }),
        });
        toast.success("员工信息已更新");
      } else {
        await fetchApi(`${API_BASE}/system/users`, {
          method: "POST",
          body: JSON.stringify({
            // employeeNo 不传，让后端自动生成
            name: formData.name,
            password: formData.password,
            phone: formData.phone || null,
            email: formData.email || null,
            department: parseInt(formData.department),
            position: parseInt(formData.position),
            workStatus: parseInt(formData.workStatus),
            skillLevel: formData.skillLevel ? parseInt(formData.skillLevel) : null,
            shiftType: formData.shiftType ? parseInt(formData.shiftType) : null,
            hireDate: formData.hireDate || null,
            roleIds: formData.roleIds,
            warehouseIds: formData.warehouseIds,
          }),
        });
        toast.success(`员工 ${formData.employeeNo} 创建成功！`);
      }
      setIsFormOpen(false);
      fetchUsers(searchKeyword, filterDepartment, filterWorkStatus);
    } catch (error: any) {
      toast.error(error.message || (isEditMode ? "更新员工失败" : "创建员工失败"));
    }
  };

  const handleEnable = async (user: User) => {
    if (!confirm(`确定要启用员工 ${user.name} 的账号吗？启用后该员工可以正常登录系统。`)) return;
    try {
      await fetchApi(`${API_BASE}/system/users/${user.id}/enable`, { method: "PATCH" });
      toast.success("账号已启用");
      fetchUsers(searchKeyword, filterDepartment, filterWorkStatus);
    } catch (error: any) {
      toast.error(error.message || "启用账号失败");
    }
  };

  const handleDisable = async (user: User) => {
    if (!confirm(`确定要禁用员工 ${user.name} 的账号吗？禁用后该员工将无法登录系统。`)) return;
    try {
      await fetchApi(`${API_BASE}/system/users/${user.id}/disable`, { method: "PATCH" });
      toast.success("账号已禁用");
      fetchUsers(searchKeyword, filterDepartment, filterWorkStatus);
    } catch (error: any) {
      toast.error(error.message || "禁用账号失败");
    }
  };

  const handleDelete = async (user: User) => {
    if (!confirm(`确定要删除员工 ${user.name} 吗？此操作不可恢复。`)) return;
    try {
      await fetchApi(`${API_BASE}/system/users/${user.id}`, { method: "DELETE" });
      toast.success("员工已删除");
      fetchUsers(searchKeyword, filterDepartment, filterWorkStatus);
    } catch (error: any) {
      toast.error(error.message || "删除员工失败");
    }
  };

  const handleResetPassword = async (user: User) => {
    if (!confirm(`确定要重置员工 ${user.name} 的密码吗？`)) return;
    try {
      const data = await fetchApi<{ newPassword: string }>(`${API_BASE}/system/users/${user.id}/reset-password`, { method: "POST" });
      toast.success(`密码已重置为: ${data.newPassword}`);
    } catch (error: any) {
      toast.error(error.message || "重置密码失败");
    }
  };

  const handleRoleChange = (roleId: number, checked: boolean) => {
    if (checked) {
      setFormData(prev => ({ ...prev, roleIds: [...prev.roleIds, roleId] }));
    } else {
      setFormData(prev => ({ ...prev, roleIds: prev.roleIds.filter(id => id !== roleId) }));
    }
    setFieldErrors(prev => ({ ...prev, roleIds: "" }));
  };

  const handleWarehouseChange = (warehouseId: number, checked: boolean) => {
    if (checked) {
      setFormData(prev => ({ ...prev, warehouseIds: [...prev.warehouseIds, warehouseId] }));
    } else {
      setFormData(prev => ({ ...prev, warehouseIds: prev.warehouseIds.filter(id => id !== warehouseId) }));
    }
    setFieldErrors(prev => ({ ...prev, warehouseIds: "" }));
  };

  const getDictName = (dicts: DictItem[], code: number | null): string => {
    if (code === null) return "-";
    const item = dicts.find(d => d.code === code);
    return item?.name || "-";
  };

  const columns = [
    { key: "employeeNo", title: "员工工号", width: "100px", render: (v: string | null) => v || "-" },
    { key: "username", title: "登录账号", width: "110px" },
    { key: "name", title: "姓名", width: "100px" },
    {
      key: "departmentName",
      title: "部门",
      width: "80px",
      render: (v: string | null, row: User) => v || getDictName(departments, row.department)
    },
    {
      key: "positionName",
      title: "岗位",
      width: "80px",
      render: (v: string | null, row: User) => v || getDictName(positions, row.position)
    },
    {
      key: "workStatusName",
      title: "工作状态",
      width: "80px",
      render: (v: string, row: User) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          row.workStatus === 1 ? 'bg-green-100 text-green-700' :
          row.workStatus === 2 ? 'bg-yellow-100 text-yellow-700' :
          'bg-gray-100 text-gray-700'
        }`}>
          {v || getDictName(workStatuses, row.workStatus)}
        </span>
      )
    },
    {
      key: "statusName",
      title: "账号状态",
      width: "80px",
      render: (v: string, row: User) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          row.status === 1 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
        }`}>
          {row.status === 1 ? '启用' : '禁用'}
        </span>
      )
    },
    {
      key: "actions",
      title: "操作",
      width: "180px",
      render: (_: any, user: User) => (
        <div className="flex items-center gap-1">
          <button
            onClick={() => fetchUserDetail(user.id)}
            className="p-1 hover:bg-gray-100 rounded text-gray-600"
            title="查看详情"
          >
            <Eye size={14} />
          </button>
          <button
            onClick={() => handleOpenEdit(user)}
            className="p-1 hover:bg-gray-100 rounded text-blue-600"
            title="编辑"
          >
            <Pencil size={14} />
          </button>
          {user.status === 1 ? (
            <button
              onClick={() => handleDisable(user)}
              className="p-1 hover:bg-gray-100 rounded text-orange-600"
              title="禁用账号"
            >
              <UserX size={14} />
            </button>
          ) : (
            <button
              onClick={() => handleEnable(user)}
              className="p-1 hover:bg-gray-100 rounded text-green-600"
              title="启用账号"
            >
              <UserCheck size={14} />
            </button>
          )}
          <button
            onClick={() => handleResetPassword(user)}
            className="p-1 hover:bg-gray-100 rounded text-gray-600"
            title="重置密码"
          >
            <Key size={14} />
          </button>
          <button
            onClick={() => handleDelete(user)}
            className="p-1 hover:bg-gray-100 rounded text-red-600"
            title="删除"
          >
            <Trash2 size={14} />
          </button>
        </div>
      )
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">员工管理</h2>
        <button
          onClick={handleOpenCreate}
          className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm"
        >
          <Plus size={16} />
          创建员工
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-2 mb-4 flex-wrap">
          <Select value={filterDepartment} onValueChange={setFilterDepartment}>
            <SelectTrigger className="w-[120px]">
              <SelectValue placeholder="全部部门" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部部门</SelectItem>
              {departments.map(d => (
                <SelectItem key={d.code} value={d.code.toString()}>{d.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select value={filterWorkStatus} onValueChange={setFilterWorkStatus}>
            <SelectTrigger className="w-[120px]">
              <SelectValue placeholder="全部状态" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部状态</SelectItem>
              {workStatuses.map(w => (
                <SelectItem key={w.code} value={w.code.toString()}>{w.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>

          <div className="relative flex-1 max-w-[300px]">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="搜索工号或姓名"
              className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            />
          </div>
          <button
            onClick={handleSearch}
            className="px-4 py-1.5 bg-gray-100 border border-gray-300 rounded text-sm hover:bg-gray-200"
          >
            查询
          </button>
        </div>

        {loading ? (
          <div className="text-center py-8 text-gray-500">加载中...</div>
        ) : (
          <DataTable columns={columns} data={users} />
        )}
      </div>

      {/* 创建/编辑员工弹窗 */}
      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{isEditMode ? "编辑员工" : "创建员工"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit}>
            <div className="space-y-6 py-4">
              {/* 基本信息 */}
              <div>
                <h3 className="text-sm font-medium mb-3 text-gray-700">基本信息</h3>
                <div className="grid grid-cols-2 gap-4">
                  {/* 员工工号字段已移除，由后端自动生成 */}
                  <div className="space-y-2">
                    <Label htmlFor="name">姓名 *</Label>
                    <Input
                      id="name"
                      required
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      onBlur={() => handleBlur("name")}
                      placeholder="请输入姓名"
                    />
                    {fieldErrors.name && <p className="text-red-500 text-xs">{fieldErrors.name}</p>}
                  </div>

                  {!isEditMode && (
                    <>
                      <div className="space-y-2">
                        <Label htmlFor="password">密码 *</Label>
                        <Input
                          id="password"
                          type="password"
                          required={!isEditMode}
                          value={formData.password}
                          onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                          onBlur={() => handleBlur("password")}
                          placeholder="请输入密码"
                        />
                        {fieldErrors.password && <p className="text-red-500 text-xs">{fieldErrors.password}</p>}
                      </div>

                      <div className="space-y-2">
                        <Label htmlFor="confirmPassword">确认密码 *</Label>
                        <Input
                          id="confirmPassword"
                          type="password"
                          required={!isEditMode}
                          value={formData.confirmPassword}
                          onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
                          onBlur={() => handleBlur("confirmPassword")}
                          placeholder="请再次输入密码"
                        />
                        {fieldErrors.confirmPassword && <p className="text-red-500 text-xs">{fieldErrors.confirmPassword}</p>}
                      </div>
                    </>
                  )}

                  <div className="space-y-2">
                    <Label htmlFor="phone">手机号</Label>
                    <Input
                      id="phone"
                      type="tel"
                      value={formData.phone}
                      onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                      onBlur={() => handleBlur("phone")}
                      placeholder="请输入手机号"
                    />
                    {fieldErrors.phone && <p className="text-red-500 text-xs">{fieldErrors.phone}</p>}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="email">邮箱</Label>
                    <Input
                      id="email"
                      type="email"
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      onBlur={() => handleBlur("email")}
                      placeholder="请输入邮箱"
                    />
                    {fieldErrors.email && <p className="text-red-500 text-xs">{fieldErrors.email}</p>}
                  </div>
                </div>
              </div>

              {/* 工作信息 */}
              <div>
                <h3 className="text-sm font-medium mb-3 text-gray-700">工作信息</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="department">部门 *</Label>
                    <Select value={formData.department} onValueChange={(v) => { setFormData({ ...formData, department: v }); setFieldErrors(prev => ({ ...prev, department: "" })); }}>
                      <SelectTrigger>
                        <SelectValue placeholder="请选择部门" />
                      </SelectTrigger>
                      <SelectContent>
                        {departments.map(d => (
                          <SelectItem key={d.code} value={d.code.toString()}>{d.name}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    {fieldErrors.department && <p className="text-red-500 text-xs">{fieldErrors.department}</p>}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="position">岗位 *</Label>
                    <Select value={formData.position} onValueChange={(v) => { setFormData({ ...formData, position: v }); setFieldErrors(prev => ({ ...prev, position: "" })); }}>
                      <SelectTrigger>
                        <SelectValue placeholder="请选择岗位" />
                      </SelectTrigger>
                      <SelectContent>
                        {positions.map(p => (
                          <SelectItem key={p.code} value={p.code.toString()}>{p.name}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    {fieldErrors.position && <p className="text-red-500 text-xs">{fieldErrors.position}</p>}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="workStatus">工作状态</Label>
                    <Select value={formData.workStatus} onValueChange={(v) => setFormData({ ...formData, workStatus: v })}>
                      <SelectTrigger>
                        <SelectValue placeholder="请选择工作状态" />
                      </SelectTrigger>
                      <SelectContent>
                        {workStatuses.map(w => (
                          <SelectItem key={w.code} value={w.code.toString()}>{w.name}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="skillLevel">技能等级</Label>
                    <Select value={formData.skillLevel} onValueChange={(v) => setFormData({ ...formData, skillLevel: v })}>
                      <SelectTrigger>
                        <SelectValue placeholder="请选择技能等级" />
                      </SelectTrigger>
                      <SelectContent>
                        {skillLevels.map(s => (
                          <SelectItem key={s.code} value={s.code.toString()}>{s.name}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="shiftType">班次</Label>
                    <Select value={formData.shiftType} onValueChange={(v) => setFormData({ ...formData, shiftType: v })}>
                      <SelectTrigger>
                        <SelectValue placeholder="请选择班次" />
                      </SelectTrigger>
                      <SelectContent>
                        {shiftTypes.map(s => (
                          <SelectItem key={s.code} value={s.code.toString()}>{s.name}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="hireDate">入职日期</Label>
                    <Input
                      id="hireDate"
                      type="date"
                      value={formData.hireDate}
                      onChange={(e) => setFormData({ ...formData, hireDate: e.target.value })}
                    />
                  </div>
                </div>
              </div>

              {/* 权限分配 */}
              <div>
                <h3 className="text-sm font-medium mb-3 text-gray-700">权限分配</h3>
                <div className="space-y-4">
                  <div>
                    <Label className="mb-2">角色分配 *（可多选）</Label>
                    <div className="grid grid-cols-3 gap-2 bg-gray-50 p-3 rounded">
                      {roles.map(role => (
                        <label key={role.id} className="flex items-center gap-2 cursor-pointer">
                          <Checkbox
                            checked={formData.roleIds.includes(role.id)}
                            onCheckedChange={(checked) => handleRoleChange(role.id, checked as boolean)}
                          />
                          <span className="text-sm">{role.name}</span>
                        </label>
                      ))}
                    </div>
                    {fieldErrors.roleIds && <p className="text-red-500 text-xs mt-1">{fieldErrors.roleIds}</p>}
                  </div>

                  <div>
                    <Label className="mb-2">仓库权限 *（可多选）</Label>
                    <div className="grid grid-cols-3 gap-2 bg-gray-50 p-3 rounded">
                      {warehouses.map(wh => (
                        <label key={wh.id} className="flex items-center gap-2 cursor-pointer">
                          <Checkbox
                            checked={formData.warehouseIds.includes(wh.id)}
                            onCheckedChange={(checked) => handleWarehouseChange(wh.id, checked as boolean)}
                          />
                          <span className="text-sm">{wh.name}</span>
                        </label>
                      ))}
                    </div>
                    {fieldErrors.warehouseIds && <p className="text-red-500 text-xs mt-1">{fieldErrors.warehouseIds}</p>}
                  </div>
                </div>
              </div>
            </div>

            <DialogFooter>
              <button
                type="button"
                onClick={() => setIsFormOpen(false)}
                className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
              >
                取消
              </button>
              <button
                type="submit"
                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
              >
                {isEditMode ? "保存" : "确认创建"}
              </button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* 员工详情弹窗 */}
      <Dialog open={isDetailOpen} onOpenChange={setIsDetailOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>员工详情</DialogTitle>
          </DialogHeader>
          {viewingUser && (
            <div className="space-y-6 py-4">
              {/* 基本信息 */}
              <div>
                <h3 className="text-sm font-medium mb-3 text-gray-700 border-b pb-2">基本信息</h3>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div><span className="text-gray-500">员工工号：</span>{viewingUser.employeeNo || "-"}</div>
                  <div><span className="text-gray-500">姓名：</span>{viewingUser.name}</div>
                  <div><span className="text-gray-500">手机号：</span>{viewingUser.phone || "-"}</div>
                  <div><span className="text-gray-500">邮箱：</span>{viewingUser.email || "-"}</div>
                  <div><span className="text-gray-500">账号状态：</span>
                    <span className={viewingUser.status === 1 ? 'text-green-600' : 'text-red-600'}>
                      {viewingUser.status === 1 ? '启用' : '禁用'}
                    </span>
                  </div>
                  <div><span className="text-gray-500">登录账号：</span>{viewingUser.username}</div>
                </div>
              </div>

              {/* 工作信息 */}
              <div>
                <h3 className="text-sm font-medium mb-3 text-gray-700 border-b pb-2">工作信息</h3>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div><span className="text-gray-500">部门：</span>{viewingUser.departmentName || getDictName(departments, viewingUser.department)}</div>
                  <div><span className="text-gray-500">岗位：</span>{viewingUser.positionName || getDictName(positions, viewingUser.position)}</div>
                  <div><span className="text-gray-500">工作状态：</span>{viewingUser.workStatusName || getDictName(workStatuses, viewingUser.workStatus)}</div>
                  <div><span className="text-gray-500">技能等级：</span>{viewingUser.skillLevelName || getDictName(skillLevels, viewingUser.skillLevel)}</div>
                  <div><span className="text-gray-500">班次：</span>{viewingUser.shiftTypeName || getDictName(shiftTypes, viewingUser.shiftType)}</div>
                  <div><span className="text-gray-500">入职日期：</span>{viewingUser.hireDate || "-"}</div>
                </div>
              </div>

              {/* 权限信息 */}
              <div>
                <h3 className="text-sm font-medium mb-3 text-gray-700 border-b pb-2">权限信息</h3>
                <div className="space-y-3">
                  <div>
                    <span className="text-gray-500 text-sm">分配角色：</span>
                    <div className="flex flex-wrap gap-2 mt-1">
                      {viewingUser.roles?.length > 0 ? viewingUser.roles.map(r => (
                        <span key={r.id} className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs">{r.name}</span>
                      )) : <span className="text-gray-400">未分配</span>}
                    </div>
                  </div>
                  <div>
                    <span className="text-gray-500 text-sm">可操作仓库：</span>
                    <div className="flex flex-wrap gap-2 mt-1">
                      {viewingUser.warehouses?.length > 0 ? viewingUser.warehouses.map(w => (
                        <span key={w.id} className="px-2 py-1 bg-green-100 text-green-700 rounded text-xs">{w.name}</span>
                      )) : <span className="text-gray-400">未分配</span>}
                    </div>
                  </div>
                </div>
              </div>

              {/* 登录信息 */}
              <div>
                <h3 className="text-sm font-medium mb-3 text-gray-700 border-b pb-2">登录信息</h3>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div><span className="text-gray-500">最后登录时间：</span>{viewingUser.lastLoginTime || "从未登录"}</div>
                  <div><span className="text-gray-500">登录次数：</span>{viewingUser.loginCount}</div>
                  <div><span className="text-gray-500">创建时间：</span>{viewingUser.createTime}</div>
                </div>
              </div>
            </div>
          )}
          <DialogFooter>
            <button
              type="button"
              onClick={() => setIsDetailOpen(false)}
              className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
            >
              关闭
            </button>
            {viewingUser && (
              <button
                type="button"
                onClick={() => { setIsDetailOpen(false); handleOpenEdit(viewingUser); }}
                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
              >
                编辑
              </button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}