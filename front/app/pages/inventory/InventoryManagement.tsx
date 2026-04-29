import { useState, useEffect } from "react";
import {
  Search, Package, AlertTriangle, Clock, BarChart3,
  Filter, Download, Eye, ChevronDown, ChevronRight
} from "lucide-react";
import { toast } from "sonner";

// ========== 类型定义 ==========
interface InventoryQuery {
  skuCode?: string;
  productName?: string;
  barcode?: string;
  warehouseId?: number;
  zoneId?: number;
  expiryStatus?: number;
  page: number;
  limit: number;
}

interface Inventory {
  id: number;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  zoneId: number;
  zoneCode: string;
  zoneName: string;
  locationId: number;
  locationCode: string;
  productId: number;
  skuCode: string;
  productName: string;
  barcode: string;
  batchNo: string;
  productionDate: string;
  expiryDate: string;
  qty: number;
  availableQty: number;
  lockedQty: number;
  expiryStatus: number;
  expiryStatusName: string;
  remainingDays: number;
  inboundTime: string;
  inboundOrderNo: string;
}

interface BatchInventory {
  id: number;
  batchNo: string;
  productId: number;
  skuCode: string;
  productName: string;
  productionDate: string;
  expiryDate: string;
  totalQty: number;
  totalAvailableQty: number;
  totalLockedQty: number;
  locationCount: number;
  expiryStatus: number;
  expiryStatusName: string;
  remainingDays: number;
}

interface ExpiryWarning {
  id: number;
  productId: number;
  skuCode: string;
  productName: string;
  batchNo: string;
  locationCode: string;
  productionDate: string;
  expiryDate: string;
  remainingDays: number;
  expiryStatus: number;
  expiryStatusName: string;
  qty: number;
  warningLevel: string;
  inboundOrderNo: string;
}

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

interface Warehouse {
  id: number;
  code: string;
  name: string;
}

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
}

// ========== 常量 ==========
const EXPIRY_STATUS_MAP: Record<number, { name: string; color: string }> = {
  0: { name: "正常", color: "text-green-600 bg-green-50" },
  1: { name: "效期预警", color: "text-yellow-600 bg-yellow-50" },
  2: { name: "临期", color: "text-orange-600 bg-orange-50" },
  3: { name: "已过期", color: "text-red-600 bg-red-50" },
};

const TABS = [
  { key: "inventory", label: "库存查询", icon: Package },
  { key: "batch", label: "批次库存", icon: BarChart3 },
  { key: "expiry", label: "效期预警", icon: AlertTriangle },
];

// ========== API 函数 ==========
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

// ========== 主组件 ==========
export default function InventoryManagement() {
  const [activeTab, setActiveTab] = useState("inventory");
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);

  // 库存查询状态
  const [inventoryQuery, setInventoryQuery] = useState<InventoryQuery>({
    page: 1,
    limit: 20,
  });
  const [inventoryList, setInventoryList] = useState<Inventory[]>([]);
  const [inventoryTotal, setInventoryTotal] = useState(0);

  // 批次库存状态
  const [batchList, setBatchList] = useState<BatchInventory[]>([]);

  // 效期预警状态
  const [expiryWarnings, setExpiryWarnings] = useState<ExpiryWarning[]>([]);
  const [expiryFilter, setExpiryFilter] = useState<number | undefined>();

  // 汇总统计
  const [summary, setSummary] = useState<InventorySummary | null>(null);

  // 详情弹窗
  const [selectedProduct, setSelectedProduct] = useState<number | null>(null);
  const [productDetail, setProductDetail] = useState<Inventory[]>([]);
  const [showDetail, setShowDetail] = useState(false);

  // 加载仓库列表
  useEffect(() => {
    loadWarehouses();
    loadSummary();
  }, []);

  // 切换 Tab 时加载数据
  useEffect(() => {
    if (activeTab === "inventory") {
      loadInventory();
    } else if (activeTab === "batch") {
      loadBatchInventory();
    } else if (activeTab === "expiry") {
      loadExpiryWarnings();
    }
  }, [activeTab, inventoryQuery.page, expiryFilter]);

  async function loadWarehouses() {
    try {
      const data = await fetchApi<{ list: Warehouse[] }>("/api/v1/base/warehouses");
      setWarehouses(data.list || []);
    } catch (err) {
      console.error("加载仓库列表失败", err);
    }
  }

  async function loadInventory() {
    try {
      const params = new URLSearchParams();
      if (inventoryQuery.skuCode) params.append("skuCode", inventoryQuery.skuCode);
      if (inventoryQuery.productName) params.append("productName", inventoryQuery.productName);
      if (inventoryQuery.barcode) params.append("barcode", inventoryQuery.barcode);
      if (inventoryQuery.warehouseId) params.append("warehouseId", String(inventoryQuery.warehouseId));
      if (inventoryQuery.expiryStatus !== undefined) params.append("expiryStatus", String(inventoryQuery.expiryStatus));
      params.append("page", String(inventoryQuery.page));
      params.append("limit", String(inventoryQuery.limit));

      const data = await fetchApi<{ list: Inventory[]; total: number }>(
        `/api/v1/inventory/list?${params.toString()}`
      );
      setInventoryList(data.list || []);
      setInventoryTotal(data.total || 0);
    } catch (err) {
      toast.error("加载库存数据失败");
    }
  }

  async function loadBatchInventory() {
    try {
      const params = new URLSearchParams();
      if (inventoryQuery.batchNo) params.append("batchNo", inventoryQuery.batchNo);
      if (inventoryQuery.warehouseId) params.append("warehouseId", String(inventoryQuery.warehouseId));

      const data = await fetchApi<BatchInventory[]>(
        `/api/v1/inventory/batch?${params.toString()}`
      );
      setBatchList(data || []);
    } catch (err) {
      toast.error("加载批次库存失败");
    }
  }

  async function loadExpiryWarnings() {
    try {
      const params = new URLSearchParams();
      if (inventoryQuery.warehouseId) params.append("warehouseId", String(inventoryQuery.warehouseId));
      if (expiryFilter !== undefined) params.append("expiryStatus", String(expiryFilter));

      const data = await fetchApi<ExpiryWarning[]>(
        `/api/v1/inventory/expiry-warning?${params.toString()}`
      );
      setExpiryWarnings(data || []);
    } catch (err) {
      toast.error("加载效期预警失败");
    }
  }

  async function loadSummary() {
    try {
      const data = await fetchApi<InventorySummary>("/api/v1/inventory/summary");
      setSummary(data);
    } catch (err) {
      console.error("加载汇总统计失败", err);
    }
  }

  async function loadProductDetail(productId: number) {
    try {
      const data = await fetchApi<Inventory[]>(
        `/api/v1/inventory/detail/${productId}`
      );
      setProductDetail(data);
      setSelectedProduct(productId);
      setShowDetail(true);
    } catch (err) {
      toast.error("加载库存明细失败");
    }
  }

  function handleSearch() {
    inventoryQuery.page = 1;
    if (activeTab === "inventory") {
      loadInventory();
    } else if (activeTab === "batch") {
      loadBatchInventory();
    } else if (activeTab === "expiry") {
      loadExpiryWarnings();
    }
  }

  function handleReset() {
    setInventoryQuery({ page: 1, limit: 20 });
    setExpiryFilter(undefined);
  }

  // ========== 渲染 ==========
  return (
    <div className="h-full flex flex-col">
      {/* 页面标题 */}
      <div className="p-4 border-b bg-white">
        <h1 className="text-xl font-semibold flex items-center gap-2">
          <Package className="text-blue-600" />
          库存管理
        </h1>
      </div>

      {/* 汇总统计卡片 */}
      {summary && (
        <div className="p-4 bg-gray-50 border-b">
          <div className="grid grid-cols-4 gap-4">
            <div className="bg-white p-4 rounded-lg border">
              <div className="text-sm text-gray-500">SKU数量</div>
              <div className="text-2xl font-bold text-blue-600">{summary.totalSku}</div>
            </div>
            <div className="bg-white p-4 rounded-lg border">
              <div className="text-sm text-gray-500">总库存</div>
              <div className="text-2xl font-bold text-green-600">{summary.totalQty}</div>
            </div>
            <div className="bg-white p-4 rounded-lg border">
              <div className="text-sm text-gray-500">可用库存</div>
              <div className="text-2xl font-bold">{summary.totalAvailableQty}</div>
            </div>
            <div className="bg-white p-4 rounded-lg border">
              <div className="text-sm text-gray-500">锁定库存</div>
              <div className="text-2xl font-bold text-orange-600">{summary.totalLockedQty}</div>
            </div>
          </div>
          <div className="grid grid-cols-4 gap-4 mt-4">
            <div className="bg-green-50 p-3 rounded-lg border border-green-200">
              <div className="text-xs text-green-600">正常</div>
              <div className="text-lg font-semibold text-green-700">{summary.normalCount}</div>
            </div>
            <div className="bg-yellow-50 p-3 rounded-lg border border-yellow-200">
              <div className="text-xs text-yellow-600">效期预警</div>
              <div className="text-lg font-semibold text-yellow-700">{summary.warningCount}</div>
            </div>
            <div className="bg-orange-50 p-3 rounded-lg border border-orange-200">
              <div className="text-xs text-orange-600">临期</div>
              <div className="text-lg font-semibold text-orange-700">{summary.nearExpiryCount}</div>
            </div>
            <div className="bg-red-50 p-3 rounded-lg border border-red-200">
              <div className="text-xs text-red-600">已过期</div>
              <div className="text-lg font-semibold text-red-700">{summary.expiredCount}</div>
            </div>
          </div>
        </div>
      )}

      {/* Tab 切换 */}
      <div className="flex border-b bg-white">
        {TABS.map((tab) => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`flex items-center gap-2 px-6 py-3 border-b-2 transition-colors ${
                activeTab === tab.key
                  ? "border-blue-600 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700"
              }`}
            >
              <Icon size={18} />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* 搜索栏 */}
      <div className="p-4 bg-white border-b">
        <div className="flex flex-wrap gap-3">
          {activeTab === "inventory" && (
            <>
              <input
                type="text"
                placeholder="SKU编码"
                value={inventoryQuery.skuCode || ""}
                onChange={(e) => setInventoryQuery({ ...inventoryQuery, skuCode: e.target.value })}
                className="px-3 py-2 border rounded-md w-40"
              />
              <input
                type="text"
                placeholder="商品名称"
                value={inventoryQuery.productName || ""}
                onChange={(e) => setInventoryQuery({ ...inventoryQuery, productName: e.target.value })}
                className="px-3 py-2 border rounded-md w-40"
              />
              <input
                type="text"
                placeholder="条码"
                value={inventoryQuery.barcode || ""}
                onChange={(e) => setInventoryQuery({ ...inventoryQuery, barcode: e.target.value })}
                className="px-3 py-2 border rounded-md w-40"
              />
            </>
          )}

          {activeTab === "batch" && (
            <input
              type="text"
              placeholder="批次号"
              value={inventoryQuery.batchNo || ""}
              onChange={(e) => setInventoryQuery({ ...inventoryQuery, batchNo: e.target.value })}
              className="px-3 py-2 border rounded-md w-40"
            />
          )}

          <select
            value={inventoryQuery.warehouseId || ""}
            onChange={(e) => setInventoryQuery({ ...inventoryQuery, warehouseId: e.target.value ? Number(e.target.value) : undefined })}
            className="px-3 py-2 border rounded-md"
          >
            <option value="">全部仓库</option>
            {warehouses.map((w) => (
              <option key={w.id} value={w.id}>{w.name}</option>
            ))}
          </select>

          {activeTab === "expiry" && (
            <select
              value={expiryFilter || ""}
              onChange={(e) => setExpiryFilter(e.target.value ? Number(e.target.value) : undefined)}
              className="px-3 py-2 border rounded-md"
            >
              <option value="">全部状态</option>
              <option value="1">效期预警</option>
              <option value="2">临期</option>
              <option value="3">已过期</option>
            </select>
          )}

          <button
            onClick={handleSearch}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 flex items-center gap-1"
          >
            <Search size={16} />
            查询
          </button>
          <button
            onClick={handleReset}
            className="px-4 py-2 border rounded-md hover:bg-gray-50"
          >
            重置
          </button>
        </div>
      </div>

      {/* 数据列表 */}
      <div className="flex-1 overflow-auto p-4">
        {activeTab === "inventory" && (
          <div className="bg-white rounded-lg border">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">SKU</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">商品名称</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">仓库</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">库位</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">批次号</th>
                  <th className="px-4 py-3 text-right text-sm font-medium text-gray-600">总数量</th>
                  <th className="px-4 py-3 text-right text-sm font-medium text-gray-600">可用</th>
                  <th className="px-4 py-3 text-right text-sm font-medium text-gray-600">锁定</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-gray-600">效期状态</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-gray-600">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {inventoryList.map((item) => (
                  <tr key={item.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm font-mono">{item.skuCode}</td>
                    <td className="px-4 py-3 text-sm">{item.productName}</td>
                    <td className="px-4 py-3 text-sm">{item.warehouseName || item.warehouseCode}</td>
                    <td className="px-4 py-3 text-sm font-mono">{item.locationCode}</td>
                    <td className="px-4 py-3 text-sm font-mono">{item.batchNo}</td>
                    <td className="px-4 py-3 text-sm text-right font-medium">{item.qty}</td>
                    <td className="px-4 py-3 text-sm text-right text-green-600">{item.availableQty}</td>
                    <td className="px-4 py-3 text-sm text-right text-orange-600">{item.lockedQty}</td>
                    <td className="px-4 py-3 text-center">
                      <span className={`px-2 py-1 rounded text-xs ${EXPIRY_STATUS_MAP[item.expiryStatus]?.color || ""}`}>
                        {item.expiryStatusName || EXPIRY_STATUS_MAP[item.expiryStatus]?.name || "正常"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <button
                        onClick={() => loadProductDetail(item.productId)}
                        className="text-blue-600 hover:text-blue-800"
                      >
                        <Eye size={16} />
                      </button>
                    </td>
                  </tr>
                ))}
                {inventoryList.length === 0 && (
                  <tr>
                    <td colSpan={10} className="px-4 py-8 text-center text-gray-500">
                      暂无库存数据
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {activeTab === "batch" && (
          <div className="bg-white rounded-lg border">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">批次号</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">SKU</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">商品名称</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">生产日期</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">过期日期</th>
                  <th className="px-4 py-3 text-right text-sm font-medium text-gray-600">总数量</th>
                  <th className="px-4 py-3 text-right text-sm font-medium text-gray-600">库位数</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-gray-600">效期状态</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {batchList.map((item, idx) => (
                  <tr key={idx} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm font-mono">{item.batchNo}</td>
                    <td className="px-4 py-3 text-sm font-mono">{item.skuCode}</td>
                    <td className="px-4 py-3 text-sm">{item.productName}</td>
                    <td className="px-4 py-3 text-sm">{item.productionDate || "-"}</td>
                    <td className="px-4 py-3 text-sm">{item.expiryDate || "-"}</td>
                    <td className="px-4 py-3 text-sm text-right font-medium">{item.totalQty}</td>
                    <td className="px-4 py-3 text-sm text-right">{item.locationCount}</td>
                    <td className="px-4 py-3 text-center">
                      <span className={`px-2 py-1 rounded text-xs ${EXPIRY_STATUS_MAP[item.expiryStatus]?.color || ""}`}>
                        {item.expiryStatusName || "正常"}
                      </span>
                    </td>
                  </tr>
                ))}
                {batchList.length === 0 && (
                  <tr>
                    <td colSpan={8} className="px-4 py-8 text-center text-gray-500">
                      暂无批次库存数据
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {activeTab === "expiry" && (
          <div className="bg-white rounded-lg border">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">SKU</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">商品名称</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">批次号</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">库位</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">过期日期</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-gray-600">剩余天数</th>
                  <th className="px-4 py-3 text-right text-sm font-medium text-gray-600">数量</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-gray-600">状态</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-gray-600">级别</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {expiryWarnings.map((item) => (
                  <tr key={item.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm font-mono">{item.skuCode}</td>
                    <td className="px-4 py-3 text-sm">{item.productName}</td>
                    <td className="px-4 py-3 text-sm font-mono">{item.batchNo}</td>
                    <td className="px-4 py-3 text-sm font-mono">{item.locationCode}</td>
                    <td className="px-4 py-3 text-sm">{item.expiryDate}</td>
                    <td className="px-4 py-3 text-center">
                      <span className={`font-bold ${item.remainingDays < 0 ? "text-red-600" : item.remainingDays <= 7 ? "text-orange-600" : "text-yellow-600"}`}>
                        {item.remainingDays < 0 ? `已过期${Math.abs(item.remainingDays)}天` : `${item.remainingDays}天`}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-right font-medium">{item.qty}</td>
                    <td className="px-4 py-3 text-center">
                      <span className={`px-2 py-1 rounded text-xs ${EXPIRY_STATUS_MAP[item.expiryStatus]?.color || ""}`}>
                        {item.expiryStatusName}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span className={`px-2 py-1 rounded text-xs ${
                        item.warningLevel === "紧急" ? "bg-red-100 text-red-700" :
                        item.warningLevel === "重要" ? "bg-orange-100 text-orange-700" :
                        "bg-gray-100 text-gray-700"
                      }`}>
                        {item.warningLevel}
                      </span>
                    </td>
                  </tr>
                ))}
                {expiryWarnings.length === 0 && (
                  <tr>
                    <td colSpan={9} className="px-4 py-8 text-center text-gray-500">
                      暂无效期预警数据
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* 库存明细弹窗 */}
      {showDetail && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg w-[800px] max-h-[80vh] overflow-hidden">
            <div className="p-4 border-b flex justify-between items-center">
              <h3 className="font-semibold">库存分布明细</h3>
              <button onClick={() => setShowDetail(false)} className="text-gray-500 hover:text-gray-700">
                ✕
              </button>
            </div>
            <div className="p-4 overflow-auto max-h-[60vh]">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-3 py-2 text-left text-sm">仓库</th>
                    <th className="px-3 py-2 text-left text-sm">库区</th>
                    <th className="px-3 py-2 text-left text-sm">库位</th>
                    <th className="px-3 py-2 text-left text-sm">批次号</th>
                    <th className="px-3 py-2 text-right text-sm">数量</th>
                    <th className="px-3 py-2 text-right text-sm">可用</th>
                    <th className="px-3 py-2 text-center text-sm">效期状态</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {productDetail.map((item) => (
                    <tr key={item.id}>
                      <td className="px-3 py-2 text-sm">{item.warehouseName}</td>
                      <td className="px-3 py-2 text-sm">{item.zoneName || item.zoneCode}</td>
                      <td className="px-3 py-2 text-sm font-mono">{item.locationCode}</td>
                      <td className="px-3 py-2 text-sm font-mono">{item.batchNo}</td>
                      <td className="px-3 py-2 text-sm text-right">{item.qty}</td>
                      <td className="px-3 py-2 text-sm text-right text-green-600">{item.availableQty}</td>
                      <td className="px-3 py-2 text-center">
                        <span className={`px-2 py-0.5 rounded text-xs ${EXPIRY_STATUS_MAP[item.expiryStatus]?.color || ""}`}>
                          {item.expiryStatusName || "正常"}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
