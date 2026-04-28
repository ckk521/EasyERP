import { useState, useEffect } from "react";
import { Plus, Search, Edit, Power, PowerOff, Trash2 } from "lucide-react";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Textarea } from "../../components/ui/textarea";
import { toast } from "sonner";

interface Product {
  id: number;
  skuCode: string;
  barcode: string;
  nameCn: string;
  nameEn: string;
  categoryId: number;
  categoryName: string;
  brand: string;
  weight: number;
  length: number;
  width: number;
  height: number;
  volume: number;
  mainImage: string;
  storageCond: number;
  shelfLife: number;
  expiryWarning: number;
  isDangerous: number;
  isFragile: number;
  isHighValue: number;
  needExpiryMgmt: number;
  status: number;
}

interface Category {
  id: number;
  code: string;
  name: string;
  children?: Category[];
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

export default function ProductList() {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState("");
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formData, setFormData] = useState({
    skuCode: "", barcode: "", nameCn: "", nameEn: "", categoryId: "", brand: "",
    weight: "", length: "", width: "", height: "", storageCond: 1, shelfLife: "", expiryWarning: "30",
    isDangerous: 0, isFragile: 0, isHighValue: 0, needExpiryMgmt: 0, description: "",
  });

  useEffect(() => {
    fetchProducts();
    fetchCategories();
  }, []);

  async function fetchProducts() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: Product[] }>("/api/v1/base/products?page=1&limit=100");
      setProducts(data.list || []);
    } catch { setProducts([]); }
    finally { setLoading(false); }
  }

  async function fetchCategories() {
    try {
      const data = await fetchApi<Category[]>("/api/v1/base/categories/tree");
      setCategories(data || []);
    } catch { setCategories([]); }
  }

  const filteredData = products.filter((p) => {
    return !searchText || p.nameCn.includes(searchText) || p.skuCode.includes(searchText);
  });

  const openCreateDialog = () => {
    setEditingId(null);
    setFormData({
      skuCode: "", barcode: "", nameCn: "", nameEn: "", categoryId: "", brand: "",
      weight: "", length: "", width: "", height: "", storageCond: 1, shelfLife: "", expiryWarning: "30",
      isDangerous: 0, isFragile: 0, isHighValue: 0, needExpiryMgmt: 0, description: "",
    });
    setIsFormOpen(true);
  };

  const openEditDialog = (product: Product) => {
    setEditingId(product.id);
    setFormData({
      skuCode: product.skuCode,
      barcode: product.barcode || "",
      nameCn: product.nameCn,
      nameEn: product.nameEn || "",
      categoryId: product.categoryId?.toString() || "",
      brand: product.brand || "",
      weight: product.weight?.toString() || "",
      length: product.length?.toString() || "",
      width: product.width?.toString() || "",
      height: product.height?.toString() || "",
      storageCond: product.storageCond || 1,
      shelfLife: product.shelfLife?.toString() || "",
      expiryWarning: product.expiryWarning?.toString() || "30",
      isDangerous: product.isDangerous || 0,
      isFragile: product.isFragile || 0,
      isHighValue: product.isHighValue || 0,
      needExpiryMgmt: product.needExpiryMgmt || 0,
      description: "",
    });
    setIsFormOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload = {
        skuCode: formData.skuCode,
        barcode: formData.barcode || null,
        nameCn: formData.nameCn,
        nameEn: formData.nameEn || null,
        categoryId: formData.categoryId ? parseInt(formData.categoryId) : null,
        brand: formData.brand || null,
        weight: formData.weight ? parseFloat(formData.weight) : null,
        length: formData.length ? parseFloat(formData.length) : null,
        width: formData.width ? parseFloat(formData.width) : null,
        height: formData.height ? parseFloat(formData.height) : null,
        storageCond: formData.storageCond,
        shelfLife: formData.shelfLife ? parseInt(formData.shelfLife) : null,
        expiryWarning: parseInt(formData.expiryWarning),
        isDangerous: formData.isDangerous,
        isFragile: formData.isFragile,
        isHighValue: formData.isHighValue,
        needExpiryMgmt: formData.needExpiryMgmt,
      };

      if (editingId) {
        await fetchApi(`/api/v1/base/products/${editingId}`, { method: "PUT", body: JSON.stringify(payload) });
        toast.success("商品信息已更新");
      } else {
        await fetchApi("/api/v1/base/products", { method: "POST", body: JSON.stringify(payload) });
        toast.success("商品创建成功");
      }
      setIsFormOpen(false);
      fetchProducts();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const handleToggleStatus = async (product: Product) => {
    try {
      const action = product.status === 1 ? "disable" : "enable";
      await fetchApi(`/api/v1/base/products/${product.id}/${action}`, { method: "PATCH" });
      toast.success(product.status === 1 ? "商品已禁用" : "商品已启用");
      fetchProducts();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定要删除这个商品吗？")) return;
    try {
      await fetchApi(`/api/v1/base/products/${id}`, { method: "DELETE" });
      toast.success("商品已删除");
      fetchProducts();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const columns = [
    { key: "skuCode", title: "SKU编码", width: "120px" },
    { key: "nameCn", title: "商品名称", width: "150px" },
    { key: "categoryName", title: "分类", width: "100px", render: (v: string) => v || "-" },
    { key: "brand", title: "品牌", width: "80px", render: (v: string) => v || "-" },
    { key: "weight", title: "重量(kg)", width: "80px", render: (v: number) => v || "-" },
    { key: "shelfLife", title: "保质期(天)", width: "100px", render: (v: number) => v || "-" },
    { key: "status", title: "状态", width: "80px", render: (v: number) => (
      <span className={`px-2 py-0.5 rounded text-xs ${v === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'}`}>{STATUS_TEXT[v]}</span>
    )},
    { key: "action", title: "操作", width: "120px", render: (_: unknown, row: Product) => (
      <div className="flex gap-2">
        <button title="编辑" onClick={() => openEditDialog(row)} className="text-blue-600 hover:text-blue-800"><Edit size={14} /></button>
        <button title={row.status === 1 ? "禁用" : "启用"} onClick={() => handleToggleStatus(row)} className={row.status === 1 ? "text-orange-600 hover:text-orange-800" : "text-green-600 hover:text-green-800"}>
          {row.status === 1 ? <PowerOff size={14} /> : <Power size={14} />}
        </button>
        <button title="删除" onClick={() => handleDelete(row.id)} className="text-red-600 hover:text-red-800"><Trash2 size={14} /></button>
      </div>
    )},
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">商品管理</h2>
        <button onClick={openCreateDialog} className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm">
          <Plus size={16} />新建商品
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex-1 relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input type="text" placeholder="搜索商品名称或SKU" value={searchText} onChange={(e) => setSearchText(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        {loading ? <div className="text-center py-12 text-gray-500">加载中...</div> :
         filteredData.length === 0 ? <div className="text-center py-12 text-gray-500"><div className="text-4xl mb-2">📦</div><div>暂无商品数据</div></div> :
         <DataTable columns={columns} data={filteredData} />}
      </div>

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>{editingId ? "编辑商品" : "新建商品"}</DialogTitle></DialogHeader>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4 py-4">
              <div className="space-y-2"><Label>SKU编码 *</Label><Input value={formData.skuCode} onChange={(e) => setFormData({ ...formData, skuCode: e.target.value })} required disabled={!!editingId} /></div>
              <div className="space-y-2"><Label>条码</Label><Input value={formData.barcode} onChange={(e) => setFormData({ ...formData, barcode: e.target.value })} /></div>
              <div className="space-y-2"><Label>中文名 *</Label><Input value={formData.nameCn} onChange={(e) => setFormData({ ...formData, nameCn: e.target.value })} required /></div>
              <div className="space-y-2"><Label>英文名</Label><Input value={formData.nameEn} onChange={(e) => setFormData({ ...formData, nameEn: e.target.value })} /></div>
              <div className="space-y-2"><Label>分类</Label>
                <Select value={formData.categoryId} onValueChange={(v) => setFormData({ ...formData, categoryId: v })}>
                  <SelectTrigger><SelectValue placeholder="请选择分类" /></SelectTrigger>
                  <SelectContent>
                    {categories.map((cat) => <SelectItem key={cat.id} value={cat.id.toString()}>{cat.name}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2"><Label>品牌</Label><Input value={formData.brand} onChange={(e) => setFormData({ ...formData, brand: e.target.value })} /></div>
              <div className="space-y-2"><Label>重量(kg)</Label><Input type="number" value={formData.weight} onChange={(e) => setFormData({ ...formData, weight: e.target.value })} /></div>
              <div className="space-y-2"><Label>长度(cm)</Label><Input type="number" value={formData.length} onChange={(e) => setFormData({ ...formData, length: e.target.value })} /></div>
              <div className="space-y-2"><Label>宽度(cm)</Label><Input type="number" value={formData.width} onChange={(e) => setFormData({ ...formData, width: e.target.value })} /></div>
              <div className="space-y-2"><Label>高度(cm)</Label><Input type="number" value={formData.height} onChange={(e) => setFormData({ ...formData, height: e.target.value })} /></div>
              <div className="space-y-2"><Label>保质期(天)</Label><Input type="number" value={formData.shelfLife} onChange={(e) => setFormData({ ...formData, shelfLife: e.target.value })} /></div>
            </div>
            <DialogFooter>
              <button type="button" onClick={() => setIsFormOpen(false)} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">取消</button>
              <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">{editingId ? "保存" : "确认创建"}</button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
