import { useState, useEffect } from "react";
import { Plus, Search, Edit, Power, PowerOff, Trash2, Layers } from "lucide-react";
import { useNavigate } from "react-router";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Textarea } from "../../components/ui/textarea";
import { toast } from "sonner";

interface Warehouse {
  id: number;
  code: string;
  name: string;
  type: number;
  storageTypes: string;
  country: string;
  province: string;
  address: string;
  area: number;
  manager: string;
  phone: string;
  status: number;
}

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
}

async function fetchApi<T>(url: string, options?: RequestInit): Promise<T> {
  const token = localStorage.getItem("token");
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options?.headers as Record<string, string> || {}),
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  const response = await fetch(url, { ...options, headers });
  const data: ApiResponse<T> = await response.json();
  if (!data.success) throw new Error(data.message || "API Error");
  return data.data as T;
}

const WAREHOUSE_TYPES: Record<number, string> = { 1: "自营仓", 2: "第三方仓", 3: "海外仓" };
const STORAGE_TYPES: Record<number, string> = { 1: "常温", 2: "冷藏", 3: "冷冻", 4: "恒温", 5: "混合" };
const STATUS_TEXT: Record<number, string> = { 0: "停用", 1: "启用" };

export default function WarehouseList() {
  const navigate = useNavigate();
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState("");
  const [typeFilter, setTypeFilter] = useState<number | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formData, setFormData] = useState({
    code: "", name: "", type: 1, storageTypes: "", country: "", province: "", address: "", area: "", manager: "", phone: "",
  });

  useEffect(() => {
    fetchWarehouses();
  }, []);

  async function fetchWarehouses() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: Warehouse[]; total: number }>("/api/v1/base/warehouses?page=1&limit=100");
      setWarehouses(data.list || []);
    } catch {
      setWarehouses([]);
    } finally {
      setLoading(false);
    }
  }

  const filteredData = warehouses.filter((w) => {
    const matchSearch = !searchText || w.name.includes(searchText) || w.code.includes(searchText);
    const matchType = typeFilter === null || w.type === typeFilter;
    return matchSearch && matchType;
  });

  const stats = {
    total: warehouses.length,
    enabled: warehouses.filter((w) => w.status === 1).length,
    disabled: warehouses.filter((w) => w.status === 0).length,
  };

  const openCreateDialog = () => {
    setEditingId(null);
    setFormData({ code: "", name: "", type: 1, storageTypes: "", country: "", province: "", address: "", area: "", manager: "", phone: "" });
    setIsFormOpen(true);
  };

  const openEditDialog = (warehouse: Warehouse) => {
    setEditingId(warehouse.id);
    setFormData({
      code: warehouse.code,
      name: warehouse.name,
      type: warehouse.type,
      storageTypes: warehouse.storageTypes || "",
      country: warehouse.country || "",
      province: warehouse.province || "",
      address: warehouse.address || "",
      area: warehouse.area?.toString() || "",
      manager: warehouse.manager || "",
      phone: warehouse.phone || "",
    });
    setIsFormOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload = {
        code: formData.code,
        name: formData.name,
        type: formData.type,
        storageTypes: formData.storageTypes,
        country: formData.country,
        province: formData.province,
        address: formData.address,
        area: formData.area ? parseFloat(formData.area) : null,
        manager: formData.manager,
        phone: formData.phone,
      };

      if (editingId) {
        await fetchApi(`/api/v1/base/warehouses/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
        toast.success("仓库信息已更新");
      } else {
        await fetchApi("/api/v1/base/warehouses", {
          method: "POST",
          body: JSON.stringify(payload),
        });
        toast.success("仓库创建成功");
      }
      setIsFormOpen(false);
      fetchWarehouses();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message || "操作失败");
    }
  };

  const handleToggleStatus = async (warehouse: Warehouse) => {
    try {
      const action = warehouse.status === 1 ? "disable" : "enable";
      await fetchApi(`/api/v1/base/warehouses/${warehouse.id}/${action}`, { method: "PATCH" });
      toast.success(warehouse.status === 1 ? "仓库已停用" : "仓库已启用");
      fetchWarehouses();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定要删除这个仓库吗？")) return;
    try {
      await fetchApi(`/api/v1/base/warehouses/${id}`, { method: "DELETE" });
      toast.success("仓库已删除");
      fetchWarehouses();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const columns = [
    { key: "code", title: "仓库编码", width: "120px" },
    { key: "name", title: "仓库名称", width: "150px" },
    {
      key: "type", title: "类型", width: "100px",
      render: (value: number) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === 1 ? 'bg-blue-100 text-blue-700' : value === 3 ? 'bg-green-100 text-green-700' : 'bg-purple-100 text-purple-700'
        }`}>{WAREHOUSE_TYPES[value] || value}</span>
      )
    },
    {
      key: "storageTypes", title: "存储类型", width: "120px",
      render: (value: string) => {
        if (!value) return "-";
        const types = value.split(",").map(t => STORAGE_TYPES[parseInt(t)] || t).join("、");
        return <span className="px-2 py-0.5 rounded text-xs bg-cyan-100 text-cyan-700">{types}</span>;
      }
    },
    { key: "country", title: "国家", width: "80px" },
    { key: "province", title: "省份/城市", width: "100px" },
    { key: "area", title: "面积(㎡)", width: "80px", render: (value: number) => value || "-" },
    { key: "manager", title: "负责人", width: "80px" },
    { key: "phone", title: "联系电话", width: "120px" },
    {
      key: "status", title: "状态", width: "80px",
      render: (value: number) => (
        <span className={`px-2 py-0.5 rounded text-xs ${value === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'}`}>
          {STATUS_TEXT[value] || value}
        </span>
      )
    },
    {
      key: "action", title: "操作", width: "160px",
      render: (_: unknown, row: Warehouse) => (
        <div className="flex gap-2">
          <button title="管理库区" onClick={() => navigate(`/base/zone?warehouseId=${row.id}`)} className="text-cyan-600 hover:text-cyan-800"><Layers size={14} /></button>
          <button title="编辑" onClick={() => openEditDialog(row)} className="text-blue-600 hover:text-blue-800"><Edit size={14} /></button>
          <button title={row.status === 1 ? "停用" : "启用"} onClick={() => handleToggleStatus(row)} className={row.status === 1 ? "text-orange-600 hover:text-orange-800" : "text-green-600 hover:text-green-800"}>
            {row.status === 1 ? <PowerOff size={14} /> : <Power size={14} />}
          </button>
          <button title="删除" onClick={() => handleDelete(row.id)} className="text-red-600 hover:text-red-800"><Trash2 size={14} /></button>
        </div>
      )
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">仓库管理</h2>
        <button onClick={openCreateDialog} className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm">
          <Plus size={16} />新建仓库
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-4 items-center">
          <div className="flex-1 relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input type="text" placeholder="搜索仓库名称或编码" value={searchText} onChange={(e) => setSearchText(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div className="flex gap-2">
            <button onClick={() => setTypeFilter(null)}
              className={`px-3 py-1.5 rounded text-sm ${typeFilter === null ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}`}>全部</button>
            {[1, 2, 3].map((type) => (
              <button key={type} onClick={() => setTypeFilter(type)}
                className={`px-3 py-1.5 rounded text-sm ${typeFilter === type ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}`}>
                {WAREHOUSE_TYPES[type]}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-4 gap-4">
        <div className="bg-white rounded-lg p-3 border border-gray-200">
          <div className="text-xs text-gray-500 mb-1">总仓库数</div>
          <div className="text-2xl font-bold">{stats.total}</div>
        </div>
        <div className="bg-white rounded-lg p-3 border border-gray-200">
          <div className="text-xs text-gray-500 mb-1">启用中</div>
          <div className="text-2xl font-bold text-green-600">{stats.enabled}</div>
        </div>
        <div className="bg-white rounded-lg p-3 border border-gray-200">
          <div className="text-xs text-gray-500 mb-1">已停用</div>
          <div className="text-2xl font-bold text-gray-600">{stats.disabled}</div>
        </div>
        <div className="bg-white rounded-lg p-3 border border-gray-200">
          <div className="text-xs text-gray-500 mb-1">总面积</div>
          <div className="text-2xl font-bold">{warehouses.reduce((sum, w) => sum + (w.area || 0), 0).toLocaleString()} ㎡</div>
        </div>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        {loading ? (
          <div className="text-center py-12 text-gray-500">加载中...</div>
        ) : filteredData.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">🏭</div>
            <div>暂无仓库数据</div>
          </div>
        ) : (
          <DataTable columns={columns} data={filteredData} />
        )}
      </div>

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>{editingId ? "编辑仓库" : "新建仓库"}</DialogTitle></DialogHeader>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="code">仓库编码 *</Label>
                <Input id="code" value={formData.code} onChange={(e) => setFormData({ ...formData, code: e.target.value })} placeholder="例：WH-SZ-001" required disabled={!!editingId} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="name">仓库名称 *</Label>
                <Input id="name" value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} placeholder="例：深圳自营仓" required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="type">仓库类型 *</Label>
                <Select value={formData.type.toString()} onValueChange={(v) => setFormData({ ...formData, type: parseInt(v) })}>
                  <SelectTrigger><SelectValue placeholder="请选择类型" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="1">自营仓</SelectItem>
                    <SelectItem value="2">第三方仓</SelectItem>
                    <SelectItem value="3">海外仓</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>存储类型（可多选）</Label>
                <div className="flex flex-wrap gap-2">
                  {[1, 2, 3, 4].map((t) => {
                    const selected = formData.storageTypes.split(",").filter(Boolean).map(Number).includes(t);
                    return (
                      <button key={t} type="button" onClick={() => {
                        const current = formData.storageTypes.split(",").filter(Boolean).map(Number);
                        const updated = selected ? current.filter(x => x !== t) : [...current, t];
                        setFormData({ ...formData, storageTypes: updated.join(",") });
                      }} className={`px-3 py-1 rounded text-sm border ${selected ? 'bg-blue-600 text-white border-blue-600' : 'bg-white text-gray-700 border-gray-300 hover:border-blue-400'}`}>
                        {STORAGE_TYPES[t]}
                      </button>
                    );
                  })}
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="country">国家 *</Label>
                <Input id="country" value={formData.country} onChange={(e) => setFormData({ ...formData, country: e.target.value })} placeholder="例：中国" required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="province">省份/城市 *</Label>
                <Input id="province" value={formData.province} onChange={(e) => setFormData({ ...formData, province: e.target.value })} placeholder="例：深圳" required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="area">面积 (㎡)</Label>
                <Input id="area" type="number" value={formData.area} onChange={(e) => setFormData({ ...formData, area: e.target.value })} placeholder="例：5000" />
              </div>
              <div className="space-y-2 col-span-2">
                <Label htmlFor="address">详细地址</Label>
                <Textarea id="address" value={formData.address} onChange={(e) => setFormData({ ...formData, address: e.target.value })} placeholder="请输入仓库详细地址" rows={2} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="manager">负责人 *</Label>
                <Input id="manager" value={formData.manager} onChange={(e) => setFormData({ ...formData, manager: e.target.value })} placeholder="请输入负责人姓名" required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="phone">联系电话 *</Label>
                <Input id="phone" type="tel" value={formData.phone} onChange={(e) => setFormData({ ...formData, phone: e.target.value })} placeholder="请输入联系电话" required />
              </div>
            </div>
            <DialogFooter>
              <button type="button" onClick={() => setIsFormOpen(false)} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">取消</button>
              <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">{editingId ? "保存" : "确认创建"}</button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
