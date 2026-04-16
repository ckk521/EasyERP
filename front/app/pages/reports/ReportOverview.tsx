import { useState, useEffect } from "react";
import { BarChart, Bar, LineChart, Line, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from "recharts";
import { Download } from "lucide-react";
import { toast } from "sonner";

interface DashboardData {
  inboundOutboundData: { month: string; inbound: number; outbound: number }[];
  turnoverData: { category: string; turnover: number }[];
  warehouseUtilization: { name: string; value: number }[];
  orderStatusData: { status: string; count: number }[];
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

const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6'];

export default function ReportOverview() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  async function fetchDashboardData() {
    try {
      setLoading(true);
      const result = await fetchApi<DashboardData>("/api/v1/reports/dashboard");
      setData(result);
    } catch {
      // API not available, use empty data
      setData({
        inboundOutboundData: [],
        turnoverData: [],
        warehouseUtilization: [],
        orderStatusData: [],
      });
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="p-4">
        <div className="text-center py-12 text-gray-500">加载中...</div>
      </div>
    );
  }

  if (!data || (data.inboundOutboundData.length === 0 && data.turnoverData.length === 0)) {
    return (
      <div className="p-4">
        <div className="text-center py-12 text-gray-500">
          <div className="text-4xl mb-2">📊</div>
          <div>暂无报表数据</div>
          <div className="text-sm text-gray-400 mt-1">API 暂未实现，后端对接后可正常使用</div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">数据报表</h2>
        <button onClick={() => toast.info("导出功能开发中")} className="px-3 py-1.5 bg-green-600 text-white rounded hover:bg-green-700 flex items-center gap-1 text-sm">
          <Download size={16} />导出报表
        </button>
      </div>

      <div className="grid grid-cols-2 gap-4">
        {/* 出入库趋势 */}
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3">出入库趋势</h3>
          <ResponsiveContainer width="100%" height={250}>
            <LineChart data={data.inboundOutboundData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="month" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="inbound" stroke="#3B82F6" name="入库" />
              <Line type="monotone" dataKey="outbound" stroke="#10B981" name="出库" />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* 库存周转 */}
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3">库存周转率</h3>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={data.turnoverData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="category" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="turnover" fill="#3B82F6" name="周转天数" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* 仓库使用率 */}
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3">仓库使用率</h3>
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie
                data={data.warehouseUtilization}
                cx="50%"
                cy="50%"
                innerRadius={60}
                outerRadius={80}
                fill="#8884d8"
                paddingAngle={5}
                dataKey="value"
              >
                {data.warehouseUtilization.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* 订单状态分布 */}
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3">订单状态分布</h3>
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie
                data={data.orderStatusData}
                cx="50%"
                cy="50%"
                outerRadius={80}
                fill="#8884d8"
                paddingAngle={5}
                dataKey="count"
                nameKey="status"
              >
                {data.orderStatusData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}