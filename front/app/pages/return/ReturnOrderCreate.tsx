import { useState } from "react";
import { useNavigate } from "react-router";
import { ArrowLeft, Search, Check, X } from "lucide-react";

interface OutboundOrder {
  id: number;
  orderNo: string;
  orderType: number;
  orderTypeName: string;
  customerName: string;
  targetWarehouseName: string;
  warehouseName: string;
  items: OutboundItem[];
}

interface OutboundItem {
  id: number;
  skuCode: string;
  productName: string;
  barcode: string;
  qty: number;
  pickedQty: number;
  packedQty: number;
}

interface ReturnItem {
  skuCode: string;
  productName: string;
  barcode: string;
  originalQty: number;
  expectedQty: number;
  selected: boolean;
}

const REASON_OPTIONS = [
  { value: 1, label: "质量问题（破损/功能缺陷）" },
  { value: 2, label: "发错商品" },
  { value: 3, label: "数量不符" },
  { value: 4, label: "客户不满意" },
  { value: 5, label: "7天无理由退货" },
  { value: 6, label: "其他" },
];

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

export default function ReturnOrderCreate() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [searching, setSearching] = useState(false);
  const [outboundNo, setOutboundNo] = useState("");
  const [outboundOrder, setOutboundOrder] = useState<OutboundOrder | null>(null);
  const [returnReason, setReturnReason] = useState<number>(0);
  const [returnReasonText, setReturnReasonText] = useState("");
  const [remark, setRemark] = useState("");
  const [items, setItems] = useState<ReturnItem[]>([]);
  const [error, setError] = useState("");

  const searchOutboundOrder = async () => {
    if (!outboundNo.trim()) {
      setError("请输入出库单号");
      return;
    }

    setSearching(true);
    setError("");
    setOutboundOrder(null);
    setItems([]);

    try {
      // 先查询出库单
      const orderData = await fetchApi<OutboundOrder>(
        `/api/v1/outbound/orders/no/${outboundNo.trim()}`
      );

      if (!orderData) {
        setError("未找到该出库单");
        return;
      }

      // 查询出库单明细
      const itemsData = await fetchApi<OutboundItem[]>(
        `/api/v1/outbound/orders/${orderData.id}/items`
      );

      setOutboundOrder({
        ...orderData,
        items: itemsData || [],
      });

      // 初始化退货商品列表
      const returnItems: ReturnItem[] = (itemsData || []).map((item) => ({
        skuCode: item.skuCode,
        productName: item.productName,
        barcode: item.barcode,
        originalQty: item.qty,
        expectedQty: 0,
        selected: false,
      }));
      setItems(returnItems);
    } catch (err: any) {
      setError(err.message || "查询出库单失败");
    } finally {
      setSearching(false);
    }
  };

  const toggleItem = (index: number) => {
    setItems((prev) =>
      prev.map((item, i) =>
        i === index ? { ...item, selected: !item.selected, expectedQty: !item.selected ? item.originalQty : 0 } : item
      )
    );
  };

  const updateQty = (index: number, qty: number) => {
    setItems((prev) =>
      prev.map((item, i) => {
        if (i === index) {
          const validQty = Math.min(Math.max(0, qty), item.originalQty);
          return { ...item, expectedQty: validQty };
        }
        return item;
      })
    );
  };

  const handleSubmit = async () => {
    setError("");

    // 校验
    if (!outboundOrder) {
      setError("请选择原出库单");
      return;
    }
    if (!returnReason) {
      setError("请选择退货原因");
      return;
    }
    const selectedItems = items.filter((item) => item.selected && item.expectedQty > 0);
    if (selectedItems.length === 0) {
      setError("请选择退货商品并填写退货数量");
      return;
    }

    setLoading(true);
    try {
      await fetchApi("/api/v1/return/orders", {
        method: "POST",
        body: JSON.stringify({
          originalOutboundId: outboundOrder.id,
          originalOutboundNo: outboundOrder.orderNo,
          returnReason,
          returnReasonText,
          remark,
          items: selectedItems.map((item) => ({
            skuCode: item.skuCode,
            productName: item.productName,
            barcode: item.barcode,
            originalQty: item.originalQty,
            expectedQty: item.expectedQty,
          })),
        }),
      });

      navigate("/return");
    } catch (err: any) {
      setError(err.message || "创建退货单失败");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      {/* 页面标题 */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate("/return")}
          className="flex items-center gap-1 text-gray-600 hover:text-gray-800"
        >
          <ArrowLeft size={20} />
          返回
        </button>
        <h2 className="text-lg font-semibold">创建退货单</h2>
      </div>

      {/* 基本信息 */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="font-medium text-gray-800 mb-4">基本信息</h3>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm text-gray-600 mb-1">
              原出库单号 <span className="text-red-500">*</span>
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={outboundNo}
                onChange={(e) => setOutboundNo(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && searchOutboundOrder()}
                placeholder="输入出库单号"
                className="flex-1 border rounded px-3 py-2 text-sm"
              />
              <button
                onClick={searchOutboundOrder}
                disabled={searching}
                className="flex items-center gap-1 px-3 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm disabled:opacity-50"
              >
                <Search size={16} />
                {searching ? "查询中..." : "查询"}
              </button>
            </div>
          </div>

          {outboundOrder && (
            <>
              <div>
                <label className="block text-sm text-gray-600 mb-1">出库类型</label>
                <div className="border rounded px-3 py-2 bg-gray-50 text-sm">
                  {outboundOrder.orderTypeName || "-"}
                </div>
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">
                  {outboundOrder.orderType === 2 ? "目标仓库" : "客户名称"}
                </label>
                <div className="border rounded px-3 py-2 bg-gray-50 text-sm">
                  {outboundOrder.orderType === 2
                    ? (outboundOrder.targetWarehouseName || "-")
                    : (outboundOrder.customerName || "-")}
                </div>
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">发货仓库</label>
                <div className="border rounded px-3 py-2 bg-gray-50 text-sm">
                  {outboundOrder.warehouseName || "-"}
                </div>
              </div>
            </>
          )}
        </div>

        {outboundOrder && (
          <div className="grid grid-cols-2 gap-4 mt-4">
            <div>
              <label className="block text-sm text-gray-600 mb-1">
                退货原因 <span className="text-red-500">*</span>
              </label>
              <select
                value={returnReason}
                onChange={(e) => setReturnReason(Number(e.target.value))}
                className="w-full border rounded px-3 py-2 text-sm"
              >
                <option value={0}>请选择退货原因</option>
                {REASON_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm text-gray-600 mb-1">详细说明</label>
              <input
                type="text"
                value={returnReasonText}
                onChange={(e) => setReturnReasonText(e.target.value)}
                placeholder="退货原因详细说明（可选）"
                className="w-full border rounded px-3 py-2 text-sm"
              />
            </div>
          </div>
        )}

        {outboundOrder && (
          <div className="mt-4">
            <label className="block text-sm text-gray-600 mb-1">备注</label>
            <input
              type="text"
              value={remark}
              onChange={(e) => setRemark(e.target.value)}
              placeholder="备注信息（可选）"
              className="w-full border rounded px-3 py-2 text-sm"
            />
          </div>
        )}
      </div>

      {/* 退货商品 */}
      {outboundOrder && (
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="font-medium text-gray-800 mb-4">退货商品</h3>

          <table className="w-full text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-3 py-2 text-left">选择</th>
                <th className="px-3 py-2 text-left">SKU编码</th>
                <th className="px-3 py-2 text-left">商品名称</th>
                <th className="px-3 py-2 text-center">原出库数量</th>
                <th className="px-3 py-2 text-center">退货数量</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item, index) => (
                <tr key={index} className="border-b">
                  <td className="px-3 py-3">
                    <input
                      type="checkbox"
                      checked={item.selected}
                      onChange={() => toggleItem(index)}
                      className="w-4 h-4"
                    />
                  </td>
                  <td className="px-3 py-3 font-mono">{item.skuCode}</td>
                  <td className="px-3 py-3">{item.productName}</td>
                  <td className="px-3 py-3 text-center">{item.originalQty}</td>
                  <td className="px-3 py-3 text-center">
                    <input
                      type="number"
                      value={item.expectedQty}
                      onChange={(e) => updateQty(index, parseInt(e.target.value) || 0)}
                      disabled={!item.selected}
                      min={0}
                      max={item.originalQty}
                      className="w-20 border rounded px-2 py-1 text-center disabled:bg-gray-100"
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* 错误提示 */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-sm">
          {error}
        </div>
      )}

      {/* 操作按钮 */}
      <div className="flex justify-end gap-3">
        <button
          onClick={() => navigate("/return")}
          className="flex items-center gap-1 px-4 py-2 border rounded hover:bg-gray-50 text-sm"
        >
          <X size={16} />
          取消
        </button>
        <button
          onClick={handleSubmit}
          disabled={loading || !outboundOrder}
          className="flex items-center gap-1 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm disabled:opacity-50"
        >
          <Check size={16} />
          {loading ? "提交中..." : "提交退货单"}
        </button>
      </div>
    </div>
  );
}
