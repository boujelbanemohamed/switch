import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import { SectionHeader } from '../components/SectionHeader';
import { User, Shield, Mail, Calendar, Clock, Key, Check, X, Copy, CheckCircle } from 'lucide-react';

export function Profile() {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const [qrCode, setQrCode] = useState<string | null>(null);
  const [secret, setSecret] = useState<string | null>(null);
  const [mfaCode, setMfaCode] = useState('');
  const [mfaError, setMfaError] = useState('');
  const [mfaEnabled, setMfaEnabled] = useState(user?.mfaEnabled || false);
  const [copied, setCopied] = useState(false);

  if (!user) return null;

  const roleColors: Record<string, string> = {
    ADMIN: '#ef4444',
    OPERATOR: '#f59e0b',
    ANALYST: '#3b82f6',
    AUDITOR: '#8b5cf6',
    VIEWER: '#6b7280',
  };

  const handleSetupMfa = async () => {
    try {
      const res = await fetch('/api/v1/auth/mfa/setup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user.username }),
      });
      if (!res.ok) throw new Error('Setup failed');
      const data = await res.json();
      setSecret(data.secret);
      setQrCode(data.uri);
      setMfaError('');
    } catch (err) {
      setMfaError(err instanceof Error ? err.message : 'Failed to setup MFA');
    }
  };

  const handleVerifyMfa = async () => {
    try {
      const res = await fetch('/api/v1/auth/mfa/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user.username, code: mfaCode }),
      });
      if (!res.ok) throw new Error('Invalid code');
      const data = await res.json();
      if (data.enabled) {
        setMfaEnabled(true);
        setMfaCode('');
        setMfaError('');
        setQrCode(null);
        setSecret(null);
      }
    } catch (err) {
      setMfaError(err instanceof Error ? err.message : 'Invalid verification code');
    }
  };

  const handleDisableMfa = async () => {
    try {
      const res = await fetch('/api/v1/auth/mfa/disable', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user.username, code: mfaCode }),
      });
      if (!res.ok) throw new Error('Invalid code');
      const data = await res.json();
      if (data.disabled) {
        setMfaEnabled(false);
        setMfaCode('');
        setMfaError('');
      }
    } catch (err) {
      setMfaError(err instanceof Error ? err.message : 'Failed to disable MFA');
    }
  };

  const copySecret = () => {
    if (secret) {
      navigator.clipboard.writeText(secret);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
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

        <div style={{
          marginTop: 24, background: 'var(--surface)', borderRadius: 12,
          border: '1px solid var(--border)', overflow: 'hidden', padding: '1.5rem 2rem',
        }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Key size={18} />
            {t('auth.mfaTitle')}
          </h3>

          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
            <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{t('auth.mfaStatus')}:</span>
            {mfaEnabled ? (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, fontSize: 13, fontWeight: 600, color: '#22c55e' }}>
                <CheckCircle size={14} /> {t('auth.mfaEnabled')}
              </span>
            ) : (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, fontSize: 13, fontWeight: 600, color: '#6b7280' }}>
                <X size={14} /> {t('auth.mfaDisabled')}
              </span>
            )}
          </div>

          {mfaError && (
            <div style={{ color: '#ef4444', fontSize: 13, marginBottom: 12 }}>{mfaError}</div>
          )}

          {!mfaEnabled && !qrCode && (
            <button onClick={handleSetupMfa} style={{
              padding: '10px 20px', borderRadius: 8, border: 'none',
              background: '#6d28d9', color: '#fff', fontSize: 13, fontWeight: 600, cursor: 'pointer',
            }}>
              {t('auth.setupMfa')}
            </button>
          )}

          {qrCode && (
            <div style={{ marginTop: 16 }}>
              <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 12 }}>
                {t('auth.mfaInstructions')}
              </p>
              <div style={{
                background: 'var(--bg)', borderRadius: 8, padding: 16, marginBottom: 12,
                display: 'flex', alignItems: 'center', gap: 12,
              }}>
                <code style={{ fontSize: 13, flex: 1, wordBreak: 'break-all', color: 'var(--text)' }}>{secret}</code>
                <button onClick={copySecret} style={{
                  padding: '6px 12px', borderRadius: 6, border: '1px solid var(--border)',
                  background: 'var(--surface)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4,
                  fontSize: 12, color: 'var(--text-secondary)',
                }}>
                  <Copy size={14} /> {copied ? t('common.copied') : t('common.copy')}
                </button>
              </div>

              <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
                <input
                  value={mfaCode}
                  onChange={e => setMfaCode(e.target.value)}
                  placeholder="000000"
                  maxLength={6}
                  style={{
                    width: 140, padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)',
                    background: 'var(--bg)', color: 'var(--text)', fontSize: 16, textAlign: 'center',
                    letterSpacing: 4, fontWeight: 700, outline: 'none',
                  }}
                />
                <button onClick={handleVerifyMfa} style={{
                  padding: '10px 20px', borderRadius: 8, border: 'none',
                  background: '#22c55e', color: '#fff', fontSize: 13, fontWeight: 600, cursor: 'pointer',
                  display: 'flex', alignItems: 'center', gap: 6,
                }}>
                  <Check size={14} /> {t('auth.verify')}
                </button>
              </div>
            </div>
          )}

          {mfaEnabled && (
            <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
              <input
                value={mfaCode}
                onChange={e => setMfaCode(e.target.value)}
                placeholder="000000"
                maxLength={6}
                style={{
                  width: 140, padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)',
                  background: 'var(--bg)', color: 'var(--text)', fontSize: 16, textAlign: 'center',
                  letterSpacing: 4, fontWeight: 700, outline: 'none',
                }}
              />
              <button onClick={handleDisableMfa} style={{
                padding: '10px 20px', borderRadius: 8, border: 'none',
                background: '#ef4444', color: '#fff', fontSize: 13, fontWeight: 600, cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: 6,
              }}>
                <X size={14} /> {t('auth.disableMfa')}
              </button>
            </div>
          )}
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
