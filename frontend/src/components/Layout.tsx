import { ReactNode, useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  LayoutDashboard, Repeat, Building2, Network, GitCompare,
  CreditCard, Store, ShieldCheck, Siren, DollarSign, Settings, ShoppingCart,
} from 'lucide-react';

interface LayoutProps {
  children: ReactNode;
}

export function Layout({ children }: LayoutProps) {
  const { t, i18n } = useTranslation();

  useEffect(() => {
    document.title = t('app.title');
  }, [t]);

  const mainNavItems = [
    { to: '/', icon: LayoutDashboard, label: t('nav.dashboard') },
    { to: '/transactions', icon: Repeat, label: t('nav.transactions') },
    { to: '/participants', icon: Building2, label: t('nav.participants') },
    { to: '/routing', icon: GitCompare, label: t('nav.routingRules') },
    { to: '/bin-tables', icon: Network, label: t('nav.binTables') },
  ];

  const moduleNavItems = [
    { to: '/issuing', icon: CreditCard, label: t('nav.issuing') },
    { to: '/acquiring', icon: Store, label: t('nav.acquiring') },
    { to: '/authorization', icon: ShieldCheck, label: t('nav.authorization') },
    { to: '/fraud', icon: Siren, label: t('nav.fraud') },
    { to: '/clearing', icon: DollarSign, label: t('nav.clearing') },
    { to: '/backoffice', icon: Settings, label: t('nav.backoffice') },
    { to: '/ecommerce', icon: ShoppingCart, label: t('nav.ecommerce') },
  ];

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
          {t('nav.general')}
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
          {t('nav.modules')}
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

        <div style={{ marginTop: 'auto', padding: '1rem 1.5rem', borderTop: '1px solid var(--border)' }}>
          <label style={{ fontSize: 11, color: 'var(--text-secondary)', fontWeight: 600, marginBottom: 6, display: 'block' }}>
            {t('common.language')}
          </label>
          <select
            value={i18n.language}
            onChange={e => {
              i18n.changeLanguage(e.target.value);
              localStorage.setItem('lang', e.target.value);
            }}
            style={{
              background: 'var(--bg)',
              border: '1px solid var(--border)',
              borderRadius: 8,
              padding: '8px 12px',
              color: 'var(--text)',
              fontSize: 13,
              fontWeight: 500,
              width: '100%',
              cursor: 'pointer',
            }}
          >
            <option value="fr">Français</option>
            <option value="en">English</option>
          </select>
        </div>
      </nav>

      <main style={{ flex: 1, padding: '1.5rem 2rem', overflow: 'auto' }}>
        {children}
      </main>
    </div>
  );
}
