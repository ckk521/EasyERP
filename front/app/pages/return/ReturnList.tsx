import { useState, useEffect } from "react";
import { Plus, Search } from "lucide-react";
import DataTable from "../../components/DataTable";
import { toast } from "sonner";

interface ReturnOrder {
  id: number;
  code: string;
  soNo: string;
  customer: string;
  sku: string;
  qty: number;
  reason: string;
  status: string;
  createTime: string;
}

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
}

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

export default function ReturnList() {
  const [orders, setOrders] = useState<ReturnOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState("");

  useEffect(() => {
    fetchOrders();
  }, []);

  async function fetchOrders() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: ReturnOrder[] }>("/api/v1/return/orders?page=1&limit=100");
      setOrders(data.list || []);
    } catch {
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }

  const filteredData = orders.filter((o) => !searchText || o.code.includes(searchText) || o.soNo.includes(searchText));

  const columns = [
    { key: "code", title: "退货单号", width: "140px" },
    { key: "soNo", title: "销售单号", width: "130px" },
    { key: "customer", title: "客户", width: "100px" },
    { key: "sku", title: "商品SKU", width: "130px" },
    { key: "qty", title: "退货数量", width: "80px" },
    { key: "reason", title: "退货原因", width: "120px" },
    {
      key: "status", title: "状态", width: "90px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '待收货' ? 'bg-gray-100 text-gray-700' : value === '验收中' ? 'bg-blue-100 text-blue-700' : 'bg-green-100 text-green-700'
        }`}>{value}</span>
      )
    },
    { key: "createTime", title: "创建时间", width: "140px" },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">退换货管理</h2>
        <button onClick={() => toast.info("功能开发中")} className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm">
          <Plus size={16} />新建退货单
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="relative mb-4">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input type="text" placeholder="搜索退货单号或销售单号" value={searchText} onChange={(e) => setSearchText(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
        {loading ? (
          <div className="text-center py-12 text-gray-500">加载中...</div>
        ) : orders.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">↩️</div>
            <div>暂无退货单数据</div>
            <div className="text-sm text-gray-400 mt-1">API 暂未实现，后端对接后可正常使用</div>
          </div>
        ) : (
          <DataTable columns={columns} data={filteredData} />
        )}
      </div>
    </div>
  );
}