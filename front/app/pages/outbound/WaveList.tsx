import { useState, useEffect } from "react";
import { Plus } from "lucide-react";
import DataTable from "../../components/DataTable";
import { toast } from "sonner";

interface WaveOrder {
  id: number;
  code: string;
  strategy: string;
  orders: number;
  skus: number;
  qty: number;
  picker: string;
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

export default function WaveList() {
  const [waves, setWaves] = useState<WaveOrder[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchWaves();
  }, []);

  async function fetchWaves() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: WaveOrder[] }>("/api/v1/outbound/waves?page=1&limit=100");
      setWaves(data.list || []);
    } catch {
      setWaves([]);
    } finally {
      setLoading(false);
    }
  }

  const columns = [
    { key: "code", title: "波次号", width: "150px" },
    { key: "strategy", title: "策略", width: "100px" },
    { key: "orders", title: "订单数", width: "80px" },
    { key: "skus", title: "SKU数", width: "80px" },
    { key: "qty", title: "总件数", width: "80px" },
    { key: "picker", title: "拣货员", width: "100px" },
    {
      key: "status", title: "状态", width: "90px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '已完成' ? 'bg-green-100 text-green-700' : value === '拣货中' ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-700'
        }`}>{value}</span>
      )
    },
    { key: "createTime", title: "创建时间", width: "140px" },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">波次管理</h2>
        <button onClick={() => toast.info("功能开发中")} className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm">
          <Plus size={16} />创建波次
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        {loading ? (
          <div className="text-center py-12 text-gray-500">加载中...</div>
        ) : waves.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">🌊</div>
            <div>暂无波次数据</div>
            <div className="text-sm text-gray-400 mt-1">API 暂未实现，后端对接后可正常使用</div>
          </div>
        ) : (
          <DataTable columns={columns} data={waves} />
        )}
      </div>
    </div>
  );
}