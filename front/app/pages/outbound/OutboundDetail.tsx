import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router";
import { ArrowLeft, Package, User, Clock, CheckCircle, Truck, Building2, ArrowRightLeft, AlertCircle, Play } from "lucide-react";
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
  shippedQty: number;
  batchNo: string;
  locationCode: string;
  status: number;
  statusName: string;
}

interface OutboundOrder {
  id: number;
  orderNo: string;
  soNo: string;
  orderType: number;
  orderTypeName: string;
  sourceType: number;
  sourceTypeName: string;
  customerId: number;
  customerCode: string;
  customerName: string;
  customerPhone: string;
  customerAddress: string;
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  targetWarehouseId: number;
  targetWarehouseCode: string;
  targetWarehouseName: string;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  priority: number;
  priorityName: string;
  logisticsCompany: string;
  trackingNo: string;
  receiverName: string;
  receiverPhone: string;
  receiverAddress: string;
  status: number;
  statusName: string;
  totalQty: number;
  totalPickedQty: number;
  totalPackedQty: number;
  totalShippedQty: number;
  progressPick: number;
  progressPack: number;
  progressShip: number;
  waveNo: string;
  transferInboundId: number | null;
  transferInboundNo: string | null;
  remark: string;
  cancelReason: string;
  createTime: string;
  completeTime: string;
  items: OutboundOrderItem[];
}

interface ApiResponse<T> {
  success: boolean;
  code: number;
  message: string;
  data: T;
}

// ==================== 常量定义 ====================

const ORDER_TYPE_NAMES: Record<number, string> = {
  1: "销售出库",
  2: "调拨出库",
  3: "退货出库",
  4: "报废出库",
  5: "样品出库",
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

const STATUS_COLORS: Record<number, string> = {
  0: "bg-yellow-100 text-yellow-700",
  1: "bg-blue-100 text-blue-700",
  2: "bg-blue-100 text-blue-700",
  3: "bg-orange-100 text-orange-700",
  4: "bg-purple-100 text-purple-700",
  5: "bg-green-100 text-green-700",
  9: "bg-gray-100 text-gray-500",
};

const PRIORITY_COLORS: Record<number, string> = {
  1: "bg-red-100 text-red-700",
  2: "bg-orange-100 text-orange-700",
  3: "bg-gray-100 text-gray-700",
  4: "bg-gray-100 text-gray-500",
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

export default function OutboundDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<OutboundOrder | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDetail();
  }, [id]);

  async function fetchDetail() {
    try {
      setLoading(true);
      const data = await fetchApi<OutboundOrder>(`/api/v1/outbound/orders/${id}`);
      setDetail(data);
    } catch {
      toast.error("获取详情失败");
    } finally {
      setLoading(false);
    }
  }

  async function handleAllocate() {
    if (!detail) return;
    try {
      await fetchApi<{ orderId: number; orderNo: string; newStatus: number }>(
        `/api/v1/outbound/orders/${detail.id}/allocate`,
        { method: "POST" }
      );
      toast.success("出库单已分配，可开始拣货");
      fetchDetail(); // 刷新详情
    } catch (err) {
      const message = err instanceof Error ? err.message : "分配失败";
      toast.error(message);
    }
  }

  const itemColumns = [
    { key: "skuCode", title: "SKU编码", width: "120px" },
    { key: "productName", title: "商品名称", width: "150px" },
    { key: "barcode", title: "条码", width: "100px" },
    { key: "qty", title: "出库数量", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "pickedQty", title: "已拣货", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "packedQty", title: "已打包", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "shippedQty", title: "已发货", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "locationCode", title: "库位", width: "100px", render: (v: string) => v || "-" },
    { key: "batchNo", title: "批次号", width: "150px", render: (v: string) => v ? <span className="font-mono text-xs">{v}</span> : "-" },
    {
      key: "status",
      title: "状态",
      width: "80px",
      render: (value: number) => (
        <span className={`px-2 py-0.5 rounded text-xs ${STATUS_COLORS[value] || "bg-gray-100"}`}>
          {STATUS_NAMES[value] || "未知"}
        </span>
      ),
    },
  ];

  if (loading) {
    return (
      <div className="p-4">
        <div className="text-center py-12 text-gray-500">加载中...</div>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="p-4">
        <div className="text-center py-12 text-gray-500">
          <div className="text-4xl mb-2">📋</div>
          <div>出库单不存在</div>
        </div>
      </div>
    );
  }

  // 根据出库类型确定显示哪些关联方信息
  const showCustomer = detail.orderType === 1 || detail.orderType === 5;
  const showTargetWarehouse = detail.orderType === 2;
  const showSupplier = detail.orderType === 3;

  return (
    <div className="space-y-4">
      {/* 页面标题 */}
      <div className="flex items-center gap-4">
        <button onClick={() => navigate(-1)} className="p-2 hover:bg-gray-100 rounded">
          <ArrowLeft size={20} />
        </button>
        <h2 className="text-lg font-semibold">出库单详情 - {detail.orderNo}</h2>
        <span className={`px-2 py-0.5 rounded text-xs ${STATUS_COLORS[detail.status] || "bg-gray-100"}`}>
          {detail.statusName || STATUS_NAMES[detail.status]}
        </span>
        <span className={`px-2 py-0.5 rounded text-xs ${PRIORITY_COLORS[detail.priority] || "bg-gray-100"}`}>
          {detail.priorityName || "普通"}
        </span>
        {/* 待分配状态显示分配按钮 */}
        {detail.status === 0 && (
          <button
            onClick={handleAllocate}
            className="px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 flex items-center gap-1"
          >
            <Play size={14} />
            分配
          </button>
        )}
      </div>

      {/* 基本信息 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <h3 className="text-sm font-semibold mb-3 text-gray-700">基本信息</h3>
        <div className="grid grid-cols-4 gap-4">
          <div className="flex items-center gap-2">
            <Package size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">出库类型：</span>
            <span className="text-sm font-medium">{detail.orderTypeName || ORDER_TYPE_NAMES[detail.orderType]}</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">销售单号：</span>
            <span className="text-sm font-medium">{detail.soNo || "-"}</span>
          </div>
          <div className="flex items-center gap-2">
            <Package size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">出库仓库：</span>
            <span className="text-sm font-medium">{detail.warehouseName || "-"}</span>
          </div>
          <div className="flex items-center gap-2">
            <Clock size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">创建时间：</span>
            <span className="text-sm font-medium">{detail.createTime}</span>
          </div>
          <div className="flex items-center gap-2">
            <Truck size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">物流公司：</span>
            <span className="text-sm font-medium">{detail.logisticsCompany || "-"}</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">物流单号：</span>
            <span className="text-sm font-medium">{detail.trackingNo || "-"}</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">完成时间：</span>
            <span className="text-sm font-medium">{detail.completeTime || "-"}</span>
          </div>
          <div className="flex items-center gap-2">
            <Package size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">波次号：</span>
            <span className="text-sm font-medium">{detail.waveNo || "-"}</span>
          </div>
        </div>
        {detail.remark && (
          <div className="mt-3 pt-3 border-t">
            <span className="text-sm text-gray-500">备注：</span>
            <span className="text-sm">{detail.remark}</span>
          </div>
        )}
        {detail.cancelReason && (
          <div className="mt-3 pt-3 border-t">
            <span className="text-sm text-gray-500">取消原因：</span>
            <span className="text-sm text-red-600">{detail.cancelReason}</span>
          </div>
        )}
      </div>

      {/* 关联方信息 - 根据出库类型显示 */}
      {showCustomer && (
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3 text-gray-700">客户信息</h3>
          <div className="grid grid-cols-4 gap-4">
            <div className="flex items-center gap-2">
              <User size={16} className="text-gray-400" />
              <span className="text-sm text-gray-500">客户：</span>
              <span className="text-sm font-medium">{detail.customerName || "-"}</span>
            </div>
            <div className="flex items-center gap-2">
              <User size={16} className="text-gray-400" />
              <span className="text-sm text-gray-500">客户电话：</span>
              <span className="text-sm font-medium">{detail.customerPhone || "-"}</span>
            </div>
            <div className="flex items-center gap-2 col-span-2">
              <Building2 size={16} className="text-gray-400" />
              <span className="text-sm text-gray-500">客户地址：</span>
              <span className="text-sm font-medium">{detail.customerAddress || "-"}</span>
            </div>
          </div>
        </div>
      )}

      {showTargetWarehouse && (
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3 text-gray-700">调拨信息</h3>
          <div className="grid grid-cols-4 gap-4">
            <div className="flex items-center gap-2">
              <ArrowRightLeft size={16} className="text-gray-400" />
              <span className="text-sm text-gray-500">调出仓库：</span>
              <span className="text-sm font-medium">{detail.warehouseName}</span>
            </div>
            <div className="flex items-center gap-2">
              <ArrowRightLeft size={16} className="text-gray-400" />
              <span className="text-sm text-gray-500">调入仓库：</span>
              <span className="text-sm font-medium text-blue-600">{detail.targetWarehouseName || "-"}</span>
            </div>
            <div className="flex items-center gap-2">
              <Package size={16} className="text-gray-400" />
              <span className="text-sm text-gray-500">调拨入库单：</span>
              {detail.transferInboundNo ? (
                <button
                  onClick={() => navigate(`/inbound/${detail.transferInboundId}`)}
                  className="text-sm font-medium text-blue-600 hover:underline"
                >
                  {detail.transferInboundNo}
                </button>
              ) : (
                <span className="text-sm text-gray-400">待生成</span>
              )}
            </div>
          </div>
        </div>
      )}

      {showSupplier && (
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3 text-gray-700">供应商信息</h3>
          <div className="grid grid-cols-4 gap-4">
            <div className="flex items-center gap-2">
              <Building2 size={16} className="text-gray-400" />
              <span className="text-sm text-gray-500">供应商：</span>
              <span className="text-sm font-medium">{detail.supplierName || "-"}</span>
            </div>
            <div className="flex items-center gap-2">
              <Package size={16} className="text-gray-400" />
              <span className="text-sm text-gray-500">供应商编码：</span>
              <span className="text-sm font-medium">{detail.supplierCode || "-"}</span>
            </div>
          </div>
        </div>
      )}

      {/* 出库进度 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <h3 className="text-sm font-semibold mb-3 text-gray-700">出库进度</h3>
        <div className="grid grid-cols-4 gap-4">
          <div className="text-center p-3 bg-gray-50 rounded">
            <div className="text-xs text-gray-500 mb-1">拣货</div>
            <div className="text-lg font-semibold">{detail.totalPickedQty}/{detail.totalQty}</div>
            <div className="text-xs text-gray-400">({detail.progressPick || 0}%)</div>
          </div>
          <div className="text-center p-3 bg-gray-50 rounded">
            <div className="text-xs text-gray-500 mb-1">打包</div>
            <div className="text-lg font-semibold">{detail.totalPackedQty}/{detail.totalPickedQty}</div>
            <div className="text-xs text-gray-400">({detail.progressPack || 0}%)</div>
          </div>
          <div className="text-center p-3 bg-gray-50 rounded">
            <div className="text-xs text-gray-500 mb-1">发货</div>
            <div className="text-lg font-semibold">{detail.totalShippedQty}/{detail.totalPackedQty}</div>
            <div className="text-xs text-gray-400">({detail.progressShip || 0}%)</div>
          </div>
          <div className="text-center p-3 bg-gray-50 rounded">
            <div className="text-xs text-gray-500 mb-1">总数量</div>
            <div className="text-lg font-semibold">{detail.totalQty}</div>
            <div className="text-xs text-gray-400">件</div>
          </div>
        </div>
      </div>

      {/* 商品明细 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <h3 className="text-sm font-semibold mb-3 text-gray-700">商品明细</h3>
        <DataTable columns={itemColumns} data={detail.items || []} />
      </div>
    </div>
  );
}
