import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { Transaction } from '../types';
import { SectionHeader } from '../components/SectionHeader';

const statusColors: Record<string, string> = {
  PENDING: '#eab308',
  ROUTING: '#3b82f6',
  PROCESSING: '#8b5cf6',
  COMPLETED: '#22c55e',
  FAILED: '#ef4444',
  TIMEOUT: '#f97316',
  REJECTED: '#dc2626',
};

const typeLabels: Record<string, string> = {
  PURC: 'Purchase',
  PRAU: 'Pre-Auth',
  COMP: 'Completion',
  REFD: 'Refund',
  VOID: 'Void',
  REVS: 'Reversal',
  OTHR: 'Other',
};

const channelColors: Record<string, string> = {
  POS: '#3b82f6',
  ATM: '#8b5cf6',
  ECOM: '#f59e0b',
};

export function Transactions() {
  const { t } = useTranslation();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [channelFilter, setChannelFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');

  const load = () => {
    setLoading(true);
    api.transactions.list(0, 50, channelFilter || undefined, typeFilter || undefined)
      .then(res => setTransactions(res.content || []))
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  useEffect(() => { load(); }, [channelFilter, typeFilter]);

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('transactions.title')}</h2>

      <SectionHeader sectionKey="transactions" />

      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <select value={channelFilter} onChange={e => setChannelFilter(e.target.value)}
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }}>
          <option value="">{t('transactions.allChannels')}</option>
          <option value="POS">POS</option>
          <option value="ATM">ATM</option>
          <option value="ECOM">E-Commerce</option>
        </select>
        <select value={typeFilter} onChange={e => setTypeFilter(e.target.value)}
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }}>
          <option value="">{t('transactions.allTypes')}</option>
          {Object.entries(typeLabels).map(([k, v]) => (
            <option key={k} value={k}>{v}</option>
          ))}
        </select>
      </div>

      {loading ? (
        <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>
      ) : (
        <div style={{ background: 'var(--surface)', borderRadius: 12, overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                {[t('transactions.txnId'), t('transactions.type'), t('transactions.protocol'), t('transactions.amount'), t('transactions.currency'), t('transactions.channel'), t('transactions.operation'), t('transactions.status'), t('transactions.response'), t('transactions.time')].map(h => (
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
                    {tx.transactionId?.substring(0, 16)}...
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
                      {tx.messageType || t('transactions.na')}
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
                    {tx.channel ? (
                      <span style={{
                        background: `${(channelColors[tx.channel] || '#64748b')}22`,
                        color: channelColors[tx.channel] || '#64748b',
                        padding: '2px 6px',
                        borderRadius: 4,
                        fontSize: 11,
                        fontWeight: 600,
                      }}>{tx.channel}</span>
                    ) : (
                      <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>-</span>
                    )}
                  </td>
                  <td style={{ padding: '12px 16px', fontSize: 12 }}>
                    {tx.transactionType ? (typeLabels[tx.transactionType] || tx.transactionType) : '-'}
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
                    {tx.processingTimeMs ? `${tx.processingTimeMs}${t('transactions.ms')}` : '-'}
                  </td>
                </tr>
              ))}
              {transactions.length === 0 && (
                <tr>
                  <td colSpan={10} style={{ padding: 32, textAlign: 'center', color: 'var(--text-secondary)' }}>
                    {t('transactions.noTransactions')}
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
