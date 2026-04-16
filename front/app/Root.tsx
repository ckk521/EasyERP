import { Outlet, Link, useLocation, useNavigate } from "react-router";
import {
  LayoutDashboard, Package, Warehouse, Users, FileText,
  ArrowLeftRight, Settings, TrendingUp, RotateCcw,
  Menu, X, Bell, User, LogOut
} from "lucide-react";
import { useState, useEffect } from "react";
import { Toaster } from "sonner";

const menuItems = [
  { 
    title: "工作台", 
    icon: LayoutDashboard, 
    path: "/" 
  },
  {
    title: "基础数据",
    icon: Warehouse,
    children: [
      { title: "仓库管理", path: "/base/warehouse" },
      { title: "库位管理", path: "/base/location" },
      { title: "商品管理", path: "/base/product" },
      { title: "供应商", path: "/base/supplier" },
      { title: "客户管理", path: "/base/customer" },
    ]
  },
  {
    title: "入库管理",
    icon: Package,
    children: [
      { title: "入库单", path: "/inbound/list" },
    ]
  },
  {
    title: "出库管理",
    icon: ArrowLeftRight,
    children: [
      { title: "出库单", path: "/outbound/list" },
      { title: "波次管理", path: "/outbound/wave" },
    ]
  },
  {
    title: "库存管理",
    icon: Package,
    children: [
      { title: "库存查询", path: "/inventory/list" },
      { title: "库存盘点", path: "/inventory/stocktake" },
      { title: "库存调拨", path: "/inventory/transfer" },
    ]
  },
  {
    title: "报表分析",
    icon: TrendingUp,
    children: [
      { title: "报表总览", path: "/reports/overview" },
    ]
  },
  {
    title: "退换货",
    icon: RotateCcw,
    children: [
      { title: "退货单", path: "/return/list" },
    ]
  },
  {
    title: "系统管理",
    icon: Settings,
    children: [
      { title: "用户管理", path: "/system/user" },
      { title: "角色管理", path: "/system/role" },
      { title: "系统配置", path: "/system/config" },
      { title: "操作日志", path: "/system/log" },
    ]
  },
];

export default function Root() {
  const location = useLocation();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [expandedMenus, setExpandedMenus] = useState<string[]>(["基础数据"]);

  // 获取当前登录用户
  const [currentUser, setCurrentUser] = useState<{name?: string} | null>(() => {
    const userStr = localStorage.getItem("user");
    return userStr ? JSON.parse(userStr) : null;
  });

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    navigate("/login");
  };

  // Check auth on mount
  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      navigate("/login");
    }
  }, [navigate]);

  const toggleMenu = (title: string) => {
    if (expandedMenus.includes(title)) {
      setExpandedMenus(expandedMenus.filter(t => t !== title));
    } else {
      setExpandedMenus([...expandedMenus, title]);
    }
  };

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <aside className={`bg-gray-900 text-white transition-all duration-300 ${sidebarOpen ? 'w-56' : 'w-16'} flex flex-col`}>
        <div className="p-3 border-b border-gray-700 flex items-center justify-between">
          {sidebarOpen && <h1 className="font-bold text-lg">WMS系统</h1>}
          <button 
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="p-1 hover:bg-gray-800 rounded"
          >
            {sidebarOpen ? <X size={18} /> : <Menu size={18} />}
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto py-2 text-sm">
          {menuItems.map((item) => (
            <div key={item.title}>
              {item.children ? (
                <>
                  <button
                    onClick={() => toggleMenu(item.title)}
                    className="w-full flex items-center gap-2 px-3 py-2 hover:bg-gray-800 text-left"
                  >
                    <item.icon size={16} />
                    {sidebarOpen && (
                      <>
                        <span className="flex-1">{item.title}</span>
                        <span className={`transition-transform ${expandedMenus.includes(item.title) ? 'rotate-90' : ''}`}>›</span>
                      </>
                    )}
                  </button>
                  {sidebarOpen && expandedMenus.includes(item.title) && (
                    <div className="bg-gray-800">
                      {item.children.map((child) => (
                        <Link
                          key={child.path}
                          to={child.path}
                          className={`block px-3 py-1.5 pl-9 hover:bg-gray-700 ${
                            location.pathname === child.path ? 'bg-gray-700 border-l-2 border-blue-500' : ''
                          }`}
                        >
                          {child.title}
                        </Link>
                      ))}
                    </div>
                  )}
                </>
              ) : (
                <Link
                  to={item.path!}
                  className={`flex items-center gap-2 px-3 py-2 hover:bg-gray-800 ${
                    location.pathname === item.path ? 'bg-gray-800 border-l-2 border-blue-500' : ''
                  }`}
                >
                  <item.icon size={16} />
                  {sidebarOpen && <span>{item.title}</span>}
                </Link>
              )}
            </div>
          ))}
        </nav>

        {sidebarOpen && (
          <div className="p-3 border-t border-gray-700 text-xs text-gray-400">
            © 2026 WMS v1.0
          </div>
        )}
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <header className="bg-white border-b border-gray-200 px-4 py-2 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <h2 className="text-sm font-semibold text-gray-700">
              {menuItems.find(m => m.path === location.pathname)?.title || 
               menuItems.flatMap(m => m.children || []).find(c => c.path === location.pathname)?.title || 
               '仓库管理系统'}
            </h2>
          </div>
          
          <div className="flex items-center gap-3">
            <button className="p-1.5 hover:bg-gray-100 rounded relative">
              <Bell size={18} />
              <span className="absolute top-0 right-0 w-2 h-2 bg-red-500 rounded-full"></span>
            </button>
            <div className="flex items-center gap-2 px-2 py-1 hover:bg-gray-100 rounded cursor-pointer group relative">
              <User size={18} />
              <span className="text-sm">{currentUser?.name || "管理员"}</span>
              {/* Dropdown Menu */}
              <div className="absolute right-0 top-full mt-1 w-40 bg-white rounded-lg shadow-lg border border-gray-200 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-50">
                <button
                  onClick={handleLogout}
                  className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2 rounded-lg"
                >
                  <LogOut size={14} />
                  退出登录
                </button>
              </div>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-auto p-4">
          <Outlet />
        </main>
      </div>
      <Toaster />
    </div>
  );
}
