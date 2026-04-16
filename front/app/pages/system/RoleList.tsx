import { useState, useEffect } from "react";
import { Plus, Pencil, Trash2, UserX, UserCheck } from "lucide-react";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { toast } from "sonner";

interface Role {
  id: number;
  code: string;
  name: string;
  type: number;
  typeText: string;
  description: string;
  userCount: number;
  permissionCount: number;
  status: number;
  statusText: string;
  isSystem: number;
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

  const data: ApiResponse<T> = await response.json();

  if (!data.success) {
    throw new Error(data.message || "API Error");
  }

  return data.data as T;
}

export default function RoleList() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(true);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [formData, setFormData] = useState({
    code: "",
    name: "",
    description: "",
  });
  const [searchKeyword, setSearchKeyword] = useState("");

  useEffect(() => {
    fetchRoles();
  }, []);

  async function fetchRoles(keyword?: string) {
    try {
      setLoading(true);
      const params = new URLSearchParams({ page: "1", limit: "100" });
      if (keyword) params.append("keyword", keyword);

      const data = await fetchApi<{ list: Role[] }>(`${API_BASE}/system/roles?${params}`);
      setRoles(data.list || []);
    } catch (error) {
      toast.error("获取角色列表失败");
      console.error(error);
    } finally {
      setLoading(false);
    }
  }

  const handleSearch = () => {
    fetchRoles(searchKeyword);
  };

  const handleOpenCreate = () => {
    setIsEditMode(false);
    setEditingRole(null);
    setFormData({
      code: "",
      name: "",
      description: "",
    });
    setIsFormOpen(true);
  };

  const handleOpenEdit = (role: Role) => {
    if (role.isSystem === 1) {
      toast.error("预置角色不可编辑");
      return;
    }
    setIsEditMode(true);
    setEditingRole(role);
    setFormData({
      code: role.code,
      name: role.name,
      description: role.description || "",
    });
    setIsFormOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (isEditMode && editingRole) {
        await fetchApi(`${API_BASE}/system/roles/${editingRole.id}`, {
          method: "PUT",
          body: JSON.stringify({
            name: formData.name,
            description: formData.description,
          }),
        });
        toast.success("角色信息已更新");
      } else {
        await fetchApi(`${API_BASE}/system/roles`, {
          method: "POST",
          body: JSON.stringify({
            code: formData.code,
            name: formData.name,
            description: formData.description,
          }),
        });
        toast.success(`角色 ${formData.name} 创建成功！`);
      }
      setIsFormOpen(false);
      fetchRoles();
    } catch (error: any) {
      toast.error(error.message || (isEditMode ? "更新角色失败" : "创建角色失败"));
    }
  };

  const handleEnable = async (role: Role) => {
    try {
      await fetchApi(`${API_BASE}/system/roles/${role.id}/enable`, { method: "PATCH" });
      toast.success("角色已启用");
      fetchRoles();
    } catch (error: any) {
      toast.error(error.message || "启用角色失败");
    }
  };

  const handleDisable = async (role: Role) => {
    try {
      await fetchApi(`${API_BASE}/system/roles/${role.id}/disable`, { method: "PATCH" });
      toast.success("角色已停用");
      fetchRoles();
    } catch (error: any) {
      toast.error(error.message || "停用角色失败");
    }
  };

  const handleDelete = async (role: Role) => {
    if (role.isSystem === 1) {
      toast.error("预置角色不可删除");
      return;
    }
    if (!confirm(`确定要删除角色 ${role.name} 吗？`)) return;
    try {
      await fetchApi(`${API_BASE}/system/roles/${role.id}`, { method: "DELETE" });
      toast.success("角色已删除");
      fetchRoles();
    } catch (error: any) {
      toast.error(error.message || "删除角色失败");
    }
  };

  const columns = [
    { key: "code", title: "角色编码", width: "120px" },
    { key: "name", title: "角色名称", width: "120px" },
    { key: "typeText", title: "类型", width: "80px" },
    { key: "description", title: "描述", width: "200px" },
    { key: "userCount", title: "用户数", width: "80px" },
    { key: "permissionCount", title: "权限数", width: "80px" },
    {
      key: "statusText",
      title: "状态",
      width: "80px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '启用' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'
        }`}>
          {value}
        </span>
      )
    },
    {
      key: "actions",
      title: "操作",
      width: "180px",
      render: (_: any, role: Role) => (
        <div className="flex items-center gap-1">
          {role.isSystem !== 1 && (
            <button
              onClick={() => handleOpenEdit(role)}
              className="p-1 hover:bg-gray-100 rounded text-blue-600"
              title="编辑"
            >
              <Pencil size={14} />
            </button>
          )}
          {role.status === 1 ? (
            <button
              onClick={() => handleDisable(role)}
              className="p-1 hover:bg-gray-100 rounded text-orange-600"
              title="停用"
            >
              <UserX size={14} />
            </button>
          ) : (
            <button
              onClick={() => handleEnable(role)}
              className="p-1 hover:bg-gray-100 rounded text-green-600"
              title="启用"
            >
              <UserCheck size={14} />
            </button>
          )}
          {role.isSystem !== 1 && (
            <button
              onClick={() => handleDelete(role)}
              className="p-1 hover:bg-gray-100 rounded text-red-600"
              title="删除"
            >
              <Trash2 size={14} />
            </button>
          )}
        </div>
      )
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">角色管理</h2>
        <button
          onClick={handleOpenCreate}
          className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm"
        >
          <Plus size={16} />
          新建角色
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-2 mb-4">
          <div className="relative flex-1">
            <input
              type="text"
              placeholder="搜索角色编码或名称"
              className="w-full pl-3 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
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
          <DataTable columns={columns} data={roles} />
        )}
      </div>

      {/* Form Dialog */}
      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>{isEditMode ? "编辑角色" : "新建角色"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit}>
            <div className="grid gap-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="code">角色编码 *</Label>
                <Input
                  id="code"
                  required
                  disabled={isEditMode}
                  value={formData.code}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                  placeholder="请输入角色编码"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="name">角色名称 *</Label>
                <Input
                  id="name"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="请输入角色名称"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="description">描述</Label>
                <Input
                  id="description"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="请输入描述"
                />
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
    </div>
  );
}