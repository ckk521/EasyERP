import { useState, useEffect } from "react";
import { Search, Download } from "lucide-react";
import DataTable from "../../components/DataTable";
import { toast } from "sonner";

interface OperationLog {
  id: number;
  operateTime: string;
  username: string;
  realName: string;
  module: string;
  action: string;
  targetObject: string;
  ipAddress: string;
  result: string;
  errorMessage?: string;
  requestMethod?: string;
  requestUrl?: string;
  requestParams?: string;
  responseTime?: number;
}

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  code?: number;
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

  const response = await fetch(url, {
    ...options,
    headers,
  });

  const data: ApiResponse<T> = await response.json();

  if (!data.success) {
    throw new Error(data.message || "API Error");
  }

  return data.data as T;
}

export default function OperationLog() {
  const [logs, setLogs] = useState<OperationLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [module, setModule] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  useEffect(() => {
    fetchLogs();
  }, []);

  async function fetchLogs() {
    try {
      setLoading(true);
      const params = new URLSearchParams({ page: "1", limit: "100" });
      if (searchKeyword) params.append("keyword", searchKeyword);
      if (module) params.append("module", module);
      if (startDate) params.append("startTime", startDate + " 00:00:00");
      if (endDate) params.append("endTime", endDate + " 23:59:59");

      const data = await fetchApi<{ list: OperationLog[] }>(`${API_BASE}/system/logs/operations?${params}`);
      setLogs(data.list || []);
    } catch (error) {
      toast.error("获取操作日志失败");
      console.error(error);
    } finally {
      setLoading(false);
    }
  }

  const handleSearch = () => {
    fetchLogs();
  };

  const columns = [
    { key: "operateTime", title: "操作时间", width: "160px" },
    { key: "username", title: "操作人", width: "100px" },
    { key: "module", title: "模块", width: "100px" },
    { key: "action", title: "操作", width: "120px" },
    { key: "targetObject", title: "操作对象", width: "150px" },
    { key: "ipAddress", title: "IP地址", width: "120px" },
    {
      key: "result",
      title: "结果",
      width: "80px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '成功' || value === 'SUCCESS' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
        }`}>
          {value}
        </span>
      )
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">操作日志</h2>
        <button
          onClick={() => toast.info("导出功能开发中")}
          className="px-3 py-1.5 bg-green-600 text-white rounded hover:bg-green-700 flex items-center gap-1 text-sm"
        >
          <Download size={16} />
          导出日志
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-4 mb-4">
          <div className="flex-1 relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="搜索操作人或操作对象"
              className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            />
          </div>
          <select
            className="px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={module}
            onChange={(e) => setModule(e.target.value)}
          >
            <option value="">全部模块</option>
            <option value="用户管理">用户管理</option>
            <option value="角色管理">角色管理</option>
            <option value="入库管理">入库管理</option>
            <option value="出库管理">出库管理</option>
            <option value="库存管理">库存管理</option>
            <option value="系统配置">系统配置</option>
          </select>
          <input
            type="date"
            className="px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
          />
          <span className="self-center text-gray-400">至</span>
          <input
            type="date"
            className="px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
          />
          <button
            onClick={handleSearch}
            className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
          >
            搜索
          </button>
        </div>
        {loading ? (
          <div className="text-center py-8 text-gray-500">加载中...</div>
        ) : (
          <DataTable columns={columns} data={logs} />
        )}
      </div>
    </div>
  );
}