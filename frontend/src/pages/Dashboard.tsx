import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api, request } from '../services/api';
import type { DashboardStats, MonitoringEvent } from '../types';
import { SectionHeader } from '../components/SectionHeader';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

const COLORS = ['#22c55e', '#3b82f6', '#eab308', '#ef4444', '#a855f7', '#f97316'];

export function Dashboard() {
  const { t } = useTranslation();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [alerts, setAlerts] = useState<MonitoringEvent[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.dashboard.stats(),
      request<MonitoringEvent[]>('/backoffice/monitoring/alerts').catch(() => []),
    ])
      .then(([s, a]) => { setStats(s); setAlerts(a); })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;
  if (!stats) return <div style={{ color: 'var(--danger)' }}>{t('dashboard.failedToLoadStats')}</div>;

  const statusData = Object.entries(stats.totalByStatus || {}).map(([name, value]) => ({
    name, value,
  }));

  const statusColors: Record<string, string> = {
    PENDING: '#eab308',
    ROUTING: '#3b82f6',
    PROCESSING: '#8b5cf6',
    COMPLETED: '#22c55e',
    FAILED: '#ef4444',
    TIMEOUT: '#f97316',
    REJECTED: '#dc2626',
  };

  const activeCritical = alerts.filter(a => a.severity === 'CRITICAL' && !a.acknowledged);
  const activeWarnings = alerts.filter(a => a.severity === 'WARNING' && !a.acknowledged);
  const healthOk = activeCritical.length === 0 && activeWarnings.length < 3;

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('dashboard.title')}</h2>

      <SectionHeader sectionKey="dashboard" />

      {activeCritical.length > 0 && (
        <div style={{ background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: 12, padding: '12px 20px', marginBottom: 20, display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ width: 10, height: 10, borderRadius: '50%', background: '#ef4444', animation: 'pulse 1.5s infinite' }} />
          <span style={{ fontWeight: 600, color: '#ef4444', fontSize: 14 }}>
            {activeCritical.length} {t('dashboard.criticalAlert')}{activeCritical.length > 1 ? 's' : ''}
          </span>
          <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{activeCritical.map(a => a.message).join('; ')}</span>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        <StatCard title={t('dashboard.totalTransactions')} value={stats.totalLastHour.toLocaleString()} />
        <StatCard title={t('dashboard.avgProcessing')} value={`${Math.round(stats.avgProcessingTimeMs)}${t('dashboard.ms')}`} />
        <StatCard title={t('dashboard.successRate')} value={
          stats.statusBreakdown?.COMPLETED
            ? `${Math.round((stats.statusBreakdown.COMPLETED / Math.max(1,
                Object.values(stats.statusBreakdown).reduce((a, b) => a + b, 0))) * 100)}%`
            : t('common.na')
        } />
        <StatCard title={t('dashboard.systemHealth')} value={healthOk ? 'Healthy' : 'Degraded'} valueColor={healthOk ? '#22c55e' : '#ef4444'} />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('dashboard.statusDistribution')}</h3>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={statusData}>
              <XAxis dataKey="name" tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} />
              <YAxis tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} />
              <Tooltip
                contentStyle={{ background: 'var(--surface-2)', border: '1px solid var(--border)', borderRadius: 8 }}
                labelStyle={{ color: 'var(--text)' }}
              />
              <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                {statusData.map((entry) => (
                  <Cell key={entry.name} fill={statusColors[entry.name] || '#3b82f6'} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('dashboard.last24h')}</h3>
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie
                data={Object.entries(stats.statusBreakdown).map(([k, v]) => ({ name: k, value: v }))}
                dataKey="value"
                nameKey="name"
                cx="50%"
                cy="50%"
                outerRadius={80}
                label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
              >
                {Object.entries(stats.statusBreakdown).map(([k]) => (
                  <Cell key={k} fill={statusColors[k] || '#3b82f6'} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {alerts.length > 0 && (
        <div style={{ marginTop: 24, background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('dashboard.recentAlerts')}</h3>
          <div style={{ display: 'grid', gap: 8 }}>
            {alerts.slice(0, 5).map(a => (
              <div key={a.id} style={{
                display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px',
                borderRadius: 8, background: 'var(--bg)', fontSize: 13,
              }}>
                <div style={{
                  width: 8, height: 8, borderRadius: '50%',
                  background: a.severity === 'CRITICAL' ? '#ef4444' : a.severity === 'WARNING' ? '#f59e0b' : '#3b82f6',
                }} />
                <span style={{ fontWeight: 600, color: 'var(--text)' }}>{a.eventType}</span>
                <span style={{ color: 'var(--text-secondary)', flex: 1 }}>{a.message}</span>
                <span style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{new Date(a.createdAt).toLocaleString()}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function StatCard({ title, value, valueColor }: { title: string; value: string; valueColor?: string }) {
  return (
    <div style={{
      background: 'var(--surface)',
      borderRadius: 12,
      padding: '16px 20px',
      border: '1px solid var(--border)',
    }}>
      <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 8 }}>{title}</p>
      <p style={{ fontSize: 28, fontWeight: 700, color: valueColor || 'var(--text)' }}>{value}</p>
    </div>
  );
}
