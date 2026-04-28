import { useState, useEffect } from "react";
import {
  Plus, Search, Edit, Power, PowerOff, Trash2,
  ChevronRight, ChevronDown, Warehouse, Layers, Grid3X3,
  Building2, Package, Settings, Eye, LayoutGrid, List
} from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Textarea } from "../../components/ui/textarea";
import { toast } from "sonner";

// ========== 类型定义 ==========
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
  zoneCount?: number;
  locationCount?: number;
}

interface Zone {
  id: number;
  code: string;
  name: string;
  warehouseId: number;
  warehouseCode: string;
  type: number;
  tempRequire: number;
  defaultShelfType: number;
  storageTypes: string;
  locationCount: number;
  status: number;
}

interface ShelfConfig {
  id: number;
  code: string;
  name: string;
  zoneId: number;
  zoneCode: string;
  rowNum: number;
  shelfType: number;
  startLayer: number;
  endLayer: number;
  columnCount: number;
  aisleCount: number;
  depthCount: number;
  storageType: number;
  maxLength: number;
  maxWidth: number;
  maxHeight: number;
  maxWeight: number;
  status: number;
  isGenerated: number;
  locationCount: number;
}

interface Location {
  id: number;
  code: string;
  rowNum: number;
  colNum: number;
  layerNum: number;
  shelfType: number;
  aisleNum: number;
  depthNum: number;
  isBlocked: number;
  status: number;
  currentQty: number;
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

// ========== 常量 ==========
const WAREHOUSE_TYPES: Record<number, string> = { 1: "自营仓", 2: "第三方仓", 3: "海外仓" };
const ZONE_TYPES: Record<number, string> = {
  1: "收货区", 2: "质检区", 3: "存储区", 4: "拣货区", 5: "打包区", 6: "发货区", 7: "退货区", 8: "残次品区"
};
const SHELF_TYPES: Record<number, string> = { 1: "立体货架", 2: "贯通式货架", 3: "自动化仓库" };
const STORAGE_TYPES: Record<number, string> = { 1: "常温", 2: "冷藏", 3: "冷冻", 4: "恒温" };
const LOCATION_STATUS: Record<number, { text: string; color: string }> = {
  1: { text: "空闲", color: "bg-green-100 text-green-700" },
  2: { text: "占用", color: "bg-blue-100 text-blue-700" },
  3: { text: "锁定", color: "bg-yellow-100 text-yellow-700" },
  4: { text: "禁用", color: "bg-gray-100 text-gray-700" },
};

// ========== 主组件 ==========
export default function WarehouseManagement() {
  // 视图模式
  const [viewMode, setViewMode] = useState<"tree" | "grid">("tree");

  // 数据
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [zones, setZones] = useState<Zone[]>([]);
  const [shelfConfigs, setShelfConfigs] = useState<ShelfConfig[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);

  // 选中状态
  const [selectedWarehouse, setSelectedWarehouse] = useState<Warehouse | null>(null);
  const [selectedZone, setSelectedZone] = useState<Zone | null>(null);
  const [selectedShelf, setSelectedShelf] = useState<ShelfConfig | null>(null);

  // 展开状态
  const [expandedWarehouses, setExpandedWarehouses] = useState<Set<number>>(new Set());
  const [expandedZones, setExpandedZones] = useState<Set<number>>(new Set());

  // 弹窗状态
  const [warehouseDialogOpen, setWarehouseDialogOpen] = useState(false);
  const [zoneDialogOpen, setZoneDialogOpen] = useState(false);
  const [shelfDialogOpen, setShelfDialogOpen] = useState(false);
  const [editingWarehouse, setEditingWarehouse] = useState<Warehouse | null>(null);
  const [editingZone, setEditingZone] = useState<Zone | null>(null);
  const [editingShelf, setEditingShelf] = useState<ShelfConfig | null>(null);

  // 加载状态
  const [loading, setLoading] = useState(true);

  // 表单数据
  const [warehouseForm, setWarehouseForm] = useState({
    code: "", name: "", type: 1, storageTypes: "", country: "中国", province: "",
    address: "", area: "", manager: "", phone: ""
  });
  const [zoneForm, setZoneForm] = useState({
    code: "", name: "", type: 3, tempRequire: 1, defaultShelfType: 1, storageTypes: ""
  });
  const [shelfForm, setShelfForm] = useState({
    rowNum: 1, shelfType: 1, startLayer: 1, endLayer: 5, columnCount: 10,
    aisleCount: 1, depthCount: 5,
    storageType: 1, maxLength: "", maxWidth: "", maxHeight: "", maxWeight: ""
  });

  // 初始化加载
  useEffect(() => {
    loadWarehouses();
  }, []);

  async function loadWarehouses() {
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

  async function loadZones(warehouseId: number) {
    try {
      const data = await fetchApi<Zone[]>(`/api/v1/base/zones/warehouse/${warehouseId}`);
      setZones(prev => {
        const others = prev.filter(z => z.warehouseId !== warehouseId);
        return [...others, ...data];
      });
    } catch {
      // ignore
    }
  }

  async function loadShelfConfigs(zoneId: number) {
    try {
      const data = await fetchApi<ShelfConfig[]>(`/api/shelf-config/zone/${zoneId}`);
      setShelfConfigs(prev => {
        const others = prev.filter(s => s.zoneId !== zoneId);
        return [...others, ...data];
      });
    } catch {
      // ignore
    }
  }

  async function loadLocations(shelfId: number) {
    try {
      const shelf = shelfConfigs.find(s => s.id === shelfId);
      if (!shelf) return;
      const data = await fetchApi<{ list: Location[] }>(
        `/api/v1/base/locations?zoneId=${shelf.zoneId}&rowNum=${shelf.rowNum}&limit=1000`
      );
      setLocations(data.list || []);
    } catch {
      setLocations([]);
    }
  }

  // 展开/折叠
  function toggleWarehouse(warehouse: Warehouse) {
    const newExpanded = new Set(expandedWarehouses);
    if (newExpanded.has(warehouse.id)) {
      newExpanded.delete(warehouse.id);
    } else {
      newExpanded.add(warehouse.id);
      loadZones(warehouse.id);
    }
    setExpandedWarehouses(newExpanded);
    setSelectedWarehouse(warehouse);
    setSelectedZone(null);
    setSelectedShelf(null);
  }

  function toggleZone(zone: Zone) {
    const newExpanded = new Set(expandedZones);
    if (newExpanded.has(zone.id)) {
      newExpanded.delete(zone.id);
    } else {
      newExpanded.add(zone.id);
      loadShelfConfigs(zone.id);
    }
    setExpandedZones(newExpanded);
    setSelectedZone(zone);
    setSelectedShelf(null);
  }

  function selectShelf(shelf: ShelfConfig) {
    setSelectedShelf(shelf);
    loadLocations(shelf.id);
  }

  // ========== 仓库操作 ==========
  function openCreateWarehouse() {
    setEditingWarehouse(null);
    setWarehouseForm({
      code: "", name: "", type: 1, storageTypes: "", country: "中国", province: "",
      address: "", area: "", manager: "", phone: ""
    });
    setWarehouseDialogOpen(true);
  }

  function openEditWarehouse(warehouse: Warehouse) {
    setEditingWarehouse(warehouse);
    setWarehouseForm({
      code: warehouse.code,
      name: warehouse.name,
      type: warehouse.type,
      storageTypes: warehouse.storageTypes || "",
      country: warehouse.country || "中国",
      province: warehouse.province || "",
      address: warehouse.address || "",
      area: warehouse.area?.toString() || "",
      manager: warehouse.manager || "",
      phone: warehouse.phone || ""
    });
    setWarehouseDialogOpen(true);
  }

  async function saveWarehouse(e: React.FormEvent) {
    e.preventDefault();
    try {
      const payload = {
        ...warehouseForm,
        area: warehouseForm.area ? parseFloat(warehouseForm.area) : null
      };
      if (editingWarehouse) {
        await fetchApi(`/api/v1/base/warehouses/${editingWarehouse.id}`, {
          method: "PUT",
          body: JSON.stringify(payload)
        });
        toast.success("仓库已更新");
      } else {
        await fetchApi("/api/v1/base/warehouses", {
          method: "POST",
          body: JSON.stringify(payload)
        });
        toast.success("仓库创建成功");
      }
      setWarehouseDialogOpen(false);
      loadWarehouses();
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  }

  async function toggleWarehouseStatus(warehouse: Warehouse) {
    try {
      const action = warehouse.status === 1 ? "disable" : "enable";
      await fetchApi(`/api/v1/base/warehouses/${warehouse.id}/${action}`, { method: "PATCH" });
      toast.success(warehouse.status === 1 ? "仓库已停用" : "仓库已启用");
      loadWarehouses();
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  }

  async function deleteWarehouse(id: number) {
    if (!confirm("确定删除此仓库？相关库区和库位也会被删除。")) return;
    try {
      await fetchApi(`/api/v1/base/warehouses/${id}`, { method: "DELETE" });
      toast.success("仓库已删除");
      loadWarehouses();
      if (selectedWarehouse?.id === id) {
        setSelectedWarehouse(null);
        setSelectedZone(null);
        setSelectedShelf(null);
      }
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  }

  // ========== 库区操作 ==========
  function openCreateZone() {
    if (!selectedWarehouse) return;
    setEditingZone(null);
    setZoneForm({
      code: "", name: "", type: 3, tempRequire: 1, storageTypes: selectedWarehouse.storageTypes?.split(",")[0] || "1"
    });
    setZoneDialogOpen(true);
  }

  function openEditZone(zone: Zone) {
    setEditingZone(zone);
    setZoneForm({
      code: zone.code,
      name: zone.name,
      type: zone.type,
      tempRequire: zone.tempRequire,
      storageTypes: zone.storageTypes || ""
    });
    setZoneDialogOpen(true);
  }

  async function saveZone(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedWarehouse) return;
    try {
      const payload = {
        ...zoneForm,
        warehouseId: selectedWarehouse.id,
        warehouseCode: selectedWarehouse.code
      };
      if (editingZone) {
        await fetchApi(`/api/v1/base/zones/${editingZone.id}`, {
          method: "PUT",
          body: JSON.stringify(payload)
        });
        toast.success("库区已更新");
      } else {
        await fetchApi("/api/v1/base/zones", {
          method: "POST",
          body: JSON.stringify(payload)
        });
        toast.success("库区创建成功");
      }
      setZoneDialogOpen(false);
      loadZones(selectedWarehouse.id);
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  }

  async function deleteZone(zone: Zone) {
    if (!confirm("确定删除此库区？相关库位也会被删除。")) return;
    try {
      await fetchApi(`/api/v1/base/zones/${zone.id}`, { method: "DELETE" });
      toast.success("库区已删除");
      loadZones(zone.warehouseId);
      if (selectedZone?.id === zone.id) {
        setSelectedZone(null);
        setSelectedShelf(null);
      }
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  }

  // ========== 货架操作 ==========
  function openCreateShelf() {
    if (!selectedZone) return;
    setEditingShelf(null);
    // 货架类型继承库区设置
    const inheritedShelfType = selectedZone.defaultShelfType || 1;
    // 自动计算下一个排号：找出当前库区最大排号 + 1
    const existingShelves = shelfConfigs.filter(s => s.zoneId === selectedZone.id);
    const maxRowNum = existingShelves.length > 0
      ? Math.max(...existingShelves.map(s => s.rowNum))
      : 0;
    const nextRowNum = maxRowNum + 1;
    setShelfForm({
      rowNum: nextRowNum, shelfType: inheritedShelfType, startLayer: 1, endLayer: 5, columnCount: 10,
      aisleCount: 1, depthCount: 5,
      storageType: parseInt(selectedZone.storageTypes?.split(",")[0] || "1"),
      maxLength: "", maxWidth: "", maxHeight: "", maxWeight: ""
    });
    setShelfDialogOpen(true);
  }

  async function saveShelf(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedZone || !selectedWarehouse) return;
    try {
      const payload = {
        ...shelfForm,
        zoneId: selectedZone.id,
        zoneCode: selectedZone.code,
        warehouseId: selectedWarehouse.id,
        warehouseCode: selectedWarehouse.code,
        maxLength: shelfForm.maxLength ? parseFloat(shelfForm.maxLength) : null,
        maxWidth: shelfForm.maxWidth ? parseFloat(shelfForm.maxWidth) : null,
        maxHeight: shelfForm.maxHeight ? parseFloat(shelfForm.maxHeight) : null,
        maxWeight: shelfForm.maxWeight ? parseFloat(shelfForm.maxWeight) : null
      };
      if (editingShelf) {
        await fetchApi(`/api/shelf-config/${editingShelf.id}`, {
          method: "PUT",
          body: JSON.stringify(payload)
        });
        toast.success("货架已更新");
      } else {
        await fetchApi("/api/shelf-config", {
          method: "POST",
          body: JSON.stringify(payload)
        });
        toast.success("货架创建成功");
      }
      setShelfDialogOpen(false);
      loadShelfConfigs(selectedZone.id);
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  }

  async function generateLocations(shelf: ShelfConfig) {
    if (!confirm(`确定生成库位？将生成 ${(shelf.endLayer - shelf.startLayer + 1) * shelf.columnCount} 个库位。`)) return;
    try {
      const result = await fetchApi<{ generatedCount: number }>(`/api/shelf-config/${shelf.id}/generate-locations`, {
        method: "POST"
      });
      toast.success(`成功生成 ${result.isGeneratedCount} 个库位`);
      loadShelfConfigs(shelf.zoneId);
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  }

  async function deleteShelf(shelf: ShelfConfig) {
    if (!confirm("确定删除此货架配置？")) return;
    try {
      await fetchApi(`/api/shelf-config/${shelf.id}`, { method: "DELETE" });
      toast.success("货架已删除");
      loadShelfConfigs(shelf.zoneId);
    } catch (err: unknown) {
      toast.error((err as Error).message);
    }
  }

  // ========== 渲染 ==========
  const filteredZones = zones.filter(z => selectedWarehouse && z.warehouseId === selectedWarehouse.id);
  const filteredShelves = shelfConfigs.filter(s => selectedZone && s.zoneId === selectedZone.id);

  return (
    <div className="h-[calc(100vh-120px)] flex gap-4">
      {/* 左侧：树形结构 */}
      <div className="w-80 bg-white rounded-lg border border-gray-200 flex flex-col">
        <div className="p-3 border-b border-gray-200 flex items-center justify-between">
          <h3 className="font-semibold flex items-center gap-2">
            <Warehouse size={18} /> 仓库结构
          </h3>
          <button onClick={openCreateWarehouse} className="p-1 hover:bg-gray-100 rounded" title="新建仓库">
            <Plus size={16} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-2">
          {loading ? (
            <div className="text-center py-8 text-gray-500">加载中...</div>
          ) : warehouses.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              <Building2 size={32} className="mx-auto mb-2 text-gray-300" />
              <div className="text-sm">暂无仓库</div>
              <button onClick={openCreateWarehouse} className="mt-2 text-blue-600 text-sm hover:underline">
                创建第一个仓库
              </button>
            </div>
          ) : (
            <div className="space-y-1">
              {warehouses.map(warehouse => (
                <div key={warehouse.id}>
                  {/* 仓库节点 */}
                  <div
                    className={`flex items-center gap-1 p-2 rounded cursor-pointer hover:bg-gray-100 ${
                      selectedWarehouse?.id === warehouse.id ? "bg-blue-50" : ""
                    }`}
                    onClick={() => toggleWarehouse(warehouse)}
                  >
                    {expandedWarehouses.has(warehouse.id) ? (
                      <ChevronDown size={14} className="text-gray-400" />
                    ) : (
                      <ChevronRight size={14} className="text-gray-400" />
                    )}
                    <Building2 size={16} className={warehouse.status === 1 ? "text-blue-600" : "text-gray-400"} />
                    <span className="flex-1 text-sm truncate">{warehouse.name}</span>
                    <span className="text-xs text-gray-400">{warehouse.code}</span>
                  </div>

                  {/* 库区列表 */}
                  {expandedWarehouses.has(warehouse.id) && (
                    <div className="ml-4 border-l border-gray-200 pl-2">
                      {filteredZones.filter(z => z.warehouseId === warehouse.id).map(zone => (
                        <div key={zone.id}>
                          {/* 库区节点 */}
                          <div
                            className={`flex items-center gap-1 p-2 rounded cursor-pointer hover:bg-gray-100 ${
                              selectedZone?.id === zone.id ? "bg-cyan-50" : ""
                            }`}
                            onClick={() => toggleZone(zone)}
                          >
                            {expandedZones.has(zone.id) ? (
                              <ChevronDown size={12} className="text-gray-400" />
                            ) : (
                              <ChevronRight size={12} className="text-gray-400" />
                            )}
                            <Layers size={14} className="text-cyan-600" />
                            <span className="flex-1 text-sm truncate">{zone.name}</span>
                            <span className="text-xs text-gray-400">{zone.locationCount || 0}位</span>
                          </div>

                          {/* 货架列表 */}
                          {expandedZones.has(zone.id) && (
                            <div className="ml-4 border-l border-gray-200 pl-2">
                              {filteredShelves.filter(s => s.zoneId === zone.id).map(shelf => (
                                <div
                                  key={shelf.id}
                                  className={`flex items-center gap-1 p-2 rounded cursor-pointer hover:bg-gray-100 ${
                                    selectedShelf?.id === shelf.id ? "bg-green-50" : ""
                                  }`}
                                  onClick={() => selectShelf(shelf)}
                                >
                                  <Grid3X3 size={12} className="text-green-600" />
                                  <span className="flex-1 text-sm">第{shelf.rowNum}排</span>
                                  <span className="text-xs text-gray-400">
                                    {shelf.endLayer - shelf.startLayer + 1}层×{shelf.columnCount}位
                                  </span>
                                </div>
                              ))}
                              <button
                                onClick={(e) => { e.stopPropagation(); setSelectedZone(zone); openCreateShelf(); }}
                                className="flex items-center gap-1 p-2 w-full text-left text-sm text-gray-500 hover:bg-gray-100 rounded"
                              >
                                <Plus size={12} /> 添加货架
                              </button>
                            </div>
                          )}
                        </div>
                      ))}
                      <button
                        onClick={(e) => { e.stopPropagation(); setSelectedWarehouse(warehouse); openCreateZone(); }}
                        className="flex items-center gap-1 p-2 w-full text-left text-sm text-gray-500 hover:bg-gray-100 rounded"
                      >
                        <Plus size={12} /> 添加库区
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* 右侧：详情/可视化 */}
      <div className="flex-1 bg-white rounded-lg border border-gray-200 flex flex-col">
        {/* 工具栏 */}
        <div className="p-3 border-b border-gray-200 flex items-center justify-between">
          <div className="flex items-center gap-2">
            {selectedShelf ? (
              <>
                <Grid3X3 size={18} className="text-green-600" />
                <span className="font-semibold">第{selectedShelf.rowNum}排 - {SHELF_TYPES[selectedShelf.shelfType]}</span>
                <span className="text-sm text-gray-500">
                  ({selectedShelf.startLayer}-{selectedShelf.endLayer}层 × {selectedShelf.columnCount}位)
                </span>
              </>
            ) : selectedZone ? (
              <>
                <Layers size={18} className="text-cyan-600" />
                <span className="font-semibold">{selectedZone.name}</span>
                <span className="text-sm text-gray-500">{ZONE_TYPES[selectedZone.type]}</span>
              </>
            ) : selectedWarehouse ? (
              <>
                <Building2 size={18} className="text-blue-600" />
                <span className="font-semibold">{selectedWarehouse.name}</span>
                <span className="text-sm text-gray-500">{WAREHOUSE_TYPES[selectedWarehouse.type]}</span>
              </>
            ) : (
              <span className="text-gray-500">请从左侧选择仓库、库区或货架</span>
            )}
          </div>

          <div className="flex items-center gap-2">
            {/* 视图切换 */}
            <div className="flex border border-gray-300 rounded overflow-hidden">
              <button
                onClick={() => setViewMode("tree")}
                className={`p-1.5 ${viewMode === "tree" ? "bg-blue-600 text-white" : "bg-white hover:bg-gray-100"}`}
                title="列表视图"
              >
                <List size={16} />
              </button>
              <button
                onClick={() => setViewMode("grid")}
                className={`p-1.5 ${viewMode === "grid" ? "bg-blue-600 text-white" : "bg-white hover:bg-gray-100"}`}
                title="可视化视图"
              >
                <LayoutGrid size={16} />
              </button>
            </div>

            {/* 操作按钮 */}
            {selectedWarehouse && !selectedZone && (
              <>
                <button onClick={() => openEditWarehouse(selectedWarehouse)} className="p-1.5 hover:bg-gray-100 rounded" title="编辑仓库">
                  <Edit size={16} />
                </button>
                <button
                  onClick={() => toggleWarehouseStatus(selectedWarehouse)}
                  className={`p-1.5 hover:bg-gray-100 rounded ${selectedWarehouse.status === 1 ? "text-orange-600" : "text-green-600"}`}
                  title={selectedWarehouse.status === 1 ? "停用" : "启用"}
                >
                  {selectedWarehouse.status === 1 ? <PowerOff size={16} /> : <Power size={16} />}
                </button>
                <button onClick={() => deleteWarehouse(selectedWarehouse.id)} className="p-1.5 hover:bg-gray-100 rounded text-red-600" title="删除仓库">
                  <Trash2 size={16} />
                </button>
              </>
            )}
            {selectedZone && !selectedShelf && (
              <>
                <button onClick={openCreateShelf} className="px-2 py-1 bg-green-600 text-white rounded text-sm hover:bg-green-700 flex items-center gap-1">
                  <Plus size={14} /> 添加货架
                </button>
                <button onClick={() => openEditZone(selectedZone)} className="p-1.5 hover:bg-gray-100 rounded" title="编辑库区">
                  <Edit size={16} />
                </button>
                <button onClick={() => deleteZone(selectedZone)} className="p-1.5 hover:bg-gray-100 rounded text-red-600" title="删除库区">
                  <Trash2 size={16} />
                </button>
              </>
            )}
            {selectedShelf && (
              <>
                {selectedShelf.isGenerated === 0 ? (
                  <button
                    onClick={() => generateLocations(selectedShelf)}
                    className="px-2 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 flex items-center gap-1"
                  >
                    <Package size={14} /> 生成库位
                  </button>
                ) : (
                  <span className="text-sm text-green-600 flex items-center gap-1">
                    <Package size={14} /> 已生成 {selectedShelf.locationCount} 个库位
                  </span>
                )}
                <button onClick={() => { setEditingShelf(selectedShelf); setShelfDialogOpen(true); }} className="p-1.5 hover:bg-gray-100 rounded" title="编辑货架">
                  <Edit size={16} />
                </button>
                {selectedShelf.isGenerated === 0 && (
                  <button onClick={() => deleteShelf(selectedShelf)} className="p-1.5 hover:bg-gray-100 rounded text-red-600" title="删除货架">
                    <Trash2 size={16} />
                  </button>
                )}
              </>
            )}
          </div>
        </div>

        {/* 内容区 */}
        <div className="flex-1 overflow-y-auto p-4">
          {!selectedWarehouse ? (
            <div className="text-center py-16 text-gray-500">
              <Building2 size={48} className="mx-auto mb-4 text-gray-300" />
              <div>请从左侧选择一个仓库开始</div>
            </div>
          ) : selectedZone ? (
            selectedShelf ? (
              /* 货架可视化 */
              <ShelfVisualization
                shelf={selectedShelf}
                locations={locations}
                viewMode={viewMode}
              />
            ) : (
              /* 货架列表 */
              <div className="space-y-4">
                <div className="text-sm text-gray-500 mb-2">
                  {selectedZone.name} - 共 {filteredShelves.filter(s => s.zoneId === selectedZone.id).length} 个货架配置
                </div>
                {viewMode === "tree" ? (
                  <div className="space-y-2">
                    {filteredShelves.filter(s => s.zoneId === selectedZone.id).map(shelf => (
                      <div
                        key={shelf.id}
                        className="p-3 border border-gray-200 rounded hover:bg-gray-50 cursor-pointer"
                        onClick={() => selectShelf(shelf)}
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <Grid3X3 size={16} className="text-green-600" />
                            <span className="font-medium">第{shelf.rowNum}排</span>
                            <span className="text-sm text-gray-500">{SHELF_TYPES[shelf.shelfType]}</span>
                          </div>
                          <div className="flex items-center gap-3 text-sm">
                            <span>{shelf.endLayer - shelf.startLayer + 1}层 × {shelf.columnCount}位</span>
                            <span className={shelf.isGenerated === 1 ? "text-green-600" : "text-gray-400"}>
                              {shelf.isGenerated === 1 ? `已生成${shelf.locationCount}位` : "未生成"}
                            </span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="grid grid-cols-4 gap-4">
                    {filteredShelves.filter(s => s.zoneId === selectedZone.id).map(shelf => (
                      <div
                        key={shelf.id}
                        className="p-4 border border-gray-200 rounded hover:border-blue-400 cursor-pointer"
                        onClick={() => selectShelf(shelf)}
                      >
                        <div className="text-center">
                          <div className="text-2xl font-bold text-blue-600">{shelf.rowNum}</div>
                          <div className="text-sm text-gray-500">第{shelf.rowNum}排</div>
                          <div className="mt-2 text-xs text-gray-400">
                            {shelf.endLayer - shelf.startLayer + 1}层 × {shelf.columnCount}位
                          </div>
                          <div className={`mt-2 text-xs ${shelf.isGenerated === 1 ? "text-green-600" : "text-gray-400"}`}>
                            {shelf.isGenerated === 1 ? `${shelf.locationCount}库位` : "待生成"}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )
          ) : (
            /* 库区列表 */
            <div className="space-y-4">
              <div className="text-sm text-gray-500 mb-2">
                {selectedWarehouse.name} - 共 {filteredZones.filter(z => z.warehouseId === selectedWarehouse.id).length} 个库区
              </div>
              {viewMode === "tree" ? (
                <div className="space-y-2">
                  {filteredZones.filter(z => z.warehouseId === selectedWarehouse.id).map(zone => (
                    <div
                      key={zone.id}
                      className="p-3 border border-gray-200 rounded hover:bg-gray-50 cursor-pointer"
                      onClick={() => toggleZone(zone)}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <Layers size={16} className="text-cyan-600" />
                          <span className="font-medium">{zone.name}</span>
                          <span className="text-sm text-gray-500">{ZONE_TYPES[zone.type]}</span>
                        </div>
                        <div className="text-sm text-gray-400">
                          {zone.locationCount || 0} 个库位
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="grid grid-cols-3 gap-4">
                  {filteredZones.filter(z => z.warehouseId === selectedWarehouse.id).map(zone => (
                    <div
                      key={zone.id}
                      className="p-4 border border-gray-200 rounded hover:border-cyan-400 cursor-pointer"
                      onClick={() => toggleZone(zone)}
                    >
                      <div className="flex items-center gap-2 mb-2">
                        <Layers size={20} className="text-cyan-600" />
                        <span className="font-medium">{zone.name}</span>
                      </div>
                      <div className="text-sm text-gray-500">{ZONE_TYPES[zone.type]}</div>
                      <div className="mt-2 text-lg font-bold">{zone.locationCount || 0}</div>
                      <div className="text-xs text-gray-400">库位数量</div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* 仓库弹窗 */}
      <Dialog open={warehouseDialogOpen} onOpenChange={setWarehouseDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editingWarehouse ? "编辑仓库" : "新建仓库"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={saveWarehouse}>
            <div className="grid grid-cols-2 gap-4 py-4">
              <div className="space-y-2">
                <Label>仓库编码 *</Label>
                <Input value={warehouseForm.code} onChange={e => setWarehouseForm({ ...warehouseForm, code: e.target.value })} required disabled={!!editingWarehouse} />
              </div>
              <div className="space-y-2">
                <Label>仓库名称 *</Label>
                <Input value={warehouseForm.name} onChange={e => setWarehouseForm({ ...warehouseForm, name: e.target.value })} required />
              </div>
              <div className="space-y-2">
                <Label>仓库类型 *</Label>
                <Select value={warehouseForm.type.toString()} onValueChange={v => setWarehouseForm({ ...warehouseForm, type: parseInt(v) })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="1">自营仓</SelectItem>
                    <SelectItem value="2">第三方仓</SelectItem>
                    <SelectItem value="3">海外仓</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>存储类型</Label>
                <div className="flex flex-wrap gap-2">
                  {[1, 2, 3, 4].map(t => {
                    const selected = warehouseForm.storageTypes.split(",").filter(Boolean).map(Number).includes(t);
                    return (
                      <button key={t} type="button" onClick={() => {
                        const current = warehouseForm.storageTypes.split(",").filter(Boolean).map(Number);
                        const updated = selected ? current.filter(x => x !== t) : [...current, t];
                        setWarehouseForm({ ...warehouseForm, storageTypes: updated.join(",") });
                      }} className={`px-3 py-1 rounded text-sm border ${selected ? "bg-blue-600 text-white border-blue-600" : "bg-white border-gray-300"}`}>
                        {STORAGE_TYPES[t]}
                      </button>
                    );
                  })}
                </div>
              </div>
              <div className="space-y-2">
                <Label>国家</Label>
                <Input value={warehouseForm.country} onChange={e => setWarehouseForm({ ...warehouseForm, country: e.target.value })} />
              </div>
              <div className="space-y-2">
                <Label>省份/城市</Label>
                <Input value={warehouseForm.province} onChange={e => setWarehouseForm({ ...warehouseForm, province: e.target.value })} />
              </div>
              <div className="space-y-2">
                <Label>面积 (㎡)</Label>
                <Input type="number" value={warehouseForm.area} onChange={e => setWarehouseForm({ ...warehouseForm, area: e.target.value })} />
              </div>
              <div className="space-y-2">
                <Label>负责人</Label>
                <Input value={warehouseForm.manager} onChange={e => setWarehouseForm({ ...warehouseForm, manager: e.target.value })} />
              </div>
              <div className="space-y-2">
                <Label>联系电话</Label>
                <Input value={warehouseForm.phone} onChange={e => setWarehouseForm({ ...warehouseForm, phone: e.target.value })} />
              </div>
              <div className="space-y-2 col-span-2">
                <Label>详细地址</Label>
                <Textarea value={warehouseForm.address} onChange={e => setWarehouseForm({ ...warehouseForm, address: e.target.value })} rows={2} />
              </div>
            </div>
            <DialogFooter>
              <button type="button" onClick={() => setWarehouseDialogOpen(false)} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">取消</button>
              <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">{editingWarehouse ? "保存" : "创建"}</button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* 库区弹窗 */}
      <Dialog open={zoneDialogOpen} onOpenChange={setZoneDialogOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>{editingZone ? "编辑库区" : "新建库区"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={saveZone}>
            <div className="space-y-4 py-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>库区编码 *</Label>
                  <Input value={zoneForm.code} onChange={e => setZoneForm({ ...zoneForm, code: e.target.value })} required disabled={!!editingZone} />
                </div>
                <div className="space-y-2">
                  <Label>库区名称 *</Label>
                  <Input value={zoneForm.name} onChange={e => setZoneForm({ ...zoneForm, name: e.target.value })} required />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>库区类型 *</Label>
                  <Select value={zoneForm.type.toString()} onValueChange={v => setZoneForm({ ...zoneForm, type: parseInt(v) })}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      {Object.entries(ZONE_TYPES).map(([k, v]) => (
                        <SelectItem key={k} value={k}>{v}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>温度要求</Label>
                  <Select value={zoneForm.tempRequire.toString()} onValueChange={v => setZoneForm({ ...zoneForm, tempRequire: parseInt(v) })}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      {Object.entries(STORAGE_TYPES).map(([k, v]) => (
                        <SelectItem key={k} value={k}>{v}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div className="space-y-2">
                <Label>默认货架类型</Label>
                <Select value={(zoneForm.defaultShelfType || 1).toString()} onValueChange={v => setZoneForm({ ...zoneForm, defaultShelfType: parseInt(v) })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {Object.entries(SHELF_TYPES).map(([k, v]) => (
                      <SelectItem key={k} value={k}>{v}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <p className="text-xs text-gray-500">新建货架时默认使用此类型</p>
              </div>
            </div>
            <DialogFooter>
              <button type="button" onClick={() => setZoneDialogOpen(false)} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">取消</button>
              <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">{editingZone ? "保存" : "创建"}</button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* 货架弹窗 */}
      <Dialog open={shelfDialogOpen} onOpenChange={setShelfDialogOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editingShelf ? "编辑货架" : "新建货架"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={saveShelf}>
            <div className="space-y-4 py-4">
              {/* 货架类型 - 继承自库区，只读显示 */}
              <div className="p-3 bg-gray-50 rounded flex items-center gap-2">
                <span className="text-sm text-gray-500">货架类型：</span>
                <span className="font-medium">{SHELF_TYPES[shelfForm.shelfType] || "立体货架"}</span>
                <span className="text-xs text-gray-400">（继承自库区设置）</span>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>排号 *</Label>
                  <Input type="number" min="1" value={shelfForm.rowNum} onChange={e => setShelfForm({ ...shelfForm, rowNum: parseInt(e.target.value) || 1 })} required />
                </div>
              </div>

              {/* 层数配置 - 所有类型通用 */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>起始层 *</Label>
                  <Input type="number" min="1" value={shelfForm.startLayer} onChange={e => setShelfForm({ ...shelfForm, startLayer: parseInt(e.target.value) || 1 })} required />
                </div>
                <div className="space-y-2">
                  <Label>结束层 *</Label>
                  <Input type="number" min="1" value={shelfForm.endLayer} onChange={e => setShelfForm({ ...shelfForm, endLayer: parseInt(e.target.value) || 1 })} required />
                </div>
              </div>

              {/* 立体货架 / 自动化仓库：每层位置数 */}
              {(shelfForm.shelfType === 1 || shelfForm.shelfType === 3) && (
                <div className="space-y-2">
                  <Label>每层位置数 *</Label>
                  <Input type="number" min="1" value={shelfForm.columnCount} onChange={e => setShelfForm({ ...shelfForm, columnCount: parseInt(e.target.value) || 1 })} required />
                </div>
              )}

              {/* 贯通式货架：通道数和深度数 */}
              {shelfForm.shelfType === 2 && (
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>通道数 *</Label>
                    <Input type="number" min="1" value={shelfForm.aisleCount} onChange={e => setShelfForm({ ...shelfForm, aisleCount: parseInt(e.target.value) || 1 })} required />
                    <p className="text-xs text-gray-500">叉车驶入的通道数量</p>
                  </div>
                  <div className="space-y-2">
                    <Label>深度数 *</Label>
                    <Input type="number" min="1" value={shelfForm.depthCount} onChange={e => setShelfForm({ ...shelfForm, depthCount: parseInt(e.target.value) || 1 })} required />
                    <p className="text-xs text-gray-500">每个通道的深度位置数</p>
                  </div>
                </div>
              )}

              {/* 预览生成数量 */}
              <div className="p-3 bg-blue-50 rounded text-sm">
                {shelfForm.shelfType === 2 ? (
                  <>将生成 <strong>{(shelfForm.endLayer - shelfForm.startLayer + 1) * shelfForm.aisleCount * shelfForm.depthCount}</strong> 个库位</>
                ) : (
                  <>将生成 <strong>{(shelfForm.endLayer - shelfForm.startLayer + 1) * shelfForm.columnCount}</strong> 个库位</>
                )}
                <div className="text-xs text-gray-500 mt-1">
                  {shelfForm.shelfType === 1 && "立体货架：每个库位独立，可任意存取"}
                  {shelfForm.shelfType === 2 && "贯通式货架：同一通道内先进后出"}
                  {shelfForm.shelfType === 3 && "自动化仓库：堆垛机自动存取"}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>存储类型</Label>
                  <Select value={shelfForm.storageType.toString()} onValueChange={v => setShelfForm({ ...shelfForm, storageType: parseInt(v) })}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      {Object.entries(STORAGE_TYPES).map(([k, v]) => (
                        <SelectItem key={k} value={k}>{v}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div className="grid grid-cols-4 gap-2">
                <div className="space-y-2">
                  <Label>长(cm)</Label>
                  <Input type="number" value={shelfForm.maxLength} onChange={e => setShelfForm({ ...shelfForm, maxLength: e.target.value })} />
                </div>
                <div className="space-y-2">
                  <Label>宽(cm)</Label>
                  <Input type="number" value={shelfForm.maxWidth} onChange={e => setShelfForm({ ...shelfForm, maxWidth: e.target.value })} />
                </div>
                <div className="space-y-2">
                  <Label>高(cm)</Label>
                  <Input type="number" value={shelfForm.maxHeight} onChange={e => setShelfForm({ ...shelfForm, maxHeight: e.target.value })} />
                </div>
                <div className="space-y-2">
                  <Label>承重(kg)</Label>
                  <Input type="number" value={shelfForm.maxWeight} onChange={e => setShelfForm({ ...shelfForm, maxWeight: e.target.value })} />
                </div>
              </div>
            </div>
            <DialogFooter>
              <button type="button" onClick={() => setShelfDialogOpen(false)} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">取消</button>
              <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">{editingShelf ? "保存" : "创建"}</button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// ========== 货架可视化组件 ==========
function ShelfVisualization({ shelf, locations, viewMode }: { shelf: ShelfConfig; locations: Location[]; viewMode: "tree" | "grid" }) {
  const layers = [];
  for (let l = shelf.startLayer; l <= shelf.endLayer; l++) {
    const layerLocations = locations.filter(loc => loc.layerNum === l);
    layers.push({ layer: l, locations: layerLocations });
  }

  // 贯通式货架的可视化
  if (shelf.shelfType === 2) {
    return <DriveInShelfVisualization shelf={shelf} locations={locations} viewMode={viewMode} />;
  }

  // 立体货架和自动化仓库的可视化
  if (viewMode === "grid") {
    return (
      <div className="space-y-4">
        <div className="text-sm text-gray-500">
          第{shelf.rowNum}排 - {SHELF_TYPES[shelf.shelfType]} - 共 {locations.length} 个库位
        </div>
        <div className="space-y-2">
          {[...layers].reverse().map(({ layer, locations: layerLocs }) => (
            <div key={layer} className="flex items-center gap-2">
              <div className="w-12 text-sm text-gray-500 text-right">{layer}层</div>
              <div className="flex-1 flex gap-1">
                {Array.from({ length: shelf.columnCount }, (_, i) => {
                  const col = i + 1;
                  const loc = layerLocs.find(l => l.colNum === col);
                  const status = loc?.status || 1;
                  const statusInfo = LOCATION_STATUS[status];
                  return (
                    <div
                      key={col}
                      className={`flex-1 h-10 rounded flex items-center justify-center text-xs cursor-pointer hover:opacity-80 ${statusInfo?.color || "bg-gray-100"}`}
                      title={loc ? `${loc.code}\n状态: ${statusInfo?.text}\n数量: ${loc.currentQty || 0}` : `第${col}位`}
                    >
                      {col}
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
        <div className="flex gap-4 text-sm">
          {Object.entries(LOCATION_STATUS).map(([k, v]) => (
            <div key={k} className="flex items-center gap-1">
              <div className={`w-4 h-4 rounded ${v.color}`}></div>
              <span>{v.text}</span>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="text-sm text-gray-500">
        第{shelf.rowNum}排 - {SHELF_TYPES[shelf.shelfType]} - 共 {locations.length} 个库位
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50">
              <th className="px-3 py-2 text-left">库位编码</th>
              <th className="px-3 py-2 text-center">层</th>
              <th className="px-3 py-2 text-center">列</th>
              <th className="px-3 py-2 text-center">状态</th>
              <th className="px-3 py-2 text-center">数量</th>
            </tr>
          </thead>
          <tbody>
            {locations.map(loc => {
              const statusInfo = LOCATION_STATUS[loc.status];
              return (
                <tr key={loc.id} className="border-b border-gray-100 hover:bg-gray-50">
                  <td className="px-3 py-2 font-mono">{loc.code}</td>
                  <td className="px-3 py-2 text-center">{loc.layerNum}</td>
                  <td className="px-3 py-2 text-center">{loc.colNum}</td>
                  <td className="px-3 py-2 text-center">
                    <span className={`px-2 py-0.5 rounded text-xs ${statusInfo?.color}`}>
                      {statusInfo?.text}
                    </span>
                  </td>
                  <td className="px-3 py-2 text-center">{loc.currentQty || 0}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ========== 贯通式货架可视化组件 ==========
function DriveInShelfVisualization({ shelf, locations, viewMode }: { shelf: ShelfConfig; locations: Location[]; viewMode: "tree" | "grid" }) {
  const aisleCount = shelf.aisleCount || 1;
  const depthCount = shelf.depthCount || shelf.columnCount || 1;

  if (viewMode === "grid") {
    return (
      <div className="space-y-4">
        <div className="text-sm text-gray-500 mb-2">
          第{shelf.rowNum}排 - 贯通式货架 - 共 {locations.length} 个库位
        </div>
        <div className="text-xs text-gray-400 mb-4">
          提示：同一通道内，深度号越大越靠里，上架时推荐最里侧，取货时只能取最外侧
        </div>

        {/* 按层显示 */}
        {Array.from({ length: shelf.endLayer - shelf.startLayer + 1 }, (_, i) => shelf.startLayer + i).reverse().map(layer => {
          const layerLocations = locations.filter(l => l.layerNum === layer);
          return (
            <div key={layer} className="mb-4">
              <div className="text-sm font-medium mb-2">{layer}层</div>
              <div className="flex gap-4">
                {/* 按通道显示 */}
                {Array.from({ length: aisleCount }, (_, aisleIdx) => {
                  const aisle = aisleIdx + 1;
                  const aisleLocations = layerLocations.filter(l => l.aisleNum === aisle);
                  return (
                    <div key={aisle} className="border border-gray-200 rounded p-2">
                      <div className="text-xs text-gray-500 mb-1 text-center">通道{aisle}</div>
                      <div className="flex flex-col gap-1">
                        {/* 深度从大到小显示（从里到外） */}
                        {Array.from({ length: depthCount }, (_, i) => depthCount - i).map(depth => {
                          const loc = aisleLocations.find(l => l.depthNum === depth);
                          const status = loc?.status || 1;
                          const statusInfo = LOCATION_STATUS[status];
                          const isBlocked = loc?.isBlocked === 1;
                          return (
                            <div
                              key={depth}
                              className={`h-8 rounded flex items-center justify-center text-xs cursor-pointer relative ${
                                isBlocked ? "bg-yellow-100 border-2 border-yellow-400" : (statusInfo?.color || "bg-gray-100")
                              }`}
                              title={loc ? `${loc.code}\n深度${depth}(${depth === depthCount ? "最里" : depth === 1 ? "最外" : ""})\n状态: ${statusInfo?.text}${isBlocked ? "\n⚠️ 被挡住" : ""}\n数量: ${loc.currentQty || 0}` : `深度${depth}`}
                            >
                              {depth}
                              {isBlocked && <span className="absolute -top-1 -right-1 w-2 h-2 bg-yellow-500 rounded-full"></span>}
                            </div>
                          );
                        })}
                      </div>
                      <div className="text-xs text-gray-400 mt-1 text-center">
                        ↑ 外 | 里 ↓
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}

        <div className="flex gap-4 text-sm mt-4">
          {Object.entries(LOCATION_STATUS).map(([k, v]) => (
            <div key={k} className="flex items-center gap-1">
              <div className={`w-4 h-4 rounded ${v.color}`}></div>
              <span>{v.text}</span>
            </div>
          ))}
          <div className="flex items-center gap-1">
            <div className="w-4 h-4 rounded bg-yellow-100 border-2 border-yellow-400"></div>
            <span>被挡住</span>
          </div>
        </div>
      </div>
    );
  }

  // 列表视图
  return (
    <div className="space-y-4">
      <div className="text-sm text-gray-500">
        第{shelf.rowNum}排 - 贯通式货架 - 共 {locations.length} 个库位
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50">
              <th className="px-3 py-2 text-left">库位编码</th>
              <th className="px-3 py-2 text-center">层</th>
              <th className="px-3 py-2 text-center">通道</th>
              <th className="px-3 py-2 text-center">深度</th>
              <th className="px-3 py-2 text-center">状态</th>
              <th className="px-3 py-2 text-center">被挡</th>
              <th className="px-3 py-2 text-center">数量</th>
            </tr>
          </thead>
          <tbody>
            {locations.map(loc => {
              const statusInfo = LOCATION_STATUS[loc.status];
              return (
                <tr key={loc.id} className="border-b border-gray-100 hover:bg-gray-50">
                  <td className="px-3 py-2 font-mono">{loc.code}</td>
                  <td className="px-3 py-2 text-center">{loc.layerNum}</td>
                  <td className="px-3 py-2 text-center">{loc.aisleNum}</td>
                  <td className="px-3 py-2 text-center">{loc.depthNum}</td>
                  <td className="px-3 py-2 text-center">
                    <span className={`px-2 py-0.5 rounded text-xs ${statusInfo?.color}`}>
                      {statusInfo?.text}
                    </span>
                  </td>
                  <td className="px-3 py-2 text-center">
                    {loc.isBlocked === 1 ? (
                      <span className="text-yellow-600">⚠️ 是</span>
                    ) : (
                      <span className="text-gray-400">否</span>
                    )}
                  </td>
                  <td className="px-3 py-2 text-center">{loc.currentQty || 0}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
