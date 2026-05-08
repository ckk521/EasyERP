import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { Search, Eye, XCircle, AlertTriangle } from "lucide-react";
import DataTable from "../../components/DataTable";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Button } from "../../components/ui/button";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Textarea } from "../../components/ui/textarea";
import { toast } from "sonner";

// 异常类型映射
const exceptionTypeMap: Record<number, { text: string; color: string }> = {
  1: { text: "破损", color: "text-red-500" },
  2: { text: "短缺", color: "text-orange-500" },
  3: { text: "质量不合格", color: "text-purple-500" },
  4: { text: "错货", color: "text-cyan-500" },
  5: { text: "其他", color: "text-gray-500" },
};

// 状态映射
const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: "待处理", color: "bg-orange-100 text-orange-700" },
  1: { text: "处理中", color: "bg-blue-100 text-blue-700" },
  2: { text: "已完成", color: "bg-green-100 text-green-700" },
  3: { text: "已取消", color: "bg-gray-100 text-gray-700" },
};

// 处理方式映射
const handleTypeMap: Record<number, string> = {
  1: "退货",
  2: "换货",
  3: "报废",
  4: "降价销售",
};

// 来源类型映射
const sourceTypeMap: Record<number, string> = {
  1: "收货异常",
  2: "验收异常",
};

interface ExceptionOrder {
  id: number;
  orderNo: string;
  inboundOrderNo: string;
  supplierName: string;
  exceptionType: number;
  totalQty: number;
  status: number;
  handleType: number | null;
  sourceType: number;
  createTime: string;
  handleTime: string | null;
  handleUserName: string | null;
  items?: ExceptionItem[];
  replacementInboundOrderId: number | null;
  replacementInboundOrderNo: string | null;
}

interface ExceptionItem {
  id: number;
  skuCode: string;
  productName: string;
  exceptionQty: number;
  exceptionType: number;
  exceptionReason: string;
  locationCode: string | null;
  status: number;
  handleType: number | null;
  handleResult: string | null;
}

export default function ExceptionList() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<ExceptionOrder[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [exceptionTypeFilter, setExceptionTypeFilter] = useState<string>("");
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentOrder, setCurrentOrder] = useState<ExceptionOrder | null>(null);
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [cancelOrderId, setCancelOrderId] = useState<number | null>(null);

  useEffect(() => {
    loadData();
  }, [page, pageSize, statusFilter, exceptionTypeFilter]);

  const loadData = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem("token");
      const params = new URLSearchParams({
        page: String(page),
        limit: String(pageSize),
      });
      if (keyword) params.append("keyword", keyword);
      if (statusFilter && statusFilter !== "all") params.append("status", statusFilter);
      if (exceptionTypeFilter && exceptionTypeFilter !== "all") params.append("exceptionType", exceptionTypeFilter);

      const res = await fetch(`/api/v1/exception-orders?${params}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const result = await res.json();
      if (result.success) {
        setData(result.data.list || []);
        setTotal(result.data.total || 0);
      }
    } catch (error) {
      toast.error("加载数据失败");
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setPage(1);
    loadData();
  };

  const handleViewDetail = async (id: number) => {
    setLoading(true);
    try {
      const token = localStorage.getItem("token");
      const res = await fetch(`/api/v1/exception-orders/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const result = await res.json();
      if (result.success) {
        setCurrentOrder(result.data);
        setDetailVisible(true);
      }
    } catch (error) {
      toast.error("加载详情失败");
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async () => {
    if (!cancelOrderId || !cancelReason.trim()) {
      toast.error("请输入取消原因");
      return;
    }

    try {
      const token = localStorage.getItem("token");
      const res = await fetch(`/api/v1/exception-orders/${cancelOrderId}/cancel`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ cancelReason }),
      });
      const result = await res.json();
      if (result.success) {
        toast.success("取消成功");
        setCancelDialogOpen(false);
        setCancelReason("");
        loadData();
      } else {
        toast.error(result.message || "取消失败");
      }
    } catch (error) {
      toast.error("取消失败");
    }
  };

  const columns = [
    {
      key: "orderNo",
      title: "异常处理单号",
      width: 180,
      render: (_: unknown, row: ExceptionOrder) => (
        <button
          className="text-blue-600 hover:underline"
          onClick={() => handleViewDetail(row.id)}
        >
          {row.orderNo}
        </button>
      ),
    },
    { key: "inboundOrderNo", title: "入库单号", width: 160 },
    { key: "supplierName", title: "供应商", width: 150 },
    {
      key: "exceptionType",
      title: "异常类型",
      width: 100,
      render: (v: number) => {
        const item = exceptionTypeMap[v];
        return <span className={item?.color}>{item?.text || v}</span>;
      },
    },
    {
      key: "sourceType",
      title: "来源",
      width: 100,
      render: (v: number) => sourceTypeMap[v] || v,
    },
    { key: "totalQty", title: "异常数量", width: 100 },
    {
      key: "status",
      title: "状态",
      width: 100,
      render: (v: number) => {
        const item = statusMap[v];
        return (
          <span className={`px-2 py-1 rounded text-xs ${item?.color}`}>
            {item?.text || v}
          </span>
        );
      },
    },
    {
      key: "handleType",
      title: "处理方式",
      width: 100,
      render: (v: number | null) => v ? handleTypeMap[v] : "-",
    },
    { key: "createTime", title: "创建时间", width: 160 },
    {
      key: "actions",
      title: "操作",
      width: 200,
      render: (_: unknown, row: ExceptionOrder) => (
        <div className="flex gap-2">
          <Button size="sm" variant="ghost" className="text-blue-600" onClick={() => handleViewDetail(row.id)}>
            详情
          </Button>
          {row.status === 0 && (
            <Button
              size="sm"
              variant="ghost"
              className="text-red-500"
              onClick={() => {
                setCancelOrderId(row.id);
                setCancelDialogOpen(true);
              }}
            >
              取消
            </Button>
          )}
          {row.status === 2 && (row.handleType === 1 || row.handleType === 2 || row.handleType === 3) && !row.replacementInboundOrderId && (
            <Button
              size="sm"
              variant="outline"
              onClick={async () => {
                const handleTypeName = row.handleType === 1 ? "退货" : row.handleType === 2 ? "换货" : "报废";
                if (confirm(`该异常单已${handleTypeName}处理，确定需要供应商补货吗？`)) {
                  try {
                    const token = localStorage.getItem("token");
                    const res = await fetch(`/api/v1/exception-orders/${row.id}/create-replacement`, {
                      method: "POST",
                      headers: { Authorization: `Bearer ${token}` },
                    });
                    const result = await res.json();
                    if (result.success) {
                      toast.success("补货入库单创建成功");
                      loadData();
                    } else {
                      toast.error(result.message || "创建失败");
                    }
                  } catch (error) {
                    toast.error("创建失败");
                  }
                }
              }}
            >
              补货
            </Button>
          )}
          {row.replacementInboundOrderId && (
            <Button
              size="sm"
              variant="ghost"
              className="text-blue-500"
              onClick={() => navigate(`/inbound/${row.replacementInboundOrderId}`)}
            >
              查看补货
            </Button>
          )}
        </div>
      ),
    },
  ];

  const itemColumns = [
    { key: "skuCode", title: "SKU编码", width: 120 },
    { key: "productName", title: "商品名称", width: 200 },
    { key: "exceptionQty", title: "异常数量", width: 80 },
    {
      key: "exceptionType",
      title: "异常类型",
      width: 100,
      render: (v: number) => exceptionTypeMap[v]?.text || v,
    },
    { key: "exceptionReason", title: "异常原因", width: 200 },
    { key: "locationCode", title: "隔离库位", width: 120 },
    {
      key: "status",
      title: "明细状态",
      width: 100,
      render: (v: number) => {
        const map: Record<number, string> = { 0: "待处理", 1: "已隔离", 2: "已处理" };
        return map[v] || String(v);
      },
    },
    { key: "handleResult", title: "处理结果", width: 150 },
  ];

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <AlertTriangle className="h-6 w-6 text-orange-500" />
          <h1 className="text-2xl font-bold">异常处理单列表</h1>
        </div>
      </div>

      {/* 搜索栏 */}
      <div className="bg-white rounded-lg shadow p-4 mb-4">
        <div className="flex flex-wrap gap-4">
          <div className="flex items-center gap-2">
            <Input
              placeholder="搜索单号/入库单号"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              className="w-48"
            />
            <Button onClick={handleSearch}>
              <Search className="h-4 w-4 mr-1" />
              查询
            </Button>
          </div>
          <Select value={exceptionTypeFilter || undefined} onValueChange={(v) => setExceptionTypeFilter(v || "")}>
            <SelectTrigger className="w-32">
              <SelectValue placeholder="异常类型" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部</SelectItem>
              <SelectItem value="1">破损</SelectItem>
              <SelectItem value="2">短缺</SelectItem>
              <SelectItem value="3">质量不合格</SelectItem>
              <SelectItem value="4">错货</SelectItem>
              <SelectItem value="5">其他</SelectItem>
            </SelectContent>
          </Select>
          <Select value={statusFilter || undefined} onValueChange={(v) => setStatusFilter(v || "")}>
            <SelectTrigger className="w-32">
              <SelectValue placeholder="状态" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部</SelectItem>
              <SelectItem value="0">待处理</SelectItem>
              <SelectItem value="1">处理中</SelectItem>
              <SelectItem value="2">已完成</SelectItem>
              <SelectItem value="3">已取消</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* 数据表格 */}
      <DataTable
        columns={columns}
        data={data}
        loading={loading}
        pagination={{
          page,
          pageSize,
          total,
          onChange: (p, ps) => {
            setPage(p);
            setPageSize(ps);
          },
        }}
      />

      {/* 详情弹窗 */}
      <Dialog open={detailVisible} onOpenChange={setDetailVisible}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>异常处理单详情</DialogTitle>
          </DialogHeader>
          {currentOrder && (
            <div className="space-y-4">
              {/* 基本信息 */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                <div>
                  <Label className="text-gray-500">异常处理单号</Label>
                  <div className="font-medium">{currentOrder.orderNo}</div>
                </div>
                <div>
                  <Label className="text-gray-500">入库单号</Label>
                  <div className="font-medium">{currentOrder.inboundOrderNo}</div>
                </div>
                <div>
                  <Label className="text-gray-500">供应商</Label>
                  <div className="font-medium">{currentOrder.supplierName}</div>
                </div>
                <div>
                  <Label className="text-gray-500">异常类型</Label>
                  <div className={`font-medium ${exceptionTypeMap[currentOrder.exceptionType]?.color}`}>
                    {exceptionTypeMap[currentOrder.exceptionType]?.text}
                  </div>
                </div>
                <div>
                  <Label className="text-gray-500">来源</Label>
                  <div className="font-medium">{sourceTypeMap[currentOrder.sourceType]}</div>
                </div>
                <div>
                  <Label className="text-gray-500">状态</Label>
                  <span className={`px-2 py-1 rounded text-xs ${statusMap[currentOrder.status]?.color}`}>
                    {statusMap[currentOrder.status]?.text}
                  </span>
                </div>
                <div>
                  <Label className="text-gray-500">异常数量</Label>
                  <div className="font-medium">{currentOrder.totalQty}</div>
                </div>
                <div>
                  <Label className="text-gray-500">处理方式</Label>
                  <div className="font-medium">
                    {currentOrder.handleType ? handleTypeMap[currentOrder.handleType] : "-"}
                  </div>
                </div>
                <div>
                  <Label className="text-gray-500">创建时间</Label>
                  <div className="font-medium">{currentOrder.createTime}</div>
                </div>
                <div>
                  <Label className="text-gray-500">处理时间</Label>
                  <div className="font-medium">{currentOrder.handleTime || "-"}</div>
                </div>
                <div>
                  <Label className="text-gray-500">处理人</Label>
                  <div className="font-medium">{currentOrder.handleUserName || "-"}</div>
                </div>
              </div>

              {/* 异常商品明细 */}
              <div className="mt-4">
                <h3 className="font-semibold mb-2">异常商品明细</h3>
                <DataTable
                  columns={itemColumns}
                  data={currentOrder.items || []}
                  pagination={false}
                />
              </div>

              {/* 操作按钮 */}
              <div className="flex justify-end gap-2 pt-4">
                <Button variant="outline" onClick={() => setDetailVisible(false)}>
                  关闭
                </Button>
                {currentOrder.status === 0 && (
                  <Button onClick={() => navigate(`/exception/${currentOrder.id}`)}>
                    去处理
                  </Button>
                )}
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* 取消弹窗 */}
      <Dialog open={cancelDialogOpen} onOpenChange={setCancelDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>取消异常处理单</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <Label>取消原因</Label>
              <Textarea
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                placeholder="请输入取消原因"
                rows={3}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCancelDialogOpen(false)}>
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
