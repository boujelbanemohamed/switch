import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { Merchant } from '../types';
import { SectionHeader } from '../components/SectionHeader';

const statusColors: Record<string, string> = {
  ACTIVE: '#22c55e',
  PENDING_APPROVAL: '#eab308',
  SUSPENDED: '#f97316',
  TERMINATED: '#ef4444',
};

export function Acquiring() {
  const { t } = useTranslation();
  const [merchants, setMerchants] = useState<Merchant[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.acquiring.merchants.list()
      .then(setMerchants)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('acquiring.title')}</h2>

      <SectionHeader sectionKey="acquiring" />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <StatCard title={t('acquiring.totalMerchants')} value={merchants.length.toLocaleString()} />
        <StatCard title={t('acquiring.activeTerminals')} value={merchants.filter(m => m.status === 'ACTIVE').length.toLocaleString()} />
        <StatCard title={t('acquiring.pendingSettlements')} value={merchants.filter(m => m.status === 'PENDING_APPROVAL').length.toLocaleString()} />
      </div>

      <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
        <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('acquiring.merchants')}</h3>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.code')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.name')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.category')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.country')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.status')}</th>
              </tr>
            </thead>
            <tbody>
              {merchants.map(m => (
                <tr key={m.id} style={{ borderBottom: '1px solid var(--border)' }}>
                  <td style={{ padding: '10px 12px', fontFamily: 'monospace', fontWeight: 600 }}>{m.code}</td>
                  <td style={{ padding: '10px 12px' }}>{m.name}</td>
                  <td style={{ padding: '10px 12px', color: 'var(--text-secondary)' }}>{m.categoryCode || '-'}</td>
                  <td style={{ padding: '10px 12px' }}>{m.countryCode || '-'}</td>
                  <td style={{ padding: '10px 12px' }}>
                    <span style={{
                      background: `${(statusColors[m.status] || '#64748b')}33`,
                      color: statusColors[m.status] || '#64748b',
                      padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                    }}>
                      {m.status}
                    </span>
                  </td>
                </tr>
              ))}
              {merchants.length === 0 && (
                <tr>
                  <td colSpan={5} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>
                    {t('acquiring.noMerchants')}
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
