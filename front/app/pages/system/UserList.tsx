import { useState, useEffect } from "react";
import { Plus, Search, Pencil, Trash2, Key, UserX, UserCheck } from "lucide-react";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { toast } from "sonner";

interface User {
  id: number;
  username: string;
  name: string;
  phone: string;
  email: string;
  roleId: number;
  roleName: string;
  warehouseId: number | null;
  warehouseName: string;
  status: number;
  statusText: string;
  lastLoginTime: string | null;
  createTime: string;
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
    // 如果是校验错误，显示具体字段错误
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
  const [isEditMode, setIsEditMode] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [formData, setFormData] = useState({
    username: "",
    name: "",
    password: "",
    confirmPassword: "",
    roleId: "",
    warehouseId: "",
    phone: "",
    email: "",
  });
  const [roles, setRoles] = useState<{id: number; name: string; status: number}[]>([]);
  const [warehouses, setWarehouses] = useState<{id: number; name: string}[]>([]);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    fetchUsers();
    fetchRoles();
    fetchWarehouses();
  }, []);

  async function fetchUsers(keyword?: string) {
    try {
      setLoading(true);
      const params = new URLSearchParams({ page: "1", limit: "100" });
      if (keyword) params.append("keyword", keyword);

      const data = await fetchApi<{ list: User[] }>(`${API_BASE}/system/users?${params}`);
      setUsers(data.list || []);
    } catch (error: any) {
      toast.error("获取用户列表失败");
      setUsers([]);
    } finally {
      setLoading(false);
    }
  }

  async function fetchRoles() {
    try {
      const data = await fetchApi<{ list: {id: number; name: string}[] }>(`${API_BASE}/system/roles?limit=100`);
      setRoles(data.list || []);
    } catch (error) {
      console.error("Failed to fetch roles", error);
    }
  }

  async function fetchWarehouses() {
    try {
      const data = await fetchApi<{ list: {id: number; name: string}[] }>(`${API_BASE}/base/warehouses?limit=100`);
      setWarehouses(data.list || []);
    } catch (error) {
      console.error("Failed to fetch warehouses", error);
    }
  }

  const handleSearch = () => {
    fetchUsers(searchKeyword);
  };

  const handleOpenCreate = () => {
    setIsEditMode(false);
    setEditingUser(null);
    setFormData({
      username: "",
      name: "",
      password: "",
      confirmPassword: "",
      roleId: "",
      warehouseId: "",
      phone: "",
      email: "",
    });
    setFieldErrors({});
    setIsFormOpen(true);
  };

  const handleOpenEdit = (user: User) => {
    setIsEditMode(true);
    setEditingUser(user);
    setFormData({
      username: user.username,
      name: user.name,
      password: "",
      confirmPassword: "",
      roleId: user.roleId?.toString() || "",
      warehouseId: user.warehouseId?.toString() || "",
      phone: user.phone || "",
      email: user.email || "",
    });
    setFieldErrors({});
    setIsFormOpen(true);
  };

  // 单个字段校验（失去焦点时调用）
  const validateField = (field: string): string | null => {
    switch (field) {
      case "username":
        if (!formData.username) return "用户名不能为空";
        if (formData.username.length < 4 || formData.username.length > 20) {
          return "用户名长度必须在4-20字符之间";
        }
        break;
      case "password":
        if (!isEditMode && (!formData.password || formData.password.length < 6 || formData.password.length > 20)) {
          return "密码长度必须在6-20字符之间";
        }
        break;
      case "confirmPassword":
        if (!isEditMode && formData.password !== formData.confirmPassword) {
          return "两次输入的密码不一致";
        }
        break;
      case "name":
        if (!formData.name || formData.name.length < 2) {
          return "姓名长度不能少于2个字符";
        }
        break;
      case "phone":
        if (!formData.phone) return "手机号不能为空";
        if (!/^1[3-9]\d{9}$/.test(formData.phone)) {
          return "手机号格式不正确";
        }
        break;
      case "email":
        if (!formData.email) return "邮箱不能为空";
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
          return "邮箱格式不正确";
        }
        break;
    }
    return null;
  };

  // 失去焦点校验
  const handleBlur = (field: string) => {
    const error = validateField(field);
    setFieldErrors(prev => ({ ...prev, [field]: error || "" }));
  };

  // 角色选择校验
  const handleRoleChange = (value: string) => {
    setFormData({ ...formData, roleId: value });
    if (!value) {
      setFieldErrors(prev => ({ ...prev, roleId: "请选择角色" }));
    } else {
      setFieldErrors(prev => ({ ...prev, roleId: "" }));
    }
  };

  // 表单校验（提交时调用）
  const validateForm = (): string | null => {
    if (!isEditMode) {
      if (!formData.username || formData.username.length < 4 || formData.username.length > 20) {
        return "用户名长度必须在4-20字符之间";
      }
      if (!formData.password || formData.password.length < 6 || formData.password.length > 20) {
        return "密码长度必须在6-20字符之间";
      }
      if (formData.password !== formData.confirmPassword) {
        return "两次输入的密码不一致";
      }
    }
    if (!formData.name || formData.name.length < 2) {
      return "姓名长度不能少于2个字符";
    }
    if (!formData.roleId) {
      return "请选择角色";
    }
    if (!formData.phone || !/^1[3-9]\d{9}$/.test(formData.phone)) {
      return "手机号格式不正确";
    }
    if (!formData.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      return "邮箱格式不正确";
    }
    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // 前端校验
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
            phone: formData.phone,
            email: formData.email,
            roleId: parseInt(formData.roleId),
            warehouseId: formData.warehouseId ? parseInt(formData.warehouseId) : null,
          }),
        });
        toast.success("用户信息已更新");
      } else {
        await fetchApi(`${API_BASE}/system/users`, {
          method: "POST",
          body: JSON.stringify({
            username: formData.username,
            name: formData.name,
            password: formData.password,
            phone: formData.phone,
            email: formData.email,
            roleId: parseInt(formData.roleId),
            warehouseId: formData.warehouseId ? parseInt(formData.warehouseId) : null,
          }),
        });
        toast.success(`用户 ${formData.username} 创建成功！`);
      }
      setIsFormOpen(false);
      fetchUsers(searchKeyword);
    } catch (error: any) {
      toast.error(error.message || (isEditMode ? "更新用户失败" : "创建用户失败"));
    }
  };

  const handleEnable = async (user: User) => {
    try {
      await fetchApi(`${API_BASE}/system/users/${user.id}/enable`, { method: "PATCH" });
      toast.success("用户已启用");
      fetchUsers();
    } catch (error: any) {
      toast.error(error.message || "启用用户失败");
    }
  };

  const handleDisable = async (user: User) => {
    try {
      await fetchApi(`${API_BASE}/system/users/${user.id}/disable`, { method: "PATCH" });
      toast.success("用户已停用");
      fetchUsers();
    } catch (error: any) {
      toast.error(error.message || "停用用户失败");
    }
  };

  const handleDelete = async (user: User) => {
    if (!confirm(`确定要删除用户 ${user.username} 吗？`)) return;
    try {
      await fetchApi(`${API_BASE}/system/users/${user.id}`, { method: "DELETE" });
      toast.success("用户已删除");
      fetchUsers();
    } catch (error: any) {
      toast.error(error.message || "删除用户失败");
    }
  };

  const handleResetPassword = async (user: User) => {
    try {
      await fetchApi(`${API_BASE}/system/users/${user.id}/reset-password`, { method: "POST" });
      toast.success("重置密码链接已发送到邮箱");
    } catch (error: any) {
      toast.error(error.message || "重置密码失败");
    }
  };

  const columns = [
    { key: "username", title: "用户名", width: "100px" },
    { key: "name", title: "姓名", width: "100px" },
    {
      key: "roleName",
      title: "角色",
      width: "110px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '系统管理员' ? 'bg-purple-100 text-purple-700' :
          value === '仓库管理员' ? 'bg-blue-100 text-blue-700' :
          'bg-green-100 text-green-700'
        }`}>
          {value}
        </span>
      )
    },
    { key: "warehouseName", title: "所属仓库", width: "100px" },
    { key: "phone", title: "手机号", width: "110px" },
    { key: "email", title: "邮箱", width: "150px" },
    { key: "lastLoginTime", title: "最后登录", width: "140px" },
    {
      key: "statusText",
      title: "状态",
      width: "80px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '正常' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'
        }`}>
          {value}
        </span>
      )
    },
    {
      key: "actions",
      title: "操作",
      width: "200px",
      render: (_: any, user: User) => (
        <div className="flex items-center gap-1">
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
              title="停用"
            >
              <UserX size={14} />
            </button>
          ) : (
            <button
              onClick={() => handleEnable(user)}
              className="p-1 hover:bg-gray-100 rounded text-green-600"
              title="启用"
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
        <h2 className="text-lg font-semibold">用户管理</h2>
        <button
          onClick={handleOpenCreate}
          className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm"
        >
          <Plus size={16} />
          新建用户
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-2 mb-4">
          <div className="relative flex-1">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="搜索用户名或姓名"
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
            搜索
          </button>
        </div>
        {loading ? (
          <div className="text-center py-8 text-gray-500">加载中...</div>
        ) : (
          <DataTable columns={columns} data={users} />
        )}
      </div>

      {/* Form Dialog */}
      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{isEditMode ? "编辑用户" : "新建用户"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={(e) => { e.preventDefault(); }}>
            <div className="grid grid-cols-2 gap-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="username">用户名 *</Label>
                <Input
                  id="username"
                  required
                  disabled={isEditMode}
                  value={formData.username}
                  onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                  onBlur={() => !isEditMode && handleBlur("username")}
                  placeholder="请输入用户名"
                />
                {fieldErrors.username && <p className="text-red-500 text-xs mt-1">{fieldErrors.username}</p>}
              </div>

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
                {fieldErrors.name && <p className="text-red-500 text-xs mt-1">{fieldErrors.name}</p>}
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
                    {fieldErrors.password && <p className="text-red-500 text-xs mt-1">{fieldErrors.password}</p>}
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
                    {fieldErrors.confirmPassword && <p className="text-red-500 text-xs mt-1">{fieldErrors.confirmPassword}</p>}
                  </div>
                </>
              )}

              <div className="space-y-2">
                <Label htmlFor="role">角色 *</Label>
                <Select required value={formData.roleId} onValueChange={handleRoleChange}>
                  <SelectTrigger>
                    <SelectValue placeholder="请选择角色" />
                  </SelectTrigger>
                  <SelectContent>
                    {roles.map((role) => (
                      <SelectItem key={role.id} value={role.id.toString()} disabled={role.status === 0}>
                        {role.name}{role.status === 0 && " (已禁用)"}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {fieldErrors.roleId && <p className="text-red-500 text-xs mt-1">{fieldErrors.roleId}</p>}
              </div>

              <div className="space-y-2">
                <Label htmlFor="warehouse">所属仓库</Label>
                <Select value={formData.warehouseId} onValueChange={(value) => setFormData({ ...formData, warehouseId: value })}>
                  <SelectTrigger>
                    <SelectValue placeholder="请选择仓库" />
                  </SelectTrigger>
                  <SelectContent>
                    {warehouses.map((wh) => (
                      <SelectItem key={wh.id} value={wh.id.toString()}>{wh.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="phone">手机号 *</Label>
                <Input
                  id="phone"
                  type="tel"
                  required
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                  onBlur={() => handleBlur("phone")}
                  placeholder="请输入手机号"
                />
                {fieldErrors.phone && <p className="text-red-500 text-xs mt-1">{fieldErrors.phone}</p>}
              </div>

              <div className="space-y-2">
                <Label htmlFor="email">邮箱 *</Label>
                <Input
                  id="email"
                  type="email"
                  required
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  onBlur={() => handleBlur("email")}
                  placeholder="请输入邮箱"
                />
                {fieldErrors.email && <p className="text-red-500 text-xs mt-1">{fieldErrors.email}</p>}
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
                type="button"
                onClick={handleSubmit}
                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
              >
                {isEditMode ? "保存" : "确认创建"}
              </button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
