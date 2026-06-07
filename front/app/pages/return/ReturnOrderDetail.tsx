import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router";
import { ArrowLeft, Check, X, Package, FileText } from "lucide-react";

interface ReturnOrderItem {
  id: number;
  skuCode: string;
  productName: string;
  barcode: string;
  originalQty: number;
  expectedQty: number;
  receivedQty: number;
  remark: string;
}

interface ReturnOrder {
  id: number;
  returnNo: string;
  originalOutboundId: number;
  originalOutboundNo: string;
  customerName: string;
  returnReason: number;
  returnReasonName: string;
  returnReasonText: string;
  status: number;
  statusName: string;
  totalExpectedQty: number;
  totalReceivedQty: number;
  inboundOrderId?: number;
  inboundOrderNo?: string;
  warehouseName: string;
  remark: string;
  cancelReason: string;
  createUserName: string;
  createTime: string;
  receiveUserName: string;
  receiveTime: string;
  completeTime: string;
  items: ReturnOrderItem[];
}

const STATUS_COLORS: Record<number, string> = {
  0: "bg-yellow-100 text-yellow-700", // 待收货
  1: "bg-blue-100 text-blue-700",     // 已收货
  2: "bg-green-100 text-green-700",   // 已完成
  9: "bg-gray-100 text-gray-500",     // 已取消
};

const STATUS_NAMES: Record<number, string> = {
  0: "待收货",
  1: "已收货",
  2: "已完成",
  9: "已取消",
};

const REASON_NAMES: Record<number, string> = {
  1: "质量问题",
  2: "发错货",
  3: "数量不符",
  4: "不满意",
  5: "7天无理由",
  6: "其他",
};

async function fetchApi<T>(url: string, options?: RequestInit): Promise<T> {
  const token = localStorage.getItem("token");
  const res = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: token ? `Bearer ${token}` : "",
      ...options?.headers,
    },
  });
  const data = await res.json();
  if (!data.success) {
    throw new Error(data.message || "请求失败");
  }
  return data.data;
}

export default function ReturnOrderDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [order, setOrder] = useState<ReturnOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [receiveMode, setReceiveMode] = useState(false);
  const [receiveItems, setReceiveItems] = useState<{ itemId: number; receivedQty: number; remark: string }[]>([]);
  const [cancelMode, setCancelMode] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [error, setError] = useState("");

  const loadOrder = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const data = await fetchApi<ReturnOrder>(`/api/v1/return/orders/${id}`);
      setOrder(data);
      // 初始化收货数据
      if (data.items) {
        setReceiveItems(
          data.items.map((item) => ({
            itemId: item.id,
            receivedQty: item.expectedQty,
            remark: "",
          }))
        );
      }
    } catch (err: any) {
      setError(err.message || "加载失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrder();
  }, [id]);

  const handleReceive = async () => {
    if (!order) return;
    setError("");

    // 校验
    const validItems = receiveItems.filter((item) => item.receivedQty >= 0);
    if (validItems.length === 0) {
      setError("请填写实收数量");
      return;
    }

    try {
      await fetchApi(`/api/v1/return/orders/${order.id}/receive`, {
        method: "POST",
        body: JSON.stringify({
          items: receiveItems,
        }),
      });

      setReceiveMode(false);
      loadOrder();
    } catch (err: any) {
      setError(err.message || "收货失败");
    }
  };

  const handleCancel = async () => {
    if (!order) return;
    setError("");

    if (!cancelReason.trim()) {
      setError("请填写取消原因");
      return;
    }

    try {
      await fetchApi(`/api/v1/return/orders/${order.id}/cancel`, {
        method: "POST",
        body: JSON.stringify({ cancelReason }),
      });

      setCancelMode(false);
      loadOrder();
    } catch (err: any) {
      setError(err.message || "取消失败");
    }
  };

  if (loading) {
    return <div className="p-8 text-center text-gray-500">加载中...</div>;
  }

  if (!order) {
    return <div className="p-8 text-center text-gray-500">退货单不存在</div>;
  }

  const canReceive = order.status === 0;
  const canCancel = order.status === 0;

  return (
    <div className="space-y-4">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate("/return")}
            className="flex items-center gap-1 text-gray-600 hover:text-gray-800"
          >
            <ArrowLeft size={20} />
            返回
          </button>
          <h2 className="text-lg font-semibold">退货单详情</h2>
        </div>
        <span className={`px-3 py-1 rounded text-sm ${STATUS_COLORS[order.status]}`}>
          {STATUS_NAMES[order.status]}
        </span>
      </div>

      {/* 基本信息 */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="font-medium text-gray-800 mb-4 flex items-center gap-2">
          <FileText size={18} />
          基本信息
        </h3>

        <div className="grid grid-cols-4 gap-4 text-sm">
          <div>
            <label className="text-gray-500">退货单号</label>
            <div className="font-mono mt-1">{order.returnNo}</div>
          </div>
          <div>
            <label className="text-gray-500">原出库单号</label>
            <div className="font-mono mt-1 text-blue-600">
              <button
                onClick={() => navigate(`/outbound/${order.originalOutboundId}`)}
                className="hover:underline"
              >
                {order.originalOutboundNo}
              </button>
            </div>
          </div>
          <div>
            <label className="text-gray-500">客户</label>
            <div className="mt-1">{order.customerName || "-"}</div>
          </div>
          <div>
            <label className="text-gray-500">仓库</label>
            <div className="mt-1">{order.warehouseName || "-"}</div>
          </div>
          <div>
            <label className="text-gray-500">退货原因</label>
            <div className="mt-1">{REASON_NAMES[order.returnReason] || "-"}</div>
          </div>
          <div>
            <label className="text-gray-500">详细说明</label>
            <div className="mt-1">{order.returnReasonText || "-"}</div>
          </div>
          <div>
            <label className="text-gray-500">预计退货数量</label>
            <div className="mt-1">{order.totalExpectedQty}</div>
          </div>
          <div>
            <label className="text-gray-500">实收数量</label>
            <div className="mt-1">{order.totalReceivedQty || 0}</div>
          </div>
        </div>

        {order.remark && (
          <div className="mt-4 text-sm">
            <label className="text-gray-500">备注</label>
            <div className="mt-1">{order.remark}</div>
          </div>
        )}
      </div>

      {/* 商品明细 */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="font-medium text-gray-800 mb-4 flex items-center gap-2">
          <Package size={18} />
          退货商品明细
        </h3>

        <table className="w-full text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-3 py-2 text-left">SKU编码</th>
              <th className="px-3 py-2 text-left">商品名称</th>
              <th className="px-3 py-2 text-center">原出库数量</th>
              <th className="px-3 py-2 text-center">预计退货</th>
              <th className="px-3 py-2 text-center">实收数量</th>
              {receiveMode && <th className="px-3 py-2 text-center">收货备注</th>}
            </tr>
          </thead>
          <tbody>
            {order.items.map((item) => (
              <tr key={item.id} className="border-b">
                <td className="px-3 py-3 font-mono">{item.skuCode}</td>
                <td className="px-3 py-3">{item.productName}</td>
                <td className="px-3 py-3 text-center">{item.originalQty}</td>
                <td className="px-3 py-3 text-center">{item.expectedQty}</td>
                <td className="px-3 py-3 text-center">
                  {receiveMode ? (
                    <input
                      type="number"
                      value={receiveItems.find((r) => r.itemId === item.id)?.receivedQty || 0}
                      onChange={(e) => {
                        const qty = parseInt(e.target.value) || 0;
                        setReceiveItems((prev) =>
                          prev.map((r) =>
                            r.itemId === item.id ? { ...r, receivedQty: Math.min(qty, item.expectedQty) } : r
                          )
                        );
                      }}
                      min={0}
                      max={item.expectedQty}
                      className="w-20 border rounded px-2 py-1 text-center"
                    />
                  ) : (
                    item.receivedQty || 0
                  )}
                </td>
                {receiveMode && (
                  <td className="px-3 py-3 text-center">
                    <input
                      type="text"
                      value={receiveItems.find((r) => r.itemId === item.id)?.remark || ""}
                      onChange={(e) => {
                        setReceiveItems((prev) =>
                          prev.map((r) =>
                            r.itemId === item.id ? { ...r, remark: e.target.value } : r
                          )
                        );
                      }}
                      placeholder="备注"
                      className="w-32 border rounded px-2 py-1"
                    />
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* 关联入库单 */}
      {order.inboundOrderNo && (
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="font-medium text-gray-800 mb-4">关联入库单</h3>
          <div className="text-sm">
            <span className="text-gray-500">入库单号: </span>
            <button
              onClick={() => navigate(`/inbound/${order.inboundOrderId}`)}
              className="text-blue-600 hover:underline font-mono"
            >
              {order.inboundOrderNo}
            </button>
            <span className="text-gray-500 ml-4">(客户退货)</span>
          </div>
        </div>
      )}

      {/* 状态时间轴 */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="font-medium text-gray-800 mb-4">处理记录</h3>
        <div className="space-y-3 text-sm">
          <div className="flex items-center gap-3">
            <div className="w-2 h-2 bg-green-500 rounded-full" />
            <span className="text-gray-500">创建退货单</span>
            <span className="text-gray-400">{order.createTime}</span>
            <span className="text-gray-400">操作人: {order.createUserName || "-"}</span>
          </div>
          {order.status >= 1 && (
            <div className="flex items-center gap-3">
              <div className="w-2 h-2 bg-blue-500 rounded-full" />
              <span className="text-gray-500">确认收货</span>
              <span className="text-gray-400">{order.receiveTime}</span>
              <span className="text-gray-400">操作人: {order.receiveUserName || "-"}</span>
            </div>
          )}
          {order.status === 2 && (
            <div className="flex items-center gap-3">
              <div className="w-2 h-2 bg-gray-500 rounded-full" />
              <span className="text-gray-500">退货完成</span>
              <span className="text-gray-400">{order.completeTime}</span>
            </div>
          )}
          {order.status === 9 && (
            <div className="flex items-center gap-3">
              <div className="w-2 h-2 bg-red-500 rounded-full" />
              <span className="text-gray-500">已取消</span>
              <span className="text-gray-400">{order.cancelReason}</span>
            </div>
          )}
        </div>
      </div>

      {/* 错误提示 */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-sm">
          {error}
        </div>
      )}

      {/* 操作按钮 */}
      <div className="flex justify-end gap-3">
        {receiveMode ? (
          <>
            <button
              onClick={() => setReceiveMode(false)}
              className="flex items-center gap-1 px-4 py-2 border rounded hover:bg-gray-50 text-sm"
            >
              <X size={16} />
              取消
            </button>
            <button
              onClick={handleReceive}
              className="flex items-center gap-1 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
            >
              <Check size={16} />
              确认收货
            </button>
          </>
        ) : cancelMode ? (
          <>
            <button
              onClick={() => setCancelMode(false)}
              className="flex items-center gap-1 px-4 py-2 border rounded hover:bg-gray-50 text-sm"
            >
              <X size={16} />
              返回
            </button>
            <div className="flex items-center gap-2">
              <input
                type="text"
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                placeholder="请输入取消原因"
                className="border rounded px-3 py-2 text-sm w-64"
              />
              <button
                onClick={handleCancel}
                className="flex items-center gap-1 px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 text-sm"
              >
                确认取消
              </button>
            </div>
          </>
        ) : (
          <>
            {canCancel && (
              <button
                onClick={() => setCancelMode(true)}
                className="flex items-center gap-1 px-4 py-2 border border-red-300 text-red-600 rounded hover:bg-red-50 text-sm"
              >
                <X size={16} />
                取消退货单
              </button>
            )}
            {canReceive && (
              <button
                onClick={() => setReceiveMode(true)}
                className="flex items-center gap-1 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
              >
                <Check size={16} />
                确认收货
              </button>
            )}
          </>
        )}
      </div>
    </div>
  );
}