import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Layout } from './components/Layout';
import { Dashboard } from './pages/Dashboard';
import { Transactions } from './pages/Transactions';
import { Participants } from './pages/Participants';
import { RoutingRules } from './pages/RoutingRules';
import { BinTables } from './pages/BinTables';

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
        </Routes>
      </Layout>
    </BrowserRouter>
  );
}
