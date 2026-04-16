import { useState, useEffect } from "react";
import { Save } from "lucide-react";
import { toast } from "sonner";

const API_BASE = "/api/v1";

async function fetchApi(url: string, options?: RequestInit): Promise<any> {
  const token = localStorage.getItem("token");
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options?.headers as Record<string, string> || {}),
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    throw new Error(`请求失败: ${response.status}`);
  }

  return response.json();
}

interface Config {
  systemName: string;
  language: string;
  timezone: string;
  currency: string;
  safetyStockDays: string;
  expiryWarningDays: string;
  stagnantDays: string;
  pickingStrategy: string;
  inboundPrefix: string;
  outboundPrefix: string;
  inventoryPrefix: string;
  serialLength: string;
  sessionTimeout: string;
  accessTokenExpire: string;
  refreshTokenExpire: string;
  allowMultiLogin: boolean;
}

export default function SystemConfig() {
  const [formData, setFormData] = useState<Config>({
    systemName: "WMS仓库管理系统",
    language: "zh-CN",
    timezone: "GMT+8",
    currency: "CNY",
    safetyStockDays: "30",
    expiryWarningDays: "30",
    stagnantDays: "90",
    pickingStrategy: "S",
    inboundPrefix: "RK",
    outboundPrefix: "CK",
    inventoryPrefix: "PD",
    serialLength: "4",
    sessionTimeout: "30",
    accessTokenExpire: "1",
    refreshTokenExpire: "7",
    allowMultiLogin: true,
  });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    async function loadConfig() {
      try {
        const res = await fetchApi(`${API_BASE}/system/configs?limit=100`);
        const categories = res.data || {};
        const allConfigs: any[] = [];
        Object.values(categories).forEach((list: any) => {
          if (Array.isArray(list)) allConfigs.push(...list);
        });

        const configMap: Record<string, string> = {};
        allConfigs.forEach((c: any) => {
          configMap[c.code] = c.value;
        });

        setFormData(prev => ({
          ...prev,
          systemName: configMap["system.name"] || prev.systemName,
          language: configMap["system.language"] || prev.language,
          timezone: configMap["system.timezone"] || prev.timezone,
          currency: configMap["system.currency"] || prev.currency,
          safetyStockDays: configMap["business.safety_days"] || prev.safetyStockDays,
          expiryWarningDays: configMap["business.validity_warning_days"] || prev.expiryWarningDays,
          stagnantDays: configMap["business.stagnant_days"] || prev.stagnantDays,
          pickingStrategy: configMap["business.picking_strategy"] || prev.pickingStrategy,
          inboundPrefix: configMap["code.inbound_prefix"] || prev.inboundPrefix,
          outboundPrefix: configMap["code.outbound_prefix"] || prev.outboundPrefix,
          inventoryPrefix: configMap["code.stocktake_prefix"] || prev.inventoryPrefix,
          serialLength: configMap["code.serial_length"] || prev.serialLength,
          sessionTimeout: configMap["session.timeout_minutes"] || prev.sessionTimeout,
        }));
      } catch (error) {
        console.error("加载配置失败", error);
      }
    }
    loadConfig();
  }, []);

  const handleChange = (field: keyof Config, value: string | boolean) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const configs = [
        { code: "system.name", value: formData.systemName },
        { code: "system.language", value: formData.language },
        { code: "system.timezone", value: formData.timezone },
        { code: "system.currency", value: formData.currency },
        { code: "business.safety_days", value: formData.safetyStockDays },
        { code: "business.validity_warning_days", value: formData.expiryWarningDays },
        { code: "business.stagnant_days", value: formData.stagnantDays },
        { code: "business.picking_strategy", value: formData.pickingStrategy },
        { code: "code.inbound_prefix", value: formData.inboundPrefix },
        { code: "code.outbound_prefix", value: formData.outboundPrefix },
        { code: "code.stocktake_prefix", value: formData.inventoryPrefix },
        { code: "code.serial_length", value: formData.serialLength },
        { code: "session.timeout_minutes", value: formData.sessionTimeout },
      ];

      await fetchApi(`${API_BASE}/system/configs/batch-update`, {
        method: "PUT",
        body: JSON.stringify(configs),
      });
      toast.success("配置已保存");
    } catch (error) {
      toast.error("保存配置失败");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">系统配置</h2>
        <button
          onClick={handleSave}
          disabled={saving}
          className="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center gap-1 text-sm disabled:opacity-50"
        >
          <Save size={16} />
          {saving ? "保存中..." : "保存配置"}
        </button>
      </div>

      <div className="grid grid-cols-2 gap-4">
        {/* Basic Config */}
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3">基础参数配置</h3>
          <div className="space-y-3">
            <div>
              <label className="block text-xs text-gray-600 mb-1">系统名称</label>
              <input
                type="text"
                value={formData.systemName}
                onChange={(e) => handleChange("systemName", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-600 mb-1">默认语言</label>
              <select
                value={formData.language}
                onChange={(e) => handleChange("language", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="zh-CN">中文</option>
                <option value="en-US">English</option>
              </select>
            </div>
            <div>
              <label className="block text-xs text-gray-600 mb-1">时区</label>
              <select
                value={formData.timezone}
                onChange={(e) => handleChange("timezone", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="GMT+8">GMT+8 (北京时间)</option>
                <option value="GMT+7">GMT+7 (曼谷时间)</option>
              </select>
            </div>
            <div>
              <label className="block text-xs text-gray-600 mb-1">货币单位</label>
              <select
                value={formData.currency}
                onChange={(e) => handleChange("currency", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="CNY">CNY (人民币)</option>
                <option value="USD">USD (美元)</option>
              </select>
            </div>
          </div>
        </div>

        {/* Business Config */}
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3">业务参数配置</h3>
          <div className="space-y-3">
            <div>
              <label className="block text-xs text-gray-600 mb-1">默认安全库存天数</label>
              <input
                type="number"
                value={formData.safetyStockDays}
                onChange={(e) => handleChange("safetyStockDays", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-600 mb-1">效期预警天数</label>
              <input
                type="number"
                value={formData.expiryWarningDays}
                onChange={(e) => handleChange("expiryWarningDays", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-600 mb-1">呆滞天数</label>
              <input
                type="number"
                value={formData.stagnantDays}
                onChange={(e) => handleChange("stagnantDays", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-600 mb-1">拣货路径策略</label>
              <select
                value={formData.pickingStrategy}
                onChange={(e) => handleChange("pickingStrategy", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="S">S形路径</option>
                <option value="NEAREST">最近优先</option>
              </select>
            </div>
          </div>
        </div>

        {/* Code Rules */}
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3">编码规则配置</h3>
          <div className="space-y-3">
            <div>
              <label className="block text-xs text-gray-600 mb-1">入库单前缀</label>
              <input
                type="text"
                value={formData.inboundPrefix}
                onChange={(e) => handleChange("inboundPrefix", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-600 mb-1">出库单前缀</label>
              <input
                type="text"
                value={formData.outboundPrefix}
                onChange={(e) => handleChange("outboundPrefix", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-600 mb-1">盘点单前缀</label>
              <input
                type="text"
                value={formData.inventoryPrefix}
                onChange={(e) => handleChange("inventoryPrefix", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-600 mb-1">流水号位数</label>
              <input
                type="number"
                value={formData.serialLength}
                onChange={(e) => handleChange("serialLength", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
        </div>

        {/* Session Config */}
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3">会话管理配置</h3>
          <div className="space-y-3">
            <div>
              <label className="block text-xs text-gray-600 mb-1">会话超时时间(分钟)</label>
              <input
                type="number"
                value={formData.sessionTimeout}
                onChange={(e) => handleChange("sessionTimeout", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-600 mb-1">Access Token有效期(小时)</label>
              <input
                type="number"
                value={formData.accessTokenExpire}
                onChange={(e) => handleChange("accessTokenExpire", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-600 mb-1">Refresh Token有效期(天)</label>
              <input
                type="number"
                value={formData.refreshTokenExpire}
                onChange={(e) => handleChange("refreshTokenExpire", e.target.value)}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="multiLogin"
                checked={formData.allowMultiLogin}
                onChange={(e) => handleChange("allowMultiLogin", e.target.checked)}
                className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <label htmlFor="multiLogin" className="text-xs text-gray-600">允许同一账号多设备登录</label>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}