import { useState, useEffect } from "react";
import { Plus, Search, Grid3x3 } from "lucide-react";
import DataTable from "../../components/DataTable";
import { toast } from "sonner";

interface Location {
  id: number;
  code: string;
  warehouse: string;
  zone: string;
  type: string;
  size: string;
  weight: string;
  status: string;
  current: string;
  qty: number;
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

export default function LocationList() {
  const [locations, setLocations] = useState<Location[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState("");
  const [warehouseFilter, setWarehouseFilter] = useState("深圳自营仓");
  const [statusFilter, setStatusFilter] = useState("全部");
  const [viewMode, setViewMode] = useState<"table" | "grid">("table");

  useEffect(() => {
    fetchLocations();
  }, []);

  async function fetchLocations() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: Location[] }>("/api/v1/base/locations?page=1&limit=100");
      setLocations(data.list || []);
    } catch {
      setLocations([]);
    } finally {
      setLoading(false);
    }
  }

  const filteredData = locations.filter((loc) => {
    const matchSearch = !searchText || loc.code.includes(searchText) || loc.zone.includes(searchText);
    const matchWarehouse = loc.warehouse === warehouseFilter;
    const matchStatus = statusFilter === "全部" || loc.status === statusFilter;
    return matchSearch && matchWarehouse && matchStatus;
  });

  const stats = {
    total: filteredData.length,
    available: filteredData.filter((l) => l.status === '空闲').length,
    occupied: filteredData.filter((l) => l.status === '占用').length,
    locked: filteredData.filter((l) => l.status === '锁定').length,
  };

  const columns = [
    { key: "code", title: "库位编码", width: "120px" },
    { key: "warehouse", title: "所属仓库", width: "120px" },
    { key: "zone", title: "所属库区", width: "120px" },
    {
      key: "type", title: "库位类型", width: "100px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '标准位' ? 'bg-blue-100 text-blue-700' : value === '大件位' ? 'bg-purple-100 text-purple-700' : value === '冷藏位' ? 'bg-cyan-100 text-cyan-700' : 'bg-yellow-100 text-yellow-700'
        }`}>{value}</span>
      )
    },
    { key: "size", title: "尺寸", width: "120px" },
    { key: "weight", title: "承重", width: "80px" },
    {
      key: "status", title: "状态", width: "80px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '空闲' ? 'bg-green-100 text-green-700' : value === '占用' ? 'bg-blue-100 text-blue-700' : value === '锁定' ? 'bg-orange-100 text-orange-700' : 'bg-gray-100 text-gray-700'
        }`}>{value}</span>
      )
    },
    { key: "current", title: "当前商品", width: "100px" },
    { key: "qty", title: "数量", width: "80px" },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">库位管理</h2>
        <div className="flex gap-2">
          <button onClick={() => setViewMode(viewMode === "table" ? "grid" : "table")}
            className="px-3 py-1.5 bg-gray-100 text-gray-700 rounded hover:bg-gray-200 flex items-center gap-1 text-sm">
            <Grid3x3 size={16} />{viewMode === "table" ? "网格视图" : "表格视图"}
          </button>
          <button onClick={() => toast.info("功能开发中")} className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm">
            <Plus size={16} />批量生成
          </button>
        </div>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-4 items-center">
          <div className="flex-1 relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input type="text" placeholder="搜索库位编码或库区" value={searchText} onChange={(e) => setSearchText(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <select value={warehouseFilter} onChange={(e) => setWarehouseFilter(e.target.value)}
            className="px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option>深圳自营仓</option>
            <option>上海自营仓</option>
          </select>
          <div className="flex gap-2">
            {["全部", "空闲", "占用", "锁定"].map((status) => (
              <button key={status} onClick={() => setStatusFilter(status)}
                className={`px-3 py-1.5 rounded text-sm ${statusFilter === status ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}`}>
                {status}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-4 gap-4">
        <div className="bg-white rounded-lg p-3 border border-gray-200"><div className="text-xs text-gray-500 mb-1">总库位数</div><div className="text-2xl font-bold">{stats.total}</div></div>
        <div className="bg-white rounded-lg p-3 border border-gray-200"><div className="text-xs text-gray-500 mb-1">空闲</div><div className="text-2xl font-bold text-green-600">{stats.available}</div></div>
        <div className="bg-white rounded-lg p-3 border border-gray-200"><div className="text-xs text-gray-500 mb-1">占用</div><div className="text-2xl font-bold text-blue-600">{stats.occupied}</div></div>
        <div className="bg-white rounded-lg p-3 border border-gray-200"><div className="text-xs text-gray-500 mb-1">锁定</div><div className="text-2xl font-bold text-orange-600">{stats.locked}</div></div>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        {loading ? (
          <div className="text-center py-12 text-gray-500">加载中...</div>
        ) : filteredData.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">📍</div>
            <div>暂无库位数据</div>
            <div className="text-sm text-gray-400 mt-1">API 暂未实现，后端对接后可正常使用</div>
          </div>
        ) : viewMode === "table" ? (
          <DataTable columns={columns} data={filteredData} />
        ) : (
          <div className="grid grid-cols-8 gap-2">
            {filteredData.map((loc) => (
              <div key={loc.id}
                className={`p-2 rounded border-2 text-center cursor-pointer hover:shadow-md transition-shadow ${
                  loc.status === '空闲' ? 'border-green-500 bg-green-50' : loc.status === '占用' ? 'border-blue-500 bg-blue-50' : loc.status === '锁定' ? 'border-orange-500 bg-orange-50' : 'border-gray-300 bg-gray-50'
                }`}>
                <div className="text-xs font-semibold mb-1">{loc.code}</div>
                <div className="text-xs text-gray-600">{loc.status}</div>
                {loc.current !== '-' && <div className="text-xs text-gray-500 mt-1 truncate">{loc.current}</div>}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}