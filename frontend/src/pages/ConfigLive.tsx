import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { request } from '../services/api';
import { SectionHeader } from '../components/SectionHeader';
import { ConfigLiveHelp, CATEGORY_LABELS, DATA_TYPE_LABELS } from '../components/ConfigLiveHelp';

interface ConfigItem {
  id: string;
  configKey: string;
  configValue: string;
  description: string;
  dataType: string;
  category: string;
  mutable: boolean;
  updatedBy: string;
  updatedAt: string;
}

const CATEGORY_COLORS: Record<string, string> = {
  TRANSACTION: '#3b82f6', AUTH: '#8b5cf6', FRAUD: '#ef4444',
  FEE: '#22c55e', CLEARING: '#f59e0b', MONITORING: '#06b6d4',
  BATCH: '#f97316', KYC: '#a855f7', SWITCH: '#6b7280',
};

const CATEGORY_BG: Record<string, string> = {
  TRANSACTION: 'rgba(59,130,246,0.1)', AUTH: 'rgba(139,92,246,0.1)',
  FRAUD: 'rgba(239,68,68,0.1)', FEE: 'rgba(34,197,94,0.1)',
  CLEARING: 'rgba(245,158,11,0.1)', MONITORING: 'rgba(6,182,212,0.1)',
  BATCH: 'rgba(249,115,22,0.1)', KYC: 'rgba(168,85,247,0.1)',
  SWITCH: 'rgba(107,114,128,0.1)',
};

export function ConfigLive() {
  const { t } = useTranslation();
  const [configs, setConfigs] = useState<ConfigItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editValue, setEditValue] = useState('');
  const [updating, setUpdating] = useState(false);

  const loadConfigs = async () => {
    setLoading(true);
    try {
      const data = await request<ConfigItem[]>('/admin/live-config');
      setConfigs(data);
    } catch { setConfigs([]); }
    setLoading(false);
  };

  useEffect(() => { loadConfigs(); }, []);

  const updateConfig = async (id: string) => {
    setUpdating(true);
    try {
      await request<ConfigItem>(`/admin/live-config/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ value: editValue }),
      });
      setEditingId(null);
      loadConfigs();
    } catch { }
    setUpdating(false);
  };

  const grouped = configs.reduce<Record<string, ConfigItem[]>>((acc, cfg) => {
    (acc[cfg.category] = acc[cfg.category] || []).push(cfg);
    return acc;
  }, {});

  const categoryOrder = ['TRANSACTION', 'AUTH', 'FRAUD', 'FEE', 'CLEARING', 'MONITORING', 'BATCH', 'KYC', 'SWITCH'];

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <h2 style={{ fontSize: 24, fontWeight: 700 }}>{t('configLive.title')}</h2>
          <ConfigLiveHelp />
        </div>
        <button onClick={loadConfigs}
          style={{ padding: '8px 16px', borderRadius: 8, border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: 13, cursor: 'pointer' }}>
          {t('common.refresh')}
        </button>
      </div>

      <SectionHeader sectionKey="configLive" />

      {categoryOrder.filter(cat => grouped[cat]).map(category => (
        <div key={category} style={{ marginBottom: 28 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
            <span style={{
              padding: '4px 10px', borderRadius: 6, fontSize: 11, fontWeight: 700,
              background: CATEGORY_BG[category] || 'rgba(107,114,128,0.1)',
              color: CATEGORY_COLORS[category] || '#6b7280',
            }}>{CATEGORY_LABELS[category] || category}</span>
            <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{grouped[category].length} parameters</span>
          </div>
          <div style={{ background: 'var(--surface)', borderRadius: 12, overflow: 'hidden' }}>
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                    <th style={{ padding: '10px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('configLive.key')}</th>
                    <th style={{ padding: '10px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('configLive.value')}</th>
                    <th style={{ padding: '10px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('configLive.type')}</th>
                    <th style={{ padding: '10px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('configLive.description')}</th>
                    <th style={{ padding: '10px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('configLive.actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {grouped[category].map(cfg => (
                    <tr key={cfg.id} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '10px 16px', fontFamily: 'monospace', fontSize: 12, fontWeight: 600 }}>{cfg.configKey}</td>
                      <td style={{ padding: '10px 16px' }}>
                        {editingId === cfg.id ? (
                          <div style={{ display: 'flex', gap: 6 }}>
                            <input value={editValue} onChange={e => setEditValue(e.target.value)}
                              style={{ padding: '6px 10px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 12, width: 180 }} />
                            <button onClick={() => updateConfig(cfg.id)} disabled={updating}
                              style={{ padding: '4px 10px', borderRadius: 6, border: 'none', background: '#22c55e', color: '#fff', fontSize: 11, fontWeight: 600, cursor: 'pointer' }}>OK</button>
                            <button onClick={() => setEditingId(null)}
                              style={{ padding: '4px 10px', borderRadius: 6, border: '1px solid var(--border)', background: 'transparent', color: 'var(--text)', fontSize: 11, cursor: 'pointer' }}>X</button>
                          </div>
                        ) : (
                          <span style={{ fontWeight: 500 }}>{cfg.configValue}</span>
                        )}
                      </td>
                      <td style={{ padding: '10px 16px', fontSize: 11, color: 'var(--text-secondary)' }}>{DATA_TYPE_LABELS[cfg.dataType] || cfg.dataType}</td>
                      <td style={{ padding: '10px 16px', fontSize: 12, color: 'var(--text-secondary)' }}>{cfg.description}</td>
                      <td style={{ padding: '10px 16px' }}>
                        {cfg.mutable && editingId !== cfg.id && (
                          <button onClick={() => { setEditingId(cfg.id); setEditValue(cfg.configValue); }}
                            style={{ padding: '4px 10px', borderRadius: 6, border: '1px solid var(--border)', background: 'transparent', color: '#3b82f6', fontSize: 11, fontWeight: 600, cursor: 'pointer' }}>
                            {t('common.edit')}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
