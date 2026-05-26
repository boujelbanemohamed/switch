import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { FraudAlert, FraudRule } from '../types';
import { SectionHeader } from '../components/SectionHeader';
import { Plus, X } from 'lucide-react';

type Tab = 'alerts' | 'rules';

const alertColors: Record<string, string> = {
  OPEN: '#ef4444',
  INVESTIGATING: '#eab308',
  CONFIRMED: '#f97316',
  DISMISSED: '#64748b',
};

const severityColors: Record<string, string> = {
  LOW: '#22c55e',
  MEDIUM: '#eab308',
  HIGH: '#f97316',
  CRITICAL: '#ef4444',
};

const categoryColors: Record<string, string> = {
  VELOCITY: '#3b82f6',
  GEO: '#22c55e',
  BEHAVIORAL: '#a855f7',
  AMOUNT: '#f97316',
  MERCHANT: '#ec4899',
  DEVICE: '#14b8a6',
  NETWORK: '#8b5cf6',
  ML_MODEL: '#06b6d4',
  MANUAL: '#64748b',
};

const OVERLAY: React.CSSProperties = {
  position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)',
  display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100,
};

const MODAL: React.CSSProperties = {
  background: 'var(--surface)', borderRadius: 16, padding: 28,
  width: 520, maxWidth: '90vw', maxHeight: '85vh', overflow: 'auto',
  border: '1px solid var(--border)',
};

const INPUT: React.CSSProperties = {
  width: '100%', padding: '10px 12px', borderRadius: 8,
  border: '1px solid var(--border)', background: 'var(--bg)',
  color: 'var(--text)', fontSize: 13, boxSizing: 'border-box',
};

const SELECT: React.CSSProperties = { ...INPUT, cursor: 'pointer' };

export function Fraud() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('alerts');
  const [alerts, setAlerts] = useState<FraudAlert[]>([]);
  const [rules, setRules] = useState<FraudRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [saving, setSaving] = useState(false);

  const [form, setForm] = useState<{
    name: string; description: string; ruleCategory: string; severity: string;
    action: string; conditionExpression: string; scoreWeight: string; cooldownSeconds: string; status: string;
  }>({
    name: '', description: '', ruleCategory: 'AMOUNT', severity: 'MEDIUM',
    action: 'FLAG', conditionExpression: '', scoreWeight: '30', cooldownSeconds: '', status: 'ACTIVE',
  });

  const loadAlerts = () => {
    api.fraud.alerts.list()
      .then(setAlerts)
      .catch(console.error);
  };

  const loadRules = () => {
    api.fraud.rules.list()
      .then(setRules)
      .catch(console.error);
  };

  useEffect(() => {
    setLoading(true);
    Promise.all([
      api.fraud.alerts.list(),
      api.fraud.rules.list(),
    ])
      .then(([a, r]) => { setAlerts(a); setRules(r); })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const openAlerts = alerts.filter(a => a.status === 'OPEN');
  const confirmedAlerts = alerts.filter(a => a.status === 'CONFIRMED');

  const handleSubmit = async () => {
    setSaving(true);
    try {
      await api.fraud.rules.create({
        name: form.name,
        description: form.description || undefined,
        ruleCategory: form.ruleCategory as FraudRule['ruleCategory'],
        severity: form.severity as FraudRule['severity'],
        action: form.action as FraudRule['action'],
        conditionExpression: form.conditionExpression || undefined,
        scoreWeight: parseInt(form.scoreWeight) || 0,
        cooldownSeconds: form.cooldownSeconds ? parseInt(form.cooldownSeconds) : undefined,
        status: form.status as FraudRule['status'],
      });
      setShowModal(false);
      setForm({ name: '', description: '', ruleCategory: 'AMOUNT', severity: 'MEDIUM', action: 'FLAG', conditionExpression: '', scoreWeight: '30', cooldownSeconds: '', status: 'ACTIVE' });
      loadRules();
    } catch (e) {
      console.error(e);
      alert(e instanceof Error ? e.message : 'Failed to create rule');
    }
    setSaving(false);
  };

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('fraud.title')}</h2>

      <SectionHeader sectionKey="fraud" />

      <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
        {(['alerts', 'rules'] as Tab[]).map(tabKey => (
          <button
            key={tabKey}
            onClick={() => setTab(tabKey)}
            style={{
              padding: '8px 20px', borderRadius: 8, border: 'none',
              background: tab === tabKey ? '#3b82f6' : 'var(--surface)',
              color: tab === tabKey ? '#fff' : 'var(--text-secondary)',
              fontWeight: 600, fontSize: 13, cursor: 'pointer',
            }}
          >
            {t(`fraud.tab${tabKey === 'alerts' ? 'Alerts' : 'Rules'}`)}
          </button>
        ))}
      </div>

      {tab === 'alerts' && (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
            <StatCard title={t('fraud.totalAlerts')} value={alerts.length.toLocaleString()} />
            <StatCard title={t('fraud.open')} value={openAlerts.length.toLocaleString()} />
            <StatCard title={t('fraud.confirmed')} value={confirmedAlerts.length.toLocaleString()} />
            <StatCard title={t('fraud.highScore')} value={alerts.filter(a => a.score > 80).length.toLocaleString()} />
          </div>

          <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
            <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('fraud.alerts')}</h3>
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('fraud.rule')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('fraud.transaction')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('fraud.score')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('fraud.status')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('fraud.decision')}</th>
                  </tr>
                </thead>
                <tbody>
                  {alerts.map(a => (
                    <tr key={a.id} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '10px 12px', fontWeight: 600 }}>{a.ruleName}</td>
                      <td style={{ padding: '10px 12px', fontFamily: 'monospace', fontSize: 12, color: 'var(--text-secondary)' }}>
                        {a.transactionId?.substring(0, 12)}...
                      </td>
                      <td style={{ padding: '10px 12px' }}>
                        <span style={{
                          color: a.score > 80 ? '#ef4444' : a.score > 50 ? '#eab308' : '#22c55e',
                          fontWeight: 700,
                        }}>
                          {Math.round(a.score)}
                        </span>
                      </td>
                      <td style={{ padding: '10px 12px' }}>
                        <span style={{
                          background: `${(alertColors[a.status] || '#64748b')}33`,
                          color: alertColors[a.status] || '#64748b',
                          padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                        }}>
                          {a.status}
                        </span>
                      </td>
                      <td style={{ padding: '10px 12px', color: 'var(--text-secondary)' }}>{a.decision || t('fraud.decision')}</td>
                    </tr>
                  ))}
                  {alerts.length === 0 && (
                    <tr>
                      <td colSpan={5} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>
                        {t('fraud.noAlerts')}
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {tab === 'rules' && (
        <>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ fontSize: 16, fontWeight: 600 }}>{t('fraud.rules')}</h3>
            <button onClick={() => setShowModal(true)} style={{
              display: 'flex', alignItems: 'center', gap: 8,
              background: '#3b82f6', color: 'white', border: 'none',
              borderRadius: 8, padding: '8px 16px', fontSize: 14,
              fontWeight: 600, cursor: 'pointer',
            }}>
              <Plus size={16} /> {t('fraud.addRule')}
            </button>
          </div>

          <div style={{ background: 'var(--surface)', borderRadius: 12, overflow: 'hidden' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                  {[t('fraud.rulesName'), t('fraud.rulesCategory'), t('fraud.rulesSeverity'), t('fraud.rulesAction'), t('fraud.rulesWeight'), t('fraud.rulesStatus'), t('fraud.rulesHits')].map(h => (
                    <th key={h} style={{ padding: '12px 16px', fontSize: 12, color: 'var(--text-secondary)', fontWeight: 600 }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rules.map(r => (
                  <tr key={r.id} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '12px 16px', fontWeight: 600, fontSize: 13 }}>
                      <div>{r.name}</div>
                      {r.description && <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginTop: 2 }}>{r.description}</div>}
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <span style={{
                        background: `${categoryColors[r.ruleCategory]}33`,
                        color: categoryColors[r.ruleCategory],
                        padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                      }}>
                        {r.ruleCategory}
                      </span>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <span style={{
                        background: `${severityColors[r.severity]}33`,
                        color: severityColors[r.severity],
                        padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                      }}>
                        {r.severity}
                      </span>
                    </td>
                    <td style={{ padding: '12px 16px', fontSize: 13, fontFamily: 'monospace' }}>{r.action}</td>
                    <td style={{ padding: '12px 16px', fontSize: 13, fontFamily: 'monospace' }}>{r.scoreWeight ?? '-'}</td>
                    <td style={{ padding: '12px 16px' }}>
                      <span style={{
                        background: r.status === 'ACTIVE' ? '#22c55e33' : r.status === 'TESTING' ? '#eab30833' : '#64748b33',
                        color: r.status === 'ACTIVE' ? '#22c55e' : r.status === 'TESTING' ? '#eab308' : '#64748b',
                        padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                      }}>
                        {r.status}
                      </span>
                    </td>
                    <td style={{ padding: '12px 16px', fontSize: 13 }}>
                      <span style={{ color: '#22c55e', fontWeight: 600 }}>{r.truePositiveCount}</span>
                      {' / '}
                      <span style={{ color: '#ef4444', fontWeight: 600 }}>{r.falsePositiveCount}</span>
                    </td>
                  </tr>
                ))}
                {rules.length === 0 && (
                  <tr>
                    <td colSpan={7} style={{ padding: 32, textAlign: 'center', color: 'var(--text-secondary)' }}>
                      {t('fraud.noRules')}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}

      {showModal && (
        <div style={OVERLAY} onClick={() => setShowModal(false)}>
          <div style={MODAL} onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h3 style={{ fontSize: 18, fontWeight: 700 }}>{t('fraud.addRule')}</h3>
              <button onClick={() => setShowModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}>
                <X size={20} />
              </button>
            </div>

            <div style={{ display: 'grid', gap: 14 }}>
              <Field label={t('fraud.rulesName')}>
                <input style={INPUT} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
              </Field>
              <Field label={t('fraud.rulesDescription')}>
                <textarea style={{ ...INPUT, resize: 'vertical', minHeight: 60, fontFamily: 'inherit' }} value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
              </Field>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('fraud.rulesCategory')}>
                  <select style={SELECT} value={form.ruleCategory} onChange={e => setForm({ ...form, ruleCategory: e.target.value })}>
                    {['VELOCITY', 'GEO', 'BEHAVIORAL', 'AMOUNT', 'MERCHANT', 'DEVICE', 'NETWORK', 'ML_MODEL', 'MANUAL'].map(c => (
                      <option key={c}>{c}</option>
                    ))}
                  </select>
                </Field>
                <Field label={t('fraud.rulesSeverity')}>
                  <select style={SELECT} value={form.severity} onChange={e => setForm({ ...form, severity: e.target.value })}>
                    {['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map(s => (
                      <option key={s}>{s}</option>
                    ))}
                  </select>
                </Field>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('fraud.rulesAction')}>
                  <select style={SELECT} value={form.action} onChange={e => setForm({ ...form, action: e.target.value })}>
                    {['BLOCK', 'FLAG', 'CHALLENGE', 'MONITOR', 'TFA', 'ALLOW'].map(a => (
                      <option key={a}>{a}</option>
                    ))}
                  </select>
                </Field>
                <Field label={t('fraud.rulesWeight')}>
                  <input style={INPUT} type="number" value={form.scoreWeight} onChange={e => setForm({ ...form, scoreWeight: e.target.value })} />
                </Field>
              </div>
              <Field label={t('fraud.conditionExpression')}>
                <input style={INPUT} value={form.conditionExpression} onChange={e => setForm({ ...form, conditionExpression: e.target.value })} placeholder={t('fraud.conditionPlaceholder')} />
              </Field>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('fraud.cooldownSeconds')}>
                  <input style={INPUT} type="number" value={form.cooldownSeconds} onChange={e => setForm({ ...form, cooldownSeconds: e.target.value })} placeholder={t('fraud.cooldownPlaceholder')} />
                </Field>
                <Field label={t('fraud.rulesStatus')}>
                  <select style={SELECT} value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>
                    {['ACTIVE', 'INACTIVE', 'TESTING'].map(s => (
                      <option key={s}>{s}</option>
                    ))}
                  </select>
                </Field>
              </div>
            </div>

            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 24 }}>
              <button onClick={() => setShowModal(false)} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>
                {t('fraud.cancel')}
              </button>
              <button onClick={handleSubmit} disabled={saving || !form.name} style={{
                padding: '10px 20px', borderRadius: 8, border: 'none',
                background: saving || !form.name ? '#64748b' : '#3b82f6',
                color: 'white', cursor: saving || !form.name ? 'not-allowed' : 'pointer',
                fontSize: 13, fontWeight: 600,
              }}>
                {saving ? t('fraud.saving') : t('fraud.save')}
              </button>
            </div>
          </div>
        </div>
      )}
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

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4, fontWeight: 500 }}>
        {label}
      </label>
      {children}
    </div>
  );
}
