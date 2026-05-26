import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { AuthRule } from '../types';

export function Authorization() {
  const { t } = useTranslation();
  const [rules, setRules] = useState<AuthRule[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.authorization.rules.list()
      .then(setRules)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('authorization.title')}</h2>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        <StatCard title={t('authorization.totalRules')} value={rules.length.toLocaleString()} />
        <StatCard title={t('authorization.activeRules')} value={rules.filter(r => r.status === 'ACTIVE').length.toLocaleString()} />
        <StatCard title={t('authorization.successRate')} value={
          rules.length > 0
            ? `${Math.round((rules.reduce((s, r) => s + r.successCount, 0) / Math.max(1,
                rules.reduce((s, r) => s + r.successCount + r.failureCount, 0))) * 100)}%`
            : 'N/A'
        } />
        <StatCard title={t('authorization.totalDecisions')} value={rules.reduce((s, r) => s + r.successCount + r.failureCount, 0).toLocaleString()} />
      </div>

      <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
        <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('authorization.rules')}</h3>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.name')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.type')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.priority')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.status')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.success')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.failures')}</th>
              </tr>
            </thead>
            <tbody>
              {rules.map(r => (
                <tr key={r.id} style={{ borderBottom: '1px solid var(--border)' }}>
                  <td style={{ padding: '10px 12px', fontWeight: 600 }}>{r.name}</td>
                  <td style={{ padding: '10px 12px', color: 'var(--text-secondary)' }}>{r.ruleType}</td>
                  <td style={{ padding: '10px 12px', fontFamily: 'monospace' }}>{r.priority}</td>
                  <td style={{ padding: '10px 12px' }}>
                    <span style={{
                      background: r.status === 'ACTIVE' ? '#22c55e33' : '#64748b33',
                      color: r.status === 'ACTIVE' ? '#22c55e' : '#64748b',
                      padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                    }}>
                      {r.status}
                    </span>
                  </td>
                  <td style={{ padding: '10px 12px', color: '#22c55e' }}>{r.successCount}</td>
                  <td style={{ padding: '10px 12px', color: '#ef4444' }}>{r.failureCount}</td>
                </tr>
              ))}
              {rules.length === 0 && (
                <tr>
                  <td colSpan={6} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>
                    {t('authorization.noRules')}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function StatCard({ title, value }: { title: string; value: string }) {
  return (
    <div style={{
      background: 'var(--surface)',
      borderRadius: 12,
      padding: '16px 20px',
      border: '1px solid var(--border)',
    }}>
      <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 8 }}>{title}</p>
      <p style={{ fontSize: 28, fontWeight: 700 }}>{value}</p>
    </div>
  );
}
