import { useEffect, useState } from 'react';
import { api } from '../services/api';
import type { Cardholder } from '../types';

export function Issuing() {
  const [cardholders, setCardholders] = useState<Cardholder[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.issuing.cardholders.list()
      .then(setCardholders)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div style={{ opacity: 0.5 }}>Loading...</div>;

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>Issuing</h2>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <StatCard title="Total Cardholders" value={cardholders.length.toLocaleString()} />
        <StatCard title="Active Cards" value={cardholders.filter(c => c.status === 'ACTIVE').length.toLocaleString()} />
        <StatCard title="KYC Level 3+" value={cardholders.filter(c => c.kycLevel >= 3).length.toLocaleString()} />
      </div>

      <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
        <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>Cardholders</h3>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>Name</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>Email</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>KYC Level</th>
              </tr>
            </thead>
            <tbody>
              {cardholders.map(ch => (
                <tr key={ch.id} style={{ borderBottom: '1px solid var(--border)' }}>
                  <td style={{ padding: '10px 12px' }}>{ch.firstName} {ch.lastName}</td>
                  <td style={{ padding: '10px 12px', color: 'var(--text-secondary)' }}>{ch.email}</td>
                  <td style={{ padding: '10px 12px' }}>
                    <span style={{
                      background: ch.status === 'ACTIVE' ? '#22c55e33' : ch.status === 'BLOCKED' ? '#ef444433' : '#eab30833',
                      color: ch.status === 'ACTIVE' ? '#22c55e' : ch.status === 'BLOCKED' ? '#ef4444' : '#eab308',
                      padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                    }}>
                      {ch.status}
                    </span>
                  </td>
                  <td style={{ padding: '10px 12px', fontFamily: 'monospace' }}>{ch.kycLevel}</td>
                </tr>
              ))}
              {cardholders.length === 0 && (
                <tr>
                  <td colSpan={4} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>
                    No cardholders found
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
