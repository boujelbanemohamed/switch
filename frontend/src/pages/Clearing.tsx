import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { ClearingRecord, NettingRecord, ReconciliationResult } from '../types';
import { SectionHeader } from '../components/SectionHeader';

export function Clearing() {
  const { t } = useTranslation();
  const [records, setRecords] = useState<ClearingRecord[]>([]);
  const [netting, setNetting] = useState<NettingRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [reconciliation, setReconciliation] = useState<ReconciliationResult | null>(null);
  const [fileDate, setFileDate] = useState(new Date().toISOString().split('T')[0]);
  const [participantId, setParticipantId] = useState('');
  const [fileFormat, setFileFormat] = useState('CSV');
  const [uploadContent, setUploadContent] = useState('');
  const today = new Date().toISOString().split('T')[0];

  useEffect(() => {
    Promise.all([
      api.clearing.getByDate(today),
      api.clearing.netting.calculate(today),
    ])
      .then(([clearingData, nettingData]) => {
        setRecords(clearingData);
        setNetting(nettingData);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const totalAmount = records.reduce((s, r) => s + r.amount, 0);
  const totalFee = records.reduce((s, r) => s + r.fee, 0);
  const pendingDisputes = records.filter(r => r.status === 'DISPUTED').length;

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('clearing.title')}</h2>

      <SectionHeader sectionKey="clearing" />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        <StatCard title={t('clearing.totalCleared')} value={`$${totalAmount.toLocaleString()}`} />
        <StatCard title={t('clearing.totalFees')} value={`$${totalFee.toLocaleString()}`} />
        <StatCard title={t('clearing.netAmount')} value={`$${(totalAmount - totalFee).toLocaleString()}`} />
        <StatCard title={t('clearing.disputes')} value={pendingDisputes.toLocaleString()} />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('clearing.clearingRecords')} ({today})</h3>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('clearing.txnId')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('clearing.amount')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('clearing.fee')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('clearing.status')}</th>
                </tr>
              </thead>
              <tbody>
                {records.map(r => (
                  <tr key={r.id} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '10px 12px', fontFamily: 'monospace', fontSize: 12 }}>{r.transactionId?.substring(0, 12)}...</td>
                    <td style={{ padding: '10px 12px' }}>${r.amount.toLocaleString()}</td>
                    <td style={{ padding: '10px 12px', color: '#f97316' }}>${r.fee.toLocaleString()}</td>
                    <td style={{ padding: '10px 12px' }}>
                      <span style={{
                        background: r.status === 'CLEARED' ? '#22c55e33' : r.status === 'DISPUTED' ? '#ef444433' : '#64748b33',
                        color: r.status === 'CLEARED' ? '#22c55e' : r.status === 'DISPUTED' ? '#ef4444' : '#64748b',
                        padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                      }}>
                        {r.status}
                      </span>
                    </td>
                  </tr>
                ))}
                {records.length === 0 && (
                  <tr>
                    <td colSpan={4} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>
                      {t('clearing.noRecords')}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('clearing.nettingResults')} ({today})</h3>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('clearing.participant')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('clearing.debit')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('clearing.credit')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('clearing.net')}</th>
                </tr>
              </thead>
              <tbody>
                {netting.map(n => (
                  <tr key={n.id} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '10px 12px', fontFamily: 'monospace', fontWeight: 600 }}>{n.participantId?.substring(0, 8)}</td>
                    <td style={{ padding: '10px 12px', color: '#ef4444' }}>(${n.grossDebit.toLocaleString()})</td>
                    <td style={{ padding: '10px 12px', color: '#22c55e' }}>${n.grossCredit.toLocaleString()}</td>
                    <td style={{ padding: '10px 12px', fontWeight: 700, color: n.netAmount >= 0 ? '#22c55e' : '#ef4444' }}>
                      {n.netAmount >= 0 ? '+' : ''}${n.netAmount.toLocaleString()}
                    </td>
                  </tr>
                ))}
                {netting.length === 0 && (
                  <tr>
                    <td colSpan={4} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>
                      {t('common.noData')}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24, marginTop: 24 }}>
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('clearing.files.generateTitle')}</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div>
              <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('clearing.files.date')}</label>
              <input type="date" value={fileDate} onChange={e => setFileDate(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)' }} />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('clearing.files.participantId')}</label>
              <input type="text" value={participantId} onChange={e => setParticipantId(e.target.value)} placeholder="UUID"
                style={{ width: '100%', padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)' }} />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('clearing.files.format')}</label>
              <select value={fileFormat} onChange={e => setFileFormat(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)' }}>
                <option value="CSV">{t('clearing.files.formatCsv')}</option>
                <option value="ISO20022">{t('clearing.files.formatIso20022')}</option>
                <option value="COMPCONF">{t('clearing.files.formatCompconf')}</option>
                <option value="CP50">{t('clearing.files.formatCp50')}</option>
              </select>
            </div>
            <button onClick={async () => {
              setGenerating(true);
              try {
                const content = await api.clearing.files.generateOutgoing(fileDate, participantId, fileFormat);
                const blob = new Blob([content], { type: 'text/plain' });
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                const ext = fileFormat === 'COMPCONF' ? 'cmp' : fileFormat === 'CP50' ? 'cp5' : fileFormat.toLowerCase();
                a.href = url;
                a.download = `clearing-${fileDate}-${participantId}.${ext}`;
                a.click();
                URL.revokeObjectURL(url);
              } catch (e) { console.error(e); }
              finally { setGenerating(false); }
            }} disabled={generating || !participantId}
              style={{ width: '100%', padding: '10px', borderRadius: 8, border: 'none', background: 'var(--accent)', color: '#fff', fontWeight: 600, cursor: generating || !participantId ? 'not-allowed' : 'pointer', opacity: generating || !participantId ? 0.6 : 1 }}>
              {generating ? t('common.loading') : t('clearing.files.generate')}
            </button>
          </div>
        </div>

        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('clearing.files.uploadTitle')}</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <textarea value={uploadContent} onChange={e => setUploadContent(e.target.value)}
              placeholder={t('clearing.files.uploadPlaceholder')}
              rows={6}
              style={{ width: '100%', padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontFamily: 'monospace', fontSize: 12, resize: 'vertical' }} />
            <button onClick={async () => {
              setUploading(true);
              try {
                const result = await api.clearing.files.uploadIncoming(uploadContent, fileFormat);
                setReconciliation(result);
              } catch (e) { console.error(e); }
              finally { setUploading(false); }
            }} disabled={uploading || !uploadContent.trim()}
              style={{ width: '100%', padding: '10px', borderRadius: 8, border: 'none', background: 'var(--accent)', color: '#fff', fontWeight: 600, cursor: uploading || !uploadContent.trim() ? 'not-allowed' : 'pointer', opacity: uploading || !uploadContent.trim() ? 0.6 : 1 }}>
              {uploading ? t('common.loading') : t('clearing.files.upload')}
            </button>
            {reconciliation && (
              <div style={{ padding: 12, borderRadius: 8, background: '#22c55e11', border: '1px solid #22c55e33' }}>
                <p style={{ fontSize: 13, fontWeight: 600, marginBottom: 4 }}>{t('clearing.files.reconciliationTitle')}</p>
                <p style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{t('clearing.files.total')}: {reconciliation.totalRecords}</p>
                <p style={{ fontSize: 12, color: '#22c55e' }}>{t('clearing.files.matched')}: {reconciliation.matched}</p>
                <p style={{ fontSize: 12, color: '#ef4444' }}>{t('clearing.files.unmatched')}: {reconciliation.unmatched}</p>
              </div>
            )}
          </div>
        </div>
      </div>
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
