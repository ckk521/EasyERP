import { useState, useEffect } from "react";
import { Search, Package, Truck, Building2, CheckCircle, History, ChevronRight, AlertTriangle } from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Button } from "../../components/ui/button";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import DataTable from "../../components/DataTable";
import { toast } from "sonner";

// ==================== 类型定义 ====================

interface OutboundOrderItem {
  id: number;
  productId: number;
  skuCode: string;
  productName: string;
  barcode: string;
  qty: number;
  pickedQty: number;
  pendingQty?: number;
  status: number;
}

interface OutboundOrder {
  id: number;
  orderNo: string;
  orderTypeName: string;
  targetWarehouseName?: string;
  warehouseId: number;
  warehouseName: string;
  status: number;
  statusName: string;
  totalQty: number;
  totalPickedQty: number;
  progressPick: number;
  items: OutboundOrderItem[];
}

interface PickRecord {
  id: number;
  outboundOrderId: number;
  outboundOrderNo: string;
  skuCode: string;
  productName: string;
  planQty: number;
  actualQty: number;
  diffQty: number;
  diffReason: string;
  pickTime: string;
  pickUserName: string;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

// ==================== 常量定义 ====================

const STATUS_COLORS: Record<number, string> = {
  0: "bg-yellow-100 text-yellow-700", // 待分配
  1: "bg-blue-100 text-blue-700",     // 已分配
  2: "bg-blue-100 text-blue-700",     // 拣货中
  3: "bg-orange-100 text-orange-700", // 待打包
  4: "bg-purple-100 text-purple-700", // 待发货
  5: "bg-green-100 text-green-700",   // 已发货
  9: "bg-gray-100 text-gray-500",     // 已取消
};

const STATUS_NAMES: Record<number, string> = {
  0: "待分配",
  1: "已分配",
  2: "拣货中",
  3: "待打包",
  4: "待发货",
  5: "已发货",
  9: "已取消",
};

const ITEM_STATUS_COLORS: Record<number, string> = {
  0: "bg-yellow-100 text-yellow-700", // 待拣货
  1: "bg-blue-100 text-blue-700",     // 拣货中
  2: "bg-green-100 text-green-700",   // 已拣货
  9: "bg-gray-100 text-gray-500",     // 已取消
};

const ITEM_STATUS_NAMES: Record<number, string> = {
  0: "待拣货",
  1: "拣货中",
  2: "已拣货",
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

export default function PickPage() {
  // 出库单列表
  const [orders, setOrders] = useState<OutboundOrder[]>([]);
  const [loadingOrders, setLoadingOrders] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<OutboundOrder | null>(null);

  // 拣货记录
  const [pickRecords, setPickRecords] = useState<PickRecord[]>([]);

  // SKU搜索
  const [skuCode, setSkuCode] = useState("");
  const [loading, setLoading] = useState(false);

  // 拣货弹窗
  const [pickModalOpen, setPickModalOpen] = useState(false);
  const [currentItem, setCurrentItem] = useState<OutboundOrderItem | null>(null);
  const [pickQty, setPickQty] = useState(0);
  const [diffReason, setDiffReason] = useState("");

  // 加载待拣货的出库单列表（状态2=拣货中）
  const loadPendingOrders = async () => {
    setLoadingOrders(true);
    try {
      const data = await fetchApi<{ list: OutboundOrder[] }>("/api/v1/outbound/orders?status=2&limit=50");
      setOrders(data.list || []);
    } catch (error) {
      console.error("Failed to load orders:", error);
      setOrders([]);
    } finally {
      setLoadingOrders(false);
    }
  };

  // 初始化加载
  useEffect(() => {
    loadPendingOrders();
  }, []);

  // 选择出库单
  const handleSelectOrder = async (order: OutboundOrder) => {
    try {
      const detail = await fetchApi<OutboundOrder>(`/api/v1/outbound/orders/${order.id}`);
      setSelectedOrder(detail);
      // 加载拣货记录
      loadPickRecords(order.id);
    } catch (error) {
      toast.error("加载出库单详情失败");
    }
  };

  // 加载拣货记录
  const loadPickRecords = async (orderId: number) => {
    try {
      // 从拣货记录API获取
      const taskData = await fetchApi<{ pickItems: PickRecord[] }>(`/api/v1/outbound/pick/records/${orderId}`);
      // 转换为记录格式
      const records: PickRecord[] = (taskData.pickItems || [])
        .filter((item: { actualQty?: number }) => item.actualQty !== null && item.actualQty !== undefined)
        .map((item: { id: number; outboundOrderId: number; outboundOrderNo: string; skuCode: string; productName: string; planQty: number; actualQty: number; diffQty?: number; diffReason?: string; completeTime?: string; pickUserName?: string }) => ({
          id: item.id,
          outboundOrderId: item.outboundOrderId,
          outboundOrderNo: item.outboundOrderNo,
          skuCode: item.skuCode,
          productName: item.productName,
          planQty: item.planQty,
          actualQty: item.actualQty,
          diffQty: item.diffQty || 0,
          diffReason: item.diffReason || "",
          pickTime: item.completeTime || "",
          pickUserName: item.pickUserName || "系统",
        }));
      setPickRecords(records);
    } catch (error) {
      console.error("Failed to load pick records:", error);
      setPickRecords([]);
    }
  };

  // 扫描/搜索商品
  const handleScanItem = () => {
    if (!selectedOrder) {
      toast.error("请先选择出库单");
      return;
    }
    if (!skuCode.trim()) {
      toast.error("请输入SKU编码");
      return;
    }
    const item = selectedOrder.items?.find(i => i.skuCode === skuCode || i.barcode === skuCode);
    if (item) {
      if (item.pickedQty >= item.qty) {
        toast.info("该商品已拣货完成");
        return;
      }
      openPickModal(item);
    } else {
      toast.error("未找到该商品");
    }
  };

  // 打开拣货弹窗
  const openPickModal = (item: OutboundOrderItem) => {
    setCurrentItem(item);
    const pendingQty = item.qty - (item.pickedQty || 0);
    setPickQty(pendingQty);
    setDiffReason("");
    setPickModalOpen(true);
  };

  // 执行拣货
  const handlePick = async () => {
    if (!selectedOrder || !currentItem) return;

    const pendingQty = currentItem.qty - (currentItem.pickedQty || 0);
    const diff = pickQty - pendingQty;

    if (diff !== 0 && !diffReason) {
      toast.error("拣货数量有差异，请填写差异原因");
      return;
    }

    setLoading(true);
    try {
      // 获取拣货记录ID - 查找未完成的记录（status 0=待拣货 或 1=拣货中）
      const taskData = await fetchApi<{ pickItems: { recordId: number; skuCode: string; status: number; locationScanned: boolean; productScanned: boolean }[] }>(`/api/v1/outbound/pick/records/${selectedOrder.id}`);

      // 找到对应SKU且未完成的记录（status不是2已完成和9已取消）
      const pickItem = taskData.pickItems?.find((i: { skuCode: string; status: number }) =>
        i.skuCode === currentItem.skuCode && i.status !== 2 && i.status !== 9
      );

      if (!pickItem) {
        toast.error("未找到对应的拣货记录");
        setLoading(false);
        return;
      }

      // 检查是否需要扫码（简化流程：如果未扫码则自动扫码）
      if (!pickItem.locationScanned) {
        await fetchApi("/api/v1/outbound/pick/scan-location", {
          method: "POST",
          body: JSON.stringify({
            recordId: pickItem.recordId,
            locationCode: "ZN-A-01-01-01", // 默认库位
          }),
        });
      }

      if (!pickItem.productScanned) {
        await fetchApi("/api/v1/outbound/pick/scan-product", {
          method: "POST",
          body: JSON.stringify({
            recordId: pickItem.recordId,
            barcode: currentItem.barcode || currentItem.skuCode,
          }),
        });
      }

      // 确认拣货
      await fetchApi("/api/v1/outbound/pick/confirm", {
        method: "POST",
        body: JSON.stringify({
          recordId: pickItem.recordId,
          locationId: 1,
          locationCode: "ZN-A-01-01-01",
          barcode: currentItem.barcode || currentItem.skuCode,
          actualQty: pickQty,
          isException: false,
          diffReason: diff !== 0 ? diffReason : null,
        }),
      });

      toast.success("拣货成功");
      setPickModalOpen(false);
      setSkuCode("");

      // 刷新数据
      const detail = await fetchApi<OutboundOrder>(`/api/v1/outbound/orders/${selectedOrder.id}`);
      setSelectedOrder(detail);
      loadPickRecords(selectedOrder.id);
      loadPendingOrders();
    } catch (error) {
      const message = error instanceof Error ? error.message : "拣货失败";
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  // 完成订单拣货
  const handleCompleteOrderPick = async () => {
    if (!selectedOrder) return;

    // 检查是否所有商品都已拣货
    const allPicked = selectedOrder.items?.every(item => item.pickedQty >= item.qty);
    if (!allPicked) {
      toast.error("还有商品未完成拣货");
      return;
    }

    setLoading(true);
    try {
      await fetchApi("/api/v1/outbound/pick/complete-order", {
        method: "POST",
        body: JSON.stringify({ orderId: selectedOrder.id }),
      });
      toast.success("订单拣货完成，已进入待打包状态");
      setSelectedOrder(null);
      setPickRecords([]);
      loadPendingOrders();
    } catch (error) {
      const message = error instanceof Error ? error.message : "完成拣货失败";
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  // 出库单列表列定义
  const orderColumns = [
    { key: "orderNo", title: "出库单号", width: "120px", render: (v: string) => <span className="font-mono">{v}</span> },
    { key: "warehouseName", title: "仓库", width: "100px", render: (v: string) => v || "-" },
    {
      key: "targetWarehouse",
      title: "目标仓库",
      width: "100px",
      render: (_: unknown, row: OutboundOrder) => row.orderTypeName === "调拨出库" ? (
        <span className="text-blue-600">{row.targetWarehouseName || "-"}</span>
      ) : "-",
    },
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
    { key: "totalQty", title: "总数量", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    {
      key: "progress",
      title: "拣货进度",
      width: "100px",
      render: (_: unknown, row: OutboundOrder) => (
        <div className="flex items-center gap-2">
          <div className="flex-1 bg-gray-200 rounded-full h-1.5">
            <div className="bg-blue-600 h-1.5 rounded-full" style={{ width: `${row.progressPick || 0}%` }} />
          </div>
          <span className="text-xs text-gray-500">{row.progressPick || 0}%</span>
        </div>
      ),
    },
    {
      key: "action",
      title: "操作",
      width: "60px",
      render: (_: unknown, row: OutboundOrder) => (
        <button
          onClick={() => handleSelectOrder(row)}
          className="text-blue-600 hover:text-blue-700 flex items-center gap-1"
        >
          拣货 <ChevronRight size={14} />
        </button>
      ),
    },
  ];

  // 商品明细列定义
  const itemColumns = [
    { key: "skuCode", title: "SKU编码", width: "120px" },
    { key: "productName", title: "商品名称", width: "150px" },
    { key: "barcode", title: "条码", width: "100px", render: (v: string) => v || "-" },
    { key: "qty", title: "出库数量", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "pickedQty", title: "已拣货", width: "80px", render: (v: number) => <span className="font-medium text-green-600">{v || 0}</span> },
    {
      key: "pendingQty",
      title: "待拣货",
      width: "80px",
      render: (_: unknown, item: OutboundOrderItem) => {
        const pending = item.qty - (item.pickedQty || 0);
        return <span className="font-medium text-blue-600">{pending}</span>;
      },
    },
    {
      key: "status",
      title: "状态",
      width: "80px",
      render: (_: unknown, item: OutboundOrderItem) => {
        const pending = item.qty - (item.pickedQty || 0);
        const isComplete = pending <= 0;
        return (
          <span className={`px-2 py-0.5 rounded text-xs ${isComplete ? "bg-green-100 text-green-700" : "bg-yellow-100 text-yellow-700"}`}>
            {isComplete ? "已拣货" : "待拣货"}
          </span>
        );
      },
    },
    {
      key: "action",
      title: "操作",
      width: "80px",
      render: (_: unknown, item: OutboundOrderItem) => {
        const pending = item.qty - (item.pickedQty || 0);
        return (
          <button
            onClick={() => openPickModal(item)}
            disabled={pending <= 0}
            className="text-blue-600 hover:text-blue-700 disabled:text-gray-400"
          >
            拣货
          </button>
        );
      },
    },
  ];

  // 拣货记录列定义
  const recordColumns = [
    { key: "pickTime", title: "拣货时间", width: "150px", render: (v: string) => v?.replace("T", " ").slice(0, 16) || "-" },
    { key: "skuCode", title: "SKU编码", width: "100px" },
    { key: "productName", title: "商品名称", width: "120px" },
    { key: "planQty", title: "计划数量", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "actualQty", title: "实际数量", width: "80px", render: (v: number) => <span className="font-medium">{v}</span> },
    {
      key: "diffQty",
      title: "差异",
      width: "60px",
      render: (v: number) => v === 0 ? "-" : <span className={v > 0 ? "text-green-600" : "text-red-600"}>{v > 0 ? "+" : ""}{v}</span>,
    },
    { key: "diffReason", title: "差异原因", width: "120px", render: (v: string) => v || "-" },
  ];

  // 计算拣货进度
  const allPicked = selectedOrder?.items?.every(item => item.pickedQty >= item.qty);
  const totalPicked = selectedOrder?.items?.reduce((sum, item) => sum + (item.pickedQty || 0), 0) || 0;
  const totalQty = selectedOrder?.totalQty || 0;

  return (
    <div className="space-y-4">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">拣货作业</h2>
        <button
          onClick={loadPendingOrders}
          className="text-sm text-blue-600 hover:text-blue-700"
        >
          刷新
        </button>
      </div>

      {/* 出库单列表 */}
      {!selectedOrder && (
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3 text-gray-700">待拣货出库单</h3>
          {loadingOrders ? (
            <div className="text-center py-8 text-gray-400 text-sm">加载中...</div>
          ) : orders.length > 0 ? (
            <DataTable columns={orderColumns} data={orders} />
          ) : (
            <div className="text-center py-8 text-gray-400 text-sm">暂无待拣货出库单</div>
          )}
        </div>
      )}

      {/* 选中的出库单详情 */}
      {selectedOrder && (
        <>
          {/* SKU搜索区 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <div className="flex gap-2">
              <div className="flex-1 relative">
                <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                <input
                  type="text"
                  placeholder="输入SKU编码或条码快速拣货"
                  className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={skuCode}
                  onChange={(e) => setSkuCode(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && handleScanItem()}
                />
              </div>
              <button
                onClick={handleScanItem}
                disabled={loading}
                className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50"
              >
                搜索商品
              </button>
              <button
                onClick={() => { setSelectedOrder(null); setPickRecords([]); }}
                className="px-4 py-2 bg-gray-100 text-gray-600 rounded hover:bg-gray-200"
              >
                返回列表
              </button>
            </div>
          </div>

          {/* 出库单信息 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold mb-3 text-gray-700">出库单信息</h3>
            <div className="grid grid-cols-4 gap-4">
              <div className="flex items-center gap-2">
                <Package size={16} className="text-gray-400" />
                <span className="text-sm text-gray-500">出库单号：</span>
                <span className="text-sm font-medium font-mono">{selectedOrder.orderNo}</span>
              </div>
              <div className="flex items-center gap-2">
                <Building2 size={16} className="text-gray-400" />
                <span className="text-sm text-gray-500">仓库：</span>
                <span className="text-sm font-medium">{selectedOrder.warehouseName}</span>
              </div>
              <div className="flex items-center gap-2">
                <Truck size={16} className="text-gray-400" />
                <span className="text-sm text-gray-500">类型：</span>
                <span className="text-sm font-medium">{selectedOrder.orderTypeName}</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle size={16} className="text-gray-400" />
                <span className="text-sm text-gray-500">状态：</span>
                <span className={`px-2 py-0.5 rounded text-xs ${STATUS_COLORS[selectedOrder.status] || "bg-gray-100"}`}>
                  {selectedOrder.statusName || STATUS_NAMES[selectedOrder.status]}
                </span>
              </div>
            </div>
            {selectedOrder.orderTypeName === "调拨出库" && selectedOrder.targetWarehouseName && (
              <div className="mt-2 pt-2 border-t">
                <span className="text-sm text-gray-500">目标仓库：</span>
                <span className="text-sm font-medium text-blue-600">{selectedOrder.targetWarehouseName}</span>
              </div>
            )}
            <div className="mt-3 pt-3 border-t">
              <div className="flex items-center gap-4">
                <span className="text-sm text-gray-500">拣货进度：</span>
                <div className="flex-1 bg-gray-200 rounded-full h-2">
                  <div className="bg-blue-600 h-2 rounded-full transition-all" style={{ width: `${selectedOrder.progressPick || 0}%` }} />
                </div>
                <span className="text-sm font-medium">{selectedOrder.progressPick || 0}%</span>
                <span className="text-sm text-gray-500">
                  ({totalPicked}/{totalQty})
                </span>
              </div>
            </div>
          </div>

          {/* 商品明细 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold mb-3 text-gray-700">商品明细</h3>
            <DataTable columns={itemColumns} data={selectedOrder.items || []} />
          </div>

          {/* 拣货记录 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold mb-3 text-gray-700 flex items-center gap-2">
              <History size={16} />
              拣货记录 ({pickRecords.length}条)
            </h3>
            {pickRecords.length > 0 ? (
              <DataTable columns={recordColumns} data={pickRecords} />
            ) : (
              <div className="text-center py-8 text-gray-400 text-sm">暂无拣货记录</div>
            )}
          </div>

          {/* 完成拣货按钮 */}
          {allPicked && (
            <div className="bg-white rounded-lg p-4 border border-gray-200">
              <div className="flex items-center justify-between">
                <div className="text-sm text-green-600 font-medium">
                  ✓ 所有商品拣货完成，可提交订单
                </div>
                <button
                  onClick={handleCompleteOrderPick}
                  disabled={loading}
                  className="px-6 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50"
                >
                  完成拣货
                </button>
              </div>
            </div>
          )}
        </>
      )}

      {/* 拣货弹窗 */}
      <Dialog open={pickModalOpen} onOpenChange={setPickModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>拣货确认</DialogTitle>
          </DialogHeader>
          {currentItem && (() => {
            const pendingQty = currentItem.qty - (currentItem.pickedQty || 0);
            const diff = pickQty - pendingQty;
            const hasDiff = diff !== 0;
            return (
              <div className="space-y-3 py-3">
                <div className="bg-gray-50 rounded p-3 text-sm">
                  <div className="grid grid-cols-2 gap-2">
                    <div><span className="text-gray-500">SKU：</span><span className="font-mono">{currentItem.skuCode}</span></div>
                    <div><span className="text-gray-500">商品：</span>{currentItem.productName}</div>
                    <div><span className="text-gray-500">出库数量：</span>{currentItem.qty}</div>
                    <div><span className="text-gray-500">已拣货：</span>{currentItem.pickedQty || 0}</div>
                  </div>
                  <div className="mt-2 pt-2 border-t flex justify-between">
                    <span className="text-gray-500 font-medium">待拣货数量：</span>
                    <span className="font-bold text-blue-600 text-lg">{pendingQty}</span>
                  </div>
                </div>
                <div>
                  <Label>实际拣货数量 *</Label>
                  <Input
                    type="number"
                    min={0}
                    max={pendingQty}
                    value={pickQty}
                    onChange={(e) => setPickQty(Number(e.target.value))}
                    className="mt-1"
                  />
                  {hasDiff && (
                    <p className={`text-xs mt-1 ${diff > 0 ? "text-green-600" : "text-orange-600"}`}>
                      {diff > 0 ? `多拣 ${diff} 件` : `短缺 ${Math.abs(diff)} 件`}
                    </p>
                  )}
                </div>
                {hasDiff && (
                  <div className="bg-orange-50 rounded p-3 text-sm border border-orange-200">
                    <div className="flex items-center gap-2 text-orange-700 font-medium mb-1">
                      <AlertTriangle className="h-4 w-4" />
                      拣货数量有差异，请填写原因
                    </div>
                  </div>
                )}
                <div>
                  <Label>差异原因 {hasDiff && "*"}</Label>
                  <select
                    className={`w-full mt-1 px-3 py-2 border rounded text-sm ${!hasDiff ? "bg-gray-100 text-gray-400 cursor-not-allowed" : ""}`}
                    value={diffReason}
                    onChange={(e) => setDiffReason(e.target.value)}
                    disabled={!hasDiff}
                  >
                    <option value="">{hasDiff ? "选择差异原因" : "数量一致，无需填写"}</option>
                    <option value="库存不足">库存不足</option>
                    <option value="商品破损">商品破损</option>
                    <option value="找不到商品">找不到商品</option>
                    <option value="其他">其他</option>
                  </select>
                </div>
              </div>
            );
          })()}
          <DialogFooter>
            <Button variant="outline" onClick={() => setPickModalOpen(false)}>取消</Button>
            <Button
              onClick={handlePick}
              disabled={loading || (currentItem && (() => {
                const pendingQty = currentItem.qty - (currentItem.pickedQty || 0);
                const diff = pickQty - pendingQty;
                return diff !== 0 && !diffReason;
              })())}
            >
              确认拣货
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}