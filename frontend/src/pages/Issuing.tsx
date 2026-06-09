import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { Cardholder, Card, CardAccount as CardAccountType, Notification as NotificationType } from '../types';
import { IssuingHelp, CARD_STATUS_LABELS, ACCOUNT_STATUS_LABELS, CARDHOLDER_STATUS_LABELS, TOKEN_STATUS_LABELS, CARD_PRODUCT_LABELS, WALLET_PROVIDER_LABELS, CARD_ACTION_LABELS, getNotificationLabel } from '../components/IssuingHelp';
import { SectionHeader } from '../components/SectionHeader';

type Tab = 'cardholders' | 'cards' | 'accounts' | 'notifications' | 'pin' | 'tokenisation';

const styles = {
  overlay: { position: 'fixed' as const, inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 },
  modal: { background: 'var(--surface)', borderRadius: 16, padding: 28, width: 520, maxWidth: '90vw', maxHeight: '85vh', overflow: 'auto', border: '1px solid var(--border)' },
  input: { width: '100%', padding: '10px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13, boxSizing: 'border-box' as const },
  select: { width: '100%', padding: '10px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13, boxSizing: 'border-box' as const, cursor: 'pointer' },
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
  const colors: Record<string, string> = {
    ACTIVE: '#22c55e', BLOCKED: '#ef4444', PENDING: '#f59e0b',
    PENDING_ACTIVATION: '#f59e0b', LOST: '#ef4444', STOLEN: '#dc2626',
    RENEWED: '#3b82f6', SUSPENDED: '#f59e0b', TERMINATED: '#6b7280',
    INACTIVE: '#6b7280',
  };
  const color = colors[status] || '#6b7280';
  return <span style={{ background: color + '33', color, padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600 }}>{label ?? status}</span>;
}

export function Issuing() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('cardholders');
  const [cardholders, setCardholders] = useState<Cardholder[]>([]);
  const [cards, setCards] = useState<Card[]>([]);
  const [accounts, setAccounts] = useState<CardAccountType[]>([]);
  const [account, setAccount] = useState<CardAccountType | null>(null);
  const [notifications, setNotifications] = useState<NotificationType[]>([]);
  const [loading, setLoading] = useState(true);
  const [amount, setAmount] = useState('');
  const [selectedCh, setSelectedCh] = useState<Cardholder | null>(null);

  const [showChModal, setShowChModal] = useState(false);
  const [showCardModal, setShowCardModal] = useState(false);
  const [showTokenModal, setShowTokenModal] = useState(false);
  const [saving, setSaving] = useState(false);

  const [chForm, setChForm] = useState({ firstName: '', lastName: '', email: '', phone: '', documentNumber: '', dateOfBirth: '', nationality: '' });
  const [cardForm, setCardForm] = useState({ cardholderId: '', cardProduct: 'CREDIT', cardNumber: '', expiryDate: '', cvv: '', status: 'PENDING_ACTIVATION' });
  const [pinCardId, setPinCardId] = useState('');
  const [rawPin, setRawPin] = useState('');
  const [pinBlock, setPinBlock] = useState('');
  const [pinResult, setPinResult] = useState<string | null>(null);
  const [oldPinBlock, setOldPinBlock] = useState('');
  const [newPinBlock, setNewPinBlock] = useState('');

  const [tokens, setTokens] = useState<{ uuid: string; dpan: string; status: string }[]>([]);
  const [tokenForm, setTokenForm] = useState({ cardId: '', walletProvider: 'APPLE_PAY', deviceId: '', fpan: '' });
  const [tokenResult, setTokenResult] = useState<string | null>(null);

  useEffect(() => {
    api.issuing.cardholders.list()
      .then(setCardholders)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (selectedCh) {
      api.issuing.cards.listByCardholder(selectedCh.id).then(setCards).catch(console.error);
      api.issuing.accounts.listByCardholder(selectedCh.id).then(setAccounts).catch(console.error);
      api.issuing.notifications.listByCardholder(selectedCh.id).then(setNotifications).catch(console.error);
    }
  }, [selectedCh]);

  const loadTokenList = async () => {
    if (!tokenForm.cardId) return;
    try {
      const data = await api.issuing.tokenVault.listByCard(tokenForm.cardId);
      setTokens(data);
    } catch { setTokens([]); }
  };

  const handleCardAction = async (action: string, cardId: string, extra?: string) => {
    try {
      let updated: Card;
      switch (action) {
        case 'activate': updated = await api.issuing.cards.activate(cardId); break;
        case 'block': updated = await api.issuing.cards.block(cardId, extra || 'BLOCKED'); break;
        case 'unblock': updated = await api.issuing.cards.unblock(cardId); break;
        case 'lost': updated = await api.issuing.cards.reportLost(cardId); break;
        case 'stolen': updated = await api.issuing.cards.reportStolen(cardId); break;
        case 'renew': updated = await api.issuing.cards.renew(cardId); break;
        default: return;
      }
      setCards(prev => prev.map(c => c.id === cardId ? { ...c, ...updated } : c));
    } catch (e) { console.error(action, e); }
  };

  const handleAccountAction = async (action: string, accountId: string) => {
    try {
      const amt = parseFloat(amount);
      if (isNaN(amt) || amt <= 0) return;
      let updated: CardAccountType;
      switch (action) {
        case 'debit': updated = await api.issuing.accounts.debit(accountId, amt, 'TND'); break;
        case 'credit': updated = await api.issuing.accounts.credit(accountId, amt, 'TND'); break;
        case 'hold': updated = await api.issuing.accounts.hold(accountId, amt); break;
        case 'release': updated = await api.issuing.accounts.releaseHold(accountId, amt); break;
        default: return;
      }
      setAccount(updated);
      setAmount('');
    } catch (e) { console.error(action, e); }
  };

  const createCardholder = async () => {
    setSaving(true);
    try {
      await api.issuing.cardholders.create(chForm);
      setShowChModal(false);
      setChForm({ firstName: '', lastName: '', email: '', phone: '', documentNumber: '', dateOfBirth: '', nationality: '' });
      const list = await api.issuing.cardholders.list();
      setCardholders(list);
    } catch (e) { console.error(e); alert(e instanceof Error ? e.message : 'Failed to create cardholder'); }
    setSaving(false);
  };

  const createCard = async () => {
    setSaving(true);
    try {
      await api.issuing.cards.create(cardForm);
      setShowCardModal(false);
      setCardForm({ cardholderId: '', cardProduct: 'CREDIT', cardNumber: '', expiryDate: '', cvv: '', status: 'PENDING_ACTIVATION' });
      if (selectedCh) {
        const list = await api.issuing.cards.listByCardholder(selectedCh.id);
        setCards(list);
      }
    } catch (e) { console.error(e); alert(e instanceof Error ? e.message : 'Failed to create card'); }
    setSaving(false);
  };

  const handleSetPin = async () => {
    try {
      const res = await api.issuing.pins.setPin(pinCardId, rawPin, pinBlock);
      setPinResult(res.message);
    } catch (e) { setPinResult(e instanceof Error ? e.message : 'Error'); }
  };

  const handleVerifyPin = async () => {
    try {
      const res = await api.issuing.pins.verifyPin(pinCardId, pinBlock);
      setPinResult(res.verified ? t('issuing.pinVerified') : t('issuing.pinNotVerified'));
    } catch (e) { setPinResult(e instanceof Error ? e.message : 'Error'); }
  };

  const handleChangePin = async () => {
    try {
      const res = await api.issuing.pins.changePin(pinCardId, oldPinBlock, newPinBlock);
      setPinResult(res.changed ? t('issuing.pinChanged') : 'Failed');
    } catch (e) { setPinResult(e instanceof Error ? e.message : 'Error'); }
  };

  const handleTokenize = async () => {
    try {
      const res = await api.issuing.tokenVault.tokenize(tokenForm.cardId, tokenForm.walletProvider, tokenForm.deviceId, tokenForm.fpan || undefined);
      setTokenResult(`Token created: ${res.dpan}`);
      loadTokenList();
    } catch (e) { setTokenResult(e instanceof Error ? e.message : 'Error'); }
  };

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;

  const tabs: { key: Tab; label: string }[] = [
    { key: 'cardholders', label: t('issuing.tabCardholders') },
    { key: 'cards', label: t('issuing.tabCards') },
    { key: 'pin', label: t('issuing.tabPin') },
    { key: 'tokenisation', label: t('issuing.tabTokenisation') },
    { key: 'accounts', label: t('issuing.tabAccounts') },
    { key: 'notifications', label: t('issuing.tabNotifications') },
  ];

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 24 }}>
        <h2 style={{ fontSize: 24, fontWeight: 700, margin: 0 }}>{t('issuing.title')}</h2>
        <IssuingHelp />
      </div>
      <SectionHeader sectionKey="issuing" />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        <StatCard title={t('issuing.totalCardholders')} value={cardholders.length.toLocaleString()} />
        <StatCard title={t('issuing.activeCards')} value={cards.filter(c => c.status === 'ACTIVE').length.toLocaleString()} />
        <StatCard title={t('issuing.kycLevel3')} value={cardholders.filter(c => c.kycLevel >= 3).length.toLocaleString()} />
        <StatCard title={t('issuing.balance')} value={account ? account.availableBalance.toLocaleString() : '-'} />
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        {tabs.map(t => (
          <button key={t.key} onClick={() => { setTab(t.key); if (t.key === 'cardholders') setSelectedCh(null); }}
            style={{
              padding: '8px 16px', border: '1px solid var(--border)', borderRadius: 8,
              background: tab === t.key ? '#3b82f6' : 'var(--surface)',
              color: tab === t.key ? '#fff' : 'var(--text)', cursor: 'pointer', fontWeight: 600, fontSize: 13,
            }}>
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'cardholders' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ fontSize: 16, fontWeight: 600 }}>{t('issuing.cardholders')}</h3>
            <button onClick={() => setShowChModal(true)} style={{
              display: 'flex', alignItems: 'center', gap: 8, background: '#3b82f6', color: 'white',
              border: 'none', borderRadius: 8, padding: '8px 16px', fontSize: 14, fontWeight: 600, cursor: 'pointer',
            }}>{t('issuing.createCardholder')}</button>
          </div>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.name')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.email')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.phone')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.status')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.kycLevel')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.created')}</th>
              </tr>
            </thead>
            <tbody>
              {cardholders.map(ch => (
                <tr key={ch.id} onClick={() => setSelectedCh(ch)}
                  style={{ borderBottom: '1px solid var(--border)', cursor: 'pointer',
                    background: selectedCh?.id === ch.id ? 'rgba(59,130,246,0.1)' : undefined }}>
                  <td style={{ padding: '10px 12px', fontWeight: 600 }}>{ch.firstName} {ch.lastName}</td>
                  <td style={{ padding: '10px 12px' }}>{ch.email}</td>
                  <td style={{ padding: '10px 12px', color: 'var(--text-secondary)' }}>{ch.phone || '-'}</td>
                   <td style={{ padding: '10px 12px' }}><StatusBadge status={ch.status} label={CARDHOLDER_STATUS_LABELS[ch.status] ?? ch.status} /></td>
                  <td style={{ padding: '10px 12px', fontFamily: 'monospace' }}>{ch.kycLevel}</td>
                  <td style={{ padding: '10px 12px', fontSize: 12 }}>{new Date(ch.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
              {cardholders.length === 0 && (
                <tr><td colSpan={6} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('issuing.noCardholders')}</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'cards' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ fontSize: 16, fontWeight: 600 }}>
              {t('issuing.tabCards')} {selectedCh ? `- ${selectedCh.firstName} ${selectedCh.lastName}` : ''}
            </h3>
            <button onClick={() => setShowCardModal(true)} style={{
              display: 'flex', alignItems: 'center', gap: 8, background: '#3b82f6', color: 'white',
              border: 'none', borderRadius: 8, padding: '8px 16px', fontSize: 14, fontWeight: 600, cursor: 'pointer',
            }}>{t('issuing.createCard')}</button>
          </div>
          {!selectedCh ? (
            <p style={{ color: 'var(--text-secondary)' }}>{t('issuing.selectCardholder')}</p>
          ) : cards.length === 0 ? (
            <p style={{ color: 'var(--text-secondary)' }}>{t('issuing.noCards')}</p>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.panSuffix')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.cardBrand')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.cardType')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.status')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.expires')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.pansuffix')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.dailyLimit')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {cards.map(card => (
                  <tr key={card.id} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '10px 12px', fontFamily: 'monospace' }}>****{card.panSuffix}</td>
                    <td style={{ padding: '10px 12px' }}>{card.cardBrand}</td>
                    <td style={{ padding: '10px 12px' }}>{card.cardType}</td>
                    <td style={{ padding: '10px 12px' }}><StatusBadge status={card.status} label={CARD_STATUS_LABELS[card.status] ?? card.status} /></td>
                    <td style={{ padding: '10px 12px', fontSize: 12 }}>{card.expiresAt ? new Date(card.expiresAt).toLocaleDateString() : '-'}</td>
                    <td style={{ padding: '10px 12px', fontFamily: 'monospace', color: 'var(--text-secondary)' }}>{card.panSuffix}</td>
                    <td style={{ padding: '10px 12px', fontFamily: 'monospace' }}>{card.dailyLimit}</td>
                    <td style={{ padding: '10px 12px', display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                      <MiniBtn label={CARD_ACTION_LABELS['activate'] ?? 'Activer'} color="#22c55e" onClick={() => handleCardAction('activate', card.id)} />
                      <MiniBtn label={CARD_ACTION_LABELS['block'] ?? 'Bloquer'} color="#ef4444" onClick={() => handleCardAction('block', card.id, 'MANUAL_BLOCK')} />
                      <MiniBtn label={CARD_ACTION_LABELS['unblock'] ?? 'Débloquer'} color="#22c55e" onClick={() => handleCardAction('unblock', card.id)} />
                      <MiniBtn label={CARD_ACTION_LABELS['lost'] ?? 'Perdue'} color="#f59e0b" onClick={() => handleCardAction('lost', card.id)} />
                      <MiniBtn label={CARD_ACTION_LABELS['stolen'] ?? 'Volée'} color="#ef4444" onClick={() => handleCardAction('stolen', card.id)} />
                      <MiniBtn label={CARD_ACTION_LABELS['renew'] ?? 'Renouveler'} color="#3b82f6" onClick={() => handleCardAction('renew', card.id)} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {tab === 'pin' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('issuing.tabPin')}</h3>
          <div style={{ display: 'grid', gap: 16, maxWidth: 500 }}>
            <Field label={t('issuing.cardholder')}>
              <select style={styles.select} value={pinCardId} onChange={e => setPinCardId(e.target.value)}>
                <option value="">-- Select card ID --</option>
                {cards.map(c => <option key={c.id} value={c.id}>****{c.panSuffix} ({c.cardBrand})</option>)}
              </select>
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <Field label={t('issuing.setPin')}>
                <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>
                  <input style={styles.input} value={rawPin} onChange={e => setRawPin(e.target.value)} placeholder={t('issuing.rawPin')} />
                  <button onClick={handleSetPin} style={{
                    padding: '8px 16px', borderRadius: 8, border: 'none', background: '#3b82f6',
                    color: 'white', fontWeight: 600, cursor: 'pointer', fontSize: 12, whiteSpace: 'nowrap',
                  }}>{t('issuing.setPin')}</button>
                </div>
              </Field>
              <Field label={t('issuing.verifyPin')}>
                <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>
                  <input style={styles.input} value={pinBlock} onChange={e => setPinBlock(e.target.value)} placeholder={t('issuing.pinBlock')} />
                  <button onClick={handleVerifyPin} style={{
                    padding: '8px 16px', borderRadius: 8, border: 'none', background: '#22c55e',
                    color: 'white', fontWeight: 600, cursor: 'pointer', fontSize: 12, whiteSpace: 'nowrap',
                  }}>{t('issuing.verifyPin')}</button>
                </div>
              </Field>
            </div>
            <Field label={t('issuing.changePin')}>
              <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>
                <input style={{ ...styles.input, width: 140 }} value={oldPinBlock} onChange={e => setOldPinBlock(e.target.value)} placeholder={t('issuing.currentPin')} />
                <input style={{ ...styles.input, width: 140 }} value={newPinBlock} onChange={e => setNewPinBlock(e.target.value)} placeholder={t('issuing.newPin')} />
                <button onClick={handleChangePin} style={{
                  padding: '8px 16px', borderRadius: 8, border: 'none', background: '#f59e0b',
                  color: 'white', fontWeight: 600, cursor: 'pointer', fontSize: 12, whiteSpace: 'nowrap',
                }}>{t('issuing.changePin')}</button>
              </div>
            </Field>
            {pinResult && (
              <div style={{ padding: 12, borderRadius: 8, background: 'var(--bg)', border: '1px solid var(--border)', fontSize: 13 }}>
                <strong>{t('issuing.pinResult')}:</strong> {pinResult}
              </div>
            )}
          </div>
        </div>
      )}

      {tab === 'tokenisation' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('issuing.tabTokenisation')}</h3>
          <div style={{ display: 'grid', gap: 14, maxWidth: 500, marginBottom: 24 }}>
            <Field label={t('issuing.cardholder')}>
              <select style={styles.select} value={tokenForm.cardId} onChange={e => setTokenForm({ ...tokenForm, cardId: e.target.value })}>
                <option value="">-- Select card --</option>
                {cards.map(c => <option key={c.id} value={c.id}>****{c.panSuffix} ({c.cardBrand})</option>)}
              </select>
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <Field label={t('issuing.walletProvider')}>
                <select style={styles.select} value={tokenForm.walletProvider} onChange={e => setTokenForm({ ...tokenForm, walletProvider: e.target.value })}>
                  {['APPLE_PAY', 'GOOGLE_PAY', 'SAMSUNG_PAY', 'OTHER'].map(p => <option key={p} value={p}>{WALLET_PROVIDER_LABELS[p] ?? p}</option>)}
                </select>
              </Field>
              <Field label={t('issuing.deviceId')}>
                <input style={styles.input} value={tokenForm.deviceId} onChange={e => setTokenForm({ ...tokenForm, deviceId: e.target.value })} placeholder="optional" />
              </Field>
            </div>
            <Field label={`${t('issuing.fpan')} (optional)`}>
              <input style={styles.input} value={tokenForm.fpan} onChange={e => setTokenForm({ ...tokenForm, fpan: e.target.value })} placeholder="FPAN if different from card PAN" />
            </Field>
            <div style={{ display: 'flex', gap: 8 }}>
              <button onClick={handleTokenize} style={{
                padding: '8px 20px', borderRadius: 8, border: 'none', background: '#3b82f6',
                color: 'white', fontWeight: 600, cursor: 'pointer', fontSize: 13,
              }}>{t('issuing.tokenize')}</button>
              <button onClick={loadTokenList} style={{
                padding: '8px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'var(--bg)', color: 'var(--text)', fontWeight: 600, cursor: 'pointer', fontSize: 13,
              }}>{t('issuing.listTokens')}</button>
            </div>
            {tokenResult && (
              <div style={{ padding: 12, borderRadius: 8, background: 'var(--bg)', border: '1px solid var(--border)', fontSize: 13 }}>
                {tokenResult}
              </div>
            )}
          </div>

          {tokens.length > 0 && (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.dpan')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.tokenStatus')}</th>
                  <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {tokens.map(tk => (
                  <tr key={tk.uuid} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '10px 12px', fontFamily: 'monospace' }}>{tk.dpan}</td>
                    <td style={{ padding: '10px 12px' }}><StatusBadge status={tk.status} label={TOKEN_STATUS_LABELS[tk.status] ?? tk.status} /></td>
                    <td style={{ padding: '10px 12px', display: 'flex', gap: 4 }}>
                      <MiniBtn label={t('issuing.revokeToken')} color="#ef4444" onClick={async () => {
                        await api.issuing.tokenVault.suspend(tk.dpan); loadTokenList();
                      }} />
                      <MiniBtn label={t('issuing.activateToken')} color="#22c55e" onClick={async () => {
                        await api.issuing.tokenVault.activate(tk.dpan); loadTokenList();
                      }} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {tokens.length === 0 && tokenForm.cardId && (
            <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{t('issuing.noTokens')}</p>
          )}
        </div>
      )}

      {tab === 'accounts' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          {!selectedCh ? (
            <p style={{ color: 'var(--text-secondary)' }}>{t('issuing.selectCardholder')}</p>
          ) : accounts.length === 0 ? (
            <p style={{ color: 'var(--text-secondary)' }}>{t('issuing.noAccount')}</p>
          ) : accounts.map(acct => (
            <div key={acct.id} style={{ background: 'var(--bg)', borderRadius: 8, padding: 16, marginBottom: 12, border: '1px solid var(--border)' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 12 }}>
                <div>
                  <p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('issuing.balance')}</p>
                  <p style={{ fontSize: 18, fontWeight: 700 }}>{acct.balance.toLocaleString()} {acct.currencyCode}</p>
                </div>
                <div>
                  <p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('issuing.availableBalance')}</p>
                  <p style={{ fontSize: 18, fontWeight: 700, color: '#22c55e' }}>{acct.availableBalance.toLocaleString()} {acct.currencyCode}</p>
                </div>
                <div>
                  <p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('issuing.holdAmount')}</p>
                  <p style={{ fontSize: 18, fontWeight: 700, color: '#f59e0b' }}>{acct.holdAmount.toLocaleString()} {acct.currencyCode}</p>
                </div>
                <div>
                  <p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('issuing.status')}</p>
                  <p style={{ fontSize: 18, fontWeight: 700 }}><StatusBadge status={acct.status} label={ACCOUNT_STATUS_LABELS[acct.status] ?? acct.status} /></p>
                </div>
              </div>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <input type="number" value={amount} onChange={e => setAmount(e.target.value)}
                  placeholder={t('issuing.amount')}
                  style={{ padding: '6px 10px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)', width: 120, fontSize: 13 }} />
                <MiniBtn label={t('issuing.debit')} color="#ef4444" onClick={() => handleAccountAction('debit', acct.id)} />
                <MiniBtn label={t('issuing.credit')} color="#22c55e" onClick={() => handleAccountAction('credit', acct.id)} />
                <MiniBtn label={t('issuing.placeHold')} color="#f59e0b" onClick={() => handleAccountAction('hold', acct.id)} />
                <MiniBtn label={t('issuing.releaseHold')} color="#3b82f6" onClick={() => handleAccountAction('release', acct.id)} />
              </div>
            </div>
          ))}
        </div>
      )}

      {tab === 'notifications' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          {!selectedCh ? (
            <p style={{ color: 'var(--text-secondary)' }}>{t('issuing.selectCardholder')}</p>
          ) : (
            <>
              <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('issuing.notifications')} - {selectedCh.firstName} {selectedCh.lastName}</h3>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.notificationType')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.notificationMessage')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.actionRequired')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.created')}</th>
                  </tr>
                </thead>
                <tbody>
                  {notifications.map(n => (
                    <tr key={n.id} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '10px 12px' }}>
                        <span style={{
                          background: n.type.includes('BLOCK') || n.type.includes('STOLEN') ? '#ef444433' :
                            n.type.includes('LOST') || n.type.includes('EXPIR') ? '#f59e0b33' : '#22c55e33',
                          color: n.type.includes('BLOCK') || n.type.includes('STOLEN') ? '#ef4444' :
                            n.type.includes('LOST') || n.type.includes('EXPIR') ? '#f59e0b' : '#22c55e',
                          padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                        }}>{getNotificationLabel(n.type)}</span>
                      </td>
                      <td style={{ padding: '10px 12px' }}>{n.message}</td>
                      <td style={{ padding: '10px 12px' }}>{n.actionRequired ? '⚠️' : '-'}</td>
                      <td style={{ padding: '10px 12px', fontSize: 12 }}>{new Date(n.createdAt).toLocaleString()}</td>
                    </tr>
                  ))}
                  {notifications.length === 0 && (
                    <tr><td colSpan={4} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('issuing.noNotifications')}</td></tr>
                  )}
                </tbody>
              </table>
            </>
          )}
        </div>
      )}

      {showChModal && (
        <div style={styles.overlay} onClick={() => setShowChModal(false)}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 20 }}>{t('issuing.createCardholder')}</h3>
            <div style={{ display: 'grid', gap: 14 }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('issuing.firstName')}><input style={styles.input} value={chForm.firstName} onChange={e => setChForm({ ...chForm, firstName: e.target.value })} /></Field>
                <Field label={t('issuing.lastName')}><input style={styles.input} value={chForm.lastName} onChange={e => setChForm({ ...chForm, lastName: e.target.value })} /></Field>
              </div>
              <Field label={t('issuing.email')}><input style={styles.input} type="email" value={chForm.email} onChange={e => setChForm({ ...chForm, email: e.target.value })} /></Field>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('issuing.phone')}><input style={styles.input} value={chForm.phone} onChange={e => setChForm({ ...chForm, phone: e.target.value })} /></Field>
                <Field label={t('issuing.documentNumber')}><input style={styles.input} value={chForm.documentNumber} onChange={e => setChForm({ ...chForm, documentNumber: e.target.value })} /></Field>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('issuing.dateOfBirth')}><input style={styles.input} type="date" value={chForm.dateOfBirth} onChange={e => setChForm({ ...chForm, dateOfBirth: e.target.value })} /></Field>
                <Field label={t('issuing.nationality')}><input style={styles.input} value={chForm.nationality} onChange={e => setChForm({ ...chForm, nationality: e.target.value })} /></Field>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 24 }}>
              <button onClick={() => setShowChModal(false)} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>{t('fraud.cancel')}</button>
              <button onClick={createCardholder} disabled={saving || !chForm.firstName || !chForm.lastName || !chForm.email} style={{
                padding: '10px 20px', borderRadius: 8, border: 'none',
                background: saving || !chForm.firstName || !chForm.lastName || !chForm.email ? '#64748b' : '#3b82f6',
                color: 'white', cursor: saving ? 'not-allowed' : 'pointer', fontSize: 13, fontWeight: 600,
              }}>{saving ? t('common.loading') : t('issuing.create')}</button>
            </div>
          </div>
        </div>
      )}

      {showCardModal && (
        <div style={styles.overlay} onClick={() => setShowCardModal(false)}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 20 }}>{t('issuing.createCard')}</h3>
            <div style={{ display: 'grid', gap: 14 }}>
              <Field label={t('issuing.cardholder')}>
                <select style={styles.select} value={cardForm.cardholderId} onChange={e => setCardForm({ ...cardForm, cardholderId: e.target.value })}>
                  <option value="">-- Select --</option>
                  {cardholders.map(ch => <option key={ch.id} value={ch.id}>{ch.firstName} {ch.lastName}</option>)}
                </select>
              </Field>
              <Field label={t('issuing.cardProduct')}>
                <select style={styles.select} value={cardForm.cardProduct} onChange={e => setCardForm({ ...cardForm, cardProduct: e.target.value })}>
                  {['CREDIT', 'DEBIT', 'PREPAID', 'CHARGE'].map(p => <option key={p} value={p}>{CARD_PRODUCT_LABELS[p] ?? p}</option>)}
                </select>
              </Field>
              <Field label={t('issuing.cardNumber')}><input style={styles.input} value={cardForm.cardNumber} onChange={e => setCardForm({ ...cardForm, cardNumber: e.target.value })} /></Field>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
                <Field label={t('issuing.expiryDate')}><input style={styles.input} value={cardForm.expiryDate} onChange={e => setCardForm({ ...cardForm, expiryDate: e.target.value })} placeholder="MM/YY" /></Field>
                <Field label={t('issuing.cvv')}><input style={styles.input} value={cardForm.cvv} onChange={e => setCardForm({ ...cardForm, cvv: e.target.value })} /></Field>
                <Field label={t('issuing.status')}>
                  <select style={styles.select} value={cardForm.status} onChange={e => setCardForm({ ...cardForm, status: e.target.value })}>
                    {['PENDING_ACTIVATION', 'ACTIVE', 'BLOCKED'].map(s => <option key={s} value={s}>{CARD_STATUS_LABELS[s] ?? s}</option>)}
                  </select>
                </Field>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 24 }}>
              <button onClick={() => setShowCardModal(false)} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>{t('fraud.cancel')}</button>
              <button onClick={createCard} disabled={saving || !cardForm.cardholderId || !cardForm.cardNumber} style={{
                padding: '10px 20px', borderRadius: 8, border: 'none',
                background: saving || !cardForm.cardholderId || !cardForm.cardNumber ? '#64748b' : '#3b82f6',
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
