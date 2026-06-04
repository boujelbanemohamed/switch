import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { CofToken, RecurringSchedule } from '../types';
import { SectionHeader } from '../components/SectionHeader';

export function CofPage() {
  const { t } = useTranslation();
  const [tokens, setTokens] = useState<CofToken[]>([]);
  const [schedules, setSchedules] = useState<RecurringSchedule[]>([]);
  const [loading, setLoading] = useState(true);
  const [showTokenForm, setShowTokenForm] = useState(false);
  const [showScheduleForm, setShowScheduleForm] = useState(false);
  const [newToken, setNewToken] = useState<Partial<CofToken>>({ panDisplay: '', panReference: '' });
  const [newSchedule, setNewSchedule] = useState<Partial<RecurringSchedule>>({ amount: 0, frequency: 'MONTHLY', nextRunDate: '' });

  const load = () => {
    setLoading(true);
    Promise.all([api.cof.tokens.list(), api.cof.schedules.list()])
      .then(([t, s]) => { setTokens(t); setSchedules(s); })
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const createToken = async () => {
    await api.cof.tokens.create(newToken);
    setShowTokenForm(false);
    setNewToken({ panDisplay: '', panReference: '' });
    load();
  };

  const createSchedule = async () => {
    await api.cof.schedules.create(newSchedule);
    setShowScheduleForm(false);
    setNewSchedule({ amount: 0, frequency: 'MONTHLY', nextRunDate: '' });
    load();
  };

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('cof.title')}</h2>
      <SectionHeader sectionKey="cof" />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ fontSize: 16, fontWeight: 600 }}>{t('cof.tokens')}</h3>
            <button onClick={() => setShowTokenForm(true)}
              style={{ padding: '6px 14px', borderRadius: 8, border: 'none', background: 'var(--accent)', color: '#fff', fontWeight: 600, cursor: 'pointer', fontSize: 12 }}>{t('common.add')}</button>
          </div>
          {showTokenForm && (
            <div style={{ marginBottom: 16, padding: 12, borderRadius: 8, background: 'var(--bg)', display: 'flex', flexDirection: 'column', gap: 8 }}>
              <input placeholder={t('cof.panDisplay')} value={newToken.panDisplay} onChange={e => setNewToken({ ...newToken, panDisplay: e.target.value })}
                style={{ padding: '8px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)' }} />
              <input placeholder={t('cof.panReference')} value={newToken.panReference} onChange={e => setNewToken({ ...newToken, panReference: e.target.value })}
                style={{ padding: '8px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)' }} />
              <div style={{ display: 'flex', gap: 8 }}>
                <button onClick={createToken} style={{ padding: '6px 14px', borderRadius: 6, border: 'none', background: 'var(--accent)', color: '#fff', fontWeight: 600, cursor: 'pointer' }}>{t('common.save')}</button>
                <button onClick={() => setShowTokenForm(false)} style={{ padding: '6px 14px', borderRadius: 6, border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', cursor: 'pointer' }}>{t('common.cancel')}</button>
              </div>
            </div>
          )}
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('cof.panDisplay')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('cof.tokenType')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('cof.status')}</th>
                </tr>
              </thead>
              <tbody>
                {tokens.map(t => (
                  <tr key={t.id} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '10px 12px', fontFamily: 'monospace' }}>{t.panDisplay}</td>
                    <td style={{ padding: '10px 12px' }}>{t.tokenType}</td>
                    <td style={{ padding: '10px 12px' }}>
                      <span style={{ background: t.status === 'ACTIVE' ? '#22c55e33' : '#64748b33', color: t.status === 'ACTIVE' ? '#22c55e' : '#64748b', padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600 }}>{t.status}</span>
                    </td>
                  </tr>
                ))}
                {tokens.length === 0 && <tr><td colSpan={3} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('common.noData')}</td></tr>}
              </tbody>
            </table>
          </div>
        </div>

        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ fontSize: 16, fontWeight: 600 }}>{t('cof.schedules')}</h3>
            <button onClick={() => setShowScheduleForm(true)}
              style={{ padding: '6px 14px', borderRadius: 8, border: 'none', background: 'var(--accent)', color: '#fff', fontWeight: 600, cursor: 'pointer', fontSize: 12 }}>{t('common.add')}</button>
          </div>
          {showScheduleForm && (
            <div style={{ marginBottom: 16, padding: 12, borderRadius: 8, background: 'var(--bg)', display: 'flex', flexDirection: 'column', gap: 8 }}>
              <input type="number" placeholder={t('cof.amount')} value={newSchedule.amount || ''} onChange={e => setNewSchedule({ ...newSchedule, amount: parseFloat(e.target.value) || 0 })}
                style={{ padding: '8px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)' }} />
              <select value={newSchedule.frequency} onChange={e => setNewSchedule({ ...newSchedule, frequency: e.target.value })}
                style={{ padding: '8px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)' }}>
                <option value="DAILY">{t('cof.daily')}</option>
                <option value="WEEKLY">{t('cof.weekly')}</option>
                <option value="MONTHLY">{t('cof.monthly')}</option>
              </select>
              <input type="date" placeholder={t('cof.nextRunDate')} value={newSchedule.nextRunDate || ''} onChange={e => setNewSchedule({ ...newSchedule, nextRunDate: e.target.value })}
                style={{ padding: '8px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)' }} />
              <div style={{ display: 'flex', gap: 8 }}>
                <button onClick={createSchedule} style={{ padding: '6px 14px', borderRadius: 6, border: 'none', background: 'var(--accent)', color: '#fff', fontWeight: 600, cursor: 'pointer' }}>{t('common.save')}</button>
                <button onClick={() => setShowScheduleForm(false)} style={{ padding: '6px 14px', borderRadius: 6, border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', cursor: 'pointer' }}>{t('common.cancel')}</button>
              </div>
            </div>
          )}
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('cof.amount')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('cof.frequency')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('cof.nextRunDate')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('cof.status')}</th>
                </tr>
              </thead>
              <tbody>
                {schedules.map(s => (
                  <tr key={s.id} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '10px 12px' }}>{s.amount.toLocaleString()} {s.currencyCode}</td>
                    <td style={{ padding: '10px 12px' }}>{s.frequency}</td>
                    <td style={{ padding: '10px 12px', fontSize: 12 }}>{s.nextRunDate}</td>
                    <td style={{ padding: '10px 12px' }}>
                      <span style={{ background: s.status === 'ACTIVE' ? '#22c55e33' : s.status === 'COMPLETED' ? '#3b82f633' : '#64748b33', color: s.status === 'ACTIVE' ? '#22c55e' : s.status === 'COMPLETED' ? '#3b82f6' : '#64748b', padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600 }}>{s.status}</span>
                    </td>
                  </tr>
                ))}
                {schedules.length === 0 && <tr><td colSpan={4} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('common.noData')}</td></tr>}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
