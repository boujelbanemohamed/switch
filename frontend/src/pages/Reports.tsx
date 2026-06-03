import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { request } from '../services/api';
import { SectionHeader } from '../components/SectionHeader';

interface ReportItem {
  id: string;
  reportType: string;
  name: string;
  parameters: string;
  status: string;
  resultSummary?: string;
  createdAt: string;
  generatedAt?: string;
  filePath?: string;
  fileFormat?: string;
  generatedBy?: string;
}

const REPORT_TYPES = ['TRANSACTION', 'SETTLEMENT', 'FRAUD', 'AUDIT', 'REGULATORY', 'PERFORMANCE', 'FINANCIAL', 'CUSTOM'];
const FORMATS = ['CSV', 'PDF', 'XLSX', 'JSON', 'XML'];

export function Reports() {
  const { t } = useTranslation();
  const [reports, setReports] = useState<ReportItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterType, setFilterType] = useState('TRANSACTION');
  const [showCreate, setShowCreate] = useState(false);
  const [creating, setCreating] = useState(false);

  const [formName, setFormName] = useState('');
  const [formType, setFormType] = useState('TRANSACTION');
  const [formFormat, setFormFormat] = useState('CSV');
  const [formDesc, setFormDesc] = useState('');
  const [generateAfter, setGenerateAfter] = useState(false);

  const loadReports = async (type: string) => {
    setLoading(true);
    try {
      const data = await request<ReportItem[]>(`/backoffice/reports?type=${type}`);
      setReports(data);
    } catch { setReports([]); }
    setLoading(false);
  };

  useEffect(() => { loadReports(filterType); }, [filterType]);

  const createReport = async () => {
    if (!formName.trim()) return;
    setCreating(true);
    try {
      const report = await request<ReportItem>('/backoffice/reports', {
        method: 'POST',
        body: JSON.stringify({
          name: formName,
          reportType: formType,
          description: formDesc,
          fileFormat: formFormat,
        }),
      });
      if (generateAfter) {
        await request<ReportItem>(`/backoffice/reports/${report.id}/generate`, { method: 'POST' });
      }
      setShowCreate(false);
      setFormName('');
      setFormDesc('');
      setGenerateAfter(false);
      loadReports(filterType);
    } catch { }
    setCreating(false);
  };

  const generateReport = async (id: string) => {
    try {
      await request<ReportItem>(`/backoffice/reports/${id}/generate`, { method: 'POST' });
      loadReports(filterType);
    } catch { }
  };

  const statusColor: Record<string, string> = {
    PENDING: '#f59e0b', GENERATING: '#3b82f6', COMPLETED: '#22c55e', FAILED: '#ef4444',
  };
  const statusBg: Record<string, string> = {
    PENDING: 'rgba(245,158,11,0.15)', GENERATING: 'rgba(59,130,246,0.15)',
    COMPLETED: 'rgba(34,197,94,0.15)', FAILED: 'rgba(239,68,68,0.15)',
  };

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('reports.title')}</h2>

      <SectionHeader sectionKey="reports" />

      <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginBottom: 20, flexWrap: 'wrap' }}>
        <select value={filterType} onChange={e => setFilterType(e.target.value)}
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }}>
          {REPORT_TYPES.map(tp => <option key={tp} value={tp}>{tp}</option>)}
        </select>
        <button onClick={() => setShowCreate(true)}
          style={{ padding: '8px 20px', borderRadius: 8, border: 'none', background: '#3b82f6', color: '#fff', fontWeight: 600, fontSize: 13, cursor: 'pointer' }}>
          + {t('reports.newReport')}
        </button>
      </div>

      {showCreate && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 24, marginBottom: 20, maxWidth: 600 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('reports.createReport')}</h3>
          <div style={{ display: 'grid', gap: 12 }}>
            <input value={formName} onChange={e => setFormName(e.target.value)} placeholder={t('reports.name')}
              style={{ padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }} />
            <div style={{ display: 'flex', gap: 12 }}>
              <select value={formType} onChange={e => setFormType(e.target.value)}
                style={{ flex: 1, padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }}>
                {REPORT_TYPES.map(tp => <option key={tp} value={tp}>{tp}</option>)}
              </select>
              <select value={formFormat} onChange={e => setFormFormat(e.target.value)}
                style={{ flex: 1, padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }}>
                {FORMATS.map(f => <option key={f} value={f}>{f}</option>)}
              </select>
            </div>
            <textarea value={formDesc} onChange={e => setFormDesc(e.target.value)} placeholder={t('reports.description')}
              style={{ padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13, minHeight: 60, resize: 'vertical' }} />
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: 'var(--text-secondary)', cursor: 'pointer' }}>
              <input type="checkbox" checked={generateAfter} onChange={e => setGenerateAfter(e.target.checked)} />
              {t('reports.generateAfterCreate')}
            </label>
            <div style={{ display: 'flex', gap: 8 }}>
              <button onClick={createReport} disabled={creating || !formName.trim()}
                style={{ padding: '10px 24px', borderRadius: 8, border: 'none', background: '#3b82f6', color: '#fff', fontWeight: 600, fontSize: 13, cursor: 'pointer' }}>
                {creating ? t('common.loading') : t('reports.create')}
              </button>
              <button onClick={() => setShowCreate(false)}
                style={{ padding: '10px 24px', borderRadius: 8, border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontWeight: 600, fontSize: 13, cursor: 'pointer' }}>
                {t('common.cancel')}
              </button>
            </div>
          </div>
        </div>
      )}

      <div style={{ background: 'var(--surface)', borderRadius: 12, overflow: 'hidden' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('reports.name')}</th>
                <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('reports.type')}</th>
                <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('reports.status')}</th>
                <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('reports.createdAt')}</th>
                <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('reports.generatedAt')}</th>
                <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('reports.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={6} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('common.loading')}</td></tr>
              ) : reports.length === 0 ? (
                <tr><td colSpan={6} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('reports.noReports')}</td></tr>
              ) : reports.map(r => (
                <tr key={r.id} style={{ borderBottom: '1px solid var(--border)' }}>
                  <td style={{ padding: '10px 16px', fontWeight: 600 }}>{r.name}</td>
                  <td style={{ padding: '10px 16px' }}>{r.reportType}</td>
                  <td style={{ padding: '10px 16px' }}>
                    <span style={{ padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600, background: statusBg[r.status] || 'rgba(107,114,128,0.15)', color: statusColor[r.status] || '#6b7280' }}>
                      {r.status}
                    </span>
                  </td>
                  <td style={{ padding: '10px 16px', fontSize: 12 }}>{new Date(r.createdAt).toLocaleString()}</td>
                  <td style={{ padding: '10px 16px', fontSize: 12 }}>{r.generatedAt ? new Date(r.generatedAt).toLocaleString() : '-'}</td>
                  <td style={{ padding: '10px 16px' }}>
                    {r.status === 'PENDING' && (
                      <button onClick={() => generateReport(r.id)}
                        style={{ padding: '4px 12px', borderRadius: 6, border: 'none', background: '#3b82f6', color: '#fff', fontSize: 11, fontWeight: 600, cursor: 'pointer' }}>
                        {t('reports.generate')}
                      </button>
                    )}
                    {r.status === 'COMPLETED' && r.resultSummary && (
                      <span style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{r.resultSummary}</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
