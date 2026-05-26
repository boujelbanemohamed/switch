import { useEffect, useState } from 'react';
import { api } from '../services/api';
import type { Transaction } from '../types';

const statusColors: Record<string, string> = {
  PENDING: '#eab308',
  ROUTING: '#3b82f6',
  PROCESSING: '#8b5cf6',
  COMPLETED: '#22c55e',
  FAILED: '#ef4444',
  TIMEOUT: '#f97316',
  REJECTED: '#dc2626',
};

export function Transactions() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.transactions.list()
      .then(res => setTransactions(res.content || []))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>Transactions</h2>

      {loading ? (
        <div style={{ opacity: 0.5 }}>Loading...</div>
      ) : (
        <div style={{ background: 'var(--surface)', borderRadius: 12, overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                {['Transaction ID', 'Type', 'Protocol', 'Amount', 'Currency', 'Status', 'Response', 'Time'].map(h => (
                  <th key={h} style={{ padding: '12px 16px', fontSize: 12, color: 'var(--text-secondary)', fontWeight: 600 }}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {transactions.map(tx => (
                <tr key={tx.id} style={{ borderBottom: '1px solid var(--border)', transition: 'background 0.15s' }}
                  onMouseEnter={e => (e.currentTarget.style.background = 'var(--surface-2)')}
                  onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                >
                  <td style={{ padding: '12px 16px', fontSize: 13 }}>
                    {tx.transactionId.substring(0, 16)}...
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <span style={{
                      background: 'rgba(59,130,246,0.15)',
                      color: '#60a5fa',
                      padding: '2px 8px',
                      borderRadius: 4,
                      fontSize: 12,
                      fontWeight: 600,
                    }}>
                      {tx.messageType || 'N/A'}
                    </span>
                  </td>
                  <td style={{ padding: '12px 16px', fontSize: 13 }}>
                    {tx.protocol}
                  </td>
                  <td style={{ padding: '12px 16px', fontSize: 13 }}>
                    {tx.amount?.toLocaleString()}
                  </td>
                  <td style={{ padding: '12px 16px', fontSize: 13 }}>
                    {tx.currencyCode}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <span style={{
                      background: `${statusColors[tx.status]}22`,
                      color: statusColors[tx.status],
                      padding: '2px 8px',
                      borderRadius: 4,
                      fontSize: 12,
                      fontWeight: 600,
                    }}>
                      {tx.status}
                    </span>
                  </td>
                  <td style={{ padding: '12px 16px', fontSize: 13 }}>
                    {tx.responseCode || '-'}
                  </td>
                  <td style={{ padding: '12px 16px', fontSize: 12, color: 'var(--text-secondary)' }}>
                    {tx.processingTimeMs ? `${tx.processingTimeMs}ms` : '-'}
                  </td>
                </tr>
              ))}
              {transactions.length === 0 && (
                <tr>
                  <td colSpan={8} style={{ padding: 32, textAlign: 'center', color: 'var(--text-secondary)' }}>
                    No transactions yet
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
