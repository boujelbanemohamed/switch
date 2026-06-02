import { useEffect, useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { Participant, RoutingRule } from '../types';
import { Plus, X, Pencil, Trash2, ToggleLeft, ToggleRight } from 'lucide-react';
import { SectionHeader } from '../components/SectionHeader';

const protocolOptions = ['ISO8583', 'ISO20022', 'BOTH'] as const;

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

type FormState = {
  name: string;
  description: string;
  priority: string;
  conditionExpression: string;
  sourceParticipantId: string;
  destinationParticipantId: string;
  protocol: 'ISO8583' | 'ISO20022' | 'BOTH';
  messageType: string;
  status: 'ACTIVE' | 'INACTIVE';
};

const emptyForm = (): FormState => ({
  name: '',
  description: '',
  priority: '100',
  conditionExpression: '',
  sourceParticipantId: '',
  destinationParticipantId: '',
  protocol: 'BOTH',
  messageType: '',
  status: 'ACTIVE',
});

export function RoutingRules() {
  const { t } = useTranslation();
  const [rules, setRules] = useState<RoutingRule[]>([]);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm());

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([
      api.routingRules.list(),
      api.participants.list(),
    ])
      .then(([r, p]) => { setRules(r); setParticipants(p); })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  const openCreate = () => {
    setEditingId(null);
    setForm(emptyForm());
    setShowModal(true);
  };

  const openEdit = (rule: RoutingRule) => {
    setEditingId(rule.id);
    setForm({
      name: rule.name,
      description: rule.description || '',
      priority: String(rule.priority),
      conditionExpression: rule.conditionExpression,
      sourceParticipantId: rule.sourceParticipant?.id || '',
      destinationParticipantId: rule.destinationParticipant.id,
      protocol: rule.protocol,
      messageType: rule.messageType || '',
      status: rule.status,
    });
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setEditingId(null);
    setForm(emptyForm());
  };

  const handleSubmit = async () => {
    if (!form.name || !form.conditionExpression || !form.destinationParticipantId) return;
    setSaving(true);
    try {
      const selectedSource = participants.find(p => p.id === form.sourceParticipantId);
      const selectedDest = participants.find(p => p.id === form.destinationParticipantId);
      const payload = {
        name: form.name,
        description: form.description || undefined,
        priority: parseInt(form.priority, 10),
        conditionExpression: form.conditionExpression,
        sourceParticipant: selectedSource,
        destinationParticipant: selectedDest!,
        protocol: form.protocol,
        messageType: form.messageType || undefined,
        status: form.status,
      };
      if (editingId) {
        await api.routingRules.update(editingId, payload);
      } else {
        await api.routingRules.create(payload);
      }
      closeModal();
      load();
    } catch (e) {
      console.error(e);
      alert(e instanceof Error ? e.message : 'Failed to save routing rule');
    }
    setSaving(false);
  };

  const handleDelete = async (id: string) => {
    try {
      await api.routingRules.delete(id);
      setConfirmDelete(null);
      load();
    } catch (e) {
      console.error(e);
      alert(e instanceof Error ? e.message : 'Failed to delete routing rule');
    }
  };

  const toggleStatus = async (rule: RoutingRule) => {
    const newStatus = rule.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await api.routingRules.update(rule.id, { status: newStatus } as Partial<RoutingRule>);
      load();
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h2 style={{ fontSize: 24, fontWeight: 700 }}>{t('switching.routingRules')}</h2>
        <button onClick={openCreate} style={{
          display: 'flex', alignItems: 'center', gap: 8,
          background: '#3b82f6', color: 'white', border: 'none',
          borderRadius: 8, padding: '8px 16px', fontSize: 14,
          fontWeight: 600, cursor: 'pointer',
        }}>
          <Plus size={16} /> {t('switching.routingRules')}
        </button>
      </div>

      <SectionHeader sectionKey="switching" />

      {loading ? (
        <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>
      ) : (
        <div style={{ background: 'var(--surface)', borderRadius: 12, overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                {[t('switching.name'), t('switching.priority'), t('switching.source'), t('switching.destination'), t('switching.protocol'), t('switching.messageType'), t('switching.status'), ''].map(h => (
                  <th key={h} style={{ padding: '12px 16px', fontSize: 12, color: 'var(--text-secondary)', fontWeight: 600 }}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rules.map(rule => (
                <tr key={rule.id} style={{ borderBottom: '1px solid var(--border)', transition: 'background 0.15s' }}
                  onMouseEnter={e => (e.currentTarget.style.background = 'var(--surface-2)')}
                  onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                >
                  <td style={{ padding: '12px 16px', fontWeight: 500 }}>
                    <div>{rule.name}</div>
                    {rule.description && (
                      <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginTop: 2 }}>{rule.description}</div>
                    )}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <span style={{
                      background: 'rgba(255,255,255,0.05)',
                      padding: '2px 8px',
                      borderRadius: 4,
                      fontSize: 12,
                    }}>
                      {rule.priority}
                    </span>
                  </td>
                  <td style={{ padding: '12px 16px', fontSize: 13 }}>
                    {rule.sourceParticipant?.code || <span style={{ color: 'var(--text-secondary)' }}>{t('switching.any')}</span>}
                  </td>
                  <td style={{ padding: '12px 16px', fontSize: 13 }}>
                    {rule.destinationParticipant?.code}
                  </td>
                  <td style={{ padding: '12px 16px', fontSize: 13 }}>
                    <span style={{
                      background: 'rgba(255,255,255,0.05)',
                      padding: '2px 8px', borderRadius: 4, fontSize: 11,
                      color: 'var(--text-secondary)',
                    }}>
                      {rule.protocol}
                    </span>
                  </td>
                  <td style={{ padding: '12px 16px', fontSize: 13 }}>
                    {rule.messageType || <span style={{ color: 'var(--text-secondary)' }}>{t('switching.all')}</span>}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <span style={{
                      background: rule.status === 'ACTIVE' ? 'rgba(34,197,94,0.15)' : 'rgba(148,163,184,0.15)',
                      color: rule.status === 'ACTIVE' ? '#22c55e' : '#94a3b8',
                      padding: '2px 8px',
                      borderRadius: 4,
                      fontSize: 12,
                      fontWeight: 600,
                    }}>
                      {rule.status}
                    </span>
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                      <button onClick={() => openEdit(rule)} title="Edit" style={{
                        background: 'none', border: 'none', cursor: 'pointer',
                        color: 'var(--text-secondary)', padding: 4, display: 'flex',
                      }}>
                        <Pencil size={14} />
                      </button>
                      <button onClick={() => toggleStatus(rule)} title={rule.status === 'ACTIVE' ? 'Deactivate' : 'Activate'} style={{
                        background: 'none', border: 'none', cursor: 'pointer',
                        color: rule.status === 'ACTIVE' ? '#22c55e' : '#94a3b8',
                        padding: 4, display: 'flex',
                      }}>
                        {rule.status === 'ACTIVE' ? <ToggleRight size={14} /> : <ToggleLeft size={14} />}
                      </button>
                      <button onClick={() => setConfirmDelete(rule.id)} title="Delete" style={{
                        background: 'none', border: 'none', cursor: 'pointer',
                        color: '#ef4444', padding: 4, display: 'flex',
                      }}>
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {rules.length === 0 && (
                <tr>
                  <td colSpan={8} style={{ padding: 32, textAlign: 'center', color: 'var(--text-secondary)' }}>
                    {t('switching.noRules')}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {showModal && (
        <div style={OVERLAY} onClick={closeModal}>
          <div style={MODAL} onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h3 style={{ fontSize: 18, fontWeight: 700 }}>
                {editingId ? t('participants.editParticipant') : t('switching.routingRules')}
              </h3>
              <button onClick={closeModal} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}>
                <X size={20} />
              </button>
            </div>

            <div style={{ display: 'grid', gap: 16 }}>
              <Field label={t('switching.name')}>
                <input style={INPUT} value={form.name} required
                  onChange={e => setForm({ ...form, name: e.target.value })} />
              </Field>

              <Field label="Description">
                <textarea style={{ ...INPUT, minHeight: 64, resize: 'vertical' }}
                  value={form.description}
                  onChange={e => setForm({ ...form, description: e.target.value })} />
              </Field>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                <Field label={t('switching.priority')}>
                  <input style={INPUT} type="number" value={form.priority}
                    onChange={e => setForm({ ...form, priority: e.target.value })} />
                </Field>
                <Field label={t('switching.protocol')}>
                  <select style={SELECT} value={form.protocol}
                    onChange={e => setForm({ ...form, protocol: e.target.value as 'ISO8583' | 'ISO20022' | 'BOTH' })}>
                    {protocolOptions.map(p => <option key={p} value={p}>{p}</option>)}
                  </select>
                </Field>
              </div>

              <Field label="Condition Expression">
                <input style={{ ...INPUT, fontFamily: 'monospace', fontSize: 12 }}
                  value={form.conditionExpression} required
                  placeholder="e.g. bin.startsWith('4765')"
                  onChange={e => setForm({ ...form, conditionExpression: e.target.value })} />
              </Field>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                <Field label={t('switching.source')}>
                  <select style={SELECT} value={form.sourceParticipantId}
                    onChange={e => setForm({ ...form, sourceParticipantId: e.target.value })}>
                    <option value="">{t('switching.any')}</option>
                    {participants.map(p => (
                      <option key={p.id} value={p.id}>{p.code} — {p.name}</option>
                    ))}
                  </select>
                </Field>
                <Field label={t('switching.destination')}>
                  <select style={SELECT} value={form.destinationParticipantId} required
                    onChange={e => setForm({ ...form, destinationParticipantId: e.target.value })}>
                    <option value="">—</option>
                    {participants.map(p => (
                      <option key={p.id} value={p.id}>{p.code} — {p.name}</option>
                    ))}
                  </select>
                </Field>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                <Field label={t('switching.messageType')}>
                  <input style={INPUT} value={form.messageType}
                    placeholder="e.g. 0100"
                    onChange={e => setForm({ ...form, messageType: e.target.value })} />
                </Field>
                <Field label={t('switching.status')}>
                  <select style={SELECT} value={form.status}
                    onChange={e => setForm({ ...form, status: e.target.value as 'ACTIVE' | 'INACTIVE' })}>
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="INACTIVE">INACTIVE</option>
                  </select>
                </Field>
              </div>
            </div>

            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 24 }}>
              <button onClick={closeModal} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>
                {t('participants.cancel')}
              </button>
              <button onClick={handleSubmit}
                disabled={saving || !form.name || !form.conditionExpression || !form.destinationParticipantId}
                style={{
                  padding: '10px 20px', borderRadius: 8, border: 'none',
                  background: saving || !form.name || !form.conditionExpression || !form.destinationParticipantId
                    ? '#64748b' : '#3b82f6',
                  color: 'white',
                  cursor: saving || !form.name || !form.conditionExpression || !form.destinationParticipantId
                    ? 'not-allowed' : 'pointer',
                  fontSize: 13, fontWeight: 600,
                }}>
                {saving ? t('participants.saving') : editingId ? t('participants.update') : t('participants.save')}
              </button>
            </div>
          </div>
        </div>
      )}

      {confirmDelete && (
        <div style={OVERLAY} onClick={() => setConfirmDelete(null)}>
          <div style={{ ...MODAL, width: 380 }} onClick={e => e.stopPropagation()}>
            <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 12 }}>Confirm Delete</h3>
            <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 20 }}>
              Are you sure you want to delete this routing rule? This action cannot be undone.
            </p>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
              <button onClick={() => setConfirmDelete(null)} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>
                {t('participants.cancel')}
              </button>
              <button onClick={() => handleDelete(confirmDelete)} style={{
                padding: '10px 20px', borderRadius: 8, border: 'none',
                background: '#ef4444', color: 'white', cursor: 'pointer',
                fontSize: 13, fontWeight: 600,
              }}>
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
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
