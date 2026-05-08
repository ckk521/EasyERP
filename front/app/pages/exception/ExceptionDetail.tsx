import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router";
import { ArrowLeft, Check, X, AlertTriangle } from "lucide-react";
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
  inboundOrderId: number;
  inboundOrderNo: string;
  purchaseOrderNo: string | null;
  supplierName: string;
  warehouseCode: string;
  zoneId: number;
  zoneCode: string;
  exceptionType: number;
  totalQty: number;
  exceptionReason: string;
  status: number;
  handleType: number | null;
  handleResult: string | null;
  handleTime: string | null;
  handleUserName: string | null;
  sourceType: number;
  remark: string;
  createUserName: string;
  createTime: string;
  items: ExceptionItem[];
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
  locationId: number | null;
  locationCode: string | null;
  status: number;
  handleType: number | null;
  handleQty: number | null;
  handleResult: string | null;
}

interface LocationItem {
  id: number;
  code: string;
}

export default function ExceptionDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [order, setOrder] = useState<ExceptionOrder | null>(null);
  const [isolateVisible, setIsolateVisible] = useState(false);
  const [handleVisible, setHandleVisible] = useState(false);
  const [locations, setLocations] = useState<LocationItem[]>([]);
  const [isolateItems, setIsolateItems] = useState<Record<number, { locationId: number; locationCode: string }>>({});
  const [handleForm, setHandleForm] = useState({
    handleType: "1",
    handleResult: "",
    logisticsCompany: "",
    logisticsNo: "",
    discountPercent: 0,
  });

  useEffect(() => {
    loadDetail();
  }, [id]);

  const loadDetail = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const token = localStorage.getItem("token");
      const res = await fetch(`/api/v1/exception-orders/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const result = await res.json();
      if (result.success) {
        setOrder(result.data);
      } else {
        toast.error("加载失败");
      }
    } catch (error) {
      toast.error("加载失败");
    } finally {
      setLoading(false);
    }
  };

  const loadLocations = async () => {
    if (!order) return;
    try {
      const token = localStorage.getItem("token");
      const res = await fetch(`/api/v1/base/locations?zoneId=${order.zoneId}&status=1&limit=100`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const result = await res.json();
      if (result.success) {
        setLocations(result.data.list || []);
      }
    } catch (error) {
      console.error("加载库位失败", error);
    }
  };

  const handleIsolate = async () => {
    if (!order) return;

    const pendingItems = order.items.filter((item) => item.status === 0);
    for (const item of pendingItems) {
      if (!isolateItems[item.id]) {
        toast.error(`请为商品 ${item.skuCode} 选择隔离库位`);
        return;
      }
    }

    setLoading(true);
    try {
      const token = localStorage.getItem("token");
      const res = await fetch(`/api/v1/exception-orders/${order.id}/isolate`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          orderId: order.id,
          items: pendingItems.map((item) => ({
            itemId: item.id,
            locationId: isolateItems[item.id].locationId,
            locationCode: isolateItems[item.id].locationCode,
          })),
        }),
      });

      const result = await res.json();
      if (result.success) {
        toast.success("隔离入库成功");
        setIsolateVisible(false);
        loadDetail();
      } else {
        toast.error(result.message || "操作失败");
      }
    } catch (error) {
      toast.error("操作失败");
    } finally {
      setLoading(false);
    }
  };

  const handleException = async () => {
    if (!order) return;

    setLoading(true);
    try {
      const token = localStorage.getItem("token");
      const res = await fetch(`/api/v1/exception-orders/${order.id}/handle`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          handleType: parseInt(handleForm.handleType),
          handleResult: handleForm.handleResult,
          logisticsCompany: handleForm.logisticsCompany,
          logisticsNo: handleForm.logisticsNo,
          discountPercent: handleForm.discountPercent,
        }),
      });

      const result = await res.json();
      if (result.success) {
        toast.success("处理成功");
        setHandleVisible(false);
        loadDetail();
      } else {
        toast.error(result.message || "处理失败");
      }
    } catch (error) {
      toast.error("处理失败");
    } finally {
      setLoading(false);
    }
  };

  const openIsolateModal = () => {
    loadLocations();
    setIsolateItems({});
    setIsolateVisible(true);
  };

  const itemColumns = [
    { key: "skuCode", title: "SKU编码", width: 120 },
    { key: "productName", title: "商品名称", width: 200 },
    { key: "exceptionQty", title: "异常数量", width: 80 },
    {
      key: "exceptionType",
      title: "异常类型",
      width: 100,
      render: (_: unknown, row: ExceptionItem) => (
        <span className={exceptionTypeMap[row.exceptionType]?.color}>
          {exceptionTypeMap[row.exceptionType]?.text}
        </span>
      ),
    },
    { key: "exceptionReason", title: "异常原因", width: 200 },
    { key: "locationCode", title: "隔离库位", width: 120 },
    {
      key: "status",
      title: "状态",
      width: 100,
      render: (_: unknown, row: ExceptionItem) => {
        const map: Record<number, { text: string; color: string }> = {
          0: { text: "待处理", color: "bg-orange-100 text-orange-700" },
          1: { text: "已隔离", color: "bg-blue-100 text-blue-700" },
          2: { text: "已处理", color: "bg-green-100 text-green-700" },
        };
        const item = map[row.status];
        return (
          <span className={`px-2 py-1 rounded text-xs ${item?.color}`}>
            {item?.text}
          </span>
        );
      },
    },
    {
      key: "handleType",
      title: "处理方式",
      width: 100,
      render: (_: unknown, row: ExceptionItem) => (row.handleType != null ? handleTypeMap[row.handleType] : "-"),
    },
    { key: "handleResult", title: "处理结果", width: 150 },
  ];

  if (!order) {
    return <div className="p-6">加载中...</div>;
  }

  // Debug log
  console.log("ExceptionOrder data:", {
    status: order.status,
    handleType: order.handleType,
    replacementInboundOrderId: order.replacementInboundOrderId,
    showButton: order.status === 2 && (order.handleType === 1 || order.handleType === 2) && !order.replacementInboundOrderId
  });

  return (
    <div className="p-6">
      {/* 头部 */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" onClick={() => navigate("/exception/list")}>
            <ArrowLeft className="h-4 w-4 mr-1" />
            返回
          </Button>
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-6 w-6 text-orange-500" />
            <h1 className="text-2xl font-bold">异常处理单详情</h1>
          </div>
        </div>
        <div className="flex gap-2">
          {order.status === 0 && (
            <>
              <Button onClick={openIsolateModal}>隔离入库</Button>
              <Button variant="destructive" onClick={async () => {
                const reason = prompt("请输入取消原因");
                if (reason) {
                  try {
                    const token = localStorage.getItem("token");
                    const res = await fetch(`/api/v1/exception-orders/${order.id}/cancel`, {
                      method: "POST",
                      headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                      },
                      body: JSON.stringify({ cancelReason: reason }),
                    });
                    const result = await res.json();
                    if (result.success) {
                      toast.success("取消成功");
                      navigate("/exception/list");
                    } else {
                      toast.error(result.message || "取消失败");
                    }
                  } catch (error) {
                    toast.error("取消失败");
                  }
                }
              }}>
                取消
              </Button>
            </>
          )}
          {order.status === 1 && (
            <>
              <Button onClick={() => setHandleVisible(true)}>异常处理</Button>
              <Button variant="outline" onClick={async () => {
                if (confirm("确定要撤销隔离入库吗？撤销后状态将回滚为待处理。")) {
                  try {
                    const token = localStorage.getItem("token");
                    const res = await fetch(`/api/v1/exception-orders/${order.id}/undo-isolate`, {
                      method: "POST",
                      headers: {
                        Authorization: `Bearer ${token}`,
                      },
                    });
                    const result = await res.json();
                    if (result.success) {
                      toast.success("撤销成功");
                      loadDetail();
                    } else {
                      toast.error(result.message || "撤销失败");
                    }
                  } catch (error) {
                    toast.error("撤销失败");
                  }
                }
              }}>
                撤销隔离
              </Button>
            </>
          )}
          {order.status === 2 && (order.handleType === 1 || order.handleType === 2) && !order.replacementInboundOrderId && (
            <Button onClick={async () => {
              if (confirm("确定要创建补货入库单吗？创建后供应商可进行补货入库。")) {
                try {
                  const token = localStorage.getItem("token");
                  const res = await fetch(`/api/v1/exception-orders/${order.id}/create-replacement`, {
                    method: "POST",
                    headers: {
                      Authorization: `Bearer ${token}`,
                    },
                  });
                  const result = await res.json();
                  if (result.success) {
                    toast.success("补货入库单创建成功");
                    loadDetail();
                  } else {
                    toast.error(result.message || "创建失败");
                  }
                } catch (error) {
                  toast.error("创建失败");
                }
              }
            }}>
              创建补货入库单
            </Button>
          )}
          {order.replacementInboundOrderId && (
            <Button variant="outline" onClick={() => navigate(`/inbound/${order.replacementInboundOrderId}`)}>
              查看补货入库单
            </Button>
          )}
        </div>
      </div>

      {/* 状态步骤 */}
      <div className="bg-white rounded-lg shadow p-4 mb-4">
        <div className="flex items-center justify-center gap-8">
          {[
            { step: 0, title: "待处理", desc: "异常已登记" },
            { step: 1, title: "处理中", desc: "已隔离入库" },
            { step: 2, title: "已完成", desc: "处理完成" },
          ].map((s, idx) => (
            <div key={s.step} className="flex items-center">
              <div className="flex flex-col items-center">
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center ${
                    order.status >= s.step
                      ? "bg-blue-500 text-white"
                      : "bg-gray-200 text-gray-500"
                  }`}
                >
                  {order.status > s.step ? <Check className="h-5 w-5" /> : s.step + 1}
                </div>
                <div className="mt-2 text-sm font-medium">{s.title}</div>
                <div className="text-xs text-gray-500">{s.desc}</div>
              </div>
              {idx < 2 && (
                <div
                  className={`w-24 h-1 mx-4 ${
                    order.status > s.step ? "bg-blue-500" : "bg-gray-200"
                  }`}
                />
              )}
            </div>
          ))}
        </div>
      </div>

      {/* 基本信息 */}
      <div className="bg-white rounded-lg shadow p-4 mb-4">
        <h2 className="font-semibold mb-4">基本信息</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
          <div>
            <Label className="text-gray-500">异常处理单号</Label>
            <div className="font-medium">{order.orderNo}</div>
          </div>
          <div>
            <Label className="text-gray-500">入库单号</Label>
            <button
              className="text-blue-600 hover:underline"
              onClick={() => navigate(`/inbound/${order.inboundOrderId}`)}
            >
              {order.inboundOrderNo}
            </button>
          </div>
          <div>
            <Label className="text-gray-500">采购订单号</Label>
            <div className="font-medium">{order.purchaseOrderNo || "-"}</div>
          </div>
          <div>
            <Label className="text-gray-500">供应商</Label>
            <div className="font-medium">{order.supplierName}</div>
          </div>
          <div>
            <Label className="text-gray-500">仓库编码</Label>
            <div className="font-medium">{order.warehouseCode}</div>
          </div>
          <div>
            <Label className="text-gray-500">隔离库区</Label>
            <div className="font-medium">{order.zoneCode}</div>
          </div>
          <div>
            <Label className="text-gray-500">异常类型</Label>
            <span className={exceptionTypeMap[order.exceptionType]?.color}>
              {exceptionTypeMap[order.exceptionType]?.text}
            </span>
          </div>
          <div>
            <Label className="text-gray-500">来源</Label>
            <div className="font-medium">{sourceTypeMap[order.sourceType]}</div>
          </div>
          <div>
            <Label className="text-gray-500">状态</Label>
            <span className={`px-2 py-1 rounded text-xs ${statusMap[order.status]?.color}`}>
              {statusMap[order.status]?.text}
            </span>
          </div>
          <div>
            <Label className="text-gray-500">异常数量</Label>
            <div className="font-medium">{order.totalQty}</div>
          </div>
          <div>
            <Label className="text-gray-500">处理方式</Label>
            <div className="font-medium">
              {order.handleType ? handleTypeMap[order.handleType] : "-"}
            </div>
          </div>
          <div>
            <Label className="text-gray-500">处理人</Label>
            <div className="font-medium">{order.handleUserName || "-"}</div>
          </div>
          <div>
            <Label className="text-gray-500">创建人</Label>
            <div className="font-medium">{order.createUserName}</div>
          </div>
          <div>
            <Label className="text-gray-500">创建时间</Label>
            <div className="font-medium">{order.createTime}</div>
          </div>
          <div>
            <Label className="text-gray-500">处理时间</Label>
            <div className="font-medium">{order.handleTime || "-"}</div>
          </div>
          <div className="col-span-2">
            <Label className="text-gray-500">异常原因</Label>
            <div className="font-medium">{order.exceptionReason || "-"}</div>
          </div>
          <div className="col-span-2">
            <Label className="text-gray-500">备注</Label>
            <div className="font-medium">{order.remark || "-"}</div>
          </div>
          {order.replacementInboundOrderId && (
            <div className="col-span-2">
              <Label className="text-gray-500">补货入库单</Label>
              <button
                className="text-blue-600 hover:underline"
                onClick={() => navigate(`/inbound/${order.replacementInboundOrderId}`)}
              >
                {order.replacementInboundOrderNo}
              </button>
            </div>
          )}
        </div>
      </div>

      {/* 异常商品明细 */}
      <div className="bg-white rounded-lg shadow p-4">
        <h2 className="font-semibold mb-4">异常商品明细</h2>
        <DataTable columns={itemColumns} data={order.items || []} pagination={false} />
      </div>

      {/* 隔离入库弹窗 */}
      <Dialog open={isolateVisible} onOpenChange={setIsolateVisible}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>隔离入库</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <DataTable
              columns={[
                { key: "skuCode", title: "SKU编码", width: 100 },
                { key: "productName", title: "商品名称", width: 150 },
                { key: "exceptionQty", title: "异常数量", width: 80 },
                {
                  key: "location",
                  title: "隔离库位",
                  width: 200,
                  render: (_: unknown, row: ExceptionItem) => (
                    <Select
                      value={isolateItems[row.id]?.locationId?.toString() || ""}
                      onValueChange={(v) => {
                        const loc = locations.find((l) => l.id === parseInt(v));
                        if (loc) {
                          setIsolateItems({
                            ...isolateItems,
                            [row.id]: { locationId: loc.id, locationCode: loc.code },
                          });
                        }
                      }}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="选择库位" />
                      </SelectTrigger>
                      <SelectContent>
                        {locations.map((loc) => (
                          <SelectItem key={loc.id} value={loc.id.toString()}>
                            {loc.code}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  ),
                },
              ]}
              data={order.items.filter((item) => item.status === 0)}
              pagination={false}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsolateVisible(false)}>
              取消
            </Button>
            <Button onClick={handleIsolate} disabled={loading}>
              确认入库
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 异常处理弹窗 */}
      <Dialog open={handleVisible} onOpenChange={setHandleVisible}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>异常处理</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <Label>处理方式</Label>
              <Select
                value={handleForm.handleType}
                onValueChange={(v) => setHandleForm({ ...handleForm, handleType: v })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="1">退货</SelectItem>
                  <SelectItem value="2">换货</SelectItem>
                  <SelectItem value="3">报废</SelectItem>
                  <SelectItem value="4">降价销售</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {handleForm.handleType === "1" && (
              <>
                <div>
                  <Label>物流公司</Label>
                  <Input
                    value={handleForm.logisticsCompany}
                    onChange={(e) =>
                      setHandleForm({ ...handleForm, logisticsCompany: e.target.value })
                    }
                    placeholder="输入物流公司"
                  />
                </div>
                <div>
                  <Label>物流单号</Label>
                  <Input
                    value={handleForm.logisticsNo}
                    onChange={(e) => setHandleForm({ ...handleForm, logisticsNo: e.target.value })}
                    placeholder="输入物流单号"
                  />
                </div>
              </>
            )}

            {handleForm.handleType === "4" && (
              <div>
                <Label>降价比例 (%)</Label>
                <Input
                  type="number"
                  value={handleForm.discountPercent}
                  onChange={(e) =>
                    setHandleForm({ ...handleForm, discountPercent: parseInt(e.target.value) || 0 })
                  }
                  min={0}
                  max={100}
                />
              </div>
            )}

            <div>
              <Label>处理结果</Label>
              <Textarea
                value={handleForm.handleResult}
                onChange={(e) => setHandleForm({ ...handleForm, handleResult: e.target.value })}
                placeholder="输入处理结果说明"
                rows={3}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setHandleVisible(false)}>
              取消
            </Button>
            <Button onClick={handleException} disabled={loading}>
              确认处理
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
