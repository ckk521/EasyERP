import { useState, useEffect } from "react";
import { Plus, Search, Image as ImageIcon } from "lucide-react";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { toast } from "sonner";

interface Product {
  id: number;
  sku: string;
  barcode: string;
  name: string;
  category: string;
  brand: string;
  weight: string;
  size: string;
  stock: number;
  safeStock: number;
  price: string;
  status: string;
  expiry?: boolean;
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

export default function ProductList() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("全部");
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formData, setFormData] = useState({
    sku: "", barcode: "", name: "", category: "", brand: "", weight: "", size: "", safeStock: "", price: "",
  });

  useEffect(() => {
    fetchProducts();
  }, []);

  async function fetchProducts() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: Product[] }>("/api/v1/base/products?page=1&limit=100");
      setProducts(data.list || []);
    } catch {
      setProducts([]);
    } finally {
      setLoading(false);
    }
  }

  const filteredData = products.filter((p) => {
    const matchSearch = !searchText || p.name.includes(searchText) || p.sku.includes(searchText) || p.barcode.includes(searchText);
    const matchCategory = categoryFilter === "全部" || p.category.startsWith(categoryFilter);
    return matchSearch && matchCategory;
  });

  const stats = {
    total: products.length,
    totalStock: products.reduce((sum, p) => sum + p.stock, 0),
    alertCount: products.filter((p) => p.status === '预警').length,
    expiryCount: products.filter((p) => p.expiry).length,
  };

  const columns = [
    { key: "image", title: "", width: "40px", render: () => <div className="w-8 h-8 bg-gray-100 rounded flex items-center justify-center"><ImageIcon size={16} className="text-gray-400" /></div> },
    { key: "sku", title: "SKU编码", width: "140px" },
    { key: "barcode", title: "条码", width: "120px" },
    { key: "name", title: "商品名称", width: "180px" },
    { key: "category", title: "分类", width: "150px" },
    { key: "brand", title: "品牌", width: "80px" },
    { key: "stock", title: "库存", width: "80px", render: (v: number, r: Product) => <span className={r.status === '预警' ? 'text-orange-600 font-semibold' : ''}>{v}</span> },
    { key: "price", title: "价格", width: "80px" },
    {
      key: "status", title: "状态", width: "80px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '正常' ? 'bg-green-100 text-green-700' : value === '预警' ? 'bg-orange-100 text-orange-700' : 'bg-gray-100 text-gray-700'
        }`}>{value}</span>
      )
    },
  ];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    toast.info("功能开发中");
    setIsFormOpen(false);
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">商品管理</h2>
        <div className="flex gap-2">
          <button className="px-3 py-1.5 bg-gray-100 text-gray-700 rounded hover:bg-gray-200 text-sm">导入商品</button>
          <button onClick={() => setIsFormOpen(true)} className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm">
            <Plus size={16} />新建商品
          </button>
        </div>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex gap-4 items-center">
          <div className="flex-1 relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input type="text" placeholder="搜索商品名称、SKU或条码" value={searchText} onChange={(e) => setSearchText(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div className="flex gap-2">
            {["全部", "电子数码", "服装", "食品保健", "美妆个护"].map((cat) => (
              <button key={cat} onClick={() => setCategoryFilter(cat)}
                className={`px-3 py-1.5 rounded text-sm whitespace-nowrap ${categoryFilter === cat ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}`}>
                {cat}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-5 gap-4">
        <div className="bg-white rounded-lg p-3 border border-gray-200"><div className="text-xs text-gray-500 mb-1">总商品数</div><div className="text-2xl font-bold">{stats.total}</div></div>
        <div className="bg-white rounded-lg p-3 border border-gray-200"><div className="text-xs text-gray-500 mb-1">库存总量</div><div className="text-2xl font-bold text-blue-600">{stats.totalStock.toLocaleString()}</div></div>
        <div className="bg-white rounded-lg p-3 border border-gray-200"><div className="text-xs text-gray-500 mb-1">库存预警</div><div className="text-2xl font-bold text-orange-600">{stats.alertCount}</div></div>
        <div className="bg-white rounded-lg p-3 border border-gray-200"><div className="text-xs text-gray-500 mb-1">效期管理</div><div className="text-2xl font-bold text-purple-600">{stats.expiryCount}</div></div>
        <div className="bg-white rounded-lg p-3 border border-gray-200"><div className="text-xs text-gray-500 mb-1">库存金额</div><div className="text-xl font-bold">-</div></div>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        {loading ? (
          <div className="text-center py-12 text-gray-500">加载中...</div>
        ) : filteredData.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">📦</div>
            <div>暂无商品数据</div>
            <div className="text-sm text-gray-400 mt-1">API 暂未实现，后端对接后可正常使用</div>
          </div>
        ) : (
          <DataTable columns={columns} data={filteredData} />
        )}
      </div>

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>新建商品</DialogTitle></DialogHeader>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4 py-4">
              <div className="space-y-2"><Label>SKU编码 *</Label><Input value={formData.sku} onChange={(e) => setFormData({ ...formData, sku: e.target.value })} placeholder="例：TS-W-S-M-White" /></div>
              <div className="space-y-2"><Label>条码 *</Label><Input value={formData.barcode} onChange={(e) => setFormData({ ...formData, barcode: e.target.value })} placeholder="例：6912345678901" /></div>
              <div className="space-y-2 col-span-2"><Label>商品名称 *</Label><Input value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} placeholder="例：女士T恤-S码-白色" /></div>
              <div className="space-y-2">
                <Label>商品分类 *</Label>
                <Select value={formData.category} onValueChange={(v) => setFormData({ ...formData, category: v })}>
                  <SelectTrigger><SelectValue placeholder="请选择分类" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="服装-女装-T恤">服装-女装-T恤</SelectItem>
                    <SelectItem value="电子数码-手机">电子数码-手机</SelectItem>
                    <SelectItem value="食品保健-乳制品">食品保健-乳制品</SelectItem>
                    <SelectItem value="美妆个护-护肤品">美妆个护-护肤品</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2"><Label>品牌</Label><Input value={formData.brand} onChange={(e) => setFormData({ ...formData, brand: e.target.value })} placeholder="例：UNIQLO" /></div>
              <div className="space-y-2"><Label>重量 (kg)</Label><Input value={formData.weight} onChange={(e) => setFormData({ ...formData, weight: e.target.value })} placeholder="例：0.2" /></div>
              <div className="space-y-2"><Label>尺寸 (cm)</Label><Input value={formData.size} onChange={(e) => setFormData({ ...formData, size: e.target.value })} placeholder="例：30x25x2" /></div>
              <div className="space-y-2"><Label>安全库存 *</Label><Input type="number" value={formData.safeStock} onChange={(e) => setFormData({ ...formData, safeStock: e.target.value })} placeholder="例：500" /></div>
              <div className="space-y-2"><Label>价格 (¥) *</Label><Input type="number" value={formData.price} onChange={(e) => setFormData({ ...formData, price: e.target.value })} placeholder="例：89" /></div>
            </div>
            <DialogFooter>
              <button type="button" onClick={() => setIsFormOpen(false)} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">取消</button>
              <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">确认创建</button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}