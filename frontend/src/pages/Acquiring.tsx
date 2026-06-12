import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { Merchant, Terminal, MerchantSettlement, NettingRecord as NettingResult, Participant } from '../types';
import { SectionHeader } from '../components/SectionHeader';
import { AcquiringHelp, MERCHANT_STATUS_LABELS, TERMINAL_STATUS_LABELS, SETTLEMENT_STATUS_LABELS } from '../components/AcquiringHelp';
const TERMINAL_TYPE_OPTIONS = ['PHYSICAL_TPE', 'SOFT_POS', 'ECOMMERCE', 'MOTO', 'ATM', 'KIOSK', 'MOBILE'];
const COUNTRY_OPTIONS = [
  { code: 'TN', label: 'Tunisia' }, { code: 'DZ', label: 'Algeria' }, { code: 'MA', label: 'Morocco' },
  { code: 'LY', label: 'Libya' }, { code: 'MR', label: 'Mauritania' }, { code: 'SD', label: 'Sudan' },
  { code: 'EG', label: 'Egypt' }, { code: 'SN', label: 'Senegal' }, { code: 'CI', label: "Côte d'Ivoire" },
  { code: 'CM', label: 'Cameroon' }, { code: 'ML', label: 'Mali' }, { code: 'BF', label: 'Burkina Faso' },
  { code: 'BJ', label: 'Benin' }, { code: 'TG', label: 'Togo' }, { code: 'NE', label: 'Niger' },
  { code: 'TD', label: 'Chad' }, { code: 'FR', label: 'France' }, { code: 'BE', label: 'Belgium' },
  { code: 'CH', label: 'Switzerland' }, { code: 'IT', label: 'Italy' }, { code: 'ES', label: 'Spain' },
  { code: 'DE', label: 'Germany' }, { code: 'GB', label: 'United Kingdom' }, { code: 'US', label: 'United States' },
  { code: 'CA', label: 'Canada' }, { code: 'AE', label: 'UAE' }, { code: 'SA', label: 'Saudi Arabia' },
  { code: 'QA', label: 'Qatar' }, { code: 'KW', label: 'Kuwait' }, { code: 'TR', label: 'Türkiye' },
  { code: 'CN', label: 'China' }, { code: 'IN', label: 'India' },
];

type Tab = 'merchants' | 'terminals' | 'settlement';

const styles = {
  overlay: { position: 'fixed' as const, inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 },
  modal: { background: 'var(--surface)', borderRadius: 16, padding: 28, width: 520, maxWidth: '90vw', maxHeight: '85vh', overflow: 'auto', border: '1px solid var(--border)' },
  input: { width: '100%', padding: '10px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13, boxSizing: 'border-box' as const },
  select: { width: '100%', padding: '10px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13, boxSizing: 'border-box' as const, cursor: 'pointer' },
};

const statusColors: Record<string, string> = {
  ACTIVE: '#22c55e', PENDING_APPROVAL: '#eab308', SUSPENDED: '#f97316', TERMINATED: '#ef4444',
};

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4, fontWeight: 500 }}>{label}</label>
      {children}
    </div>
  );
}

function MiniBtn({ label, color, onClick }: { label: string; color: string; onClick: () => void }) {
  return (
    <button onClick={onClick} style={{
      padding: '4px 8px', border: `1px solid ${color}33`, borderRadius: 4,
      background: color + '15', color, cursor: 'pointer', fontSize: 11, fontWeight: 600, whiteSpace: 'nowrap',
    }}>{label}</button>
  );
}

function StatusBadge({ status, label }: { status: string; label?: string }) {
  const color = statusColors[status] || '#6b7280';
  return <span style={{ background: color + '33', color, padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600 }}>{label ?? status}</span>;
}

export function Acquiring() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('merchants');
  const [merchants, setMerchants] = useState<Merchant[]>([]);
  const [terminals, setTerminals] = useState<Terminal[]>([]);
  const [settlements, setSettlements] = useState<MerchantSettlement[]>([]);
  const [netting, setNetting] = useState<NettingResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedMerchant, setSelectedMerchant] = useState<Merchant | null>(null);

  const [showMerchantModal, setShowMerchantModal] = useState(false);
  const [showTerminalModal, setShowTerminalModal] = useState(false);
  const [showSettlementModal, setShowSettlementModal] = useState(false);
  const [saving, setSaving] = useState(false);

  const [merchantForm, setMerchantForm] = useState({
    name: '', mcc: '5999', country: 'TN', status: 'PENDING_ONBOARDING',
    contactEmail: '', contactPhone: '', address: '',
    acquiringParticipantId: '',
  });
  const [participants, setParticipants] = useState<Participant[]>([]);

  const [terminalForm, setTerminalForm] = useState({
    merchantId: '', terminalId: '', serialNumber: '', terminalType: 'PHYSICAL_TPE', model: '', location: '', status: 'ACTIVE',
  });
  const [keyForm, setKeyForm] = useState({ tid: '', mKey: '', pik: '', mak: '' });

  const [settlementForm, setSettlementForm] = useState({ merchantId: '', from: '', to: '', settlementDate: '', currencyCode: 'TND' });
  const [settlementResult, setSettlementResult] = useState<string | null>(null);

  const loadMerchants = () => api.acquiring.merchants.list().then(setMerchants).catch(console.error);
  const loadParticipants = () => api.participants.list()
    .then(list => setParticipants(list.filter(p => p.type === 'ACQUIRER')))
    .catch(console.error);

  useEffect(() => {
    Promise.all([loadMerchants(), loadParticipants()]).finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (selectedMerchant) {
      api.acquiring.terminals.listByMerchant(selectedMerchant.id).then(setTerminals).catch(() => setTerminals([]));
    }
  }, [selectedMerchant]);

  const createMerchant = async () => {
    setSaving(true);
    try {
      const merchantId = merchantForm.name.toUpperCase().replace(/[^A-Z0-9]/g, '_').slice(0, 15);
      const payload: Record<string, unknown> = {
        merchantId,
        legalName: merchantForm.name,
        tradingName: merchantForm.name,
        merchantCategoryCode: merchantForm.mcc,
        countryCode: merchantForm.country,
        email: merchantForm.contactEmail,
        phone: merchantForm.contactPhone,
        addressLine1: merchantForm.address,
      };
      if (merchantForm.acquiringParticipantId) {
        payload.acquiringParticipant = { id: merchantForm.acquiringParticipantId };
      }
      await api.acquiring.merchants.create(payload as Partial<Merchant>);
      setShowMerchantModal(false);
      setMerchantForm({ name: '', mcc: '5999', country: 'TN', status: 'ACTIVE', contactEmail: '', contactPhone: '', address: '', acquiringParticipantId: '' });
      await loadMerchants();
    } catch (e) { console.error(e); alert(e instanceof Error ? e.message : 'Failed'); }
    setSaving(false);
  };

  const registerTerminal = async () => {
    setSaving(true);
    try {
      await api.acquiring.terminals.register(terminalForm);
      setShowTerminalModal(false);
      setTerminalForm({ merchantId: '', terminalId: '', serialNumber: '', terminalType: 'PHYSICAL_TPE', model: '', location: '', status: 'ACTIVE' });
      if (selectedMerchant) {
        const list = await api.acquiring.terminals.listByMerchant(selectedMerchant.id);
        setTerminals(list);
      }
    } catch (e) { console.error(e); alert(e instanceof Error ? e.message : 'Failed'); }
    setSaving(false);
  };

  const injectKeys = async () => {
    try {
      await api.acquiring.terminals.injectKeys(keyForm.tid, keyForm.mKey, keyForm.pik, keyForm.mak);
      alert(t('acquiring.keysInjected'));
      setKeyForm({ tid: '', mKey: '', pik: '', mak: '' });
    } catch (e) { alert(e instanceof Error ? e.message : 'Failed'); }
  };

  const createSettlement = async () => {
    setSaving(true);
    try {
      await api.acquiring.settlements.create(settlementForm.merchantId, settlementForm.settlementDate, settlementForm.currencyCode);
      setShowSettlementModal(false);
      setSettlementResult('Settlement created');
      if (settlementForm.merchantId && settlementForm.from && settlementForm.to) {
        const list = await api.acquiring.settlements.listByMerchant(settlementForm.merchantId, settlementForm.from, settlementForm.to);
        setSettlements(list);
        const net = await api.acquiring.netting.calculate(settlementForm.merchantId, settlementForm.from);
        setNetting(net as unknown as NettingResult);
      }
      setSettlementForm({ merchantId: '', from: '', to: '', settlementDate: '', currencyCode: 'TND' });
    } catch (e) { console.error(e); alert(e instanceof Error ? e.message : 'Failed'); }
    setSaving(false);
  };

  const loadSettlements = async (merchantId: string, from: string, to: string) => {
    try {
      const list = await api.acquiring.settlements.listByMerchant(merchantId, from, to);
      setSettlements(list);
      const net = await api.acquiring.netting.calculate(merchantId, from);
      setNetting(net as unknown as NettingResult);
    } catch { setSettlements([]); setNetting(null); }
  };

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;

  const tabs: { key: Tab; label: string }[] = [
    { key: 'merchants', label: t('acquiring.tabMerchants') },
    { key: 'terminals', label: t('acquiring.tabTerminals') },
    { key: 'settlement', label: t('acquiring.tabSettlement') },
  ];

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 24 }}>
        <h2 style={{ fontSize: 24, fontWeight: 700, margin: 0 }}>{t('acquiring.title')}</h2>
        <AcquiringHelp />
      </div>
      <SectionHeader sectionKey="acquiring" />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <StatCard title={t('acquiring.totalMerchants')} value={merchants.length.toLocaleString()} />
        <StatCard title={t('acquiring.activeTerminals')} value={terminals.filter(tm => tm.status === 'ACTIVE').length.toLocaleString()} />
        <StatCard title={t('acquiring.pendingSettlements')} value={settlements.filter(s => s.status === 'PENDING').length.toLocaleString()} />
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        {tabs.map(tk => (
          <button key={tk.key} onClick={() => setTab(tk.key)} style={{
            padding: '8px 16px', border: '1px solid var(--border)', borderRadius: 8,
            background: tab === tk.key ? '#3b82f6' : 'var(--surface)',
            color: tab === tk.key ? '#fff' : 'var(--text)', cursor: 'pointer', fontWeight: 600, fontSize: 13,
          }}>{tk.label}</button>
        ))}
      </div>

      {tab === 'merchants' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ fontSize: 16, fontWeight: 600 }}>{t('acquiring.merchants')}</h3>
            <button onClick={() => setShowMerchantModal(true)} style={{
              display: 'flex', alignItems: 'center', gap: 8, background: '#3b82f6', color: 'white',
              border: 'none', borderRadius: 8, padding: '8px 16px', fontSize: 14, fontWeight: 600, cursor: 'pointer',
            }}>{t('acquiring.addMerchant')}</button>
          </div>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.code')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.name')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.mcc')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.country')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.contactEmail')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.status')}</th>
                </tr>
              </thead>
              <tbody>
                {merchants.map(m => (
                  <tr key={m.id}
                    onClick={() => setSelectedMerchant(m)}
                    style={{ borderBottom: '1px solid var(--border)', cursor: 'pointer',
                      background: selectedMerchant?.id === m.id ? 'rgba(59,130,246,0.1)' : undefined }}>
                    <td style={{ padding: '10px 12px', fontFamily: 'monospace', fontWeight: 600 }}>{m.code}</td>
                    <td style={{ padding: '10px 12px' }}>{m.name}</td>
                    <td style={{ padding: '10px 12px', color: 'var(--text-secondary)' }}>{m.categoryCode || '-'}</td>
                    <td style={{ padding: '10px 12px' }}>{m.countryCode || '-'}</td>
                    <td style={{ padding: '10px 12px', color: 'var(--text-secondary)' }}>-</td>
                    <td style={{ padding: '10px 12px' }}><StatusBadge status={m.status} label={MERCHANT_STATUS_LABELS[m.status] ?? m.status} /></td>
                  </tr>
                ))}
                {merchants.length === 0 && (
                  <tr><td colSpan={6} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('acquiring.noMerchants')}</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {tab === 'terminals' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ fontSize: 16, fontWeight: 600 }}>{t('acquiring.terminals')}</h3>
            <button onClick={() => setShowTerminalModal(true)} style={{
              display: 'flex', alignItems: 'center', gap: 8, background: '#3b82f6', color: 'white',
              border: 'none', borderRadius: 8, padding: '8px 16px', fontSize: 14, fontWeight: 600, cursor: 'pointer',
            }}>{t('acquiring.registerTerminal')}</button>
          </div>
          {!selectedMerchant ? (
            <p style={{ color: 'var(--text-secondary)' }}>{t('issuing.selectCardholder')}</p>
          ) : (
            <>
              <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 12 }}>Merchant: {selectedMerchant.name}</p>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.terminalId')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.serialNumber')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.model')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.location')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.status')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {terminals.map(tm => (
                    <tr key={tm.id} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '10px 12px', fontFamily: 'monospace', fontWeight: 600 }}>{tm.terminalId}</td>
                      <td style={{ padding: '10px 12px' }}>{tm.serialNumber || '-'}</td>
                      <td style={{ padding: '10px 12px' }}>{tm.type || '-'}</td>
                      <td style={{ padding: '10px 12px' }}>{tm.location || '-'}</td>
                      <td style={{ padding: '10px 12px' }}><StatusBadge status={tm.status} label={TERMINAL_STATUS_LABELS[tm.status] ?? tm.status} /></td>
                      <td style={{ padding: '10px 12px', display: 'flex', gap: 4 }}>
                        <MiniBtn label={t('acquiring.injectKeys')} color="#3b82f6" onClick={() => {
                          setKeyForm({ ...keyForm, tid: tm.terminalId });
                          injectKeys();
                        }} />
                      </td>
                    </tr>
                  ))}
                  {terminals.length === 0 && (
                    <tr><td colSpan={6} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('acquiring.noTerminals')}</td></tr>
                  )}
                </tbody>
              </table>
            </>
          )}
          <div style={{ marginTop: 20, padding: 16, background: 'var(--bg)', borderRadius: 8, border: '1px solid var(--border)' }}>
            <h4 style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>{t('acquiring.injectKeys')}</h4>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: 8, alignItems: 'flex-end' }}>
              <Field label="Terminal ID">
                <input style={styles.input} value={keyForm.tid} onChange={e => setKeyForm({ ...keyForm, tid: e.target.value })} />
              </Field>
              <Field label="MKey"><input style={styles.input} value={keyForm.mKey} onChange={e => setKeyForm({ ...keyForm, mKey: e.target.value })} /></Field>
              <Field label="PIK"><input style={styles.input} value={keyForm.pik} onChange={e => setKeyForm({ ...keyForm, pik: e.target.value })} /></Field>
              <Field label="MAK"><input style={styles.input} value={keyForm.mak} onChange={e => setKeyForm({ ...keyForm, mak: e.target.value })} /></Field>
            </div>
            <button onClick={injectKeys} style={{
              marginTop: 12, padding: '8px 20px', borderRadius: 8, border: 'none', background: '#f59e0b',
              color: 'white', fontWeight: 600, cursor: 'pointer', fontSize: 13,
            }}>{t('acquiring.injectKeys')}</button>
          </div>
        </div>
      )}

      {tab === 'settlement' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ fontSize: 16, fontWeight: 600 }}>{t('acquiring.settlements')}</h3>
            <button onClick={() => setShowSettlementModal(true)} style={{
              display: 'flex', alignItems: 'center', gap: 8, background: '#3b82f6', color: 'white',
              border: 'none', borderRadius: 8, padding: '8px 16px', fontSize: 14, fontWeight: 600, cursor: 'pointer',
            }}>{t('acquiring.createSettlement')}</button>
          </div>

          <div style={{ display: 'flex', gap: 12, marginBottom: 20, alignItems: 'flex-end' }}>
            <Field label={t('acquiring.merchants')}>
              <select style={styles.select} value={settlementForm.merchantId} onChange={e => setSettlementForm({ ...settlementForm, merchantId: e.target.value })}>
                <option value="">-- Select --</option>
                {merchants.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
              </select>
            </Field>
            <Field label={t('issuing.created')}>
              <input style={styles.input} type="date" value={settlementForm.from} onChange={e => setSettlementForm({ ...settlementForm, from: e.target.value })} />
            </Field>
            <Field label="To">
              <input style={styles.input} type="date" value={settlementForm.to} onChange={e => setSettlementForm({ ...settlementForm, to: e.target.value })} />
            </Field>
            <button onClick={() => settlementForm.merchantId && settlementForm.from && settlementForm.to && loadSettlements(settlementForm.merchantId, settlementForm.from, settlementForm.to)} style={{
              padding: '8px 20px', borderRadius: 8, border: 'none', background: '#3b82f6',
              color: 'white', fontWeight: 600, cursor: 'pointer', fontSize: 13,
            }}>Load</button>
          </div>

          {settlements.length > 0 && (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13, marginBottom: 24 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.settlementDate')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.totalAmount')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.totalFee')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.netAmount')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('acquiring.settlementStatus')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {settlements.map(s => (
                  <tr key={s.id} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '10px 12px' }}>{new Date(s.settlementDate).toLocaleDateString()}</td>
                    <td style={{ padding: '10px 12px', fontFamily: 'monospace' }}>{s.totalAmount.toLocaleString()} {s.currencyCode}</td>
                    <td style={{ padding: '10px 12px', fontFamily: 'monospace', color: '#ef4444' }}>{s.totalFee.toLocaleString()} {s.currencyCode}</td>
                    <td style={{ padding: '10px 12px', fontFamily: 'monospace', fontWeight: 700, color: s.netAmount >= 0 ? '#22c55e' : '#ef4444' }}>{s.netAmount.toLocaleString()} {s.currencyCode}</td>
                    <td style={{ padding: '10px 12px' }}><StatusBadge status={s.status} label={SETTLEMENT_STATUS_LABELS[s.status] ?? s.status} /></td>
                    <td style={{ padding: '10px 12px', display: 'flex', gap: 4 }}>
                      {s.status === 'PENDING' && (
                        <MiniBtn label={t('acquiring.confirmSettlement')} color="#22c55e" onClick={async () => {
                          await api.acquiring.settlements.confirm(s.id);
                          if (settlementForm.merchantId && settlementForm.from && settlementForm.to)
                            loadSettlements(settlementForm.merchantId, settlementForm.from, settlementForm.to);
                        }} />
                      )}
                      {s.status === 'CONFIRMED' && (
                        <MiniBtn label={t('acquiring.markPaid')} color="#3b82f6" onClick={async () => {
                          const ref = prompt('Payment ref:');
                          if (ref) { await api.acquiring.settlements.markPaid(s.id, ref); loadSettlements(settlementForm.merchantId, settlementForm.from, settlementForm.to); }
                        }} />
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {netting && (
            <div style={{ background: 'var(--bg)', borderRadius: 8, padding: 16, border: '1px solid var(--border)' }}>
              <h4 style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>{t('acquiring.nettingResults')}</h4>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
                <div>
                  <p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('acquiring.grossDebit')}</p>
                  <p style={{ fontSize: 20, fontWeight: 700, color: '#ef4444' }}>{(netting as any).grossDebit || '-'}</p>
                </div>
                <div>
                  <p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('acquiring.grossCredit')}</p>
                  <p style={{ fontSize: 20, fontWeight: 700, color: '#22c55e' }}>{(netting as any).grossCredit || '-'}</p>
                </div>
                <div>
                  <p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('acquiring.net')}</p>
                  <p style={{ fontSize: 20, fontWeight: 700 }}>{(netting as any).netAmount || '-'}</p>
                </div>
              </div>
            </div>
          )}

          {settlements.length === 0 && !netting && (
            <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{t('acquiring.noSettlements')}</p>
          )}
        </div>
      )}

      {showMerchantModal && (
        <div style={styles.overlay} onClick={() => setShowMerchantModal(false)}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 20 }}>{t('acquiring.addMerchant')}</h3>
            <div style={{ display: 'grid', gap: 14 }}>
              <Field label={t('acquiring.name')}><input style={styles.input} value={merchantForm.name} onChange={e => setMerchantForm({ ...merchantForm, name: e.target.value })} /></Field>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('acquiring.mcc')}><input style={styles.input} value={merchantForm.mcc} onChange={e => setMerchantForm({ ...merchantForm, mcc: e.target.value })} /></Field>
                <Field label={t('acquiring.country')}>
                  <select style={styles.select} value={merchantForm.country} onChange={e => setMerchantForm({ ...merchantForm, country: e.target.value })}>
                    {COUNTRY_OPTIONS.map(c => <option key={c.code} value={c.code}>{c.label}</option>)}
                  </select>
                </Field>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('acquiring.contactEmail')}><input style={styles.input} type="email" value={merchantForm.contactEmail} onChange={e => setMerchantForm({ ...merchantForm, contactEmail: e.target.value })} /></Field>
                <Field label={t('acquiring.contactPhone')}><input style={styles.input} value={merchantForm.contactPhone} onChange={e => setMerchantForm({ ...merchantForm, contactPhone: e.target.value })} /></Field>
              </div>
              <Field label={t('acquiring.address')}><input style={styles.input} value={merchantForm.address} onChange={e => setMerchantForm({ ...merchantForm, address: e.target.value })} /></Field>
              <Field label={t('acquiring.status')}>
                <select style={styles.select} value={merchantForm.status} onChange={e => setMerchantForm({ ...merchantForm, status: e.target.value })}>
                  {['ACTIVE', 'PENDING_ONBOARDING', 'SUSPENDED', 'TERMINATED', 'UNDER_REVIEW'].map(s => <option key={s} value={s}>{MERCHANT_STATUS_LABELS[s] ?? s}</option>)}
                </select>
              </Field>
              <Field label="Acquiring Bank">
                <select style={styles.select} value={merchantForm.acquiringParticipantId} onChange={e => setMerchantForm({ ...merchantForm, acquiringParticipantId: e.target.value })}>
                  <option value="">-- None --</option>
                  {participants.map(p => <option key={p.id} value={p.id}>{p.name} ({p.code})</option>)}
                </select>
              </Field>
            </div>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 24 }}>
              <button onClick={() => setShowMerchantModal(false)} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>{t('fraud.cancel')}</button>
              <button onClick={createMerchant} disabled={saving || !merchantForm.name} style={{
                padding: '10px 20px', borderRadius: 8, border: 'none',
                background: saving || !merchantForm.name ? '#64748b' : '#3b82f6',
                color: 'white', cursor: saving ? 'not-allowed' : 'pointer', fontSize: 13, fontWeight: 600,
              }}>{saving ? t('common.loading') : t('issuing.create')}</button>
            </div>
          </div>
        </div>
      )}

      {showTerminalModal && (
        <div style={styles.overlay} onClick={() => setShowTerminalModal(false)}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 20 }}>{t('acquiring.registerTerminal')}</h3>
            <div style={{ display: 'grid', gap: 14 }}>
              <Field label={t('acquiring.merchants')}>
                <select style={styles.select} value={terminalForm.merchantId} onChange={e => setTerminalForm({ ...terminalForm, merchantId: e.target.value })}>
                  <option value="">-- Select --</option>
                  {merchants.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
                </select>
              </Field>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('acquiring.terminalId')}><input style={styles.input} value={terminalForm.terminalId} onChange={e => setTerminalForm({ ...terminalForm, terminalId: e.target.value })} /></Field>
                <Field label={t('acquiring.serialNumber')}><input style={styles.input} value={terminalForm.serialNumber} onChange={e => setTerminalForm({ ...terminalForm, serialNumber: e.target.value })} /></Field>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('acquiring.model')}><input style={styles.input} value={terminalForm.model} onChange={e => setTerminalForm({ ...terminalForm, model: e.target.value })} /></Field>
                <Field label={t('acquiring.location')}><input style={styles.input} value={terminalForm.location} onChange={e => setTerminalForm({ ...terminalForm, location: e.target.value })} /></Field>
              </div>
              <Field label={t('acquiring.terminalType')}>
                <select style={styles.select} value={terminalForm.terminalType} onChange={e => setTerminalForm({ ...terminalForm, terminalType: e.target.value })}>
                  {TERMINAL_TYPE_OPTIONS.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </Field>
              <Field label={t('acquiring.status')}>
                <select style={styles.select} value={terminalForm.status} onChange={e => setTerminalForm({ ...terminalForm, status: e.target.value })}>
                  {['ACTIVE', 'INACTIVE', 'SUSPENDED'].map(s => <option key={s} value={s}>{TERMINAL_STATUS_LABELS[s] ?? s}</option>)}
                </select>
              </Field>
            </div>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 24 }}>
              <button onClick={() => setShowTerminalModal(false)} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>{t('fraud.cancel')}</button>
              <button onClick={registerTerminal} disabled={saving || !terminalForm.merchantId || !terminalForm.terminalId} style={{
                padding: '10px 20px', borderRadius: 8, border: 'none',
                background: saving || !terminalForm.merchantId || !terminalForm.terminalId ? '#64748b' : '#3b82f6',
                color: 'white', cursor: saving ? 'not-allowed' : 'pointer', fontSize: 13, fontWeight: 600,
              }}>{saving ? t('common.loading') : t('issuing.create')}</button>
            </div>
          </div>
        </div>
      )}

      {showSettlementModal && (
        <div style={styles.overlay} onClick={() => setShowSettlementModal(false)}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 20 }}>{t('acquiring.createSettlement')}</h3>
            <div style={{ display: 'grid', gap: 14 }}>
              <Field label={t('acquiring.merchants')}>
                <select style={styles.select} value={settlementForm.merchantId} onChange={e => setSettlementForm({ ...settlementForm, merchantId: e.target.value })}>
                  <option value="">-- Select --</option>
                  {merchants.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
                </select>
              </Field>
              <Field label={t('acquiring.settlementDate')}><input style={styles.input} type="date" value={settlementForm.settlementDate} onChange={e => setSettlementForm({ ...settlementForm, settlementDate: e.target.value })} /></Field>
              <Field label={t('issuing.currency')}>
                <select style={styles.select} value={settlementForm.currencyCode} onChange={e => setSettlementForm({ ...settlementForm, currencyCode: e.target.value })}>
                  {['TND', 'EUR', 'USD'].map(c => <option key={c}>{c}</option>)}
                </select>
              </Field>
            </div>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 24 }}>
              <button onClick={() => setShowSettlementModal(false)} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>{t('fraud.cancel')}</button>
              <button onClick={createSettlement} disabled={saving || !settlementForm.merchantId || !settlementForm.settlementDate} style={{
                padding: '10px 20px', borderRadius: 8, border: 'none',
                background: saving || !settlementForm.merchantId || !settlementForm.settlementDate ? '#64748b' : '#3b82f6',
                color: 'white', cursor: saving ? 'not-allowed' : 'pointer', fontSize: 13, fontWeight: 600,
              }}>{saving ? t('common.loading') : t('issuing.create')}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function StatCard({ title, value }: { title: string; value: string }) {
  return (
    <div style={{ background: 'var(--surface)', borderRadius: 12, padding: '16px 20px', border: '1px solid var(--border)' }}>
      <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 8 }}>{title}</p>
      <p style={{ fontSize: 28, fontWeight: 700 }}>{value}</p>
    </div>
  );
}
