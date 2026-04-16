import { useState, useEffect } from "react";
import { Plus } from "lucide-react";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Textarea } from "../../components/ui/textarea";
import { toast } from "sonner";

interface StocktakeOrder {
  id: number;
  code: string;
  type: string;
  warehouse: string;
  zone: string;
  items: number;
  status: string;
  accuracy: string;
  creator: string;
  createTime: string;
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

export default function StocktakeList() {
  const [orders, setOrders] = useState<StocktakeOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formData, setFormData] = useState({
    type: "",
    warehouse: "",
    zone: "",
    planDate: "",
    remark: "",
  });

  useEffect(() => {
    fetchOrders();
  }, []);

  async function fetchOrders() {
    try {
      setLoading(true);
      const data = await fetchApi<{ list: StocktakeOrder[] }>("/api/v1/inventory/stocktakes?page=1&limit=100");
      setOrders(data.list || []);
    } catch {
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }

  const columns = [
    { key: "code", title: "盘点单号", width: "140px" },
    {
      key: "type",
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
    { key: "warehouse", title: "仓库", width: "100px" },
    { key: "zone", title: "盘点范围", width: "120px" },
    { key: "items", title: "盘点SKU数", width: "100px" },
    {
      key: "status",
      title: "状态",
      width: "90px",
      render: (value: string) => (
        <span className={`px-2 py-0.5 rounded text-xs ${
          value === '盘点中' ? 'bg-blue-100 text-blue-700' :
          'bg-green-100 text-green-700'
        }`}>{value}</span>
      )
    },
    { key: "accuracy", title: "准确率", width: "80px" },
    { key: "creator", title: "创建人", width: "80px" },
    { key: "createTime", title: "创建时间", width: "140px" },
  ];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    toast.info("功能开发中");
    setIsFormOpen(false);
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">盘点管理</h2>
        <button
          onClick={() => setIsFormOpen(true)}
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
            <div className="text-sm text-gray-400 mt-1">API 暂未实现，后端对接后可正常使用</div>
          </div>
        ) : (
          <DataTable columns={columns} data={orders} />
        )}
      </div>

      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>新建盘点单</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit}>
            <div className="grid gap-4 py-4">
              <div className="space-y-2">
                <Label>盘点类型</Label>
                <Select value={formData.type} onValueChange={(v) => setFormData({ ...formData, type: v })}>
                  <SelectTrigger><SelectValue placeholder="请选择类型" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="全盘">全盘</SelectItem>
                    <SelectItem value="抽盘">抽盘</SelectItem>
                    <SelectItem value="循环盘">循环盘</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>仓库</Label>
                <Select value={formData.warehouse} onValueChange={(v) => setFormData({ ...formData, warehouse: v })}>
                  <SelectTrigger><SelectValue placeholder="请选择仓库" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="深圳自营仓">深圳自营仓</SelectItem>
                    <SelectItem value="上海自营仓">上海自营仓</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>盘点范围</Label>
                <Input value={formData.zone} onChange={(e) => setFormData({ ...formData, zone: e.target.value })} placeholder="如：A区" />
              </div>
              <div className="space-y-2">
                <Label>计划日期</Label>
                <Input type="date" value={formData.planDate} onChange={(e) => setFormData({ ...formData, planDate: e.target.value })} />
              </div>
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