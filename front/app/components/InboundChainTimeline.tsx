import { useState, useEffect } from "react";
import { ArrowRight, AlertTriangle, Package, RotateCcw, Trash2, Tag } from "lucide-react";
import { Button } from "./ui/button";

interface QuantityNode {
  expectedQty: number;
  receivedQty: number;
  qualifiedQty: number;
  rejectedQty: number;
  putawayQty: number;
  isolatedQty: number;
}

interface ReplacementInboundDTO {
  inboundOrderId: number;
  inboundOrderNo: string;
  quantityNode: QuantityNode;
  subExceptionChains: ExceptionChainDTO[];
}

interface ExceptionChainDTO {
  exceptionOrderId: number;
  exceptionOrderNo: string;
  exceptionType: number;
  exceptionTypeName: string;
  sourceType: number;
  sourceTypeName: string;
  exceptionQty: number;
  handleType: number;
  handleTypeName: string;
  status: number;
  statusName: string;
  replacementInbound: ReplacementInboundDTO | null;
}

interface InboundChainDTO {
  inboundOrderId: number;
  inboundOrderNo: string;
  orderType: number;
  orderTypeName: string;
  poNo: string;
  supplierName: string;
  quantityNode: QuantityNode;
  exceptionChains: ExceptionChainDTO[];
}

interface Props {
  inboundOrderId: number;
  onViewInbound?: (id: number) => void;
  onViewException?: (id: number) => void;
}

const handleTypeIcons: Record<number, React.ReactNode> = {
  1: <RotateCcw className="h-3 w-3" />, // 退货
  2: <RotateCcw className="h-3 w-3" />, // 换货
  3: <Trash2 className="h-3 w-3" />,    // 报废
  4: <Tag className="h-3 w-3" />,       // 降价销售
};

const handleTypeColors: Record<number, string> = {
  1: "text-blue-600 bg-blue-50",
  2: "text-green-600 bg-green-50",
  3: "text-red-600 bg-red-50",
  4: "text-orange-600 bg-orange-50",
};

export default function InboundChainTimeline({ inboundOrderId, onViewInbound, onViewException }: Props) {
  const [chain, setChain] = useState<InboundChainDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState<Set<string>>(new Set(["main"]));

  useEffect(() => {
    loadChain();
  }, [inboundOrderId]);

  const loadChain = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem("token");
      const res = await fetch(`/api/v1/inbound/orders/${inboundOrderId}/chain`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const result = await res.json();
      if (result.success) {
        setChain(result.data);
      }
    } catch (error) {
      console.error("加载链路失败", error);
    } finally {
      setLoading(false);
    }
  };

  const toggleExpand = (key: string) => {
    setExpanded(prev => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  };

  if (loading) {
    return <div className="p-4 text-center text-gray-500">加载中...</div>;
  }

  if (!chain) {
    return <div className="p-4 text-center text-gray-500">无链路数据</div>;
  }

  return (
    <div className="p-4 bg-gray-50 rounded-lg">
      {/* 主入库单 */}
      <div className="mb-4">
        <div className="flex items-center gap-2 mb-2">
          <Package className="h-4 w-4 text-blue-600" />
          <span className="font-medium">{chain.inboundOrderNo}</span>
          <span className="text-xs text-gray-500">({chain.orderTypeName})</span>
          {chain.poNo && (
            <span className="text-xs text-gray-400 ml-2">采购单: {chain.poNo}</span>
          )}
        </div>
        <QuantityFlow node={chain.quantityNode} />
      </div>

      {/* 异常处理链路 */}
      {chain.exceptionChains && chain.exceptionChains.length > 0 && (
        <div className="ml-4 border-l-2 border-orange-200 pl-4 space-y-4">
          {chain.exceptionChains.map((ex, idx) => (
            <ExceptionChainItem
              key={ex.exceptionOrderId}
              exception={ex}
              level={0}
              expanded={expanded}
              onToggle={toggleExpand}
              onViewInbound={onViewInbound}
              onViewException={onViewException}
            />
          ))}
        </div>
      )}
    </div>
  );
}

// 数量流转组件
function QuantityFlow({ node }: { node: QuantityNode }) {
  const items = [
    { label: "预期", value: node.expectedQty, color: "text-gray-600" },
    { label: "收货", value: node.receivedQty, color: "text-blue-600" },
    { label: "合格", value: node.qualifiedQty, color: "text-green-600" },
    { label: "上架", value: node.putawayQty, color: "text-green-700" },
  ];

  const rejectedOrIsolated = (node.rejectedQty > 0 || node.isolatedQty > 0);

  return (
    <div className="flex flex-wrap items-center gap-2 text-sm">
      {items.map((item, idx) => (
        <div key={idx} className="flex items-center gap-1">
          {idx > 0 && <ArrowRight className="h-3 w-3 text-gray-400" />}
          <span className={item.color}>
            {item.label}: <strong>{item.value}</strong>
          </span>
        </div>
      ))}
      {rejectedOrIsolated && (
        <div className="flex items-center gap-2 ml-2 text-orange-600">
          <ArrowRight className="h-3 w-3 text-gray-400" />
          {node.rejectedQty > 0 && <span>不合格: <strong>{node.rejectedQty}</strong></span>}
          {node.isolatedQty > 0 && <span>隔离: <strong>{node.isolatedQty}</strong></span>}
        </div>
      )}
    </div>
  );
}

// 异常处理链路项
function ExceptionChainItem({
  exception,
  level,
  expanded,
  onToggle,
  onViewInbound,
  onViewException,
}: {
  exception: ExceptionChainDTO;
  level: number;
  expanded: Set<string>;
  onToggle: (key: string) => void;
  onViewInbound?: (id: number) => void;
  onViewException?: (id: number) => void;
}) {
  const key = `ex-${exception.exceptionOrderId}`;
  const isExpanded = expanded.has(key);
  const hasReplacement = exception.replacementInbound !== null;

  return (
    <div>
      {/* 异常处理单 */}
      <div className="flex items-center gap-2 mb-1">
        <AlertTriangle className="h-4 w-4 text-orange-500" />
        <button
          onClick={() => onViewException?.(exception.exceptionOrderId)}
          className="text-sm font-medium text-orange-600 hover:underline"
        >
          {exception.exceptionOrderNo}
        </button>
        <span className="text-xs text-gray-500">
          ({exception.exceptionTypeName} - {exception.sourceTypeName})
        </span>
        <span className="text-sm">
          <strong className="text-orange-600">{exception.exceptionQty}</strong> 件
        </span>
        {exception.handleType && (
          <span className={`px-1.5 py-0.5 rounded text-xs ${handleTypeColors[exception.handleType]}`}>
            {handleTypeIcons[exception.handleType]}
            <span className="ml-1">{exception.handleTypeName}</span>
          </span>
        )}
      </div>

      {/* 补货入库单 */}
      {hasReplacement && exception.replacementInbound && (
        <div className="ml-4 mt-2 border-l-2 border-green-200 pl-4">
          <div className="flex items-center gap-2 mb-1">
            <Package className="h-4 w-4 text-green-600" />
            <button
              onClick={() => onViewInbound?.(exception.replacementInbound!.inboundOrderId)}
              className="text-sm font-medium text-green-600 hover:underline"
            >
              {exception.replacementInbound.inboundOrderNo}
            </button>
            <span className="text-xs text-gray-500">(补货入库)</span>
          </div>
          <QuantityFlow node={exception.replacementInbound.quantityNode} />

          {/* 递归显示子异常 */}
          {exception.replacementInbound.subExceptionChains &&
           exception.replacementInbound.subExceptionChains.length > 0 && (
            <div className="ml-4 mt-3 border-l-2 border-orange-200 pl-4 space-y-3">
              {exception.replacementInbound.subExceptionChains.map((subEx) => (
                <ExceptionChainItem
                  key={subEx.exceptionOrderId}
                  exception={subEx}
                  level={level + 1}
                  expanded={expanded}
                  onToggle={onToggle}
                  onViewInbound={onViewInbound}
                  onViewException={onViewException}
                />
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
