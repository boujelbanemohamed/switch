import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { AuthRule, AuthDecision, HoldRecord } from '../types';
import { SectionHeader } from '../components/SectionHeader';

type Tab = 'rules' | 'decisions' | 'holds' | 'simulator';

const styles = {
  overlay: { position: 'fixed' as const, inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 },
  modal: { background: 'var(--surface)', borderRadius: 16, padding: 28, width: 520, maxWidth: '90vw', maxHeight: '85vh', overflow: 'auto', border: '1px solid var(--border)' },
  input: { width: '100%', padding: '10px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13, boxSizing: 'border-box' as const },
  select: { width: '100%', padding: '10px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13, boxSizing: 'border-box' as const, cursor: 'pointer' },
};

const decisionColors: Record<string, string> = {
  APPROVE: '#22c55e', DECLINE: '#ef4444', REVIEW: '#f59e0b', FLAG: '#f97316',
  BLOCK: '#dc2626', CHALLENGE: '#8b5cf6',
};

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4, fontWeight: 500 }}>{label}</label>
      {children}
    </div>
  );
}

function MiniBtn({ label, color, onClick }: { label: string; color: string; onClick: () => void }) {
  return (
    <button onClick={onClick} style={{
      padding: '4px 8px', border: `1px solid ${color}33`, borderRadius: 4,
      background: color + '15', color, cursor: 'pointer', fontSize: 11, fontWeight: 600, whiteSpace: 'nowrap',
    }}>{label}</button>
  );
}

function StatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    ACTIVE: '#22c55e', INACTIVE: '#64748b', ACTIVE_HOLD: '#3b82f6',
    RELEASED: '#22c55e', EXPIRED: '#6b7280', CAPTURED: '#8b5cf6',
  };
  const color = colors[status] || '#6b7280';
  return <span style={{ background: color + '33', color, padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600 }}>{status}</span>;
}

export function Authorization() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('rules');
  const [rules, setRules] = useState<AuthRule[]>([]);
  const [decisions, setDecisions] = useState<AuthDecision[]>([]);
  const [holds, setHolds] = useState<HoldRecord[]>([]);
  const [loading, setLoading] = useState(true);

  const [showRuleModal, setShowRuleModal] = useState(false);
  const [saving, setSaving] = useState(false);
  const [ruleForm, setRuleForm] = useState({
    name: '', ruleType: 'AMOUNT_LIMIT', conditionExpression: '{}', priority: '1',
    action: 'APPROVE', timeRestrictions: '', status: 'ACTIVE',
  });

  const [simForm, setSimForm] = useState({ cardNumber: '', amount: '', merchant: '' });
  const [simResult, setSimResult] = useState<AuthDecision | null>(null);

  const [decisionsCardId, setDecisionsCardId] = useState('');
  const [holdsCardId, setHoldsCardId] = useState('');

  useEffect(() => {
    api.authorization.rules.list()
      .then(setRules)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const loadDecisions = async (cardId: string) => {
    if (!cardId) return;
    try {
      const data = await api.authorization.decisions.list(cardId, 20);
      setDecisions(data);
    } catch { setDecisions([]); }
  };

  const loadHolds = async (cardId: string) => {
    if (!cardId) return;
    try {
      const data = await api.authorization.holds.listByCard(cardId);
      setHolds(data);
    } catch { setHolds([]); }
  };

  const createRule = async () => {
    setSaving(true);
    try {
      await api.authorization.rules.create({
        name: ruleForm.name,
        ruleType: ruleForm.ruleType,
        priority: parseInt(ruleForm.priority) || 1,
        status: ruleForm.status,
        parameters: ruleForm.conditionExpression,
      });
      setShowRuleModal(false);
      setRuleForm({ name: '', ruleType: 'AMOUNT_LIMIT', conditionExpression: '{}', priority: '1', action: 'APPROVE', timeRestrictions: '', status: 'ACTIVE' });
      const list = await api.authorization.rules.list();
      setRules(list);
    } catch (e) { console.error(e); alert(e instanceof Error ? e.message : 'Failed to create rule'); }
    setSaving(false);
  };

  const handleSimulate = async () => {
    try {
      const result = await api.authorization.authorize({
        cardNumber: simForm.cardNumber,
        amount: parseFloat(simForm.amount) || 0,
        merchantId: simForm.merchant,
      });
      setSimResult(result as unknown as AuthDecision);
    } catch (e) {
      setSimResult({ id: '', cardId: '', transactionId: '', decision: 'ERROR', responseCode: '99', timestamp: new Date().toISOString() });
    }
  };

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;

  const tabs: { key: Tab; label: string }[] = [
    { key: 'rules', label: t('authorization.tabRules') },
    { key: 'decisions', label: t('authorization.tabDecisions') },
    { key: 'holds', label: t('authorization.tabHolds') },
    { key: 'simulator', label: t('authorization.tabSimulator') },
  ];

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('authorization.title')}</h2>
      <SectionHeader sectionKey="authorization" />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        <StatCard title={t('authorization.totalRules')} value={rules.length.toLocaleString()} />
        <StatCard title={t('authorization.activeRules')} value={rules.filter(r => r.status === 'ACTIVE').length.toLocaleString()} />
        <StatCard title={t('authorization.successRate')} value={
          rules.length > 0
            ? `${Math.round((rules.reduce((s, r) => s + r.successCount, 0) / Math.max(1,
                rules.reduce((s, r) => s + r.successCount + r.failureCount, 0))) * 100)}%`
            : t('authorization.na')
        } />
        <StatCard title={t('authorization.totalDecisions')} value={rules.reduce((s, r) => s + r.successCount + r.failureCount, 0).toLocaleString()} />
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        {tabs.map(tk => (
          <button key={tk.key} onClick={() => setTab(tk.key)} style={{
            padding: '8px 16px', border: '1px solid var(--border)', borderRadius: 8,
            background: tab === tk.key ? '#3b82f6' : 'var(--surface)',
            color: tab === tk.key ? '#fff' : 'var(--text)', cursor: 'pointer', fontWeight: 600, fontSize: 13,
          }}>{tk.label}</button>
        ))}
      </div>

      {tab === 'rules' && (
        <>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ fontSize: 16, fontWeight: 600 }}>{t('authorization.rules')}</h3>
            <button onClick={() => setShowRuleModal(true)} style={{
              display: 'flex', alignItems: 'center', gap: 8, background: '#3b82f6', color: 'white',
              border: 'none', borderRadius: 8, padding: '8px 16px', fontSize: 14, fontWeight: 600, cursor: 'pointer',
            }}>{t('authorization.addRule')}</button>
          </div>
          <div style={{ background: 'var(--surface)', borderRadius: 12, overflow: 'hidden' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                  <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.name')}</th>
                  <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.type')}</th>
                  <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.action')}</th>
                  <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.priority')}</th>
                  <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.status')}</th>
                  <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.success')}</th>
                  <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.failures')}</th>
                </tr>
              </thead>
              <tbody>
                {rules.map(r => (
                  <tr key={r.id} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '12px 16px', fontWeight: 600 }}>{r.name}</td>
                    <td style={{ padding: '12px 16px', color: 'var(--text-secondary)' }}>{r.ruleType}</td>
                    <td style={{ padding: '12px 16px' }}>-</td>
                    <td style={{ padding: '12px 16px', fontFamily: 'monospace' }}>{r.priority}</td>
                    <td style={{ padding: '12px 16px' }}><StatusBadge status={r.status} /></td>
                    <td style={{ padding: '12px 16px', color: '#22c55e', fontWeight: 600 }}>{r.successCount}</td>
                    <td style={{ padding: '12px 16px', color: '#ef4444', fontWeight: 600 }}>{r.failureCount}</td>
                  </tr>
                ))}
                {rules.length === 0 && (
                  <tr><td colSpan={7} style={{ padding: 32, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('authorization.noRules')}</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}

      {tab === 'decisions' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('authorization.tabDecisions')}</h3>
          <div style={{ display: 'flex', gap: 12, marginBottom: 16, alignItems: 'flex-end' }}>
            <Field label={t('authorization.cardId')}>
              <input style={styles.input} value={decisionsCardId} onChange={e => setDecisionsCardId(e.target.value)} placeholder="Card UUID" />
            </Field>
            <button onClick={() => loadDecisions(decisionsCardId)} style={{
              padding: '8px 20px', borderRadius: 8, border: 'none', background: '#3b82f6',
              color: 'white', fontWeight: 600, cursor: 'pointer', fontSize: 13,
            }}>Load</button>
          </div>
          {decisions.length > 0 ? (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.transactionId')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.amount')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.decision')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.responseCode')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.riskScore')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.timestamp')}</th>
                  </tr>
                </thead>
                <tbody>
                  {decisions.map(d => (
                    <tr key={d.id} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '10px 12px', fontFamily: 'monospace', fontSize: 12 }}>{d.transactionId?.substring(0, 16) || '-'}</td>
                      <td style={{ padding: '10px 12px', fontFamily: 'monospace' }}>{(d as any).amount || '-'}</td>
                      <td style={{ padding: '10px 12px' }}>
                        <span style={{
                          background: `${(decisionColors[d.decision] || '#64748b')}33`,
                          color: decisionColors[d.decision] || '#64748b',
                          padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                        }}>{d.decision}</span>
                      </td>
                      <td style={{ padding: '10px 12px', fontFamily: 'monospace' }}>{d.responseCode}</td>
                      <td style={{ padding: '10px 12px' }}>{(d as any).riskScore || '-'}</td>
                      <td style={{ padding: '10px 12px', fontSize: 12 }}>{new Date(d.timestamp).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{t('authorization.noDecisions')}</p>
          )}
        </div>
      )}

      {tab === 'holds' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('authorization.tabHolds')}</h3>
          <div style={{ display: 'flex', gap: 12, marginBottom: 16, alignItems: 'flex-end' }}>
            <Field label={t('authorization.cardId')}>
              <input style={styles.input} value={holdsCardId} onChange={e => setHoldsCardId(e.target.value)} placeholder="Card UUID" />
            </Field>
            <button onClick={() => loadHolds(holdsCardId)} style={{
              padding: '8px 20px', borderRadius: 8, border: 'none', background: '#3b82f6',
              color: 'white', fontWeight: 600, cursor: 'pointer', fontSize: 13,
            }}>Load</button>
          </div>
          {holds.length > 0 ? (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.holdId')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.holdAmount')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.holdReason')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.holdStatus')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.expiresAt')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {holds.map(h => (
                    <tr key={h.id} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '10px 12px', fontFamily: 'monospace', fontSize: 12 }}>{h.id.substring(0, 8)}...</td>
                      <td style={{ padding: '10px 12px', fontFamily: 'monospace', fontWeight: 600 }}>{h.amount?.toLocaleString() || '-'}</td>
                      <td style={{ padding: '10px 12px' }}>{h.reason || '-'}</td>
                      <td style={{ padding: '10px 12px' }}><StatusBadge status={h.status} /></td>
                      <td style={{ padding: '10px 12px', fontSize: 12 }}>{h.expiresAt ? new Date(h.expiresAt).toLocaleString() : '-'}</td>
                      <td style={{ padding: '10px 12px', display: 'flex', gap: 4 }}>
                        {h.status === 'ACTIVE_HOLD' && (
                          <>
                            <MiniBtn label={t('authorization.releaseHold')} color="#22c55e" onClick={async () => {
                              await api.authorization.holds.release(h.id);
                              loadHolds(holdsCardId);
                            }} />
                            <MiniBtn label={t('authorization.captureHold')} color="#8b5cf6" onClick={async () => {
                              await api.authorization.holds.capture(h.id);
                              loadHolds(holdsCardId);
                            }} />
                          </>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{t('authorization.noHolds')}</p>
          )}
        </div>
      )}

      {tab === 'simulator' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('authorization.simulateTitle')}</h3>
          <div style={{ display: 'grid', gap: 14, maxWidth: 450 }}>
            <Field label={t('authorization.simCardNumber')}>
              <input style={styles.input} value={simForm.cardNumber} onChange={e => setSimForm({ ...simForm, cardNumber: e.target.value })} placeholder="4111111111111111" />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <Field label={t('authorization.simAmount')}>
                <input style={styles.input} type="number" value={simForm.amount} onChange={e => setSimForm({ ...simForm, amount: e.target.value })} placeholder="100.00" />
              </Field>
              <Field label={t('authorization.simMerchant')}>
                <input style={styles.input} value={simForm.merchant} onChange={e => setSimForm({ ...simForm, merchant: e.target.value })} placeholder="MERCH001" />
              </Field>
            </div>
            <button onClick={handleSimulate} style={{
              padding: '10px 20px', borderRadius: 8, border: 'none', background: '#8b5cf6',
              color: 'white', fontWeight: 600, cursor: 'pointer', fontSize: 14, marginTop: 8,
            }}>{t('authorization.simulate')}</button>

            {simResult && (
              <div style={{ marginTop: 16, padding: 16, borderRadius: 8, background: 'var(--bg)', border: '1px solid var(--border)' }}>
                <h4 style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>{t('authorization.simResult')}</h4>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12 }}>
                  <div>
                    <p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('authorization.simDecision')}</p>
                    <span style={{
                      background: `${(decisionColors[simResult.decision] || '#64748b')}33`,
                      color: decisionColors[simResult.decision] || '#64748b',
                      padding: '4px 12px', borderRadius: 4, fontSize: 13, fontWeight: 700,
                    }}>{simResult.decision}</span>
                  </div>
                  <div>
                    <p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('authorization.simResponseCode')}</p>
                    <p style={{ fontSize: 18, fontWeight: 700, fontFamily: 'monospace' }}>{simResult.responseCode}</p>
                  </div>
                </div>
                {simResult.reason && (
                  <div style={{ marginTop: 8 }}>
                    <p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('authorization.simReason')}</p>
                    <p style={{ fontSize: 13 }}>{simResult.reason}</p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {showRuleModal && (
        <div style={styles.overlay} onClick={() => setShowRuleModal(false)}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 20 }}>{t('authorization.addRule')}</h3>
            <div style={{ display: 'grid', gap: 14 }}>
              <Field label={t('authorization.name')}><input style={styles.input} value={ruleForm.name} onChange={e => setRuleForm({ ...ruleForm, name: e.target.value })} /></Field>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('authorization.type')}>
                  <select style={styles.select} value={ruleForm.ruleType} onChange={e => setRuleForm({ ...ruleForm, ruleType: e.target.value })}>
                    {['AMOUNT_LIMIT', 'VELOCITY', 'GEO', 'MERCHANT_BLOCK', 'TIME_RESTRICTION', 'BIN_ROUTING'].map(t => <option key={t}>{t}</option>)}
                  </select>
                </Field>
                <Field label={t('authorization.action')}>
                  <select style={styles.select} value={ruleForm.action} onChange={e => setRuleForm({ ...ruleForm, action: e.target.value })}>
                    {['APPROVE', 'DECLINE', 'REVIEW', 'FLAG', 'CHALLENGE'].map(a => <option key={a}>{a}</option>)}
                  </select>
                </Field>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('authorization.priority')}><input style={styles.input} type="number" value={ruleForm.priority} onChange={e => setRuleForm({ ...ruleForm, priority: e.target.value })} /></Field>
                <Field label={t('authorization.status')}>
                  <select style={styles.select} value={ruleForm.status} onChange={e => setRuleForm({ ...ruleForm, status: e.target.value })}>
                    {['ACTIVE', 'INACTIVE'].map(s => <option key={s}>{s}</option>)}
                  </select>
                </Field>
              </div>
              <Field label={t('authorization.conditions')}>
                <textarea style={{ ...styles.input, minHeight: 80, fontFamily: 'monospace', resize: 'vertical' } as React.CSSProperties}
                  value={ruleForm.conditionExpression} onChange={e => setRuleForm({ ...ruleForm, conditionExpression: e.target.value })}
                  placeholder='{"amount": {"max": 10000}}' />
              </Field>
              <Field label={t('authorization.timeRestrictions')}>
                <input style={styles.input} value={ruleForm.timeRestrictions} onChange={e => setRuleForm({ ...ruleForm, timeRestrictions: e.target.value })} placeholder="e.g. 08:00-18:00" />
              </Field>
            </div>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 24 }}>
              <button onClick={() => setShowRuleModal(false)} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>{t('authorization.cancel')}</button>
              <button onClick={createRule} disabled={saving || !ruleForm.name} style={{
                padding: '10px 20px', borderRadius: 8, border: 'none',
                background: saving || !ruleForm.name ? '#64748b' : '#3b82f6',
                color: 'white', cursor: saving ? 'not-allowed' : 'pointer', fontSize: 13, fontWeight: 600,
              }}>{saving ? t('authorization.saving') : t('authorization.save')}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function StatCard({ title, value }: { title: string; value: string }) {
  return (
    <div style={{ background: 'var(--surface)', borderRadius: 12, padding: '16px 20px', border: '1px solid var(--border)' }}>
      <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 8 }}>{title}</p>
      <p style={{ fontSize: 28, fontWeight: 700 }}>{value}</p>
    </div>
  );
}
