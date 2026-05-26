import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { AuditLog, MonitoringEvent } from '../types';

export function Backoffice() {
  const { t } = useTranslation();
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [alerts, setAlerts] = useState<MonitoringEvent[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.backoffice.audit.list(),
      api.backoffice.monitoring.alerts(),
    ])
      .then(([logs, events]) => {
        setAuditLogs(logs);
        setAlerts(events);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const criticalAlerts = alerts.filter(a => a.severity === 'CRITICAL' && !a.acknowledged);
  const warningAlerts = alerts.filter(a => a.severity === 'WARNING' && !a.acknowledged);

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('backoffice.title')}</h2>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        <StatCard title={t('backoffice.auditLogs')} value={auditLogs.length.toLocaleString()} />
        <StatCard title={t('backoffice.critical')} value={criticalAlerts.length.toLocaleString()} />
        <StatCard title={t('backoffice.warnings')} value={warningAlerts.length.toLocaleString()} />
        <StatCard title={t('backoffice.events')} value={alerts.length.toLocaleString()} />
      </div>

      {criticalAlerts.length > 0 && (
        <div style={{
          background: '#ef444433', border: '1px solid #ef4444', borderRadius: 12,
          padding: 16, marginBottom: 24,
        }}>
          <h3 style={{ fontSize: 14, fontWeight: 700, color: '#ef4444', marginBottom: 8 }}>
            {criticalAlerts.length} {t('backoffice.criticalAlertMsg')}
          </h3>
          {criticalAlerts.map(a => (
            <p key={a.id} style={{ fontSize: 13, marginBottom: 4 }}>
              <strong>{a.eventType}</strong>: {a.message}
            </p>
          ))}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('backoffice.auditLog')}</h3>
          <div style={{ overflowX: 'auto', maxHeight: 400, overflowY: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                  <th style={{ padding: '6px 10px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('backoffice.action')}</th>
                  <th style={{ padding: '6px 10px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('backoffice.resource')}</th>
                  <th style={{ padding: '6px 10px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('backoffice.status')}</th>
                  <th style={{ padding: '6px 10px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('backoffice.time')}</th>
                </tr>
              </thead>
              <tbody>
                {auditLogs.map(log => (
                  <tr key={log.id} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '6px 10px', fontWeight: 600 }}>{log.action}</td>
                    <td style={{ padding: '6px 10px', color: 'var(--text-secondary)' }}>{log.resourceType}/{log.resourceId?.substring(0, 8)}</td>
                    <td style={{ padding: '6px 10px' }}>
                      <span style={{
                        background: log.status === 'SUCCESS' ? '#22c55e33' : '#ef444433',
                        color: log.status === 'SUCCESS' ? '#22c55e' : '#ef4444',
                        padding: '1px 6px', borderRadius: 4, fontSize: 10, fontWeight: 600,
                      }}>
                        {log.status}
                      </span>
                    </td>
                    <td style={{ padding: '6px 10px', color: 'var(--text-secondary)', fontSize: 11 }}>
                      {new Date(log.createdAt).toLocaleTimeString()}
                    </td>
                  </tr>
                ))}
                {auditLogs.length === 0 && (
                  <tr>
                    <td colSpan={4} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>
                      {t('backoffice.noAuditLogs')}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('backoffice.monitoringEvents')}</h3>
          <div style={{ overflowX: 'auto', maxHeight: 400, overflowY: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                  <th style={{ padding: '6px 10px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('backoffice.type')}</th>
                  <th style={{ padding: '6px 10px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('backoffice.severity')}</th>
                  <th style={{ padding: '6px 10px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('backoffice.message')}</th>
                  <th style={{ padding: '6px 10px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('backoffice.ack')}</th>
                </tr>
              </thead>
              <tbody>
                {alerts.map(e => (
                  <tr key={e.id} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '6px 10px', fontWeight: 600 }}>{e.eventType}</td>
                    <td style={{ padding: '6px 10px' }}>
                      <span style={{
                        background: e.severity === 'CRITICAL' ? '#ef444433' : e.severity === 'WARNING' ? '#eab30833' : '#3b82f633',
                        color: e.severity === 'CRITICAL' ? '#ef4444' : e.severity === 'WARNING' ? '#eab308' : '#3b82f6',
                        padding: '1px 6px', borderRadius: 4, fontSize: 10, fontWeight: 600,
                      }}>
                        {e.severity}
                      </span>
                    </td>
                    <td style={{ padding: '6px 10px', color: 'var(--text-secondary)' }}>{e.message}</td>
                    <td style={{ padding: '6px 10px' }}>
                      {e.acknowledged
                        ? <span style={{ color: '#22c55e', fontSize: 12 }}>{t('backoffice.yes')}</span>
                        : <span style={{ color: '#eab308', fontSize: 12 }}>{t('backoffice.no')}</span>
                      }
                    </td>
                  </tr>
                ))}
                {alerts.length === 0 && (
                  <tr>
                    <td colSpan={4} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>
                      {t('backoffice.noEvents')}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
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
