import { useState, useEffect } from "react";
import { Package, Truck, Building2, CheckCircle, ChevronRight, History, Send, ScanLine } from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Button } from "../../components/ui/button";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import DataTable from "../../components/DataTable";
import { toast } from "sonner";

// ==================== 类型定义 ====================

interface OutboundOrder {
  id: number;
  orderNo: string;
  orderTypeName: string;
  warehouseName: string;
  customerName: string;
  receiverName: string;
  receiverPhone: string;
  receiverAddress: string;
  logisticsCompany: string;
  trackingNo: string;
  status: number;
  statusName: string;
  totalQty: number;
  totalPackedQty: number;
  progressPack: number;
  items: OutboundOrderItem[];
}

interface OutboundOrderItem {
  id: number;
  productId: number;
  skuCode: string;
  productName: string;
  barcode: string;
  qty: number;
  packedQty: number;
  shippedQty: number;
  status: number;
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
  4: "bg-purple-100 text-purple-700", // 待发货
  5: "bg-green-100 text-green-700",
  9: "bg-gray-100 text-gray-500",
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

// 商品明细状态映射（与订单状态不同）
const ITEM_STATUS_NAMES: Record<number, string> = {
  0: "待拣货",
  1: "拣货中",
  2: "已拣货",
  3: "已打包",
  4: "已发货",
  9: "已取消",
};

const ITEM_STATUS_COLORS: Record<number, string> = {
  0: "bg-yellow-100 text-yellow-700",
  1: "bg-blue-100 text-blue-700",
  2: "bg-green-100 text-green-700",
  3: "bg-orange-100 text-orange-700",
  4: "bg-purple-100 text-purple-700",
  9: "bg-gray-100 text-gray-500",
};

const LOGISTICS_COMPANIES = [
  { code: "SF", name: "顺丰速运" },
  { code: "JD", name: "京东物流" },
  { code: "ZT", name: "中通快递" },
  { code: "YT", name: "圆通速递" },
  { code: "ST", name: "申通快递" },
  { code: "EMS", name: "EMS" },
  { code: "YD", name: "韵达快递" },
  { code: "DB", name: "德邦快递" },
  { code: "OTHER", name: "其他" },
];

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

export default function ShipPage() {
  // 待发货订单列表
  const [orders, setOrders] = useState<OutboundOrder[]>([]);
  const [loadingOrders, setLoadingOrders] = useState(false);

  // 选中的订单
  const [selectedOrder, setSelectedOrder] = useState<OutboundOrder | null>(null);

  // 扫码输入（出库单号或包裹号）
  const [scanInput, setScanInput] = useState("");

  // 发货确认弹窗
  const [confirmModalOpen, setConfirmModalOpen] = useState(false);
  const [logisticsCompany, setLogisticsCompany] = useState("");
  const [trackingNo, setTrackingNo] = useState("");
  const [remark, setRemark] = useState("");

  // 批量发货模式
  const [batchMode, setBatchMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  // 当前用户
  const [currentUser] = useState<{id?: number, name?: string} | null>(() => {
    const userStr = localStorage.getItem("user");
    return userStr ? JSON.parse(userStr) : null;
  });

  const [loading, setLoading] = useState(false);

  // 加载待发货订单（状态4=待发货）
  const loadPendingOrders = async () => {
    setLoadingOrders(true);
    try {
      const data = await fetchApi<{ list: OutboundOrder[] }>("/api/v1/outbound/orders?status=4&limit=50");
      console.log("ShipPage loaded orders:", data.list?.map(o => ({ id: o.id, orderNo: o.orderNo, status: o.status, statusName: o.statusName })));
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

  // 扫码查询订单
  const handleScanOrder = async () => {
    if (!scanInput.trim()) {
      toast.error("请输入出库单号或包裹号");
      return;
    }

    try {
      // 先尝试按出库单号查询
      const data = await fetchApi<{ list: OutboundOrder[] }>(`/api/v1/outbound/orders?orderNo=${scanInput}`);
      if (data.list && data.list.length > 0) {
        const order = data.list[0];
        if (order.status !== 4) {
          toast.error(`订单状态为${STATUS_NAMES[order.status]}，不是待发货状态`);
          return;
        }
        setSelectedOrder(order);
        // 获取详情
        const detail = await fetchApi<OutboundOrder>(`/api/v1/outbound/orders/${order.id}`);
        setSelectedOrder(detail);
        toast.success("订单已找到");
        return;
      }

      // 再尝试按包裹号查询（通过发货接口）
      const shipData = await fetchApi<{ outboundOrderId?: number }>(`/api/v1/outbound/ship/by-package/${scanInput}`);
      if (shipData?.outboundOrderId) {
        const detail = await fetchApi<OutboundOrder>(`/api/v1/outbound/orders/${shipData.outboundOrderId}`);
        if (detail.status !== 4) {
          toast.error(`订单状态为${STATUS_NAMES[detail.status]}，不是待发货状态`);
          return;
        }
        setSelectedOrder(detail);
        toast.success("订单已找到");
        return;
      }

      toast.error("未找到订单");
    } catch (error) {
      const message = error instanceof Error ? error.message : "查询失败";
      toast.error(message);
    }
  };

  // 选择订单
  const handleSelectOrder = async (order: OutboundOrder) => {
    try {
      const detail = await fetchApi<OutboundOrder>(`/api/v1/outbound/orders/${order.id}`);
      setSelectedOrder(detail);
    } catch (error) {
      toast.error("加载订单详情失败");
    }
  };

  // 打开发货确认弹窗
  const openConfirmModal = () => {
    if (selectedOrder) {
      setLogisticsCompany(selectedOrder.logisticsCompany || "");
      setTrackingNo("");
      setRemark("");
    } else {
      setLogisticsCompany("");
      setTrackingNo("");
      setRemark("");
    }
    setConfirmModalOpen(true);
  };

  // 确认发货（单个）
  const handleConfirmShip = async () => {
    if (!currentUser?.id) {
      toast.error("请先登录");
      return;
    }

    if (!trackingNo.trim()) {
      toast.error("请输入物流单号");
      return;
    }

    if (!logisticsCompany) {
      toast.error("请选择物流公司");
      return;
    }

    setLoading(true);
    try {
      if (selectedOrder) {
        // 单个发货
        await fetchApi("/api/v1/outbound/ship/confirm", {
          method: "POST",
          body: JSON.stringify({
            outboundOrderId: selectedOrder.id,
            outboundOrderNo: selectedOrder.orderNo,
            logisticsCompany: LOGISTICS_COMPANIES.find(l => l.code === logisticsCompany)?.name || logisticsCompany,
            logisticsCompanyCode: logisticsCompany,
            trackingNo: trackingNo,
            shipUserId: currentUser.id,
            shipUserName: currentUser.name || "发货员",
            remark: remark,
          }),
        });
        toast.success("发货成功");
      } else if (batchMode && selectedIds.length > 0) {
        // 批量发货
        await fetchApi("/api/v1/outbound/ship/batch", {
          method: "POST",
          body: JSON.stringify({
            orderIds: selectedIds,
            logisticsCompany: LOGISTICS_COMPANIES.find(l => l.code === logisticsCompany)?.name || logisticsCompany,
            logisticsCompanyCode: logisticsCompany,
            trackingNo: trackingNo,
            shipUserId: currentUser.id,
            shipUserName: currentUser.name || "发货员",
            isBatch: true,
          }),
        });
        toast.success(`批量发货成功: ${selectedIds.length} 个订单`);
        setSelectedIds([]);
        setBatchMode(false);
      }

      setConfirmModalOpen(false);
      setSelectedOrder(null);
      setScanInput("");
      loadPendingOrders();
    } catch (error) {
      const message = error instanceof Error ? error.message : "发货失败";
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  // 批量选择
  const handleBatchSelect = (orderId: number) => {
    if (selectedIds.includes(orderId)) {
      setSelectedIds(selectedIds.filter(id => id !== orderId));
    } else {
      setSelectedIds([...selectedIds, orderId]);
    }
  };

  // 订单列表列定义
  const orderColumns = [
    ...(batchMode ? [{
      key: "select",
      title: "选择",
      width: "50px",
      render: (_: unknown, row: OutboundOrder) => (
        <input
          type="checkbox"
          checked={selectedIds.includes(row.id)}
          onChange={() => handleBatchSelect(row.id)}
          className="rounded"
        />
      ),
    }] : []),
    { key: "orderNo", title: "出库单号", width: "120px", render: (v: string) => <span className="font-mono">{v}</span> },
    { key: "customerName", title: "客户", width: "100px", render: (v: string) => v || "-" },
    { key: "receiverName", title: "收货人", width: "80px" },
    { key: "receiverPhone", title: "电话", width: "100px" },
    { key: "logisticsCompany", title: "物流", width: "80px", render: (v: string) => v || "-" },
    { key: "totalQty", title: "数量", width: "60px", render: (v: number) => v?.toLocaleString() || 0 },
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
    {
      key: "action",
      title: "操作",
      width: "60px",
      render: (_: unknown, row: OutboundOrder) => (
        <button
          onClick={() => handleSelectOrder(row)}
          className="text-blue-600 hover:text-blue-700 flex items-center gap-1"
        >
          发货 <ChevronRight size={14} />
        </button>
      ),
    },
  ];

  // 商品明细列定义
  const itemColumns = [
    { key: "skuCode", title: "SKU", width: "100px" },
    { key: "productName", title: "商品名称", width: "150px" },
    { key: "barcode", title: "条码", width: "100px", render: (v: string) => <span className="font-mono text-xs">{v}</span> },
    { key: "qty", title: "订单数量", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "packedQty", title: "已打包", width: "80px", render: (v: number) => <span className="font-medium text-blue-600">{v}</span> },
    {
      key: "status",
      title: "状态",
      width: "80px",
      render: (v: number) => (
        <span className={`px-2 py-0.5 rounded text-xs ${ITEM_STATUS_COLORS[v] || "bg-gray-100"}`}>
          {ITEM_STATUS_NAMES[v]}
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">发货作业</h2>
        <div className="flex gap-2">
          <button
            onClick={() => setBatchMode(!batchMode)}
            className={`px-3 py-1 rounded text-sm ${batchMode ? "bg-blue-100 text-blue-700" : "bg-gray-100 text-gray-600"}`}
          >
            {batchMode ? "取消批量" : "批量发货"}
          </button>
          <button
            onClick={loadPendingOrders}
            className="text-sm text-blue-600 hover:text-blue-700"
          >
            刷新
          </button>
        </div>
      </div>

      {/* 扫码区 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-2">
          <div className="flex-1 relative">
            <ScanLine size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="扫描或输入出库单号/包裹号"
              className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={scanInput}
              onChange={(e) => setScanInput(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleScanOrder()}
            />
          </div>
          <button
            onClick={handleScanOrder}
            className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
          >
            扫码查询
          </button>
          {(batchMode && selectedIds.length > 0) && (
            <button
              onClick={openConfirmModal}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              批量发货 ({selectedIds.length})
            </button>
          )}
        </div>
      </div>

      {/* 订单列表 */}
      {!selectedOrder && (
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3 text-gray-700">待发货订单</h3>
          {loadingOrders ? (
            <div className="text-center py-8 text-gray-400 text-sm">加载中...</div>
          ) : orders.length > 0 ? (
            <DataTable columns={orderColumns} data={orders} />
          ) : (
            <div className="text-center py-8 text-gray-400 text-sm">暂无待发货订单</div>
          )}
        </div>
      )}

      {/* 选中的订单详情 */}
      {selectedOrder && (
        <>
          {/* 订单信息 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold mb-3 text-gray-700">订单信息</h3>
            <div className="grid grid-cols-4 gap-4">
              <div className="flex items-center gap-2">
                <Package size={16} className="text-gray-400" />
                <span className="text-sm text-gray-500">出库单号：</span>
                <span className="text-sm font-medium font-mono">{selectedOrder.orderNo}</span>
              </div>
              <div className="flex items-center gap-2">
                <Truck size={16} className="text-gray-400" />
                <span className="text-sm text-gray-500">客户：</span>
                <span className="text-sm font-medium">{selectedOrder.customerName || "-"}</span>
              </div>
              <div className="flex items-center gap-2">
                <Building2 size={16} className="text-gray-400" />
                <span className="text-sm text-gray-500">收货人：</span>
                <span className="text-sm font-medium">{selectedOrder.receiverName || "-"}</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle size={16} className="text-gray-400" />
                <span className="text-sm text-gray-500">电话：</span>
                <span className="text-sm font-medium">{selectedOrder.receiverPhone || "-"}</span>
              </div>
            </div>
            <div className="mt-2 pt-2 border-t text-sm text-gray-500">
              收货地址：{selectedOrder.receiverAddress || "-"}
            </div>
            {selectedOrder.logisticsCompany && (
              <div className="mt-1 text-sm text-gray-500">
                物流公司：{selectedOrder.logisticsCompany}
              </div>
            )}
          </div>

          {/* 商品明细 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold mb-3 text-gray-700">商品明细</h3>
            <DataTable columns={itemColumns} data={selectedOrder.items || []} />
          </div>

          {/* 操作按钮 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <div className="flex items-center justify-between">
              <div className="text-sm text-gray-500">
                总数量: <span className="font-medium">{selectedOrder.totalQty}</span> 件，
                已打包: <span className="font-medium text-blue-600">{selectedOrder.totalPackedQty}</span> 件
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => setSelectedOrder(null)}
                  className="px-4 py-2 bg-gray-100 text-gray-600 rounded hover:bg-gray-200"
                >
                  返回列表
                </button>
                <button
                  onClick={openConfirmModal}
                  disabled={loading}
                  className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50"
                >
                  确认发货
                </button>
              </div>
            </div>
          </div>
        </>
      )}

      {/* 发货确认弹窗 */}
      <Dialog open={confirmModalOpen} onOpenChange={setConfirmModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>
              {batchMode ? "批量发货确认" : "发货确认"}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-3 py-3">
            {(selectedOrder || batchMode) && (
              <div className="bg-gray-50 rounded p-3 text-sm">
                {selectedOrder ? (
                  <div className="grid grid-cols-2 gap-2">
                    <div><span className="text-gray-500">出库单号：</span><span className="font-mono">{selectedOrder.orderNo}</span></div>
                    <div><span className="text-gray-500">收货人：</span>{selectedOrder.receiverName}</div>
                    <div><span className="text-gray-500">总数量：</span>{selectedOrder.totalQty} 件</div>
                    <div><span className="text-gray-500">已打包：</span>{selectedOrder.totalPackedQty} 件</div>
                  </div>
                ) : (
                  <div>
                    批量发货: <span className="font-medium text-blue-600">{selectedIds.length}</span> 个订单
                  </div>
                )}
              </div>
            )}

            <div>
              <Label>物流公司 *</Label>
              <Select
                value={logisticsCompany}
                onValueChange={setLogisticsCompany}
              >
                <SelectTrigger className="mt-1">
                  <SelectValue placeholder="选择物流公司" />
                </SelectTrigger>
                <SelectContent>
                  {LOGISTICS_COMPANIES.map((l) => (
                    <SelectItem key={l.code} value={l.code}>
                      {l.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label>物流单号 *</Label>
              <Input
                value={trackingNo}
                onChange={(e) => setTrackingNo(e.target.value)}
                className="mt-1"
                placeholder="输入物流单号"
              />
            </div>

            {!batchMode && (
              <div>
                <Label>备注</Label>
                <Input
                  value={remark}
                  onChange={(e) => setRemark(e.target.value)}
                  className="mt-1"
                  placeholder="可选备注"
                />
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmModalOpen(false)}>取消</Button>
            <Button
              onClick={handleConfirmShip}
              disabled={loading || !trackingNo.trim() || !logisticsCompany}
            >
              确认发货
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}