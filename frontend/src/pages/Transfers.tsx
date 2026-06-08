import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { Transfer, TransferLimit } from '../types';

export function Transfers() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<'a2a' | 'p2p' | 'history' | 'limits'>('history');
  const [transfers, setTransfers] = useState<Transfer[]>([]);
  const [limits, setLimits] = useState<TransferLimit[]>([]);
  const [loading, setLoading] = useState(false);

  // A2A form
  const [a2aSource, setA2aSource] = useState('');
  const [a2aDest, setA2aDest] = useState('');
  const [a2aAmount, setA2aAmount] = useState('');
  const [a2aCurrency, setA2aCurrency] = useState('TND');

  // P2P form
  const [p2pSource, setP2pSource] = useState('');
  const [p2pDest, setP2pDest] = useState('');
  const [p2pAmount, setP2pAmount] = useState('');
  const [p2pCurrency, setP2pCurrency] = useState('TND');

  // Reverse
  const [reverseId, setReverseId] = useState('');
  const [reverseReason, setReverseReason] = useState('');

  useEffect(() => {
    loadTransfers();
    loadLimits();
  }, []);

  async function loadTransfers() {
    try {
      const data = await api.transfers.list();
      setTransfers(data);
    } catch { /* ignore */ }
  }

  async function loadLimits() {
    try {
      const data = await api.transfers.limits.list();
      setLimits(data);
    } catch { /* ignore */ }
  }

  async function handleA2A() {
    if (!a2aSource || !a2aDest || !a2aAmount) return;
    setLoading(true);
    try {
      await api.transfers.executeA2A({
        sourceAccountId: a2aSource,
        destinationAccountId: a2aDest,
        amount: Number(a2aAmount),
        currencyCode: a2aCurrency,
        channel: 'BACKOFFICE',
      });
      loadTransfers();
      setA2aAmount('');
    } catch (e: any) { alert(e.message); }
    setLoading(false);
  }

  async function handleP2P() {
    if (!p2pSource || !p2pDest || !p2pAmount) return;
    setLoading(true);
    try {
      await api.transfers.executeP2P({
        sourcePan: p2pSource,
        destinationRef: p2pDest,
        amount: Number(p2pAmount),
        currencyCode: p2pCurrency,
        channel: 'BACKOFFICE',
      });
      loadTransfers();
      setP2pAmount('');
    } catch (e: any) { alert(e.message); }
    setLoading(false);
  }

  async function handleReverse(id: string) {
    const reason = prompt(t('transfers.reasonPlaceholder'));
    if (!reason) return;
    setLoading(true);
    try {
      await api.transfers.reverse(id, reason);
      loadTransfers();
    } catch (e: any) { alert(e.message); }
    setLoading(false);
  }

  const btnStyle: React.CSSProperties = {
    padding: '10px 20px', borderRadius: 8, border: 'none',
    background: '#2563eb', color: '#fff', fontSize: 14, fontWeight: 600,
    cursor: 'pointer',
  };

  const inputStyle: React.CSSProperties = {
    width: '100%', padding: '10px 12px', borderRadius: 8, border: '1px solid var(--border)',
    background: 'var(--bg)', color: 'var(--text)', fontSize: 14,
    boxSizing: 'border-box',
  };

  const labelStyle: React.CSSProperties = {
    fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 4, display: 'block',
  };

  return (
    <div>
      <h2 style={{ fontSize: 22, fontWeight: 700, marginBottom: 24 }}>{t('transfers.title')}</h2>

      <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
        {(['history', 'a2a', 'p2p', 'limits'] as const).map(tabKey => (
          <button key={tabKey} onClick={() => setTab(tabKey)}
            style={{
              padding: '8px 18px', borderRadius: 20, border: '1px solid var(--border)',
              background: tab === tabKey ? '#2563eb' : 'transparent',
              color: tab === tabKey ? '#fff' : 'var(--text-secondary)',
              fontSize: 13, fontWeight: 600, cursor: 'pointer',
            }}
          >{t('transfers.' + tabKey)}</button>
        ))}
      </div>

      {/* History */}
      {tab === 'history' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, border: '1px solid var(--border)', overflow: 'hidden' }}>
          <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)', fontWeight: 600, fontSize: 15 }}>
            {t('transfers.history')}
          </div>
          {transfers.length === 0 ? (
            <div style={{ padding: 40, textAlign: 'center', color: 'var(--text-secondary)', fontSize: 14 }}>
              {t('transfers.noTransfers')}
            </div>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ fontSize: 12, color: 'var(--text-secondary)', textAlign: 'left' }}>
                  <th style={{ padding: '10px 16px' }}>{t('transfers.date')}</th>
                  <th style={{ padding: '10px 16px' }}>{t('transfers.transferType')}</th>
                  <th style={{ padding: '10px 16px' }}>{t('transfers.amount')}</th>
                  <th style={{ padding: '10px 16px' }}>{t('transfers.fee')}</th>
                  <th style={{ padding: '10px 16px' }}>{t('transfers.status')}</th>
                  <th style={{ padding: '10px 16px' }}></th>
                </tr>
              </thead>
              <tbody>
                {transfers.map(tx => (
                  <tr key={tx.id} style={{ borderTop: '1px solid var(--border)', fontSize: 14 }}>
                    <td style={{ padding: '10px 16px' }}>{new Date(tx.createdAt).toLocaleString()}</td>
                    <td style={{ padding: '10px 16px' }}>{tx.transferType}</td>
                    <td style={{ padding: '10px 16px' }}>{tx.amount} {tx.currencyCode}</td>
                    <td style={{ padding: '10px 16px' }}>{tx.feeAmount}</td>
                    <td style={{ padding: '10px 16px' }}>
                      <span style={{
                        padding: '3px 10px', borderRadius: 10, fontSize: 12, fontWeight: 600,
                        background: tx.status === 'COMPLETED' ? '#065f4620' : tx.status === 'REVERSED' ? '#b91c1c20' : '#d9770620',
                        color: tx.status === 'COMPLETED' ? '#34d399' : tx.status === 'REVERSED' ? '#ef4444' : '#f59e0b',
                      }}>
                        {t('transfers.' + tx.status.toLowerCase())}
                      </span>
                    </td>
                    <td style={{ padding: '10px 16px' }}>
                      {tx.status === 'COMPLETED' && !tx.originalTransferId && (
                        <button onClick={() => handleReverse(tx.id)} style={{
                          padding: '5px 12px', borderRadius: 6, border: '1px solid var(--border)',
                          background: 'transparent', color: '#ef4444', fontSize: 12, cursor: 'pointer',
                        }}>{t('transfers.reverse')}</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {/* A2A */}
      {tab === 'a2a' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, border: '1px solid var(--border)', padding: 24, maxWidth: 500 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 20 }}>{t('transfers.a2a')}</h3>
          <div style={{ marginBottom: 16 }}>
            <label style={labelStyle}>{t('transfers.sourceAccount')}</label>
            <input style={inputStyle} value={a2aSource} onChange={e => setA2aSource(e.target.value)} placeholder="UUID" />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label style={labelStyle}>{t('transfers.destAccount')}</label>
            <input style={inputStyle} value={a2aDest} onChange={e => setA2aDest(e.target.value)} placeholder="UUID" />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label style={labelStyle}>{t('transfers.amount')}</label>
            <input style={inputStyle} type="number" value={a2aAmount} onChange={e => setA2aAmount(e.target.value)} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label style={labelStyle}>{t('transfers.currency')}</label>
            <select style={inputStyle} value={a2aCurrency} onChange={e => setA2aCurrency(e.target.value)}>
              <option value="TND">TND</option>
              <option value="EUR">EUR</option>
            </select>
          </div>
          <button style={btnStyle} onClick={handleA2A} disabled={loading}>{t('transfers.execute')}</button>
        </div>
      )}

      {/* P2P */}
      {tab === 'p2p' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, border: '1px solid var(--border)', padding: 24, maxWidth: 500 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 20 }}>{t('transfers.p2p')}</h3>
          <div style={{ marginBottom: 16 }}>
            <label style={labelStyle}>{t('transfers.sourcePan')}</label>
            <input style={inputStyle} value={p2pSource} onChange={e => setP2pSource(e.target.value)} placeholder="Card suffix (last 8 digits)" />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label style={labelStyle}>{t('transfers.destRef')}</label>
            <input style={inputStyle} value={p2pDest} onChange={e => setP2pDest(e.target.value)} placeholder="UUID" />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label style={labelStyle}>{t('transfers.amount')}</label>
            <input style={inputStyle} type="number" value={p2pAmount} onChange={e => setP2pAmount(e.target.value)} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label style={labelStyle}>{t('transfers.currency')}</label>
            <select style={inputStyle} value={p2pCurrency} onChange={e => setP2pCurrency(e.target.value)}>
              <option value="TND">TND</option>
              <option value="EUR">EUR</option>
            </select>
          </div>
          <button style={btnStyle} onClick={handleP2P} disabled={loading}>{t('transfers.execute')}</button>
        </div>
      )}

      {/* Limits */}
      {tab === 'limits' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, border: '1px solid var(--border)', overflow: 'hidden' }}>
          <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)', fontWeight: 600, fontSize: 15 }}>
            {t('transfers.limits')}
          </div>
          {limits.length === 0 ? (
            <div style={{ padding: 40, textAlign: 'center', color: 'var(--text-secondary)', fontSize: 14 }}>
              {t('transfers.noTransfers')}
            </div>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ fontSize: 12, color: 'var(--text-secondary)', textAlign: 'left' }}>
                  <th style={{ padding: '10px 16px' }}>{t('transfers.transferType')}</th>
                  <th style={{ padding: '10px 16px' }}>{t('transfers.perTransferMax')}</th>
                  <th style={{ padding: '10px 16px' }}>{t('transfers.dailyMaxAmount')}</th>
                  <th style={{ padding: '10px 16px' }}>{t('transfers.dailyMaxCount')}</th>
                </tr>
              </thead>
              <tbody>
                {limits.map(l => (
                  <tr key={l.id} style={{ borderTop: '1px solid var(--border)', fontSize: 14 }}>
                    <td style={{ padding: '10px 16px' }}>{l.transferType}</td>
                    <td style={{ padding: '10px 16px' }}>{l.perTransferMax} {l.currencyCode}</td>
                    <td style={{ padding: '10px 16px' }}>{l.dailyMaxAmount} {l.currencyCode}</td>
                    <td style={{ padding: '10px 16px' }}>{l.dailyMaxCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
