import { useState, useEffect } from "react";
import { Plus, Search, Edit, Trash2, Link2 } from "lucide-react";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { toast } from "sonner";

interface SupplierProduct {
  id: number;
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  productId: number;
  skuCode: string;
  productName: string;
  supplierSkuCode: string;
  purchasePrice: number;
  minOrderQty: number;
  leadTime: number;
  status: number;
}

interface Supplier {
  id: number;
  code: string;
  name: string;
}

interface Product {
  id: number;
  skuCode: string;
  nameCn: string;
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
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const response = await fetch(url, { ...options, headers });
  const data: ApiResponse<T> = await response.json();
  if (!data.success) throw new Error(data.message || "API Error");
  return data.data as T;
}

const STATUS_TEXT: Record<number, string> = { 0: "禁用", 1: "启用" };

export default function SupplierProductList() {
  const [supplierProducts, setSupplierProducts] = useState<SupplierProduct[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchSupplier, setSearchSupplier] = useState<number | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formData, setFormData] = useState({
    supplierId: "",
    productId: "",
    supplierSkuCode: "",
    purchasePrice: "",
    minOrderQty: "",
    leadTime: "",
  });

  useEffect(() => {
    fetchData();
  }, []);

  async function fetchData() {
    try {
      setLoading(true);
      const [spData, supplierData, productData] = await Promise.all([
        fetchApi<{ list: SupplierProduct[] }>("/api/v1/base/supplier-products?page=1&limit=100"),
        fetchApi<{ list: Supplier[] }>("/api/v1/base/suppliers?page=1&limit=100"),
        fetchApi<{ list: Product[] }>("/api/v1/base/products?page=1&limit=100"),
      ]);
      setSupplierProducts(spData.list || []);
      setSuppliers(supplierData.list || []);
      setProducts(productData.list || []);
    } catch {
      setSupplierProducts([]);
    } finally {
      setLoading(false);
    }
  }

  const filteredData = supplierProducts.filter((sp) => {
    return searchSupplier === null || sp.supplierId === searchSupplier;
  });

  const openCreateDialog = () => {
    setEditingId(null);
    setFormData({
      supplierId: "",
      productId: "",
      supplierSkuCode: "",
      purchasePrice: "",
      minOrderQty: "",
      leadTime: "",
    });
    setIsFormOpen(true);
  };

  const openEditDialog = (sp: SupplierProduct) => {
    setEditingId(sp.id);
    setFormData({
      supplierId: sp.supplierId.toString(),
      productId: sp.productId.toString(),
      supplierSkuCode: sp.supplierSkuCode || "",
      purchasePrice: sp.purchasePrice?.toString() || "",
      minOrderQty: sp.minOrderQty?.toString() || "",
      leadTime: sp.leadTime?.toString() || "",
    });
    setIsFormOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload = {
        supplierId: parseInt(formData.supplierId),
        productId: parseInt(formData.productId),
        supplierSkuCode: formData.supplierSkuCode || null,
        purchasePrice: formData.purchasePrice ? parseFloat(formData.purchasePrice) : null,
        minOrderQty: formData.minOrderQty ? parseFloat(formData.minOrderQty) : null,
        leadTime: formData.leadTime ? parseFloat(formData.leadTime) : null,
      };

      if (editingId) {
        await fetchApi(`/api/v1/base/supplier-products/${editingId}`, { method: "PUT", body: JSON.stringify(payload) });
        toast.success("更新成功");
      } else {
        await fetchApi("/api/v1/base/supplier-products", { method: "POST", body: JSON.stringify(payload) });
        toast.success("关联成功");
      }
      setIsFormOpen(false);
      fetchData();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定要删除这个关联关系吗？")) return;
    try {
      await fetchApi(`/api/v1/base/supplier-products/${id}`, { method: "DELETE" });
      toast.success("删除成功");
      fetchData();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const columns = [
    { key: "supplierCode", title: "供应商编码", width: "100px" },
    { key: "supplierName", title: "供应商名称", width: "150px" },
    { key: "skuCode", title: "SKU编码", width: "100px" },
    { key: "productName", title: "商品名称", width: "150px" },
    { key: "supplierSkuCode", title: "供应商商品编码", width: "120px", render: (v: string) => v || "-" },
    { key: "purchasePrice", title: "采购价", width: "100px", render: (v: number) => v ? `¥${v}` : "-" },
    { key: "minOrderQty", title: "最小起订量", width: "100px", render: (v: number) => v || "-" },
    { key: "leadTime", title: "交货周期(天)", width: "100px", render: (v: number) => v || "-" },
    { key: "status", title: "状态", width: "80px", render: (v: number) => (
      <span className={`px-2 py-0.5 rounded text-xs ${v === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'}`}>{STATUS_TEXT[v]}</span>
    )},
    { key: "action", title: "操作", width: "100px", render: (_: unknown, row: SupplierProduct) => (
      <div className="flex gap-2">
        <button title="编辑" onClick={() => openEditDialog(row)} className="text-blue-600 hover:text-blue-800"><Edit size={14} /></button>
        <button title="删除" onClick={() => handleDelete(row.id)} className="text-red-600 hover:text-red-800"><Trash2 size={14} /></button>
      </div>
    )},
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">供应商商品管理</h2>
        <button onClick={openCreateDialog} className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm">
          <Link2 size={16} />新建关联
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-4 items-center">
          <Select value={searchSupplier?.toString() || "all"} onValueChange={(v) => setSearchSupplier(v === "all" ? null : parseInt(v))}>
            <SelectTrigger className="w-48"><SelectValue placeholder="全部供应商" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部供应商</SelectItem>
              {suppliers.map((s) => (
                <SelectItem key={s.id} value={s.id.toString()}>{s.code} - {s.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        {loading ? (
          <div className="text-center py-12 text-gray-500">加载中...</div>
        ) : filteredData.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">🔗</div>
            <div>暂无供应商商品关联数据</div>
          </div>
        ) : (
          <DataTable columns={columns} data={filteredData} />
        )}
      </div>

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader><DialogTitle>{editingId ? "编辑关联" : "新建关联"}</DialogTitle></DialogHeader>
          <form onSubmit={handleSubmit}>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label>供应商 *</Label>
                <Select value={formData.supplierId} onValueChange={(v) => setFormData({ ...formData, supplierId: v })} required disabled={!!editingId}>
                  <SelectTrigger><SelectValue placeholder="请选择供应商" /></SelectTrigger>
                  <SelectContent>
                    {suppliers.map((s) => (
                      <SelectItem key={s.id} value={s.id.toString()}>{s.code} - {s.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>商品 *</Label>
                <Select value={formData.productId} onValueChange={(v) => setFormData({ ...formData, productId: v })} required disabled={!!editingId}>
                  <SelectTrigger><SelectValue placeholder="请选择商品" /></SelectTrigger>
                  <SelectContent>
                    {products.map((p) => (
                      <SelectItem key={p.id} value={p.id.toString()}>{p.skuCode} - {p.nameCn}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>供应商商品编码</Label>
                <Input value={formData.supplierSkuCode} onChange={(e) => setFormData({ ...formData, supplierSkuCode: e.target.value })} placeholder="供应商对该商品的编码" />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>采购价</Label>
                  <Input type="number" step="0.01" value={formData.purchasePrice} onChange={(e) => setFormData({ ...formData, purchasePrice: e.target.value })} placeholder="0.00" />
                </div>
                <div className="space-y-2">
                  <Label>最小起订量</Label>
                  <Input type="number" value={formData.minOrderQty} onChange={(e) => setFormData({ ...formData, minOrderQty: e.target.value })} placeholder="1" />
                </div>
              </div>
              <div className="space-y-2">
                <Label>交货周期(天)</Label>
                <Input type="number" value={formData.leadTime} onChange={(e) => setFormData({ ...formData, leadTime: e.target.value })} placeholder="下单后多少天交货" />
              </div>
            </div>
            <DialogFooter>
              <button type="button" onClick={() => setIsFormOpen(false)} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">取消</button>
              <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">{editingId ? "保存" : "确认关联"}</button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
