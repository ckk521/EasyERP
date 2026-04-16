import { useState, useEffect } from "react";
import { Plus, Search, Edit, MapPin } from "lucide-react";
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
  type: string;
  country: string;
  city: string;
  area: string;
  manager: string;
  phone: string;
  status: string;
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

export default function WarehouseList() {
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState("");
  const [typeFilter, setTypeFilter] = useState("全部");
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formData, setFormData] = useState({
    code: "", name: "", type: "", country: "", city: "", address: "", area: "", manager: "", phone: "", email: "",
  });

  useEffect(() => {
    fetchWarehouses();
  }, []);

  async function fetchWarehouses() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: Warehouse[] }>("/api/v1/base/warehouses?page=1&limit=100");
      setWarehouses(data.list || []);
    } catch {
      setWarehouses([]);
    } finally {
      setLoading(false);
    }
  }

  const filteredData = warehouses.filter((w) => {
    const matchSearch = !searchText || w.name.includes(searchText) || w.code.includes(searchText);
    const matchType = typeFilter === "全部" || w.type === typeFilter;
    return matchSearch && matchType;
  });

  const stats = {
    total: warehouses.length,
    selfOperated: warehouses.filter((w) => w.type === '自营仓').length,
    overseas: warehouses.filter((w) => w.type === '海外仓').length,
  };

  const columns = [
    { key: "code", title: "仓库编码", width: "120px" },
    { key: "name", title: "仓库名称", width: "150px" },
    {
      key: "type", title: "类型", width: "100px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '自营仓' ? 'bg-blue-100 text-blue-700' : value === '海外仓' ? 'bg-green-100 text-green-700' : 'bg-purple-100 text-purple-700'
        }`}>{value}</span>
      )
    },
    { key: "country", title: "国家", width: "80px" },
    { key: "city", title: "城市", width: "100px" },
    { key: "area", title: "面积", width: "80px" },
    { key: "manager", title: "负责人", width: "80px" },
    { key: "phone", title: "联系电话", width: "120px" },
    {
      key: "status", title: "状态", width: "80px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${value === '启用' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'}`}>{value}</span>
      )
    },
    {
      key: "action", title: "操作", width: "100px",
      render: () => (
        <div className="flex gap-2">
          <button className="text-blue-600 hover:text-blue-800"><Edit size={14} /></button>
          <button className="text-green-600 hover:text-green-800"><MapPin size={14} /></button>
        </div>
      )
    },
  ];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    toast.info("功能开发中");
    setIsFormOpen(false);
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">仓库管理</h2>
        <button onClick={() => setIsFormOpen(true)} className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm">
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
            {["全部", "自营仓", "海外仓", "第三方仓"].map((type) => (
              <button key={type} onClick={() => setTypeFilter(type)}
                className={`px-3 py-1.5 rounded text-sm ${typeFilter === type ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}`}>
                {type}
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
          <div className="text-xs text-gray-500 mb-1">自营仓</div>
          <div className="text-2xl font-bold text-blue-600">{stats.selfOperated}</div>
        </div>
        <div className="bg-white rounded-lg p-3 border border-gray-200">
          <div className="text-xs text-gray-500 mb-1">海外仓</div>
          <div className="text-2xl font-bold text-green-600">{stats.overseas}</div>
        </div>
        <div className="bg-white rounded-lg p-3 border border-gray-200">
          <div className="text-xs text-gray-500 mb-1">总面积</div>
          <div className="text-2xl font-bold">-</div>
        </div>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        {loading ? (
          <div className="text-center py-12 text-gray-500">加载中...</div>
        ) : filteredData.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">🏭</div>
            <div>暂无仓库数据</div>
            <div className="text-sm text-gray-400 mt-1">API 暂未实现，后端对接后可正常使用</div>
          </div>
        ) : (
          <DataTable columns={columns} data={filteredData} />
        )}
      </div>

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>新建仓库</DialogTitle></DialogHeader>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4 py-4">
              <div className="space-y-2"><Label>仓库编码 *</Label><Input value={formData.code} onChange={(e) => setFormData({ ...formData, code: e.target.value })} placeholder="例：WH-SZ-001" /></div>
              <div className="space-y-2"><Label>仓库名称 *</Label><Input value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} placeholder="例：深圳自营仓" /></div>
              <div className="space-y-2">
                <Label>仓库类型 *</Label>
                <Select value={formData.type} onValueChange={(v) => setFormData({ ...formData, type: v })}>
                  <SelectTrigger><SelectValue placeholder="请选择类型" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="自营仓">自营仓</SelectItem>
                    <SelectItem value="海外仓">海外仓</SelectItem>
                    <SelectItem value="第三方仓">第三方仓</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2"><Label>国家 *</Label><Input value={formData.country} onChange={(e) => setFormData({ ...formData, country: e.target.value })} placeholder="例：中国" /></div>
              <div className="space-y-2"><Label>城市 *</Label><Input value={formData.city} onChange={(e) => setFormData({ ...formData, city: e.target.value })} placeholder="例：深圳" /></div>
              <div className="space-y-2"><Label>面积 (㎡) *</Label><Input type="number" value={formData.area} onChange={(e) => setFormData({ ...formData, area: e.target.value })} placeholder="例：5000" /></div>
              <div className="space-y-2 col-span-2"><Label>详细地址 *</Label><Textarea value={formData.address} onChange={(e) => setFormData({ ...formData, address: e.target.value })} placeholder="请输入仓库详细地址" rows={2} /></div>
              <div className="space-y-2"><Label>负责人 *</Label><Input value={formData.manager} onChange={(e) => setFormData({ ...formData, manager: e.target.value })} placeholder="请输入负责人姓名" /></div>
              <div className="space-y-2"><Label>联系电话 *</Label><Input type="tel" value={formData.phone} onChange={(e) => setFormData({ ...formData, phone: e.target.value })} placeholder="请输入联系电话" /></div>
            </div>
            <DialogFooter>
              <button type="button" onClick={() => setIsFormOpen(false)} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">取消</button>
              <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">确认创建</button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}