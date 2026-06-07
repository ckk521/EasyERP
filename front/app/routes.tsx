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
import OutboundDetail from "./pages/outbound/OutboundDetail";
import WaveList from "./pages/outbound/WaveList";
import PickPage from "./pages/outbound/PickPage";
import PackPage from "./pages/outbound/PackPage";
import ShipPage from "./pages/outbound/ShipPage";
import InventoryList from "./pages/inventory/InventoryList";
import InventoryManagement from "./pages/inventory/InventoryManagement";
import StocktakeList from "./pages/inventory/StocktakeList";
import StocktakeDetail from "./pages/inventory/StocktakeDetail";
import TransferList from "./pages/inventory/TransferList";
import ExceptionList from "./pages/exception/ExceptionList";
import ExceptionDetail from "./pages/exception/ExceptionDetail";
import ReportOverview from "./pages/reports/ReportOverview";
import ReturnOrderList from "./pages/return/ReturnOrderList";
import ReturnOrderCreate from "./pages/return/ReturnOrderCreate";
import ReturnOrderDetail from "./pages/return/ReturnOrderDetail";
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
    element: <RequireAuth><Root /></RequireAuth>,
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
      { path: "outbound/:id", Component: OutboundDetail },
      { path: "outbound/wave", Component: WaveList },
      { path: "outbound/pick", Component: PickPage },
      { path: "outbound/pack", Component: PackPage },
      { path: "outbound/ship", Component: ShipPage },

      // 库存管理
      { path: "inventory/list", Component: InventoryManagement },
      { path: "inventory/management", Component: InventoryManagement },
      { path: "inventory/stocktake", Component: StocktakeList },
      { path: "inventory/stocktake/:id", Component: StocktakeDetail },
      { path: "inventory/transfer", Component: TransferList },

      // 异常管理
      { path: "exception/list", Component: ExceptionList },
      { path: "exception/:id", Component: ExceptionDetail },

      // 报表分析
      { path: "reports/overview", Component: ReportOverview },

      // 退换货
      { path: "return", Component: ReturnOrderList },
      { path: "return/list", Component: ReturnOrderList },
      { path: "return/create", Component: ReturnOrderCreate },
      { path: "return/:id", Component: ReturnOrderDetail },

      // 系统管理
      { path: "system/user", Component: UserList },
      { path: "system/role", Component: RoleList },
      { path: "system/config", Component: SystemConfig },
      { path: "system/log", Component: OperationLog },

      { path: "*", Component: NotFound },
    ],
  },
]);
