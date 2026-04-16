import { useState, useEffect } from "react";
import { Plus, Search, Eye } from "lucide-react";
import { useNavigate } from "react-router";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Textarea } from "../../components/ui/textarea";
import { toast } from "sonner";

interface InboundOrder {
  id: number;
  code: string;
  type: string;
  poNo: string;
  supplier: string;
  warehouse: string;
  qty: number;
  status: string;
  createTime: string;
  completeTime?: string;
}

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
}

const API_BASE = "/api/v1";

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

const statusOptions = ["全部", "待收货", "收货中", "验收中", "待上架", "已完成"];

export default function InboundList() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<InboundOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState("");
  const [statusFilter, setStatusFilter] = useState("全部");
  const [typeFilter, setTypeFilter] = useState("全部");
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formData, setFormData] = useState({
    type: "",
    poNo: "",
    supplier: "",
    warehouse: "",
    expectedDate: "",
    remark: "",
  });

  useEffect(() => {
    fetchOrders();
  }, []);

  async function fetchOrders() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: InboundOrder[] }>("/api/v1/inbound/orders?page=1&limit=100");
      setOrders(data.list || []);
    } catch (error) {
      // API 暂未实现，显示空状态
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }

  const filteredData = orders.filter((order) => {
    const matchSearch = !searchText || order.code.includes(searchText) || order.supplier.includes(searchText);
    const matchStatus = statusFilter === "全部" || order.status === statusFilter;
    const matchType = typeFilter === "全部" || order.type === typeFilter;
    return matchSearch && matchStatus && matchType;
  });

  const columns = [
    { key: "code", title: "入库单号", width: "160px" },
    { key: "type", title: "类型", width: "100px" },
    { key: "poNo", title: "采购单号", width: "140px" },
    { key: "supplier", title: "供应商", width: "120px" },
    { key: "warehouse", title: "仓库", width: "100px" },
    { key: "qty", title: "数量", width: "80px", render: (v: number) => v.toLocaleString() },
    {
      key: "status",
      title: "状态",
      width: "90px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '已完成' ? 'bg-green-100 text-green-700' :
          value === '待收货' || value === '待上架' ? 'bg-yellow-100 text-yellow-700' :
          value === '收货中' || value === '验收中' ? 'bg-blue-100 text-blue-700' :
          'bg-gray-100 text-gray-700'
        }`}>{value}</span>
      )
    },
    { key: "createTime", title: "创建时间", width: "140px" },
    {
      key: "actions",
      title: "操作",
      width: "100px",
      render: (_: any, order: InboundOrder) => (
        <button
          onClick={() => navigate(`/inbound/detail/${order.id}`)}
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
        <h2 className="text-lg font-semibold">入库管理</h2>
        <button
          onClick={() => toast.info("功能开发中")}
          className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm"
        >
          <Plus size={16} />
          新建入库单
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-4 mb-4">
          <div className="flex-1 relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="搜索入库单号或供应商"
              className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
            />
          </div>
          <select
            className="px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            {statusOptions.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
          <select
            className="px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
          >
            <option value="全部">全部类型</option>
            <option value="采购入库">采购入库</option>
            <option value="退货入库">退货入库</option>
            <option value="调拨入库">调拨入库</option>
          </select>
        </div>

        {loading ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">📦</div>
            <div>加载中...</div>
          </div>
        ) : filteredData.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">📭</div>
            <div>暂无入库单数据</div>
            <div className="text-sm text-gray-400 mt-1">API 暂未实现，后端对接后可正常使用</div>
          </div>
        ) : (
          <DataTable columns={columns} data={filteredData} />
        )}
      </div>
    </div>
  );
}