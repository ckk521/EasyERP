import { useState, useEffect } from "react";
import { Search, Package, Building2, CheckCircle, History, ChevronRight, Layers } from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Button } from "../../components/ui/button";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import DataTable from "../../components/DataTable";
import { toast } from "sonner";

// ==================== 类型定义 ====================

interface InboundOrderItem {
  id: number;
  productId: number;
  skuCode: string;
  productName: string;
  qualifiedQty: number;
  putawayQty: number;
  batchNo: string;
  status: number;
}

interface InboundOrder {
  id: number;
  orderNo: string;
  warehouseId: number;
  warehouseName: string;
  supplierName: string;
  status: number;
  statusName: string;
  totalQualifiedQty: number;
  totalPutawayQty: number;
  progressPutaway: number;
  items: InboundOrderItem[];
}

interface Zone {
  id: number;
  code: string;
  name: string;
  warehouseId?: number;
}

interface Location {
  id: number;
  code: string;
  name: string;
  warehouseId: number;
  warehouseName: string;
  zoneId: number;
  zoneName: string;
  type: number;
  typeName: string;
}

interface PutawayRecord {
  id: number;
  inboundOrderId: number;
  inboundOrderNo: string;
  inboundItemId: number;
  productId: number;
  skuCode: string;
  productName: string;
  locationCode: string;
  putawayQty: number;
  putawayTime: string;
  putawayUser: number;
  putawayUserName: string;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

// ==================== 常量定义 ====================

const STATUS_COLORS: Record<number, string> = {
  0: "bg-yellow-100 text-yellow-700",
  1: "bg-blue-100 text-blue-700",
  2: "bg-blue-100 text-blue-700",
  3: "bg-orange-100 text-orange-700",
  4: "bg-green-100 text-green-700",
  9: "bg-gray-100 text-gray-500",
};

const STATUS_NAMES: Record<number, string> = {
  0: "待收货",
  1: "收货中",
  2: "验收中",
  3: "待上架",
  4: "已完成",
  9: "已取消",
};

// ==================== API 函数 ====================

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

// ==================== 主组件 ====================

export default function PutawayPage() {
  // 入库单列表
  const [orders, setOrders] = useState<InboundOrder[]>([]);
  const [loadingOrders, setLoadingOrders] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<InboundOrder | null>(null);

  // 上架记录（全局）
  const [recentRecords, setRecentRecords] = useState<PutawayRecord[]>([]);
  const [loadingRecords, setLoadingRecords] = useState(false);

  // 当前入库单的上架记录
  const [orderRecords, setOrderRecords] = useState<PutawayRecord[]>([]);

  // 操作状态
  const [loading, setLoading] = useState(false);

  // 上架弹窗
  const [putawayModalOpen, setPutawayModalOpen] = useState(false);
  const [currentItem, setCurrentItem] = useState<InboundOrderItem | null>(null);
  const [selectedZoneId, setSelectedZoneId] = useState<number | "">("");
  const [selectedLocationId, setSelectedLocationId] = useState<number | "">("");
  const [putawayQty, setPutawayQty] = useState(0);
  const [zones, setZones] = useState<Zone[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);
  const [loadingZones, setLoadingZones] = useState(false);
  const [loadingLocations, setLoadingLocations] = useState(false);

  // 根据选中的库区过滤库位
  const filteredLocations = selectedZoneId
    ? locations.filter(loc => loc.zoneId === selectedZoneId)
    : locations;

  // 加载待上架的入库单列表
  const loadPendingOrders = async () => {
    setLoadingOrders(true);
    try {
      // 状态 2=验收中, 3=待上架（验收中有合格商品即可开始上架）
      const data = await fetchApi<{ list: InboundOrder[] }>("/api/v1/inbound/orders?status=2,3&limit=50");
      setOrders(data.list || []);
    } catch (error) {
      console.error("Failed to load orders:", error);
      setOrders([]);
    } finally {
      setLoadingOrders(false);
    }
  };

  // 加载最近的上架记录
  const loadRecentRecords = async () => {
    setLoadingRecords(true);
    try {
      const data = await fetchApi<{ list: PutawayRecord[] }>("/api/v1/inbound/putaway/records/recent?limit=20");
      setRecentRecords(data.list || []);
    } catch (error) {
      console.error("Failed to load records:", error);
      setRecentRecords([]);
    } finally {
      setLoadingRecords(false);
    }
  };

  // 初始化加载
  useEffect(() => {
    loadPendingOrders();
    loadRecentRecords();
  }, []);

  // 选择入库单
  const handleSelectOrder = async (order: InboundOrder) => {
    try {
      const detail = await fetchApi<InboundOrder>(`/api/v1/inbound/orders/${order.id}`);
      setSelectedOrder(detail);
      // 加载该入库单的上架记录
      const records = await fetchApi<PutawayRecord[]>(`/api/v1/inbound/putaway/records/order/${order.id}`);
      setOrderRecords(records || []);
    } catch (error) {
      toast.error("加载入库单详情失败");
    }
  };

  // 打开上架弹窗
  const openPutawayModal = async (item: InboundOrderItem) => {
    setCurrentItem(item);
    const remaining = (item.qualifiedQty || 0) - (item.putawayQty || 0);
    setPutawayQty(remaining);
    setSelectedZoneId("");
    setSelectedLocationId("");
    setPutawayModalOpen(true);

    // 加载该仓库的库区和库位列表
    if (selectedOrder?.warehouseId) {
      setLoadingZones(true);
      setLoadingLocations(true);
      try {
        const [zonesData, locationsData] = await Promise.all([
          fetchApi<{ list: Zone[] }>(`/api/v1/base/zones?warehouseId=${selectedOrder.warehouseId}&limit=200`),
          fetchApi<{ list: Location[] }>(`/api/v1/base/locations?warehouseId=${selectedOrder.warehouseId}&limit=200`)
        ]);
        setZones(zonesData.list || []);
        setLocations(locationsData.list || []);
      } catch {
        setZones([]);
        setLocations([]);
      } finally {
        setLoadingZones(false);
        setLoadingLocations(false);
      }
    }
  };

  // 执行上架
  const handlePutaway = async () => {
    if (!selectedOrder || !currentItem) return;

    if (!selectedLocationId) {
      toast.error("请选择库位");
      return;
    }

    const selectedLocation = locations.find(l => l.id === selectedLocationId);
    if (!selectedLocation) {
      toast.error("库位不存在");
      return;
    }

    const remaining = (currentItem.qualifiedQty || 0) - (currentItem.putawayQty || 0);
    if (putawayQty > remaining) {
      toast.error("上架数量超过待上架数量");
      return;
    }

    setLoading(true);
    try {
      await fetchApi("/api/v1/inbound/putaway/execute", {
        method: "POST",
        body: JSON.stringify({
          orderId: selectedOrder.id,
          itemId: currentItem.id,
          locationCode: selectedLocation.code,
          putawayQty,
        }),
      });
      toast.success("上架成功");
      setPutawayModalOpen(false);

      // 刷新数据
      const detail = await fetchApi<InboundOrder>(`/api/v1/inbound/orders/${selectedOrder.id}`);
      setSelectedOrder(detail);
      const records = await fetchApi<PutawayRecord[]>(`/api/v1/inbound/putaway/records/order/${selectedOrder.id}`);
      setOrderRecords(records || []);

      // 刷新列表和全局记录
      loadPendingOrders();
      loadRecentRecords();
    } catch (error) {
      const message = error instanceof Error ? error.message : "上架失败";
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  // 入库单列表列定义
  const orderColumns = [
    { key: "orderNo", title: "入库单号", width: "120px", render: (v: string) => <span className="font-mono">{v}</span> },
    { key: "supplierName", title: "供应商", width: "150px", render: (v: string) => v || "-" },
    { key: "warehouseName", title: "仓库", width: "100px", render: (v: string) => v || "-" },
    {
      key: "status",
      title: "状态",
      width: "80px",
      render: (v: number) => (
        <span className={`px-2 py-0.5 rounded text-xs ${STATUS_COLORS[v] || "bg-gray-100"}`}>
          {STATUS_NAMES[v]}
        </span>
      ),
    },
    { key: "totalQualifiedQty", title: "合格数量", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    {
      key: "progress",
      title: "上架进度",
      width: "100px",
      render: (_: unknown, row: InboundOrder) => (
        <div className="flex items-center gap-2">
          <div className="flex-1 bg-gray-200 rounded-full h-1.5">
            <div className="bg-orange-600 h-1.5 rounded-full" style={{ width: `${row.progressPutaway || 0}%` }} />
          </div>
          <span className="text-xs text-gray-500">{row.progressPutaway || 0}%</span>
        </div>
      ),
    },
    {
      key: "action",
      title: "操作",
      width: "60px",
      render: (_: unknown, row: InboundOrder) => (
        <button
          onClick={() => handleSelectOrder(row)}
          className="text-blue-600 hover:text-blue-700 flex items-center gap-1"
        >
          上架 <ChevronRight size={14} />
        </button>
      ),
    },
  ];

  // 商品明细列定义
  const itemColumns = [
    { key: "skuCode", title: "SKU编码", width: "120px" },
    { key: "productName", title: "商品名称", width: "150px" },
    { key: "qualifiedQty", title: "合格数量", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "putawayQty", title: "已上架", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    {
      key: "pending",
      title: "待上架",
      width: "80px",
      render: (_: unknown, item: InboundOrderItem) => {
        const pending = (item.qualifiedQty || 0) - (item.putawayQty || 0);
        return <span className={pending > 0 ? "text-orange-600 font-medium" : "text-green-600"}>{pending}</span>;
      },
    },
    { key: "batchNo", title: "批次号", width: "150px", render: (v: string) => v || "-" },
    {
      key: "action",
      title: "操作",
      width: "80px",
      render: (_: unknown, item: InboundOrderItem) => {
        const pending = (item.qualifiedQty || 0) - (item.putawayQty || 0);
        return (
          <button
            onClick={() => openPutawayModal(item)}
            disabled={pending <= 0}
            className="text-blue-600 hover:text-blue-700 disabled:text-gray-400"
          >
            上架
          </button>
        );
      },
    },
  ];

  // 上架记录列定义
  const recordColumns = [
    { key: "putawayTime", title: "上架时间", width: "150px", render: (v: string) => v?.replace("T", " ").slice(0, 16) },
    { key: "inboundOrderNo", title: "入库单号", width: "100px", render: (v: string) => <span className="font-mono">{v}</span> },
    { key: "skuCode", title: "SKU编码", width: "100px" },
    { key: "productName", title: "商品名称", width: "120px" },
    { key: "locationCode", title: "库位", width: "80px", render: (v: string) => <span className="font-mono">{v}</span> },
    { key: "putawayQty", title: "上架数量", width: "80px", render: (v: number) => <span className="font-medium">{v}</span> },
    { key: "putawayUserName", title: "操作人", width: "80px" },
  ];

  // 当前入库单的上架记录列定义
  const orderRecordColumns = [
    { key: "putawayTime", title: "上架时间", width: "150px", render: (v: string) => v?.replace("T", " ").slice(0, 16) },
    { key: "skuCode", title: "SKU编码", width: "100px" },
    { key: "productName", title: "商品名称", width: "120px" },
    { key: "locationCode", title: "库位", width: "80px", render: (v: string) => <span className="font-mono">{v}</span> },
    { key: "putawayQty", title: "上架数量", width: "80px", render: (v: number) => <span className="font-medium">{v}</span> },
    { key: "putawayUserName", title: "操作人", width: "80px" },
  ];

  return (
    <div className="space-y-4">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">上架作业</h2>
        <button
          onClick={() => { loadPendingOrders(); loadRecentRecords(); }}
          className="text-sm text-blue-600 hover:text-blue-700"
        >
          刷新
        </button>
      </div>

      {/* 入库单列表 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <h3 className="text-sm font-semibold mb-3 text-gray-700">待上架入库单</h3>
        {loadingOrders ? (
          <div className="text-center py-8 text-gray-400 text-sm">加载中...</div>
        ) : orders.length > 0 ? (
          <DataTable columns={orderColumns} data={orders} />
        ) : (
          <div className="text-center py-8 text-gray-400 text-sm">暂无待上架入库单</div>
        )}
      </div>

      {/* 选中的入库单详情 */}
      {selectedOrder && (
        <>
          {/* 入库单信息 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-semibold text-gray-700">入库单信息</h3>
              <button
                onClick={() => setSelectedOrder(null)}
                className="text-sm text-gray-500 hover:text-gray-700"
              >
                返回列表
              </button>
            </div>
            <div className="grid grid-cols-4 gap-4">
              <div className="flex items-center gap-2">
                <Package size={16} className="text-gray-400" />
                <span className="text-sm text-gray-500">入库单号：</span>
                <span className="text-sm font-medium font-mono">{selectedOrder.orderNo}</span>
              </div>
              <div className="flex items-center gap-2">
                <Building2 size={16} className="text-gray-400" />
                <span className="text-sm text-gray-500">仓库：</span>
                <span className="text-sm font-medium">{selectedOrder.warehouseName || "-"}</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-sm text-gray-500">供应商：</span>
                <span className="text-sm font-medium">{selectedOrder.supplierName || "-"}</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle size={16} className="text-gray-400" />
                <span className="text-sm text-gray-500">状态：</span>
                <span className={`px-2 py-0.5 rounded text-xs ${STATUS_COLORS[selectedOrder.status] || "bg-gray-100"}`}>
                  {selectedOrder.statusName || STATUS_NAMES[selectedOrder.status]}
                </span>
              </div>
            </div>
            <div className="mt-3 pt-3 border-t">
              <div className="flex items-center gap-4">
                <span className="text-sm text-gray-500">上架进度：</span>
                <div className="flex-1 bg-gray-200 rounded-full h-2">
                  <div className="bg-orange-600 h-2 rounded-full transition-all" style={{ width: `${selectedOrder.progressPutaway || 0}%` }} />
                </div>
                <span className="text-sm font-medium">{selectedOrder.progressPutaway || 0}%</span>
                <span className="text-sm text-gray-500">
                  ({selectedOrder.totalPutawayQty || 0}/{selectedOrder.totalQualifiedQty})
                </span>
              </div>
            </div>
          </div>

          {/* 商品明细 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold mb-3 text-gray-700">商品明细</h3>
            <DataTable columns={itemColumns} data={selectedOrder.items || []} />
          </div>

          {/* 当前入库单的上架记录 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold mb-3 text-gray-700 flex items-center gap-2">
              <History size={16} />
              本单上架记录 ({orderRecords.length}条)
            </h3>
            {orderRecords.length > 0 ? (
              <DataTable columns={orderRecordColumns} data={orderRecords} />
            ) : (
              <div className="text-center py-8 text-gray-400 text-sm">暂无上架记录</div>
            )}
          </div>
        </>
      )}

      {/* 最近上架记录 - 放在最下面 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <h3 className="text-sm font-semibold mb-3 text-gray-700 flex items-center gap-2">
          <History size={16} />
          最近上架记录
        </h3>
        {loadingRecords ? (
          <div className="text-center py-8 text-gray-400 text-sm">加载中...</div>
        ) : recentRecords.length > 0 ? (
          <DataTable columns={recordColumns} data={recentRecords} />
        ) : (
          <div className="text-center py-8 text-gray-400 text-sm">暂无上架记录</div>
        )}
      </div>

      {/* 上架弹窗 */}
      <Dialog open={putawayModalOpen} onOpenChange={setPutawayModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>商品上架</DialogTitle>
          </DialogHeader>
          {currentItem && (() => {
            const pendingQty = (currentItem.qualifiedQty || 0) - (currentItem.putawayQty || 0);
            return (
              <div className="space-y-3 py-3">
                <div className="bg-gray-50 rounded p-3 text-sm">
                  <div className="grid grid-cols-2 gap-2">
                    <div><span className="text-gray-500">SKU：</span>{currentItem.skuCode}</div>
                    <div><span className="text-gray-500">商品：</span>{currentItem.productName}</div>
                    <div><span className="text-gray-500">合格数量：</span>{currentItem.qualifiedQty}</div>
                    <div><span className="text-gray-500">已上架：</span>{currentItem.putawayQty || 0}</div>
                  </div>
                  <div className="mt-2 pt-2 border-t flex justify-between items-center">
                    <span className="text-gray-500 font-medium">待上架数量：</span>
                    <span className="font-bold text-orange-600 text-lg">{pendingQty}</span>
                  </div>
                  <div className="text-xs text-gray-400 mt-1">
                    批次号：{currentItem.batchNo || "待验收后生成"}
                  </div>
                </div>

                {/* 库区选择 */}
                <div>
                  <Label>选择库区</Label>
                  {loadingZones ? (
                    <div className="mt-1 text-sm text-gray-400">加载库区列表...</div>
                  ) : zones.length > 0 ? (
                    <select
                      className="w-full mt-1 px-3 py-2 border rounded text-sm"
                      value={selectedZoneId}
                      onChange={(e) => {
                        setSelectedZoneId(e.target.value ? Number(e.target.value) : "");
                        setSelectedLocationId("");
                      }}
                    >
                      <option value="">全部库区</option>
                      {zones.map((zone) => (
                        <option key={zone.id} value={zone.id}>
                          {zone.code} - {zone.name}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <div className="mt-1 text-sm text-gray-400">该仓库暂无库区</div>
                  )}
                </div>

                {/* 库位选择 */}
                <div>
                  <Label>选择库位 *</Label>
                  {loadingLocations ? (
                    <div className="mt-1 text-sm text-gray-400">加载库位列表...</div>
                  ) : filteredLocations.length > 0 ? (
                    <select
                      className="w-full mt-1 px-3 py-2 border rounded text-sm"
                      value={selectedLocationId}
                      onChange={(e) => setSelectedLocationId(e.target.value ? Number(e.target.value) : "")}
                    >
                      <option value="">请选择库位</option>
                      {filteredLocations.map((loc) => (
                        <option key={loc.id} value={loc.id}>
                          {loc.code} - {loc.name} ({loc.zoneName || "默认区域"})
                        </option>
                      ))}
                    </select>
                  ) : (
                    <div className="mt-1 text-sm text-red-500">
                      {selectedZoneId ? "该库区暂无库位" : "该仓库暂无库位，请先添加库位"}
                    </div>
                  )}
                </div>

                {/* 上架数量 */}
                <div>
                  <Label>上架数量 *</Label>
                  <Input
                    type="number"
                    min={1}
                    max={pendingQty}
                    value={putawayQty}
                    onChange={(e) => setPutawayQty(Number(e.target.value))}
                    className="mt-1"
                  />
                  {putawayQty > pendingQty && (
                    <p className="text-xs text-red-500 mt-1">上架数量不能超过待上架数量({pendingQty})</p>
                  )}
                </div>
              </div>
            );
          })()}
          <DialogFooter>
            <Button variant="outline" onClick={() => setPutawayModalOpen(false)}>取消</Button>
            <Button onClick={handlePutaway} disabled={loading || !selectedLocationId}>确认上架</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}