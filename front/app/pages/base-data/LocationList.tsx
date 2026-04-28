import { useState, useEffect } from "react";
import { Plus, Search, Edit, Power, PowerOff, Trash2, Layers, Grid3x3 } from "lucide-react";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { toast } from "sonner";

interface Location {
  id: number;
  code: string;
  zoneId: number;
  zoneCode: string;
  warehouseId: number;
  warehouseCode: string;
  rowNum: number;
  colNum: number;
  layerNum: number;
  type: number;
  storageType: number;
  maxLength: number;
  maxWidth: number;
  maxHeight: number;
  maxWeight: number;
  status: number;
  currentQty: number;
}

interface Zone {
  id: number;
  code: string;
  name: string;
  warehouseId: number;
  warehouseCode: string;
  storageTypes?: string;
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

const LOCATION_TYPES: Record<number, string> = { 1: "标准位", 2: "大件位", 3: "冷藏位", 4: "高值位", 5: "危险品位" };
const STORAGE_TYPES: Record<number, string> = { 1: "常温", 2: "冷藏", 3: "冷冻", 4: "恒温" };
const STATUS_TEXT: Record<number, string> = { 1: "空闲", 2: "占用", 3: "锁定", 4: "禁用" };

export default function LocationList() {
  const [locations, setLocations] = useState<Location[]>([]);
  const [zones, setZones] = useState<Zone[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState("");
  const [statusFilter, setStatusFilter] = useState<number | null>(null);
  const [warehouseFilter, setWarehouseFilter] = useState<number | null>(null);
  const [zoneFilter, setZoneFilter] = useState<number | null>(null);
  const [viewMode, setViewMode] = useState<"table" | "grid">("table");
  const [isBatchOpen, setIsBatchOpen] = useState(false);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [batchData, setBatchData] = useState({
    warehouseId: "", zoneId: "", startRow: 1, endRow: 1, startCol: 1, endCol: 1, startLayer: 1, endLayer: 1, type: 1, storageType: 1,
  });
  const [editData, setEditData] = useState({ maxLength: "", maxWidth: "", maxHeight: "", maxWeight: "" });

  useEffect(() => {
    fetchLocations();
    fetchZones();
    fetchWarehouses();
  }, []);

  async function fetchLocations() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: Location[]; total: number }>("/api/v1/base/locations?page=1&limit=100");
      setLocations(data.list || []);
    } catch {
      setLocations([]);
    } finally {
      setLoading(false);
    }
  }

  async function fetchZones() {
    try {
      const data = await fetchApi<{ list: Zone[] }>("/api/v1/base/zones?page=1&limit=100");
      setZones(data.list || []);
    } catch {
      setZones([]);
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

  // 批量生成时，按选择的仓库筛选库区
  const batchFilteredZones = batchData.warehouseId
    ? zones.filter(z => z.warehouseId.toString() === batchData.warehouseId)
    : zones;

  const selectedZone = batchFilteredZones.find(z => z.id.toString() === batchData.zoneId);
  const selectedWarehouse = warehouses.find(w => w.id === selectedZone?.warehouseId);
  const allowedStorageTypes = selectedWarehouse?.storageTypes?.split(",").map(Number) || [];

  const filteredZones = zoneFilter === null
    ? zones
    : zones.filter(z => z.id === zoneFilter);

  const filteredData = locations.filter((l) => {
    const matchSearch = !searchText || l.code.includes(searchText);
    const matchStatus = statusFilter === null || l.status === statusFilter;
    const matchWarehouse = warehouseFilter === null || l.warehouseId === warehouseFilter;
    const matchZone = zoneFilter === null || l.zoneId === zoneFilter;
    return matchSearch && matchStatus && matchWarehouse && matchZone;
  });

  const stats = {
    total: locations.length,
    available: locations.filter(l => l.status === 1).length,
    occupied: locations.filter(l => l.status === 2).length,
    locked: locations.filter(l => l.status === 3).length,
  };

  const openEditDialog = (location: Location) => {
    setEditingId(location.id);
    setEditData({
      maxLength: location.maxLength?.toString() || "",
      maxWidth: location.maxWidth?.toString() || "",
      maxHeight: location.maxHeight?.toString() || "",
      maxWeight: location.maxWeight?.toString() || "",
    });
    setIsEditOpen(true);
  };

  const handleBatchGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload = {
        zoneId: parseInt(batchData.zoneId),
        startRow: batchData.startRow,
        endRow: batchData.endRow,
        startCol: batchData.startCol,
        endCol: batchData.endCol,
        startLayer: batchData.startLayer,
        endLayer: batchData.endLayer,
        type: batchData.type,
        storageType: batchData.storageType,
      };
      const data = await fetchApi<{ count: number }>("/api/v1/base/locations/batch", { method: "POST", body: JSON.stringify(payload) });
      toast.success(`已生成${data.count}个库位`);
      setIsBatchOpen(false);
      setBatchData({ warehouseId: "", zoneId: "", startRow: 1, endRow: 1, startCol: 1, endCol: 1, startLayer: 1, endLayer: 1, type: 1, storageType: 1 });
      fetchLocations();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingId) return;
    try {
      const payload: Record<string, number> = {};
      if (editData.maxLength) payload.maxLength = parseFloat(editData.maxLength);
      if (editData.maxWidth) payload.maxWidth = parseFloat(editData.maxWidth);
      if (editData.maxHeight) payload.maxHeight = parseFloat(editData.maxHeight);
      if (editData.maxWeight) payload.maxWeight = parseFloat(editData.maxWeight);
      await fetchApi(`/api/v1/base/locations/${editingId}`, { method: "PUT", body: JSON.stringify(payload) });
      toast.success("库位信息已更新");
      setIsEditOpen(false);
      fetchLocations();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const handleToggleStatus = async (location: Location) => {
    try {
      const action = location.status === 4 ? "enable" : "disable";
      await fetchApi(`/api/v1/base/locations/${location.id}/${action}`, { method: "PATCH" });
      toast.success(location.status === 4 ? "库位已启用" : "库位已禁用");
      fetchLocations();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定要删除这个库位吗？")) return;
    try {
      await fetchApi(`/api/v1/base/locations/${id}`, { method: "DELETE" });
      toast.success("库位已删除");
      fetchLocations();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const getWarehouseName = (warehouseId: number) => {
    const wh = warehouses.find(w => w.id === warehouseId);
    return wh ? `${wh.code} - ${wh.name}` : "-";
  };

  const columns = [
    { key: "code", title: "库位编码", width: "140px" },
    { key: "warehouseId", title: "仓库", width: "120px", render: (v: number) => getWarehouseName(v) },
    { key: "zoneCode", title: "库区", width: "80px" },
    { key: "rowNum", title: "排", width: "60px" },
    { key: "colNum", title: "列", width: "60px" },
    { key: "layerNum", title: "层", width: "60px" },
    { key: "type", title: "库位类型", width: "80px", render: (v: number) => LOCATION_TYPES[v] || v },
    { key: "storageType", title: "存储类型", width: "80px", render: (v: number) => (
      <span className={`px-2 py-0.5 rounded text-xs ${
        v === 2 ? 'bg-blue-100 text-blue-700' : v === 3 ? 'bg-cyan-100 text-cyan-700' : v === 4 ? 'bg-amber-100 text-amber-700' : 'bg-green-100 text-green-700'
      }`}>{STORAGE_TYPES[v] || v}</span>
    )},
    { key: "currentQty", title: "数量", width: "60px" },
    { key: "status", title: "状态", width: "80px", render: (v: number) => (
      <span className={`px-2 py-0.5 rounded text-xs ${
        v === 1 ? 'bg-green-100 text-green-700' : v === 2 ? 'bg-blue-100 text-blue-700' : v === 3 ? 'bg-yellow-100 text-yellow-700' : 'bg-gray-100 text-gray-700'
      }`}>{STATUS_TEXT[v]}</span>
    )},
    { key: "action", title: "操作", width: "120px", render: (_: unknown, row: Location) => (
      <div className="flex gap-2">
        <button title="编辑" onClick={() => openEditDialog(row)} className="text-blue-600 hover:text-blue-800"><Edit size={14} /></button>
        <button title={row.status === 4 ? "启用" : "禁用"} onClick={() => handleToggleStatus(row)} className={row.status === 4 ? "text-green-600 hover:text-green-800" : "text-orange-600 hover:text-orange-800"}>
          {row.status === 4 ? <Power size={14} /> : <PowerOff size={14} />}
        </button>
        <button title="删除" onClick={() => handleDelete(row.id)} className="text-red-600 hover:text-red-800"><Trash2 size={14} /></button>
      </div>
    )},
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">库位管理</h2>
        <div className="flex gap-2">
          <button onClick={() => setViewMode(viewMode === "table" ? "grid" : "table")}
            className="px-3 py-1.5 bg-gray-100 text-gray-700 rounded hover:bg-gray-200 flex items-center gap-1 text-sm">
            <Grid3x3 size={16} />{viewMode === "table" ? "网格视图" : "表格视图"}
          </button>
          <button onClick={() => setIsBatchOpen(true)} className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm">
            <Layers size={16} />批量生成
          </button>
        </div>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-4 items-center flex-wrap">
          <div className="flex-1 min-w-[200px] relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input type="text" placeholder="搜索库位编码" value={searchText} onChange={(e) => setSearchText(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <Select value={warehouseFilter?.toString() || "all"} onValueChange={(v) => { setWarehouseFilter(v === "all" ? null : parseInt(v)); setZoneFilter(null); }}>
            <SelectTrigger className="w-40"><SelectValue placeholder="全部仓库" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部仓库</SelectItem>
              {warehouses.map((w) => (
                <SelectItem key={w.id} value={w.id.toString()}>{w.code} - {w.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select value={zoneFilter?.toString() || "all"} onValueChange={(v) => setZoneFilter(v === "all" ? null : parseInt(v))}>
            <SelectTrigger className="w-40"><SelectValue placeholder="全部库区" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部库区</SelectItem>
              {zones.filter(z => warehouseFilter === null || z.warehouseId === warehouseFilter).map((z) => (
                <SelectItem key={z.id} value={z.id.toString()}>{z.code} - {z.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <div className="flex gap-2">
            <button onClick={() => setStatusFilter(null)}
              className={`px-3 py-1.5 rounded text-sm ${statusFilter === null ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}`}>全部</button>
            {[1, 2, 3, 4].map((s) => (
              <button key={s} onClick={() => setStatusFilter(s)}
                className={`px-3 py-1.5 rounded text-sm ${statusFilter === s ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}`}>
                {STATUS_TEXT[s]}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-4 gap-4">
        <div className="bg-white rounded-lg p-3 border border-gray-200">
          <div className="text-xs text-gray-500 mb-1">总库位数</div>
          <div className="text-2xl font-bold">{stats.total}</div>
        </div>
        <div className="bg-white rounded-lg p-3 border border-gray-200">
          <div className="text-xs text-gray-500 mb-1">空闲</div>
          <div className="text-2xl font-bold text-green-600">{stats.available}</div>
        </div>
        <div className="bg-white rounded-lg p-3 border border-gray-200">
          <div className="text-xs text-gray-500 mb-1">占用</div>
          <div className="text-2xl font-bold text-blue-600">{stats.occupied}</div>
        </div>
        <div className="bg-white rounded-lg p-3 border border-gray-200">
          <div className="text-xs text-gray-500 mb-1">锁定</div>
          <div className="text-2xl font-bold text-orange-600">{stats.locked}</div>
        </div>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        {loading ? (
          <div className="text-center py-12 text-gray-500">加载中...</div>
        ) : filteredData.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">📍</div>
            <div>暂无库位数据</div>
          </div>
        ) : viewMode === "table" ? (
          <DataTable columns={columns} data={filteredData} />
        ) : (
          <div className="grid grid-cols-8 gap-2">
            {filteredData.map((loc) => (
              <div key={loc.id}
                className={`p-2 rounded border-2 text-center cursor-pointer hover:shadow-md transition-shadow ${
                  loc.status === 1 ? 'border-green-500 bg-green-50' : loc.status === 2 ? 'border-blue-500 bg-blue-50' : loc.status === 3 ? 'border-orange-500 bg-orange-50' : 'border-gray-300 bg-gray-50'
                }`}>
                <div className="text-xs font-semibold mb-1">{loc.code}</div>
                <div className="text-xs text-gray-600">{STATUS_TEXT[loc.status]}</div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 批量生成弹窗 */}
      <Dialog open={isBatchOpen} onOpenChange={setIsBatchOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>批量生成库位</DialogTitle></DialogHeader>
          <form onSubmit={handleBatchGenerate}>
            <div className="grid grid-cols-2 gap-4 py-4">
              <div className="space-y-2 col-span-2">
                <Label>所属仓库 *</Label>
                <Select
                  value={batchData.warehouseId}
                  onValueChange={(v) => setBatchData({ ...batchData, warehouseId: v, zoneId: "" })}
                  required
                >
                  <SelectTrigger><SelectValue placeholder="请先选择仓库" /></SelectTrigger>
                  <SelectContent>
                    {warehouses.map((w) => (
                      <SelectItem key={w.id} value={w.id.toString()}>{w.code} - {w.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2 col-span-2">
                <Label>所属库区 *</Label>
                <Select
                  value={batchData.zoneId}
                  onValueChange={(v) => setBatchData({ ...batchData, zoneId: v })}
                  required
                  disabled={!batchData.warehouseId}
                >
                  <SelectTrigger><SelectValue placeholder={batchData.warehouseId ? "请选择库区" : "请先选择仓库"} /></SelectTrigger>
                  <SelectContent>
                    {batchFilteredZones.map((z) => (
                      <SelectItem key={z.id} value={z.id.toString()}>{z.code} - {z.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              {selectedZone && (
                <div className="col-span-2 p-2 bg-gray-50 rounded text-sm">
                  <div className="flex gap-4">
                    <span>仓库: <strong>{selectedZone.warehouseCode}</strong></span>
                    {selectedWarehouse?.storageTypes && (
                      <span>支持存储类型: <strong>{selectedWarehouse.storageTypes.split(",").map(t => STORAGE_TYPES[parseInt(t)]).join("、")}</strong></span>
                    )}
                  </div>
                </div>
              )}
              <div className="space-y-2"><Label>起始排</Label><Input type="number" value={batchData.startRow} onChange={(e) => setBatchData({ ...batchData, startRow: parseInt(e.target.value) || 1 })} /></div>
              <div className="space-y-2"><Label>结束排</Label><Input type="number" value={batchData.endRow} onChange={(e) => setBatchData({ ...batchData, endRow: parseInt(e.target.value) || 1 })} /></div>
              <div className="space-y-2"><Label>起始列</Label><Input type="number" value={batchData.startCol} onChange={(e) => setBatchData({ ...batchData, startCol: parseInt(e.target.value) || 1 })} /></div>
              <div className="space-y-2"><Label>结束列</Label><Input type="number" value={batchData.endCol} onChange={(e) => setBatchData({ ...batchData, endCol: parseInt(e.target.value) || 1 })} /></div>
              <div className="space-y-2"><Label>起始层</Label><Input type="number" value={batchData.startLayer} onChange={(e) => setBatchData({ ...batchData, startLayer: parseInt(e.target.value) || 1 })} /></div>
              <div className="space-y-2"><Label>结束层</Label><Input type="number" value={batchData.endLayer} onChange={(e) => setBatchData({ ...batchData, endLayer: parseInt(e.target.value) || 1 })} /></div>
              <div className="space-y-2 col-span-2">
                <Label>库位类型</Label>
                <Select value={batchData.type.toString()} onValueChange={(v) => setBatchData({ ...batchData, type: parseInt(v) })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {Object.entries(LOCATION_TYPES).map(([k, v]) => (
                      <SelectItem key={k} value={k}>{v}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2 col-span-2">
                <Label>存储类型 * {allowedStorageTypes.length > 0 && <span className="text-gray-500 font-normal">(仅限仓库支持的类型)</span>}</Label>
                <Select value={batchData.storageType.toString()} onValueChange={(v) => setBatchData({ ...batchData, storageType: parseInt(v) })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {Object.entries(STORAGE_TYPES)
                      .filter(([k]) => allowedStorageTypes.length === 0 || allowedStorageTypes.includes(parseInt(k)))
                      .map(([k, v]) => (
                        <SelectItem key={k} value={k}>{v}</SelectItem>
                      ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <DialogFooter>
              <button type="button" onClick={() => setIsBatchOpen(false)} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">取消</button>
              <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">生成</button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* 编辑弹窗 */}
      <Dialog open={isEditOpen} onOpenChange={setIsEditOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader><DialogTitle>编辑库位尺寸限制</DialogTitle></DialogHeader>
          <form onSubmit={handleEditSubmit}>
            <div className="grid grid-cols-2 gap-4 py-4">
              <div className="space-y-2"><Label>最大长度(cm)</Label><Input type="number" value={editData.maxLength} onChange={(e) => setEditData({ ...editData, maxLength: e.target.value })} /></div>
              <div className="space-y-2"><Label>最大宽度(cm)</Label><Input type="number" value={editData.maxWidth} onChange={(e) => setEditData({ ...editData, maxWidth: e.target.value })} /></div>
              <div className="space-y-2"><Label>最大高度(cm)</Label><Input type="number" value={editData.maxHeight} onChange={(e) => setEditData({ ...editData, maxHeight: e.target.value })} /></div>
              <div className="space-y-2"><Label>最大承重(kg)</Label><Input type="number" value={editData.maxWeight} onChange={(e) => setEditData({ ...editData, maxWeight: e.target.value })} /></div>
            </div>
            <DialogFooter>
              <button type="button" onClick={() => setIsEditOpen(false)} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">取消</button>
              <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">保存</button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
