import { useState, useEffect } from "react";
import { Search, Package, CheckCircle, History, ChevronRight } from "lucide-react";
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
  qualifiedQty: number;
  rejectedQty: number;
  status: number;
}

interface InboundOrder {
  id: number;
  orderNo: string;
  supplierName: string;
  warehouseName: string;
  status: number;
  statusName: string;
  totalReceivedQty: number;
  totalQualifiedQty: number;
  totalRejectedQty: number;
  progressInspect: number;
  items: InboundOrderItem[];
}

interface InspectRecord {
  id: number;
  inboundOrderId: number;
  inboundOrderNo: string;
  inboundItemId: number;
  productId: number;
  skuCode: string;
  productName: string;
  qualifiedQty: number;
  rejectedQty: number;
  rejectReason: string;
  inspectTime: string;
  inspectUser: number;
  inspectUserName: string;
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

export default function InspectPage() {
  // 入库单列表
  const [orders, setOrders] = useState<InboundOrder[]>([]);
  const [loadingOrders, setLoadingOrders] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<InboundOrder | null>(null);

  // 验收记录（全局）
  const [recentRecords, setRecentRecords] = useState<InspectRecord[]>([]);
  const [loadingRecords, setLoadingRecords] = useState(false);

  // 当前入库单的验收记录
  const [orderRecords, setOrderRecords] = useState<InspectRecord[]>([]);

  // 操作状态
  const [loading, setLoading] = useState(false);

  // 验收弹窗
  const [inspectModalOpen, setInspectModalOpen] = useState(false);
  const [currentItem, setCurrentItem] = useState<InboundOrderItem | null>(null);
  const [qualifiedQty, setQualifiedQty] = useState(0);
  const [rejectedQty, setRejectedQty] = useState(0);
  const [rejectReason, setRejectReason] = useState("");
  const [inspectMode, setInspectMode] = useState<"full" | "partial">("full"); // 验收模式：全部/部分

  // 加载待验收的入库单列表
  const loadPendingOrders = async () => {
    setLoadingOrders(true);
    try {
      // 状态 1=收货中, 2=验收中（收货完成后进入验收）
      const data = await fetchApi<{ list: InboundOrder[] }>("/api/v1/inbound/orders?status=1,2&limit=50");
      setOrders(data.list || []);
    } catch (error) {
      console.error("Failed to load orders:", error);
      setOrders([]);
    } finally {
      setLoadingOrders(false);
    }
  };

  // 加载最近的验收记录
  const loadRecentRecords = async () => {
    setLoadingRecords(true);
    try {
      const data = await fetchApi<{ list: InspectRecord[] }>("/api/v1/inbound/inspect/records/recent?limit=20");
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
      // 加载该入库单的验收记录
      const records = await fetchApi<InspectRecord[]>(`/api/v1/inbound/inspect/records/order/${order.id}`);
      setOrderRecords(records || []);
    } catch (error) {
      toast.error("加载入库单详情失败");
    }
  };

  // 打开验收弹窗
  const openInspectModal = (item: InboundOrderItem) => {
    setCurrentItem(item);
    const remaining = item.receivedQty - (item.qualifiedQty || 0) - (item.rejectedQty || 0);
    setQualifiedQty(remaining); // 默认全部验收
    setRejectedQty(0);
    setRejectReason("");
    setInspectMode("full"); // 默认全部验收模式
    setInspectModalOpen(true);
  };

  // 执行验收
  const handleInspect = async () => {
    if (!selectedOrder || !currentItem) return;

    const totalToInspect = qualifiedQty + rejectedQty;
    const remaining = currentItem.receivedQty - (currentItem.qualifiedQty || 0) - (currentItem.rejectedQty || 0);

    // 基础校验
    if (totalToInspect <= 0) {
      toast.error("验收数量必须大于0");
      return;
    }

    if (totalToInspect > remaining) {
      toast.error(`验收数量不能超过待验收数量(${remaining})`);
      return;
    }

    // 根据验收模式校验
    if (inspectMode === "full") {
      // 全部验收模式：合格 + 不合格 必须等于待验收数量
      if (totalToInspect !== remaining) {
        toast.error(`全部验收模式：合格数量 + 不合格数量 必须等于待验收数量(${remaining})`);
        return;
      }
    } else {
      // 部分验收模式：合格 + 不合格 必须小于待验收数量
      if (totalToInspect >= remaining) {
        toast.error(`部分验收模式：验收数量必须小于待验收数量(${remaining})，如需全部验收请切换为"全部验收"模式`);
        return;
      }
    }

    if (rejectedQty > 0 && !rejectReason) {
      toast.error("不合格品必须填写原因");
      return;
    }

    setLoading(true);
    try {
      await fetchApi("/api/v1/inbound/inspect/execute", {
        method: "POST",
        body: JSON.stringify({
          orderId: selectedOrder.id,
          itemId: currentItem.id,
          qualifiedQty,
          rejectedQty,
          rejectReason,
        }),
      });

      // 根据模式显示不同的成功提示
      if (inspectMode === "partial") {
        toast.success(`部分验收成功：本次验收${totalToInspect}件，剩余${remaining - totalToInspect}件待下次验收`);
      } else {
        toast.success("验收完成");
      }
      setInspectModalOpen(false);

      // 刷新数据
      const detail = await fetchApi<InboundOrder>(`/api/v1/inbound/orders/${selectedOrder.id}`);
      setSelectedOrder(detail);
      const records = await fetchApi<InspectRecord[]>(`/api/v1/inbound/inspect/records/order/${selectedOrder.id}`);
      setOrderRecords(records || []);

      // 刷新列表和全局记录
      loadPendingOrders();
      loadRecentRecords();
    } catch (error) {
      const message = error instanceof Error ? error.message : "验收失败";
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
    { key: "totalReceivedQty", title: "收货数量", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    {
      key: "progress",
      title: "验收进度",
      width: "100px",
      render: (_: unknown, row: InboundOrder) => (
        <div className="flex items-center gap-2">
          <div className="flex-1 bg-gray-200 rounded-full h-1.5">
            <div className="bg-green-600 h-1.5 rounded-full" style={{ width: `${row.progressInspect || 0}%` }} />
          </div>
          <span className="text-xs text-gray-500">{row.progressInspect || 0}%</span>
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
          验收 <ChevronRight size={14} />
        </button>
      ),
    },
  ];

  // 商品明细列定义
  const itemColumns = [
    { key: "skuCode", title: "SKU编码", width: "120px" },
    { key: "productName", title: "商品名称", width: "150px" },
    { key: "receivedQty", title: "收货数量", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "qualifiedQty", title: "合格数量", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "rejectedQty", title: "不合格", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    {
      key: "pending",
      title: "待验收",
      width: "80px",
      render: (_: unknown, item: InboundOrderItem) => {
        const pending = item.receivedQty - (item.qualifiedQty || 0) - (item.rejectedQty || 0);
        return <span className="font-medium text-green-600">{pending}</span>;
      },
    },
    {
      key: "status",
      title: "状态",
      width: "80px",
      render: (_: unknown, item: InboundOrderItem) => {
        const pending = item.receivedQty - (item.qualifiedQty || 0) - (item.rejectedQty || 0);
        const isComplete = pending <= 0;
        return (
          <span className={`px-2 py-0.5 rounded text-xs ${isComplete ? "bg-green-100 text-green-700" : "bg-yellow-100 text-yellow-700"}`}>
            {isComplete ? "已验收" : "待验收"}
          </span>
        );
      },
    },
    {
      key: "action",
      title: "操作",
      width: "80px",
      render: (_: unknown, item: InboundOrderItem) => {
        const pending = item.receivedQty - (item.qualifiedQty || 0) - (item.rejectedQty || 0);
        return (
          <button
            onClick={() => openInspectModal(item)}
            disabled={pending <= 0}
            className="text-blue-600 hover:text-blue-700 disabled:text-gray-400"
          >
            验收
          </button>
        );
      },
    },
  ];

  // 验收记录列定义
  const recordColumns = [
    { key: "inspectTime", title: "验收时间", width: "150px", render: (v: string) => v?.replace("T", " ").slice(0, 16) },
    { key: "inboundOrderNo", title: "入库单号", width: "100px", render: (v: string) => <span className="font-mono">{v}</span> },
    { key: "skuCode", title: "SKU编码", width: "100px" },
    { key: "productName", title: "商品名称", width: "120px" },
    { key: "qualifiedQty", title: "合格数量", width: "80px", render: (v: number) => <span className="font-medium text-green-600">{v}</span> },
    { key: "rejectedQty", title: "不合格", width: "60px", render: (v: number) => v > 0 ? <span className="text-red-600">{v}</span> : "-" },
    { key: "inspectUserName", title: "操作人", width: "80px" },
  ];

  // 当前入库单的验收记录列定义
  const orderRecordColumns = [
    { key: "inspectTime", title: "验收时间", width: "150px", render: (v: string) => v?.replace("T", " ").slice(0, 16) },
    { key: "skuCode", title: "SKU编码", width: "100px" },
    { key: "productName", title: "商品名称", width: "120px" },
    { key: "qualifiedQty", title: "合格数量", width: "80px", render: (v: number) => <span className="font-medium text-green-600">{v}</span> },
    { key: "rejectedQty", title: "不合格", width: "60px", render: (v: number) => v > 0 ? <span className="text-red-600">{v}</span> : "-" },
    { key: "rejectReason", title: "不合格原因", width: "120px", render: (v: string) => v || "-" },
    { key: "inspectUserName", title: "操作人", width: "80px" },
  ];

  return (
    <div className="space-y-4">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">验收作业</h2>
        <button
          onClick={() => { loadPendingOrders(); loadRecentRecords(); }}
          className="text-sm text-blue-600 hover:text-blue-700"
        >
          刷新
        </button>
      </div>

      {/* 入库单列表 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <h3 className="text-sm font-semibold mb-3 text-gray-700">待验收入库单</h3>
        {loadingOrders ? (
          <div className="text-center py-8 text-gray-400 text-sm">加载中...</div>
        ) : orders.length > 0 ? (
          <DataTable columns={orderColumns} data={orders} />
        ) : (
          <div className="text-center py-8 text-gray-400 text-sm">暂无待验收入库单</div>
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
                <span className="text-sm text-gray-500">供应商：</span>
                <span className="text-sm font-medium">{selectedOrder.supplierName || "-"}</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-sm text-gray-500">仓库：</span>
                <span className="text-sm font-medium">{selectedOrder.warehouseName || "-"}</span>
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
                <span className="text-sm text-gray-500">验收进度：</span>
                <div className="flex-1 bg-gray-200 rounded-full h-2">
                  <div className="bg-green-600 h-2 rounded-full transition-all" style={{ width: `${selectedOrder.progressInspect || 0}%` }} />
                </div>
                <span className="text-sm font-medium">{selectedOrder.progressInspect || 0}%</span>
                <span className="text-sm text-gray-500">
                  ({selectedOrder.totalQualifiedQty || 0}/{selectedOrder.totalReceivedQty})
                </span>
              </div>
            </div>
          </div>

          {/* 商品明细 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold mb-3 text-gray-700">商品明细</h3>
            <DataTable columns={itemColumns} data={selectedOrder.items || []} />
          </div>

          {/* 当前入库单的验收记录 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold mb-3 text-gray-700 flex items-center gap-2">
              <History size={16} />
              本单验收记录 ({orderRecords.length}条)
            </h3>
            {orderRecords.length > 0 ? (
              <DataTable columns={orderRecordColumns} data={orderRecords} />
            ) : (
              <div className="text-center py-8 text-gray-400 text-sm">暂无验收记录</div>
            )}
          </div>
        </>
      )}

      {/* 最近验收记录 - 放在最下面 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <h3 className="text-sm font-semibold mb-3 text-gray-700 flex items-center gap-2">
          <History size={16} />
          最近验收记录
        </h3>
        {loadingRecords ? (
          <div className="text-center py-8 text-gray-400 text-sm">加载中...</div>
        ) : recentRecords.length > 0 ? (
          <DataTable columns={recordColumns} data={recentRecords} />
        ) : (
          <div className="text-center py-8 text-gray-400 text-sm">暂无验收记录</div>
        )}
      </div>

      {/* 验收弹窗 */}
      <Dialog open={inspectModalOpen} onOpenChange={setInspectModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>验收确认</DialogTitle>
          </DialogHeader>
          {currentItem && (() => {
            const remaining = currentItem.receivedQty - (currentItem.qualifiedQty || 0) - (currentItem.rejectedQty || 0);
            const totalToInspect = qualifiedQty + rejectedQty;
            return (
              <div className="space-y-3 py-3">
                <div className="bg-gray-50 rounded p-3 text-sm">
                  <div className="grid grid-cols-2 gap-2">
                    <div><span className="text-gray-500">SKU：</span>{currentItem.skuCode}</div>
                    <div><span className="text-gray-500">商品：</span>{currentItem.productName}</div>
                    <div><span className="text-gray-500">收货数量：</span>{currentItem.receivedQty}</div>
                    <div><span className="text-gray-500">待验收：</span><span className="font-bold text-blue-600">{remaining}</span></div>
                  </div>
                </div>

                {/* 验收模式选择 */}
                <div>
                  <Label>验收模式</Label>
                  <div className="flex gap-2 mt-1">
                    <button
                      type="button"
                      onClick={() => {
                        setInspectMode("full");
                        setQualifiedQty(remaining);
                        setRejectedQty(0);
                      }}
                      className={`flex-1 py-2 px-3 rounded border text-sm font-medium transition-colors ${
                        inspectMode === "full"
                          ? "bg-blue-50 border-blue-500 text-blue-700"
                          : "bg-white border-gray-300 text-gray-600 hover:bg-gray-50"
                      }`}
                    >
                      全部验收
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setInspectMode("partial");
                        setQualifiedQty(0);
                        setRejectedQty(0);
                      }}
                      className={`flex-1 py-2 px-3 rounded border text-sm font-medium transition-colors ${
                        inspectMode === "partial"
                          ? "bg-orange-50 border-orange-500 text-orange-700"
                          : "bg-white border-gray-300 text-gray-600 hover:bg-gray-50"
                      }`}
                    >
                      部分验收
                    </button>
                  </div>
                  {inspectMode === "partial" && (
                    <p className="text-xs text-orange-600 mt-1">
                      部分验收：本次验收后剩余商品可下次继续验收
                    </p>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <Label>合格数量 *</Label>
                    <Input
                      type="number"
                      min={0}
                      max={remaining}
                      value={qualifiedQty}
                      onChange={(e) => setQualifiedQty(Number(e.target.value))}
                      className="mt-1"
                    />
                  </div>
                  <div>
                    <Label>不合格数量</Label>
                    <Input
                      type="number"
                      min={0}
                      max={remaining}
                      value={rejectedQty}
                      onChange={(e) => setRejectedQty(Number(e.target.value))}
                      className="mt-1"
                    />
                  </div>
                </div>

                {/* 验收数量汇总提示 */}
                <div className={`p-2 rounded text-sm ${
                  totalToInspect === remaining ? "bg-green-50 text-green-700" :
                  totalToInspect < remaining ? "bg-orange-50 text-orange-700" :
                  "bg-red-50 text-red-700"
                }`}>
                  <div className="flex justify-between">
                    <span>本次验收：{totalToInspect} 件</span>
                    {totalToInspect < remaining && (
                      <span>剩余：{remaining - totalToInspect} 件</span>
                    )}
                  </div>
                </div>

                {rejectedQty > 0 && (
                  <div>
                    <Label>不合格原因 *</Label>
                    <select
                      className="w-full mt-1 px-3 py-2 border rounded text-sm"
                      value={rejectReason}
                      onChange={(e) => setRejectReason(e.target.value)}
                    >
                      <option value="">选择不合格原因</option>
                      <option value="包装破损">包装破损</option>
                      <option value="商品损坏">商品损坏</option>
                      <option value="规格不符">规格不符</option>
                      <option value="效期问题">效期问题</option>
                      <option value="质量问题">质量问题</option>
                      <option value="其他">其他</option>
                    </select>
                  </div>
                )}
              </div>
            );
          })()}
          <DialogFooter>
            <Button variant="outline" onClick={() => setInspectModalOpen(false)}>取消</Button>
            <Button onClick={handleInspect} disabled={loading}>确认验收</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}