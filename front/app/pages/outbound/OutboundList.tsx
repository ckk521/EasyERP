import { useState, useEffect } from "react";
import { Plus, Search, Eye } from "lucide-react";
import { useNavigate } from "react-router";
import DataTable from "../../components/DataTable";
import { toast } from "sonner";

interface OutboundOrder {
  id: number;
  code: string;
  soNo: string;
  customer: string;
  warehouse: string;
  qty: number;
  status: string;
  priority: string;
  createTime: string;
  shipTime?: string;
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

const statusOptions = ["全部", "待分配", "待拣货", "拣货中", "打包中", "已出库", "已取消"];

export default function OutboundList() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<OutboundOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState("");
  const [statusFilter, setStatusFilter] = useState("全部");

  useEffect(() => {
    fetchOrders();
  }, []);

  async function fetchOrders() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: OutboundOrder[] }>("/api/v1/outbound/orders?page=1&limit=100");
      setOrders(data.list || []);
    } catch {
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }

  const filteredData = orders.filter((order) => {
    const matchSearch = !searchText || order.code.includes(searchText) || order.soNo.includes(searchText);
    const matchStatus = statusFilter === "全部" || order.status === statusFilter;
    return matchSearch && matchStatus;
  });

  const columns = [
    { key: "code", title: "出库单号", width: "160px" },
    { key: "soNo", title: "销售单号", width: "130px" },
    { key: "customer", title: "客户", width: "120px" },
    { key: "warehouse", title: "仓库", width: "100px" },
    { key: "qty", title: "数量", width: "80px", render: (v: number) => v.toLocaleString() },
    {
      key: "priority",
      title: "优先级",
      width: "80px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '紧急' ? 'bg-red-100 text-red-700' :
          value === '高' ? 'bg-orange-100 text-orange-700' :
          'bg-gray-100 text-gray-700'
        }`}>{value}</span>
      )
    },
    {
      key: "status",
      title: "状态",
      width: "90px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '已出库' ? 'bg-green-100 text-green-700' :
          value === '待分配' || value === '待拣货' ? 'bg-yellow-100 text-yellow-700' :
          value === '拣货中' || value === '打包中' ? 'bg-blue-100 text-blue-700' :
          value === '已取消' ? 'bg-gray-100 text-gray-700' :
          'bg-gray-100 text-gray-700'
        }`}>{value}</span>
      )
    },
    { key: "createTime", title: "创建时间", width: "140px" },
    {
      key: "actions",
      title: "操作",
      width: "80px",
      render: (_: any, order: OutboundOrder) => (
        <button
          onClick={() => navigate(`/outbound/detail/${order.id}`)}
          className="p-1 hover:bg-gray-100 rounded text-blue-600"
          title="查看详情"
        >
          <Eye size={14} />
        </button>
      )
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">出库管理</h2>
        <button
          onClick={() => toast.info("功能开发中")}
          className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm"
        >
          <Plus size={16} />
          新建出库单
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-4 mb-4">
          <div className="flex-1 relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="搜索出库单号或销售单号"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div className="flex gap-2">
            {statusOptions.slice(1).map((status) => (
              <button
                key={status}
                onClick={() => setStatusFilter(status)}
                className={`px-3 py-1.5 rounded text-sm ${
                  statusFilter === status ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {status}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <div className="text-center py-12 text-gray-500">加载中...</div>
        ) : filteredData.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">📭</div>
            <div>暂无出库单数据</div>
            <div className="text-sm text-gray-400 mt-1">API 暂未实现，后端对接后可正常使用</div>
          </div>
        ) : (
          <DataTable columns={columns} data={filteredData} />
        )}
      </div>
    </div>
  );
}