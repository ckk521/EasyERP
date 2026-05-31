import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { Plus, HelpCircle, Play, Eye, XCircle } from "lucide-react";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Textarea } from "../../components/ui/textarea";
import { toast } from "sonner";

interface StocktakeOrder {
  id: number;
  orderNo: string;
  stocktakeType: number;
  stocktakeTypeName: string;
  warehouseId: number;
  warehouseName: string;
  scopeType: string;
  scopeName: string;
  totalItems: number;
  status: number;
  statusName: string;
  accuracyRate: number | null;
  createUserName: string;
  createTime: string;
  planDate: string;
}

interface Warehouse {
  id: number;
  code: string;
  name: string;
  hasInventory?: boolean;
}

interface Zone {
  id: number;
  code: string;
  name: string;
  type: string;
}

interface Category {
  id: number;
  code: string;
  name: string;
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

// 获取当天日期字符串 (YYYY-MM-DD)
function getTodayDate(): string {
  const today = new Date();
  return today.toISOString().split("T")[0];
}

// 抽盘筛选方式
const SAMPLE_FILTER_TYPES = [
  { value: "zone", label: "按库区", desc: "盘点指定库区的所有商品" },
  { value: "category", label: "按商品分类", desc: "盘点指定分类的商品" },
  { value: "abc", label: "按ABC分类", desc: "盘点A/B/C类商品" },
  { value: "sku", label: "指定SKU", desc: "手动选择要盘点的商品" },
  { value: "random", label: "随机抽取", desc: "按比例随机抽取商品" },
];

export default function StocktakeList() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<StocktakeOrder[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [zones, setZones] = useState<Zone[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [cancelDialog, setCancelDialog] = useState<{ open: boolean; order: StocktakeOrder | null; reason: string }>({
    open: false,
    order: null,
    reason: ""
  });
  const [formData, setFormData] = useState({
    type: "",
    warehouse: "",
    // 盘点范围（全盘用）
    scopeType: "all",
    zoneId: "",
    // 抽盘筛选（抽盘用）
    sampleFilterType: "zone",
    sampleZoneIds: [] as string[],
    sampleCategoryIds: [] as string[],
    sampleAbcClass: "",
    sampleSkuCodes: "",
    sampleRandomPercent: 10,
    // 循环盘配置
    cycleType: "daily", // daily, weekly, monthly
    cycleDay: 1, // 周几(1-7) 或 每月第几天(1-31)
    cycleStrategy: "zone_rotation", // zone_rotation, sku_rotation, fixed
    cycleZoneIds: [] as string[], // 按库区轮转的库区列表
    cycleSkuPercent: 10, // 按SKU轮转的比例
    // 其他
    planDate: getTodayDate(),
    blindMode: false,
    remark: "",
  });

  useEffect(() => {
    fetchOrders();
    fetchWarehouses();
    fetchCategories();
  }, []);

  // 仓库变化时加载对应库区
  useEffect(() => {
    if (formData.warehouse) {
      fetchZones(Number(formData.warehouse));
    } else {
      setZones([]);
    }
  }, [formData.warehouse]);

  async function fetchOrders() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: StocktakeOrder[]; total: number }>("/api/stocktake/list?page=1&limit=100");
      setOrders(data.list || []);
    } catch {
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }

  async function fetchWarehouses() {
    try {
      const warehouseData = await fetchApi<{ list: Warehouse[] }>("/api/v1/base/warehouses");
      const warehouseList = warehouseData.list || [];

      const inventorySummary = await fetchApi<{ [key: string]: number }>("/api/v1/inventory/summary-by-warehouse");

      const warehousesWithInventory = warehouseList.map(w => ({
        ...w,
        hasInventory: (inventorySummary[w.id] || 0) > 0
      })).sort((a, b) => {
        if (a.hasInventory && !b.hasInventory) return -1;
        if (!a.hasInventory && b.hasInventory) return 1;
        return 0;
      });

      setWarehouses(warehousesWithInventory);
    } catch {
      setWarehouses([]);
    }
  }

  async function fetchZones(warehouseId: number) {
    try {
      const data = await fetchApi<{ list: Zone[] }>(`/api/v1/base/zones?warehouseId=${warehouseId}`);
      setZones(data.list || []);
    } catch {
      setZones([]);
    }
  }

  async function fetchCategories() {
    try {
      const data = await fetchApi<{ list: Category[] }>("/api/v1/base/categories");
      setCategories(data.list || []);
    } catch {
      setCategories([]);
    }
  }

  // 取消盘点单
  async function handleCancelOrder() {
    if (!cancelDialog.order) return;

    try {
      const url = cancelDialog.order.status === 0
        ? `/api/stocktake/cancel/${cancelDialog.order.id}`
        : `/api/stocktake/force-cancel/${cancelDialog.order.id}`;

      await fetchApi(url, {
        method: "POST",
        body: JSON.stringify({ reason: cancelDialog.reason || "用户取消" }),
      });

      toast.success("盘点单已取消");
      setCancelDialog({ open: false, order: null, reason: "" });
      fetchOrders();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "取消失败");
    }
  }

  const columns = [
    { key: "orderNo", title: "盘点单号", width: "140px" },
    {
      key: "stocktakeTypeName",
      title: "盘点类型",
      width: "100px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '全盘' ? 'bg-purple-100 text-purple-700' :
          value === '抽盘' ? 'bg-blue-100 text-blue-700' :
          'bg-green-100 text-green-700'
        }`}>{value}</span>
      )
    },
    { key: "warehouseName", title: "仓库", width: "100px" },
    { key: "scopeName", title: "盘点范围", width: "120px" },
    { key: "totalItems", title: "盘点SKU数", width: "100px" },
    {
      key: "statusName",
      title: "状态",
      width: "90px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '盘点中' ? 'bg-blue-100 text-blue-700' :
          value === '待盘点' ? 'bg-yellow-100 text-yellow-700' :
          value === '待审核' ? 'bg-orange-100 text-orange-700' :
          value === '已完成' ? 'bg-green-100 text-green-700' :
          'bg-gray-100 text-gray-700'
        }`}>{value}</span>
      )
    },
    { key: "accuracyRate", title: "准确率", width: "80px", render: (v: number | null) => v != null ? `${v}%` : '-' },
    { key: "createUserName", title: "创建人", width: "80px" },
    { key: "createTime", title: "创建时间", width: "140px" },
    {
      key: "actions",
      title: "操作",
      width: "150px",
      render: (_: unknown, row: StocktakeOrder) => (
        <div className="flex items-center gap-1">
          {(row.status === 0 || row.status === 1) && (
            <button
              onClick={() => navigate(`/inventory/stocktake/${row.id}`)}
              className="p-1.5 text-blue-600 hover:bg-blue-50 rounded"
              title="开始盘点"
            >
              <Play size={16} />
            </button>
          )}
          {row.status >= 2 && (
            <button
              onClick={() => navigate(`/inventory/stocktake/${row.id}`)}
              className="px-2 py-1 text-blue-600 hover:bg-blue-50 rounded text-xs"
              title="查看详情"
            >
              详情
            </button>
          )}
          {row.status !== 3 && row.status !== 4 && (
            <button
              onClick={() => setCancelDialog({ open: true, order: row, reason: "" })}
              className="p-1.5 text-red-600 hover:bg-red-50 rounded"
              title="取消盘点单"
            >
              <XCircle size={16} />
            </button>
          )}
        </div>
      )
    },
  ];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // 验证必填项
    if (!formData.type) {
      toast.error("请选择盘点类型");
      return;
    }
    if (!formData.warehouse) {
      toast.error("请选择仓库");
      return;
    }

    // 抽盘时验证筛选条件
    if (formData.type === "抽盘") {
      if (formData.sampleFilterType === "zone" && formData.sampleZoneIds.length === 0) {
        toast.error("请选择至少一个库区");
        return;
      }
      if (formData.sampleFilterType === "category" && formData.sampleCategoryIds.length === 0) {
        toast.error("请选择至少一个商品分类");
        return;
      }
      if (formData.sampleFilterType === "abc" && !formData.sampleAbcClass) {
        toast.error("请选择ABC分类");
        return;
      }
      if (formData.sampleFilterType === "sku" && !formData.sampleSkuCodes.trim()) {
        toast.error("请输入要盘点的SKU编码");
        return;
      }
    } else if (formData.type === "全盘") {
      if (formData.scopeType === "zone" && !formData.zoneId) {
        toast.error("请选择库区");
        return;
      }
    }

    if (!formData.planDate) {
      toast.error("请选择计划日期");
      return;
    }

    // 构建提交数据
    const submitData: Record<string, unknown> = {
      warehouseId: Number(formData.warehouse),
      stocktakeType: formData.type === "全盘" ? 1 : formData.type === "抽盘" ? 2 : 3,
      blindMode: formData.blindMode ? 1 : 0,
      planDate: formData.planDate,
      remark: formData.remark,
    };

    // 全盘范围
    if (formData.type === "全盘") {
      submitData.scopeType = formData.scopeType;
      if (formData.scopeType === "zone") {
        submitData.zoneId = Number(formData.zoneId);
      }
    }

    // 抽盘筛选
    if (formData.type === "抽盘") {
      submitData.scopeType = formData.sampleFilterType;
      if (formData.sampleFilterType === "zone") {
        submitData.zoneIds = formData.sampleZoneIds.map(Number);
      }
      if (formData.sampleFilterType === "category") {
        submitData.categoryIds = formData.sampleCategoryIds.map(Number);
      }
      if (formData.sampleFilterType === "abc") {
        submitData.abcClass = formData.sampleAbcClass;
      }
      if (formData.sampleFilterType === "sku") {
        submitData.skuCodes = formData.sampleSkuCodes.split(",").map(s => s.trim());
      }
      if (formData.sampleFilterType === "random") {
        submitData.randomPercent = formData.sampleRandomPercent;
      }
    }

    // 循环盘配置
    if (formData.type === "循环盘") {
      submitData.cycleType = formData.cycleType;
      submitData.cycleDay = formData.cycleDay;
      submitData.cycleStrategy = formData.cycleStrategy;
      if (formData.cycleStrategy === "zone_rotation") {
        submitData.cycleZoneIds = formData.cycleZoneIds.map(Number);
      }
      if (formData.cycleStrategy === "sku_rotation") {
        submitData.cycleSkuPercent = formData.cycleSkuPercent;
      }
    }

    try {
      await fetchApi<{ orderId: number }>("/api/stocktake/create", {
        method: "POST",
        body: JSON.stringify(submitData),
      });
      toast.success("盘点单创建成功");
      setIsFormOpen(false);
      fetchOrders();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "创建失败");
    }
  };

  // 重置表单
  const resetForm = () => {
    setFormData({
      type: "",
      warehouse: "",
      scopeType: "all",
      zoneId: "",
      sampleFilterType: "zone",
      sampleZoneIds: [],
      sampleCategoryIds: [],
      sampleAbcClass: "",
      sampleSkuCodes: "",
      sampleRandomPercent: 10,
      cycleType: "daily",
      cycleDay: 1,
      cycleStrategy: "zone_rotation",
      cycleZoneIds: [],
      cycleSkuPercent: 10,
      planDate: getTodayDate(),
      blindMode: false,
      remark: "",
    });
  };

  // 打开表单时重置
  const handleOpenForm = () => {
    resetForm();
    setIsFormOpen(true);
  };

  // 渲染抽盘筛选条件
  const renderSampleFilter = () => {
    if (formData.type !== "抽盘") return null;

    const currentFilter = SAMPLE_FILTER_TYPES.find(f => f.value === formData.sampleFilterType);

    return (
      <div className="space-y-3 p-3 bg-blue-50 rounded-lg border border-blue-200">
        <div className="flex items-center gap-2 text-sm font-medium text-blue-700">
          <span>抽盘筛选条件</span>
          <span className="text-xs text-blue-500">(选择要盘点的商品范围)</span>
        </div>

        {/* 筛选方式选择 */}
        <div className="grid grid-cols-2 gap-2">
          {SAMPLE_FILTER_TYPES.map((filter) => (
            <button
              key={filter.value}
              type="button"
              onClick={() => setFormData({ ...formData, sampleFilterType: filter.value })}
              className={`p-2 text-left rounded border text-sm transition-colors ${
                formData.sampleFilterType === filter.value
                  ? "border-blue-500 bg-blue-100 text-blue-700"
                  : "border-gray-200 bg-white hover:border-blue-300"
              }`}
            >
              <div className="font-medium">{filter.label}</div>
              <div className="text-xs text-gray-500">{filter.desc}</div>
            </button>
          ))}
        </div>

        {/* 根据筛选方式显示具体选项 */}
        {formData.sampleFilterType === "zone" && (
          <div className="space-y-2">
            <Label>选择库区 <span className="text-red-500">*</span></Label>
            <div className="grid grid-cols-2 gap-2 max-h-32 overflow-y-auto">
              {zones.map((z) => (
                <label
                  key={z.id}
                  className={`flex items-center gap-2 p-2 rounded border cursor-pointer text-sm ${
                    formData.sampleZoneIds.includes(String(z.id))
                      ? "border-blue-500 bg-blue-50"
                      : "border-gray-200 hover:border-blue-300"
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={formData.sampleZoneIds.includes(String(z.id))}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setFormData({ ...formData, sampleZoneIds: [...formData.sampleZoneIds, String(z.id)] });
                      } else {
                        setFormData({ ...formData, sampleZoneIds: formData.sampleZoneIds.filter(id => id !== String(z.id)) });
                      }
                    }}
                    className="rounded"
                  />
                  <span>{z.name}</span>
                </label>
              ))}
              {zones.length === 0 && (
                <div className="col-span-2 text-sm text-gray-500 text-center py-2">该仓库暂无库区</div>
              )}
            </div>
            {formData.sampleZoneIds.length > 0 && (
              <p className="text-xs text-gray-500">已选择 {formData.sampleZoneIds.length} 个库区</p>
            )}
          </div>
        )}

        {formData.sampleFilterType === "category" && (
          <div className="space-y-2">
            <Label>选择商品分类 <span className="text-red-500">*</span></Label>
            <div className="grid grid-cols-2 gap-2 max-h-32 overflow-y-auto">
              {categories.map((c) => (
                <label
                  key={c.id}
                  className={`flex items-center gap-2 p-2 rounded border cursor-pointer text-sm ${
                    formData.sampleCategoryIds.includes(String(c.id))
                      ? "border-blue-500 bg-blue-50"
                      : "border-gray-200 hover:border-blue-300"
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={formData.sampleCategoryIds.includes(String(c.id))}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setFormData({ ...formData, sampleCategoryIds: [...formData.sampleCategoryIds, String(c.id)] });
                      } else {
                        setFormData({ ...formData, sampleCategoryIds: formData.sampleCategoryIds.filter(id => id !== String(c.id)) });
                      }
                    }}
                    className="rounded"
                  />
                  <span>{c.name}</span>
                </label>
              ))}
              {categories.length === 0 && (
                <div className="col-span-2 text-sm text-gray-500 text-center py-2">暂无商品分类</div>
              )}
            </div>
            {formData.sampleCategoryIds.length > 0 && (
              <p className="text-xs text-gray-500">已选择 {formData.sampleCategoryIds.length} 个分类</p>
            )}
          </div>
        )}

        {formData.sampleFilterType === "abc" && (
          <div className="space-y-2">
            <Label>选择ABC分类 <span className="text-red-500">*</span></Label>
            <Select
              value={formData.sampleAbcClass}
              onValueChange={(v) => setFormData({ ...formData, sampleAbcClass: v })}
            >
              <SelectTrigger><SelectValue placeholder="请选择" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="A">A类商品（高价值/高周转）</SelectItem>
                <SelectItem value="B">B类商品（中等价值/周转）</SelectItem>
                <SelectItem value="C">C类商品（低价值/低周转）</SelectItem>
              </SelectContent>
            </Select>
            <p className="text-xs text-gray-500">A类：出库量占80%的商品；B类：15%；C类：5%</p>
          </div>
        )}

        {formData.sampleFilterType === "sku" && (
          <div className="space-y-2">
            <Label>输入SKU编码 <span className="text-red-500">*</span></Label>
            <Textarea
              value={formData.sampleSkuCodes}
              onChange={(e) => setFormData({ ...formData, sampleSkuCodes: e.target.value })}
              placeholder="多个SKU用逗号分隔，如：SKU-001, SKU-002, SKU-003"
              rows={3}
            />
            <p className="text-xs text-gray-500">手动输入要盘点的商品SKU编码</p>
          </div>
        )}

        {formData.sampleFilterType === "random" && (
          <div className="space-y-2">
            <Label>随机抽取比例</Label>
            <div className="flex items-center gap-3">
              <input
                type="range"
                min="5"
                max="50"
                step="5"
                value={formData.sampleRandomPercent}
                onChange={(e) => setFormData({ ...formData, sampleRandomPercent: Number(e.target.value) })}
                className="flex-1"
              />
              <span className="text-sm font-medium w-12 text-right">{formData.sampleRandomPercent}%</span>
            </div>
            <p className="text-xs text-gray-500">系统将随机抽取该仓库 {formData.sampleRandomPercent}% 的SKU进行盘点</p>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">盘点管理</h2>
        <button
          onClick={handleOpenForm}
          className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm"
        >
          <Plus size={16} />
          新建盘点单
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        {loading ? (
          <div className="text-center py-12 text-gray-500">加载中...</div>
        ) : orders.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">📋</div>
            <div>暂无盘点单数据</div>
            <div className="text-sm text-gray-400 mt-1">点击"新建盘点单"开始盘点</div>
          </div>
        ) : (
          <DataTable columns={columns} data={orders} />
        )}
      </div>

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>新建盘点单</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit}>
            <div className="grid gap-4 py-4">
              {/* 盘点类型 */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>盘点类型 <span className="text-red-500">*</span></Label>
                  <Select value={formData.type} onValueChange={(v) => setFormData({ ...formData, type: v })}>
                    <SelectTrigger><SelectValue placeholder="请选择" /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="全盘">全盘</SelectItem>
                      <SelectItem value="抽盘">抽盘</SelectItem>
                      <SelectItem value="循环盘">循环盘</SelectItem>
                    </SelectContent>
                  </Select>
                  <p className="text-xs text-gray-500">
                    {formData.type === "全盘" && "盘点仓库所有商品"}
                    {formData.type === "抽盘" && "按条件抽取部分商品盘点"}
                    {formData.type === "循环盘" && "按周期自动轮转盘点"}
                  </p>
                </div>
                <div className="space-y-2">
                  <Label>盘点模式</Label>
                  <Select
                    value={formData.blindMode ? "blind" : "open"}
                    onValueChange={(v) => setFormData({ ...formData, blindMode: v === "blind" })}
                  >
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="open">明盘（显示系统数量）</SelectItem>
                      <SelectItem value="blind">盲盘（不显示系统数量）</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {/* 仓库 */}
              <div className="space-y-2">
                <Label>仓库 <span className="text-red-500">*</span></Label>
                <Select value={formData.warehouse} onValueChange={(v) => setFormData({ ...formData, warehouse: v, zoneId: "", sampleZoneIds: [] })}>
                  <SelectTrigger><SelectValue placeholder="请选择仓库" /></SelectTrigger>
                  <SelectContent>
                    {warehouses.map((wh) => (
                      <SelectItem
                        key={wh.id}
                        value={String(wh.id)}
                        disabled={!wh.hasInventory}
                        className={!wh.hasInventory ? "text-gray-400" : ""}
                      >
                        {wh.name}
                        {!wh.hasInventory && " (无库存)"}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* 全盘范围 */}
              {formData.type === "全盘" && (
                <div className="space-y-2">
                  <Label>盘点范围 <span className="text-red-500">*</span></Label>
                  <div className="flex gap-2">
                    <Select
                      value={formData.scopeType}
                      onValueChange={(v) => setFormData({ ...formData, scopeType: v, zoneId: "" })}
                      className="w-32"
                    >
                      <SelectTrigger><SelectValue /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">全仓库</SelectItem>
                        <SelectItem value="zone">指定库区</SelectItem>
                      </SelectContent>
                    </Select>
                    {formData.scopeType === "zone" && (
                      <Select
                        value={formData.zoneId}
                        onValueChange={(v) => setFormData({ ...formData, zoneId: v })}
                        className="flex-1"
                      >
                        <SelectTrigger><SelectValue placeholder="选择库区" /></SelectTrigger>
                        <SelectContent>
                          {zones.map((z) => (
                            <SelectItem key={z.id} value={String(z.id)}>
                              {z.name} ({z.code})
                            </SelectItem>
                          ))}
                          {zones.length === 0 && (
                            <SelectItem value="_empty" disabled>该仓库暂无库区</SelectItem>
                          )}
                        </SelectContent>
                      </Select>
                    )}
                  </div>
                  {formData.scopeType === "all" && (
                    <p className="text-xs text-gray-500">将盘点该仓库所有库区的库存</p>
                  )}
                </div>
              )}

              {/* 抽盘筛选条件 */}
              {renderSampleFilter()}

              {/* 循环盘周期配置 */}
              {formData.type === "循环盘" && (
                <div className="space-y-4 p-3 bg-green-50 rounded-lg border border-green-200">
                  <div className="flex items-center gap-2 text-sm font-medium text-green-700">
                    <span>循环盘周期配置</span>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label>盘点周期</Label>
                      <Select
                        value={formData.cycleType}
                        onValueChange={(v) => setFormData({ ...formData, cycleType: v, cycleDay: 1 })}
                      >
                        <SelectTrigger><SelectValue /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="daily">每日盘点</SelectItem>
                          <SelectItem value="weekly">每周盘点</SelectItem>
                          <SelectItem value="monthly">每月盘点</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>

                    {formData.cycleType === "weekly" && (
                      <div className="space-y-2">
                        <Label>盘点日（周几）</Label>
                        <Select
                          value={String(formData.cycleDay)}
                          onValueChange={(v) => setFormData({ ...formData, cycleDay: Number(v) })}
                        >
                          <SelectTrigger><SelectValue /></SelectTrigger>
                          <SelectContent>
                            <SelectItem value="1">周一</SelectItem>
                            <SelectItem value="2">周二</SelectItem>
                            <SelectItem value="3">周三</SelectItem>
                            <SelectItem value="4">周四</SelectItem>
                            <SelectItem value="5">周五</SelectItem>
                            <SelectItem value="6">周六</SelectItem>
                            <SelectItem value="7">周日</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                    )}

                    {formData.cycleType === "monthly" && (
                      <div className="space-y-2">
                        <Label>盘点日（每月第几天）</Label>
                        <Select
                          value={String(formData.cycleDay)}
                          onValueChange={(v) => setFormData({ ...formData, cycleDay: Number(v) })}
                        >
                          <SelectTrigger><SelectValue /></SelectTrigger>
                          <SelectContent>
                            {Array.from({ length: 28 }, (_, i) => (
                              <SelectItem key={i + 1} value={String(i + 1)}>
                                每月第 {i + 1} 天
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                    )}
                  </div>

                  <p className="text-xs text-gray-500">
                    {formData.cycleType === "daily" && "系统将每天自动生成盘点任务"}
                    {formData.cycleType === "weekly" && `系统将每周${["一", "二", "三", "四", "五", "六", "日"][formData.cycleDay - 1]}自动生成盘点任务`}
                    {formData.cycleType === "monthly" && `系统将每月第${formData.cycleDay}天自动生成盘点任务`}
                  </p>

                  {/* 轮转策略 */}
                  <div className="space-y-3 border-t border-green-300 pt-3">
                    <div className="text-sm font-medium text-green-700">轮转策略</div>

                    <div className="grid grid-cols-3 gap-2">
                      <button
                        type="button"
                        onClick={() => setFormData({ ...formData, cycleStrategy: "zone_rotation" })}
                        className={`p-2 text-left rounded border text-sm transition-colors ${
                          formData.cycleStrategy === "zone_rotation"
                            ? "border-green-500 bg-green-100 text-green-700"
                            : "border-gray-200 bg-white hover:border-green-300"
                        }`}
                      >
                        <div className="font-medium">按库区轮转</div>
                        <div className="text-xs text-gray-500">每次盘一个库区</div>
                      </button>
                      <button
                        type="button"
                        onClick={() => setFormData({ ...formData, cycleStrategy: "sku_rotation" })}
                        className={`p-2 text-left rounded border text-sm transition-colors ${
                          formData.cycleStrategy === "sku_rotation"
                            ? "border-green-500 bg-green-100 text-green-700"
                            : "border-gray-200 bg-white hover:border-green-300"
                        }`}
                      >
                        <div className="font-medium">按SKU轮转</div>
                        <div className="text-xs text-gray-500">每次盘部分SKU</div>
                      </button>
                      <button
                        type="button"
                        onClick={() => setFormData({ ...formData, cycleStrategy: "fixed" })}
                        className={`p-2 text-left rounded border text-sm transition-colors ${
                          formData.cycleStrategy === "fixed"
                            ? "border-green-500 bg-green-100 text-green-700"
                            : "border-gray-200 bg-white hover:border-green-300"
                        }`}
                      >
                        <div className="font-medium">固定范围</div>
                        <div className="text-xs text-gray-500">每次盘相同商品</div>
                      </button>
                    </div>

                    {/* 按库区轮转配置 */}
                    {formData.cycleStrategy === "zone_rotation" && (
                      <div className="space-y-2">
                        <Label>选择轮转库区 <span className="text-red-500">*</span></Label>
                        <div className="grid grid-cols-2 gap-2 max-h-32 overflow-y-auto">
                          {zones.map((z) => (
                            <label
                              key={z.id}
                              className={`flex items-center gap-2 p-2 rounded border cursor-pointer text-sm ${
                                formData.cycleZoneIds.includes(String(z.id))
                                  ? "border-green-500 bg-green-50"
                                  : "border-gray-200 hover:border-green-300"
                              }`}
                            >
                              <input
                                type="checkbox"
                                checked={formData.cycleZoneIds.includes(String(z.id))}
                                onChange={(e) => {
                                  if (e.target.checked) {
                                    setFormData({ ...formData, cycleZoneIds: [...formData.cycleZoneIds, String(z.id)] });
                                  } else {
                                    setFormData({ ...formData, cycleZoneIds: formData.cycleZoneIds.filter(id => id !== String(z.id)) });
                                  }
                                }}
                                className="rounded"
                              />
                              <span>{z.name}</span>
                            </label>
                          ))}
                          {zones.length === 0 && (
                            <div className="col-span-2 text-sm text-gray-500 text-center py-2">请先选择仓库</div>
                          )}
                        </div>
                        {formData.cycleZoneIds.length > 0 && (
                          <p className="text-xs text-gray-500">已选择 {formData.cycleZoneIds.length} 个库区，将按顺序轮转盘点</p>
                        )}
                      </div>
                    )}

                    {/* 按SKU轮转配置 */}
                    {formData.cycleStrategy === "sku_rotation" && (
                      <div className="space-y-2">
                        <Label>每次盘点比例</Label>
                        <div className="flex items-center gap-3">
                          <input
                            type="range"
                            min="5"
                            max="30"
                            step="5"
                            value={formData.cycleSkuPercent}
                            onChange={(e) => setFormData({ ...formData, cycleSkuPercent: Number(e.target.value) })}
                            className="flex-1"
                          />
                          <span className="text-sm font-medium w-16 text-right">{formData.cycleSkuPercent}%</span>
                        </div>
                        <p className="text-xs text-gray-500">
                          每次盘点 {formData.cycleSkuPercent}% 的SKU，约需 {Math.ceil(100 / formData.cycleSkuPercent)} 次完成一轮
                        </p>
                      </div>
                    )}

                    {/* 固定范围配置 */}
                    {formData.cycleStrategy === "fixed" && (
                      <p className="text-xs text-gray-500">
                        固定范围将每次盘点仓库的所有库存商品
                      </p>
                    )}
                  </div>
                </div>
              )}

              {/* 计划日期（全盘和抽盘显示） */}
              {formData.type !== "循环盘" && (
                <div className="space-y-2">
                  <Label>计划日期 <span className="text-red-500">*</span></Label>
                  <Input
                    type="date"
                    value={formData.planDate}
                    min={getTodayDate()}
                    onChange={(e) => setFormData({ ...formData, planDate: e.target.value })}
                  />
                  <p className="text-xs text-gray-500">计划执行盘点的日期，当天将锁定相关库位</p>
                </div>
              )}

              {/* 备注 */}
              <div className="space-y-2">
                <Label>备注</Label>
                <Textarea
                  value={formData.remark}
                  onChange={(e) => setFormData({ ...formData, remark: e.target.value })}
                  placeholder="选填，可记录盘点原因或特殊说明"
                  rows={2}
                />
              </div>
            </div>

            <DialogFooter>
              <button
                type="button"
                onClick={() => setIsFormOpen(false)}
                className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
              >
                取消
              </button>
              <button
                type="submit"
                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
              >
                确认创建
              </button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* 取消确认对话框 */}
      <Dialog open={cancelDialog.open} onOpenChange={(open) => setCancelDialog({ ...cancelDialog, open })}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>取消盘点单</DialogTitle>
          </DialogHeader>
          <div className="py-4 space-y-4">
            <p className="text-sm text-gray-600">
              确定要取消盘点单 <span className="font-medium">{cancelDialog.order?.orderNo}</span> 吗？
              {cancelDialog.order?.status !== 0 && (
                <span className="block mt-1 text-orange-600 text-xs">
                  该盘点单状态为"{cancelDialog.order?.statusName}"，将强制取消。
                </span>
              )}
            </p>
            <div className="space-y-2">
              <Label>取消原因</Label>
              <Textarea
                value={cancelDialog.reason}
                onChange={(e) => setCancelDialog({ ...cancelDialog, reason: e.target.value })}
                placeholder="请输入取消原因（可选）"
                rows={2}
              />
            </div>
          </div>
          <DialogFooter>
            <button
              type="button"
              onClick={() => setCancelDialog({ open: false, order: null, reason: "" })}
              className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
            >
              返回
            </button>
            <button
              type="button"
              onClick={handleCancelOrder}
              className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
            >
              确认取消
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
