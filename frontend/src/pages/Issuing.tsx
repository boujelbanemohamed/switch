import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { Cardholder, Card, CardAccount as CardAccountType, Notification as NotificationType } from '../types';
import { SectionHeader } from '../components/SectionHeader';

type Tab = 'cardholders' | 'cards' | 'accounts' | 'notifications';

export function Issuing() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('cardholders');
  const [cardholders, setCardholders] = useState<Cardholder[]>([]);
  const [selectedCh, setSelectedCh] = useState<Cardholder | null>(null);
  const [cards, setCards] = useState<Card[]>([]);
  const [accounts, setAccounts] = useState<CardAccountType[]>([]);
  const [account, setAccount] = useState<CardAccountType | null>(null);
  const [notifications, setNotifications] = useState<NotificationType[]>([]);
  const [loading, setLoading] = useState(true);
  const [amount, setAmount] = useState('');

  useEffect(() => {
    api.issuing.cardholders.list()
      .then(setCardholders)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (selectedCh) {
      api.issuing.cards.listByCardholder(selectedCh.id)
        .then(setCards).catch(console.error);
      api.issuing.accounts.listByCardholder(selectedCh.id)
        .then(setAccounts).catch(console.error);
      api.issuing.notifications.listByCardholder(selectedCh.id)
        .then(setNotifications).catch(console.error);
    }
  }, [selectedCh]);

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

  if (loading) return <div style={{ opacity: 0.5 }}>{t('common.loading')}</div>;

  const tabs: { key: Tab; label: string }[] = [
    { key: 'cardholders', label: t('issuing.tabCardholders') },
    { key: 'cards', label: t('issuing.tabCards') },
    { key: 'accounts', label: t('issuing.tabAccounts') },
    { key: 'notifications', label: t('issuing.tabNotifications') },
  ];

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('issuing.title')}</h2>
      <SectionHeader sectionKey="issuing" />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        <StatCard title={t('issuing.totalCardholders')} value={cardholders.length.toLocaleString()} />
        <StatCard title={t('issuing.activeCards')} value={cardholders.filter(c => c.status === 'ACTIVE').length.toLocaleString()} />
        <StatCard title={t('issuing.kycLevel3')} value={cardholders.filter(c => c.kycLevel >= 3).length.toLocaleString()} />
        <StatCard title={t('issuing.balance')} value={account ? account.availableBalance.toLocaleString() : '-'} />
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        {tabs.map(t => (
          <button key={t.key} onClick={() => { setTab(t.key); if (t.key === 'cardholders') setSelectedCh(null); }}
            style={{
              padding: '8px 16px', border: '1px solid var(--border)', borderRadius: 8,
              background: tab === t.key ? 'var(--accent)' : 'var(--surface)',
              color: tab === t.key ? '#fff' : 'var(--text)', cursor: 'pointer', fontWeight: 600, fontSize: 13,
            }}>
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'cardholders' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('issuing.cardholders')}</h3>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.name')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.email')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.status')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.kycLevel')}</th>
                <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.created')}</th>
              </tr>
            </thead>
            <tbody>
              {cardholders.map(ch => (
                <tr key={ch.id} onClick={() => setSelectedCh(ch)}
                  style={{ borderBottom: '1px solid var(--border)', cursor: 'pointer',
                    background: selectedCh?.id === ch.id ? 'var(--accent)' : undefined,
                    color: selectedCh?.id === ch.id ? '#fff' : undefined }}>
                  <td style={{ padding: '10px 12px' }}>{ch.firstName} {ch.lastName}</td>
                  <td style={{ padding: '10px 12px' }}>{ch.email}</td>
                  <td style={{ padding: '10px 12px' }}>
                    <StatusBadge status={ch.status} />
                  </td>
                  <td style={{ padding: '10px 12px', fontFamily: 'monospace' }}>{ch.kycLevel}</td>
                  <td style={{ padding: '10px 12px', fontSize: 12 }}>{new Date(ch.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
              {cardholders.length === 0 && (
                <tr><td colSpan={5} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('issuing.noCardholders')}</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'cards' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          {!selectedCh ? (
            <p style={{ color: 'var(--text-secondary)' }}>{t('issuing.selectCardholder')}</p>
          ) : (
            <>
              <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>
                {t('issuing.tabCards')} - {selectedCh.firstName} {selectedCh.lastName}
              </h3>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.panSuffix')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.cardBrand')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.cardType')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.status')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.expires')}</th>
                    <th style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('issuing.actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {cards.map(card => (
                    <tr key={card.id} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '10px 12px', fontFamily: 'monospace' }}>****{card.panSuffix}</td>
                      <td style={{ padding: '10px 12px' }}>{card.cardBrand}</td>
                      <td style={{ padding: '10px 12px' }}>{card.cardType}</td>
                      <td style={{ padding: '10px 12px' }}><StatusBadge status={card.status} /></td>
                      <td style={{ padding: '10px 12px', fontSize: 12 }}>{card.expiresAt ? new Date(card.expiresAt).toLocaleDateString() : '-'}</td>
                      <td style={{ padding: '10px 12px', display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                        <MiniBtn label="Activate" color="#22c55e" onClick={() => handleCardAction('activate', card.id)} />
                        <MiniBtn label="Block" color="#ef4444" onClick={() => handleCardAction('block', card.id, 'MANUAL_BLOCK')} />
                        <MiniBtn label="Unblock" color="#22c55e" onClick={() => handleCardAction('unblock', card.id)} />
                        <MiniBtn label="Lost" color="#f59e0b" onClick={() => handleCardAction('lost', card.id)} />
                        <MiniBtn label="Stolen" color="#ef4444" onClick={() => handleCardAction('stolen', card.id)} />
                        <MiniBtn label="Renew" color="#3b82f6" onClick={() => handleCardAction('renew', card.id)} />
                      </td>
                    </tr>
                  ))}
                  {cards.length === 0 && (
                    <tr><td colSpan={6} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('issuing.noCards')}</td></tr>
                  )}
                </tbody>
              </table>
            </>
          )}
        </div>
      )}

      {tab === 'accounts' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          {!selectedCh ? (
            <p style={{ color: 'var(--text-secondary)' }}>{t('issuing.selectCardholder')}</p>
          ) : (
            <>
              <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>
                {t('issuing.tabAccounts')} - {selectedCh.firstName} {selectedCh.lastName}
              </h3>
              {accounts.length === 0 ? (
                <p style={{ color: 'var(--text-secondary)' }}>{t('issuing.noAccount')}</p>
              ) : (
                accounts.map(acct => (
                  <div key={acct.id} style={{
                    background: 'var(--bg)', borderRadius: 8, padding: 16, marginBottom: 12,
                    border: '1px solid var(--border)',
                  }}>
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
                        <p style={{ fontSize: 18, fontWeight: 700 }}><StatusBadge status={acct.status} /></p>
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      <input type="number" value={amount} onChange={e => setAmount(e.target.value)}
                        placeholder={t('issuing.amount')}
                        style={{
                          padding: '6px 10px', borderRadius: 6, border: '1px solid var(--border)',
                          background: 'var(--surface)', color: 'var(--text)', width: 120, fontSize: 13,
                        }} />
                      <MiniBtn label={t('issuing.debit')} color="#ef4444" onClick={() => handleAccountAction('debit', acct.id)} />
                      <MiniBtn label={t('issuing.credit')} color="#22c55e" onClick={() => handleAccountAction('credit', acct.id)} />
                      <MiniBtn label={t('issuing.placeHold')} color="#f59e0b" onClick={() => handleAccountAction('hold', acct.id)} />
                      <MiniBtn label={t('issuing.releaseHold')} color="#3b82f6" onClick={() => handleAccountAction('release', acct.id)} />
                    </div>
                  </div>
                ))
              )}
            </>
          )}
        </div>
      )}

      {tab === 'notifications' && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          {!selectedCh ? (
            <p style={{ color: 'var(--text-secondary)' }}>{t('issuing.selectCardholder')}</p>
          ) : (
            <>
              <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>
                {t('issuing.notifications')} - {selectedCh.firstName} {selectedCh.lastName}
              </h3>
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
                        }}>{n.type}</span>
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
    </div>
  );
}

function StatCard({ title, value }: { title: string; value: string }) {
  return (
    <div style={{
      background: 'var(--surface)', borderRadius: 12, padding: '16px 20px',
      border: '1px solid var(--border)',
    }}>
      <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 8 }}>{title}</p>
      <p style={{ fontSize: 28, fontWeight: 700 }}>{value}</p>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    ACTIVE: '#22c55e', BLOCKED: '#ef4444', PENDING: '#f59e0b',
    PENDING_ACTIVATION: '#f59e0b', LOST: '#ef4444', STOLEN: '#dc2626',
    RENEWED: '#3b82f6', SUSPENDED: '#f59e0b', TERMINATED: '#6b7280',
    INACTIVE: '#6b7280',
  };
  const color = colors[status] || '#6b7280';
  return (
    <span style={{
      background: color + '33', color, padding: '2px 8px', borderRadius: 4,
      fontSize: 11, fontWeight: 600,
    }}>{status}</span>
  );
}

function MiniBtn({ label, color, onClick }: { label: string; color: string; onClick: () => void }) {
  return (
    <button onClick={onClick} style={{
      padding: '4px 8px', border: `1px solid ${color}33`, borderRadius: 4,
      background: color + '15', color, cursor: 'pointer', fontSize: 11, fontWeight: 600,
      whiteSpace: 'nowrap',
    }}>{label}</button>
  );
}
