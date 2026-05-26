import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { BinTable } from '../types';
import { SectionHeader } from '../components/SectionHeader';

const brandColors: Record<string, string> = {
  VISA: '#1a1f71',
  MASTERCARD: '#eb001b',
  AMEX: '#2e77bc',
  CB: '#0066b3',
  OTHER: '#64748b',
};

export function BinTables() {
  const { t } = useTranslation();
  const [tables, setTables] = useState<BinTable[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.binTables.list()
      .then(setTables)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('nav.binTables')}</h2>

      <SectionHeader sectionKey="binTables" />

      {loading ? (
        <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 16 }}>
          {tables.map(bt => (
            <div key={bt.id} style={{
              background: 'var(--surface)',
              borderRadius: 12,
              padding: 20,
              border: '1px solid var(--border)',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
                <span style={{ fontSize: 18, fontWeight: 700, fontFamily: 'monospace' }}>
                  {bt.bin}{'*'.repeat(16 - bt.binLength)}
                </span>
                {bt.cardBrand && (
                  <span style={{
                    background: `${brandColors[bt.cardBrand]}33`,
                    color: brandColors[bt.cardBrand],
                    padding: '2px 8px',
                    borderRadius: 4,
                    fontSize: 11,
                    fontWeight: 600,
                  }}>
                    {bt.cardBrand}
                  </span>
                )}
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, fontSize: 13 }}>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>{t('binTables.length')}: </span>
                  {bt.binLength}
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>{t('binTables.type')}: </span>
                  {bt.cardType || t('binTables.na')}
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>{t('binTables.country')}: </span>
                  {bt.countryCode || t('binTables.na')}
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>{t('binTables.currency')}: </span>
                  {bt.currencyCode || t('binTables.na')}
                </div>
              </div>

              <div style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid var(--border)', fontSize: 12, color: 'var(--text-secondary)' }}>
                {bt.participant?.name || bt.participant?.code}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
