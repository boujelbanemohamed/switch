import { useEffect, useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type {
  LoyaltyProgram, LoyaltyTier, LoyaltyMembership,
  LoyaltyTransaction, LoyaltyReward, LoyaltyRedemption,
} from '../types';

const styles: Record<string, React.CSSProperties> = {
  page: { padding: 24, maxWidth: 1200, margin: '0 auto' },
  header: { fontSize: 24, fontWeight: 700, marginBottom: 24, color: 'var(--text)' },
  card: {
    background: 'var(--surface)',
    border: '1px solid var(--border)',
    borderRadius: 12,
    padding: 20,
    marginBottom: 16,
  },
  cardTitle: { fontSize: 16, fontWeight: 600, marginBottom: 12, color: 'var(--text)' },
  grid2: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 },
  grid3: { display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 },
  label: { fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 },
  value: { fontSize: 14, fontWeight: 600, color: 'var(--text)' },
  tabs: { display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' as const },
  tab: {
    padding: '8px 16px', borderRadius: 8, border: '1px solid var(--border)',
    background: 'transparent', color: 'var(--text-secondary)', cursor: 'pointer', fontSize: 13, fontWeight: 500,
  },
  tabActive: {
    padding: '8px 16px', borderRadius: 8, border: '1px solid #2563eb',
    background: 'rgba(37,99,235,0.1)', color: '#60a5fa', cursor: 'pointer', fontSize: 13, fontWeight: 600,
  },
  input: {
    width: '100%', padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)',
    background: 'var(--bg)', color: 'var(--text)', fontSize: 13, marginBottom: 8,
    boxSizing: 'border-box' as const,
  },
  select: {
    width: '100%', padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)',
    background: 'var(--bg)', color: 'var(--text)', fontSize: 13, marginBottom: 8,
  },
  btn: {
    padding: '8px 16px', borderRadius: 8, border: 'none', fontSize: 13, fontWeight: 600,
    cursor: 'pointer', background: '#2563eb', color: '#fff', marginRight: 8, marginBottom: 8,
  },
  btnSm: {
    padding: '5px 10px', borderRadius: 6, border: 'none', fontSize: 11, fontWeight: 600,
    cursor: 'pointer',
  },
  table: { width: '100%', borderCollapse: 'collapse' as const, fontSize: 13 },
  th: { textAlign: 'left' as const, padding: '10px 8px', borderBottom: '1px solid var(--border)', color: 'var(--text-secondary)', fontSize: 11, fontWeight: 600, textTransform: 'uppercase' as const },
  td: { padding: '10px 8px', borderBottom: '1px solid var(--border)', color: 'var(--text)' },
  badge: {
    display: 'inline-block', padding: '2px 8px', borderRadius: 10, fontSize: 11, fontWeight: 600,
  },
  modalOverlay: {
    position: 'fixed' as const, inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex',
    alignItems: 'center', justifyContent: 'center', zIndex: 1000,
  },
  modal: {
    background: 'var(--surface)', borderRadius: 12, padding: 24, width: 480, maxWidth: '90vw',
    maxHeight: '80vh', overflowY: 'auto' as const,
  },
  textarea: {
    width: '100%', padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)',
    background: 'var(--bg)', color: 'var(--text)', fontSize: 13, marginBottom: 8, resize: 'vertical' as const,
    minHeight: 60, boxSizing: 'border-box' as const,
  },
};

export function Loyalty() {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState<'programs' | 'memberships' | 'rewards'>('programs');

  const [programs, setPrograms] = useState<LoyaltyProgram[]>([]);
  const [memberships, setMemberships] = useState<LoyaltyMembership[]>([]);
  const [selectedProgram, setSelectedProgram] = useState<string>('');
  const [tiers, setTiers] = useState<LoyaltyTier[]>([]);
  const [rewards, setRewards] = useState<LoyaltyReward[]>([]);
  const [transactions, setTransactions] = useState<LoyaltyTransaction[]>([]);
  const [redemptions, setRedemptions] = useState<LoyaltyRedemption[]>([]);

  const [showCreateProgram, setShowCreateProgram] = useState(false);
  const [showCreateTier, setShowCreateTier] = useState(false);
  const [showCreateReward, setShowCreateReward] = useState(false);
  const [showEarn, setShowEarn] = useState(false);
  const [showBurn, setShowBurn] = useState(false);
  const [selectedMembership, setSelectedMembership] = useState<string>('');

  const loadPrograms = useCallback(async () => {
    try { setPrograms(await api.loyalty.programs.list()); } catch { /* ignore */ }
  }, []);

  const loadTiers = useCallback(async (pid: string) => {
    try { setTiers(await api.loyalty.programs.tiers.list(pid)); } catch { setTiers([]); }
  }, []);

  const loadRewards = useCallback(async (pid: string) => {
    try { setRewards(await api.loyalty.programs.rewards.list(pid)); } catch { setRewards([]); }
  }, []);

  const loadMemberships = useCallback(async () => {
    try { setMemberships(await api.loyalty.memberships.listByCardholder('all')); } catch { setMemberships([]); }
  }, []);

  const loadTransactions = useCallback(async (mid: string) => {
    try { setTransactions(await api.loyalty.memberships.transactions(mid)); } catch { setTransactions([]); }
  }, []);

  const loadRedemptions = useCallback(async (mid: string) => {
    try { setRedemptions(await api.loyalty.memberships.redemptions(mid)); } catch { setRedemptions([]); }
  }, []);

  useEffect(() => { loadPrograms(); }, [loadPrograms]);

  useEffect(() => {
    if (selectedProgram) {
      loadTiers(selectedProgram);
      loadRewards(selectedProgram);
    }
  }, [selectedProgram, loadTiers, loadRewards]);

  useEffect(() => {
    if (selectedMembership) {
      loadTransactions(selectedMembership);
      loadRedemptions(selectedMembership);
    }
  }, [selectedMembership, loadTransactions, loadRedemptions]);

  async function handleCreateProgram(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    await api.loyalty.programs.create({
      name: fd.get('name') as string,
      description: fd.get('description') as string,
      earningRate: Number(fd.get('earningRate')),
      currency: (fd.get('currency') as string) || 'TND',
    });
    setShowCreateProgram(false);
    loadPrograms();
  }

  async function handleCreateTier(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    await api.loyalty.programs.tiers.create(selectedProgram, {
      name: fd.get('name') as string,
      minLifetimePoints: Number(fd.get('minLifetimePoints')),
      earningMultiplier: Number(fd.get('earningMultiplier')),
      benefits: fd.get('benefits') as string,
    });
    setShowCreateTier(false);
    loadTiers(selectedProgram);
  }

  async function handleCreateReward(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    await api.loyalty.programs.rewards.create(selectedProgram, {
      name: fd.get('name') as string,
      description: fd.get('description') as string,
      pointsCost: Number(fd.get('pointsCost')),
      stock: fd.get('stock') ? Number(fd.get('stock')) : undefined,
    });
    setShowCreateReward(false);
    loadRewards(selectedProgram);
  }

  async function handleEnroll() {
    const cardholderId = prompt('Cardholder ID:');
    if (!cardholderId || !selectedProgram) return;
    try {
      await api.loyalty.memberships.enroll(cardholderId, selectedProgram);
      loadMemberships();
    } catch (err: any) { alert(err.message); }
  }

  async function handleEarn(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    try {
      await api.loyalty.memberships.earn(selectedMembership, {
        amount: Number(fd.get('amount')),
        transactionRef: fd.get('transactionRef') as string,
        description: fd.get('description') as string,
      });
      setShowEarn(false);
      loadTransactions(selectedMembership);
    } catch (err: any) { alert(err.message); }
  }

  async function handleBurn(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    try {
      await api.loyalty.memberships.burn(selectedMembership, {
        points: Number(fd.get('points')),
        description: fd.get('description') as string,
      });
      setShowBurn(false);
      loadTransactions(selectedMembership);
    } catch (err: any) { alert(err.message); }
  }

  return (
    <div style={styles.page}>
      <h1 style={styles.header}>{t('loyalty.title')}</h1>

      <div style={styles.tabs}>
        {(['programs', 'memberships', 'rewards'] as const).map(tab => (
          <button key={tab} style={activeTab === tab ? styles.tabActive : styles.tab}
            onClick={() => setActiveTab(tab)}>{t(`loyalty.${tab}`)}</button>
        ))}
      </div>

      {activeTab === 'programs' && (
        <div>
          <button style={styles.btn} onClick={() => setShowCreateProgram(true)}>{t('loyalty.createProgram')}</button>
          {programs.length === 0 && <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{t('loyalty.noPrograms')}</p>}
          {programs.map(p => (
            <div key={p.id} style={styles.card}>
              <div style={styles.grid3}>
                <div><div style={styles.label}>{t('loyalty.name')}</div><div style={styles.value}>{p.name}</div></div>
                <div><div style={styles.label}>{t('loyalty.earningRate')}</div><div style={styles.value}>{p.earningRate}</div></div>
                <div><div style={styles.label}>{t('loyalty.currency')}</div><div style={styles.value}>{p.currency}</div></div>
              </div>
              <div style={{ marginTop: 8, display: 'flex', gap: 8 }}>
                {p.status === 'ACTIVE'
                  ? <button style={{ ...styles.btnSm, background: '#ef4444', color: '#fff' }} onClick={async () => { await api.loyalty.programs.toggle(p.id); loadPrograms(); }}>{t('common.deactivate') || 'Deactivate'}</button>
                  : <button style={{ ...styles.btnSm, background: '#22c55e', color: '#fff' }} onClick={async () => { await api.loyalty.programs.toggle(p.id); loadPrograms(); }}>{t('common.activate') || 'Activate'}</button>
                }
                <button style={{ ...styles.btnSm, background: '#3b82f6', color: '#fff' }}
                  onClick={() => { setSelectedProgram(p.id); }}>{t('loyalty.tiers')}</button>
              </div>

              {selectedProgram === p.id && (
                <div style={{ marginTop: 12 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                    <span style={{ fontSize: 14, fontWeight: 600 }}>{t('loyalty.tiers')}</span>
                    <button style={{ ...styles.btnSm, background: '#2563eb', color: '#fff' }}
                      onClick={() => setShowCreateTier(true)}>{t('loyalty.createTier')}</button>
                  </div>
                  {tiers.length === 0 && <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>No tiers</p>}
                  {tiers.map(tier => (
                    <div key={tier.id} style={{ display: 'flex', gap: 12, padding: '6px 0', borderBottom: '1px solid var(--border)', fontSize: 13 }}>
                      <span style={{ fontWeight: 600, width: 80 }}>{tier.name}</span>
                      <span style={{ color: 'var(--text-secondary)' }}>{t('loyalty.minPoints')}: {tier.minLifetimePoints}</span>
                      <span style={{ color: 'var(--text-secondary)' }}>{t('loyalty.multiplier')}: x{tier.earningMultiplier}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {activeTab === 'memberships' && (
        <div>
          <button style={styles.btn} onClick={handleEnroll}>{t('loyalty.enroll')}</button>
          {memberships.length === 0 && <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{t('loyalty.noMemberships')}</p>}
          <div style={styles.grid2}>
            {memberships.map(m => (
              <div key={m.id} style={styles.card}>
                <div style={styles.grid3}>
                  <div><div style={styles.label}>ID</div><div style={{ ...styles.value, fontSize: 12 }}>{m.id.slice(0, 8)}…</div></div>
                  <div><div style={styles.label}>{t('loyalty.pointsBalance')}</div><div style={styles.value}>{m.pointsBalance}</div></div>
                  <div><div style={styles.label}>{t('loyalty.lifetimePoints')}</div><div style={styles.value}>{m.lifetimePoints}</div></div>
                </div>
                <div style={{ marginTop: 8 }}>
                  <span style={{
                    ...styles.badge,
                    background: m.status === 'ACTIVE' ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)',
                    color: m.status === 'ACTIVE' ? '#22c55e' : '#ef4444',
                  }}>{t(`loyalty.${m.status.toLowerCase()}`)}</span>
                  {m.status === 'ACTIVE' && (
                    <button style={{ ...styles.btnSm, background: '#f59e0b', color: '#fff', marginLeft: 8 }}
                      onClick={async () => { await api.loyalty.memberships.suspend(m.id); loadMemberships(); }}>{t('loyalty.suspend')}</button>
                  )}
                  <button style={{ ...styles.btnSm, background: '#3b82f6', color: '#fff', marginLeft: 8 }}
                    onClick={() => { setSelectedMembership(m.id); }}>{t('loyalty.transactionRef') || 'View'}</button>
                </div>

                {selectedMembership === m.id && (
                  <div style={{ marginTop: 12 }}>
                    <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                      <button style={{ ...styles.btnSm, background: '#22c55e', color: '#fff' }}
                        onClick={() => setShowEarn(true)}>{t('loyalty.earn')}</button>
                      <button style={{ ...styles.btnSm, background: '#f59e0b', color: '#fff' }}
                        onClick={() => setShowBurn(true)}>{t('loyalty.burn')}</button>
                    </div>

                    <div style={{ fontSize: 13, fontWeight: 600, margin: '8px 0' }}>{t('loyalty.transactionRef') || 'Transactions'}</div>
                    {transactions.length === 0 && <p style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{t('loyalty.noTransactions')}</p>}
                    <table style={styles.table}>
                      <thead>
                        <tr>
                          <th style={styles.th}>{t('loyalty.transactionRef') || 'Type'}</th>
                          <th style={styles.th}>{t('loyalty.points')}</th>
                          <th style={styles.th}>{t('common.description') || 'Description'}</th>
                          <th style={styles.th}>{t('common.date') || 'Date'}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {transactions.slice(0, 10).map(tx => (
                          <tr key={tx.id}>
                            <td style={styles.td}>
                              <span style={{
                                ...styles.badge,
                                background: tx.type === 'EARN' ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)',
                                color: tx.type === 'EARN' ? '#22c55e' : '#ef4444',
                              }}>{tx.type}</span>
                            </td>
                            <td style={styles.td}>{tx.type === 'EARN' ? '+' : ''}{tx.points}</td>
                            <td style={styles.td}>{tx.description || '-'}</td>
                            <td style={styles.td}>{new Date(tx.createdAt).toLocaleDateString()}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>

                    <div style={{ fontSize: 13, fontWeight: 600, margin: '8px 0' }}>{t('loyalty.redeem') || 'Redemptions'}</div>
                    {redemptions.length === 0 && <p style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{t('loyalty.noRedemptions')}</p>}
                    <table style={styles.table}>
                      <thead>
                        <tr>
                          <th style={styles.th}>{t('loyalty.points')}</th>
                          <th style={styles.th}>{t('credit.interest') || 'Credit'}</th>
                          <th style={styles.th}>{t('loyalty.status')}</th>
                          <th style={styles.th}>{t('common.date') || 'Date'}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {redemptions.slice(0, 10).map(r => (
                          <tr key={r.id}>
                            <td style={styles.td}>{r.pointsSpent}</td>
                            <td style={styles.td}>{r.balanceCreditAmount ? `${r.balanceCreditAmount} TND` : '-'}</td>
                            <td style={styles.td}>{r.status}</td>
                            <td style={styles.td}>{new Date(r.createdAt).toLocaleDateString()}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'rewards' && (
        <div>
          <button style={styles.btn} onClick={() => setShowCreateReward(true)}>{t('loyalty.createReward')}</button>
          {rewards.length === 0 && <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{t('loyalty.noRewards')}</p>}
          <div style={styles.grid2}>
            {rewards.map(r => (
              <div key={r.id} style={styles.card}>
                <div style={{ fontSize: 16, fontWeight: 600, marginBottom: 8 }}>{r.name}</div>
                {r.description && <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 8 }}>{r.description}</p>}
                <div style={styles.grid3}>
                  <div><div style={styles.label}>{t('loyalty.pointsCost')}</div><div style={styles.value}>{r.pointsCost}</div></div>
                  <div><div style={styles.label}>{t('loyalty.stock')}</div><div style={styles.value}>{r.stock ?? '∞'}</div></div>
                  <div><div style={styles.label}>{t('loyalty.status')}</div>
                    <span style={{
                      ...styles.badge,
                      background: r.status === 'ACTIVE' ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)',
                      color: r.status === 'ACTIVE' ? '#22c55e' : '#ef4444',
                    }}>{r.status}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {showCreateProgram && (
        <div style={styles.modalOverlay} onClick={() => setShowCreateProgram(false)}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 16 }}>{t('loyalty.createProgram')}</h2>
            <form onSubmit={handleCreateProgram}>
              <input name="name" placeholder={t('loyalty.name')} style={styles.input} required />
              <textarea name="description" placeholder={t('loyalty.description')} style={styles.textarea} />
              <input name="earningRate" type="number" step="0.0001" placeholder={t('loyalty.earningRate')} style={styles.input} required />
              <select name="currency" style={styles.select}><option value="TND">TND</option><option value="EUR">EUR</option><option value="USD">USD</option></select>
              <button type="submit" style={styles.btn}>{t('credit.create')}</button>
              <button type="button" style={{ ...styles.btn, background: '#6b7280' }} onClick={() => setShowCreateProgram(false)}>{t('common.cancel') || 'Cancel'}</button>
            </form>
          </div>
        </div>
      )}

      {showCreateTier && (
        <div style={styles.modalOverlay} onClick={() => setShowCreateTier(false)}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 16 }}>{t('loyalty.createTier')}</h2>
            <form onSubmit={handleCreateTier}>
              <input name="name" placeholder={t('loyalty.name')} style={styles.input} required />
              <input name="minLifetimePoints" type="number" step="1" placeholder={t('loyalty.minPoints')} style={styles.input} required />
              <input name="earningMultiplier" type="number" step="0.01" placeholder={t('loyalty.multiplier')} style={styles.input} required />
              <textarea name="benefits" placeholder={t('loyalty.benefits')} style={styles.textarea} />
              <button type="submit" style={styles.btn}>{t('credit.create')}</button>
              <button type="button" style={{ ...styles.btn, background: '#6b7280' }} onClick={() => setShowCreateTier(false)}>{t('common.cancel') || 'Cancel'}</button>
            </form>
          </div>
        </div>
      )}

      {showCreateReward && (
        <div style={styles.modalOverlay} onClick={() => setShowCreateReward(false)}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 16 }}>{t('loyalty.createReward')}</h2>
            <form onSubmit={handleCreateReward}>
              <input name="name" placeholder={t('loyalty.name')} style={styles.input} required />
              <textarea name="description" placeholder={t('loyalty.description')} style={styles.textarea} />
              <input name="pointsCost" type="number" step="1" placeholder={t('loyalty.pointsCost')} style={styles.input} required />
              <input name="stock" type="number" step="1" placeholder={t('loyalty.stock')} style={styles.input} />
              <button type="submit" style={styles.btn}>{t('credit.create')}</button>
              <button type="button" style={{ ...styles.btn, background: '#6b7280' }} onClick={() => setShowCreateReward(false)}>{t('common.cancel') || 'Cancel'}</button>
            </form>
          </div>
        </div>
      )}

      {showEarn && (
        <div style={styles.modalOverlay} onClick={() => setShowEarn(false)}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 16 }}>{t('loyalty.earn')}</h2>
            <form onSubmit={handleEarn}>
              <input name="amount" type="number" step="0.001" placeholder={t('loyalty.amount')} style={styles.input} required />
              <input name="transactionRef" placeholder={t('loyalty.transactionRef')} style={styles.input} />
              <textarea name="description" placeholder={t('loyalty.description')} style={styles.textarea} />
              <button type="submit" style={styles.btn}>{t('loyalty.earn')}</button>
              <button type="button" style={{ ...styles.btn, background: '#6b7280' }} onClick={() => setShowEarn(false)}>{t('common.cancel') || 'Cancel'}</button>
            </form>
          </div>
        </div>
      )}

      {showBurn && (
        <div style={styles.modalOverlay} onClick={() => setShowBurn(false)}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 16 }}>{t('loyalty.burn')}</h2>
            <form onSubmit={handleBurn}>
              <input name="points" type="number" step="1" placeholder={t('loyalty.points')} style={styles.input} required />
              <textarea name="description" placeholder={t('loyalty.description')} style={styles.textarea} />
              <button type="submit" style={styles.btn}>{t('loyalty.burn')}</button>
              <button type="button" style={{ ...styles.btn, background: '#6b7280' }} onClick={() => setShowBurn(false)}>{t('common.cancel') || 'Cancel'}</button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
