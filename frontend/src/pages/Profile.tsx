import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import { SectionHeader } from '../components/SectionHeader';
import { User, Shield, Mail, Calendar, Clock } from 'lucide-react';

export function Profile() {
  const { t } = useTranslation();
  const { user, logout } = useAuth();

  if (!user) return null;

  const roleColors: Record<string, string> = {
    ADMIN: '#ef4444',
    OPERATOR: '#f59e0b',
    ANALYST: '#3b82f6',
    AUDITOR: '#8b5cf6',
    VIEWER: '#6b7280',
  };

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('auth.title')}</h2>
      <SectionHeader sectionKey="auth" />

      <div style={{ maxWidth: 640, marginTop: 24 }}>
        <div style={{
          background: 'var(--surface)', borderRadius: 12, border: '1px solid var(--border)', overflow: 'hidden',
        }}>
          <div style={{
            padding: '2rem', display: 'flex', alignItems: 'center', gap: 20,
            borderBottom: '1px solid var(--border)',
          }}>
            <div style={{
              width: 64, height: 64, borderRadius: '50%',
              background: 'linear-gradient(135deg, #60a5fa, #3b82f6)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 24,
              fontWeight: 700, color: '#fff',
            }}>
              {(user.displayName || user.username).charAt(0).toUpperCase()}
            </div>
            <div>
              <h2 style={{ fontSize: 18, fontWeight: 600, color: 'var(--text)', marginBottom: 4 }}>
                {user.displayName || user.username}
              </h2>
              <p style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{user.username}</p>
            </div>
          </div>

          <div style={{ padding: '1.5rem 2rem' }}>
            <Row icon={User} label={t('auth.username')} value={user.username} />
            <Row icon={Mail} label={t('auth.email')} value={user.email || '-'} />
            <Row icon={Shield} label={t('auth.role')} value={
              <span style={{
                display: 'inline-block', padding: '3px 10px', borderRadius: 6,
                fontSize: 12, fontWeight: 600,
                background: `${roleColors[user.role] || '#6b7280'}20`,
                color: roleColors[user.role] || '#6b7280',
              }}>
                {user.role}
              </span>
            } />
            <Row icon={Calendar} label={t('backoffice.createdAt')} value={user.createdAt || '-'} />
            <Row icon={Clock} label={t('auth.lastLogin')} value={user.lastLogin || '-'} />
          </div>
        </div>

        <div style={{ marginTop: 24, display: 'flex', gap: 12 }}>
          <button
            onClick={logout}
            style={{
              padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
              background: 'var(--surface)', color: '#ef4444', fontSize: 13, fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            {t('auth.logout')}
          </button>
        </div>
      </div>
    </div>
  );
}

function Row({ icon: Icon, label, value }: { icon: React.ComponentType<{ size?: number; className?: string }>; label: string; value: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
      <Icon size={16} />
      <span style={{ fontSize: 13, color: 'var(--text-secondary)', minWidth: 120 }}>{label}</span>
      <span style={{ fontSize: 14, color: 'var(--text)', fontWeight: 500 }}>{value}</span>
    </div>
  );
}
