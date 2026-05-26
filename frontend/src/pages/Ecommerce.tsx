import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { AcsAuthentication, AcsChallenge, EpgTransaction, ThreeDsSession } from '../types';
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
  );
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
