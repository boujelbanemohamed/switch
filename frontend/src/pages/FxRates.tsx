import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { FxRate } from '../types';
import { SectionHeader } from '../components/SectionHeader';
import { FxRatesHelp } from '../components/FxRatesHelp';

export function FxRates() {
  const { t } = useTranslation();
  const [rates, setRates] = useState<FxRate[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [showConvert, setShowConvert] = useState(false);
  const [convertResult, setConvertResult] = useState<{ convertedAmount: number; dccAmount: number } | null>(null);
  const [newRate, setNewRate] = useState<Partial<FxRate>>({ sourceCurrency: '', targetCurrency: '', rate: 0, marginPercentage: 0, effectiveDate: '' });
  const [convert, setConvert] = useState({ amount: 0, sourceCurrency: 'TND', targetCurrency: 'EUR' });

  const load = () => {
    setLoading(true);
    api.fx.rates.list().then(setRates).catch(console.error).finally(() => setLoading(false));
  };

  useEffect(load, []);

  const createRate = async () => {
    await api.fx.rates.create(newRate);
    setShowForm(false);
    setNewRate({ sourceCurrency: '', targetCurrency: '', rate: 0, marginPercentage: 0, effectiveDate: '' });
    load();
  };

  const doConvert = async () => {
    const [conv, dcc] = await Promise.all([
      api.fx.convert(convert.amount, convert.sourceCurrency, convert.targetCurrency),
      api.fx.proposeDcc(convert.amount, convert.sourceCurrency, convert.targetCurrency),
    ]);
    setConvertResult({ convertedAmount: conv.convertedAmount, dccAmount: dcc.dccAmount });
  };

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 }}>
        <h2 style={{ fontSize: 24, fontWeight: 700, margin: 0 }}>{t('fx.title')}</h2>
        <FxRatesHelp />
      </div>
      <SectionHeader sectionKey="fx" />

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 24, marginBottom: 24 }}>
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ fontSize: 16, fontWeight: 600 }}>{t('fx.rates')}</h3>
            <button onClick={() => setShowForm(true)}
              style={{ padding: '6px 14px', borderRadius: 8, border: 'none', background: 'var(--accent)', color: '#fff', fontWeight: 600, cursor: 'pointer', fontSize: 12 }}>{t('common.add')}</button>
          </div>
          {showForm && (
            <div style={{ marginBottom: 16, padding: 12, borderRadius: 8, background: 'var(--bg)', display: 'flex', flexDirection: 'column', gap: 8 }}>
              <div style={{ display: 'flex', gap: 8 }}>
                <input placeholder="TND" value={newRate.sourceCurrency} onChange={e => setNewRate({ ...newRate, sourceCurrency: e.target.value })}
                  style={{ flex: 1, padding: '8px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)' }} />
                <input placeholder="EUR" value={newRate.targetCurrency} onChange={e => setNewRate({ ...newRate, targetCurrency: e.target.value })}
                  style={{ flex: 1, padding: '8px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)' }} />
              </div>
              <input type="number" step="0.0001" placeholder={t('fx.rate')} value={newRate.rate || ''} onChange={e => setNewRate({ ...newRate, rate: parseFloat(e.target.value) || 0 })}
                style={{ padding: '8px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)' }} />
              <input type="number" step="0.01" placeholder={t('fx.margin')} value={newRate.marginPercentage ?? ''} onChange={e => setNewRate({ ...newRate, marginPercentage: parseFloat(e.target.value) || 0 })}
                style={{ padding: '8px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)' }} />
              <input type="date" placeholder={t('fx.effectiveDate')} value={newRate.effectiveDate || ''} onChange={e => setNewRate({ ...newRate, effectiveDate: e.target.value })}
                style={{ padding: '8px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)' }} />
              <div style={{ display: 'flex', gap: 8 }}>
                <button onClick={createRate} style={{ padding: '6px 14px', borderRadius: 6, border: 'none', background: 'var(--accent)', color: '#fff', fontWeight: 600, cursor: 'pointer' }}>{t('common.save')}</button>
                <button onClick={() => setShowForm(false)} style={{ padding: '6px 14px', borderRadius: 6, border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', cursor: 'pointer' }}>{t('common.cancel')}</button>
              </div>
            </div>
          )}
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('fx.pair')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('fx.rate')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('fx.margin')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('fx.effectiveDate')}</th>
                </tr>
              </thead>
              <tbody>
                {rates.map(r => (
                  <tr key={r.id} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '10px 12px', fontWeight: 600 }}>{r.sourceCurrency}/{r.targetCurrency}</td>
                    <td style={{ padding: '10px 12px', fontFamily: 'monospace' }}>{r.rate}</td>
                    <td style={{ padding: '10px 12px' }}>{r.marginPercentage}%</td>
                    <td style={{ padding: '10px 12px', fontSize: 12 }}>{r.effectiveDate}</td>
                  </tr>
                ))}
                {rates.length === 0 && <tr><td colSpan={4} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('common.noData')}</td></tr>}
              </tbody>
            </table>
          </div>
        </div>

        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('fx.converter')}</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <input type="number" placeholder={t('fx.amount')} value={convert.amount || ''} onChange={e => setConvert({ ...convert, amount: parseFloat(e.target.value) || 0 })}
              style={{ padding: '8px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)' }} />
            <div style={{ display: 'flex', gap: 8 }}>
              <input placeholder={t('fx.from')} value={convert.sourceCurrency} onChange={e => setConvert({ ...convert, sourceCurrency: e.target.value.toUpperCase() })}
                style={{ flex: 1, padding: '8px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)' }} />
              <input placeholder={t('fx.to')} value={convert.targetCurrency} onChange={e => setConvert({ ...convert, targetCurrency: e.target.value.toUpperCase() })}
                style={{ flex: 1, padding: '8px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)' }} />
            </div>
            <button onClick={doConvert}
              style={{ padding: '10px', borderRadius: 8, border: 'none', background: 'var(--accent)', color: '#fff', fontWeight: 600, cursor: 'pointer' }}>{t('fx.convert')}</button>
            {convertResult && (
              <div style={{ padding: 12, borderRadius: 8, background: '#22c55e11', border: '1px solid #22c55e33' }}>
                <p style={{ fontSize: 13, fontWeight: 600 }}>{t('fx.result')}</p>
                <p style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{t('fx.convertedAmount')}: <strong>{convertResult.convertedAmount.toLocaleString()}</strong></p>
                <p style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{t('fx.dccAmount')}: <strong>{convertResult.dccAmount.toLocaleString()}</strong></p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
