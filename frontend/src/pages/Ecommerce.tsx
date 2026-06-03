import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { AcsAuthentication, AcsChallenge, AcsEnrollment, EpgTransaction, EpgMerchantConfig, ThreeDsSession, ThreeDsSessionCancelResponse } from '../types';
import { SectionHeader } from '../components/SectionHeader';

type Tab = 'acs' | 'epg' | 'threeDs';

export function Ecommerce() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('acs');
  const [authResult, setAuthResult] = useState<AcsAuthentication | null>(null);
  const [epgResult, setEpgResult] = useState<EpgTransaction | null>(null);
  const [threeDsResult, setThreeDsResult] = useState<ThreeDsSession | null>(null);
  const [challengeResult, setChallengeResult] = useState<AcsChallenge | null>(null);
  const [loading, setLoading] = useState(false);

  const tabs: { key: Tab; label: string }[] = [
    { key: 'acs', label: t('ecommerce.tabAcs') },
    { key: 'epg', label: t('ecommerce.tabEpg') },
    { key: 'threeDs', label: t('ecommerce.tab3dss') },
  ];

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('ecommerce.title')}</h2>

      <SectionHeader sectionKey="ecommerce" />

      <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
        {tabs.map(tabItem => (
          <button
            key={tabItem.key}
            onClick={() => setTab(tabItem.key)}
            style={{
              padding: '8px 20px',
              borderRadius: 8,
              border: 'none',
              background: tab === tabItem.key ? '#3b82f6' : 'var(--surface)',
              color: tab === tabItem.key ? '#fff' : 'var(--text-secondary)',
              fontWeight: 600,
              fontSize: 13,
              cursor: 'pointer',
            }}
          >
            {tabItem.label}
          </button>
        ))}
      </div>

      {tab === 'acs' && <AcsPanel loading={loading} setLoading={setLoading} onResult={setAuthResult} />}
      {tab === 'epg' && <EpgPanel loading={loading} setLoading={setLoading} onResult={setEpgResult} />}
      {tab === 'threeDs' && <ThreeDsPanel loading={loading} setLoading={setLoading} onResult={setThreeDsResult} />}
    </div>
  );
}

function AcsPanel({ loading, setLoading, onResult }: {
  loading: boolean;
  setLoading: (v: boolean) => void;
  onResult: (r: AcsAuthentication | null) => void;
}) {
  const { t } = useTranslation();
  const [txnId, setTxnId] = useState('TXN-' + Date.now());
  const [cardId, setCardId] = useState('');
  const [amount, setAmount] = useState('150.00');
  const [currency, setCurrency] = useState('TND');
  const [auth, setAuth] = useState<AcsAuthentication | null>(null);
  const [challenge, setChallenge] = useState<AcsChallenge | null>(null);

  const createAuth = async () => {
    setLoading(true);
    try {
      const result = await api.acs.authentications.create({
        transactionId: txnId,
        cardId: cardId || undefined,
        amount,
        currencyCode: currency,
      });
      setAuth(result);
      onResult(result);
    } catch (e) { console.error(e); }
    setLoading(false);
  };

  const requestChallenge = async () => {
    if (!auth) return;
    setLoading(true);
    try {
      const result = await api.acs.authentications.requestChallenge(auth.id);
      setAuth(result);
    } catch (e) { console.error(e); }
    setLoading(false);
  };

  return (
    <>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('ecommerce.createAuthentication')}</h3>
          <Field label={t('ecommerce.transactionId')}>
            <input value={txnId} onChange={e => setTxnId(e.target.value)} />
          </Field>
          <Field label={t('ecommerce.cardId')}>
            <input value={cardId} onChange={e => setCardId(e.target.value)} placeholder={t('ecommerce.optional')} />
          </Field>
          <Field label={t('ecommerce.amount')}>
            <input value={amount} onChange={e => setAmount(e.target.value)} />
          </Field>
          <Field label={t('ecommerce.currency')}>
            <select value={currency} onChange={e => setCurrency(e.target.value)}>
              <option>TND</option>
              <option>USD</option>
              <option>EUR</option>
            </select>
          </Field>
          <button onClick={createAuth} disabled={loading} style={btnStyle}>
            {loading ? t('ecommerce.creating') : t('ecommerce.createAuthentication')}
          </button>
        </div>

        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('ecommerce.acsResult')}</h3>
          {auth ? (
            <div>
              <DetailRow label={t('ecommerce.id')} value={auth.id} />
              <DetailRow label={t('ecommerce.status')} value={auth.status} />
              <DetailRow label={t('ecommerce.amount')} value={`${auth.amount} ${auth.currencyCode}`} />
              <DetailRow label={t('ecommerce.threeDsVersion')} value={auth.threeDsVersion} />
              <DetailRow label={t('ecommerce.authValue')} value={auth.authenticationValue || t('ecommerce.na')} />
              <DetailRow label={t('ecommerce.eci')} value={auth.eci || t('ecommerce.na')} />
              {auth.status === 'CREATED' && (
                <button onClick={requestChallenge} disabled={loading} style={btnStyle}>
                  {t('ecommerce.requestChallenge')}
                </button>
              )}
            </div>
          ) : (
            <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{t('common.noData')}</p>
          )}
        </div>
      </div>

      <div style={{ borderTop: '1px solid var(--border)', margin: '24px 0' }} />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <AcsEnrollmentsSection />
        <AcsAuthenticationsSection />
      </div>
    </>
  );
}

function EpgPanel({ loading, setLoading, onResult }: {
  loading: boolean;
  setLoading: (v: boolean) => void;
  onResult: (r: EpgTransaction | null) => void;
}) {
  const { t } = useTranslation();
  const [merchantId, setMerchantId] = useState('');
  const [merchantTxnId, setMerchantTxnId] = useState('TXN-' + Date.now());
  const [amount, setAmount] = useState('99.99');
  const [currency, setCurrency] = useState('TND');
  const [txn, setTxn] = useState<EpgTransaction | null>(null);

  const initiate = async () => {
    setLoading(true);
    try {
      const result = await api.epg.transactions.initiate(merchantId, merchantTxnId, parseFloat(amount), currency);
      setTxn(result);
      onResult(result);
    } catch (e) { console.error(e); }
    setLoading(false);
  };

  return (
    <>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('ecommerce.initiatePayment')}</h3>
          <Field label={t('ecommerce.merchantId')}>
            <input value={merchantId} onChange={e => setMerchantId(e.target.value)} />
          </Field>
          <Field label={t('ecommerce.merchantTxnId')}>
            <input value={merchantTxnId} onChange={e => setMerchantTxnId(e.target.value)} />
          </Field>
          <Field label={t('ecommerce.amount')}>
            <input value={amount} onChange={e => setAmount(e.target.value)} />
          </Field>
          <Field label={t('ecommerce.currency')}>
            <select value={currency} onChange={e => setCurrency(e.target.value)}>
              <option>TND</option>
              <option>USD</option>
              <option>EUR</option>
            </select>
          </Field>
          <button onClick={initiate} disabled={loading} style={btnStyle}>
            {loading ? t('ecommerce.initiating') : t('ecommerce.initiatePayment')}
          </button>
        </div>

        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('ecommerce.epgResult')}</h3>
          {txn ? (
            <div>
              <DetailRow label={t('ecommerce.id')} value={txn.id} />
              <DetailRow label={t('ecommerce.status')} value={txn.status} />
              <DetailRow label={t('ecommerce.amount')} value={`${txn.amount} ${txn.currencyCode}`} />
              <DetailRow label={t('ecommerce.threeDsRequired')} value={txn.threeDsRequired ? t('ecommerce.yes') : t('ecommerce.no')} />
              <DetailRow label={t('ecommerce.threeDsStatus')} value={txn.threeDsStatus || t('ecommerce.na')} />
            </div>
          ) : (
            <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{t('common.noData')}</p>
          )}
        </div>
      </div>

      <div style={{ borderTop: '1px solid var(--border)', margin: '24px 0' }} />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <EpgMerchantsSection />
        <EpgTransactionsSection />
      </div>
    </>
  );
}

function ThreeDsPanel({ loading, setLoading, onResult }: {
  loading: boolean;
  setLoading: (v: boolean) => void;
  onResult: (r: ThreeDsSession | null) => void;
}) {
  const { t } = useTranslation();
  const [txnId, setTxnId] = useState('3DS-' + Date.now());
  const [notificationUrl, setNotificationUrl] = useState('https://merchant.example.com/3ds-callback');
  const [session, setSession] = useState<ThreeDsSession | null>(null);

  const create = async () => {
    setLoading(true);
    try {
      const result = await api.threeDs.sessions.create({
        transactionId: txnId,
        notificationUrl,
      });
      setSession(result);
      onResult(result);
    } catch (e) { console.error(e); }
    setLoading(false);
  };

  return (
    <>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('ecommerce.createSession')}</h3>
          <Field label={t('ecommerce.transactionId')}>
            <input value={txnId} onChange={e => setTxnId(e.target.value)} />
          </Field>
          <Field label={t('ecommerce.notificationUrl')}>
            <input value={notificationUrl} onChange={e => setNotificationUrl(e.target.value)} />
          </Field>
          <button onClick={create} disabled={loading} style={btnStyle}>
            {loading ? t('ecommerce.creating') : t('ecommerce.createSession')}
          </button>
        </div>

        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('ecommerce.threeDsResult')}</h3>
          {session ? (
            <div>
              <DetailRow label={t('ecommerce.id')} value={session.id} />
              <DetailRow label={t('ecommerce.status')} value={session.status} />
              <DetailRow label={t('ecommerce.threeDsVersion')} value={session.threeDsVersion} />
              <DetailRow label={t('ecommerce.authValue')} value={session.authenticationValue || t('ecommerce.na')} />
              <DetailRow label={t('ecommerce.eci')} value={session.eci || t('ecommerce.na')} />
              <DetailRow label={t('ecommerce.acsUrl')} value={session.acsUrl || t('ecommerce.na')} />
            </div>
          ) : (
            <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{t('common.noData')}</p>
          )}
        </div>
      </div>

      <div style={{ borderTop: '1px solid var(--border)', margin: '24px 0' }} />

      <ThreeDsSessionsSection />
    </>
  );
}

function maskCard(pan?: string): string {
  if (!pan || pan.length < 10) return pan || '-';
  return pan.slice(0, 6) + 'xxxxxx' + pan.slice(-4);
}

function AcsEnrollmentsSection() {
  const { t } = useTranslation();
  const [list, setList] = useState<AcsEnrollment[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState('');
  const [unenrolling, setUnenrolling] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [newCardId, setNewCardId] = useState('');
  const [enrolling, setEnrolling] = useState(false);

  const fetch = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.acs.enrollments.list();
      setList(data);
      setLoaded(true);
    } catch (e) { setError(t('common.failedToLoad')); }
    setLoading(false);
  }, [t]);

  const handleEnroll = async () => {
    if (!newCardId.trim()) return;
    setEnrolling(true);
    try {
      await api.acs.enrollments.enroll({ cardId: newCardId });
      setNewCardId('');
      setShowForm(false);
      fetch();
    } catch (e) { console.error(e); }
    setEnrolling(false);
  };

  const handleUnenroll = async (id: string) => {
    setUnenrolling(id);
    try {
      await api.acs.enrollments.unenroll(id);
      fetch();
    } catch (e) { console.error(e); }
    setUnenrolling(null);
  };

  return (
    <SectionCard title={t('ecommerce.sectionAcsEnrollments')} onRefresh={fetch}>
      {loaded && (
        <div style={{ marginBottom: 8 }}>
          <button onClick={() => setShowForm(!showForm)} style={smallBtnStyle}>
            {t('ecommerce.enrollCard')}
          </button>
        </div>
      )}

      {showForm && loaded && (
        <div style={{ background: 'var(--surface)', borderRadius: 8, padding: 12, marginBottom: 12 }}>
          <Field label={t('ecommerce.cardId')}>
            <input value={newCardId} onChange={e => setNewCardId(e.target.value)} placeholder={t('ecommerce.optional')} />
          </Field>
          <button onClick={handleEnroll} disabled={enrolling} style={smallBtnStyle}>
            {enrolling ? t('ecommerce.creating') : t('ecommerce.enroll')}
          </button>
        </div>
      )}

      {!loaded && !loading && <EmptyState msg={t('common.noData')} />}
      {loading && <Skeleton />}
      {!loading && error && <ErrorMsg msg={error} />}
      {!loading && loaded && !error && list.length === 0 && <EmptyState msg={t('ecommerce.noEnrollments')} />}
      {!loading && loaded && !error && list.length > 0 && (
        <div>
          {list.map(item => (
            <Row key={item.id}>
              <div style={{ flex: 1 }}>
                <RowLabel>Card ID</RowLabel>
                <RowValue style={{ fontSize: 11 }}>{item.cardId}</RowValue>
              </div>
              <div style={{ width: 80 }}>
                <RowLabel>{t('ecommerce.status')}</RowLabel>
                <RowValue>{item.status}</RowValue>
              </div>
              <div style={{ width: 140 }}>
                <RowLabel>{t('ecommerce.enrolledAt')}</RowLabel>
                <RowValue>{new Date(item.enrolledAt || item.createdAt).toLocaleDateString()}</RowValue>
              </div>
              <div style={{ width: 70 }}>
                <RowLabel>{t('ecommerce.cardBrand')}</RowLabel>
                <RowValue>{item.cardBrand || '-'}</RowValue>
              </div>
              <div style={{ width: 100 }}>
                <button
                  onClick={() => handleUnenroll(item.id)}
                  disabled={unenrolling === item.id}
                  style={{ ...smallBtnStyle, background: '#ef4444' }}
                >
                  {unenrolling === item.id ? '...' : t('ecommerce.unenroll')}
                </button>
              </div>
            </Row>
          ))}
        </div>
      )}
    </SectionCard>
  );
}

function AcsAuthenticationsSection() {
  const { t } = useTranslation();
  const [list, setList] = useState<AcsAuthentication[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.acs.authentications.list();
      setList(data);
      setLoaded(true);
    } catch (e) { setError(t('common.failedToLoad')); }
    setLoading(false);
  }, [t]);

  return (
    <SectionCard title={t('ecommerce.sectionAcsAuthHistory')} onRefresh={fetch}>
      {!loaded && !loading && <EmptyState msg={t('common.noData')} />}
      {loading && <Skeleton />}
      {!loading && error && <ErrorMsg msg={error} />}
      {!loading && loaded && !error && list.length === 0 && <EmptyState msg={t('ecommerce.noAuthentications')} />}
      {!loading && loaded && !error && list.length > 0 && (
        <div>
          {list.map(item => (
            <div key={item.id}>
              <Row onClick={() => setExpandedId(expandedId === item.id ? null : item.id)} style={{ cursor: 'pointer' }}>
                <div style={{ flex: 1 }}>
                  <RowLabel>{t('ecommerce.authId')}</RowLabel>
                  <RowValue>{item.id.length > 12 ? item.id.slice(0, 12) + '...' : item.id}</RowValue>
                </div>
                <div style={{ width: 120 }}>
                  <RowLabel>{t('ecommerce.status')}</RowLabel>
                  <RowValue>{item.status}</RowValue>
                </div>
                <div style={{ width: 100 }}>
                  <RowLabel>{t('ecommerce.amount')}</RowLabel>
                  <RowValue>{item.amount} {item.currencyCode}</RowValue>
                </div>
                <div style={{ width: 140 }}>
                  <RowLabel>{t('ecommerce.createdAt') || 'Date'}</RowLabel>
                  <RowValue>{new Date(item.createdAt).toLocaleDateString()}</RowValue>
                </div>
              </Row>
              {expandedId === item.id && (
                <div style={{ background: 'var(--surface)', borderRadius: 8, padding: 12, margin: '4px 0 8px 0' }}>
                  <DetailRow label={t('ecommerce.id')} value={item.id} />
                  <DetailRow label={t('ecommerce.transactionId')} value={item.transactionId} />
                  <DetailRow label={t('ecommerce.cardId')} value={item.cardId || t('ecommerce.na')} />
                  {item.panHash && <DetailRow label="PAN Hash" value={item.panHash} />}
                  <DetailRow label={t('ecommerce.status')} value={item.status} />
                  <DetailRow label={t('ecommerce.amount')} value={`${item.amount} ${item.currencyCode}`} />
                  <DetailRow label={t('ecommerce.threeDsVersion')} value={item.threeDsVersion} />
                  <DetailRow label={t('ecommerce.authValue')} value={item.authenticationValue || t('ecommerce.na')} />
                  <DetailRow label={t('ecommerce.eci')} value={item.eci || t('ecommerce.na')} />
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </SectionCard>
  );
}

function EpgMerchantsSection() {
  const { t } = useTranslation();
  const [list, setList] = useState<EpgMerchantConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState<EpgMerchantConfig | null>(null);
  const [creating, setCreating] = useState(false);
  const [form, setForm] = useState({ merchantId: '', apiKeyHash: '', apiSecretHash: '', webhookUrl: '' });
  const [saving, setSaving] = useState(false);

  const fetch = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.epg.merchants.list();
      setList(data);
      setLoaded(true);
    } catch (e) { setError(t('common.failedToLoad')); }
    setLoading(false);
  }, [t]);

  const startCreate = () => {
    setForm({ merchantId: '', apiKeyHash: '', apiSecretHash: '', webhookUrl: '' });
    setCreating(true);
    setEditing(null);
  };

  const startEdit = (item: EpgMerchantConfig) => {
    setForm({ merchantId: item.merchantId, apiKeyHash: item.apiKeyHash || '', apiSecretHash: item.apiSecretHash || '', webhookUrl: item.webhookUrl || '' });
    setEditing(item);
    setCreating(false);
  };

  const handleSave = async () => {
    if (!form.merchantId.trim() || !form.apiKeyHash.trim() || !form.apiSecretHash.trim()) return;
    setSaving(true);
    try {
      if (editing) {
        await api.epg.merchants.update(editing.id, form);
      } else {
        await api.epg.merchants.create(form);
      }
      setCreating(false);
      setEditing(null);
      fetch();
    } catch (e) { console.error(e); }
    setSaving(false);
  };

  const handleDelete = async (id: string) => {
    try {
      await api.epg.merchants.delete(id);
      fetch();
    } catch (e) { console.error(e); }
  };

  return (
    <SectionCard title={t('ecommerce.sectionEpgMerchants')} onRefresh={fetch}>
      <div style={{ marginBottom: 8 }}>
        <button onClick={startCreate} style={smallBtnStyle}>{t('ecommerce.addMerchant')}</button>
      </div>

      {(creating || editing) && (
        <div style={{ background: 'var(--surface)', borderRadius: 8, padding: 12, marginBottom: 12 }}>
          <Field label={t('ecommerce.merchantId')}>
            <input value={form.merchantId} onChange={e => setForm(f => ({ ...f, merchantId: e.target.value }))} />
          </Field>
          <Field label="API Key Hash">
            <input value={form.apiKeyHash} onChange={e => setForm(f => ({ ...f, apiKeyHash: e.target.value }))} />
          </Field>
          <Field label="API Secret Hash">
            <input value={form.apiSecretHash} onChange={e => setForm(f => ({ ...f, apiSecretHash: e.target.value }))} />
          </Field>
          <Field label="Webhook URL">
            <input value={form.webhookUrl} onChange={e => setForm(f => ({ ...f, webhookUrl: e.target.value }))} placeholder={t('ecommerce.optional')} />
          </Field>
          <div style={{ display: 'flex', gap: 8 }}>
            <button onClick={handleSave} disabled={saving} style={smallBtnStyle}>
              {saving ? t('ecommerce.creating') : editing ? t('ecommerce.updateMerchant') : t('ecommerce.createMerchant')}
            </button>
            <button onClick={() => { setCreating(false); setEditing(null); }} style={{ ...smallBtnStyle, background: '#6b7280' }}>
              {t('authorization.cancel')}
            </button>
          </div>
        </div>
      )}

      {!loaded && !loading && <EmptyState msg={t('common.noData')} />}
      {loading && <Skeleton />}
      {!loading && error && <ErrorMsg msg={error} />}
      {!loading && loaded && !error && list.length === 0 && <EmptyState msg={t('ecommerce.noMerchants')} />}
      {!loading && loaded && !error && list.length > 0 && (
        <div>
          {list.map(item => (
            <Row key={item.id}>
              <div style={{ flex: 1 }}>
                <RowLabel>{t('ecommerce.merchantId')}</RowLabel>
                <RowValue style={{ fontSize: 11 }}>{item.merchantId}</RowValue>
              </div>
              <div style={{ width: 100 }}>
                <RowLabel>{t('ecommerce.status')}</RowLabel>
                <RowValue>{item.isActive ? 'ACTIVE' : 'INACTIVE'}</RowValue>
              </div>
              <div style={{ width: 140 }}>
                <RowLabel>{t('ecommerce.createdAt')}</RowLabel>
                <RowValue>{new Date(item.createdAt).toLocaleDateString()}</RowValue>
              </div>
              <div style={{ width: 130 }}>
                <button onClick={() => startEdit(item)} style={{ ...smallBtnStyle, background: '#f59e0b', marginRight: 4 }}>
                  {t('ecommerce.editMerchant')}
                </button>
                <button onClick={() => handleDelete(item.id)} style={{ ...smallBtnStyle, background: '#ef4444' }}>
                  {t('ecommerce.deleteMerchant')}
                </button>
              </div>
            </Row>
          ))}
        </div>
      )}
    </SectionCard>
  );
}

function EpgTransactionsSection() {
  const { t } = useTranslation();
  const [list, setList] = useState<EpgTransaction[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('');

  const fetch = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.epg.transactions.list();
      setList(data);
      setLoaded(true);
    } catch (e) { setError(t('common.failedToLoad')); }
    setLoading(false);
  }, [t]);

  const filtered = filter ? list.filter(txn => txn.status === filter) : list;
  const uniqueStatuses = [...new Set(list.map(txn => txn.status))];

  return (
    <SectionCard title={t('ecommerce.sectionEpgTransactions')} onRefresh={fetch}>
      <div style={{ marginBottom: 8 }}>
        <select value={filter} onChange={e => setFilter(e.target.value)} style={filterSelectStyle}>
          <option value="">{t('ecommerce.allStatuses')}</option>
          {uniqueStatuses.map(s => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>

      {!loaded && !loading && <EmptyState msg={t('common.noData')} />}
      {loading && <Skeleton />}
      {!loading && error && <ErrorMsg msg={error} />}
      {!loading && loaded && !error && filtered.length === 0 && <EmptyState msg={t('ecommerce.noTransactions')} />}
      {!loading && loaded && !error && filtered.length > 0 && (
        <div>
          {filtered.map(txn => (
            <Row key={txn.id}>
              <div style={{ flex: 1 }}>
                <RowLabel>{t('ecommerce.id')}</RowLabel>
                <RowValue>{txn.id.length > 12 ? txn.id.slice(0, 12) + '...' : txn.id}</RowValue>
              </div>
              <div style={{ flex: 1 }}>
                <RowLabel>{t('ecommerce.txnMerchant')}</RowLabel>
                <RowValue>{txn.merchantId}</RowValue>
              </div>
              <div style={{ width: 100 }}>
                <RowLabel>{t('ecommerce.txnAmount')}</RowLabel>
                <RowValue>{txn.amount} {txn.currencyCode}</RowValue>
              </div>
              <div style={{ width: 80 }}>
                <RowLabel>{t('ecommerce.status')}</RowLabel>
                <RowValue>{txn.status}</RowValue>
              </div>
              <div style={{ width: 60 }}>
                <RowLabel>{t('ecommerce.txnThreeDs')}</RowLabel>
                <RowValue>{txn.threeDsRequired ? t('ecommerce.threeDsRequired_yes') : t('ecommerce.threeDsRequired_no')}</RowValue>
              </div>
            </Row>
          ))}
        </div>
      )}
    </SectionCard>
  );
}

function ThreeDsSessionsSection() {
  const { t } = useTranslation();
  const [list, setList] = useState<ThreeDsSession[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState<string | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.threeDs.sessions.list();
      setList(data);
      setLoaded(true);
    } catch (e) { setError(t('common.failedToLoad')); }
    setLoading(false);
  }, [t]);

  const handleCancel = async (id: string) => {
    setCancelling(id);
    try {
      await api.threeDs.sessions.cancel(id);
      fetch();
    } catch (e) { console.error(e); }
    setCancelling(null);
  };

  const canCancel = (status: string) => !['COMPLETED', 'FAILED', 'CANCELLED'].includes(status);

  return (
    <SectionCard title={t('ecommerce.sectionThreeDsSessions')} onRefresh={fetch}>
      {!loaded && !loading && <EmptyState msg={t('common.noData')} />}
      {loading && <Skeleton />}
      {!loading && error && <ErrorMsg msg={error} />}
      {!loading && loaded && !error && list.length === 0 && <EmptyState msg={t('ecommerce.noSessions')} />}
      {!loading && loaded && !error && list.length > 0 && (
        <div>
          {list.map(session => (
            <div key={session.id}>
              <Row onClick={() => setExpandedId(expandedId === session.id ? null : session.id)} style={{ cursor: 'pointer' }}>
                <div style={{ flex: 1 }}>
                  <RowLabel>{t('ecommerce.id')}</RowLabel>
                  <RowValue>{session.id.length > 12 ? session.id.slice(0, 12) + '...' : session.id}</RowValue>
                </div>
                <div style={{ width: 120 }}>
                  <RowLabel>{t('ecommerce.status')}</RowLabel>
                  <RowValue>{session.status}</RowValue>
                </div>
                <div style={{ width: 140 }}>
                  <RowLabel>{t('ecommerce.txnDate')}</RowLabel>
                  <RowValue>{new Date(session.createdAt).toLocaleDateString()}</RowValue>
                </div>
                <div style={{ width: 100 }}>
                  <button
                    onClick={(e) => { e.stopPropagation(); handleCancel(session.id); }}
                    disabled={cancelling === session.id || !canCancel(session.status)}
                    style={{ ...smallBtnStyle, background: '#ef4444', opacity: canCancel(session.status) ? 1 : 0.4 }}
                  >
                    {cancelling === session.id ? t('ecommerce.cancelling') : t('ecommerce.cancelSession')}
                  </button>
                </div>
              </Row>
              {expandedId === session.id && (
                <div style={{ background: 'var(--surface)', borderRadius: 8, padding: 12, margin: '4px 0 8px 0' }}>
                  <DetailRow label={t('ecommerce.id')} value={session.id} />
                  <DetailRow label={t('ecommerce.transactionId')} value={session.transactionId} />
                  <DetailRow label={t('ecommerce.status')} value={session.status} />
                  <DetailRow label={t('ecommerce.threeDsVersion')} value={session.threeDsVersion} />
                  <DetailRow label={t('ecommerce.authValue')} value={session.authenticationValue || t('ecommerce.na')} />
                  <DetailRow label={t('ecommerce.eci')} value={session.eci || t('ecommerce.na')} />
                  <DetailRow label={t('ecommerce.acsUrl')} value={session.acsUrl || t('ecommerce.na')} />
                  <DetailRow label="ACS Trans ID" value={session.acsTransId || t('ecommerce.na')} />
                  <DetailRow label="DS Trans ID" value={session.dsTransId || t('ecommerce.na')} />
                  <DetailRow label={t('ecommerce.notificationUrl')} value={session.notificationUrl || t('ecommerce.na')} />
                  <DetailRow label={t('ecommerce.createdAt') || 'Created'} value={new Date(session.createdAt).toLocaleString()} />
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </SectionCard>
  );
}

function SectionCard({ title, onRefresh, children }: { title: string; onRefresh: () => void; children: React.ReactNode }) {
  const { t } = useTranslation();
  return (
    <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <h4 style={{ fontSize: 14, fontWeight: 600, margin: 0 }}>{title}</h4>
        <button onClick={onRefresh} style={smallBtnStyle}>{t('ecommerce.refresh')}</button>
      </div>
      {children}
    </div>
  );
}

function Skeleton() {
  return <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>Loading...</p>;
}

function EmptyState({ msg }: { msg: string }) {
  return <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{msg}</p>;
}

function ErrorMsg({ msg }: { msg: string }) {
  return <p style={{ color: '#ef4444', fontSize: 13 }}>{msg}</p>;
}

function Row({ children, onClick, style }: { children: React.ReactNode; onClick?: () => void; style?: React.CSSProperties }) {
  return (
    <div
      onClick={onClick}
      style={{
        display: 'flex',
        gap: 8,
        alignItems: 'center',
        padding: '8px 0',
        borderBottom: '1px solid var(--border)',
        fontSize: 13,
        ...style,
      }}
    >
      {children}
    </div>
  );
}

function RowLabel({ children }: { children: React.ReactNode }) {
  return <div style={{ color: 'var(--text-secondary)', fontSize: 11, marginBottom: 2 }}>{children}</div>;
}

function RowValue({ children, style }: { children: React.ReactNode; style?: React.CSSProperties }) {
  return <div style={{ fontWeight: 600, fontSize: 12, ...style }}>{children}</div>;
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div style={{ marginBottom: 12 }}>
      <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4, fontWeight: 500 }}>
        {label}
      </label>
      {children}
    </div>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border)', fontSize: 13 }}>
      <span style={{ color: 'var(--text-secondary)' }}>{label}</span>
      <span style={{ fontWeight: 600, fontFamily: value.length > 20 ? 'monospace' : undefined, fontSize: value.length > 20 ? 11 : 13 }}>
        {value}
      </span>
    </div>
  );
}

const smallBtnStyle: React.CSSProperties = {
  padding: '6px 12px',
  borderRadius: 6,
  border: 'none',
  background: '#3b82f6',
  color: '#fff',
  fontWeight: 600,
  fontSize: 12,
  cursor: 'pointer',
};

const filterSelectStyle: React.CSSProperties = {
  padding: '6px 10px',
  borderRadius: 6,
  border: '1px solid var(--border)',
  background: 'var(--surface)',
  fontSize: 12,
  color: 'inherit',
};

const btnStyle: React.CSSProperties = {
  marginTop: 12,
  padding: '10px 24px',
  borderRadius: 8,
  border: 'none',
  background: '#3b82f6',
  color: '#fff',
  fontWeight: 600,
  fontSize: 13,
  cursor: 'pointer',
  width: '100%',
};
