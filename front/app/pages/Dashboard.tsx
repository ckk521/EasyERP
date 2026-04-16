import { TrendingUp, TrendingDown, Package, ShoppingCart, AlertCircle, Clock } from "lucide-react";
import { AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from "recharts";

const stats = [
  { title: "今日入库", value: "156", change: "+12%", trend: "up", icon: Package, color: "blue" },
  { title: "今日出库", value: "243", change: "+8%", trend: "up", icon: ShoppingCart, color: "green" },
  { title: "库存预警", value: "23", change: "-5", trend: "down", icon: AlertCircle, color: "orange" },
  { title: "待处理", value: "18", change: "+3", trend: "up", icon: Clock, color: "red" },
];

const trendData = [
  { date: "01-04", inbound: 120, outbound: 180 },
  { date: "01-05", inbound: 150, outbound: 210 },
  { date: "01-06", inbound: 140, outbound: 195 },
  { date: "01-07", inbound: 160, outbound: 220 },
  { date: "01-08", inbound: 155, outbound: 240 },
  { date: "01-09", inbound: 165, outbound: 235 },
  { date: "01-10", inbound: 156, outbound: 243 },
];

const categoryData = [
  { name: "电子数码", value: 3500 },
  { name: "服装服饰", value: 2800 },
  { name: "食品保健", value: 2200 },
  { name: "美妆个护", value: 1800 },
  { name: "其他", value: 1200 },
];

const warehouseData = [
  { warehouse: "深圳仓", stock: 15600, capacity: 20000 },
  { warehouse: "上海仓", stock: 12300, capacity: 18000 },
  { warehouse: "越南仓", stock: 8900, capacity: 15000 },
  { warehouse: "泰国仓", stock: 6500, capacity: 12000 },
];

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

const recentActivities = [
  { id: 1, type: "入库", order: "RK-20260410-0012", status: "已完成", time: "10:23", operator: "张三" },
  { id: 2, type: "出库", order: "CK-20260410-0089", status: "拣货中", time: "10:18", operator: "李四" },
  { id: 3, type: "盘点", order: "PD-20260410-0003", status: "进行中", time: "09:45", operator: "王五" },
  { id: 4, type: "入库", order: "RK-20260410-0011", status: "验收中", time: "09:30", operator: "赵六" },
  { id: 5, type: "出库", order: "CK-20260410-0088", status: "已发货", time: "09:15", operator: "孙七" },
];

const alerts = [
  { id: 1, type: "库存预警", message: "SKU-12345 库存不足，当前: 50", level: "high" },
  { id: 2, type: "效期预警", message: "SKU-67890 将于15天后过期", level: "medium" },
  { id: 3, type: "待审核", message: "入库单 RK-20260410-0010 待质检", level: "low" },
];

export default function Dashboard() {
  return (
    <div className="space-y-4">
      {/* Stats Cards */}
      <div className="grid grid-cols-4 gap-4">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <div key={stat.title} className="bg-white rounded-lg p-4 border border-gray-200">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs text-gray-500">{stat.title}</span>
                <Icon size={16} className={`text-${stat.color}-500`} />
              </div>
              <div className="flex items-baseline gap-2">
                <span className="text-2xl font-bold">{stat.value}</span>
                <span className={`text-xs flex items-center ${stat.trend === 'up' ? 'text-green-600' : 'text-red-600'}`}>
                  {stat.trend === 'up' ? <TrendingUp size={12} /> : <TrendingDown size={12} />}
                  {stat.change}
                </span>
              </div>
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-3 gap-4">
        {/* Trend Chart */}
        <div className="col-span-2 bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3">出入库趋势</h3>
          <ResponsiveContainer width="100%" height={200}>
            <AreaChart data={trendData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="date" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip contentStyle={{ fontSize: 12 }} />
              <Legend wrapperStyle={{ fontSize: 11 }} />
              <Area type="monotone" dataKey="inbound" stackId="1" stroke="#3b82f6" fill="#3b82f6" fillOpacity={0.6} name="入库" />
              <Area type="monotone" dataKey="outbound" stackId="2" stroke="#10b981" fill="#10b981" fillOpacity={0.6} name="出库" />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Category Distribution */}
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3">品类分布</h3>
          <ResponsiveContainer width="100%" height={200}>
            <PieChart>
              <Pie
                data={categoryData}
                cx="50%"
                cy="50%"
                innerRadius={40}
                outerRadius={70}
                paddingAngle={2}
                dataKey="value"
              >
                {categoryData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip contentStyle={{ fontSize: 11 }} />
            </PieChart>
          </ResponsiveContainer>
          <div className="grid grid-cols-2 gap-2 mt-2 text-xs">
            {categoryData.map((item, index) => (
              <div key={item.name} className="flex items-center gap-1">
                <div className="w-2 h-2 rounded-full" style={{ backgroundColor: COLORS[index] }}></div>
                <span className="text-gray-600">{item.name}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4">
        {/* Warehouse Capacity */}
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3">仓库容量</h3>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={warehouseData} layout="horizontal">
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis type="number" tick={{ fontSize: 11 }} />
              <YAxis dataKey="warehouse" type="category" tick={{ fontSize: 11 }} width={60} />
              <Tooltip contentStyle={{ fontSize: 11 }} />
              <Bar dataKey="stock" fill="#3b82f6" name="当前库存" />
              <Bar dataKey="capacity" fill="#e5e7eb" name="总容量" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Recent Activities */}
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3">最近活动</h3>
          <div className="space-y-2">
            {recentActivities.map((activity) => (
              <div key={activity.id} className="flex items-center justify-between text-xs py-1.5 border-b border-gray-100">
                <div className="flex items-center gap-2">
                  <span className={`px-1.5 py-0.5 rounded text-xs ${
                    activity.type === '入库' ? 'bg-blue-100 text-blue-700' :
                    activity.type === '出库' ? 'bg-green-100 text-green-700' :
                    'bg-orange-100 text-orange-700'
                  }`}>
                    {activity.type}
                  </span>
                  <span className="text-gray-600">{activity.order}</span>
                </div>
                <span className="text-gray-400">{activity.time}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Alerts */}
        <div className="bg-white rounded-lg p-4 border border-gray-200">
          <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
            <AlertCircle size={16} className="text-orange-500" />
            预警提醒
          </h3>
          <div className="space-y-2">
            {alerts.map((alert) => (
              <div key={alert.id} className={`p-2 rounded text-xs border-l-2 ${
                alert.level === 'high' ? 'bg-red-50 border-red-500' :
                alert.level === 'medium' ? 'bg-orange-50 border-orange-500' :
                'bg-blue-50 border-blue-500'
              }`}>
                <div className="font-medium mb-1">{alert.type}</div>
                <div className="text-gray-600">{alert.message}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
