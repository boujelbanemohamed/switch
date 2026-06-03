import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { Dispute, DisputeEvidence, DisputeTimeline } from '../types';

type Tab = 'all' | 'open' | 'resolved';

const STATUS_COLORS: Record<string, string> = {
  OPEN: '#f59e0b',
  UNDER_REVIEW: '#3b82f6',
  EVIDENCE_REQUESTED: '#8b5cf6',
  EVIDENCE_SUBMITTED: '#06b6d4',
  REPRESENTMENT: '#f97316',
  PRE_ARBITRATION: '#ef4444',
  ARBITRATION: '#dc2626',
  WON: '#22c55e',
  LOST: '#ef4444',
  WITHDRAWN: '#6b7280',
};

export function Disputes() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('all');
  const [disputes, setDisputes] = useState<Dispute[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Dispute | null>(null);
  const [timeline, setTimeline] = useState<DisputeTimeline[]>([]);
  const [evidence, setEvidence] = useState<DisputeEvidence[]>([]);
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({ transactionId: '', disputeType: 'FRAUD', amount: '', currencyCode: 'TND', reasonDescription: '', initiatedBy: 'MERCHANT' });
  const [evidenceForm, setEvidenceForm] = useState({ evidenceType: 'RECEIPT', description: '', fileReference: '' });
  const [transitionNotes, setTransitionNotes] = useState('');

  const tabs: { key: Tab; label: string }[] = [
    { key: 'all', label: t('disputes.tabAll') },
    { key: 'open', label: t('disputes.tabOpen') },
    { key: 'resolved', label: t('disputes.tabResolved') },
  ];

  const loadDisputes = useCallback(async () => {
    setLoading(true);
    try {
      let data = await api.disputes.list();
      if (tab === 'open') {
        data = data.filter(d => ['OPEN', 'UNDER_REVIEW', 'EVIDENCE_REQUESTED', 'EVIDENCE_SUBMITTED', 'REPRESENTMENT', 'PRE_ARBITRATION', 'ARBITRATION'].includes(d.status));
      } else if (tab === 'resolved') {
        data = data.filter(d => ['WON', 'LOST', 'WITHDRAWN'].includes(d.status));
      }
      setDisputes(data);
    } catch (err) {
      console.error('Failed to load disputes', err);
    }
    setLoading(false);
  }, [tab]);

  useEffect(() => { loadDisputes(); }, [loadDisputes]);

  async function openDispute() {
    try {
      await api.disputes.open({
        transactionId: createForm.transactionId,
        disputeType: createForm.disputeType,
        amount: parseFloat(createForm.amount),
        currencyCode: createForm.currencyCode,
        reasonDescription: createForm.reasonDescription,
        initiatedBy: createForm.initiatedBy,
      });
      setShowCreate(false);
      setCreateForm({ transactionId: '', disputeType: 'FRAUD', amount: '', currencyCode: 'TND', reasonDescription: '', initiatedBy: 'MERCHANT' });
      loadDisputes();
    } catch (err) {
      console.error('Failed to create dispute', err);
    }
  }

  async function selectDispute(d: Dispute) {
    setSelected(d);
    try {
      const detail = await api.disputes.get(d.id);
      setTimeline(detail.timeline);
      setEvidence(await api.disputes.getEvidence(d.id));
    } catch (err) {
      console.error('Failed to load dispute detail', err);
    }
  }

  async function submitEvidence() {
    if (!selected) return;
    try {
      await api.disputes.submitEvidence(selected.id, evidenceForm);
      setEvidenceForm({ evidenceType: 'RECEIPT', description: '', fileReference: '' });
      setEvidence(await api.disputes.getEvidence(selected.id));
    } catch (err) {
      console.error('Failed to submit evidence', err);
    }
  }

  async function transitionStatus(newStatus: string) {
    if (!selected) return;
    try {
      await api.disputes.transition(selected.id, newStatus, transitionNotes);
      setTransitionNotes('');
      const detail = await api.disputes.get(selected.id);
      setSelected(detail.dispute);
      setTimeline(detail.timeline);
      loadDisputes();
    } catch (err) {
      console.error('Failed to transition', err);
    }
  }

  function daysUntil(dateStr?: string): number | null {
    if (!dateStr) return null;
    const diff = new Date(dateStr).getTime() - Date.now();
    return Math.ceil(diff / 86400000);
  }

  function statusBadge(status: string) {
    const color = STATUS_COLORS[status] || '#6b7280';
    return <span style={{ background: color + '20', color, padding: '2px 10px', borderRadius: 12, fontSize: 12, fontWeight: 600 }}>{status}</span>;
  }

  const allowedTransitions: Record<string, string[]> = {
    OPEN: ['UNDER_REVIEW', 'WITHDRAWN'],
    UNDER_REVIEW: ['EVIDENCE_REQUESTED', 'WON', 'LOST'],
    EVIDENCE_REQUESTED: ['EVIDENCE_SUBMITTED'],
    EVIDENCE_SUBMITTED: ['REPRESENTMENT', 'WON', 'LOST'],
    REPRESENTMENT: ['PRE_ARBITRATION', 'WON', 'LOST'],
    PRE_ARBITRATION: ['ARBITRATION', 'WON', 'LOST'],
    ARBITRATION: ['WON', 'LOST'],
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h2 style={{ fontSize: 24, fontWeight: 700 }}>{t('disputes.title')}</h2>
        <button onClick={() => setShowCreate(true)} style={{ padding: '8px 20px', borderRadius: 8, border: 'none', background: '#3b82f6', color: '#fff', fontWeight: 600, fontSize: 13, cursor: 'pointer' }}>
          {t('disputes.openNew')}
        </button>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
        {tabs.map(t => (
          <button key={t.key} onClick={() => setTab(t.key)} style={{
            padding: '8px 20px', borderRadius: 8, border: 'none', background: tab === t.key ? '#3b82f6' : 'var(--surface)',
            color: tab === t.key ? '#fff' : 'var(--text-secondary)', fontWeight: 600, fontSize: 13, cursor: 'pointer',
          }}>{t.label}</button>
        ))}
      </div>

      {loading && <div style={{ padding: 40, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('common.loading')}</div>}

      {!loading && selected && (
        <div style={{ marginBottom: 24, padding: 20, background: 'var(--surface)', borderRadius: 12, border: '1px solid var(--border)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 16 }}>
            <div>
              <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>{selected.disputeNumber}</h3>
              <p style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{t('disputes.transactionId')}: {selected.transactionId}</p>
            </div>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              {statusBadge(selected.status)}
              <button onClick={() => setSelected(null)} style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', fontSize: 13, fontWeight: 600 }}>
                {t('common.close')}
              </button>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 16, marginBottom: 20, fontSize: 13 }}>
            <div><strong>{t('disputes.amount')}:</strong> {selected.amount} {selected.currencyCode}</div>
            <div><strong>{t('disputes.type')}:</strong> {selected.disputeType}</div>
            <div><strong>{t('disputes.initiatedBy')}:</strong> {selected.initiatedBy}</div>
            <div><strong>{t('disputes.evidenceDeadline')}:</strong> {selected.evidenceDeadline ? new Date(selected.evidenceDeadline).toLocaleDateString() : '-'}
              {daysUntil(selected.evidenceDeadline) !== null && daysUntil(selected.evidenceDeadline)! <= 7 && (
                <span style={{ marginLeft: 8, color: '#ef4444', fontWeight: 600 }}>({daysUntil(selected.evidenceDeadline)}j)</span>
              )}
            </div>
            <div><strong>{t('disputes.resolutionDeadline')}:</strong> {selected.resolutionDeadline ? new Date(selected.resolutionDeadline).toLocaleDateString() : '-'}</div>
            <div><strong>{t('disputes.reason')}:</strong> {selected.reasonDescription || '-'}</div>
          </div>

          {!['WON', 'LOST', 'WITHDRAWN'].includes(selected.status) && (
            <div style={{ marginBottom: 20 }}>
              <h4 style={{ fontSize: 14, fontWeight: 600, marginBottom: 8 }}>{t('disputes.transitionStatus')}</h4>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 8 }}>
                {(allowedTransitions[selected.status] || []).map(s => (
                  <button key={s} onClick={() => transitionStatus(s)} style={{
                    padding: '6px 14px', borderRadius: 6, border: 'none', background: STATUS_COLORS[s] + '20',
                    color: STATUS_COLORS[s], fontWeight: 600, fontSize: 12, cursor: 'pointer',
                  }}>{s}</button>
                ))}
              </div>
              <input placeholder={t('disputes.transitionNotes')} value={transitionNotes} onChange={e => setTransitionNotes(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }} />
            </div>
          )}

          <div style={{ marginBottom: 20 }}>
            <h4 style={{ fontSize: 14, fontWeight: 600, marginBottom: 8 }}>{t('disputes.evidence')}</h4>
            <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
              <select value={evidenceForm.evidenceType} onChange={e => setEvidenceForm(p => ({ ...p, evidenceType: e.target.value }))}
                style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }}>
                {['RECEIPT', 'CONTRACT', 'COMMUNICATION', 'DELIVERY_PROOF', 'REFUND_PROOF', 'OTHER_DOCUMENT'].map(t => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
              <input placeholder={t('disputes.description')} value={evidenceForm.description} onChange={e => setEvidenceForm(p => ({ ...p, description: e.target.value }))}
                style={{ flex: 1, padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }} />
              <button onClick={submitEvidence} style={{ padding: '8px 16px', borderRadius: 8, border: 'none', background: '#22c55e', color: '#fff', fontWeight: 600, fontSize: 13, cursor: 'pointer' }}>
                {t('disputes.submitEvidence')}
              </button>
            </div>
            {evidence.map(e => (
              <div key={e.id} style={{ padding: '8px 12px', background: 'var(--bg)', borderRadius: 8, marginBottom: 4, fontSize: 13 }}>
                <strong>{e.evidenceType}</strong> — {e.description} <span style={{ color: 'var(--text-secondary)' }}>({e.submittedBy}, {new Date(e.submittedAt).toLocaleString()})</span>
              </div>
            ))}
          </div>

          <div>
            <h4 style={{ fontSize: 14, fontWeight: 600, marginBottom: 8 }}>{t('disputes.timeline')}</h4>
            <div style={{ position: 'relative', paddingLeft: 20 }}>
              {timeline.map((entry, i) => (
                <div key={entry.id} style={{ position: 'relative', paddingBottom: 12, borderLeft: '2px solid var(--border)', paddingLeft: 16, marginLeft: -10 }}>
                  <div style={{ position: 'absolute', left: -6, top: 4, width: 10, height: 10, borderRadius: '50%', background: '#3b82f6' }} />
                  <div style={{ fontSize: 12, fontWeight: 600 }}>{entry.action}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                    {entry.oldStatus && <span style={{ color: STATUS_COLORS[entry.oldStatus] }}>{entry.oldStatus}</span>}
                    {entry.oldStatus && entry.newStatus && <span>{' → '}</span>}
                    {entry.newStatus && <span style={{ color: STATUS_COLORS[entry.newStatus] }}>{entry.newStatus}</span>}
                    {entry.performedBy && <span> by {entry.performedBy}</span>}
                  </div>
                  {entry.notes && <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 2 }}>{entry.notes}</div>}
                  <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginTop: 2 }}>{new Date(entry.createdAt).toLocaleString()}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {!loading && !selected && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, border: '1px solid var(--border)', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)' }}>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: 'var(--text-secondary)' }}>{t('disputes.number')}</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: 'var(--text-secondary)' }}>{t('disputes.transactionId')}</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: 'var(--text-secondary)' }}>{t('disputes.type')}</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: 'var(--text-secondary)' }}>{t('disputes.amount')}</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: 'var(--text-secondary)' }}>{t('disputes.status')}</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: 'var(--text-secondary)' }}>{t('disputes.deadline')}</th>
              </tr>
            </thead>
            <tbody>
              {disputes.map(d => (
                <tr key={d.id} onClick={() => selectDispute(d)} style={{ borderBottom: '1px solid var(--border)', cursor: 'pointer' }}>
                  <td style={{ padding: '12px 16px', fontWeight: 600 }}>{d.disputeNumber}</td>
                  <td style={{ padding: '12px 16px', color: 'var(--text-secondary)' }}>{d.transactionId}</td>
                  <td style={{ padding: '12px 16px' }}>{d.disputeType}</td>
                  <td style={{ padding: '12px 16px' }}>{d.amount} {d.currencyCode}</td>
                  <td style={{ padding: '12px 16px' }}>{statusBadge(d.status)}</td>
                  <td style={{ padding: '12px 16px' }}>
                    {d.evidenceDeadline ? (
                      daysUntil(d.evidenceDeadline) !== null && daysUntil(d.evidenceDeadline)! <= 7
                        ? <span style={{ color: '#ef4444', fontWeight: 600 }}>{daysUntil(d.evidenceDeadline)}j</span>
                        : <span style={{ color: 'var(--text-secondary)' }}>{daysUntil(d.evidenceDeadline)}j</span>
                    ) : '-'}
                  </td>
                </tr>
              ))}
              {disputes.length === 0 && (
                <tr><td colSpan={6} style={{ padding: 40, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('common.noData')}</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {showCreate && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ background: 'var(--surface)', borderRadius: 16, padding: 24, width: 480, maxWidth: '90vw' }}>
            <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 16 }}>{t('disputes.openNew')}</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <input placeholder={t('disputes.transactionId')} value={createForm.transactionId} onChange={e => setCreateForm(p => ({ ...p, transactionId: e.target.value }))}
                style={{ padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }} />
              <select value={createForm.disputeType} onChange={e => setCreateForm(p => ({ ...p, disputeType: e.target.value }))}
                style={{ padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }}>
                {['FRAUD', 'NOT_RECEIVED', 'DUPLICATE', 'INCORRECT_AMOUNT', 'QUALITY_ISSUE', 'CANCELLED', 'CREDIT_NOT_PROCESSED', 'OTHER'].map(t => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
              <div style={{ display: 'flex', gap: 8 }}>
                <input placeholder={t('disputes.amount')} type="number" value={createForm.amount} onChange={e => setCreateForm(p => ({ ...p, amount: e.target.value }))}
                  style={{ flex: 1, padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }} />
                <select value={createForm.currencyCode} onChange={e => setCreateForm(p => ({ ...p, currencyCode: e.target.value }))}
                  style={{ width: 100, padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }}>
                  <option value="TND">TND</option>
                  <option value="EUR">EUR</option>
                  <option value="USD">USD</option>
                </select>
              </div>
              <textarea placeholder={t('disputes.description')} value={createForm.reasonDescription} onChange={e => setCreateForm(p => ({ ...p, reasonDescription: e.target.value }))}
                style={{ padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13, minHeight: 80 }} />
              <select value={createForm.initiatedBy} onChange={e => setCreateForm(p => ({ ...p, initiatedBy: e.target.value }))}
                style={{ padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }}>
                <option value="MERCHANT">{t('disputes.merchant')}</option>
                <option value="CARDHOLDER">{t('disputes.cardholder')}</option>
                <option value="ISSUER">{t('disputes.issuer')}</option>
                <option value="ACQUIRER">{t('disputes.acquirer')}</option>
              </select>
            </div>
            <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
              <button onClick={() => setShowCreate(false)} style={{ padding: '8px 20px', borderRadius: 8, border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontWeight: 600, fontSize: 13, cursor: 'pointer' }}>
                {t('common.cancel')}
              </button>
              <button onClick={openDispute} style={{ padding: '8px 20px', borderRadius: 8, border: 'none', background: '#3b82f6', color: '#fff', fontWeight: 600, fontSize: 13, cursor: 'pointer' }}>
                {t('disputes.create')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
