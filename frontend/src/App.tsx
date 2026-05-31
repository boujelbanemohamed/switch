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
import { Backoffice } from './pages/Backoffice';
import { Ecommerce } from './pages/Ecommerce';
import { MerchantPortal } from './pages/MerchantPortal';
import { Login } from './pages/Login';
import { Profile } from './pages/Profile';
import { Users } from './pages/Users';
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
      <Route path="/clearing" element={<ProtectedRoute><Layout><Clearing /></Layout></ProtectedRoute>} />
      <Route path="/backoffice" element={<ProtectedRoute><Layout><Backoffice /></Layout></ProtectedRoute>} />
      <Route path="/ecommerce" element={<ProtectedRoute><Layout><Ecommerce /></Layout></ProtectedRoute>} />
      <Route path="/merchant-portal" element={<ProtectedRoute><Layout><MerchantPortal /></Layout></ProtectedRoute>} />
      <Route path="/profile" element={<ProtectedRoute><Layout><Profile /></Layout></ProtectedRoute>} />
      <Route path="/users" element={<ProtectedRoute><Layout><Users /></Layout></ProtectedRoute>} />
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
