import { useState, useEffect } from "react";
import { Plus, Search, Eye, X, Trash2, AlertTriangle } from "lucide-react";
import { useNavigate } from "react-router";
import DataTable from "../../components/DataTable";
import { toast } from "sonner";

interface OutboundOrder {
  id: number;
  orderNo: string;
  soNo?: string;
  orderType: number;
  customerName?: string;
  supplierName?: string;
  targetWarehouseName?: string;
  warehouseName?: string;
  totalQty: number;
  status: number;
  statusName: string;
  priority: number;
  priorityName: string;
  createTime: string;
  shipTime?: string;
}

interface Warehouse {
  id: number;
  code: string;
  name: string;
}

interface Customer {
  id: number;
  code: string;
  name: string;
  contact?: string;
  phone?: string;
  address?: string;
}

interface Supplier {
  id: number;
  code: string;
  name: string;
  contact?: string;
  phone?: string;
  address?: string;
}

interface Product {
  id: number;
  skuCode: string;
  nameCn: string;
  barcode?: string;
  categoryName?: string;
}

interface InventorySummary {
  productId: number;
  skuCode: string;
  productName: string;
  availableQty: number;
}

interface OutboundItem {
  productId?: number;
  skuCode: string;
  productName?: string;
  barcode?: string;
  qty: number;
  availableQty?: number;
  locationId?: number;
  locationCode?: string;
  batchNo?: string;
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
  if (!data.success) {
    throw new Error(data.message || "API Error");
  }
  return data.data as T;
}

const statusOptions = [
  { value: "", label: "全部" },
  { value: "0", label: "待分配" },
  { value: "1", label: "已分配" },
  { value: "2", label: "拣货中" },
  { value: "3", label: "待打包" },
  { value: "4", label: "待发货" },
  { value: "5", label: "已发货" },
  { value: "9", label: "已取消" },
];

const orderTypeOptions = [
  { value: 1, label: "销售出库" },
  { value: 2, label: "调拨出库" },
  { value: 3, label: "退货出库" },
  { value: 4, label: "报废出库" },
  { value: 5, label: "样品出库" },
];

const sourceTypeOptions = [
  { value: 1, label: "ERP推送" },
  { value: 2, label: "手工创建" },
  { value: 3, label: "调拨申请" },
];

const priorityOptions = [
  { value: 1, label: "紧急" },
  { value: 2, label: "高" },
  { value: 3, label: "中" },
  { value: 4, label: "低" },
];

export default function OutboundList() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<OutboundOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [showCreateModal, setShowCreateModal] = useState(false);

  // 基础数据
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [inventorySummary, setInventorySummary] = useState<InventorySummary[]>([]);
  const [productSearch, setProductSearch] = useState("");
  const [showProductDropdown, setShowProductDropdown] = useState(false);

  // 创建表单状态
  const [formData, setFormData] = useState({
    orderType: 1,
    sourceType: 2,
    soNo: "",
    customerId: null as number | null,
    customerCode: "",
    customerName: "",
    customerPhone: "",
    customerAddress: "",
    supplierId: null as number | null,
    supplierCode: "",
    supplierName: "",
    targetWarehouseId: null as number | null,
    targetWarehouseCode: "",
    targetWarehouseName: "",
    warehouseId: null as number | null,
    warehouseCode: "",
    warehouseName: "",
    priority: 3,
    logisticsCompany: "",
    receiverName: "",
    receiverPhone: "",
    receiverAddress: "",
    remark: "",
    items: [] as OutboundItem[],
  });
  const [newItem, setNewItem] = useState<OutboundItem>({
    skuCode: "",
    productName: "",
    qty: 1,
    availableQty: 0,
  });

  useEffect(() => {
    fetchOrders();
  }, []);

  // 加载基础数据
  useEffect(() => {
    if (showCreateModal) {
      loadBaseData();
    }
  }, [showCreateModal]);

  // 仓库变化时加载库存
  useEffect(() => {
    if (formData.warehouseId) {
      loadInventorySummary(formData.warehouseId);
    }
  }, [formData.warehouseId]);

  async function loadBaseData() {
    try {
      const [whData, custData, suppData, prodData] = await Promise.all([
        fetchApi<{ list: Warehouse[] }>("/api/v1/base/warehouses?page=1&limit=100"),
        fetchApi<{ list: Customer[] }>("/api/v1/base/customers?page=1&limit=100"),
        fetchApi<{ list: Supplier[] }>("/api/v1/base/suppliers?page=1&limit=100"),
        fetchApi<{ list: Product[] }>("/api/v1/base/products?page=1&limit=100"),
      ]);
      setWarehouses(whData.list || []);
      setCustomers(custData.list || []);
      setSuppliers(suppData.list || []);
      setProducts(prodData.list || []);
    } catch (err) {
      console.error("Failed to load base data:", err);
    }
  }

  async function loadInventorySummary(warehouseId: number) {
    try {
      const data = await fetchApi<InventorySummary[]>(
        `/api/v1/inventory/available-summary?warehouseId=${warehouseId}`
      );
      setInventorySummary(data || []);
    } catch (err) {
      console.error("Failed to load inventory summary:", err);
      setInventorySummary([]);
    }
  }

  async function fetchOrders() {
    try {
      setLoading(true);
      const params = new URLSearchParams({
        page: "1",
        limit: "100",
      });
      if (statusFilter) {
        params.set("status", statusFilter);
      }
      if (searchText) {
        params.set("keyword", searchText);
      }
      const data = await fetchApi<{ list: OutboundOrder[]; total: number }>(
        `/api/v1/outbound/orders?${params.toString()}`
      );
      setOrders(data.list || []);
    } catch {
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }

  function handleSearch() {
    fetchOrders();
  }

  function handleWarehouseChange(warehouseId: number) {
    const wh = warehouses.find((w) => w.id === warehouseId);
    if (wh) {
      setFormData({
        ...formData,
        warehouseId: wh.id,
        warehouseCode: wh.code,
        warehouseName: wh.name,
        items: [], // 切换仓库时清空商品明细
      });
    }
  }

  function handleCustomerChange(customerId: number) {
    const cust = customers.find((c) => c.id === customerId);
    if (cust) {
      setFormData({
        ...formData,
        customerId: cust.id,
        customerCode: cust.code,
        customerName: cust.name,
        customerPhone: cust.phone || "",
        customerAddress: cust.address || "",
      });
    }
  }

  function handleSupplierChange(supplierId: number) {
    const sup = suppliers.find((s) => s.id === supplierId);
    if (sup) {
      setFormData({
        ...formData,
        supplierId: sup.id,
        supplierCode: sup.code,
        supplierName: sup.name,
      });
    }
  }

  function handleTargetWarehouseChange(warehouseId: number) {
    const wh = warehouses.find((w) => w.id === warehouseId);
    if (wh) {
      setFormData({
        ...formData,
        targetWarehouseId: wh.id,
        targetWarehouseCode: wh.code,
        targetWarehouseName: wh.name,
      });
    }
  }

  function handleProductSelect(product: Product) {
    // 查找该商品在当前仓库的可用库存
    const inv = inventorySummary.find((i) => i.productId === product.id);
    const availableQty = inv?.availableQty || 0;

    setNewItem({
      productId: product.id,
      skuCode: product.skuCode,
      productName: product.nameCn,
      qty: 1,
      availableQty: availableQty,
    });
    setShowProductDropdown(false);
    setProductSearch("");
  }

  async function handleCreateOrder() {
    try {
      if (formData.items.length === 0) {
        toast.error("请添加商品明细");
        return;
      }
      if (!formData.warehouseId) {
        toast.error("请选择出库仓库");
        return;
      }

      // 根据出库类型验证关联方
      const orderType = formData.orderType;
      if (orderType === 1 || orderType === 5) {
        // 销售出库、样品出库需要客户
        if (!formData.customerId) {
          toast.error("请选择客户");
          return;
        }
      } else if (orderType === 2) {
        // 调拨出库需要目标仓库
        if (!formData.targetWarehouseId) {
          toast.error("请选择目标仓库");
          return;
        }
        if (formData.targetWarehouseId === formData.warehouseId) {
          toast.error("目标仓库不能与出库仓库相同");
          return;
        }
      } else if (orderType === 3) {
        // 退货出库需要供应商
        if (!formData.supplierId) {
          toast.error("请选择供应商");
          return;
        }
      }

      // 验证商品数量
      for (const item of formData.items) {
        if (item.qty <= 0) {
          toast.error(`${item.skuCode} 数量必须大于0`);
          return;
        }
        if (item.availableQty !== undefined && item.qty > item.availableQty) {
          toast.error(`${item.skuCode} 库存不足，可用库存: ${item.availableQty}`);
          return;
        }
      }

      const payload = {
        orderType: formData.orderType,
        sourceType: formData.sourceType,
        soNo: formData.soNo || undefined,
        customerId: formData.customerId || undefined,
        customerCode: formData.customerCode || undefined,
        customerName: formData.customerName || undefined,
        customerPhone: formData.customerPhone || undefined,
        customerAddress: formData.customerAddress || undefined,
        supplierId: formData.supplierId || undefined,
        supplierCode: formData.supplierCode || undefined,
        supplierName: formData.supplierName || undefined,
        targetWarehouseId: formData.targetWarehouseId || undefined,
        targetWarehouseCode: formData.targetWarehouseCode || undefined,
        targetWarehouseName: formData.targetWarehouseName || undefined,
        warehouseId: formData.warehouseId,
        warehouseCode: formData.warehouseCode,
        warehouseName: formData.warehouseName,
        priority: formData.priority,
        logisticsCompany: formData.logisticsCompany || undefined,
        receiverName: formData.receiverName || undefined,
        receiverPhone: formData.receiverPhone || undefined,
        receiverAddress: formData.receiverAddress || undefined,
        remark: formData.remark || undefined,
        items: formData.items.map((item) => ({
          productId: item.productId,
          skuCode: item.skuCode,
          productName: item.productName,
          barcode: item.barcode,
          qty: item.qty,
          locationId: item.locationId,
          locationCode: item.locationCode,
          batchNo: item.batchNo,
        })),
      };

      await fetchApi<{ id: number }>(
        "/api/v1/outbound/orders",
        {
          method: "POST",
          body: JSON.stringify(payload),
        }
      );

      toast.success("出库单创建成功");
      setShowCreateModal(false);
      resetForm();
      fetchOrders();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "创建失败";
      toast.error(message);
    }
  }

  function resetForm() {
    setFormData({
      orderType: 1,
      sourceType: 2,
      soNo: "",
      customerId: null,
      customerCode: "",
      customerName: "",
      customerPhone: "",
      customerAddress: "",
      supplierId: null,
      supplierCode: "",
      supplierName: "",
      targetWarehouseId: null,
      targetWarehouseCode: "",
      targetWarehouseName: "",
      warehouseId: null,
      warehouseCode: "",
      warehouseName: "",
      priority: 3,
      logisticsCompany: "",
      receiverName: "",
      receiverPhone: "",
      receiverAddress: "",
      remark: "",
      items: [],
    });
    setNewItem({ skuCode: "", productName: "", qty: 1, availableQty: 0 });
    setInventorySummary([]);
  }

  function addItem() {
    if (!newItem.skuCode) {
      toast.error("请选择商品");
      return;
    }
    if (newItem.qty <= 0) {
      toast.error("数量必须大于0");
      return;
    }
    if (newItem.availableQty !== undefined && newItem.qty > newItem.availableQty) {
      toast.error(`库存不足，可用库存: ${newItem.availableQty}`);
      return;
    }
    // 检查是否已添加过该商品
    const existing = formData.items.find((i) => i.skuCode === newItem.skuCode);
    if (existing) {
      const newQty = existing.qty + newItem.qty;
      if (newQty > (existing.availableQty || 0)) {
        toast.error(`累计数量超过可用库存: ${existing.availableQty}`);
        return;
      }
      // 更新已有商品数量
      const updatedItems = formData.items.map((i) =>
        i.skuCode === newItem.skuCode ? { ...i, qty: newQty } : i
      );
      setFormData({ ...formData, items: updatedItems });
    } else {
      setFormData({
        ...formData,
        items: [...formData.items, { ...newItem }],
      });
    }
    setNewItem({ skuCode: "", productName: "", qty: 1, availableQty: 0 });
  }

  function removeItem(index: number) {
    const items = formData.items.filter((_, i) => i !== index);
    setFormData({ ...formData, items });
  }

  const filteredData = orders.filter((order) => {
    const matchSearch =
      !searchText ||
      order.orderNo?.toLowerCase().includes(searchText.toLowerCase()) ||
      order.soNo?.toLowerCase().includes(searchText.toLowerCase()) ||
      order.customerName?.toLowerCase().includes(searchText.toLowerCase());
    const matchStatus =
      !statusFilter || order.status.toString() === statusFilter;
    return matchSearch && matchStatus;
  });

  // 过滤商品列表（只显示有库存的商品）
  const filteredProducts = products.filter((p) => {
    const hasStock = inventorySummary.some((i) => i.productId === p.id && i.availableQty > 0);
    const matchSearch = !productSearch ||
      p.skuCode.toLowerCase().includes(productSearch.toLowerCase()) ||
      p.nameCn.toLowerCase().includes(productSearch.toLowerCase()) ||
      (p.barcode && p.barcode.includes(productSearch));
    return hasStock && matchSearch;
  });

  const columns = [
    { key: "orderNo", title: "出库单号", width: "150px" },
    { key: "soNo", title: "销售单号", width: "130px" },
    {
      key: "relatedParty",
      title: "关联方",
      width: "150px",
      render: (_: unknown, order: OutboundOrder) => {
        // 根据出库类型显示不同关联方
        if (order.orderType === 1 || order.orderType === 5) {
          return order.customerName || "-";
        } else if (order.orderType === 2) {
          return <span className="text-blue-600">→ {order.targetWarehouseName || "-"}</span>;
        } else if (order.orderType === 3) {
          return <span className="text-orange-600">退: {order.supplierName || "-"}</span>;
        } else if (order.orderType === 4) {
          return <span className="text-gray-400">报废</span>;
        }
        return "-";
      },
    },
    { key: "warehouseName", title: "仓库", width: "100px" },
    {
      key: "totalQty",
      title: "数量",
      width: "80px",
      render: (v: number) => v?.toLocaleString() || "0",
    },
    {
      key: "priorityName",
      title: "优先级",
      width: "80px",
      render: (value: string) => (
        <span
          className={`px-2 py-0.5 rounded text-xs ${
            value === "紧急"
              ? "bg-red-100 text-red-700"
              : value === "高"
              ? "bg-orange-100 text-orange-700"
              : "bg-gray-100 text-gray-700"
          }`}
        >
          {value}
        </span>
      ),
    },
    {
      key: "statusName",
      title: "状态",
      width: "90px",
      render: (value: string) => (
        <span
          className={`px-2 py-0.5 rounded text-xs ${
            value === "已发货"
              ? "bg-green-100 text-green-700"
              : value === "待分配"
              ? "bg-yellow-100 text-yellow-700"
              : value === "拣货中" || value === "待打包"
              ? "bg-blue-100 text-blue-700"
              : value === "已取消"
              ? "bg-gray-100 text-gray-700"
              : "bg-gray-100 text-gray-700"
          }`}
        >
          {value}
        </span>
      ),
    },
    { key: "createTime", title: "创建时间", width: "140px" },
    {
      key: "actions",
      title: "操作",
      width: "80px",
      render: (_: unknown, order: OutboundOrder) => (
        <button
          onClick={() => navigate(`/outbound/${order.id}`)}
          className="p-1 hover:bg-gray-100 rounded text-blue-600"
          title="查看详情"
        >
          <Eye size={14} />
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">出库管理</h2>
        <button
          onClick={() => setShowCreateModal(true)}
          className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm"
        >
          <Plus size={16} />
          新建出库单
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-4 mb-4">
          <div className="flex-1 relative">
            <Search
              size={16}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
            />
            <input
              type="text"
              placeholder="搜索出库单号/销售单号/客户名称"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSearch()}
              className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-3 py-1.5 border border-gray-300 rounded text-sm"
          >
            {statusOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <button
            onClick={handleSearch}
            className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
          >
            查询
          </button>
        </div>

        {loading ? (
          <div className="text-center py-12 text-gray-500">加载中...</div>
        ) : filteredData.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">📤</div>
            <div>暂无出库单数据</div>
          </div>
        ) : (
          <DataTable columns={columns} data={filteredData} />
        )}
      </div>

      {/* 创建出库单弹窗 */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg w-[700px] max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between p-4 border-b">
              <h3 className="font-semibold">新建出库单</h3>
              <button
                onClick={() => setShowCreateModal(false)}
                className="p-1 hover:bg-gray-100 rounded"
              >
                <X size={18} />
              </button>
            </div>

            <div className="p-4 space-y-4">
              {/* 基本信息 */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm text-gray-600 mb-1">
                    出库类型 *
                  </label>
                  <select
                    value={formData.orderType}
                    onChange={(e) => {
                      const newType = parseInt(e.target.value);
                      setFormData({
                        ...formData,
                        orderType: newType,
                        // 切换类型时清除关联方数据
                        customerId: null,
                        customerCode: "",
                        customerName: "",
                        customerPhone: "",
                        customerAddress: "",
                        supplierId: null,
                        supplierCode: "",
                        supplierName: "",
                        targetWarehouseId: null,
                        targetWarehouseCode: "",
                        targetWarehouseName: "",
                        soNo: newType === 1 ? formData.soNo : "",
                      });
                    }}
                    className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
                  >
                    {orderTypeOptions.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm text-gray-600 mb-1">
                    来源类型 *
                  </label>
                  <select
                    value={formData.sourceType}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        sourceType: parseInt(e.target.value),
                      })
                    }
                    className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
                  >
                    {sourceTypeOptions.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm text-gray-600 mb-1">
                    仓库 *
                  </label>
                  <select
                    value={formData.warehouseId || ""}
                    onChange={(e) =>
                      handleWarehouseChange(parseInt(e.target.value))
                    }
                    className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
                  >
                    <option value="">请选择仓库</option>
                    {warehouses.map((wh) => (
                      <option key={wh.id} value={wh.id}>
                        {wh.name} ({wh.code})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm text-gray-600 mb-1">
                    优先级
                  </label>
                  <select
                    value={formData.priority}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        priority: parseInt(e.target.value),
                      })
                    }
                    className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
                  >
                    {priorityOptions.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm text-gray-600 mb-1">
                    销售订单号
                  </label>
                  <input
                    type="text"
                    value={formData.soNo}
                    onChange={(e) =>
                      setFormData({ ...formData, soNo: e.target.value })
                    }
                    placeholder="留空自动生成"
                    className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm bg-gray-50"
                  />
                  <p className="text-xs text-gray-400 mt-1">留空将自动生成</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-600 mb-1">
                    物流公司
                  </label>
                  <input
                    type="text"
                    value={formData.logisticsCompany}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        logisticsCompany: e.target.value,
                      })
                    }
                    placeholder="顺丰/京东/中通等"
                    className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
                  />
                </div>
              </div>

              {/* 关联方信息 - 根据出库类型显示 */}
              {(formData.orderType === 1 || formData.orderType === 5) && (
                <div className="border-t pt-4">
                  <h4 className="text-sm font-medium text-gray-700 mb-2">客户信息</h4>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="col-span-2">
                      <label className="block text-sm text-gray-600 mb-1">客户 *</label>
                      <select
                        value={formData.customerId || ""}
                        onChange={(e) => handleCustomerChange(parseInt(e.target.value))}
                        className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
                      >
                        <option value="">请选择客户</option>
                        {customers.map((cust) => (
                          <option key={cust.id} value={cust.id}>
                            {cust.name} ({cust.code})
                          </option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm text-gray-600 mb-1">客户电话</label>
                      <input
                        type="text"
                        value={formData.customerPhone}
                        readOnly
                        className="w-full px-3 py-1.5 border border-gray-200 rounded text-sm bg-gray-50 text-gray-600"
                      />
                    </div>
                    <div>
                      <label className="block text-sm text-gray-600 mb-1">客户地址</label>
                      <input
                        type="text"
                        value={formData.customerAddress}
                        readOnly
                        className="w-full px-3 py-1.5 border border-gray-200 rounded text-sm bg-gray-50 text-gray-600"
                      />
                    </div>
                  </div>
                </div>
              )}

              {formData.orderType === 2 && (
                <div className="border-t pt-4">
                  <h4 className="text-sm font-medium text-gray-700 mb-2">调拨信息</h4>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm text-gray-600 mb-1">目标仓库 *</label>
                      <select
                        value={formData.targetWarehouseId || ""}
                        onChange={(e) => handleTargetWarehouseChange(parseInt(e.target.value))}
                        className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
                      >
                        <option value="">请选择目标仓库</option>
                        {warehouses.filter((w) => w.id !== formData.warehouseId).map((wh) => (
                          <option key={wh.id} value={wh.id}>
                            {wh.name} ({wh.code})
                          </option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm text-gray-600 mb-1">目标仓库编码</label>
                      <input
                        type="text"
                        value={formData.targetWarehouseCode}
                        readOnly
                        className="w-full px-3 py-1.5 border border-gray-200 rounded text-sm bg-gray-50 text-gray-600"
                      />
                    </div>
                  </div>
                </div>
              )}

              {formData.orderType === 3 && (
                <div className="border-t pt-4">
                  <h4 className="text-sm font-medium text-gray-700 mb-2">供应商信息</h4>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm text-gray-600 mb-1">供应商 *</label>
                      <select
                        value={formData.supplierId || ""}
                        onChange={(e) => handleSupplierChange(parseInt(e.target.value))}
                        className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
                      >
                        <option value="">请选择供应商</option>
                        {suppliers.map((sup) => (
                          <option key={sup.id} value={sup.id}>
                            {sup.name} ({sup.code})
                          </option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm text-gray-600 mb-1">供应商编码</label>
                      <input
                        type="text"
                        value={formData.supplierCode}
                        readOnly
                        className="w-full px-3 py-1.5 border border-gray-200 rounded text-sm bg-gray-50 text-gray-600"
                      />
                    </div>
                  </div>
                </div>
              )}

              {formData.orderType === 4 && (
                <div className="border-t pt-4">
                  <h4 className="text-sm font-medium text-gray-700 mb-2">报废信息</h4>
                  <p className="text-sm text-gray-500">报废出库无需关联方，请在备注中说明报废原因</p>
                </div>
              )}

              {/* 商品明细 */}
              <div className="border-t pt-4">
                <h4 className="text-sm font-medium text-gray-700 mb-2">
                  商品明细
                </h4>
                {!formData.warehouseId && (
                  <p className="text-sm text-gray-500 mb-3">请先选择仓库</p>
                )}

                {/* 已添加的商品 */}
                {formData.items.length > 0 && (
                  <div className="mb-3 border rounded divide-y">
                    {formData.items.map((item, index) => (
                      <div
                        key={index}
                        className="flex items-center justify-between px-3 py-2"
                      >
                        <div className="flex-1 text-sm">
                          <span className="font-medium">{item.skuCode}</span>
                          <span className="text-gray-500 ml-2">
                            {item.productName}
                          </span>
                          <span className="text-blue-600 ml-2">x{item.qty}</span>
                          <span className="text-gray-400 ml-2">
                            (可用: {item.availableQty || 0})
                          </span>
                        </div>
                        <button
                          onClick={() => removeItem(index)}
                          className="p-1 hover:bg-gray-100 rounded text-red-500"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                {/* 添加新商品 */}
                {formData.warehouseId && (
                  <div className="relative">
                    <div className="flex gap-2">
                      <div className="flex-1 relative">
                        <input
                          type="text"
                          value={productSearch || newItem.skuCode}
                          onChange={(e) => {
                            setProductSearch(e.target.value);
                            setShowProductDropdown(true);
                          }}
                          onFocus={() => setShowProductDropdown(true)}
                          placeholder="搜索SKU编码/商品名称/条码"
                          className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
                        />
                        {/* 商品下拉列表 */}
                        {showProductDropdown && filteredProducts.length > 0 && (
                          <div className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-md shadow-lg max-h-60 overflow-y-auto">
                            {filteredProducts.slice(0, 20).map((product) => {
                              const inv = inventorySummary.find(
                                (i) => i.productId === product.id
                              );
                              return (
                                <div
                                  key={product.id}
                                  onClick={() => handleProductSelect(product)}
                                  className="px-3 py-2 hover:bg-gray-100 cursor-pointer text-sm"
                                >
                                  <div className="flex justify-between">
                                    <span className="font-medium">{product.skuCode}</span>
                                    <span className="text-green-600">
                                      库存: {inv?.availableQty || 0}
                                    </span>
                                  </div>
                                  <div className="text-gray-500 text-xs">
                                    {product.nameCn}
                                    {product.barcode && ` | ${product.barcode}`}
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        )}
                      </div>
                      <input
                        type="number"
                        value={newItem.qty}
                        onChange={(e) =>
                          setNewItem({
                            ...newItem,
                            qty: parseInt(e.target.value) || 1,
                          })
                        }
                        min={1}
                        max={newItem.availableQty || 9999}
                        className="w-20 px-3 py-1.5 border border-gray-300 rounded text-sm"
                        placeholder="数量"
                      />
                      <button
                        onClick={addItem}
                        disabled={!newItem.skuCode || newItem.qty <= 0}
                        className="px-3 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed"
                      >
                        添加
                      </button>
                    </div>
                    {newItem.skuCode && newItem.availableQty !== undefined && (
                      <p className="text-xs text-gray-500 mt-1">
                        可用库存: {newItem.availableQty}
                        {newItem.qty > newItem.availableQty && (
                          <span className="text-red-500 ml-2 flex items-center gap-1">
                            <AlertTriangle size={12} />
                            数量超过可用库存
                          </span>
                        )}
                      </p>
                    )}
                  </div>
                )}
              </div>

              {/* 备注 */}
              <div>
                <label className="block text-sm text-gray-600 mb-1">备注</label>
                <textarea
                  value={formData.remark}
                  onChange={(e) =>
                    setFormData({ ...formData, remark: e.target.value })
                  }
                  rows={2}
                  className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
                />
              </div>
            </div>

            {/* 操作按钮 */}
            <div className="flex justify-end gap-2 p-4 border-t bg-gray-50">
              <button
                onClick={() => setShowCreateModal(false)}
                className="px-4 py-1.5 border border-gray-300 rounded text-sm hover:bg-gray-100"
              >
                取消
              </button>
              <button
                onClick={handleCreateOrder}
                className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
              >
                创建出库单
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}