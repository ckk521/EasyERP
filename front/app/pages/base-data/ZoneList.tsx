import { useState, useEffect } from "react";
import { Plus, Search, Edit, Power, PowerOff, Trash2, Building2 } from "lucide-react";
import { useSearchParams } from "react-router";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { toast } from "sonner";

interface Zone {
  id: number;
  code: string;
  name: string;
  warehouseId: number;
  warehouseCode: string;
  type: number;
  tempRequire: number;
  storageTypes: string;
  locationCount: number;
  status: number;
}

interface Warehouse {
  id: number;
  code: string;
  name: string;
  storageTypes?: string;
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

const ZONE_TYPES: Record<number, string> = { 1: "收货区", 2: "质检区", 3: "存储区", 4: "拣货区", 5: "打包区", 6: "发货区", 7: "退货区", 8: "残次品区" };
const ZONE_TYPE_ABBR: Record<number, string> = { 1: "RC", 2: "QC", 3: "CC", 4: "PC", 5: "PK", 6: "SC", 7: "RT", 8: "DM" };
const TEMP_REQUIRE: Record<number, string> = { 1: "常温", 2: "冷藏", 3: "冷冻" };
const STORAGE_TYPES: Record<number, string> = { 1: "常温", 2: "冷藏", 3: "冷冻", 4: "恒温" };
const STATUS_TEXT: Record<number, string> = { 0: "停用", 1: "启用" };

function getZoneTypeAbbr(type: number): string {
  return ZONE_TYPE_ABBR[type] || "ZN";
}

export default function ZoneList() {
  const [searchParams] = useSearchParams();
  const [zones, setZones] = useState<Zone[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState("");
  const [warehouseFilter, setWarehouseFilter] = useState<number | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formData, setFormData] = useState({
    code: "", name: "", warehouseId: "", type: 3, tempRequire: 1, storageTypes: "",
  });

  useEffect(() => {
    fetchZones();
    fetchWarehouses();
  }, []);

  // 从 URL 参数获取仓库ID并设置筛选
  useEffect(() => {
    const warehouseIdParam = searchParams.get("warehouseId");
    if (warehouseIdParam) {
      setWarehouseFilter(parseInt(warehouseIdParam));
    }
  }, [searchParams]);

  async function fetchZones() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: Zone[]; total: number }>("/api/v1/base/zones?page=1&limit=100");
      setZones(data.list || []);
    } catch {
      setZones([]);
    } finally {
      setLoading(false);
    }
  }

  async function fetchWarehouses() {
    try {
      const data = await fetchApi<{ list: Warehouse[] }>("/api/v1/base/warehouses?page=1&limit=100");
      setWarehouses(data.list || []);
    } catch {
      setWarehouses([]);
    }
  }

  const filteredData = zones.filter((z) => {
    const matchSearch = !searchText || z.name.includes(searchText) || z.code.includes(searchText);
    const matchWarehouse = warehouseFilter === null || z.warehouseId === warehouseFilter;
    return matchSearch && matchWarehouse;
  });

  const openCreateDialog = () => {
    setEditingId(null);
    setFormData({
      code: "",
      name: "",
      warehouseId: warehouseFilter?.toString() || "",
      type: 3,
      tempRequire: 1,
      storageTypes: "",
    });
    setIsFormOpen(true);
  };

  const openEditDialog = (zone: Zone) => {
    setEditingId(zone.id);
    setFormData({
      code: zone.code,
      name: zone.name,
      warehouseId: zone.warehouseId.toString(),
      type: zone.type,
      tempRequire: zone.tempRequire,
      storageTypes: zone.storageTypes || "",
    });
    setIsFormOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload: Record<string, unknown> = {
        name: formData.name,
        warehouseId: parseInt(formData.warehouseId),
        type: formData.type,
        tempRequire: formData.tempRequire,
        storageTypes: formData.storageTypes,
      };

      if (editingId) {
        payload.code = formData.code;
        await fetchApi(`/api/v1/base/zones/${editingId}`, { method: "PUT", body: JSON.stringify(payload) });
        toast.success("库区信息已更新");
      } else {
        await fetchApi("/api/v1/base/zones", { method: "POST", body: JSON.stringify(payload) });
        toast.success("库区创建成功");
      }
      setIsFormOpen(false);
      fetchZones();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const handleToggleStatus = async (zone: Zone) => {
    try {
      const action = zone.status === 1 ? "disable" : "enable";
      await fetchApi(`/api/v1/base/zones/${zone.id}/${action}`, { method: "PATCH" });
      toast.success(zone.status === 1 ? "库区已停用" : "库区已启用");
      fetchZones();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定要删除这个库区吗？")) return;
    try {
      await fetchApi(`/api/v1/base/zones/${id}`, { method: "DELETE" });
      toast.success("库区已删除");
      fetchZones();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const columns = [
    { key: "code", title: "库区编码", width: "100px" },
    { key: "name", title: "库区名称", width: "120px" },
    { key: "warehouseCode", title: "所属仓库", width: "100px" },
    { key: "type", title: "库区类型", width: "80px", render: (v: number) => ZONE_TYPES[v] || v },
    { key: "tempRequire", title: "温度要求", width: "80px", render: (v: number) => TEMP_REQUIRE[v] || v },
    { key: "storageTypes", title: "存储类型", width: "100px", render: (v: string) => {
      if (!v) return "-";
      return v.split(",").map(t => STORAGE_TYPES[parseInt(t)] || t).join("、");
    }},
    { key: "locationCount", title: "库位数", width: "80px" },
    { key: "status", title: "状态", width: "80px", render: (v: number) => (
      <span className={`px-2 py-0.5 rounded text-xs ${v === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'}`}>{STATUS_TEXT[v]}</span>
    )},
    { key: "action", title: "操作", width: "120px", render: (_: unknown, row: Zone) => (
      <div className="flex gap-2">
        <button title="编辑" onClick={() => openEditDialog(row)} className="text-blue-600 hover:text-blue-800"><Edit size={14} /></button>
        <button title={row.status === 1 ? "停用" : "启用"} onClick={() => handleToggleStatus(row)} className={row.status === 1 ? "text-orange-600 hover:text-orange-800" : "text-green-600 hover:text-green-800"}>
          {row.status === 1 ? <PowerOff size={14} /> : <Power size={14} />}
        </button>
        <button title="删除" onClick={() => handleDelete(row.id)} className="text-red-600 hover:text-red-800"><Trash2 size={14} /></button>
      </div>
    )},
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">库区管理</h2>
        <button onClick={openCreateDialog} className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm">
          <Plus size={16} />新建库区
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-4 items-center">
          <div className="flex-1 relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input type="text" placeholder="搜索库区名称或编码" value={searchText} onChange={(e) => setSearchText(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <Select value={warehouseFilter?.toString() || "all"} onValueChange={(v) => setWarehouseFilter(v === "all" ? null : parseInt(v))}>
            <SelectTrigger className="w-48">
              <Building2 size={16} className="mr-1 text-gray-400" />
              <SelectValue placeholder="全部仓库" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部仓库</SelectItem>
              {warehouses.map((wh) => (
                <SelectItem key={wh.id} value={wh.id.toString()}>{wh.code} - {wh.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        {loading ? (
          <div className="text-center py-12 text-gray-500">加载中...</div>
        ) : filteredData.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">📦</div>
            <div>暂无库区数据</div>
          </div>
        ) : (
          <DataTable columns={columns} data={filteredData} />
        )}
      </div>

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>{editingId ? "编辑库区" : "新建库区"}</DialogTitle></DialogHeader>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4 py-4">
              {!editingId && (
                <>
                  <div className="space-y-2">
                    <Label htmlFor="warehouseId">所属仓库 *</Label>
                    <Select value={formData.warehouseId} onValueChange={(v) => setFormData({ ...formData, warehouseId: v })} required>
                      <SelectTrigger><SelectValue placeholder="请选择仓库" /></SelectTrigger>
                      <SelectContent>
                        {warehouses.map((wh) => (
                          <SelectItem key={wh.id} value={wh.id.toString()}>{wh.code} - {wh.name}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="type">库区类型 *</Label>
                    <Select value={formData.type.toString()} onValueChange={(v) => setFormData({ ...formData, type: parseInt(v) })}>
                      <SelectTrigger><SelectValue placeholder="请选择类型" /></SelectTrigger>
                      <SelectContent>
                        {Object.entries(ZONE_TYPES).map(([k, v]) => (
                          <SelectItem key={k} value={k}>{v}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  {formData.warehouseId && formData.type && (
                    <div className="col-span-2 p-2 bg-gray-50 rounded text-sm text-gray-600">
                      库区编码将自动生成：<strong>{warehouses.find(w => w.id.toString() === formData.warehouseId)?.code}-{getZoneTypeAbbr(formData.type)}-XX</strong>
                    </div>
                  )}
                </>
              )}
              {editingId && (
                <div className="space-y-2">
                  <Label>库区编码</Label>
                  <div className="px-3 py-2 bg-gray-100 rounded text-sm font-mono">{formData.code}</div>
                </div>
              )}
              <div className="space-y-2">
                <Label htmlFor="name">库区名称 *</Label>
                <Input id="name" value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} placeholder="例：存储区A" required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="tempRequire">温度要求 *</Label>
                <Select value={formData.tempRequire.toString()} onValueChange={(v) => setFormData({ ...formData, tempRequire: parseInt(v) })}>
                  <SelectTrigger><SelectValue placeholder="请选择温度要求" /></SelectTrigger>
                  <SelectContent>
                    {Object.entries(TEMP_REQUIRE).map(([k, v]) => (
                      <SelectItem key={k} value={k}>{v}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2 col-span-2">
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
