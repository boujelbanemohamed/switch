import { ReactNode, useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard, Repeat, Building2, Network, GitCompare,
  CreditCard, Store, ShieldCheck, Siren, DollarSign, Settings, ShoppingCart,
  LogIn, User, Users as UsersIcon, Briefcase, Scale, Timer, GitMerge, Receipt, Layers, CreditCard as VirtualCardIcon, IdCard,
  FileText, Sliders,
} from 'lucide-react';

interface LayoutProps {
  children: ReactNode;
}

export function Layout({ children }: LayoutProps) {
  const { t, i18n } = useTranslation();
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    document.title = t('app.title');
  }, [t]);

  const role = user?.role;

  const canSeeAdmin = role === 'ADMIN' || role === 'OPERATOR';
  const canSeeAnalyst = canSeeAdmin || role === 'ANALYST';
  const isMerchant = role === 'MERCHANT';
  const isViewer = role === 'VIEWER';

  const mainNavItems = [
    { to: '/', icon: LayoutDashboard, label: t('nav.dashboard'), show: !isMerchant },
    { to: '/transactions', icon: Repeat, label: t('nav.transactions'), show: !isMerchant },
    { to: '/participants', icon: Building2, label: t('nav.participants'), show: canSeeAdmin },
    { to: '/routing', icon: GitCompare, label: t('nav.routingRules'), show: canSeeAdmin },
    { to: '/bin-tables', icon: Network, label: t('nav.binTables'), show: canSeeAdmin },
  ];

  const moduleNavItems = isMerchant ? [
    { to: '/merchant-portal', icon: Briefcase, label: t('nav.merchantPortal'), show: true },
  ] : [
    { to: '/issuing', icon: CreditCard, label: t('nav.issuing'), show: canSeeAdmin },
    { to: '/acquiring', icon: Store, label: t('nav.acquiring'), show: canSeeAdmin },
    { to: '/authorization', icon: ShieldCheck, label: t('nav.authorization'), show: canSeeAnalyst },
    { to: '/fraud', icon: Siren, label: t('nav.fraud'), show: canSeeAnalyst },
    { to: '/clearing', icon: DollarSign, label: t('nav.clearing'), show: canSeeAdmin },
    { to: '/batch', icon: Timer, label: t('nav.batch'), show: canSeeAdmin },
    { to: '/netting', icon: GitMerge, label: t('nav.netting'), show: canSeeAdmin },
    { to: '/fees', icon: Receipt, label: t('nav.fees'), show: canSeeAnalyst },
    { to: '/card-programs', icon: Layers, label: t('nav.cardPrograms'), show: canSeeAnalyst },
    { to: '/virtual-cards', icon: VirtualCardIcon, label: t('nav.virtualCards'), show: canSeeAdmin },
    { to: '/kyc', icon: IdCard, label: t('nav.kyc'), show: canSeeAnalyst },
    { to: '/backoffice', icon: Settings, label: t('nav.backoffice'), show: canSeeAdmin },
    { to: '/disputes', icon: Scale, label: t('nav.disputes'), show: canSeeAnalyst },
    { to: '/ecommerce', icon: ShoppingCart, label: t('nav.ecommerce'), show: canSeeAdmin },
    { to: '/merchant-portal', icon: Briefcase, label: t('nav.merchantPortal'), show: true },
    { to: '/profile', icon: User, label: t('nav.auth'), show: true },
    { to: '/users', icon: UsersIcon, label: t('nav.users'), show: role === 'ADMIN' },
    { to: '/reports', icon: FileText, label: t('nav.reports'), show: canSeeAdmin },
    { to: '/config-live', icon: Sliders, label: t('nav.configLive'), show: role === 'ADMIN' },
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
        {mainNavItems.filter(i => i.show).map(item => (
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
        {moduleNavItems.filter(i => i.show).map(item => (
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
          {isAuthenticated && user ? (
            <div style={{ marginBottom: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                <div style={{
                  width: 32, height: 32, borderRadius: '50%',
                  background: 'linear-gradient(135deg, #60a5fa, #3b82f6)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 12, fontWeight: 700, color: '#fff', flexShrink: 0,
                }}>
                  {(user.displayName || user.username).charAt(0).toUpperCase()}
                </div>
                <div style={{ flex: 1, overflow: 'hidden' }}>
                  <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {user.displayName || user.username}
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{user.role}</div>
                </div>
              </div>
              <button
                onClick={() => { logout(); navigate('/login'); }}
                style={{
                  width: '100%', padding: '7px 12px', borderRadius: 8, border: '1px solid var(--border)',
                  background: 'transparent', color: '#ef4444', fontSize: 12, fontWeight: 600,
                  cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, justifyContent: 'center',
                }}
              >
                <LogIn size={14} />
                {t('auth.logout')}
              </button>
            </div>
          ) : (
            <div style={{ marginBottom: 12 }}>
              <button
                onClick={() => navigate('/login')}
                style={{
                  width: '100%', padding: '8px 12px', borderRadius: 8, border: 'none',
                  background: '#2563eb', color: '#fff', fontSize: 13, fontWeight: 600,
                  cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, justifyContent: 'center',
                }}
              >
                <LogIn size={16} />
                {t('auth.login')}
              </button>
            </div>
          )}

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
