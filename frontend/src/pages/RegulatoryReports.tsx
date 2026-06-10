import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { RegulatoryReportTemplate } from '../types';
import { SectionHeader } from '../components/SectionHeader';
import { RegulatoryReportsHelp, PERIODICITY_LABELS, FORMAT_LABELS } from '../components/RegulatoryReportsHelp';

export function RegulatoryReports() {
  const { t } = useTranslation();
  const [templates, setTemplates] = useState<RegulatoryReportTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState('');
  const [startDate, setStartDate] = useState(new Date().toISOString().split('T')[0]);
  const [endDate, setEndDate] = useState(new Date().toISOString().split('T')[0]);
  const [format, setFormat] = useState('CSV');

  useEffect(() => {
    api.regulatory.reports.listTemplates()
      .then(setTemplates)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const generate = async () => {
    setGenerating(true);
    try {
      const content = await api.regulatory.reports.generate(selectedTemplate, startDate, endDate, format);
      const blob = new Blob([content], { type: 'text/plain' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${selectedTemplate}-${startDate}-to-${endDate}.${format.toLowerCase()}`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) { console.error(e); }
    finally { setGenerating(false); }
  };

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 24 }}>
        <h2 style={{ fontSize: 24, fontWeight: 700 }}>{t('regulatory.title')}</h2>
        <RegulatoryReportsHelp />
      </div>
      <SectionHeader sectionKey="regulatory" />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('regulatory.templates')}</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {templates.map(t => (
              <div key={t.id} onClick={() => setSelectedTemplate(t.id)}
                style={{ padding: 12, borderRadius: 8, border: selectedTemplate === t.id ? '2px solid var(--accent)' : '1px solid var(--border)', cursor: 'pointer', background: selectedTemplate === t.id ? 'var(--accent)11' : 'transparent' }}>
                <p style={{ fontSize: 13, fontWeight: 600, marginBottom: 2 }}>{t.name}</p>
                <p style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{t.description}</p>
                <span style={{ fontSize: 10, color: 'var(--accent)', fontWeight: 600, textTransform: 'uppercase' }}>{PERIODICITY_LABELS[t.periodicity] || t.periodicity}</span>
              </div>
            ))}
          </div>
        </div>

        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('regulatory.generateTitle')}</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div>
              <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('regulatory.startDate')}</label>
              <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)' }} />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('regulatory.endDate')}</label>
              <input type="date" value={endDate} onChange={e => setEndDate(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)' }} />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('regulatory.format')}</label>
              <select value={format} onChange={e => setFormat(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)' }}>
                <option value="CSV">CSV</option>
              </select>
            </div>
            <button onClick={generate} disabled={generating || !selectedTemplate}
              style={{ width: '100%', padding: '10px', borderRadius: 8, border: 'none', background: 'var(--accent)', color: '#fff', fontWeight: 600, cursor: generating || !selectedTemplate ? 'not-allowed' : 'pointer', opacity: generating || !selectedTemplate ? 0.6 : 1 }}>
              {generating ? t('common.loading') : t('regulatory.generate')}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
