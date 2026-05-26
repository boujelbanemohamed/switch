import { useEffect, useState } from 'react';
import { api } from '../services/api';
import type { BinTable } from '../types';

const brandColors: Record<string, string> = {
  VISA: '#1a1f71',
  MASTERCARD: '#eb001b',
  AMEX: '#2e77bc',
  CB: '#0066b3',
  OTHER: '#64748b',
};

export function BinTables() {
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
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>BIN Tables</h2>

      {loading ? (
        <div style={{ opacity: 0.5 }}>Loading...</div>
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
                  <span style={{ color: 'var(--text-secondary)' }}>Length: </span>
                  {bt.binLength}
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>Type: </span>
                  {bt.cardType || '-'}
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>Country: </span>
                  {bt.countryCode || '-'}
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>Currency: </span>
                  {bt.currencyCode || '-'}
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
