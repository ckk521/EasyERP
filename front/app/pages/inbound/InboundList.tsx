import { useState, useEffect, useCallback } from "react";
import { Plus, Search, Eye, Edit, Trash2, RotateCcw, XCircle, ChevronDown, AlertCircle } from "lucide-react";
import { useNavigate } from "react-router";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Button } from "../../components/ui/button";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Textarea } from "../../components/ui/textarea";
import { toast } from "sonner";

// ==================== 类型定义 ====================

interface InboundOrderItem {
  id: number;
  productId: number;
  skuCode: string;
  productName: string;
  barcode: string;
  expectedQty: number;
  receivedQty: number;
  qualifiedQty: number;
  rejectedQty: number;
  status: number;
}

interface InboundOrder {
  id: number;
  orderNo: string;
  deliveryBatchNo: string;
  orderType: number;
  orderTypeName: string;
  poNo: string;
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  expectedDate: string;
  status: number;
  statusName: string;
  totalExpectedQty: number;
  totalReceivedQty: number;
  totalQualifiedQty: number;
  totalRejectedQty: number;
  remark: string;
  cancelReason: string;
  createTime: string;
  items?: InboundOrderItem[];
}

interface Supplier {
  id: number;
  code: string;
  name: string;
  contact?: string;
  phone?: string;
}

interface Warehouse {
  id: number;
  code: string;
  name: string;
  type?: number;
  manager?: string;
}

// 供应商商品关系（包含商品信息和采购价格）
interface SupplierProduct {
  id: number;
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  productId: number;
  skuCode: string;
  productName: string;
  barcode?: string;
  purchasePrice: number;
  minOrderQty: number;
  leadTime: number;
  supplierSkuCode?: string;
  status: number;
}

interface ApiResponse<T> {
  success: boolean;
  code: number;
  message: string;
  data: T;
}

interface PagedResult<T> {
  list: T[];
  total: number;
}

// ==================== 常量定义 ====================

const ORDER_TYPES = [
  { value: 1, label: "采购入库" },
  { value: 2, label: "退货入库" },
  { value: 3, label: "调拨入库" },
  { value: 4, label: "赠品入库" },
  { value: 5, label: "其他入库" },
];

const STATUS_OPTIONS = [
  { value: -1, label: "全部状态" },
  { value: 0, label: "待收货" },
  { value: 1, label: "收货中" },
  { value: 2, label: "验收中" },
  { value: 3, label: "待上架" },
  { value: 4, label: "已完成" },
  { value: 9, label: "已取消" },
];

const STATUS_COLORS: Record<number, string> = {
  0: "bg-yellow-100 text-yellow-700",
  1: "bg-blue-100 text-blue-700",
  2: "bg-blue-100 text-blue-700",
  3: "bg-orange-100 text-orange-700",
  4: "bg-green-100 text-green-700",
  9: "bg-gray-100 text-gray-500",
};

// ==================== API 函数 ====================

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

// ==================== 主组件 ====================

export default function InboundList() {
  const navigate = useNavigate();

  // 状态管理
  const [orders, setOrders] = useState<InboundOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(20);

  // 筛选条件
  const [searchText, setSearchText] = useState("");
  const [statusFilter, setStatusFilter] = useState(-1);
  const [typeFilter, setTypeFilter] = useState(-1);
  const [deliveryBatchNo, setDeliveryBatchNo] = useState("");

  // 表单相关
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isCancelOpen, setIsCancelOpen] = useState(false);
  const [editingOrder, setEditingOrder] = useState<InboundOrder | null>(null);
  const [cancelOrderId, setCancelOrderId] = useState<number | null>(null);
  const [cancelReason, setCancelReason] = useState("");

  // 下拉数据
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [supplierProducts, setSupplierProducts] = useState<SupplierProduct[]>([]); // 供应商商品关系
  const [loadingProducts, setLoadingProducts] = useState(false);
  const [showProductSelector, setShowProductSelector] = useState(false);
  const [currentItemIndex, setCurrentItemIndex] = useState<number | null>(null);
  const [productSearch, setProductSearch] = useState("");

  // 表单数据
  const [formData, setFormData] = useState({
    orderType: 1,
    poNo: "",
    deliveryBatchNo: "",
    supplierId: 0,
    supplierCode: "",
    supplierName: "",
    warehouseId: 0,
    warehouseCode: "",
    warehouseName: "",
    expectedDate: "",
    remark: "",
    items: [
      {
        productId: 0,
        skuCode: "",
        productName: "",
        barcode: "",
        expectedQty: 1,
      },
    ],
  });

  // 加载供应商和仓库数据
  useEffect(() => {
    const loadDropdownData = async () => {
      try {
        const [supplierData, warehouseData] = await Promise.all([
          fetchApi<PagedResult<Supplier>>(`${API_BASE}/base/suppliers?limit=100&status=1`),
          fetchApi<PagedResult<Warehouse>>(`${API_BASE}/base/warehouses?limit=100&status=1`),
        ]);
        setSuppliers(supplierData.list || []);
        setWarehouses(warehouseData.list || []);
      } catch (error) {
        console.error("Failed to load dropdown data:", error);
      }
    };
    loadDropdownData();
  }, []);

  // 加载供应商关联的商品
  const loadSupplierProducts = useCallback(async (supplierId: number) => {
    if (!supplierId) {
      setSupplierProducts([]);
      return;
    }
    try {
      setLoadingProducts(true);
      const data = await fetchApi<SupplierProduct[]>(
        `${API_BASE}/base/supplier-products/by-supplier/${supplierId}`
      );
      setSupplierProducts(data || []);
    } catch (error) {
      console.error("Failed to load supplier products:", error);
      setSupplierProducts([]);
    } finally {
      setLoadingProducts(false);
    }
  }, []);

  // 生成采购订单号（日期+时分秒+2位序号）
  const generatePoNo = useCallback(() => {
    const today = new Date();
    const dateStr = today.toISOString().slice(0, 10).replace(/-/g, "");
    const timeStr = today.toTimeString().slice(0, 8).replace(/:/g, "");
    const seq = String(Math.floor(Math.random() * 99) + 1).padStart(2, "0");
    return `PO${dateStr}${timeStr}${seq}`;
  }, []);

  // 生成送货批次号（与采购订单号关联）
  const generateDeliveryBatchNo = useCallback((poNo: string) => {
    if (!poNo) return "";
    // 从采购订单号提取日期部分
    const dateMatch = poNo.match(/PO(\d{8})/);
    const dateStr = dateMatch ? dateMatch[1] : new Date().toISOString().slice(0, 10).replace(/-/g, "");
    const timeStr = new Date().toTimeString().slice(0, 5).replace(":", "");
    // 时段后缀: AM(凌晨0-5) MO(上午6-11) PM(下午12-17) EV(晚上18-23)
    const suffix = ["AM", "MO", "PM", "EV"][Math.floor(new Date().getHours() / 6)];
    return `${dateStr}-${timeStr}-${suffix}`;
  }, []);

  // 加载数据
  const fetchOrders = useCallback(async () => {
    try {
      setLoading(true);
      const params = new URLSearchParams();
      params.set("page", String(page));
      params.set("limit", String(limit));
      if (searchText) params.set("keyword", searchText);
      if (statusFilter >= 0) params.set("status", String(statusFilter));
      if (typeFilter >= 0) params.set("orderType", String(typeFilter));
      if (deliveryBatchNo) params.set("deliveryBatchNo", deliveryBatchNo);

      const data = await fetchApi<PagedResult<InboundOrder>>(`${API_BASE}/inbound/orders?${params}`);
      setOrders(data.list || []);
      setTotal(data.total || 0);
    } catch (error) {
      console.error("Failed to fetch orders:", error);
      setOrders([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [page, limit, searchText, statusFilter, typeFilter, deliveryBatchNo]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  // 重置表单
  const resetForm = () => {
    const poNo = generatePoNo();
    setFormData({
      orderType: 1,
      poNo: poNo,
      deliveryBatchNo: generateDeliveryBatchNo(poNo),
      supplierId: 0,
      supplierCode: "",
      supplierName: "",
      warehouseId: 0,
      warehouseCode: "",
      warehouseName: "",
      expectedDate: new Date().toISOString().slice(0, 10),
      remark: "",
      items: [
        {
          productId: 0,
          skuCode: "",
          productName: "",
          barcode: "",
          expectedQty: 1,
        },
      ],
    });
    setEditingOrder(null);
  };

  // 打开创建表单
  const openCreateForm = () => {
    resetForm();
    setIsFormOpen(true);
  };

  // 打开编辑表单
  const openEditForm = async (order: InboundOrder) => {
    try {
      const detail = await fetchApi<InboundOrder>(`${API_BASE}/inbound/orders/${order.id}`);
      setEditingOrder(detail);
      setFormData({
        orderType: detail.orderType,
        poNo: detail.poNo || "",
        deliveryBatchNo: detail.deliveryBatchNo || "",
        supplierId: detail.supplierId || 0,
        supplierCode: detail.supplierCode || "",
        supplierName: detail.supplierName || "",
        warehouseId: detail.warehouseId || 0,
        warehouseCode: detail.warehouseCode || "",
        warehouseName: detail.warehouseName || "",
        expectedDate: detail.expectedDate || "",
        remark: detail.remark || "",
        items: detail.items?.map(item => ({
          productId: item.productId,
          skuCode: item.skuCode,
          productName: item.productName || "",
          barcode: item.barcode || "",
          expectedQty: item.expectedQty,
        })) || [],
      });
      setIsFormOpen(true);
    } catch (error) {
      toast.error("获取入库单详情失败");
    }
  };

  // 选择供应商
  const handleSupplierChange = (supplierId: number) => {
    const supplier = suppliers.find(s => s.id === supplierId);
    if (supplier) {
      setFormData(prev => ({
        ...prev,
        supplierId: supplier.id,
        supplierCode: supplier.code,
        supplierName: supplier.name,
        // 清空商品明细，因为换了供应商
        items: [{ productId: 0, skuCode: "", productName: "", barcode: "", expectedQty: 1 }],
      }));
      // 加载该供应商关联的商品
      loadSupplierProducts(supplier.id);
    }
  };

  // 选择仓库
  const handleWarehouseChange = (warehouseId: number) => {
    const warehouse = warehouses.find(w => w.id === warehouseId);
    if (warehouse) {
      setFormData(prev => ({
        ...prev,
        warehouseId: warehouse.id,
        warehouseCode: warehouse.code,
        warehouseName: warehouse.name,
      }));
    }
  };

  // 选择商品
  const handleProductSelect = (product: SupplierProduct) => {
    if (currentItemIndex === null) return;

    setFormData(prev => ({
      ...prev,
      items: prev.items.map((item, i) =>
        i === currentItemIndex ? {
          ...item,
          productId: product.productId,
          skuCode: product.skuCode,
          productName: product.productName,
          barcode: product.barcode || "",
        } : item
      ),
    }));
    setShowProductSelector(false);
    setCurrentItemIndex(null);
    setProductSearch("");
  };

  // 提交表单
  const handleSubmit = async () => {
    try {
      // 验证必填字段
      if (!formData.warehouseId) {
        toast.error("请选择仓库");
        return;
      }
      if (!formData.supplierId) {
        toast.error("请选择供应商");
        return;
      }
      if (formData.orderType === 1 && !formData.poNo) {
        toast.error("采购入库必须填写采购订单号");
        return;
      }
      if (formData.items.length === 0 || !formData.items[0].productId) {
        toast.error("请添加商品明细");
        return;
      }

      if (editingOrder) {
        await fetchApi(`${API_BASE}/inbound/orders/${editingOrder.id}`, {
          method: "PUT",
          body: JSON.stringify(formData),
        });
        toast.success("入库单更新成功");
      } else {
        await fetchApi<{ id: number }>(`${API_BASE}/inbound/orders`, {
          method: "POST",
          body: JSON.stringify(formData),
        });
        toast.success("入库单创建成功");
      }
      setIsFormOpen(false);
      resetForm();
      fetchOrders();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "操作失败";
      toast.error(message);
    }
  };

  // 取消入库单
  const handleCancel = async () => {
    if (!cancelOrderId || !cancelReason.trim()) {
      toast.error("请填写取消原因");
      return;
    }
    try {
      await fetchApi(`${API_BASE}/inbound/orders/${cancelOrderId}/cancel`, {
        method: "POST",
        body: JSON.stringify({ reason: cancelReason }),
      });
      toast.success("入库单已取消");
      setIsCancelOpen(false);
      setCancelOrderId(null);
      setCancelReason("");
      fetchOrders();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "取消失败";
      toast.error(message);
    }
  };

  // 重新激活
  const handleReactivate = async (id: number) => {
    try {
      await fetchApi(`${API_BASE}/inbound/orders/${id}/reactivate`, {
        method: "POST",
      });
      toast.success("入库单已重新激活");
      fetchOrders();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "重新激活失败";
      toast.error(message);
    }
  };

  // 删除入库单
  const handleDelete = async (id: number) => {
    if (!confirm("确定要删除这个入库单吗？")) return;
    try {
      await fetchApi(`${API_BASE}/inbound/orders/${id}`, {
        method: "DELETE",
      });
      toast.success("入库单已删除");
      fetchOrders();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "删除失败";
      toast.error(message);
    }
  };

  // 添加商品明细行
  const addItemRow = () => {
    setFormData(prev => ({
      ...prev,
      items: [
        ...prev.items,
        { productId: 0, skuCode: "", productName: "", barcode: "", expectedQty: 1 },
      ],
    }));
  };

  // 删除商品明细行
  const removeItemRow = (index: number) => {
    if (formData.items.length <= 1) return;
    setFormData(prev => ({
      ...prev,
      items: prev.items.filter((_, i) => i !== index),
    }));
  };

  // 更新商品明细
  const updateItem = (index: number, field: string, value: string | number) => {
    setFormData(prev => ({
      ...prev,
      items: prev.items.map((item, i) =>
        i === index ? { ...item, [field]: value } : item
      ),
    }));
  };

  // 过滤供应商商品
  const filteredProducts = supplierProducts.filter(p =>
    !productSearch ||
    p.skuCode?.toLowerCase().includes(productSearch.toLowerCase()) ||
    p.productName?.toLowerCase().includes(productSearch.toLowerCase()) ||
    p.barcode?.toLowerCase().includes(productSearch.toLowerCase()) ||
    p.supplierSkuCode?.toLowerCase().includes(productSearch.toLowerCase())
  );

  // 表格列定义
  const columns = [
    { key: "orderNo", title: "入库单号", width: "150px" },
    { key: "orderTypeName", title: "类型", width: "90px" },
    { key: "poNo", title: "采购单号", width: "130px" },
    { key: "supplierName", title: "供应商", width: "120px" },
    { key: "deliveryBatchNo", title: "送货批次号", width: "110px" },
    { key: "warehouseName", title: "仓库", width: "90px" },
    {
      key: "totalExpectedQty",
      title: "预期数量",
      width: "80px",
      render: (v: number) => v?.toLocaleString() || 0,
    },
    {
      key: "status",
      title: "状态",
      width: "80px",
      render: (value: number, order: InboundOrder) => (
        <span className={`px-2 py-0.5 rounded text-xs ${STATUS_COLORS[value] || "bg-gray-100"}`}>
          {order.statusName}
        </span>
      ),
    },
    { key: "createTime", title: "创建时间", width: "150px" },
    {
      key: "actions",
      title: "操作",
      width: "160px",
      render: (_: unknown, order: InboundOrder) => (
        <div className="flex gap-1">
          <button
            onClick={() => navigate(`/inbound/${order.id}`)}
            className="p-1 hover:bg-gray-100 rounded text-blue-600"
            title="查看详情"
          >
            <Eye size={14} />
          </button>
          {order.status === 0 && (
            <>
              <button
                onClick={() => openEditForm(order)}
                className="p-1 hover:bg-gray-100 rounded text-blue-600"
                title="编辑"
              >
                <Edit size={14} />
              </button>
              <button
                onClick={() => handleDelete(order.id)}
                className="p-1 hover:bg-gray-100 rounded text-red-600"
                title="删除"
              >
                <Trash2 size={14} />
              </button>
              <button
                onClick={() => {
                  setCancelOrderId(order.id);
                  setIsCancelOpen(true);
                }}
                className="p-1 hover:bg-gray-100 rounded text-orange-600"
                title="取消"
              >
                <XCircle size={14} />
              </button>
            </>
          )}
          {order.status === 9 && (
            <button
              onClick={() => handleReactivate(order.id)}
              className="p-1 hover:bg-gray-100 rounded text-green-600"
              title="重新激活"
            >
              <RotateCcw size={14} />
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">入库管理</h2>
        <button
          onClick={openCreateForm}
          className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm"
        >
          <Plus size={16} />
          新建入库单
        </button>
      </div>

      {/* 筛选区域 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex flex-wrap gap-4 mb-4">
          <div className="flex-1 min-w-[200px] relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="搜索入库单号/采购单号/供应商"
              className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
            />
          </div>
          <input
            type="text"
            placeholder="送货批次号"
            className="w-32 px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={deliveryBatchNo}
            onChange={(e) => setDeliveryBatchNo(e.target.value)}
          />
          <select
            className="px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={statusFilter}
            onChange={(e) => setStatusFilter(Number(e.target.value))}
          >
            {STATUS_OPTIONS.map((s) => (
              <option key={s.value} value={s.value}>{s.label}</option>
            ))}
          </select>
          <select
            className="px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={typeFilter}
            onChange={(e) => setTypeFilter(Number(e.target.value))}
          >
            <option value={-1}>全部类型</option>
            {ORDER_TYPES.map((t) => (
              <option key={t.value} value={t.value}>{t.label}</option>
            ))}
          </select>
          <button
            onClick={() => fetchOrders()}
            className="px-4 py-1.5 bg-gray-100 text-gray-700 rounded hover:bg-gray-200 text-sm"
          >
            查询
          </button>
        </div>

        {/* 数据表格 */}
        {loading ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">加载中...</div>
          </div>
        ) : orders.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">暂无入库单数据</div>
            <div className="text-sm text-gray-400 mt-1">点击上方"新建入库单"创建</div>
          </div>
        ) : (
          <DataTable columns={columns} data={orders} />
        )}

        {/* 分页 */}
        {total > limit && (
          <div className="flex items-center justify-between mt-4 pt-4 border-t">
            <div className="text-sm text-gray-500">
              共 {total} 条记录
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => setPage(p => Math.max(1, p - 1))}
                disabled={page === 1}
                className="px-3 py-1 border rounded text-sm disabled:opacity-50"
              >
                上一页
              </button>
              <span className="px-3 py-1 text-sm">{page} / {Math.ceil(total / limit)}</span>
              <button
                onClick={() => setPage(p => p + 1)}
                disabled={page * limit >= total}
                className="px-3 py-1 border rounded text-sm disabled:opacity-50"
              >
                下一页
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 创建/编辑表单弹窗 */}
      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-5xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editingOrder ? "编辑入库单" : "新建入库单"}</DialogTitle>
          </DialogHeader>

          <div className="space-y-5 py-4">
            {/* 基本信息 - 第一行 */}
            <div className="grid grid-cols-2 gap-6">
              <div>
                <Label className="text-sm font-medium">入库类型 *</Label>
                <select
                  className="w-full mt-1.5 px-3 py-2 border rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  value={formData.orderType}
                  onChange={(e) => setFormData(prev => ({ ...prev, orderType: Number(e.target.value) }))}
                  disabled={!!editingOrder}
                >
                  {ORDER_TYPES.map((t) => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <Label className="text-sm font-medium">预计到货日期</Label>
                <Input
                  type="date"
                  value={formData.expectedDate}
                  onChange={(e) => setFormData(prev => ({ ...prev, expectedDate: e.target.value }))}
                  className="mt-1.5"
                />
              </div>
            </div>

            {/* 编号信息 - 第二行 */}
            <div className="grid grid-cols-2 gap-6">
              <div>
                <Label className="text-sm font-medium">采购订单号 {formData.orderType === 1 && "*"}</Label>
                <div className="flex gap-2 mt-1.5">
                  <Input
                    value={formData.poNo}
                    onChange={(e) => {
                      const poNo = e.target.value;
                      setFormData(prev => ({
                        ...prev,
                        poNo,
                        deliveryBatchNo: generateDeliveryBatchNo(poNo),
                      }));
                    }}
                    placeholder={formData.orderType === 1 ? "点击生成按钮自动生成" : ""}
                    disabled={formData.orderType !== 1}
                    className="flex-1 font-mono"
                  />
                  {formData.orderType === 1 && !editingOrder && (
                    <button
                      type="button"
                      onClick={() => {
                        const poNo = generatePoNo();
                        setFormData(prev => ({
                          ...prev,
                          poNo,
                          deliveryBatchNo: generateDeliveryBatchNo(poNo),
                        }));
                      }}
                      className="px-4 py-2 bg-blue-50 text-blue-600 rounded-md text-sm hover:bg-blue-100 font-medium"
                    >
                      生成
                    </button>
                  )}
                </div>
              </div>
              <div>
                <Label className="text-sm font-medium">送货批次号</Label>
                <Input
                  value={formData.deliveryBatchNo}
                  onChange={(e) => setFormData(prev => ({ ...prev, deliveryBatchNo: e.target.value }))}
                  placeholder="根据采购订单号自动生成"
                  className="mt-1.5 bg-gray-50 font-mono"
                />
              </div>
            </div>

            {/* 供应商和仓库 - 第三行 */}
            <div className="grid grid-cols-2 gap-6">
              <div>
                <Label className="text-sm font-medium">供应商 *</Label>
                <div className="relative mt-1.5">
                  <select
                    className="w-full px-3 py-2 border rounded-md text-sm appearance-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    value={formData.supplierId || ""}
                    onChange={(e) => handleSupplierChange(Number(e.target.value))}
                  >
                    <option value="">请选择供应商</option>
                    {suppliers.map((s) => (
                      <option key={s.id} value={s.id}>{s.name} ({s.code})</option>
                    ))}
                  </select>
                  <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                </div>
              </div>
              <div>
                <Label className="text-sm font-medium">仓库 *</Label>
                <div className="relative mt-1.5">
                  <select
                    className="w-full px-3 py-2 border rounded-md text-sm appearance-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    value={formData.warehouseId || ""}
                    onChange={(e) => handleWarehouseChange(Number(e.target.value))}
                  >
                    <option value="">请选择仓库</option>
                    {warehouses.map((w) => (
                      <option key={w.id} value={w.id}>{w.name} ({w.code})</option>
                    ))}
                  </select>
                  <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                </div>
              </div>
            </div>

            {/* 备注 */}
            <div>
              <Label className="text-sm font-medium">备注</Label>
              <Textarea
                value={formData.remark}
                onChange={(e) => setFormData(prev => ({ ...prev, remark: e.target.value }))}
                placeholder="输入备注信息"
                rows={2}
                className="mt-1.5"
              />
            </div>

            {/* 商品明细 */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <Label className="text-sm font-medium">商品明细 *</Label>
                <button
                  type="button"
                  onClick={addItemRow}
                  className="text-sm text-blue-600 hover:text-blue-700 font-medium"
                >
                  + 添加商品
                </button>
              </div>
              <div className="border rounded-md overflow-hidden">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-4 py-2.5 text-left font-medium text-gray-600">SKU编码 *</th>
                      <th className="px-4 py-2.5 text-left font-medium text-gray-600">商品名称</th>
                      <th className="px-4 py-2.5 text-left font-medium text-gray-600 w-36">条码</th>
                      <th className="px-4 py-2.5 text-left font-medium text-gray-600 w-28">预期数量 *</th>
                      <th className="px-4 py-2.5 w-14"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {formData.items.map((item, index) => (
                      <tr key={index} className="border-t hover:bg-gray-50">
                        <td className="px-4 py-2">
                          <div className="flex gap-2">
                            <input
                              type="text"
                              className="flex-1 px-3 py-1.5 border rounded-md text-sm bg-gray-50 font-mono cursor-pointer hover:bg-gray-100"
                              value={item.skuCode}
                              placeholder={formData.supplierId ? "点击选择商品" : "请先选择供应商"}
                              readOnly
                              onClick={() => {
                                if (!formData.supplierId) {
                                  toast.warning("请先选择供应商");
                                  return;
                                }
                                setCurrentItemIndex(index);
                                setShowProductSelector(true);
                                setProductSearch("");
                              }}
                            />
                            <button
                              type="button"
                              onClick={() => {
                                if (!formData.supplierId) {
                                  toast.warning("请先选择供应商");
                                  return;
                                }
                                setCurrentItemIndex(index);
                                setShowProductSelector(true);
                                setProductSearch("");
                              }}
                              className={`px-3 py-1.5 rounded-md text-sm font-medium ${
                                formData.supplierId
                                  ? "bg-blue-50 text-blue-600 hover:bg-blue-100"
                                  : "bg-gray-100 text-gray-400 cursor-not-allowed"
                              }`}
                            >
                              选择
                            </button>
                          </div>
                        </td>
                        <td className="px-4 py-2">
                          <input
                            type="text"
                            className="w-full px-3 py-1.5 border rounded-md text-sm bg-gray-50"
                            value={item.productName}
                            readOnly
                            placeholder="自动填充"
                          />
                        </td>
                        <td className="px-4 py-2">
                          <input
                            type="text"
                            className="w-full px-3 py-1.5 border rounded-md text-sm bg-gray-50 font-mono"
                            value={item.barcode}
                            readOnly
                            placeholder="自动填充"
                          />
                        </td>
                        <td className="px-4 py-2">
                          <input
                            type="number"
                            min="1"
                            className="w-full px-3 py-1.5 border rounded-md text-sm text-center"
                            value={item.expectedQty}
                            onChange={(e) => updateItem(index, "expectedQty", Number(e.target.value))}
                          />
                        </td>
                        <td className="px-4 py-2 text-center">
                          {formData.items.length > 1 && (
                            <button
                              type="button"
                              onClick={() => removeItemRow(index)}
                              className="p-1 text-red-500 hover:text-red-600 hover:bg-red-50 rounded"
                            >
                              <Trash2 size={16} />
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <DialogFooter className="gap-2">
            <Button variant="outline" onClick={() => setIsFormOpen(false)}>
              取消
            </Button>
            <Button onClick={handleSubmit} className="px-6">
              {editingOrder ? "保存" : "创建"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 商品选择器弹窗 */}
      <Dialog open={showProductSelector} onOpenChange={setShowProductSelector}>
        <DialogContent className="max-w-4xl">
          <DialogHeader>
            <DialogTitle>
              选择商品
              {formData.supplierName && (
                <span className="text-sm font-normal text-gray-500 ml-2">
                  (供应商: {formData.supplierName})
                </span>
              )}
            </DialogTitle>
          </DialogHeader>
          <div className="py-4">
            {!formData.supplierId ? (
              <div className="text-center py-8">
                <AlertCircle className="mx-auto text-yellow-500 mb-3" size={48} />
                <p className="text-gray-600 font-medium">请先选择供应商</p>
                <p className="text-sm text-gray-400 mt-1">商品列表将根据供应商关联关系显示</p>
              </div>
            ) : loadingProducts ? (
              <div className="text-center py-8 text-gray-500">加载商品中...</div>
            ) : (
              <>
                <input
                  type="text"
                  placeholder="搜索SKU编码/商品名称/条码/供应商货号"
                  className="w-full px-4 py-2.5 border rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  value={productSearch}
                  onChange={(e) => setProductSearch(e.target.value)}
                  autoFocus
                />
                <div className="max-h-80 overflow-y-auto border rounded-md mt-4">
                  {filteredProducts.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">
                      <p>该供应商暂无关联商品</p>
                      <p className="text-sm mt-1">请先在供应商管理中配置商品关系</p>
                    </div>
                  ) : (
                    <table className="w-full text-sm">
                      <thead className="bg-gray-50 sticky top-0">
                        <tr>
                          <th className="px-4 py-2.5 text-left font-medium text-gray-600">SKU编码</th>
                          <th className="px-4 py-2.5 text-left font-medium text-gray-600">商品名称</th>
                          <th className="px-4 py-2.5 text-left font-medium text-gray-600">条码</th>
                          <th className="px-4 py-2.5 text-right font-medium text-gray-600">采购价</th>
                          <th className="px-4 py-2.5 text-right font-medium text-gray-600">起订量</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredProducts.map((product) => (
                          <tr
                            key={product.id}
                            className="border-t hover:bg-blue-50 cursor-pointer"
                            onClick={() => handleProductSelect(product)}
                          >
                            <td className="px-4 py-2.5 font-mono">{product.skuCode}</td>
                            <td className="px-4 py-2.5">{product.productName}</td>
                            <td className="px-4 py-2.5 font-mono">{product.barcode || '-'}</td>
                            <td className="px-4 py-2.5 text-right text-green-600 font-medium">
                              ¥{product.purchasePrice?.toFixed(2) || '-'}
                            </td>
                            <td className="px-4 py-2.5 text-right">{product.minOrderQty || '-'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              </>
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* 取消确认弹窗 */}
      <Dialog open={isCancelOpen} onOpenChange={setIsCancelOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>取消入库单</DialogTitle>
          </DialogHeader>
          <div className="py-4">
            <Label>取消原因 *</Label>
            <Textarea
              className="mt-2"
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              placeholder="请输入取消原因"
              rows={3}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsCancelOpen(false)}>
              取消
            </Button>
            <Button variant="destructive" onClick={handleCancel}>
              确认取消
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
