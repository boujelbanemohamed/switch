import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { RoutingRule } from '../types';

export function RoutingRules() {
  const { t } = useTranslation();
  const [rules, setRules] = useState<RoutingRule[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.routingRules.list()
      .then(setRules)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('switching.routingRules')}</h2>

      {loading ? (
        <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>
      ) : (
        <div style={{ background: 'var(--surface)', borderRadius: 12, overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                {[t('switching.name'), t('switching.priority'), t('switching.source'), t('switching.destination'), t('switching.protocol'), t('switching.messageType'), t('switching.status')].map(h => (
                  <th key={h} style={{ padding: '12px 16px', fontSize: 12, color: 'var(--text-secondary)', fontWeight: 600 }}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rules.map(rule => (
                <tr key={rule.id} style={{ borderBottom: '1px solid var(--border)', transition: 'background 0.15s' }}
                  onMouseEnter={e => (e.currentTarget.style.background = 'var(--surface-2)')}
                  onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                >
                  <td style={{ padding: '12px 16px', fontWeight: 500 }}>{rule.name}</td>
                  <td style={{ padding: '12px 16px' }}>
                    <span style={{
                      background: 'rgba(255,255,255,0.05)',
                      padding: '2px 8px',
                      borderRadius: 4,
                      fontSize: 12,
                    }}>
                      {rule.priority}
                    </span>
                  </td>
                  <td style={{ padding: '12px 16px', fontSize: 13 }}>
                    {rule.sourceParticipant?.code || <span style={{ color: 'var(--text-secondary)' }}>{t('switching.any')}</span>}
                  </td>
                  <td style={{ padding: '12px 16px', fontSize: 13 }}>
                    {rule.destinationParticipant?.code}
                  </td>
                  <td style={{ padding: '12px 16px', fontSize: 13 }}>{rule.protocol}</td>
                  <td style={{ padding: '12px 16px', fontSize: 13 }}>
                    {rule.messageType || <span style={{ color: 'var(--text-secondary)' }}>{t('switching.all')}</span>}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <span style={{
                      background: rule.status === 'ACTIVE' ? 'rgba(34,197,94,0.15)' : 'rgba(148,163,184,0.15)',
                      color: rule.status === 'ACTIVE' ? '#22c55e' : '#94a3b8',
                      padding: '2px 8px',
                      borderRadius: 4,
                      fontSize: 12,
                      fontWeight: 600,
                    }}>
                      {rule.status}
                    </span>
                  </td>
                </tr>
              ))}
              {rules.length === 0 && (
                <tr>
                  <td colSpan={7} style={{ padding: 32, textAlign: 'center', color: 'var(--text-secondary)' }}>
                    {t('switching.noRules')}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
