import { useState, useEffect } from "react";
import { Search, Package, Truck, Building2, CheckCircle, History, ChevronRight } from "lucide-react";
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
  expectedQty: number;
  receivedQty: number;
  status: number;
}

interface InboundOrder {
  id: number;
  orderNo: string;
  deliveryBatchNo: string;
  orderTypeName: string;
  supplierName: string;
  warehouseName: string;
  status: number;
  statusName: string;
  totalExpectedQty: number;
  totalReceivedQty: number;
  progressReceive: number;
  items: InboundOrderItem[];
}

interface ReceiveRecord {
  id: number;
  inboundOrderId: number;
  inboundOrderNo: string;
  inboundItemId: number;
  productId: number;
  skuCode: string;
  productName: string;
  receiveQty: number;
  diffQty: number;
  diffReason: string;
  receiveTime: string;
  receiveUser: number;
  receiveUserName: string;
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

export default function ReceivePage() {
  // 入库单列表
  const [orders, setOrders] = useState<InboundOrder[]>([]);
  const [loadingOrders, setLoadingOrders] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<InboundOrder | null>(null);

  // 收货记录（全局）
  const [recentRecords, setRecentRecords] = useState<ReceiveRecord[]>([]);
  const [loadingRecords, setLoadingRecords] = useState(false);

  // 当前入库单的收货记录
  const [orderRecords, setOrderRecords] = useState<ReceiveRecord[]>([]);

  // SKU扫描
  const [skuCode, setSkuCode] = useState("");
  const [loading, setLoading] = useState(false);

  // 收货弹窗
  const [receiveModalOpen, setReceiveModalOpen] = useState(false);
  const [currentItem, setCurrentItem] = useState<InboundOrderItem | null>(null);
  const [receiveQty, setReceiveQty] = useState(0);
  const [diffReason, setDiffReason] = useState("");

  // 差异确认弹窗
  const [diffModalOpen, setDiffModalOpen] = useState(false);

  // 加载待收货的入库单列表
  const loadPendingOrders = async () => {
    setLoadingOrders(true);
    try {
      // 状态 0=待收货, 1=收货中
      const data = await fetchApi<{ list: InboundOrder[] }>("/api/v1/inbound/orders?status=0,1&limit=50");
      setOrders(data.list || []);
    } catch (error) {
      console.error("Failed to load orders:", error);
      setOrders([]);
    } finally {
      setLoadingOrders(false);
    }
  };

  // 加载最近的收货记录
  const loadRecentRecords = async () => {
    setLoadingRecords(true);
    try {
      const data = await fetchApi<{ list: ReceiveRecord[] }>("/api/v1/inbound/receive/records/recent?limit=20");
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
      // 加载该入库单的收货记录
      const records = await fetchApi<ReceiveRecord[]>(`/api/v1/inbound/receive/records/order/${order.id}`);
      setOrderRecords(records || []);
    } catch (error) {
      toast.error("加载入库单详情失败");
    }
  };

  // 扫描商品
  const handleScanItem = () => {
    if (!selectedOrder) {
      toast.error("请先选择入库单");
      return;
    }
    if (!skuCode.trim()) {
      toast.error("请输入SKU编码");
      return;
    }
    const item = selectedOrder.items?.find(i => i.skuCode === skuCode);
    if (item) {
      openReceiveModal(item);
    } else {
      toast.error("未找到该商品");
    }
  };

  // 打开收货弹窗
  const openReceiveModal = (item: InboundOrderItem) => {
    setCurrentItem(item);
    const pendingQty = item.expectedQty - (item.receivedQty || 0);
    setReceiveQty(pendingQty);
    setDiffReason("");
    setReceiveModalOpen(true);
  };

  // 执行收货
  const handleReceive = async () => {
    if (!selectedOrder || !currentItem) return;

    const pendingQty = currentItem.expectedQty - (currentItem.receivedQty || 0);
    const diff = receiveQty - pendingQty;
    const hasDiff = diff !== 0;
    const diffPercent = pendingQty > 0 ? Math.abs(diff) / pendingQty : 0;

    if (hasDiff && !diffReason) {
      toast.error("收货数量有差异，请填写差异原因");
      return;
    }

    if (diffPercent > 0.1 && !diffModalOpen) {
      setDiffModalOpen(true);
      return;
    }

    await executeReceive();
  };

  // 执行收货请求
  const executeReceive = async () => {
    if (!selectedOrder || !currentItem) return;
    setLoading(true);
    try {
      await fetchApi("/api/v1/inbound/receive/execute", {
        method: "POST",
        body: JSON.stringify({
          orderId: selectedOrder.id,
          itemId: currentItem.id,
          receivedQty: receiveQty,
          diffReason: diffReason,
        }),
      });
      toast.success("收货成功");
      setReceiveModalOpen(false);
      setDiffModalOpen(false);
      setSkuCode("");

      // 刷新数据
      const detail = await fetchApi<InboundOrder>(`/api/v1/inbound/orders/${selectedOrder.id}`);
      setSelectedOrder(detail);
      const records = await fetchApi<ReceiveRecord[]>(`/api/v1/inbound/receive/records/order/${selectedOrder.id}`);
      setOrderRecords(records || []);

      // 刷新列表和全局记录
      loadPendingOrders();
      loadRecentRecords();
    } catch (error) {
      const message = error instanceof Error ? error.message : "收货失败";
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
    { key: "totalExpectedQty", title: "预期数量", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    {
      key: "progress",
      title: "进度",
      width: "100px",
      render: (_: unknown, row: InboundOrder) => (
        <div className="flex items-center gap-2">
          <div className="flex-1 bg-gray-200 rounded-full h-1.5">
            <div className="bg-blue-600 h-1.5 rounded-full" style={{ width: `${row.progressReceive || 0}%` }} />
          </div>
          <span className="text-xs text-gray-500">{row.progressReceive || 0}%</span>
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
          收货 <ChevronRight size={14} />
        </button>
      ),
    },
  ];

  // 商品明细列定义
  const itemColumns = [
    { key: "skuCode", title: "SKU编码", width: "120px" },
    { key: "productName", title: "商品名称", width: "150px" },
    { key: "expectedQty", title: "预期数量", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "receivedQty", title: "已收货", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    {
      key: "pendingQty",
      title: "待收货",
      width: "80px",
      render: (_: unknown, item: InboundOrderItem) => {
        const pending = item.expectedQty - (item.receivedQty || 0);
        return <span className="font-medium text-blue-600">{pending}</span>;
      },
    },
    {
      key: "status",
      title: "状态",
      width: "80px",
      render: (_: unknown, item: InboundOrderItem) => {
        const pending = item.expectedQty - (item.receivedQty || 0);
        const isComplete = pending <= 0;
        return (
          <span className={`px-2 py-0.5 rounded text-xs ${isComplete ? "bg-green-100 text-green-700" : "bg-yellow-100 text-yellow-700"}`}>
            {isComplete ? "已收货" : "待收货"}
          </span>
        );
      },
    },
    {
      key: "action",
      title: "操作",
      width: "80px",
      render: (_: unknown, item: InboundOrderItem) => {
        const pending = item.expectedQty - (item.receivedQty || 0);
        return (
          <button
            onClick={() => openReceiveModal(item)}
            disabled={pending <= 0}
            className="text-blue-600 hover:text-blue-700 disabled:text-gray-400"
          >
            收货
          </button>
        );
      },
    },
  ];

  // 收货记录列定义
  const recordColumns = [
    { key: "receiveTime", title: "收货时间", width: "150px", render: (v: string) => v?.replace("T", " ").slice(0, 16) },
    { key: "inboundOrderNo", title: "入库单号", width: "100px", render: (v: string) => <span className="font-mono">{v}</span> },
    { key: "skuCode", title: "SKU编码", width: "100px" },
    { key: "productName", title: "商品名称", width: "120px" },
    { key: "receiveQty", title: "收货数量", width: "80px", render: (v: number) => <span className="font-medium">{v}</span> },
    {
      key: "diffQty",
      title: "差异",
      width: "60px",
      render: (v: number) => v === 0 ? "-" : <span className={v > 0 ? "text-green-600" : "text-red-600"}>{v > 0 ? "+" : ""}{v}</span>,
    },
    { key: "receiveUserName", title: "操作人", width: "80px" },
  ];

  // 当前入库单的收货记录列定义
  const orderRecordColumns = [
    { key: "receiveTime", title: "收货时间", width: "150px", render: (v: string) => v?.replace("T", " ").slice(0, 16) },
    { key: "skuCode", title: "SKU编码", width: "100px" },
    { key: "productName", title: "商品名称", width: "120px" },
    { key: "receiveQty", title: "收货数量", width: "80px", render: (v: number) => <span className="font-medium">{v}</span> },
    {
      key: "diffQty",
      title: "差异",
      width: "60px",
      render: (v: number) => v === 0 ? "-" : <span className={v > 0 ? "text-green-600" : "text-red-600"}>{v > 0 ? "+" : ""}{v}</span>,
    },
    { key: "diffReason", title: "差异原因", width: "120px", render: (v: string) => v || "-" },
    { key: "receiveUserName", title: "操作人", width: "80px" },
  ];

  return (
    <div className="space-y-4">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">收货作业</h2>
        <button
          onClick={() => { loadPendingOrders(); loadRecentRecords(); }}
          className="text-sm text-blue-600 hover:text-blue-700"
        >
          刷新
        </button>
      </div>

      {/* 入库单列表 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <h3 className="text-sm font-semibold mb-3 text-gray-700">待收货入库单</h3>
        {loadingOrders ? (
          <div className="text-center py-8 text-gray-400 text-sm">加载中...</div>
        ) : orders.length > 0 ? (
          <DataTable columns={orderColumns} data={orders} />
        ) : (
          <div className="text-center py-8 text-gray-400 text-sm">暂无待收货入库单</div>
        )}
      </div>

      {/* 选中的入库单详情 */}
      {selectedOrder && (
        <>
          {/* SKU扫描区 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <div className="flex gap-2">
              <div className="flex-1 relative">
                <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                <input
                  type="text"
                  placeholder="扫描或输入SKU编码快速收货"
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
                扫描商品
              </button>
              <button
                onClick={() => setSelectedOrder(null)}
                className="px-4 py-2 bg-gray-100 text-gray-600 rounded hover:bg-gray-200"
              >
                返回列表
              </button>
            </div>
          </div>

          {/* 入库单信息 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold mb-3 text-gray-700">入库单信息</h3>
            <div className="grid grid-cols-4 gap-4">
              <div className="flex items-center gap-2">
                <Package size={16} className="text-gray-400" />
                <span className="text-sm text-gray-500">入库单号：</span>
                <span className="text-sm font-medium font-mono">{selectedOrder.orderNo}</span>
              </div>
              <div className="flex items-center gap-2">
                <Truck size={16} className="text-gray-400" />
                <span className="text-sm text-gray-500">送货批次：</span>
                <span className="text-sm font-medium">{selectedOrder.deliveryBatchNo || "-"}</span>
              </div>
              <div className="flex items-center gap-2">
                <Building2 size={16} className="text-gray-400" />
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
                <span className="text-sm text-gray-500">收货进度：</span>
                <div className="flex-1 bg-gray-200 rounded-full h-2">
                  <div className="bg-blue-600 h-2 rounded-full transition-all" style={{ width: `${selectedOrder.progressReceive || 0}%` }} />
                </div>
                <span className="text-sm font-medium">{selectedOrder.progressReceive || 0}%</span>
                <span className="text-sm text-gray-500">
                  ({selectedOrder.totalReceivedQty || 0}/{selectedOrder.totalExpectedQty})
                </span>
              </div>
            </div>
          </div>

          {/* 商品明细 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold mb-3 text-gray-700">商品明细</h3>
            <DataTable columns={itemColumns} data={selectedOrder.items || []} />
          </div>

          {/* 当前入库单的收货记录 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold mb-3 text-gray-700 flex items-center gap-2">
              <History size={16} />
              本单收货记录 ({orderRecords.length}条)
            </h3>
            {orderRecords.length > 0 ? (
              <DataTable columns={orderRecordColumns} data={orderRecords} />
            ) : (
              <div className="text-center py-8 text-gray-400 text-sm">暂无收货记录</div>
            )}
          </div>
        </>
      )}

      {/* 最近收货记录 - 放在最下面 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <h3 className="text-sm font-semibold mb-3 text-gray-700 flex items-center gap-2">
          <History size={16} />
          最近收货记录
        </h3>
        {loadingRecords ? (
          <div className="text-center py-8 text-gray-400 text-sm">加载中...</div>
        ) : recentRecords.length > 0 ? (
          <DataTable columns={recordColumns} data={recentRecords} />
        ) : (
          <div className="text-center py-8 text-gray-400 text-sm">暂无收货记录</div>
        )}
      </div>

      {/* 收货弹窗 */}
      <Dialog open={receiveModalOpen} onOpenChange={setReceiveModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>收货确认</DialogTitle>
          </DialogHeader>
          {currentItem && (() => {
            const pendingQty = currentItem.expectedQty - (currentItem.receivedQty || 0);
            const diff = receiveQty - pendingQty;
            const hasDiff = diff !== 0;
            return (
              <div className="space-y-3 py-3">
                <div className="bg-gray-50 rounded p-3 text-sm">
                  <div className="grid grid-cols-2 gap-2">
                    <div><span className="text-gray-500">SKU：</span><span className="font-mono">{currentItem.skuCode}</span></div>
                    <div><span className="text-gray-500">商品：</span>{currentItem.productName}</div>
                    <div><span className="text-gray-500">预期：</span>{currentItem.expectedQty}</div>
                    <div><span className="text-gray-500">已收：</span>{currentItem.receivedQty || 0}</div>
                  </div>
                  <div className="mt-2 pt-2 border-t flex justify-between">
                    <span className="text-gray-500 font-medium">待收货数量：</span>
                    <span className="font-bold text-blue-600 text-lg">{pendingQty}</span>
                  </div>
                </div>
                <div>
                  <Label>实际收货数量 *</Label>
                  <Input
                    type="number"
                    min={1}
                    max={pendingQty * 2}
                    value={receiveQty}
                    onChange={(e) => setReceiveQty(Number(e.target.value))}
                    className="mt-1"
                  />
                  {hasDiff && (
                    <p className={`text-xs mt-1 ${diff > 0 ? "text-green-600" : "text-red-600"}`}>
                      差异：{diff > 0 ? "+" : ""}{diff} 件
                    </p>
                  )}
                </div>
                <div>
                  <Label>差异原因 {hasDiff && "*"}</Label>
                  <select
                    className={`w-full mt-1 px-3 py-2 border rounded text-sm ${!hasDiff ? "bg-gray-100 text-gray-400 cursor-not-allowed" : ""}`}
                    value={diffReason}
                    onChange={(e) => setDiffReason(e.target.value)}
                    disabled={!hasDiff}
                  >
                    <option value="">{hasDiff ? "选择差异原因" : "数量一致，无需填写"}</option>
                    <option value="供应商少发货">供应商少发货</option>
                    <option value="供应商多发货">供应商多发货</option>
                    <option value="运输损耗">运输损耗</option>
                    <option value="包装破损">包装破损</option>
                    <option value="其他">其他</option>
                  </select>
                </div>
              </div>
            );
          })()}
          <DialogFooter>
            <Button variant="outline" onClick={() => setReceiveModalOpen(false)}>取消</Button>
            <Button onClick={handleReceive} disabled={loading}>确认收货</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 差异确认弹窗 */}
      <Dialog open={diffModalOpen} onOpenChange={setDiffModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>差异确认</DialogTitle>
          </DialogHeader>
          {currentItem && (() => {
            const pendingQty = currentItem.expectedQty - (currentItem.receivedQty || 0);
            const diff = receiveQty - pendingQty;
            const diffPercent = pendingQty > 0 ? Math.abs(diff) / pendingQty * 100 : 0;
            return (
              <div className="py-3">
                <p className="text-sm text-gray-600 mb-2">
                  收货数量与待收货数量差异超过10%，请确认。
                </p>
                <div className="bg-yellow-50 p-3 rounded text-sm mb-3">
                  <div className="flex justify-between"><span>待收货数量：</span><span>{pendingQty}</span></div>
                  <div className="flex justify-between"><span>本次收货：</span><span>{receiveQty}</span></div>
                  <div className="flex justify-between font-medium"><span>差异：</span><span className={diff > 0 ? "text-green-600" : "text-red-600"}>{diff > 0 ? "+" : ""}{diff} ({diffPercent.toFixed(1)}%)</span></div>
                </div>
                <div>
                  <Label>差异原因 *</Label>
                  <textarea
                    className="w-full mt-1 px-3 py-2 border rounded text-sm"
                    rows={2}
                    value={diffReason}
                    onChange={(e) => setDiffReason(e.target.value)}
                    placeholder="请详细描述差异原因"
                  />
                </div>
              </div>
            );
          })()}
          <DialogFooter>
            <Button variant="outline" onClick={() => setDiffModalOpen(false)}>取消</Button>
            <Button onClick={executeReceive} disabled={loading || !diffReason}>确认提交</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}