import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { FraudAlert } from '../types';

const alertColors: Record<string, string> = {
  OPEN: '#ef4444',
  INVESTIGATING: '#eab308',
  CONFIRMED: '#f97316',
  DISMISSED: '#64748b',
};

export function Fraud() {
  const { t } = useTranslation();
  const [alerts, setAlerts] = useState<FraudAlert[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.fraud.alerts.list()
      .then(setAlerts)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const openAlerts = alerts.filter(a => a.status === 'OPEN');
  const confirmedAlerts = alerts.filter(a => a.status === 'CONFIRMED');

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('fraud.title')}</h2>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        <StatCard title={t('fraud.totalAlerts')} value={alerts.length.toLocaleString()} />
        <StatCard title={t('fraud.open')} value={openAlerts.length.toLocaleString()} />
        <StatCard title={t('fraud.confirmed')} value={confirmedAlerts.length.toLocaleString()} />
        <StatCard title={t('fraud.highScore')} value={alerts.filter(a => a.score > 80).length.toLocaleString()} />
      </div>

      <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
        <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('fraud.alerts')}</h3>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('fraud.rule')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>Transaction</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('fraud.score')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('fraud.status')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('fraud.decision')}</th>
              </tr>
            </thead>
            <tbody>
              {alerts.map(a => (
                <tr key={a.id} style={{ borderBottom: '1px solid var(--border)' }}>
                  <td style={{ padding: '10px 12px', fontWeight: 600 }}>{a.ruleName}</td>
                  <td style={{ padding: '10px 12px', fontFamily: 'monospace', fontSize: 12, color: 'var(--text-secondary)' }}>
                    {a.transactionId?.substring(0, 12)}...
                  </td>
                  <td style={{ padding: '10px 12px' }}>
                    <span style={{
                      color: a.score > 80 ? '#ef4444' : a.score > 50 ? '#eab308' : '#22c55e',
                      fontWeight: 700,
                    }}>
                      {Math.round(a.score)}
                    </span>
                  </td>
                  <td style={{ padding: '10px 12px' }}>
                    <span style={{
                      background: `${(alertColors[a.status] || '#64748b')}33`,
                      color: alertColors[a.status] || '#64748b',
                      padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                    }}>
                      {a.status}
                    </span>
                  </td>
                  <td style={{ padding: '10px 12px', color: 'var(--text-secondary)' }}>{a.decision || '-'}</td>
                </tr>
              ))}
              {alerts.length === 0 && (
                <tr>
                  <td colSpan={5} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>
                    {t('fraud.noAlerts')}
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
