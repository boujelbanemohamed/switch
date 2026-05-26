import { useEffect, useState } from 'react';
import { api } from '../services/api';
import type { Participant } from '../types';
import { Plus } from 'lucide-react';

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

export function Participants() {
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.participants.list()
      .then(setParticipants)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h2 style={{ fontSize: 24, fontWeight: 700 }}>Participants</h2>
        <button style={{
          display: 'flex', alignItems: 'center', gap: 8,
          background: '#3b82f6', color: 'white', border: 'none',
          borderRadius: 8, padding: '8px 16px', fontSize: 14,
          fontWeight: 600, cursor: 'pointer',
        }}>
          <Plus size={16} /> Add Participant
        </button>
      </div>

      {loading ? (
        <div style={{ opacity: 0.5 }}>Loading...</div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: 16 }}>
          {participants.map(p => (
            <div key={p.id} style={{
              background: 'var(--surface)',
              borderRadius: 12,
              padding: 20,
              border: '1px solid var(--border)',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 12 }}>
                <div>
                  <p style={{ fontSize: 16, fontWeight: 600 }}>{p.name}</p>
                  <p style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{p.code}</p>
                </div>
                <span style={{
                  background: `${typeColors[p.type]}22`,
                  color: typeColors[p.type],
                  padding: '2px 8px',
                  borderRadius: 4,
                  fontSize: 11,
                  fontWeight: 600,
                }}>
                  {p.type}
                </span>
              </div>

              <div style={{ display: 'flex', gap: 16, fontSize: 13, color: 'var(--text-secondary)' }}>
                <span>Status: <span style={{ color: statusColors[p.status], fontWeight: 600 }}>{p.status}</span></span>
                {p.endpointUrl && <span>Endpoint: {p.endpointUrl}</span>}
              </div>

              {p.supportedProtocols && p.supportedProtocols.length > 0 && (
                <div style={{ marginTop: 12, display: 'flex', gap: 6 }}>
                  {p.supportedProtocols.map(proto => (
                    <span key={proto} style={{
                      background: 'rgba(255,255,255,0.05)',
                      padding: '2px 8px',
                      borderRadius: 4,
                      fontSize: 11,
                      color: 'var(--text-secondary)',
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
    </div>
  );
}
