import { useState, useCallback, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import { SectionHeader } from '../components/SectionHeader';
import type { Cardholder, Card, Merchant, Terminal, PosResult } from '../types';

type EntryMode = 'CHIP' | 'CONTACTLESS';

const DECISION_COLORS: Record<string, string> = {
  APPROVED: '#22c55e',
  DECLINED: '#ef4444',
  CHALLENGED: '#f59e0b',
  ERROR: '#64748b',
};

const DECISION_LABELS: Record<string, string> = {
  APPROVED: 'Approuvé',
  DECLINED: 'Refusé',
  CHALLENGED: 'Défi',
  ERROR: 'Erreur',
};

export function PosSimulator() {
  const { t } = useTranslation();

  const [cardholders, setCardholders] = useState<Cardholder[]>([]);
  const [cards, setCards] = useState<Card[]>([]);
  const [merchants, setMerchants] = useState<Merchant[]>([]);
  const [terminals, setTerminals] = useState<Terminal[]>([]);

  const [cardholderId, setCardholderId] = useState('');
  const [cardId, setCardId] = useState('');
  const [merchantId, setMerchantId] = useState('');
  const [terminalId, setTerminalId] = useState('');
  const [amount, setAmount] = useState('250.00');
  const [currencyCode, setCurrencyCode] = useState('788');
  const [countryCode, setCountryCode] = useState('788');
  const [entryMode, setEntryMode] = useState<EntryMode>('CHIP');
  const [pinVerified, setPinVerified] = useState(true);

  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<PosResult | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.issuing.cardholders.list().then(setCardholders).catch(console.error);
    api.acquiring.merchants.list().then(setMerchants).catch(console.error);
  }, []);

  useEffect(() => {
    if (cardholderId) {
      api.issuing.cards.listByCardholder(cardholderId).then(setCards).catch(console.error);
    } else {
      setCards([]);
    }
    setCardId('');
  }, [cardholderId]);

  useEffect(() => {
    if (merchantId) {
      api.acquiring.terminals.listByMerchant(merchantId).then(setTerminals).catch(console.error);
    } else {
      setTerminals([]);
    }
    setTerminalId('');
  }, [merchantId]);

  const handleSubmit = useCallback(async () => {
    setLoading(true);
    setError('');
    setResult(null);
    try {
      const data = await api.post('/simulator/pos/transaction', {
        cardId,
        merchantId,
        terminalId,
        amount: parseFloat(amount),
        currencyCode,
        countryCode,
        entryMode,
        pinVerified,
      });
      setResult(data as unknown as PosResult);
    } catch (e) {
      setError((e as Error).message || t('common.failedToLoad'));
    }
    setLoading(false);
  }, [cardId, merchantId, terminalId, amount, currencyCode, countryCode, entryMode, pinVerified, t]);

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 }}>
        <h2 style={{ fontSize: 24, fontWeight: 700 }}>{t('posSimulator.title')}</h2>
      </div>
      <SectionHeader sectionKey="posSimulator" />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('posSimulator.formTitle')}</h3>
          <div style={{ display: 'grid', gap: 14 }}>
            <Field label={t('posSimulator.cardholder')}>
              <select value={cardholderId} onChange={e => setCardholderId(e.target.value)}
                style={selectStyle}>
                <option value="">-- {t('common.select')} --</option>
                {cardholders.map(ch => (
                  <option key={ch.id} value={ch.id}>
                    {ch.firstName} {ch.lastName} ({ch.email})
                  </option>
                ))}
              </select>
            </Field>

            <Field label={t('posSimulator.card')}>
              <select value={cardId} onChange={e => setCardId(e.target.value)}
                style={selectStyle} disabled={!cardholderId}>
                <option value="">-- {t('common.select')} --</option>
                {cards.map(c => (
                  <option key={c.id} value={c.id}>
                    ****{c.panSuffix} ({c.cardBrand} - {c.cardType})
                  </option>
                ))}
              </select>
            </Field>

            <Field label={t('posSimulator.merchant')}>
              <select value={merchantId} onChange={e => setMerchantId(e.target.value)}
                style={selectStyle}>
                <option value="">-- {t('common.select')} --</option>
                {merchants.map(m => (
                  <option key={m.id} value={m.id}>
                    {m.name} ({m.merchantId || m.id.slice(0, 8)})
                  </option>
                ))}
              </select>
            </Field>

            <Field label={t('posSimulator.terminal')}>
              <select value={terminalId} onChange={e => setTerminalId(e.target.value)}
                style={selectStyle} disabled={!merchantId}>
                <option value="">-- {t('common.select')} --</option>
                {terminals.map(tm => (
                  <option key={tm.id} value={tm.id}>
                    {tm.terminalId} ({tm.model || tm.type})
                  </option>
                ))}
              </select>
            </Field>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <Field label={t('posSimulator.amount')}>
                <input type="number" step="0.01" value={amount}
                  onChange={e => setAmount(e.target.value)} style={inputStyle} />
              </Field>
              <Field label={t('posSimulator.currency')}>
                <input value={currencyCode} onChange={e => setCurrencyCode(e.target.value)}
                  style={inputStyle} maxLength={3} />
              </Field>
            </div>

            <Field label={t('posSimulator.countryCode')}>
              <input value={countryCode} onChange={e => setCountryCode(e.target.value)}
                style={inputStyle} maxLength={3} />
            </Field>

            <Field label={t('posSimulator.entryMode')}>
              <select value={entryMode} onChange={e => setEntryMode(e.target.value as EntryMode)}
                style={selectStyle}>
                <option value="CHIP">{t('posSimulator.chip')}</option>
                <option value="CONTACTLESS">{t('posSimulator.contactless')}</option>
              </select>
            </Field>

            {entryMode === 'CHIP' && (
              <Field label={t('posSimulator.pinVerified')}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                  <input type="checkbox" checked={pinVerified}
                    onChange={e => setPinVerified(e.target.checked)}
                    style={{ width: 18, height: 18, cursor: 'pointer' }} />
                  <span style={{ fontSize: 13, color: pinVerified ? '#22c55e' : '#ef4444' }}>
                    {pinVerified ? t('posSimulator.pinOk') : t('posSimulator.pinWrong')}
                  </span>
                </label>
              </Field>
            )}

            <button onClick={handleSubmit} disabled={loading || !cardId || !merchantId || !terminalId}
              style={{
                padding: '12px 24px', borderRadius: 8, border: 'none',
                background: '#8b5cf6', color: '#fff', fontWeight: 700, fontSize: 14,
                cursor: loading || !cardId || !merchantId || !terminalId ? 'not-allowed' : 'pointer',
                opacity: loading || !cardId || !merchantId || !terminalId ? 0.5 : 1,
                marginTop: 8,
              }}>
              {loading ? t('common.loading') + '...' : t('posSimulator.submit')}
            </button>

            {error && (
              <p style={{ color: '#ef4444', fontSize: 13, marginTop: 8 }}>{error}</p>
            )}
          </div>
        </div>

        <div>
          {result && (
            <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
              <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>
                {t('posSimulator.resultTitle')}
              </h3>

              <div style={{ display: 'grid', gap: 12 }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div style={{ padding: 16, borderRadius: 8, background: 'var(--bg)', border: '1px solid var(--border)' }}>
                    <p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('posSimulator.decision')}</p>
                    <span style={{
                      background: `${(DECISION_COLORS[result.decision] || '#64748b')}33`,
                      color: DECISION_COLORS[result.decision] || '#64748b',
                      padding: '4px 12px', borderRadius: 4, fontSize: 13, fontWeight: 700,
                    }}>{DECISION_LABELS[result.decision] ?? result.decision}</span>
                  </div>
                  <div style={{ padding: 16, borderRadius: 8, background: 'var(--bg)', border: '1px solid var(--border)' }}>
                    <p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('posSimulator.responseCode')}</p>
                    <p style={{ fontSize: 24, fontWeight: 700, fontFamily: 'monospace' }}>{result.responseCode}</p>
                  </div>
                </div>

                <DetailRow label={t('posSimulator.message')} value={result.message} />
                <DetailRow label={t('posSimulator.cardBrand')} value={`${result.cardBrand} / ${result.cardType}`} />
                <DetailRow label={t('posSimulator.channel')} value={result.channel} />
                <DetailRow label={t('posSimulator.posEntryMode')} value={`${result.posEntryMode} / ${result.posConditionCode}`} />
                <DetailRow label={t('posSimulator.pinStatus')} value={result.pinStatus} />
                <DetailRow label={t('posSimulator.riskScore')} value={`${result.riskScore} (${result.riskDecision})`} />
                <DetailRow label={t('posSimulator.transactionId')} value={result.transactionId} />
                <DetailRow label={t('posSimulator.stan')} value={result.stan} />
                <DetailRow label={t('posSimulator.processingTime')} value={`${result.processingTimeMs} ms`} />
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4, fontWeight: 600 }}>
        {label}
      </p>
      {children}
    </div>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
      <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{label}</span>
      <span style={{ fontSize: 13, fontWeight: 600, textAlign: 'right' }}>{value}</span>
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '8px 10px', borderRadius: 6,
  border: '1px solid var(--border)', background: 'var(--bg)',
  color: 'var(--text)', fontSize: 13, boxSizing: 'border-box',
};

const selectStyle: React.CSSProperties = {
  ...inputStyle,
  cursor: 'pointer',
};
