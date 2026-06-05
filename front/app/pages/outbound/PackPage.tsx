import { useState, useEffect } from "react";
import { Package, Truck, Building2, CheckCircle, ChevronRight, Weight, Box } from "lucide-react";
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
  packedQty: number;
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
  customerId?: number;
  customerName?: string;
  receiverName?: string;
  receiverPhone?: string;
  receiverAddress?: string;
  logisticsCompany?: string;
  status: number;
  statusName: string;
  totalQty: number;
  totalPickedQty: number;
  totalPackedQty: number;
  progressPack: number;
  items: OutboundOrderItem[];
}

interface PackRecord {
  id: number;
  outboundOrderId: number;
  outboundOrderNo: string;
  skuCode: string;
  productName: string;
  qty: number;
  packedQty: number;
  boxType: string;
  weight: number;
  packTime: string;
  packUserName: string;
}

interface BoxType {
  id: number;
  code: string;
  name: string;
  length: number;
  width: number;
  height: number;
  maxWeight: number;
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
  4: "bg-purple-100 text-purple-700",
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

const ITEM_STATUS_COLORS: Record<number, string> = {
  0: "bg-yellow-100 text-yellow-700", // 待拣货
  1: "bg-blue-100 text-blue-700",     // 拣货中
  2: "bg-green-100 text-green-700",   // 已拣货
  3: "bg-orange-100 text-orange-700", // 已打包
  4: "bg-purple-100 text-purple-700", // 已发货
  9: "bg-gray-100 text-gray-500",     // 已取消
};

const ITEM_STATUS_NAMES: Record<number, string> = {
  0: "待拣货",
  1: "拣货中",
  2: "已拣货",
  3: "已打包",
  4: "已发货",
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

export default function PackPage() {
  // 出库单列表
  const [orders, setOrders] = useState<OutboundOrder[]>([]);
  const [loadingOrders, setLoadingOrders] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<OutboundOrder | null>(null);

  // 打包记录
  const [packRecords, setPackRecords] = useState<PackRecord[]>([]);

  // 箱型列表
  const [boxTypes, setBoxTypes] = useState<BoxType[]>([]);

  // 打包弹窗
  const [packModalOpen, setPackModalOpen] = useState(false);
  const [currentItem, setCurrentItem] = useState<OutboundOrderItem | null>(null);
  const [packQty, setPackQty] = useState(0);
  const [selectedBoxType, setSelectedBoxType] = useState("");
  const [weight, setWeight] = useState("");
  const [remark, setRemark] = useState("");

  // 当前用户
  const [currentUser] = useState<{id?: number, name?: string} | null>(() => {
    const userStr = localStorage.getItem("user");
    return userStr ? JSON.parse(userStr) : null;
  });

  const [loading, setLoading] = useState(false);

  // 加载待打包的出库单列表（状态3=待打包）
  const loadPendingOrders = async () => {
    setLoadingOrders(true);
    try {
      const data = await fetchApi<{ list: OutboundOrder[] }>("/api/v1/outbound/orders?status=3&limit=50");
      setOrders(data.list || []);
    } catch (error) {
      console.error("Failed to load orders:", error);
      setOrders([]);
    } finally {
      setLoadingOrders(false);
    }
  };

  // 加载箱型列表
  const loadBoxTypes = async () => {
    // 使用默认箱型（后续可对接实际API）
    setBoxTypes([
      { id: 1, code: "BOX-S", name: "小箱", length: 30, width: 20, height: 15, maxWeight: 5 },
      { id: 2, code: "BOX-M", name: "中箱", length: 40, width: 30, height: 20, maxWeight: 10 },
      { id: 3, code: "BOX-L", name: "大箱", length: 50, width: 40, height: 30, maxWeight: 20 },
    ]);
  };

  // 初始化加载
  useEffect(() => {
    loadPendingOrders();
    loadBoxTypes();
  }, []);

  // 选择出库单
  const handleSelectOrder = async (order: OutboundOrder) => {
    try {
      const detail = await fetchApi<OutboundOrder>(`/api/v1/outbound/orders/${order.id}`);
      setSelectedOrder(detail);
    } catch (error) {
      toast.error("加载出库单详情失败");
    }
  };

  // 打开打包弹窗
  const openPackModal = (item: OutboundOrderItem) => {
    setCurrentItem(item);
    const pendingQty = item.pickedQty - (item.packedQty || 0);
    setPackQty(pendingQty);
    setSelectedBoxType(boxTypes[0]?.code || "BOX-M");
    setWeight("");
    setRemark("");
    setPackModalOpen(true);
  };

  // 执行打包
  const handlePack = async () => {
    if (!selectedOrder || !currentItem || !currentUser?.id) return;

    if (!selectedBoxType) {
      toast.error("请选择箱型");
      return;
    }

    if (!weight || parseFloat(weight) <= 0) {
      toast.error("请输入重量");
      return;
    }

    setLoading(true);
    try {
      await fetchApi("/api/v1/outbound/pack/confirm", {
        method: "POST",
        body: JSON.stringify({
          outboundOrderId: selectedOrder.id,
          outboundOrderNo: selectedOrder.orderNo,
          itemId: currentItem.id,
          boxTypeCode: selectedBoxType,
          weight: parseFloat(weight),
          packQty: packQty,
          packUserId: currentUser.id,
          packUserName: currentUser.name || "打包员",
          remark: remark,
        }),
      });

      toast.success("打包成功");
      setPackModalOpen(false);

      // 刷新数据
      const detail = await fetchApi<OutboundOrder>(`/api/v1/outbound/orders/${selectedOrder.id}`);
      setSelectedOrder(detail);
      loadPendingOrders();
    } catch (error) {
      const message = error instanceof Error ? error.message : "打包失败";
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  // 完成订单打包
  const handleCompleteOrderPack = async () => {
    if (!selectedOrder) return;

    // 检查是否所有商品都已打包
    const allPacked = selectedOrder.items?.every(item => item.packedQty >= item.pickedQty);
    if (!allPacked) {
      toast.error("还有商品未完成打包");
      return;
    }

    setLoading(true);
    try {
      await fetchApi("/api/v1/outbound/pack/complete", {
        method: "POST",
        body: JSON.stringify({ orderId: selectedOrder.id }),
      });
      toast.success("订单打包完成，已进入待发货状态");
      setSelectedOrder(null);
      loadPendingOrders();
    } catch (error) {
      const message = error instanceof Error ? error.message : "完成打包失败";
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
      key: "customerInfo",
      title: "客户/收货人",
      width: "150px",
      render: (_: unknown, row: OutboundOrder) => row.customerName || row.receiverName || "-",
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
      title: "打包进度",
      width: "100px",
      render: (_: unknown, row: OutboundOrder) => (
        <div className="flex items-center gap-2">
          <div className="flex-1 bg-gray-200 rounded-full h-1.5">
            <div className="bg-orange-600 h-1.5 rounded-full" style={{ width: `${row.progressPack || 0}%` }} />
          </div>
          <span className="text-xs text-gray-500">{row.progressPack || 0}%</span>
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
          打包 <ChevronRight size={14} />
        </button>
      ),
    },
  ];

  // 商品明细列定义
  const itemColumns = [
    { key: "skuCode", title: "SKU编码", width: "120px" },
    { key: "productName", title: "商品名称", width: "150px" },
    { key: "barcode", title: "条码", width: "100px", render: (v: string) => v || "-" },
    { key: "pickedQty", title: "已拣货", width: "80px", render: (v: number) => <span className="font-medium text-green-600">{v || 0}</span> },
    { key: "packedQty", title: "已打包", width: "80px", render: (v: number) => <span className="font-medium text-blue-600">{v || 0}</span> },
    {
      key: "pendingQty",
      title: "待打包",
      width: "80px",
      render: (_: unknown, item: OutboundOrderItem) => {
        const pending = item.pickedQty - (item.packedQty || 0);
        return <span className="font-medium text-orange-600">{pending}</span>;
      },
    },
    {
      key: "status",
      title: "状态",
      width: "80px",
      render: (_: unknown, item: OutboundOrderItem) => {
        const pending = item.pickedQty - (item.packedQty || 0);
        const isComplete = pending <= 0;
        return (
          <span className={`px-2 py-0.5 rounded text-xs ${isComplete ? "bg-green-100 text-green-700" : "bg-orange-100 text-orange-700"}`}>
            {isComplete ? "已打包" : "待打包"}
          </span>
        );
      },
    },
    {
      key: "action",
      title: "操作",
      width: "80px",
      render: (_: unknown, item: OutboundOrderItem) => {
        const pending = item.pickedQty - (item.packedQty || 0);
        return (
          <button
            onClick={() => openPackModal(item)}
            disabled={pending <= 0}
            className="text-blue-600 hover:text-blue-700 disabled:text-gray-400"
          >
            打包
          </button>
        );
      },
    },
  ];

  // 计算打包进度
  const allPacked = selectedOrder?.items?.every(item => item.packedQty >= item.pickedQty);
  const totalPacked = selectedOrder?.items?.reduce((sum, item) => sum + (item.packedQty || 0), 0) || 0;
  const totalPicked = selectedOrder?.totalPickedQty || 0;

  return (
    <div className="space-y-4">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">打包作业</h2>
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
          <h3 className="text-sm font-semibold mb-3 text-gray-700">待打包出库单</h3>
          {loadingOrders ? (
            <div className="text-center py-8 text-gray-400 text-sm">加载中...</div>
          ) : orders.length > 0 ? (
            <DataTable columns={orderColumns} data={orders} />
          ) : (
            <div className="text-center py-8 text-gray-400 text-sm">暂无待打包出库单</div>
          )}
        </div>
      )}

      {/* 选中的出库单详情 */}
      {selectedOrder && (
        <>
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
                <span className="text-sm text-gray-500">客户：</span>
                <span className="text-sm font-medium">{selectedOrder.customerName || "-"}</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle size={16} className="text-gray-400" />
                <span className="text-sm text-gray-500">状态：</span>
                <span className={`px-2 py-0.5 rounded text-xs ${STATUS_COLORS[selectedOrder.status] || "bg-gray-100"}`}>
                  {selectedOrder.statusName || STATUS_NAMES[selectedOrder.status]}
                </span>
              </div>
            </div>
            {(selectedOrder.receiverName || selectedOrder.receiverPhone) && (
              <div className="mt-2 pt-2 border-t text-sm text-gray-500">
                收货人：{selectedOrder.receiverName || "-"}，电话：{selectedOrder.receiverPhone || "-"}
                {selectedOrder.receiverAddress && <span className="ml-2">，地址：{selectedOrder.receiverAddress}</span>}
              </div>
            )}
            <div className="mt-3 pt-3 border-t">
              <div className="flex items-center gap-4">
                <span className="text-sm text-gray-500">打包进度：</span>
                <div className="flex-1 bg-gray-200 rounded-full h-2">
                  <div className="bg-orange-600 h-2 rounded-full transition-all" style={{ width: `${selectedOrder.progressPack || 0}%` }} />
                </div>
                <span className="text-sm font-medium">{selectedOrder.progressPack || 0}%</span>
                <span className="text-sm text-gray-500">
                  ({totalPacked}/{totalPicked})
                </span>
              </div>
            </div>
          </div>

          {/* 商品明细 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold mb-3 text-gray-700">商品明细</h3>
            <DataTable columns={itemColumns} data={selectedOrder.items || []} />
          </div>

          {/* 完成打包按钮 */}
          {allPacked && (
            <div className="bg-white rounded-lg p-4 border border-gray-200">
              <div className="flex items-center justify-between">
                <div className="text-sm text-green-600 font-medium">
                  ✓ 所有商品打包完成，可提交订单
                </div>
                <button
                  onClick={handleCompleteOrderPack}
                  disabled={loading}
                  className="px-6 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50"
                >
                  完成打包
                </button>
              </div>
            </div>
          )}

          {/* 返回按钮 */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <button
              onClick={() => setSelectedOrder(null)}
              className="px-4 py-2 bg-gray-100 text-gray-600 rounded hover:bg-gray-200"
            >
              返回列表
            </button>
          </div>
        </>
      )}

      {/* 打包弹窗 */}
      <Dialog open={packModalOpen} onOpenChange={setPackModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>打包确认</DialogTitle>
          </DialogHeader>
          {currentItem && (
            <div className="space-y-3 py-3">
              <div className="bg-gray-50 rounded p-3 text-sm">
                <div className="grid grid-cols-2 gap-2">
                  <div><span className="text-gray-500">SKU：</span><span className="font-mono">{currentItem.skuCode}</span></div>
                  <div><span className="text-gray-500">商品：</span>{currentItem.productName}</div>
                  <div><span className="text-gray-500">已拣货：</span>{currentItem.pickedQty}</div>
                  <div><span className="text-gray-500">已打包：</span>{currentItem.packedQty || 0}</div>
                </div>
                <div className="mt-2 pt-2 border-t flex justify-between">
                  <span className="text-gray-500 font-medium">待打包数量：</span>
                  <span className="font-bold text-orange-600 text-lg">{currentItem.pickedQty - (currentItem.packedQty || 0)}</span>
                </div>
              </div>

              <div>
                <Label>实际打包数量 *</Label>
                <Input
                  type="number"
                  min={0}
                  max={currentItem.pickedQty - (currentItem.packedQty || 0)}
                  value={packQty}
                  onChange={(e) => setPackQty(Number(e.target.value))}
                  className="mt-1"
                />
              </div>

              <div>
                <Label>选择包装箱型 *</Label>
                <Select
                  value={selectedBoxType}
                  onValueChange={setSelectedBoxType}
                >
                  <SelectTrigger className="mt-1">
                    <SelectValue placeholder="选择箱型" />
                  </SelectTrigger>
                  <SelectContent>
                    {boxTypes.map((box) => (
                      <SelectItem key={box.code} value={box.code}>
                        {box.name} ({box.length}×{box.width}×{box.height}cm, 承重{box.maxWeight}kg)
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label>包裹重量 (kg) *</Label>
                <Input
                  type="number"
                  min={0.1}
                  step={0.1}
                  value={weight}
                  onChange={(e) => setWeight(e.target.value)}
                  className="mt-1"
                  placeholder="例如: 2.5"
                />
              </div>

              <div>
                <Label>备注</Label>
                <Input
                  value={remark}
                  onChange={(e) => setRemark(e.target.value)}
                  className="mt-1"
                  placeholder="可选备注"
                />
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setPackModalOpen(false)}>取消</Button>
            <Button
              onClick={handlePack}
              disabled={loading || !selectedBoxType || !weight || parseFloat(weight) <= 0}
            >
              确认打包
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}