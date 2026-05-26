import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { Participant } from '../types';
import { Plus, X } from 'lucide-react';
import { SectionHeader } from '../components/SectionHeader';

const typeColors: Record<string, string> = {
  ACQUIRER: '#22c55e',
  ISSUER: '#3b82f6',
  SWITCH: '#a855f7',
  PROCESSOR: '#f97316',
};

const statusColors: Record<string, string> = {
  ACTIVE: '#22c55e',
  INACTIVE: '#94a3b8',
  SUSPENDED: '#ef4444',
};

const OVERLAY: React.CSSProperties = {
  position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)',
  display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100,
};

const MODAL: React.CSSProperties = {
  background: 'var(--surface)', borderRadius: 16, padding: 28,
  width: 480, maxWidth: '90vw', maxHeight: '85vh', overflow: 'auto',
  border: '1px solid var(--border)',
};

const INPUT: React.CSSProperties = {
  width: '100%', padding: '10px 12px', borderRadius: 8,
  border: '1px solid var(--border)', background: 'var(--bg)',
  color: 'var(--text)', fontSize: 13, boxSizing: 'border-box',
};

const SELECT: React.CSSProperties = { ...INPUT, cursor: 'pointer' };

export function Participants() {
  const { t } = useTranslation();
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState<{
    name: string; code: string; type: string; status: string;
    endpointUrl: string; supportedProtocols: string;
  }>({
    name: '', code: '', type: 'ISSUER', status: 'ACTIVE',
    endpointUrl: '', supportedProtocols: '',
  });

  const load = () => {
    setLoading(true);
    api.participants.list()
      .then(setParticipants)
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleSubmit = async () => {
    setSaving(true);
    try {
      const pType = form.type as Participant['type'];
      const pStatus = form.status as Participant['status'];
      await api.participants.create({
        name: form.name,
        code: form.code,
        type: pType,
        status: pStatus,
        endpointUrl: form.endpointUrl || undefined,
        supportedProtocols: form.supportedProtocols
          ? form.supportedProtocols.split(',').map(s => s.trim())
          : [],
      });
      setShowModal(false);
      setForm({ name: '', code: '', type: 'ISSUER', status: 'ACTIVE', endpointUrl: '', supportedProtocols: '' });
      load();
    } catch (e) {
      console.error(e);
      alert(e instanceof Error ? e.message : t('participants.createError'));
    }
    setSaving(false);
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h2 style={{ fontSize: 24, fontWeight: 700 }}>{t('participants.title')}</h2>
        <button onClick={() => setShowModal(true)} style={{
          display: 'flex', alignItems: 'center', gap: 8,
          background: '#3b82f6', color: 'white', border: 'none',
          borderRadius: 8, padding: '8px 16px', fontSize: 14,
          fontWeight: 600, cursor: 'pointer',
        }}>
          <Plus size={16} /> {t('participants.addParticipant')}
        </button>
      </div>

      <SectionHeader sectionKey="participants" />

      {loading ? (
        <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: 16 }}>
          {participants.map(p => (
            <div key={p.id} style={{
              background: 'var(--surface)', borderRadius: 12, padding: 20,
              border: '1px solid var(--border)',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 12 }}>
                <div>
                  <p style={{ fontSize: 16, fontWeight: 600 }}>{p.name}</p>
                  <p style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{p.code}</p>
                </div>
                <span style={{
                  background: `${typeColors[p.type]}22`, color: typeColors[p.type],
                  padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                }}>
                  {p.type}
                </span>
              </div>
              <div style={{ display: 'flex', gap: 16, fontSize: 13, color: 'var(--text-secondary)' }}>
                <span>{t('participants.status')}: <span style={{ color: statusColors[p.status], fontWeight: 600 }}>{p.status}</span></span>
                {p.endpointUrl && <span>{t('participants.endpoint')}: {p.endpointUrl}</span>}
              </div>
              {p.supportedProtocols && p.supportedProtocols.length > 0 && (
                <div style={{ marginTop: 12, display: 'flex', gap: 6 }}>
                  {p.supportedProtocols.map(proto => (
                    <span key={proto} style={{
                      background: 'rgba(255,255,255,0.05)', padding: '2px 8px',
                      borderRadius: 4, fontSize: 11, color: 'var(--text-secondary)',
                    }}>
                      {proto}
                    </span>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <div style={OVERLAY} onClick={() => setShowModal(false)}>
          <div style={MODAL} onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h3 style={{ fontSize: 18, fontWeight: 700 }}>{t('participants.addParticipant')}</h3>
              <button onClick={() => setShowModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}>
                <X size={20} />
              </button>
            </div>

            <div style={{ display: 'grid', gap: 16 }}>
              <Field label={t('participants.name')}>
                <input style={INPUT} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
              </Field>
              <Field label={t('participants.code')}>
                <input style={INPUT} value={form.code} onChange={e => setForm({ ...form, code: e.target.value })} />
              </Field>
              <Field label={t('participants.type')}>
                <select style={SELECT} value={form.type} onChange={e => setForm({ ...form, type: e.target.value })}>
                  <option value="ISSUER">ISSUER</option>
                  <option value="ACQUIRER">ACQUIRER</option>
                  <option value="SWITCH">SWITCH</option>
                  <option value="PROCESSOR">PROCESSOR</option>
                </select>
              </Field>
              <Field label={t('participants.status')}>
                <select style={SELECT} value={form.status} onChange={e => setForm({ ...form, status: e.target.value as Participant['status'] })}>
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="INACTIVE">INACTIVE</option>
                  <option value="SUSPENDED">SUSPENDED</option>
                </select>
              </Field>
              <Field label={t('participants.endpointUrl')}>
                <input style={INPUT} value={form.endpointUrl} onChange={e => setForm({ ...form, endpointUrl: e.target.value })} placeholder={t('participants.placeholderUrl')} />
              </Field>
              <Field label={t('participants.supportedProtocols')}>
                <input style={INPUT} value={form.supportedProtocols} onChange={e => setForm({ ...form, supportedProtocols: e.target.value })} placeholder={t('participants.placeholderProtocols')} />
              </Field>
            </div>

            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 24 }}>
              <button onClick={() => setShowModal(false)} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>
                {t('participants.cancel')}
              </button>
              <button onClick={handleSubmit} disabled={saving || !form.name || !form.code} style={{
                padding: '10px 20px', borderRadius: 8, border: 'none',
                background: saving || !form.name || !form.code ? '#64748b' : '#3b82f6',
                color: 'white', cursor: saving || !form.name || !form.code ? 'not-allowed' : 'pointer',
                fontSize: 13, fontWeight: 600,
              }}>
                {saving ? t('participants.creating') : t('participants.save')}
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
