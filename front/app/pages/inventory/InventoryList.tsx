import { useState, useEffect } from "react";
import { Search, Download } from "lucide-react";
import DataTable from "../../components/DataTable";
import { toast } from "sonner";

interface InventoryItem {
  id: number;
  sku: string;
  name: string;
  warehouse: string;
  location: string;
  batch: string;
  total: number;
  available: number;
  locked: number;
  expiry: string;
  status: string;
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

export default function InventoryList() {
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState("");
  const [warehouseFilter, setWarehouseFilter] = useState("全部");
  const [statusFilter, setStatusFilter] = useState("全部");

  useEffect(() => {
    fetchInventory();
  }, []);

  async function fetchInventory() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: InventoryItem[] }>("/api/v1/inventory/stocks?page=1&limit=100");
      setItems(data.list || []);
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  }

  const filteredData = items.filter((item) => {
    const matchSearch = !searchText || item.sku.includes(searchText) || item.name.includes(searchText);
    const matchWarehouse = warehouseFilter === "全部" || item.warehouse === warehouseFilter;
    const matchStatus = statusFilter === "全部" || item.status === statusFilter;
    return matchSearch && matchWarehouse && matchStatus;
  });

  const stats = {
    totalQty: items.reduce((sum, item) => sum + item.total, 0),
    availableQty: items.reduce((sum, item) => sum + item.available, 0),
    lockedQty: items.reduce((sum, item) => sum + item.locked, 0),
    alertCount: items.filter((item) => item.status !== '正常').length,
  };

  const columns = [
    { key: "sku", title: "SKU", width: "130px" },
    { key: "name", title: "商品名称", width: "160px" },
    { key: "warehouse", title: "仓库", width: "100px" },
    { key: "location", title: "库位", width: "100px" },
    { key: "batch", title: "批次号", width: "100px" },
    { key: "total", title: "总数量", width: "80px", render: (v: number) => v.toLocaleString() },
    { key: "available", title: "可用", width: "80px", render: (v: number) => <span className="text-green-600 font-semibold">{v.toLocaleString()}</span> },
    { key: "locked", title: "锁定", width: "80px", render: (v: number) => <span className="text-orange-600">{v.toLocaleString()}</span> },
    { key: "expiry", title: "过期日期", width: "100px" },
    {
      key: "status",
      title: "状态",
      width: "90px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '正常' ? 'bg-green-100 text-green-700' :
          value === '临期预警' ? 'bg-orange-100 text-orange-700' :
          value === '库存不足' ? 'bg-red-100 text-red-700' :
          'bg-gray-100 text-gray-700'
        }`}>{value}</span>
      )
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">库存查询</h2>
        <button
          onClick={() => toast.info("导出功能开发中")}
          className="px-3 py-1.5 bg-green-600 text-white rounded hover:bg-green-700 flex items-center gap-1 text-sm"
        >
          <Download size={16} />
          导出库存
        </button>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-4 gap-4">
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <div className="text-sm text-gray-500">总库存数量</div>
          <div className="text-2xl font-bold text-blue-600">{stats.totalQty.toLocaleString()}</div>
        </div>
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <div className="text-sm text-gray-500">可用数量</div>
          <div className="text-2xl font-bold text-green-600">{stats.availableQty.toLocaleString()}</div>
        </div>
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <div className="text-sm text-gray-500">锁定数量</div>
          <div className="text-2xl font-bold text-orange-600">{stats.lockedQty.toLocaleString()}</div>
        </div>
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <div className="text-sm text-gray-500">预警数量</div>
          <div className="text-2xl font-bold text-red-600">{stats.alertCount}</div>
        </div>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-4 mb-4">
          <div className="flex-1 relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="搜索SKU或商品名称"
              className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
            />
          </div>
          <select
            className="px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={warehouseFilter}
            onChange={(e) => setWarehouseFilter(e.target.value)}
          >
            <option value="全部">全部仓库</option>
            <option value="深圳自营仓">深圳自营仓</option>
            <option value="上海自营仓">上海自营仓</option>
          </select>
          <select
            className="px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="全部">全部状态</option>
            <option value="正常">正常</option>
            <option value="临期预警">临期预警</option>
            <option value="库存不足">库存不足</option>
          </select>
        </div>

        {loading ? (
          <div className="text-center py-12 text-gray-500">加载中...</div>
        ) : filteredData.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">📭</div>
            <div>暂无库存数据</div>
            <div className="text-sm text-gray-400 mt-1">API 暂未实现，后端对接后可正常使用</div>
          </div>
        ) : (
          <DataTable columns={columns} data={filteredData} />
        )}
      </div>
    </div>
  );
}