import { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Repeat, Building2, Network, GitCompare } from 'lucide-react';

interface LayoutProps {
  children: ReactNode;
}

const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/transactions', icon: Repeat, label: 'Transactions' },
  { to: '/participants', icon: Building2, label: 'Participants' },
  { to: '/routing', icon: GitCompare, label: 'Routing Rules' },
  { to: '/bin-tables', icon: Network, label: 'BIN Tables' },
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

        {navItems.map(item => (
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
      </nav>

      <main style={{ flex: 1, padding: '1.5rem 2rem', overflow: 'auto' }}>
        {children}
      </main>
    </div>
  );
}
