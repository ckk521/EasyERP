import { useState, useEffect, useCallback } from "react";
import { Search, Download, RefreshCw, Package, AlertTriangle, Clock, X } from "lucide-react";
import DataTable from "../../components/DataTable";
import { toast } from "sonner";

// 库存项接口定义
interface InventoryItem {
  id: number;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  zoneId?: number;
  zoneCode?: string;
  zoneName?: string;
  locationId: number;
  locationCode: string;
  productId: number;
  skuCode: string;
  productName: string;
  barcode?: string;
  batchNo: string;
  productionDate?: string;
  expiryDate?: string;
  qty: number;
  availableQty: number;
  lockedQty: number;
  expiryStatus: number;
  expiryStatusName: string;
  remainingDays?: number;
  inboundTime?: string;
  inboundOrderNo?: string;
  updateTime?: string;
}

// 查询条件接口
interface QueryParams {
  page: number;
  limit: number;
  skuCode?: string;
  productName?: string;
  barcode?: string;
  warehouseId?: number;
  zoneId?: number;
  batchNo?: string;
  expiryStatus?: number;
}

// API响应接口
interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
}

// 分页响应
interface PaginatedResponse {
  list: InventoryItem[];
  total: number;
  page: number;
  limit: number;
}

// 统计汇总
interface InventorySummary {
  totalSku: number;
  totalQty: number;
  totalAvailableQty: number;
  totalLockedQty: number;
  normalCount: number;
  warningCount: number;
  nearExpiryCount: number;
  expiredCount: number;
}

// 仓库选项
interface Warehouse {
  id: number;
  name: string;
  code: string;
}

// 效期状态选项
const expiryStatusOptions = [
  { value: 0, label: "正常" },
  { value: 1, label: "效期预警" },
  { value: 2, label: "临期" },
  { value: 3, label: "已过期" },
];

// 效期状态样式
const getExpiryStatusStyle = (status: number) => {
  switch (status) {
    case 0:
      return "bg-green-100 text-green-700";
    case 1:
      return "bg-yellow-100 text-yellow-700";
    case 2:
      return "bg-red-100 text-red-700";
    case 3:
      return "bg-gray-100 text-gray-700";
    default:
      return "bg-gray-100 text-gray-700";
  }
};

// 效期状态图标
const getExpiryStatusIcon = (status: number) => {
  switch (status) {
    case 1:
      return "⚠";
    case 2:
      return "🔴";
    case 3:
      return "⚫";
    default:
      return "";
  }
};

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
  // 数据状态
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [summary, setSummary] = useState<InventorySummary | null>(null);

  // 查询条件
  const [queryParams, setQueryParams] = useState<QueryParams>({
    page: 1,
    limit: 20,
  });

  // 筛选输入
  const [keyword, setKeyword] = useState("");
  const [selectedWarehouseId, setSelectedWarehouseId] = useState<number | undefined>();
  const [selectedExpiryStatus, setSelectedExpiryStatus] = useState<number | undefined>();
  const [batchNo, setBatchNo] = useState("");

  // 下拉选项
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);

  // 分页状态
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);

  // 加载仓库列表
  useEffect(() => {
    async function loadWarehouses() {
      try {
        const data = await fetchApi<{ list: Warehouse[] }>("/api/v1/base/warehouses?page=1&limit=100");
        setWarehouses(data.list || []);
      } catch {
        // 忽略错误
      }
    }
    loadWarehouses();
  }, []);

  // 加载库存数据
  const fetchInventory = useCallback(async () => {
    try {
      setLoading(true);

      // 构建查询参数
      const params = new URLSearchParams();
      params.append("page", currentPage.toString());
      params.append("limit", queryParams.limit.toString());

      if (keyword) {
        // 判断是SKU还是商品名称
        if (/^[A-Za-z0-9-]+$/.test(keyword) && keyword.toUpperCase().startsWith("SKU")) {
          params.append("skuCode", keyword);
        } else if (/^\d+$/.test(keyword) && keyword.length >= 8) {
          // 可能是条码
          params.append("barcode", keyword);
        } else {
          params.append("productName", keyword);
        }
      }
      if (selectedWarehouseId) {
        params.append("warehouseId", selectedWarehouseId.toString());
      }
      if (selectedExpiryStatus !== undefined) {
        params.append("expiryStatus", selectedExpiryStatus.toString());
      }
      if (batchNo) {
        params.append("batchNo", batchNo);
      }

      const data = await fetchApi<PaginatedResponse>(`/api/v1/inventory/list?${params.toString()}`);
      setItems(data.list || []);
      setTotal(data.total || 0);
    } catch {
      setItems([]);
      setTotal(0);
      toast.error("加载库存数据失败");
    } finally {
      setLoading(false);
    }
  }, [currentPage, queryParams.limit, keyword, selectedWarehouseId, selectedExpiryStatus, batchNo]);

  // 加载库存汇总
  const fetchSummary = useCallback(async () => {
    try {
      const params = new URLSearchParams();
      if (selectedWarehouseId) {
        params.append("warehouseId", selectedWarehouseId.toString());
      }
      const data = await fetchApi<InventorySummary>(`/api/v1/inventory/summary?${params.toString()}`);
      setSummary(data);
    } catch {
      setSummary(null);
    }
  }, [selectedWarehouseId]);

  useEffect(() => {
    fetchInventory();
    fetchSummary();
  }, [fetchInventory, fetchSummary]);

  // 重置筛选条件
  const handleReset = () => {
    setKeyword("");
    setSelectedWarehouseId(undefined);
    setSelectedExpiryStatus(undefined);
    setBatchNo("");
    setCurrentPage(1);
  };

  // 导出功能
  const handleExport = async () => {
    try {
      toast.info("正在导出库存数据...");

      const params = new URLSearchParams();
      params.append("page", "1");
      params.append("limit", "10000");

      if (keyword) {
        if (/^[A-Za-z0-9-]+$/.test(keyword) && keyword.toUpperCase().startsWith("SKU")) {
          params.append("skuCode", keyword);
        } else if (/^\d+$/.test(keyword) && keyword.length >= 8) {
          params.append("barcode", keyword);
        } else {
          params.append("productName", keyword);
        }
      }
      if (selectedWarehouseId) {
        params.append("warehouseId", selectedWarehouseId.toString());
      }
      if (selectedExpiryStatus !== undefined) {
        params.append("expiryStatus", selectedExpiryStatus.toString());
      }

      const data = await fetchApi<PaginatedResponse>(`/api/v1/inventory/list?${params.toString()}`);

      // 生成CSV内容
      const headers = ["SKU编码", "商品名称", "条码", "仓库", "库位", "批次号", "总数量", "可用数量", "锁定数量", "效期状态", "过期日期"];
      const rows = data.list.map(item => [
        item.skuCode,
        item.productName,
        item.barcode || "",
        item.warehouseName,
        item.locationCode,
        item.batchNo,
        item.qty,
        item.availableQty,
        item.lockedQty,
        item.expiryStatusName,
        item.expiryDate || "",
      ]);

      const csvContent = [headers, ...rows].map(row => row.join(",")).join("\n");
      const blob = new Blob(["﻿" + csvContent], { type: "text/csv;charset=utf-8;" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `库存查询_${new Date().toISOString().slice(0, 10)}.csv`;
      link.click();
      URL.revokeObjectURL(url);

      toast.success(`成功导出 ${data.list.length} 条库存记录`);
    } catch {
      toast.error("导出失败");
    }
  };

  // 列定义
  const columns = [
    {
      key: "skuCode",
      title: "SKU编码",
      width: "120px",
      render: (value: string) => (
        <span className="font-mono text-blue-600 hover:underline cursor-pointer">{value}</span>
      )
    },
    {
      key: "productName",
      title: "商品名称",
      width: "200px",
      render: (value: string) => (
        <span className="truncate" title={value}>{value}</span>
      )
    },
    {
      key: "warehouseName",
      title: "仓库",
      width: "100px",
    },
    {
      key: "locationCode",
      title: "库位",
      width: "120px",
      render: (value: string) => (
        <span className="font-mono text-sm">{value}</span>
      )
    },
    {
      key: "batchNo",
      title: "批次号",
      width: "150px",
      render: (value: string) => (
        <span className="font-mono text-sm text-gray-600">{value}</span>
      )
    },
    {
      key: "qty",
      title: "总数量",
      width: "80px",
      render: (value: number) => (
        <span className="font-semibold">{value?.toLocaleString() || 0}</span>
      )
    },
    {
      key: "availableQty",
      title: "可用",
      width: "80px",
      render: (value: number) => (
        <span className="text-green-600 font-semibold">{value?.toLocaleString() || 0}</span>
      )
    },
    {
      key: "lockedQty",
      title: "锁定",
      width: "80px",
      render: (value: number) => (
        <span className="text-orange-600">{value?.toLocaleString() || 0}</span>
      )
    },
    {
      key: "expiryStatusName",
      title: "效期状态",
      width: "100px",
      render: (value: string, row: InventoryItem) => (
        <span className={`px-2 py-0.5 rounded text-xs ${getExpiryStatusStyle(row.expiryStatus)}`}>
          {getExpiryStatusIcon(row.expiryStatus)} {value}
        </span>
      )
    },
    {
      key: "expiryDate",
      title: "过期日期",
      width: "100px",
      render: (value: string, row: InventoryItem) => {
        if (!value) return "-";
        const isNearExpiry = row.expiryStatus === 2 || row.expiryStatus === 3;
        return (
          <span className={isNearExpiry ? "text-red-600 font-semibold" : ""}>
            {value}
            {row.remainingDays !== undefined && row.remainingDays > 0 && (
              <span className="text-xs text-gray-400 ml-1">({row.remainingDays}天)</span>
            )}
          </span>
        );
      }
    },
  ];

  // 快捷筛选按钮
  const quickFilters = [
    {
      label: "今日入库",
      icon: Clock,
      onClick: () => {
        toast.info("今日入库筛选功能开发中");
      }
    },
    {
      label: "临期商品",
      icon: AlertTriangle,
      onClick: () => {
        setSelectedExpiryStatus(2);
        setCurrentPage(1);
      }
    },
    {
      label: "已过期",
      icon: X,
      onClick: () => {
        setSelectedExpiryStatus(3);
        setCurrentPage(1);
      }
    },
  ];

  return (
    <div className="space-y-4">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Package size={20} className="text-blue-600" />
          <h2 className="text-lg font-semibold">库存查询</h2>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => { fetchInventory(); fetchSummary(); }}
            className="px-3 py-1.5 border border-gray-300 rounded hover:bg-gray-50 flex items-center gap-1 text-sm"
          >
            <RefreshCw size={16} />
            刷新
          </button>
          <button
            onClick={handleExport}
            className="px-3 py-1.5 bg-green-600 text-white rounded hover:bg-green-700 flex items-center gap-1 text-sm"
          >
            <Download size={16} />
            导出
          </button>
        </div>
      </div>

      {/* 统计卡片 */}
      {summary && (
        <div className="grid grid-cols-5 gap-4">
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <div className="text-sm text-gray-500">SKU数</div>
            <div className="text-2xl font-bold text-blue-600">{summary.totalSku}</div>
          </div>
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <div className="text-sm text-gray-500">总库存</div>
            <div className="text-2xl font-bold text-blue-600">{summary.totalQty?.toLocaleString()}</div>
          </div>
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <div className="text-sm text-gray-500">可用数量</div>
            <div className="text-2xl font-bold text-green-600">{summary.totalAvailableQty?.toLocaleString()}</div>
          </div>
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <div className="text-sm text-gray-500">锁定数量</div>
            <div className="text-2xl font-bold text-orange-600">{summary.totalLockedQty?.toLocaleString()}</div>
          </div>
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <div className="text-sm text-gray-500">预警数</div>
            <div className="text-2xl font-bold text-red-600">
              {summary.nearExpiryCount + summary.expiredCount}
            </div>
            {summary.nearExpiryCount > 0 && (
              <div className="text-xs text-orange-600">临期 {summary.nearExpiryCount}</div>
            )}
            {summary.expiredCount > 0 && (
              <div className="text-xs text-red-600">过期 {summary.expiredCount}</div>
            )}
          </div>
        </div>
      )}

      {/* 查询条件 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
          {/* SKU/名称/条码 */}
          <div className="relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="SKU / 名称 / 条码"
              className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  setCurrentPage(1);
                  fetchInventory();
                }
              }}
            />
          </div>

          {/* 仓库筛选 */}
          <select
            className="px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={selectedWarehouseId || ""}
            onChange={(e) => {
              setSelectedWarehouseId(e.target.value ? Number(e.target.value) : undefined);
              setCurrentPage(1);
            }}
          >
            <option value="">全部仓库</option>
            {warehouses.map((wh) => (
              <option key={wh.id} value={wh.id}>{wh.name}</option>
            ))}
          </select>

          {/* 效期状态筛选 */}
          <select
            className="px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={selectedExpiryStatus ?? ""}
            onChange={(e) => {
              setSelectedExpiryStatus(e.target.value ? Number(e.target.value) : undefined);
              setCurrentPage(1);
            }}
          >
            <option value="">全部状态</option>
            {expiryStatusOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>

          {/* 批次号 */}
          <input
            type="text"
            placeholder="批次号"
            className="px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={batchNo}
            onChange={(e) => setBatchNo(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                setCurrentPage(1);
                fetchInventory();
              }
            }}
          />
        </div>

        {/* 操作按钮 */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <button
              onClick={() => { setCurrentPage(1); fetchInventory(); }}
              className="px-4 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
            >
              查询
            </button>
            <button
              onClick={handleReset}
              className="px-4 py-1.5 border border-gray-300 rounded hover:bg-gray-50 text-sm"
            >
              重置
            </button>
          </div>

          {/* 快捷筛选 */}
          <div className="flex items-center gap-2">
            {quickFilters.map((filter, index) => (
              <button
                key={index}
                onClick={filter.onClick}
                className="px-3 py-1 border border-gray-300 rounded text-sm hover:bg-gray-50 flex items-center gap-1"
              >
                <filter.icon size={14} />
                {filter.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* 库存列表 */}
      <div className="bg-white rounded-lg border border-gray-200">
        {loading ? (
          <div className="text-center py-12 text-gray-500">
            <RefreshCw size={24} className="animate-spin mx-auto mb-2" />
            加载中...
          </div>
        ) : items.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">📭</div>
            <div>未找到匹配的库存记录</div>
            <div className="text-sm text-gray-400 mt-1">请尝试调整筛选条件</div>
          </div>
        ) : (
          <>
            <DataTable columns={columns} data={items} />

            {/* 分页 */}
            {total > queryParams.limit && (
              <div className="flex items-center justify-between px-4 py-3 border-t">
                <div className="text-sm text-gray-500">
                  共 {total} 条记录
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                    disabled={currentPage === 1}
                    className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                  >
                    上一页
                  </button>
                  <span className="text-sm">
                    第 {currentPage} / {Math.ceil(total / queryParams.limit)} 页
                  </span>
                  <button
                    onClick={() => setCurrentPage(p => Math.min(Math.ceil(total / queryParams.limit), p + 1))}
                    disabled={currentPage >= Math.ceil(total / queryParams.limit)}
                    className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                  >
                    下一页
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
