import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { BinTable } from '../types';
import { SectionHeader } from '../components/SectionHeader';
import { ChevronDown, ChevronRight, Terminal } from 'lucide-react';

const brandColors: Record<string, string> = {
  VISA: '#1a1f71',
  MASTERCARD: '#eb001b',
  AMEX: '#2e77bc',
  CB: '#0066b3',
  OTHER: '#64748b',
};

export function BinTables() {
  const { t } = useTranslation();
  const [tables, setTables] = useState<BinTable[]>([]);
  const [loading, setLoading] = useState(true);
  const [guideOpen, setGuideOpen] = useState(false);
  const [participantsOpen, setParticipantsOpen] = useState(false);

  useEffect(() => {
    api.binTables.list()
      .then(setTables)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('nav.binTables')}</h2>

      <SectionHeader sectionKey="binTables" />

      <div style={{ marginBottom: 24 }}>
        <button
          onClick={() => setGuideOpen(!guideOpen)}
          style={{
            display: 'flex', alignItems: 'center', gap: 8,
            background: 'var(--surface)', border: '1px solid var(--border)',
            borderRadius: 10, padding: '8px 16px', width: '100%',
            cursor: 'pointer', color: 'var(--text)', fontSize: 13, fontWeight: 500,
          }}
        >
          {guideOpen ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
          <Terminal size={14} />
          <span>Comment ajouter un BIN ?</span>
        </button>

        {guideOpen && (
          <div style={{ marginTop: 8, padding: 20, background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 10 }}>
            <div style={{ marginBottom: 16 }}>
              <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8 }}>Via l'API REST (curl)</div>
              <pre style={{ fontSize: 12, background: 'var(--bg)', padding: 12, borderRadius: 8, overflowX: 'auto', lineHeight: 1.6 }}>
{`curl -X POST http://localhost:3000/api/v1/admin/bin-tables \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer $TOKEN" \\
  -d '{
    "bin": "123456",
    "binLength": 6,
    "participantId": "UUID_DU_PARTICIPANT",
    "cardBrand": "VISA",
    "cardType": "CREDIT",
    "countryCode": "TN",
    "currencyCode": "TND"
  }'`}
              </pre>
            </div>

            <div style={{ marginBottom: participantsOpen ? 16 : 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8 }}>Via SQL</div>
              <pre style={{ fontSize: 12, background: 'var(--bg)', padding: 12, borderRadius: 8, overflowX: 'auto', lineHeight: 1.6 }}>
{`INSERT INTO bin_tables (bin, bin_length, participant_id, card_brand, card_type, country_code, currency_code)
SELECT '123456', 6, id, 'VISA', 'CREDIT', 'TN', 'TND'
FROM participants WHERE code = 'SIB';`}
              </pre>
            </div>

            <div>
              <button
                onClick={() => setParticipantsOpen(!participantsOpen)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#3b82f6', fontSize: 13, fontWeight: 600, padding: 0 }}
              >
                {participantsOpen ? '▼' : '▶'} Voir les participants disponibles
              </button>
              {participantsOpen && (
                <div style={{ marginTop: 8 }}>
                  {tables.length === 0 ? (
                    <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                      Aucun participant chargé
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                      {Array.from(new Map(tables.filter(bt => bt.participant).map(bt => [bt.participant!.id, bt.participant!])).values()).map(p => (
                        <div key={p.id} style={{ fontSize: 12, padding: '4px 8px', background: 'var(--bg)', borderRadius: 6 }}>
                          <strong>{p.code}</strong> — {p.name} <span style={{ color: 'var(--text-secondary)', fontFamily: 'monospace' }}>({p.id})</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {loading ? (
        <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 16 }}>
          {tables.map(bt => (
            <div key={bt.id} style={{
              background: 'var(--surface)',
              borderRadius: 12,
              padding: 20,
              border: '1px solid var(--border)',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
                <span style={{ fontSize: 18, fontWeight: 700, fontFamily: 'monospace' }}>
                  {bt.bin}{'*'.repeat(16 - bt.binLength)}
                </span>
                {bt.cardBrand && (
                  <span style={{
                    background: brandColors[bt.cardBrand] || '#64748b',
                    color: '#fff',
                    padding: '3px 10px',
                    borderRadius: 5,
                    fontSize: 12,
                    fontWeight: 700,
                    letterSpacing: '0.02em',
                  }}>
                    {bt.cardBrand === 'VISA' ? 'VISA' :
                     bt.cardBrand === 'MASTERCARD' ? 'MASTERCARD' :
                     bt.cardBrand === 'AMEX' ? 'AMEX' :
                     bt.cardBrand === 'CB' ? 'CB' : bt.cardBrand}
                  </span>
                )}
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, fontSize: 13 }}>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>{t('binTables.length')}: </span>
                  {bt.binLength}
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>{t('binTables.type')}: </span>
                  {bt.cardType || t('binTables.na')}
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>{t('binTables.country')}: </span>
                  {bt.countryCode || t('binTables.na')}
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>{t('binTables.currency')}: </span>
                  {bt.currencyCode || t('binTables.na')}
                </div>
              </div>

              <div style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid var(--border)', fontSize: 12, color: 'var(--text-secondary)' }}>
                {bt.participant?.name || bt.participant?.code}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
