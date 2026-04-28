import { useState, useEffect } from "react";
import { Plus, Search, Edit, Power, PowerOff, Trash2 } from "lucide-react";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Textarea } from "../../components/ui/textarea";
import { toast } from "sonner";

interface Customer {
  id: number;
  code: string;
  name: string;
  type: number;
  contact: string;
  phone: string;
  email: string;
  address: string;
  creditLimit: number;
  level: number;
  remark: string;
  status: number;
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

const CUSTOMER_TYPES: Record<number, string> = { 1: "个人", 2: "企业" };
const CUSTOMER_LEVELS: Record<number, string> = { 1: "普通", 2: "银卡", 3: "金卡", 4: "VIP" };
const STATUS_TEXT: Record<number, string> = { 0: "禁用", 1: "启用" };

export default function CustomerList() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState("");
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formData, setFormData] = useState({
    code: "", name: "", type: 1, contact: "", phone: "", email: "", address: "", creditLimit: "", level: 1, remark: "",
  });

  useEffect(() => {
    fetchCustomers();
  }, []);

  async function fetchCustomers() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: Customer[] }>("/api/v1/base/customers?page=1&limit=100");
      setCustomers(data.list || []);
    } catch { setCustomers([]); }
    finally { setLoading(false); }
  }

  const filteredData = customers.filter((c) => {
    return !searchText || c.name.includes(searchText) || c.code.includes(searchText);
  });

  const openCreateDialog = () => {
    setEditingId(null);
    setFormData({ code: "", name: "", type: 1, contact: "", phone: "", email: "", address: "", creditLimit: "", level: 1, remark: "" });
    setIsFormOpen(true);
  };

  const openEditDialog = (customer: Customer) => {
    setEditingId(customer.id);
    setFormData({
      code: customer.code,
      name: customer.name,
      type: customer.type || 1,
      contact: customer.contact || "",
      phone: customer.phone || "",
      email: customer.email || "",
      address: customer.address || "",
      creditLimit: customer.creditLimit?.toString() || "",
      level: customer.level || 1,
      remark: customer.remark || "",
    });
    setIsFormOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload = {
        code: formData.code,
        name: formData.name,
        type: formData.type,
        contact: formData.contact || null,
        phone: formData.phone || null,
        email: formData.email || null,
        address: formData.address || null,
        creditLimit: formData.creditLimit ? parseFloat(formData.creditLimit) : null,
        level: formData.level,
        remark: formData.remark || null,
      };

      if (editingId) {
        await fetchApi(`/api/v1/base/customers/${editingId}`, { method: "PUT", body: JSON.stringify(payload) });
        toast.success("客户信息已更新");
      } else {
        await fetchApi("/api/v1/base/customers", { method: "POST", body: JSON.stringify(payload) });
        toast.success("客户创建成功");
      }
      setIsFormOpen(false);
      fetchCustomers();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const handleToggleStatus = async (customer: Customer) => {
    try {
      const action = customer.status === 1 ? "disable" : "enable";
      await fetchApi(`/api/v1/base/customers/${customer.id}/${action}`, { method: "PATCH" });
      toast.success(customer.status === 1 ? "客户已禁用" : "客户已启用");
      fetchCustomers();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定要删除这个客户吗？")) return;
    try {
      await fetchApi(`/api/v1/base/customers/${id}`, { method: "DELETE" });
      toast.success("客户已删除");
      fetchCustomers();
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message);
    }
  };

  const columns = [
    { key: "code", title: "客户编码", width: "120px" },
    { key: "name", title: "客户名称", width: "150px" },
    { key: "type", title: "类型", width: "80px", render: (v: number) => (
      <span className={`px-2 py-0.5 rounded text-xs ${v === 2 ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-700'}`}>{CUSTOMER_TYPES[v] || v}</span>
    )},
    { key: "contact", title: "联系人", width: "80px", render: (v: string) => v || "-" },
    { key: "phone", title: "联系电话", width: "120px", render: (v: string) => v || "-" },
    { key: "level", title: "等级", width: "80px", render: (v: number) => (
      <span className={`px-2 py-0.5 rounded text-xs ${v === 4 ? 'bg-yellow-100 text-yellow-700' : v === 3 ? 'bg-orange-100 text-orange-700' : v === 2 ? 'bg-gray-200 text-gray-700' : 'bg-gray-100 text-gray-600'}`}>{CUSTOMER_LEVELS[v] || v}</span>
    )},
    { key: "status", title: "状态", width: "80px", render: (v: number) => (
      <span className={`px-2 py-0.5 rounded text-xs ${v === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'}`}>{STATUS_TEXT[v]}</span>
    )},
    { key: "action", title: "操作", width: "120px", render: (_: unknown, row: Customer) => (
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
        <h2 className="text-lg font-semibold">客户管理</h2>
        <button onClick={openCreateDialog} className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm">
          <Plus size={16} />新建客户
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="flex-1 relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input type="text" placeholder="搜索客户名称或编码" value={searchText} onChange={(e) => setSearchText(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        {loading ? <div className="text-center py-12 text-gray-500">加载中...</div> :
         filteredData.length === 0 ? <div className="text-center py-12 text-gray-500"><div className="text-4xl mb-2">👥</div><div>暂无客户数据</div></div> :
         <DataTable columns={columns} data={filteredData} />}
      </div>

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>{editingId ? "编辑客户" : "新建客户"}</DialogTitle></DialogHeader>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4 py-4">
              <div className="space-y-2"><Label>客户编码 *</Label><Input value={formData.code} onChange={(e) => setFormData({ ...formData, code: e.target.value })} required disabled={!!editingId} /></div>
              <div className="space-y-2"><Label>客户名称 *</Label><Input value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} required /></div>
              <div className="space-y-2">
                <Label>客户类型</Label>
                <Select value={formData.type.toString()} onValueChange={(v) => setFormData({ ...formData, type: parseInt(v) })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {Object.entries(CUSTOMER_TYPES).map(([k, v]) => <SelectItem key={k} value={k}>{v}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>客户等级</Label>
                <Select value={formData.level.toString()} onValueChange={(v) => setFormData({ ...formData, level: parseInt(v) })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {Object.entries(CUSTOMER_LEVELS).map(([k, v]) => <SelectItem key={k} value={k}>{v}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2"><Label>联系人</Label><Input value={formData.contact} onChange={(e) => setFormData({ ...formData, contact: e.target.value })} /></div>
              <div className="space-y-2"><Label>联系电话</Label><Input type="tel" value={formData.phone} onChange={(e) => setFormData({ ...formData, phone: e.target.value })} /></div>
              <div className="space-y-2 col-span-2"><Label>邮箱</Label><Input type="email" value={formData.email} onChange={(e) => setFormData({ ...formData, email: e.target.value })} /></div>
              <div className="space-y-2 col-span-2"><Label>地址</Label><Textarea value={formData.address} onChange={(e) => setFormData({ ...formData, address: e.target.value })} rows={2} /></div>
              <div className="space-y-2"><Label>信用额度</Label><Input type="number" value={formData.creditLimit} onChange={(e) => setFormData({ ...formData, creditLimit: e.target.value })} /></div>
              <div className="space-y-2 col-span-2"><Label>备注</Label><Textarea value={formData.remark} onChange={(e) => setFormData({ ...formData, remark: e.target.value })} rows={2} /></div>
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
