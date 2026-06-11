import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Layout } from './components/Layout';
import { Dashboard } from './pages/Dashboard';
import { Transactions } from './pages/Transactions';
import { Participants } from './pages/Participants';
import { RoutingRules } from './pages/RoutingRules';
import { BinTables } from './pages/BinTables';
import { Issuing } from './pages/Issuing';
import { Acquiring } from './pages/Acquiring';
import { Authorization } from './pages/Authorization';
import { Fraud } from './pages/Fraud';
import { Clearing } from './pages/Clearing';
import { ErrorBoundary } from './components/ErrorBoundary';
import { Backoffice } from './pages/Backoffice';
import { Ecommerce } from './pages/Ecommerce';
import { MerchantPortal } from './pages/MerchantPortal';
import { Disputes } from './pages/Disputes';
import { Batch } from './pages/Batch';
import { Netting } from './pages/Netting';
import { FeeSchedules } from './pages/FeeSchedules';
import { InterchangeFees } from './pages/InterchangeFees';
import { CardPrograms } from './pages/CardPrograms';
import { VirtualCards } from './pages/VirtualCards';
import { Kyc } from './pages/Kyc';
import { Login } from './pages/Login';
import { Profile } from './pages/Profile';
import { Users } from './pages/Users';
import { Reports } from './pages/Reports';
import { ConfigLive } from './pages/ConfigLive';
import { StandIn } from './pages/StandIn';
import { CofPage } from './pages/CofPage';
import { CreditLines } from './pages/CreditLines';
import { Loyalty } from './pages/Loyalty';
import { Transfers } from './pages/Transfers';
import { FxRates } from './pages/FxRates';
import { RegulatoryReports } from './pages/RegulatoryReports';
import { PosSimulator } from './pages/PosSimulator';
import type { ReactNode } from 'react';

function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, loading } = useAuth();
  if (loading) return <div style={{ padding: 40, textAlign: 'center', color: 'var(--text-secondary)' }}>Loading...</div>;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function AppRoutes() {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      <Route path="/login" element={isAuthenticated ? <Navigate to="/" replace /> : <Login />} />
      <Route path="/" element={<ProtectedRoute><Layout><Dashboard /></Layout></ProtectedRoute>} />
      <Route path="/transactions" element={<ProtectedRoute><Layout><Transactions /></Layout></ProtectedRoute>} />
      <Route path="/participants" element={<ProtectedRoute><Layout><Participants /></Layout></ProtectedRoute>} />
      <Route path="/routing" element={<ProtectedRoute><Layout><RoutingRules /></Layout></ProtectedRoute>} />
      <Route path="/bin-tables" element={<ProtectedRoute><Layout><BinTables /></Layout></ProtectedRoute>} />
      <Route path="/issuing" element={<ProtectedRoute><Layout><Issuing /></Layout></ProtectedRoute>} />
      <Route path="/acquiring" element={<ProtectedRoute><Layout><Acquiring /></Layout></ProtectedRoute>} />
      <Route path="/authorization" element={<ProtectedRoute><Layout><Authorization /></Layout></ProtectedRoute>} />
      <Route path="/fraud" element={<ProtectedRoute><Layout><Fraud /></Layout></ProtectedRoute>} />
      <Route path="/clearing" element={<ProtectedRoute><Layout><ErrorBoundary><Clearing /></ErrorBoundary></Layout></ProtectedRoute>} />
      <Route path="/backoffice" element={<ProtectedRoute><Layout><Backoffice /></Layout></ProtectedRoute>} />
      <Route path="/ecommerce" element={<ProtectedRoute><Layout><Ecommerce /></Layout></ProtectedRoute>} />
      <Route path="/disputes" element={<ProtectedRoute><Layout><Disputes /></Layout></ProtectedRoute>} />
      <Route path="/merchant-portal" element={<ProtectedRoute><Layout><MerchantPortal /></Layout></ProtectedRoute>} />
      <Route path="/batch" element={<ProtectedRoute><Layout><Batch /></Layout></ProtectedRoute>} />
      <Route path="/netting" element={<ProtectedRoute><Layout><Netting /></Layout></ProtectedRoute>} />
      <Route path="/fees" element={<ProtectedRoute><Layout><FeeSchedules /></Layout></ProtectedRoute>} />
      <Route path="/interchange-fees" element={<ProtectedRoute><Layout><InterchangeFees /></Layout></ProtectedRoute>} />
      <Route path="/card-programs" element={<ProtectedRoute><Layout><CardPrograms /></Layout></ProtectedRoute>} />
      <Route path="/virtual-cards" element={<ProtectedRoute><Layout><VirtualCards /></Layout></ProtectedRoute>} />
      <Route path="/kyc" element={<ProtectedRoute><Layout><Kyc /></Layout></ProtectedRoute>} />
      <Route path="/profile" element={<ProtectedRoute><Layout><Profile /></Layout></ProtectedRoute>} />
      <Route path="/users" element={<ProtectedRoute><Layout><Users /></Layout></ProtectedRoute>} />
      <Route path="/reports" element={<ProtectedRoute><Layout><Reports /></Layout></ProtectedRoute>} />
      <Route path="/config-live" element={<ProtectedRoute><Layout><ConfigLive /></Layout></ProtectedRoute>} />
      <Route path="/stand-in" element={<ProtectedRoute><Layout><StandIn /></Layout></ProtectedRoute>} />
      <Route path="/credit" element={<ProtectedRoute><Layout><CreditLines /></Layout></ProtectedRoute>} />
      <Route path="/loyalty" element={<ProtectedRoute><Layout><Loyalty /></Layout></ProtectedRoute>} />
      <Route path="/transfers" element={<ProtectedRoute><Layout><Transfers /></Layout></ProtectedRoute>} />
      <Route path="/cof" element={<ProtectedRoute><Layout><CofPage /></Layout></ProtectedRoute>} />
      <Route path="/fx-rates" element={<ProtectedRoute><Layout><FxRates /></Layout></ProtectedRoute>} />
      <Route path="/regulatory-reports" element={<ProtectedRoute><Layout><RegulatoryReports /></Layout></ProtectedRoute>} />
      <Route path="/pos-simulator" element={<ProtectedRoute><Layout><PosSimulator /></Layout></ProtectedRoute>} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
