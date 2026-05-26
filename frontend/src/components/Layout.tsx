import { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard, Repeat, Building2, Network, GitCompare,
  CreditCard, Store, ShieldCheck, Siren, DollarSign, Settings, ShoppingCart,
} from 'lucide-react';

interface LayoutProps {
  children: ReactNode;
}

const mainNavItems = [
  { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/transactions', icon: Repeat, label: 'Transactions' },
  { to: '/participants', icon: Building2, label: 'Participants' },
  { to: '/routing', icon: GitCompare, label: 'Routing Rules' },
  { to: '/bin-tables', icon: Network, label: 'BIN Tables' },
];

const moduleNavItems = [
  { to: '/issuing', icon: CreditCard, label: 'Issuing' },
  { to: '/acquiring', icon: Store, label: 'Acquiring' },
  { to: '/authorization', icon: ShieldCheck, label: 'Authorization' },
  { to: '/fraud', icon: Siren, label: 'Fraud' },
  { to: '/clearing', icon: DollarSign, label: 'Clearing' },
  { to: '/backoffice', icon: Settings, label: 'Back Office' },
  { to: '/ecommerce', icon: ShoppingCart, label: 'E-Commerce' },
];

export function Layout({ children }: LayoutProps) {
  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      <nav style={{
        width: 240,
        background: 'var(--surface)',
        borderRight: '1px solid var(--border)',
        padding: '1.5rem 0',
        display: 'flex',
        flexDirection: 'column',
      }}>
        <div style={{ padding: '0 1.5rem', marginBottom: 24 }}>
          <h1 style={{ fontSize: 20, fontWeight: 700, color: 'var(--text)' }}>
            Switch Platform
          </h1>
          <p style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
            ISO 8583 & 20022
          </p>
        </div>

        <div style={{ padding: '0 1.5rem', marginBottom: 8, fontSize: 11, color: 'var(--text-secondary)', fontWeight: 600, letterSpacing: 1, textTransform: 'uppercase' }}>
          General
        </div>
        {mainNavItems.map(item => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              padding: '10px 1.5rem',
              color: isActive ? '#60a5fa' : 'var(--text-secondary)',
              background: isActive ? 'rgba(96,165,250,0.1)' : 'transparent',
              borderRight: isActive ? '3px solid #60a5fa' : '3px solid transparent',
              textDecoration: 'none',
              fontSize: 14,
              fontWeight: isActive ? 600 : 400,
            })}
          >
            <item.icon size={18} />
            {item.label}
          </NavLink>
        ))}

        <div style={{ padding: '0 1.5rem', marginTop: 16, marginBottom: 8, fontSize: 11, color: 'var(--text-secondary)', fontWeight: 600, letterSpacing: 1, textTransform: 'uppercase' }}>
          Modules
        </div>
        {moduleNavItems.map(item => (
          <NavLink
            key={item.to}
            to={item.to}
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              padding: '10px 1.5rem',
              color: isActive ? '#60a5fa' : 'var(--text-secondary)',
              background: isActive ? 'rgba(96,165,250,0.1)' : 'transparent',
              borderRight: isActive ? '3px solid #60a5fa' : '3px solid transparent',
              textDecoration: 'none',
              fontSize: 14,
              fontWeight: isActive ? 600 : 400,
            })}
          >
            <item.icon size={18} />
            {item.label}
          </NavLink>
        ))}
      </nav>

      <main style={{ flex: 1, padding: '1.5rem 2rem', overflow: 'auto' }}>
        {children}
      </main>
    </div>
  );
}
