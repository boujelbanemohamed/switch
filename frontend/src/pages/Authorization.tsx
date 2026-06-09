import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { AuthRule, AuthDecision, HoldRecord, Condition, ConditionField, ConditionOperator } from '../types';
import { CONDITION_FIELDS, OPERATORS_BY_FIELD, IS_MULTI_VALUE_OP, IS_RANGE_OP } from '../types';
import { AuthorizationHelp, RULE_TYPE_LABELS, RULE_STATUS_LABELS, ACTION_LABELS, DECISION_LABELS, HOLD_STATUS_LABELS } from '../components/AuthorizationHelp';
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

function Field({ label, children, required }: { label: string; children: React.ReactNode; required?: boolean }) {
  return (
    <div>
      <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4, fontWeight: 500 }}>
        {label}{required && <span style={{ color: '#ef4444', marginLeft: 2 }}>*</span>}
      </label>
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

function StatusBadge({ status, label }: { status: string; label?: string }) {
  const colors: Record<string, string> = {
    ACTIVE: '#22c55e', INACTIVE: '#64748b', ACTIVE_HOLD: '#3b82f6',
    RELEASED: '#22c55e', EXPIRED: '#6b7280', CAPTURED: '#8b5cf6',
  };
  const color = colors[status] || '#6b7280';
  return <span style={{ background: color + '33', color, padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600 }}>{label ?? status}</span>;
}

export function Authorization() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('rules');
  const [rules, setRules] = useState<AuthRule[]>([]);
  const [decisions, setDecisions] = useState<AuthDecision[]>([]);
  const [holds, setHolds] = useState<HoldRecord[]>([]);
  const [loading, setLoading] = useState(true);

  const [showRuleModal, setShowRuleModal] = useState(false);
  const [editingRule, setEditingRule] = useState<AuthRule | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<{ id: string; name: string } | null>(null);
  const [saving, setSaving] = useState(false);
  const [ruleForm, setRuleForm] = useState({
    name: '', ruleType: 'LIMIT', conditionExpression: '{}', priority: '1',
    action: 'APPROVE', timeRestrictions: '', status: 'ACTIVE',
  });

  const [simForm, setSimForm] = useState({ cardNumber: '', amount: '', merchant: '' });
  const [simResult, setSimResult] = useState<AuthDecision | null>(null);

  const [conditions, setConditions] = useState<Condition[]>([]);
  const [conditionErrors, setConditionErrors] = useState<boolean[]>([]);
  const [formError, setFormError] = useState('');

  const parseConditions = useCallback((json: string): Condition[] => {
    if (!json || json === '{}') return [];
    try {
      const parsed = JSON.parse(json);
      if (Array.isArray(parsed)) return parsed.filter((c: any) => c && c.field && c.operator);
      return [];
    } catch { return []; }
  }, []);

  const serializeConditions = useCallback((conds: Condition[]): string => {
    return JSON.stringify(conds);
  }, []);

  const validateCondition = useCallback((c: Condition): boolean => {
    if (!c.field || !c.operator) return false;
    if (!c.value || (typeof c.value === 'string' && c.value.trim() === '')) return false;
    return true;
  }, []);

  const validateAllConditions = useCallback((conds: Condition[]): boolean[] => {
    return conds.map(c => validateCondition(c));
  }, [validateCondition]);

  useEffect(() => {
    if (showRuleModal) {
      const parsed = parseConditions(ruleForm.conditionExpression);
      setConditions(parsed);
      setConditionErrors([]);
    }
  }, [showRuleModal, ruleForm.conditionExpression, parseConditions]);

  const syncConditionsToForm = useCallback((conds: Condition[]) => {
    setConditions(conds);
    setRuleForm(prev => ({ ...prev, conditionExpression: serializeConditions(conds) }));
    const errors = validateAllConditions(conds);
    setConditionErrors(errors);
  }, [validateAllConditions, serializeConditions]);

  const isConditionValid = useCallback((c: Condition): boolean => {
    return validateCondition(c);
  }, [validateCondition]);

  const canSubmit = ruleForm.name.trim() !== '' && conditions.every(c => isConditionValid(c));

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

  const openEditModal = (rule: AuthRule) => {
    setEditingRule(rule);
    setRuleForm({
      name: rule.name,
      ruleType: rule.ruleType,
      conditionExpression: rule.conditionExpression || '{}',
      priority: String(rule.priority),
      action: rule.action,
      timeRestrictions: '',
      status: rule.status,
    });
    setShowRuleModal(true);
    setFormError('');
  };

  const createRule = async () => {
    setFormError('');
    if (!ruleForm.name.trim()) { setFormError(t('authorization.errorNameRequired')); setSaving(false); return; }
    if (!ruleForm.ruleType) { setFormError(t('authorization.errorTypeRequired')); setSaving(false); return; }
    if (!ruleForm.priority || parseInt(ruleForm.priority) < 1) { setFormError(t('authorization.errorPriorityRequired')); setSaving(false); return; }
    setSaving(true);
    try {
      const payload = {
        name: ruleForm.name,
        ruleType: ruleForm.ruleType,
        action: ruleForm.action,
        priority: parseInt(ruleForm.priority) || 1,
        status: ruleForm.status,
        conditionExpression: ruleForm.conditionExpression,
      };
      if (editingRule) {
        await api.authorization.rules.update(editingRule.id, payload);
      } else {
        await api.authorization.rules.create(payload);
      }
      setShowRuleModal(false);
      setEditingRule(null);
      setRuleForm({ name: '', ruleType: 'LIMIT', conditionExpression: '{}', priority: '1', action: 'APPROVE', timeRestrictions: '', status: 'ACTIVE' });
      await reloadRules();
    } catch (e) { console.error(e); await reloadRules(); alert(e instanceof Error ? e.message : 'Failed to save rule'); }
    setSaving(false);
  };

  const reloadRules = async () => {
    try {
      const list = await api.authorization.rules.list();
      setRules(list);
    } catch { /* ignore */ }
  };

  const toggleRuleStatus = async (rule: AuthRule) => {
    const newStatus = rule.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await api.authorization.rules.update(rule.id, { status: newStatus });
      await reloadRules();
    } catch (e) {
      await reloadRules();
      alert(e instanceof Error ? e.message : 'Failed to toggle rule status');
    }
  };

  const deleteRule = async () => {
    if (!deleteConfirm) return;
    try {
      await api.authorization.rules.remove(deleteConfirm.id);
      setDeleteConfirm(null);
      await reloadRules();
    } catch (e) {
      await reloadRules();
      alert(e instanceof Error ? e.message : 'Failed to delete rule');
    }
  };

  const addRule = () => {
    setEditingRule(null);
    setRuleForm({ name: '', ruleType: 'LIMIT', conditionExpression: '{}', priority: '1', action: 'APPROVE', timeRestrictions: '', status: 'ACTIVE' });
    setShowRuleModal(true);
    setFormError('');
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
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 24 }}>
        <h2 style={{ fontSize: 24, fontWeight: 700, margin: 0 }}>{t('authorization.title')}</h2>
        <AuthorizationHelp />
      </div>
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
            <button onClick={addRule} style={{
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
                  <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('authorization.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {rules.map(r => (
                  <tr key={r.id} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '12px 16px', fontWeight: 600 }}>{r.name}</td>
                    <td style={{ padding: '12px 16px', color: 'var(--text-secondary)' }}>{RULE_TYPE_LABELS[r.ruleType] ?? r.ruleType}</td>
                    <td style={{ padding: '12px 16px' }}>-</td>
                    <td style={{ padding: '12px 16px', fontFamily: 'monospace' }}>{r.priority}</td>
                    <td style={{ padding: '12px 16px' }}><StatusBadge status={r.status} label={RULE_STATUS_LABELS[r.status] ?? r.status} /></td>
                    <td style={{ padding: '12px 16px', color: '#22c55e', fontWeight: 600 }}>{r.successCount}</td>
                    <td style={{ padding: '12px 16px', color: '#ef4444', fontWeight: 600 }}>{r.failureCount}</td>
                    <td style={{ padding: '12px 16px', display: 'flex', gap: 4 }}>
                      <MiniBtn label={t('authorization.editRule')} color="#3b82f6" onClick={() => openEditModal(r)} />
                      <MiniBtn label={t(r.status === 'ACTIVE' ? 'authorization.deactivate' : 'authorization.activate')} color="#f59e0b" onClick={() => toggleRuleStatus(r)} />
                      <MiniBtn label={t('authorization.deleteRule')} color="#ef4444" onClick={() => setDeleteConfirm({ id: r.id, name: r.name })} />
                    </td>
                  </tr>
                ))}
                  {rules.length === 0 && (
                  <tr><td colSpan={8} style={{ padding: 32, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('authorization.noRules')}</td></tr>
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
                        }}>{DECISION_LABELS[d.decision] ?? d.decision}</span>
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
                      <td style={{ padding: '10px 12px' }}><StatusBadge status={h.status} label={HOLD_STATUS_LABELS[h.status] ?? h.status} /></td>
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
                    }}>{DECISION_LABELS[simResult.decision] ?? simResult.decision}</span>
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
        <div style={styles.overlay} onClick={() => { setShowRuleModal(false); setEditingRule(null); }}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 20 }}>{editingRule ? t('authorization.editRule') : t('authorization.addRule')}</h3>
            <div style={{ display: 'grid', gap: 14 }}>
              <Field label={t('authorization.name')} required><input style={styles.input} value={ruleForm.name} onChange={e => setRuleForm({ ...ruleForm, name: e.target.value })} /></Field>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('authorization.type')} required>
                  <select style={styles.select} value={ruleForm.ruleType} onChange={e => setRuleForm({ ...ruleForm, ruleType: e.target.value })}>
                    {['LIMIT', 'VELOCITY', 'GEO', 'FRAUD', 'MERCHANT', 'RISK', 'PRODUCT', 'SOLDE', 'TIME', 'CUSTOM'].map(t => <option key={t} value={t}>{RULE_TYPE_LABELS[t] ?? t}</option>)}
                  </select>
                </Field>
                <Field label={t('authorization.action')}>
                  <select style={styles.select} value={ruleForm.action} onChange={e => setRuleForm({ ...ruleForm, action: e.target.value })}>
                    {['APPROVE', 'DECLINE', 'REVIEW', 'CHALLENGE', 'TFA', 'PIN'].map(a => <option key={a} value={a}>{ACTION_LABELS[a] ?? a}</option>)}
                  </select>
                </Field>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('authorization.priority')} required><input style={styles.input} type="number" value={ruleForm.priority} onChange={e => setRuleForm({ ...ruleForm, priority: e.target.value })} /></Field>
                <Field label={t('authorization.status')}>
                  <select style={styles.select} value={ruleForm.status} onChange={e => setRuleForm({ ...ruleForm, status: e.target.value })}>
                    {['ACTIVE', 'INACTIVE'].map(s => <option key={s} value={s}>{RULE_STATUS_LABELS[s] ?? s}</option>)}
                  </select>
                </Field>
              </div>
              <div>
                <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 6, fontWeight: 500 }}>
                  {t('authorization.conditions')}
                </label>
                <div style={{ display: 'flex', gap: 6, flexDirection: 'column' }}>
                  {conditions.map((c, idx) => {
                    const ops = OPERATORS_BY_FIELD[c.field as ConditionField] || ['EQUAL'];
                    const isMulti = IS_MULTI_VALUE_OP.includes(c.operator as ConditionOperator);
                    const isRange = IS_RANGE_OP.includes(c.operator as ConditionOperator);
                    const hasError = conditionErrors[idx];
                    return (
                      <div key={idx}>
                        {idx > 0 && (
                          <div style={{ textAlign: 'center', fontSize: 11, fontWeight: 700, color: 'var(--text-secondary)', padding: '4px 0', letterSpacing: 1 }}>
                            {t('authorization.conditionAnd')}
                          </div>
                        )}
                        <div style={{
                          display: 'grid', gridTemplateColumns: '1fr auto 1fr auto', gap: 6, alignItems: 'start',
                          padding: 8, borderRadius: 8, border: `1px solid ${hasError ? '#ef4444' : 'var(--border)'}`,
                          background: hasError ? '#ef444408' : 'transparent',
                        }}>
                          <div>
                            <select style={{ ...styles.select, fontSize: 12, padding: '6px 8px' }}
                              value={c.field} onChange={e => {
                                const field = e.target.value as ConditionField;
                                const newOps = OPERATORS_BY_FIELD[field] || ['EQUAL'];
                                const newOp = newOps.includes(c.operator as ConditionOperator) ? c.operator : newOps[0];
                                const updated = conditions.map((cc, i) => i === idx ? { field, operator: newOp as ConditionOperator, value: '' } : cc);
                                syncConditionsToForm(updated);
                              }}>
                              <option value="" disabled>{t('authorization.conditionField')}</option>
                              {CONDITION_FIELDS.map(f => (
                                <option key={f} value={f}>{t(`authorization.conditionField_${f}`)}</option>
                              ))}
                            </select>
                          </div>
                          <div>
                            <select style={{ ...styles.select, fontSize: 12, padding: '6px 8px' }}
                              value={c.operator} onChange={e => {
                                const op = e.target.value as ConditionOperator;
                                const updated = conditions.map((cc, i) => i === idx ? { ...cc, operator: op, value: IS_RANGE_OP.includes(op) ? '' : IS_MULTI_VALUE_OP.includes(op) ? '' : '' } : cc);
                                syncConditionsToForm(updated);
                              }}>
                              {ops.map(op => (
                                <option key={op} value={op}>{t(`authorization.conditionOp_${op}`)}</option>
                              ))}
                            </select>
                          </div>
                          <div>
                            {isRange ? (
                              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 4 }}>
                                <input style={{ ...styles.input, fontSize: 12, padding: '6px 8px' }}
                                  value={c.value?.split(',')[0] || ''} onChange={e => {
                                    const parts = (c.value || '').split(',');
                                    const val = `${e.target.value},${parts[1] || ''}`;
                                    const updated = conditions.map((cc, i) => i === idx ? { ...cc, value: val } : cc);
                                    syncConditionsToForm(updated);
                                  }} placeholder={t('authorization.conditionMinPlaceholder')} />
                                <input style={{ ...styles.input, fontSize: 12, padding: '6px 8px' }}
                                  value={c.value?.split(',')[1] || ''} onChange={e => {
                                    const parts = (c.value || '').split(',');
                                    const val = `${parts[0] || ''},${e.target.value}`;
                                    const updated = conditions.map((cc, i) => i === idx ? { ...cc, value: val } : cc);
                                    syncConditionsToForm(updated);
                                  }} placeholder={t('authorization.conditionMaxPlaceholder')} />
                              </div>
                            ) : isMulti ? (
                              <TagsInput value={c.value} onChange={val => {
                                const updated = conditions.map((cc, i) => i === idx ? { ...cc, value: val } : cc);
                                syncConditionsToForm(updated);
                              }} placeholder={t('authorization.conditionTagsPlaceholder')} />
                            ) : (
                              <input style={{ ...styles.input, fontSize: 12, padding: '6px 8px' }}
                                value={c.value || ''} onChange={e => {
                                  const updated = conditions.map((cc, i) => i === idx ? { ...cc, value: e.target.value } : cc);
                                  syncConditionsToForm(updated);
                                }} placeholder={t('authorization.conditionValuePlaceholder')} />
                            )}
                          </div>
                          <button type="button" onClick={() => {
                            const updated = conditions.filter((_, i) => i !== idx);
                            syncConditionsToForm(updated);
                          }} title={t('authorization.conditionRemove')} style={{
                            background: 'transparent', border: 'none', color: '#ef4444', cursor: 'pointer',
                            fontSize: 16, fontWeight: 700, padding: '4px 6px', lineHeight: 1,
                          }}>×</button>
                        </div>
                        {hasError && (
                          <div style={{ fontSize: 11, color: '#ef4444', marginTop: 2, paddingLeft: 4 }}>
                            {t('authorization.conditionRequired')}
                          </div>
                        )}
                      </div>
                    );
                  })}
                  <button type="button" onClick={() => {
                    const updated = [...conditions, { field: '' as any, operator: 'EQUAL' as ConditionOperator, value: '' }];
                    syncConditionsToForm(updated);
                  }} style={{
                    display: 'flex', alignItems: 'center', gap: 4, background: 'transparent',
                    border: `1px dashed var(--border)`, borderRadius: 6, padding: '6px 12px',
                    color: '#3b82f6', cursor: 'pointer', fontSize: 12, fontWeight: 600, justifyContent: 'center',
                  }}>
                    + {t('authorization.conditionAdd')}
                  </button>
                </div>
              </div>
              <Field label={t('authorization.timeRestrictions')}>
                <input style={styles.input} value={ruleForm.timeRestrictions} onChange={e => setRuleForm({ ...ruleForm, timeRestrictions: e.target.value })} placeholder="e.g. 08:00-18:00" />
              </Field>
            </div>
            {formError && <p style={{ color: '#ef4444', fontSize: 12, margin: 0 }}>{formError}</p>}
            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: formError ? 12 : 24 }}>
              <button onClick={() => { setShowRuleModal(false); setEditingRule(null); }} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>{t('authorization.cancel')}</button>
              <button onClick={createRule} disabled={saving || !ruleForm.name || !canSubmit} style={{
                padding: '10px 20px', borderRadius: 8, border: 'none',
                background: saving || !ruleForm.name || !canSubmit ? '#64748b' : '#3b82f6',
                color: 'white', cursor: saving ? 'not-allowed' : 'pointer', fontSize: 13, fontWeight: 600,
              }}>{saving ? t('authorization.saving') : t('authorization.save')}</button>
            </div>
          </div>
        </div>
      )}

      {deleteConfirm && (
        <div style={styles.overlay} onClick={() => setDeleteConfirm(null)}>
          <div style={{ ...styles.modal, width: 400 }} onClick={e => e.stopPropagation()}>
            <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 12 }}>{t('authorization.deleteRule')}</h3>
            <p style={{ fontSize: 14, color: 'var(--text-secondary)', marginBottom: 24 }}>
              {t('authorization.confirmDelete', { name: deleteConfirm.name })}
            </p>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
              <button onClick={() => setDeleteConfirm(null)} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>{t('authorization.cancel')}</button>
              <button onClick={deleteRule} style={{
                padding: '10px 20px', borderRadius: 8, border: 'none',
                background: '#ef4444', color: 'white', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>{t('authorization.deleteRule')}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function TagsInput({ value, onChange, placeholder }: { value: string; onChange: (v: string) => void; placeholder: string }) {
  const tags = value ? value.split(',').filter(Boolean) : [];
  const [inputVal, setInputVal] = useState('');
  const addTag = (tag: string) => {
    const trimmed = tag.trim();
    if (trimmed && !tags.includes(trimmed)) {
      onChange([...tags, trimmed].join(','));
    }
    setInputVal('');
  };
  const removeTag = (tag: string) => {
    onChange(tags.filter(t => t !== tag).join(','));
  };
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, padding: '4px 6px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', minHeight: 30, alignItems: 'center', cursor: 'text' }}
      onClick={e => { if (e.target === e.currentTarget) (e.currentTarget.querySelector('input') as HTMLInputElement)?.focus(); }}>
      {tags.map(tag => (
        <span key={tag} style={{ display: 'inline-flex', alignItems: 'center', gap: 2, background: '#3b82f620', color: '#3b82f6', borderRadius: 4, padding: '1px 6px', fontSize: 11, fontWeight: 600 }}>
          {tag}
          <button type="button" onClick={() => removeTag(tag)} style={{ background: 'none', border: 'none', color: '#3b82f6', cursor: 'pointer', padding: 0, fontSize: 14, lineHeight: 1, opacity: 0.6 }}>×</button>
        </span>
      ))}
      <input value={inputVal} onChange={e => setInputVal(e.target.value)}
        onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addTag(inputVal); } if (e.key === ',' ) { e.preventDefault(); addTag(inputVal); } if (e.key === 'Backspace' && !inputVal && tags.length) removeTag(tags[tags.length - 1]); }}
        onBlur={() => { if (inputVal.trim()) addTag(inputVal); }}
        placeholder={tags.length === 0 ? placeholder : ''}
        style={{ border: 'none', outline: 'none', background: 'transparent', color: 'var(--text)', fontSize: 12, flex: 1, minWidth: 60, padding: 0 }} />
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
