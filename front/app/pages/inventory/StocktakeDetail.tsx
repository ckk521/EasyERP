import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router";
import { ArrowLeft, CheckCircle, XCircle, AlertCircle, Search } from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Input } from "../../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Textarea } from "../../components/ui/textarea";
import { toast } from "sonner";

interface StocktakeItem {
  id: number;
  orderId: number;
  orderNo: string;
  productId: number;
  skuCode: string;
  productName: string;
  barcode: string;
  locationId: number;
  locationCode: string;
  batchNo: string;
  systemQty: number;
  countedQty: number | null;
  diffQty: number | null;
  diffReason: string | null;
  diffReasonName: string | null;
  diffRemark: string | null;
  status: number;
  statusName: string;
  roundNo: number;
  countUserName: string | null;
  countTime: string | null;
}

interface StocktakeOrder {
  id: number;
  orderNo: string;
  warehouseName: string;
  stocktakeTypeName: string;
  stocktakeType: number;
  blindMode: number;
  status: number;
  statusName: string;
  totalItems: number;
  countedItems: number;
  diffItems: number;
  accuracyRate: number | null;
  // 循环盘字段
  cycleType?: string;
  cycleDay?: number;
  cycleStrategy?: string;
  cycleStrategyName?: string;
  nextCycleDate?: string;
  canGenerateCycleData?: boolean;
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

// 差异原因选项
const DIFF_REASONS = [
  { value: "profit", label: "盘盈", desc: "实际数量大于系统数量" },
  { value: "loss", label: "盘亏", desc: "实际数量小于系统数量" },
  { value: "wrong", label: "错放", desc: "商品放置在错误库位" },
  { value: "missed", label: "漏扫", desc: "之前入库时漏扫" },
  { value: "other", label: "其他", desc: "其他原因" },
];

export default function StocktakeDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [order, setOrder] = useState<StocktakeOrder | null>(null);
  const [items, setItems] = useState<StocktakeItem[]>([]);
  const [approveRecords, setApproveRecords] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [filterStatus, setFilterStatus] = useState<string>("all");
  const [countDialogOpen, setCountDialogOpen] = useState(false);
  const [finishDialogOpen, setFinishDialogOpen] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [selectedItem, setSelectedItem] = useState<StocktakeItem | null>(null);
  const [countForm, setCountForm] = useState({
    countedQty: 0,
    diffReason: "",
    diffRemark: "",
  });

  useEffect(() => {
    if (id) {
      fetchOrderDetail();
      fetchApproveRecords();
    }
  }, [id]);

  async function fetchOrderDetail() {
    try {
      setLoading(true);
      const data = await fetchApi<StocktakeOrder & { items: StocktakeItem[] }>(`/api/stocktake/detail/${id}`);
      setOrder(data);
      setItems(data.items || []);
    } catch (err) {
      toast.error("加载盘点单失败");
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  async function fetchApproveRecords() {
    try {
      const data = await fetchApi<any[]>(`/api/stocktake/approve-records/${id}`);
      setApproveRecords(data || []);
    } catch (err) {
      // 审核记录加载失败不影响主流程
      console.error("加载审核记录失败", err);
    }
  }

  // 筛选后的明细列表
  const filteredItems = items.filter((item) => {
    const matchStatus = filterStatus === "all" ||
      (filterStatus === "pending" && item.status === 0) ||
      (filterStatus === "counted" && item.status === 1);

    const matchKeyword = !searchKeyword ||
      item.skuCode?.toLowerCase().includes(searchKeyword.toLowerCase()) ||
      item.productName?.toLowerCase().includes(searchKeyword.toLowerCase()) ||
      item.locationCode?.toLowerCase().includes(searchKeyword.toLowerCase()) ||
      item.barcode?.toLowerCase().includes(searchKeyword.toLowerCase());

    return matchStatus && matchKeyword;
  });

  // 打开盘点弹窗
  const openCountDialog = (item: StocktakeItem) => {
    setSelectedItem(item);
    setCountForm({
      countedQty: item.countedQty ?? item.systemQty,
      diffReason: item.diffReason || "",
      diffRemark: item.diffRemark || "",
    });
    setCountDialogOpen(true);
  };

  // 提交盘点数量
  const handleCountSubmit = async () => {
    if (!selectedItem) return;

    const diffQty = countForm.countedQty - selectedItem.systemQty;

    // 有差异时必须选择原因
    if (diffQty !== 0 && !countForm.diffReason) {
      toast.error("存在差异，请选择差异原因");
      return;
    }

    try {
      await fetchApi("/api/stocktake/count", {
        method: "POST",
        body: JSON.stringify({
          itemId: selectedItem.id,
          countedQty: countForm.countedQty,
          diffReason: countForm.diffReason || null,
          diffRemark: countForm.diffRemark || null,
          roundNo: 1,
        }),
      });
      toast.success("盘点录入成功");
      setCountDialogOpen(false);
      fetchOrderDetail();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "盘点录入失败");
    }
  };

  // 完成盘点
  const handleFinish = async () => {
    try {
      await fetchApi(`/api/stocktake/finish/${id}`, {
        method: "POST",
      });
      toast.success("盘点已完成，进入审核状态");
      setFinishDialogOpen(false);
      navigate("/inventory/stocktake");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "完成盘点失败");
    }
  };

  // 生成循环盘数据
  const handleGenerateCycleData = async () => {
    try {
      await fetchApi(`/api/stocktake/generate-cycle-data/${id}`, {
        method: "POST",
      });
      toast.success("盘点数据生成成功");
      fetchOrderDetail();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "生成失败");
    }
  };

  // 审核通过
  const handleApprove = async () => {
    try {
      await fetchApi(`/api/stocktake/approve/${id}`, {
        method: "POST",
      });
      toast.success("审核通过，库存已调整");
      fetchOrderDetail();
      fetchApproveRecords();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "审核失败");
    }
  };

  // 审核驳回
  const handleReject = async () => {
    if (!rejectReason.trim()) {
      toast.error("请输入驳回原因");
      return;
    }
    try {
      await fetchApi(`/api/stocktake/reject/${id}`, {
        method: "POST",
        body: JSON.stringify({ reason: rejectReason }),
      });
      toast.success("已驳回，可重新盘点");
      setRejectDialogOpen(false);
      setRejectReason("");
      fetchOrderDetail();
      fetchApproveRecords();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "驳回失败");
    }
  };

  // 计算差异
  const currentDiff = selectedItem
    ? countForm.countedQty - selectedItem.systemQty
    : 0;

  // 统计信息
  const stats = {
    total: items.length,
    counted: items.filter(i => i.status >= 1).length,
    profit: items.filter(i => i.diffQty && i.diffQty > 0).length,
    loss: items.filter(i => i.diffQty && i.diffQty < 0).length,
  };

  // 判断是否是循环盘且无数据
  const isCycleWithoutData = order?.stocktakeType === 3 && items.length === 0 && order?.canGenerateCycleData;

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">加载中...</div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">盘点单不存在</div>
      </div>
    );
  }

  const isBlindMode = order.blindMode === 1;
  const canCount = order.status === 0 || order.status === 1;
  const canFinish = order.status === 1 && stats.counted === stats.total;
  const canApprove = order.status === 2; // 待审核状态

  return (
    <div className="space-y-4">
      {/* 头部信息 */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate("/inventory/stocktake")}
          className="p-2 hover:bg-gray-100 rounded"
        >
          <ArrowLeft size={20} />
        </button>
        <div className="flex-1">
          <h2 className="text-lg font-semibold">盘点作业</h2>
          <div className="text-sm text-gray-500">
            {order.orderNo} - {order.warehouseName}
          </div>
        </div>
        <div className="flex items-center gap-2">
          <span className={`px-3 py-1 rounded text-sm ${
            order.status === 0 ? "bg-yellow-100 text-yellow-700" :
            order.status === 1 ? "bg-blue-100 text-blue-700" :
            order.status === 2 ? "bg-orange-100 text-orange-700" :
            "bg-green-100 text-green-700"
          }`}>
            {order.statusName}
          </span>
          {isBlindMode && (
            <span className="px-3 py-1 rounded text-sm bg-red-100 text-red-700">
              盲盘模式
            </span>
          )}
          {/* 审核按钮 */}
          {canApprove && (
            <>
              <button
                onClick={() => setRejectDialogOpen(true)}
                className="px-3 py-1.5 border border-red-300 text-red-600 rounded hover:bg-red-50 text-sm"
              >
                驳回
              </button>
              <button
                onClick={handleApprove}
                className="px-3 py-1.5 bg-green-600 text-white rounded hover:bg-green-700 text-sm"
              >
                审核通过
              </button>
            </>
          )}
        </div>
      </div>

      {/* 循环盘无数据提示 */}
      {isCycleWithoutData && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 text-center">
          <div className="text-blue-600 text-4xl mb-3">📅</div>
          <h3 className="text-lg font-medium text-blue-700 mb-2">循环盘点策略</h3>
          <div className="bg-white rounded-lg p-4 mb-4 inline-block">
            <div className="text-left space-y-2">
              <div className="flex items-center gap-2">
                <span className="text-gray-500 text-sm">盘点周期：</span>
                <span className="font-medium">
                  {order.cycleType === "daily" && "每日盘点"}
                  {order.cycleType === "weekly" && `每周${["一", "二", "三", "四", "五", "六", "日"][order.cycleDay! - 1]}盘点`}
                  {order.cycleType === "monthly" && `每月第${order.cycleDay}天盘点`}
                </span>
              </div>
              {order.nextCycleDate && (
                <div className="flex items-center gap-2">
                  <span className="text-gray-500 text-sm">下次盘点日：</span>
                  <span className="font-medium text-blue-600">{order.nextCycleDate}</span>
                </div>
              )}
              {order.cycleStrategyName && (
                <div className="flex items-center gap-2">
                  <span className="text-gray-500 text-sm">轮转策略：</span>
                  <span className="font-medium">{order.cycleStrategyName}</span>
                </div>
              )}
            </div>
          </div>
          <p className="text-sm text-gray-600 mb-4">
            数据将在盘点日（{order.nextCycleDate}）凌晨0点自动更新
          </p>
          <button
            onClick={handleGenerateCycleData}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            立即生成盘点数据
          </button>
        </div>
      )}

      {/* 统计卡片 */}
      <div className="grid grid-cols-4 gap-4">
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <div className="text-sm text-gray-500">总商品数</div>
          <div className="text-2xl font-bold">{stats.total}</div>
        </div>
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <div className="text-sm text-gray-500">已盘点</div>
          <div className="text-2xl font-bold text-blue-600">{stats.counted}</div>
          <div className="text-xs text-gray-400">
            进度 {((stats.counted / stats.total) * 100).toFixed(1)}%
          </div>
        </div>
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <div className="text-sm text-gray-500">盘盈</div>
          <div className="text-2xl font-bold text-green-600">{stats.profit}</div>
        </div>
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <div className="text-sm text-gray-500">盘亏</div>
          <div className="text-2xl font-bold text-red-600">{stats.loss}</div>
        </div>
      </div>

      {/* 筛选和操作 */}
      <div className="flex items-center gap-4">
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          <input
            type="text"
            placeholder="搜索SKU、商品名称、库位、条码..."
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <select
          value={filterStatus}
          onChange={(e) => setFilterStatus(e.target.value)}
          className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="all">全部状态</option>
          <option value="pending">待盘点</option>
          <option value="counted">已盘点</option>
        </select>
        {canFinish && (
          <button
            onClick={() => setFinishDialogOpen(true)}
            className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 flex items-center gap-2"
          >
            <CheckCircle size={18} />
            完成盘点
          </button>
        )}
      </div>

      {/* 明细列表 - 循环盘无数据时不显示 */}
      {!isCycleWithoutData && (
        <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">SKU编码</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">商品名称</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">库位</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">批次号</th>
              <th className="px-4 py-3 text-right text-sm font-medium text-gray-600">
                {isBlindMode ? "系统数量" : "系统数量"}
              </th>
              <th className="px-4 py-3 text-right text-sm font-medium text-gray-600">盘点数量</th>
              <th className="px-4 py-3 text-right text-sm font-medium text-gray-600">差异</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">状态</th>
              <th className="px-4 py-3 text-center text-sm font-medium text-gray-600">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {filteredItems.map((item) => (
              <tr key={item.id} className="hover:bg-gray-50">
                <td className="px-4 py-3 text-sm font-mono">{item.skuCode}</td>
                <td className="px-4 py-3 text-sm">{item.productName}</td>
                <td className="px-4 py-3 text-sm font-mono">{item.locationCode}</td>
                <td className="px-4 py-3 text-sm">{item.batchNo || "-"}</td>
                <td className="px-4 py-3 text-sm text-right">
                  {isBlindMode ? "****" : item.systemQty}
                </td>
                <td className="px-4 py-3 text-sm text-right">
                  {item.countedQty ?? "-"}
                </td>
                <td className="px-4 py-3 text-sm text-right">
                  {item.diffQty !== null ? (
                    <span className={item.diffQty > 0 ? "text-green-600" : item.diffQty < 0 ? "text-red-600" : ""}>
                      {item.diffQty > 0 ? `+${item.diffQty}` : item.diffQty}
                    </span>
                  ) : "-"}
                </td>
                <td className="px-4 py-3">
                  <span className={`px-2 py-0.5 rounded text-xs ${
                    item.status === 0 ? "bg-yellow-100 text-yellow-700" :
                    item.status === 1 ? "bg-green-100 text-green-700" :
                    "bg-blue-100 text-blue-700"
                  }`}>
                    {item.statusName}
                  </span>
                </td>
                <td className="px-4 py-3 text-center">
                  {canCount && item.status === 0 && (
                    <button
                      onClick={() => openCountDialog(item)}
                      className="px-3 py-1 text-sm bg-blue-600 text-white rounded hover:bg-blue-700"
                    >
                      盘点
                    </button>
                  )}
                  {item.status >= 1 && (
                    <span className="text-sm text-gray-500">
                      {item.countUserName} {item.countTime?.substring(11, 16)}
                    </span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {filteredItems.length === 0 && (
          <div className="text-center py-12 text-gray-500">
            没有找到匹配的商品
          </div>
        )}
      </div>
      )}

      {/* 盘点录入弹窗 */}
      <Dialog open={countDialogOpen} onOpenChange={setCountDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>盘点录入</DialogTitle>
          </DialogHeader>

          {selectedItem && (
            <div className="space-y-4 py-4">
              {/* 商品信息 */}
              <div className="bg-gray-50 rounded-lg p-3 space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">SKU</span>
                  <span className="font-mono">{selectedItem.skuCode}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">商品名称</span>
                  <span>{selectedItem.productName}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">库位</span>
                  <span className="font-mono">{selectedItem.locationCode}</span>
                </div>
                {!isBlindMode && (
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-500">系统数量</span>
                    <span className="font-bold">{selectedItem.systemQty}</span>
                  </div>
                )}
              </div>

              {/* 盘点数量 */}
              <div className="space-y-2">
                <Label>盘点数量 <span className="text-red-500">*</span></Label>
                <Input
                  type="number"
                  min="0"
                  value={countForm.countedQty}
                  onChange={(e) => setCountForm({ ...countForm, countedQty: parseInt(e.target.value) || 0 })}
                  className="text-lg font-bold"
                />
              </div>

              {/* 差异提示 */}
              {!isBlindMode && currentDiff !== 0 && (
                <div className={`p-3 rounded-lg flex items-start gap-2 ${
                  currentDiff > 0 ? "bg-green-50 border border-green-200" : "bg-red-50 border border-red-200"
                }`}>
                  {currentDiff > 0 ? (
                    <AlertCircle className="text-green-600 mt-0.5" size={18} />
                  ) : (
                    <AlertCircle className="text-red-600 mt-0.5" size={18} />
                  )}
                  <div>
                    <div className={`font-medium ${currentDiff > 0 ? "text-green-700" : "text-red-700"}`}>
                      {currentDiff > 0 ? `盘盈 ${currentDiff}` : `盘亏 ${Math.abs(currentDiff)}`}
                    </div>
                    <div className="text-sm text-gray-600">请选择差异原因</div>
                  </div>
                </div>
              )}

              {/* 差异原因 */}
              {currentDiff !== 0 && (
                <div className="space-y-2">
                  <Label>差异原因 <span className="text-red-500">*</span></Label>
                  <div className="grid grid-cols-2 gap-2">
                    {DIFF_REASONS.map((reason) => {
                      // 根据差异类型禁用不合理的选项
                      const isDisabled =
                        (currentDiff > 0 && (reason.value === "loss" || reason.value === "missed")) ||
                        (currentDiff < 0 && reason.value === "profit");

                      return (
                        <button
                          key={reason.value}
                          type="button"
                          disabled={isDisabled}
                          onClick={() => setCountForm({ ...countForm, diffReason: reason.value })}
                          className={`p-2 text-left rounded border text-sm transition-colors ${
                            isDisabled
                              ? "border-gray-100 bg-gray-50 text-gray-300 cursor-not-allowed"
                              : countForm.diffReason === reason.value
                                ? "border-blue-500 bg-blue-50 text-blue-700"
                                : "border-gray-200 bg-white hover:border-blue-300"
                          }`}
                        >
                          <div className="font-medium">{reason.label}</div>
                          <div className={`text-xs ${isDisabled ? "text-gray-300" : "text-gray-500"}`}>
                            {reason.desc}
                          </div>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* 差异说明 */}
              {currentDiff !== 0 && (
                <div className="space-y-2">
                  <Label>差异说明</Label>
                  <Textarea
                    value={countForm.diffRemark}
                    onChange={(e) => setCountForm({ ...countForm, diffRemark: e.target.value })}
                    placeholder="选填，可记录详细说明"
                    rows={2}
                  />
                </div>
              )}
            </div>
          )}

          <DialogFooter>
            <button
              type="button"
              onClick={() => setCountDialogOpen(false)}
              className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
            >
              取消
            </button>
            <button
              type="button"
              onClick={handleCountSubmit}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              确认
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 完成盘点确认弹窗 */}
      <Dialog open={finishDialogOpen} onOpenChange={setFinishDialogOpen}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>确认完成盘点</DialogTitle>
          </DialogHeader>

          <div className="py-4">
            <div className="bg-blue-50 rounded-lg p-4 space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-gray-500">总商品数</span>
                <span className="font-bold">{stats.total}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-500">已盘点</span>
                <span className="font-bold text-blue-600">{stats.counted}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-500">盘盈</span>
                <span className="font-bold text-green-600">{stats.profit}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-500">盘亏</span>
                <span className="font-bold text-red-600">{stats.loss}</span>
              </div>
            </div>
            <p className="text-sm text-gray-500 mt-3">
              完成后将进入审核状态，无法再修改盘点数据。
            </p>
          </div>

          <DialogFooter>
            <button
              type="button"
              onClick={() => setFinishDialogOpen(false)}
              className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
            >
              取消
            </button>
            <button
              type="button"
              onClick={handleFinish}
              className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
            >
              确认完成
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 驳回确认对话框 */}
      <Dialog open={rejectDialogOpen} onOpenChange={setRejectDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>驳回盘点单</DialogTitle>
          </DialogHeader>
          <div className="py-4 space-y-4">
            <p className="text-sm text-gray-600">
              驳回后，盘点单将回到"盘点中"状态，可以重新盘点。
            </p>
            <div className="space-y-2">
              <Label>驳回原因 <span className="text-red-500">*</span></Label>
              <Textarea
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="请输入驳回原因"
                rows={3}
              />
            </div>
          </div>
          <DialogFooter>
            <button
              type="button"
              onClick={() => {
                setRejectDialogOpen(false);
                setRejectReason("");
              }}
              className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
            >
              取消
            </button>
            <button
              type="button"
              onClick={handleReject}
              className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
            >
              确认驳回
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 审核记录 */}
      {approveRecords.length > 0 && (
        <div className="bg-white rounded-lg border border-gray-200 p-4">
          <h3 className="text-sm font-medium text-gray-700 mb-3">审核记录</h3>
          <div className="space-y-3">
            {approveRecords.map((record, index) => (
              <div key={record.id || index} className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg">
                <div className={`p-2 rounded-full ${
                  record.action === 'approve' ? 'bg-green-100' : 'bg-red-100'
                }`}>
                  {record.action === 'approve' ? (
                    <CheckCircle size={16} className="text-green-600" />
                  ) : (
                    <XCircle size={16} className="text-red-600" />
                  )}
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                      record.action === 'approve'
                        ? 'bg-green-100 text-green-700'
                        : 'bg-red-100 text-red-700'
                    }`}>
                      {record.action === 'approve' ? '审核通过' : '驳回'}
                    </span>
                  </div>
                  <div className="flex items-center gap-4 text-sm text-gray-600 mb-1">
                    <span>
                      <span className="text-gray-400">审核人：</span>
                      <span className="font-medium">{record.operator_name}</span>
                    </span>
                    {record.operator_role_name && (
                      <span>
                        <span className="text-gray-400">角色：</span>
                        <span>{record.operator_role_name}</span>
                      </span>
                    )}
                  </div>
                  {record.reason && (
                    <div className="text-sm text-gray-500 mt-1">
                      <span className="text-gray-400">原因：</span>{record.reason}
                    </div>
                  )}
                  <div className="text-xs text-gray-400 mt-1">
                    <span className="text-gray-400">时间：</span>{record.create_time}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
