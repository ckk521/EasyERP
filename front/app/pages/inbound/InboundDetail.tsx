import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router";
import { ArrowLeft, Package, User, Clock, CheckCircle, Truck, Building2 } from "lucide-react";
import DataTable from "../../components/DataTable";
import { toast } from "sonner";

// ==================== 类型定义 ====================

interface InboundOrderItem {
  id: number;
  productId: number;
  skuCode: string;
  productName: string;
  barcode: string;
  expectedQty: number;
  receivedQty: number;
  qualifiedQty: number;
  rejectedQty: number;
  putawayQty: number;
  returnQty: number;
  batchNo: string;
  status: number;
  statusName: string;
}

interface InboundOrder {
  id: number;
  orderNo: string;
  deliveryBatchNo: string;
  orderType: number;
  orderTypeName: string;
  poNo: string;
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  expectedDate: string;
  actualArrivalDate: string;
  status: number;
  statusName: string;
  progressReceive: number;
  progressInspect: number;
  progressPutaway: number;
  totalExpectedQty: number;
  totalReceivedQty: number;
  totalQualifiedQty: number;
  totalRejectedQty: number;
  totalPutawayQty: number;
  totalReturnQty: number;
  remark: string;
  cancelReason: string;
  createTime: string;
  completeTime: string;
  items: InboundOrderItem[];
}

interface ApiResponse<T> {
  success: boolean;
  code: number;
  message: string;
  data: T;
}

// ==================== 常量定义 ====================

const ORDER_TYPE_NAMES: Record<number, string> = {
  1: "采购入库",
  2: "退货入库",
  3: "调拨入库",
  4: "赠品入库",
  5: "其他入库",
};

const STATUS_NAMES: Record<number, string> = {
  0: "待收货",
  1: "收货中",
  2: "验收中",
  3: "待上架",
  4: "已完成",
  9: "已取消",
};

const STATUS_COLORS: Record<number, string> = {
  0: "bg-yellow-100 text-yellow-700",
  1: "bg-blue-100 text-blue-700",
  2: "bg-blue-100 text-blue-700",
  3: "bg-orange-100 text-orange-700",
  4: "bg-green-100 text-green-700",
  9: "bg-gray-100 text-gray-500",
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

export default function InboundDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<InboundOrder | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDetail();
  }, [id]);

  async function fetchDetail() {
    try {
      setLoading(true);
      const data = await fetchApi<InboundOrder>(`/api/v1/inbound/orders/${id}`);
      setDetail(data);
    } catch {
      toast.error("获取详情失败");
    } finally {
      setLoading(false);
    }
  }

  const itemColumns = [
    { key: "skuCode", title: "SKU编码", width: "120px" },
    { key: "productName", title: "商品名称", width: "150px" },
    { key: "barcode", title: "条码", width: "100px" },
    { key: "expectedQty", title: "预期数量", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "receivedQty", title: "已收货", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "qualifiedQty", title: "合格数量", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "rejectedQty", title: "不合格", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "putawayQty", title: "已上架", width: "80px", render: (v: number) => v?.toLocaleString() || 0 },
    { key: "batchNo", title: "批次号", width: "150px" },
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
          <div>入库单不存在</div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* 页面标题 */}
      <div className="flex items-center gap-4">
        <button onClick={() => navigate(-1)} className="p-2 hover:bg-gray-100 rounded">
          <ArrowLeft size={20} />
        </button>
        <h2 className="text-lg font-semibold">入库单详情 - {detail.orderNo}</h2>
        <span className={`px-2 py-0.5 rounded text-xs ${STATUS_COLORS[detail.status] || "bg-gray-100"}`}>
          {detail.statusName || STATUS_NAMES[detail.status]}
        </span>
      </div>

      {/* 基本信息 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <h3 className="text-sm font-semibold mb-3 text-gray-700">基本信息</h3>
        <div className="grid grid-cols-4 gap-4">
          <div className="flex items-center gap-2">
            <Package size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">入库类型：</span>
            <span className="text-sm font-medium">{detail.orderTypeName || ORDER_TYPE_NAMES[detail.orderType]}</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">采购单号：</span>
            <span className="text-sm font-medium">{detail.poNo || "-"}</span>
          </div>
          <div className="flex items-center gap-2">
            <Building2 size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">供应商：</span>
            <span className="text-sm font-medium">{detail.supplierName || "-"}</span>
          </div>
          <div className="flex items-center gap-2">
            <Truck size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">送货批次：</span>
            <span className="text-sm font-medium">{detail.deliveryBatchNo || "-"}</span>
          </div>
          <div className="flex items-center gap-2">
            <Package size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">仓库：</span>
            <span className="text-sm font-medium">{detail.warehouseName}</span>
          </div>
          <div className="flex items-center gap-2">
            <Clock size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">预计到货：</span>
            <span className="text-sm font-medium">{detail.expectedDate || "-"}</span>
          </div>
          <div className="flex items-center gap-2">
            <User size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">创建时间：</span>
            <span className="text-sm font-medium">{detail.createTime}</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">完成时间：</span>
            <span className="text-sm font-medium">{detail.completeTime || "-"}</span>
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

      {/* 入库进度 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <h3 className="text-sm font-semibold mb-3 text-gray-700">入库进度</h3>
        <div className="grid grid-cols-4 gap-4">
          <div className="text-center p-3 bg-gray-50 rounded">
            <div className="text-xs text-gray-500 mb-1">收货</div>
            <div className="text-lg font-semibold">{detail.totalReceivedQty}/{detail.totalExpectedQty}</div>
            <div className="text-xs text-gray-400">({detail.progressReceive || 0}%)</div>
          </div>
          <div className="text-center p-3 bg-gray-50 rounded">
            <div className="text-xs text-gray-500 mb-1">验收</div>
            <div className="text-lg font-semibold">{detail.totalQualifiedQty}/{detail.totalReceivedQty}</div>
            <div className="text-xs text-gray-400">({detail.progressInspect || 0}%)</div>
          </div>
          <div className="text-center p-3 bg-gray-50 rounded">
            <div className="text-xs text-gray-500 mb-1">上架</div>
            <div className="text-lg font-semibold">{detail.totalPutawayQty}/{detail.totalQualifiedQty}</div>
            <div className="text-xs text-gray-400">({detail.progressPutaway || 0}%)</div>
          </div>
          <div className="text-center p-3 bg-gray-50 rounded">
            <div className="text-xs text-gray-500 mb-1">退货</div>
            <div className="text-lg font-semibold text-red-600">{detail.totalReturnQty}</div>
            <div className="text-xs text-gray-400">({detail.totalExpectedQty > 0 ? Math.round(detail.totalReturnQty / detail.totalExpectedQty * 100) : 0}%)</div>
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
