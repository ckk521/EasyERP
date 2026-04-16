import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router";
import { ArrowLeft, Package, User, Clock, CheckCircle } from "lucide-react";
import DataTable from "../../components/DataTable";
import { toast } from "sonner";

interface InboundDetail {
  id: number;
  code: string;
  type: string;
  poNo: string;
  supplier: string;
  warehouse: string;
  status: string;
  createTime: string;
  creator: string;
  items: any[];
  timeline: any[];
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

export default function InboundDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<InboundDetail | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDetail();
  }, [id]);

  async function fetchDetail() {
    try {
      setLoading(true);
      const data = await fetchApi<InboundDetail>(`/api/v1/inbound/orders/${id}`);
      setDetail(data);
    } catch {
      toast.error("获取详情失败");
    } finally {
      setLoading(false);
    }
  }

  const itemColumns = [
    { key: "sku", title: "SKU", width: "130px" },
    { key: "name", title: "商品名称", width: "160px" },
    { key: "expected", title: "预期数量", width: "90px" },
    { key: "received", title: "实收数量", width: "90px" },
    { key: "qualified", title: "合格数量", width: "90px" },
    { key: "location", title: "库位", width: "100px" },
  ];

  if (loading) {
    return (
      <div className="p-4">
        <div className="text-center py-12 text-gray-500">加载中...</div>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="p-4">
        <div className="text-center py-12 text-gray-500">
          <div className="text-4xl mb-2">📋</div>
          <div>暂无数据</div>
          <div className="text-sm text-gray-400 mt-1">API 暂未实现，后端对接后可正常使用</div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-4">
        <button onClick={() => navigate(-1)} className="p-2 hover:bg-gray-100 rounded">
          <ArrowLeft size={20} />
        </button>
        <h2 className="text-lg font-semibold">入库单详情 - {detail.code}</h2>
      </div>

      {/* 基本信息 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <div className="grid grid-cols-4 gap-4">
          <div className="flex items-center gap-2">
            <Package size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">状态：</span>
            <span className={`px-2 py-0.5 rounded text-xs ${detail.status === '已完成' ? 'bg-green-100 text-green-700' : 'bg-blue-100 text-blue-700'}`}>{detail.status}</span>
          </div>
          <div className="flex items-center gap-2">
            <User size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">创建人：</span>
            <span className="text-sm font-medium">{detail.creator}</span>
          </div>
          <div className="flex items-center gap-2">
            <Clock size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">创建时间：</span>
            <span className="text-sm font-medium">{detail.createTime}</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle size={16} className="text-gray-400" />
            <span className="text-sm text-gray-500">入库类型：</span>
            <span className="text-sm font-medium">{detail.type}</span>
          </div>
        </div>
      </div>

      {/* 商品明细 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <h3 className="text-sm font-semibold mb-3">商品明细</h3>
        <DataTable columns={itemColumns} data={detail.items || []} />
      </div>

      {/* 操作时间线 */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <h3 className="text-sm font-semibold mb-3">操作时间线</h3>
        <div className="space-y-3">
          {detail.timeline?.map((item, index) => (
            <div key={index} className="flex items-start gap-3">
              <div className="w-2 h-2 rounded-full bg-blue-500 mt-1.5" />
              <div>
                <div className="text-sm font-medium">{item.time} - {item.status}</div>
                <div className="text-xs text-gray-500">{item.operator}：{item.desc}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}