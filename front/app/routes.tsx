import { createBrowserRouter, Navigate } from "react-router";
import Root from "./Root";
import Dashboard from "./pages/Dashboard";
import WarehouseManagement from "./pages/base-data/WarehouseManagement";
import ProductList from "./pages/base-data/ProductList";
import SupplierList from "./pages/base-data/SupplierList";
import SupplierProductList from "./pages/base-data/SupplierProductList";
import CustomerList from "./pages/base-data/CustomerList";
import InboundList from "./pages/inbound/InboundList";
import InboundDetail from "./pages/inbound/InboundDetail";
import ReceivePage from "./pages/inbound/ReceivePage";
import InspectPage from "./pages/inbound/InspectPage";
import PutawayPage from "./pages/inbound/PutawayPage";
import OutboundList from "./pages/outbound/OutboundList";
import WaveList from "./pages/outbound/WaveList";
import InventoryList from "./pages/inventory/InventoryList";
import InventoryManagement from "./pages/inventory/InventoryManagement";
import StocktakeList from "./pages/inventory/StocktakeList";
import TransferList from "./pages/inventory/TransferList";
import ReportOverview from "./pages/reports/ReportOverview";
import ReturnList from "./pages/return/ReturnList";
import UserList from "./pages/system/UserList";
import RoleList from "./pages/system/RoleList";
import SystemConfig from "./pages/system/SystemConfig";
import OperationLog from "./pages/system/OperationLog";
import NotFound from "./pages/NotFound";
import Login from "./pages/Login";

// Auth check component
function RequireAuth({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem("token");
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

export const router = createBrowserRouter([
  // Login route (standalone, no Root layout)
  { path: "/login", Component: Login },

  // Protected routes with Root layout
  {
    path: "/",
    Component: Root,
    children: [
      { index: true, Component: Dashboard },

      // 基础数据
      { path: "base/warehouse", Component: WarehouseManagement },
      { path: "base/product", Component: ProductList },
      { path: "base/supplier", Component: SupplierList },
      { path: "base/supplier-product", Component: SupplierProductList },
      { path: "base/customer", Component: CustomerList },

      // 入库管理
      { path: "inbound/list", Component: InboundList },
      { path: "inbound/:id", Component: InboundDetail },
      { path: "inbound/receive", Component: ReceivePage },
      { path: "inbound/inspect", Component: InspectPage },
      { path: "inbound/putaway", Component: PutawayPage },

      // 出库管理
      { path: "outbound/list", Component: OutboundList },
      { path: "outbound/wave", Component: WaveList },

      // 库存管理
      { path: "inventory/list", Component: InventoryManagement },
      { path: "inventory/management", Component: InventoryManagement },
      { path: "inventory/stocktake", Component: StocktakeList },
      { path: "inventory/transfer", Component: TransferList },

      // 报表分析
      { path: "reports/overview", Component: ReportOverview },

      // 退换货
      { path: "return/list", Component: ReturnList },

      // 系统管理
      { path: "system/user", Component: UserList },
      { path: "system/role", Component: RoleList },
      { path: "system/config", Component: SystemConfig },
      { path: "system/log", Component: OperationLog },

      { path: "*", Component: NotFound },
    ],
  },
]);
