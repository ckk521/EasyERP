import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { Package, Plus, Search, Eye } from "lucide-react";

interface ReturnOrder {
  id: number;
  returnNo: string;
  originalOutboundNo: string;
  customerName: string;
  totalExpectedQty: number;
  totalReceivedQty: number;
  status: number;
  statusName: string;
  returnReason: number;
  returnReasonName: string;
  createTime: string;
  inboundOrderNo?: string;
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

export default function ReturnOrderList() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<ReturnOrder[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [keyword, setKeyword] = useState("");
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const limit = 10;

  const loadOrders = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.set("page", String(page));
      params.set("limit", String(limit));
      if (statusFilter) params.set("status", statusFilter);
      if (keyword) params.set("keyword", keyword);

      const data = await fetchApi<{ records: ReturnOrder[]; total: number }>(
        `/api/v1/return/orders?${params.toString()}`
      );
      setOrders(data.records || []);
      setTotal(data.total || 0);
    } catch (error) {
      console.error("Failed to load return orders:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrders();
  }, [page, statusFilter]);

  const handleSearch = () => {
    setPage(1);
    loadOrders();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      handleSearch();
    }
  };

  const totalPages = Math.ceil(total / limit);

  return (
    <div className="space-y-4">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Package className="w-5 h-5 text-gray-600" />
          <h2 className="text-lg font-semibold">退货单管理</h2>
        </div>
        <button
          onClick={() => navigate("/return/create")}
          className="flex items-center gap-1 px-3 py-1.5 bg-blue-600 text-white rounded-md hover:bg-blue-700 text-sm"
        >
          <Plus size={16} />
          创建退货单
        </button>
      </div>

      {/* 筛选区域 */}
      <div className="bg-white rounded-lg shadow p-4">
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-2">
            <label className="text-sm text-gray-600">状态:</label>
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(1);
              }}
              className="border rounded px-2 py-1 text-sm"
            >
              <option value="">全部</option>
              <option value="0">待收货</option>
              <option value="1">已收货</option>
              <option value="2">已完成</option>
              <option value="9">已取消</option>
            </select>
          </div>

          <div className="flex items-center gap-2 flex-1">
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="搜索退货单号/出库单号"
              className="border rounded px-3 py-1 text-sm flex-1 max-w-xs"
            />
            <button
              onClick={handleSearch}
              className="flex items-center gap-1 px-3 py-1 bg-gray-100 text-gray-700 rounded hover:bg-gray-200 text-sm"
            >
              <Search size={14} />
              查询
            </button>
          </div>
        </div>
      </div>

      {/* 列表 */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b">
            <tr>
              <th className="px-4 py-3 text-left font-medium text-gray-600">退货单号</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">原出库单号</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">客户</th>
              <th className="px-4 py-3 text-center font-medium text-gray-600">退货数量</th>
              <th className="px-4 py-3 text-center font-medium text-gray-600">退货原因</th>
              <th className="px-4 py-3 text-center font-medium text-gray-600">状态</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">创建时间</th>
              <th className="px-4 py-3 text-center font-medium text-gray-600">操作</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-center text-gray-500">
                  加载中...
                </td>
              </tr>
            ) : orders.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-center text-gray-500">
                  暂无退货单
                </td>
              </tr>
            ) : (
              orders.map((order) => (
                <tr key={order.id} className="border-b hover:bg-gray-50">
                  <td className="px-4 py-3 font-mono text-blue-600">{order.returnNo}</td>
                  <td className="px-4 py-3 font-mono">{order.originalOutboundNo}</td>
                  <td className="px-4 py-3">{order.customerName || "-"}</td>
                  <td className="px-4 py-3 text-center">{order.totalExpectedQty}</td>
                  <td className="px-4 py-3 text-center">
                    {REASON_NAMES[order.returnReason] || "-"}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <span className={`px-2 py-0.5 rounded text-xs ${STATUS_COLORS[order.status] || "bg-gray-100"}`}>
                      {STATUS_NAMES[order.status] || "未知"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-500">
                    {new Date(order.createTime).toLocaleString("zh-CN")}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <button
                      onClick={() => navigate(`/return/${order.id}`)}
                      className="text-blue-600 hover:text-blue-700 flex items-center gap-1 justify-center"
                    >
                      <Eye size={14} />
                      查看
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* 分页 */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t">
            <div className="text-sm text-gray-500">
              共 {total} 条记录
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={page === 1}
                className="px-3 py-1 border rounded text-sm disabled:opacity-50"
              >
                上一页
              </button>
              <span className="text-sm">
                {page} / {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                disabled={page === totalPages}
                className="px-3 py-1 border rounded text-sm disabled:opacity-50"
              >
                下一页
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
