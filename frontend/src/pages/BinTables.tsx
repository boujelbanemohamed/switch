import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { BinTable, Participant } from '../types';
import { SectionHeader } from '../components/SectionHeader';
import { ChevronDown, ChevronRight, Terminal, Plus, X, Pencil, Trash2, ToggleLeft, ToggleRight } from 'lucide-react';

const brandColors: Record<string, string> = {
  VISA: '#1a1f71',
  MASTERCARD: '#eb001b',
  AMEX: '#2e77bc',
  CB: '#0066b3',
  OTHER: '#64748b',
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

const BIN_RE = /^\d{4,8}$/;

const cardSchemes = ['VISA', 'MASTERCARD', 'AMEX', 'CB', 'OTHER'] as const;
const cardTypes = ['DEBIT', 'CREDIT', 'PREPAID'] as const;

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

type FormState = {
  binPrefix: string;
  cardScheme: string;
  issuingCountry: string;
  cardType: string;
  currency: string;
  participantId: string;
};

const emptyForm = (): FormState => ({
  binPrefix: '',
  cardScheme: 'VISA',
  issuingCountry: 'TN',
  cardType: 'CREDIT',
  currency: 'TND',
  participantId: '',
});

export function BinTables() {
  const { t } = useTranslation();
  const [tables, setTables] = useState<BinTable[]>([]);
  const [loading, setLoading] = useState(true);
  const [guideOpen, setGuideOpen] = useState(false);
  const [participantsOpen, setParticipantsOpen] = useState(false);

  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState<FormState>(emptyForm());
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    Promise.all([
      api.binTables.list(),
      api.participants.list(),
    ])
      .then(([bins, ps]) => { setTables(bins); setParticipants(ps); })
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const openCreate = () => {
    setEditingId(null);
    setForm(emptyForm());
    setFeedback(null);
    setShowModal(true);
  };

  const openEdit = (bt: BinTable) => {
    setEditingId(bt.id);
    setForm({
      binPrefix: bt.bin,
      cardScheme: bt.cardBrand || 'VISA',
      issuingCountry: bt.countryCode || 'TN',
      cardType: bt.cardType || 'CREDIT',
      currency: bt.currencyCode || 'TND',
      participantId: bt.participant?.id || '',
    });
    setFeedback(null);
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setEditingId(null);
    setForm(emptyForm());
    setFeedback(null);
  };

  const isFormValid = () => {
    return (
      BIN_RE.test(form.binPrefix) &&
      form.issuingCountry.length === 2 &&
      form.currency.length === 3 &&
      form.participantId
    );
  };

  const handleSubmit = async () => {
    if (!isFormValid()) return;
    setSaving(true);
    setFeedback(null);
    try {
      const selectedParticipant = participants.find(p => p.id === form.participantId);
      if (!selectedParticipant) throw new Error('Participant not found');
      const payload = {
        bin: form.binPrefix,
        binLength: form.binPrefix.length,
        participant: selectedParticipant,
        cardBrand: form.cardScheme as BinTable['cardBrand'],
        cardType: form.cardType as BinTable['cardType'],
        countryCode: form.issuingCountry,
        currencyCode: form.currency,
      };
      if (editingId) {
        await api.binTables.update(editingId, payload);
        setFeedback({ type: 'success', message: t('binTables.updated') });
      } else {
        await api.binTables.create({ ...payload, isActive: true });
        setFeedback({ type: 'success', message: t('binTables.created') });
      }
      closeModal();
      load();
    } catch (e) {
      setFeedback({
        type: 'error',
        message: e instanceof Error ? e.message : (editingId ? t('binTables.updateError') : t('binTables.createError')),
      });
    }
    setSaving(false);
  };

  const toggleStatus = async (bt: BinTable) => {
    try {
      await api.binTables.update(bt.id, { isActive: !bt.isActive } as Partial<BinTable>);
      load();
    } catch (e) {
      console.error(e);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await api.binTables.delete(id);
      setConfirmDelete(null);
      load();
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h2 style={{ fontSize: 24, fontWeight: 700 }}>{t('nav.binTables')}</h2>
        <button onClick={openCreate} style={{
          display: 'flex', alignItems: 'center', gap: 8,
          background: '#3b82f6', color: 'white', border: 'none',
          borderRadius: 8, padding: '8px 16px', fontSize: 14,
          fontWeight: 600, cursor: 'pointer',
        }}>
          <Plus size={16} /> {t('binTables.addBin')}
        </button>
      </div>

      <SectionHeader sectionKey="binTables" />

      <div style={{ marginBottom: 24 }}>
        <button
          onClick={() => setGuideOpen(!guideOpen)}
          style={{
            display: 'flex', alignItems: 'center', gap: 8,
            background: 'var(--surface)', border: '1px solid var(--border)',
            borderRadius: 10, padding: '8px 16px', width: '100%',
            cursor: 'pointer', color: 'var(--text)', fontSize: 13, fontWeight: 500,
          }}
        >
          {guideOpen ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
          <Terminal size={14} />
          <span>Comment ajouter un BIN ?</span>
        </button>

        {guideOpen && (
          <div style={{ marginTop: 8, padding: 20, background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 10 }}>
            <div style={{ marginBottom: 16 }}>
              <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8 }}>Via l'API REST (curl)</div>
              <pre style={{ fontSize: 12, background: 'var(--bg)', padding: 12, borderRadius: 8, overflowX: 'auto', lineHeight: 1.6 }}>
{`curl -X POST \${window.location.origin}/api/v1/admin/bin-tables \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer $TOKEN" \\
  -d '{
    "bin": "123456",
    "binLength": 6,
    "participantId": "UUID_DU_PARTICIPANT",
    "cardBrand": "VISA",
    "cardType": "CREDIT",
    "countryCode": "TN",
    "currencyCode": "TND"
  }'`}
              </pre>
            </div>

            <div style={{ marginBottom: participantsOpen ? 16 : 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8 }}>Via SQL</div>
              <pre style={{ fontSize: 12, background: 'var(--bg)', padding: 12, borderRadius: 8, overflowX: 'auto', lineHeight: 1.6 }}>
{`INSERT INTO bin_tables (bin, bin_length, participant_id, card_brand, card_type, country_code, currency_code)
SELECT '123456', 6, id, 'VISA', 'CREDIT', 'TN', 'TND'
FROM participants WHERE code = 'SIB';`}
              </pre>
            </div>

            <div>
              <button
                onClick={() => setParticipantsOpen(!participantsOpen)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#3b82f6', fontSize: 13, fontWeight: 600, padding: 0 }}
              >
                {participantsOpen ? '▼' : '▶'} Voir les participants disponibles
              </button>
              {participantsOpen && (
                <div style={{ marginTop: 8 }}>
                  {participants.length === 0 ? (
                    <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                      Aucun participant chargé
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                      {participants.map(p => (
                        <div key={p.id} style={{ fontSize: 12, padding: '4px 8px', background: 'var(--bg)', borderRadius: 6 }}>
                          <strong>{p.code}</strong> — {p.name} <span style={{ color: 'var(--text-secondary)', fontFamily: 'monospace' }}>({p.id})</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {loading ? (
        <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 16 }}>
          {tables.map(bt => (
            <div key={bt.id} style={{
              background: 'var(--surface)',
              borderRadius: 12,
              padding: 20,
              border: '1px solid var(--border)',
              opacity: bt.isActive ? 1 : 0.5,
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
                <span style={{ fontSize: 18, fontWeight: 700, fontFamily: 'monospace' }}>
                  {bt.bin}{'*'.repeat(16 - bt.binLength)}
                </span>
                {bt.cardBrand && (
                  <span style={{
                    background: brandColors[bt.cardBrand] || '#64748b',
                    color: '#fff',
                    padding: '3px 10px',
                    borderRadius: 5,
                    fontSize: 12,
                    fontWeight: 700,
                    letterSpacing: '0.02em',
                  }}>
                    {bt.cardBrand === 'VISA' ? 'VISA' :
                     bt.cardBrand === 'MASTERCARD' ? 'MASTERCARD' :
                     bt.cardBrand === 'AMEX' ? 'AMEX' :
                     bt.cardBrand === 'CB' ? 'CB' : bt.cardBrand}
                  </span>
                )}
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, fontSize: 13 }}>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>{t('binTables.length')}: </span>
                  {bt.binLength}
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>{t('binTables.type')}: </span>
                  {bt.cardType || t('binTables.na')}
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>{t('binTables.country')}: </span>
                  {bt.countryCode || t('binTables.na')}
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>{t('binTables.currency')}: </span>
                  {bt.currencyCode || t('binTables.na')}
                </div>
              </div>

              <div style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                  {bt.participant?.name || bt.participant?.code}
                </span>
                <div style={{ display: 'flex', gap: 4 }}>
                  <button onClick={() => openEdit(bt)} title="Edit" style={{
                    background: 'none', border: 'none', cursor: 'pointer',
                    color: 'var(--text-secondary)', padding: 4, display: 'flex',
                  }}>
                    <Pencil size={14} />
                  </button>
                  <button onClick={() => toggleStatus(bt)} title={bt.isActive ? 'Deactivate' : 'Activate'} style={{
                    background: 'none', border: 'none', cursor: 'pointer',
                    color: bt.isActive ? '#22c55e' : '#94a3b8',
                    padding: 4, display: 'flex',
                  }}>
                    {bt.isActive ? <ToggleRight size={14} /> : <ToggleLeft size={14} />}
                  </button>
                  <button onClick={() => setConfirmDelete(bt.id)} title="Delete" style={{
                    background: 'none', border: 'none', cursor: 'pointer',
                    color: '#ef4444', padding: 4, display: 'flex',
                  }}>
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <div style={OVERLAY} onClick={closeModal}>
          <div style={MODAL} onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h3 style={{ fontSize: 18, fontWeight: 700 }}>
                {editingId ? t('binTables.editBin') : t('binTables.createBin')}
              </h3>
              <button onClick={closeModal} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}>
                <X size={20} />
              </button>
            </div>

            <div style={{ display: 'grid', gap: 16 }}>
              <Field label={t('binTables.binPrefix')}>
                <input
                  style={{ ...INPUT, ...(form.binPrefix && !BIN_RE.test(form.binPrefix) ? { borderColor: '#ef4444' } : {}) }}
                  value={form.binPrefix}
                  maxLength={8}
                  placeholder="123456"
                  onChange={e => setForm({ ...form, binPrefix: e.target.value.replace(/\D/g, '') })}
                />
                {form.binPrefix && form.binPrefix.length > 0 && !BIN_RE.test(form.binPrefix) && (
                  <div style={{ fontSize: 11, color: '#ef4444', marginTop: 2 }}>4 to 8 digits required</div>
                )}
              </Field>

              <Field label={t('binTables.cardScheme')}>
                <select style={SELECT} value={form.cardScheme}
                  onChange={e => setForm({ ...form, cardScheme: e.target.value })}>
                  {cardSchemes.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
              </Field>

              <Field label={t('binTables.participant')}>
                <select style={SELECT} value={form.participantId}
                  onChange={e => setForm({ ...form, participantId: e.target.value })}>
                  <option value="">{t('binTables.selectParticipant')}</option>
                  {participants.map(p => (
                    <option key={p.id} value={p.id}>{p.code} — {p.name}</option>
                  ))}
                </select>
              </Field>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                <Field label={t('binTables.issuingCountry')}>
                  <input
                    style={{ ...INPUT, textTransform: 'uppercase' }}
                    value={form.issuingCountry}
                    maxLength={2}
                    placeholder="TN"
                    onChange={e => setForm({ ...form, issuingCountry: e.target.value.toUpperCase().replace(/[^A-Z]/g, '') })}
                  />
                </Field>
                <Field label={t('binTables.cardType')}>
                  <select style={SELECT} value={form.cardType}
                    onChange={e => setForm({ ...form, cardType: e.target.value })}>
                    {cardTypes.map(t => <option key={t} value={t}>{t}</option>)}
                  </select>
                </Field>
              </div>

              <Field label={t('binTables.currency')}>
                <input
                  style={{ ...INPUT, textTransform: 'uppercase' }}
                  value={form.currency}
                  maxLength={3}
                  placeholder="TND"
                  onChange={e => setForm({ ...form, currency: e.target.value.toUpperCase().replace(/[^A-Z]/g, '') })}
                />
              </Field>
            </div>

            {feedback && (
              <div style={{
                marginTop: 16, padding: '10px 14px', borderRadius: 8, fontSize: 13,
                background: feedback.type === 'success' ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)',
                color: feedback.type === 'success' ? '#22c55e' : '#ef4444',
              }}>
                {feedback.message}
              </div>
            )}

            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 24 }}>
              <button onClick={closeModal} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>
                {t('participants.cancel')}
              </button>
              <button onClick={handleSubmit}
                disabled={saving || !isFormValid()}
                style={{
                  padding: '10px 20px', borderRadius: 8, border: 'none',
                  background: saving || !isFormValid() ? '#64748b' : '#3b82f6',
                  color: 'white',
                  cursor: saving || !isFormValid() ? 'not-allowed' : 'pointer',
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
              {t('binTables.deleteConfirm')}
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
