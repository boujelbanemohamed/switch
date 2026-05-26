import { BrowserRouter, Routes, Route } from 'react-router-dom';
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

export default function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/transactions" element={<Transactions />} />
          <Route path="/participants" element={<Participants />} />
          <Route path="/routing" element={<RoutingRules />} />
          <Route path="/bin-tables" element={<BinTables />} />
          <Route path="/issuing" element={<Issuing />} />
          <Route path="/acquiring" element={<Acquiring />} />
          <Route path="/authorization" element={<Authorization />} />
          <Route path="/fraud" element={<Fraud />} />
          <Route path="/clearing" element={<Clearing />} />
          <Route path="/backoffice" element={<Backoffice />} />
          <Route path="/ecommerce" element={<Ecommerce />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  );
}
