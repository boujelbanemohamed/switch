import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { DashboardStats } from '../types';
import { SectionHeader } from '../components/SectionHeader';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

const COLORS = ['#22c55e', '#3b82f6', '#eab308', '#ef4444', '#a855f7', '#f97316'];

export function Dashboard() {
  const { t } = useTranslation();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.dashboard.stats()
      .then(setStats)
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

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('dashboard.title')}</h2>

      <SectionHeader sectionKey="dashboard" />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        <StatCard title={t('dashboard.totalTransactions')} value={stats.totalLastHour.toLocaleString()} />
        <StatCard title={t('dashboard.activeParticipants')} value={stats.totalLast24h.toLocaleString()} />
        <StatCard title={t('dashboard.avgProcessing')} value={`${Math.round(stats.avgProcessingTimeMs)}${t('dashboard.ms')}`} />
        <StatCard title={t('dashboard.successRate')} value={
          stats.statusBreakdown?.COMPLETED
            ? `${Math.round((stats.statusBreakdown.COMPLETED / Math.max(1,
                Object.values(stats.statusBreakdown).reduce((a, b) => a + b, 0))) * 100)}%`
            : t('common.na')
        } />
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
