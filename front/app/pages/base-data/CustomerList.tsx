import { useState, useEffect } from "react";
import { Plus, Search } from "lucide-react";
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
  type: string;
  phone: string;
  email: string;
  orders: number;
  level: string;
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

export default function CustomerList() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formData, setFormData] = useState({
    code: "", name: "", type: "个人", phone: "", email: "", address: "", remark: "",
  });

  useEffect(() => {
    fetchCustomers();
  }, []);

  async function fetchCustomers() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: Customer[] }>("/api/v1/base/customers?page=1&limit=100");
      setCustomers(data.list || []);
    } catch {
      setCustomers([]);
    } finally {
      setLoading(false);
    }
  }

  const columns = [
    { key: "code", title: "客户编码" },
    { key: "name", title: "客户名称" },
    {
      key: "type", title: "类型",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${value === '企业' ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-700'}`}>{value}</span>
      )
    },
    { key: "phone", title: "联系电话" },
    { key: "email", title: "邮箱" },
    { key: "orders", title: "订单数" },
    {
      key: "level", title: "等级",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${value === 'VIP' ? 'bg-yellow-100 text-yellow-700' : 'bg-gray-100 text-gray-700'}`}>{value}</span>
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
        <h2 className="text-lg font-semibold">客户管理</h2>
        <button onClick={() => setIsFormOpen(true)} className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm">
          <Plus size={16} />新建客户
        </button>
      </div>

      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="relative mb-4">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input type="text" placeholder="搜索客户名称或编码"
            className="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
        {loading ? (
          <div className="text-center py-12 text-gray-500">加载中...</div>
        ) : customers.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="text-4xl mb-2">👥</div>
            <div>暂无客户数据</div>
            <div className="text-sm text-gray-400 mt-1">API 暂未实现，后端对接后可正常使用</div>
          </div>
        ) : (
          <DataTable columns={columns} data={customers} />
        )}
      </div>

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader><DialogTitle>新建客户</DialogTitle></DialogHeader>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4 py-4">
              <div className="space-y-2"><Label>客户编码 *</Label><Input value={formData.code} onChange={(e) => setFormData({ ...formData, code: e.target.value })} placeholder="例：CUS-001" /></div>
              <div className="space-y-2"><Label>客户名称 *</Label><Input value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} placeholder="请输入客户名称" /></div>
              <div className="space-y-2">
                <Label>客户类型 *</Label>
                <Select value={formData.type} onValueChange={(v) => setFormData({ ...formData, type: v })}>
                  <SelectTrigger><SelectValue placeholder="请选择类型" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="个人">个人</SelectItem>
                    <SelectItem value="企业">企业</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2"><Label>联系电话 *</Label><Input type="tel" value={formData.phone} onChange={(e) => setFormData({ ...formData, phone: e.target.value })} placeholder="请输入联系电话" /></div>
              <div className="space-y-2 col-span-2"><Label>邮箱 *</Label><Input type="email" value={formData.email} onChange={(e) => setFormData({ ...formData, email: e.target.value })} placeholder="请输入邮箱" /></div>
              <div className="space-y-2 col-span-2"><Label>地址</Label><Textarea value={formData.address} onChange={(e) => setFormData({ ...formData, address: e.target.value })} placeholder="请输入地址" rows={2} /></div>
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