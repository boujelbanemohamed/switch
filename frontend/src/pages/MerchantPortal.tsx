import { useState, useCallback, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { request } from '../services/api';

type MerchantTab = 'dashboard' | 'transactions' | 'terminals' | 'settlements' | 'refunds' | 'reports' | 'info';

interface DashboardData {
  merchantCode: string;
  merchantName: string;
  merchantStatus: string;
  totalTerminals: number;
  activeTerminals: number;
  totalSwitchTransactions: number;
  totalEpgTransactions: number;
  totalTransactions: number;
  totalSwitchVolume: number;
  totalEpgVolume: number;
  totalVolume: number;
  totalSettled: number;
  pendingSettlements: number;
  refundedCount: number;
  currency: string;
}

interface TransactionItem {
  id: string;
  transactionId: string;
  type: 'TPE' | 'ECOMMERCE';
  protocol: string;
  messageType: string;
  amount: number;
  currency: string;
  stan: string | null;
  rrn: string | null;
  status: string;
  responseCode: string | null;
  terminalId: string | null;
  cardholderName?: string;
  customerEmail?: string;
  deviceChannel?: string;
  createdAt: string;
}

interface TerminalItem {
  id: string;
  terminalId: string;
  serialNumber: string | null;
  terminalType: string;
  manufacturer: string | null;
  model: string | null;
  status: string;
  createdAt: string;
}

interface SettlementItem {
  id: string;
  settlementDate: string;
  totalAmount: number;
  totalFees: number;
  netAmount: number;
  status: string;
  createdAt: string;
}

interface ReportData {
  merchantCode: string;
  merchantName: string;
  periodFrom: string;
  periodTo: string;
  totalTransactions: number;
  totalVolume: number;
  approvedCount: number;
  failedCount: number;
  successRate: number;
  totalFees: number;
  totalSettlements: number;
  currency: string;
}

export function MerchantPortal() {
  const { t } = useTranslation();
  const [merchantCode, setMerchantCode] = useState(
    () => sessionStorage.getItem('mp_code') || ''
  );
  const [loggedIn, setLoggedIn] = useState(false);
  const [tab, setTab] = useState<MerchantTab>('dashboard');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [dashboard, setDashboard] = useState<DashboardData | null>(null);
  const [transactions, setTransactions] = useState<TransactionItem[]>([]);
  const [terminals, setTerminals] = useState<TerminalItem[]>([]);
  const [settlements, setSettlements] = useState<SettlementItem[]>([]);
  const [report, setReport] = useState<ReportData | null>(null);
  const [reportFrom, setReportFrom] = useState('');
  const [reportTo, setReportTo] = useState('');
  const [refundEpgId, setRefundEpgId] = useState('');
  const [refundMsg, setRefundMsg] = useState('');
  const [merchantInfo, setMerchantInfo] = useState<Record<string, any> | null>(null);

  const login = useCallback(async () => {
    if (!merchantCode.trim()) return;
    setLoading(true);
    setError('');
    try {
      const [dashData, txnData, termData, settData, infoData] = await Promise.all([
        request<DashboardData>(`/merchant-portal/dashboard/${merchantCode}`),
        request<TransactionItem[]>(`/merchant-portal/transactions/${merchantCode}?page=0&size=50`),
        request<TerminalItem[]>(`/merchant-portal/terminals/${merchantCode}`),
        request<SettlementItem[]>(`/merchant-portal/settlements/${merchantCode}`),
        request<Record<string, any>>(`/merchant-portal/info/${merchantCode}`),
      ]);
      setDashboard(dashData);
      setTransactions(txnData);
      setTerminals(termData);
      setSettlements(settData);
      setMerchantInfo(infoData);
      setLoggedIn(true);
      sessionStorage.setItem('mp_code', merchantCode);
    } catch (e) {
      setError(t('common.failedToLoad'));
    }
    setLoading(false);
  }, [merchantCode, t]);

  const logout = () => {
    sessionStorage.removeItem('mp_code');
    setLoggedIn(false);
    setMerchantCode('');
    setDashboard(null);
    setTransactions([]);
    setTerminals([]);
    setSettlements([]);
    setReport(null);
    setMerchantInfo(null);
  };

  useEffect(() => {
    if (merchantCode && !loggedIn && !loading) {
      login();
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const loadTab = useCallback(async (newTab: MerchantTab) => {
    setTab(newTab);
    if (newTab === 'reports' && reportFrom && reportTo) {
      setLoading(true);
      try {
        const r = await request<ReportData>(
          `/merchant-portal/reports/${merchantCode}?from=${reportFrom}&to=${reportTo}`
        );
        setReport(r);
      } catch { setError(t('common.failedToLoad')); }
      setLoading(false);
    }
  }, [merchantCode, reportFrom, reportTo, t]);

  const generateReport = async () => {
    if (!reportFrom || !reportTo) return;
    setLoading(true);
    try {
      const r = await request<ReportData>(
        `/merchant-portal/reports/${merchantCode}?from=${reportFrom}&to=${reportTo}`
      );
      setReport(r);
    } catch { setError(t('common.failedToLoad')); }
    setLoading(false);
  };

  const handleRefund = async () => {
    if (!refundEpgId.trim()) return;
    setLoading(true);
    setRefundMsg('');
    try {
      await request(`/merchant-portal/refunds/${merchantCode}`, {
        method: 'POST',
        body: JSON.stringify({ epgTransactionId: refundEpgId }),
      });
      setRefundMsg(t('merchantPortal.refundSuccess'));
      setRefundEpgId('');
      const txnData = await request<TransactionItem[]>(`/merchant-portal/transactions/${merchantCode}`);
      setTransactions(txnData);
    } catch { setError(t('common.failedToLoad')); }
    setLoading(false);
  };

  if (!loggedIn) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 32, width: 400, maxWidth: '100%' }}>
          <h2 style={{ fontSize: 22, fontWeight: 700, marginBottom: 8 }}>{t('merchantPortal.title')}</h2>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 20 }}>
            {t('merchantPortal.enterCode')}
          </p>
          <input
            value={merchantCode}
            onChange={e => setMerchantCode(e.target.value)}
            placeholder={t('merchantPortal.merchantCode')}
            onKeyDown={e => e.key === 'Enter' && login()}
            style={{
              width: '100%', padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)',
              background: 'var(--bg)', color: 'var(--text)', fontSize: 14, marginBottom: 16, boxSizing: 'border-box',
            }}
          />
          {error && <p style={{ color: '#ef4444', fontSize: 13, marginBottom: 12 }}>{error}</p>}
          <button
            onClick={login}
            disabled={loading || !merchantCode.trim()}
            style={{
              width: '100%', padding: '10px', borderRadius: 8, border: 'none',
              background: '#3b82f6', color: '#fff', fontWeight: 600, fontSize: 14, cursor: 'pointer',
            }}
          >
            {loading ? t('common.loading') : t('merchantPortal.login')}
          </button>
        </div>
      </div>
    );
  }

  const tabs: { key: MerchantTab; label: string }[] = [
    { key: 'dashboard', label: t('merchantPortal.dashboard') },
    { key: 'transactions', label: t('merchantPortal.transactions') },
    { key: 'terminals', label: t('merchantPortal.terminals') },
    { key: 'settlements', label: t('merchantPortal.settlements') },
    { key: 'refunds', label: t('merchantPortal.refunds') },
    { key: 'reports', label: t('merchantPortal.reports') },
    { key: 'info', label: t('merchantPortal.info') },
  ];

  const btnStyle: React.CSSProperties = {
    padding: '8px 20px', borderRadius: 8, border: 'none',
    background: '#3b82f6', color: '#fff', fontWeight: 600, fontSize: 13, cursor: 'pointer',
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
        <h2 style={{ fontSize: 24, fontWeight: 700 }}>{t('merchantPortal.title')}</h2>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, fontSize: 13, color: 'var(--text-secondary)' }}>
          <strong>{dashboard?.merchantName}</strong> ({merchantCode})
          <button onClick={logout} style={{
            padding: '4px 10px', borderRadius: 6, border: '1px solid var(--border)',
            background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 12,
          }}>{t('auth.logout')}</button>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 24, flexWrap: 'wrap' }}>
        {tabs.map(tabItem => (
          <button
            key={tabItem.key}
            onClick={() => loadTab(tabItem.key)}
            style={{
              padding: '8px 20px', borderRadius: 8, border: 'none',
              background: tab === tabItem.key ? '#3b82f6' : 'var(--surface)',
              color: tab === tabItem.key ? '#fff' : 'var(--text-secondary)',
              fontWeight: 600, fontSize: 13, cursor: 'pointer',
            }}
          >
            {tabItem.label}
          </button>
        ))}
      </div>

      {error && (
        <div style={{ background: 'rgba(239,68,68,0.1)', color: '#ef4444', padding: '10px 16px', borderRadius: 8, fontSize: 13, marginBottom: 16 }}>
          {error}
        </div>
      )}

      {tab === 'dashboard' && dashboard && <DashboardTab data={dashboard} t={t} />}
      {tab === 'transactions' && <TransactionsTab data={transactions} t={t} />}
      {tab === 'terminals' && <TerminalsTab data={terminals} t={t} />}
      {tab === 'settlements' && <SettlementsTab data={settlements} t={t} />}
      {tab === 'refunds' && (
        <RefundsTab
          t={t}
          refundEpgId={refundEpgId}
          setRefundEpgId={setRefundEpgId}
          handleRefund={handleRefund}
          refundMsg={refundMsg}
          loading={loading}
        />
      )}
      {tab === 'reports' && (
        <ReportsTab
          t={t}
          reportFrom={reportFrom}
          setReportFrom={setReportFrom}
          reportTo={reportTo}
          setReportTo={setReportTo}
          generateReport={generateReport}
          report={report}
          loading={loading}
        />
      )}
      {tab === 'info' && merchantInfo && <InfoTab data={merchantInfo} t={t} />}
    </div>
  );
}

function DashboardTab({ data, t }: { data: DashboardData; t: (key: string) => string }) {
  const cards = [
    { label: t('merchantPortal.totalTerminals'), value: data.totalTerminals },
    { label: t('merchantPortal.activeTerminals'), value: data.activeTerminals },
    { label: t('merchantPortal.totalTransactions'), value: data.totalTransactions.toLocaleString() },
    { label: t('merchantPortal.totalVolume'), value: `${data.totalVolume.toLocaleString()} ${data.currency}` },
    { label: t('merchantPortal.totalSettled'), value: `${data.totalSettled.toLocaleString()} ${data.currency}` },
    { label: t('merchantPortal.pendingSettlements'), value: data.pendingSettlements },
    { label: t('merchantPortal.refundedCount'), value: data.refundedCount },
  ];

  return (
    <div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 16 }}>
        {cards.map((card, i) => (
          <div key={i} style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
            <div style={{ fontSize: 12, color: 'var(--text-secondary)', fontWeight: 500, marginBottom: 8 }}>{card.label}</div>
            <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--text)' }}>{card.value}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

function TransactionsTab({ data, t }: { data: TransactionItem[]; t: (key: string) => string }) {
  return (
    <div style={{ background: 'var(--surface)', borderRadius: 12, overflow: 'hidden' }}>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.date')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.transactionId')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.type')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.amount')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.currency')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.stan')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.rrn')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.status')}</th>
            </tr>
          </thead>
          <tbody>
            {data.length === 0 ? (
              <tr><td colSpan={8} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('merchantPortal.noData')}</td></tr>
            ) : data.slice(0, 100).map(txn => (
              <tr key={txn.id} style={{ borderBottom: '1px solid var(--border)' }}>
                <td style={{ padding: '10px 16px', whiteSpace: 'nowrap' }}>{new Date(txn.createdAt).toLocaleString()}</td>
                <td style={{ padding: '10px 16px', fontFamily: 'monospace', fontSize: 12 }}>{txn.transactionId}</td>
                <td style={{ padding: '10px 16px' }}>
                  <span style={{
                    padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                    background: txn.type === 'ECOMMERCE' ? 'rgba(139,92,246,0.15)' : 'rgba(59,130,246,0.15)',
                    color: txn.type === 'ECOMMERCE' ? '#a78bfa' : '#60a5fa',
                  }}>
                    {txn.type === 'ECOMMERCE' ? t('merchantPortal.ecommerce') : t('merchantPortal.tpe')}
                  </span>
                </td>
                <td style={{ padding: '10px 16px', fontWeight: 600 }}>{txn.amount?.toFixed(3)}</td>
                <td style={{ padding: '10px 16px' }}>{txn.currency}</td>
                <td style={{ padding: '10px 16px', fontFamily: 'monospace', fontSize: 12 }}>{txn.stan || '-'}</td>
                <td style={{ padding: '10px 16px', fontFamily: 'monospace', fontSize: 12 }}>{txn.rrn || '-'}</td>
                <td style={{ padding: '10px 16px' }}>
                  <StatusBadge status={txn.status} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function TerminalsTab({ data, t }: { data: TerminalItem[]; t: (key: string) => string }) {
  return (
    <div style={{ background: 'var(--surface)', borderRadius: 12, overflow: 'hidden' }}>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.terminalId')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.serialNumber')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.terminalType')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.manufacturer')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.status')}</th>
            </tr>
          </thead>
          <tbody>
            {data.length === 0 ? (
              <tr><td colSpan={5} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('merchantPortal.noData')}</td></tr>
            ) : data.map(t => (
              <tr key={t.id} style={{ borderBottom: '1px solid var(--border)' }}>
                <td style={{ padding: '10px 16px', fontFamily: 'monospace', fontWeight: 600 }}>{t.terminalId}</td>
                <td style={{ padding: '10px 16px' }}>{t.serialNumber || '-'}</td>
                <td style={{ padding: '10px 16px' }}>{t.terminalType}</td>
                <td style={{ padding: '10px 16px' }}>{t.manufacturer ? `${t.manufacturer} ${t.model || ''}` : '-'}</td>
                <td style={{ padding: '10px 16px' }}>
                  <StatusBadge status={t.status} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function SettlementsTab({ data, t }: { data: SettlementItem[]; t: (key: string) => string }) {
  return (
    <div style={{ background: 'var(--surface)', borderRadius: 12, overflow: 'hidden' }}>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.settlementDate')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.totalAmount')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.totalFees')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.netAmount')}</th>
              <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t('merchantPortal.settlementStatus')}</th>
            </tr>
          </thead>
          <tbody>
            {data.length === 0 ? (
              <tr><td colSpan={5} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>{t('merchantPortal.noData')}</td></tr>
            ) : data.map(s => (
              <tr key={s.id} style={{ borderBottom: '1px solid var(--border)' }}>
                <td style={{ padding: '10px 16px' }}>{s.settlementDate}</td>
                <td style={{ padding: '10px 16px', fontWeight: 600 }}>{s.totalAmount?.toFixed(3)}</td>
                <td style={{ padding: '10px 16px', color: '#ef4444' }}>{s.totalFees?.toFixed(3)}</td>
                <td style={{ padding: '10px 16px', fontWeight: 600, color: '#22c55e' }}>{s.netAmount?.toFixed(3)}</td>
                <td style={{ padding: '10px 16px' }}>
                  <StatusBadge status={s.status} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function RefundsTab({ t, refundEpgId, setRefundEpgId, handleRefund, refundMsg, loading }: {
  t: (key: string) => string;
  refundEpgId: string;
  setRefundEpgId: (v: string) => void;
  handleRefund: () => void;
  refundMsg: string;
  loading: boolean;
}) {
  return (
    <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 24, maxWidth: 500 }}>
      <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('merchantPortal.refundEpg')}</h3>
      <input
        value={refundEpgId}
        onChange={e => setRefundEpgId(e.target.value)}
        placeholder={t('merchantPortal.epgTransactionId')}
        style={{
          width: '100%', padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)',
          background: 'var(--bg)', color: 'var(--text)', fontSize: 14, marginBottom: 12, boxSizing: 'border-box',
        }}
      />
      <button
        onClick={handleRefund}
        disabled={loading || !refundEpgId.trim()}
        style={{
          padding: '10px 24px', borderRadius: 8, border: 'none',
          background: '#ef4444', color: '#fff', fontWeight: 600, fontSize: 13, cursor: 'pointer',
        }}
      >
        {loading ? t('common.loading') : t('merchantPortal.refund')}
      </button>
      {refundMsg && (
        <p style={{ color: '#22c55e', fontSize: 13, marginTop: 12 }}>{refundMsg}</p>
      )}
    </div>
  );
}

function ReportsTab({ t, reportFrom, setReportFrom, reportTo, setReportTo, generateReport, report, loading }: {
  t: (key: string) => string;
  reportFrom: string;
  setReportFrom: (v: string) => void;
  reportTo: string;
  setReportTo: (v: string) => void;
  generateReport: () => void;
  report: ReportData | null;
  loading: boolean;
}) {
  return (
    <div>
      <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 24, marginBottom: 24, display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
        <div>
          <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4, fontWeight: 500 }}>{t('merchantPortal.periodFrom')}</label>
          <input type="date" value={reportFrom} onChange={e => setReportFrom(e.target.value)}
            style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }} />
        </div>
        <div>
          <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4, fontWeight: 500 }}>{t('merchantPortal.periodTo')}</label>
          <input type="date" value={reportTo} onChange={e => setReportTo(e.target.value)}
            style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13 }} />
        </div>
        <button onClick={generateReport} disabled={loading || !reportFrom || !reportTo}
          style={{ padding: '8px 20px', borderRadius: 8, border: 'none', background: '#3b82f6', color: '#fff', fontWeight: 600, fontSize: 13, cursor: 'pointer' }}>
          {loading ? t('common.loading') : t('merchantPortal.generateReport')}
        </button>
      </div>

      {report && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 24 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>{t('merchantPortal.reportSummary')}</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 16 }}>
            <ReportCard label={t('merchantPortal.merchantName')} value={report.merchantName} />
            <ReportCard label={t('merchantPortal.totalTransactions')} value={report.totalTransactions.toLocaleString()} />
            <ReportCard label={t('merchantPortal.totalVolume')} value={`${report.totalVolume.toLocaleString()} ${report.currency}`} />
            <ReportCard label={t('merchantPortal.approvedCount')} value={report.approvedCount.toLocaleString()} />
            <ReportCard label={t('merchantPortal.failedCount')} value={report.failedCount.toLocaleString()} />
            <ReportCard label={t('merchantPortal.successRate')} value={`${report.successRate.toFixed(1)}%`} />
            <ReportCard label={t('merchantPortal.feesCollected')} value={`${report.totalFees.toLocaleString()} ${report.currency}`} />
            <ReportCard label={t('merchantPortal.totalSettlements')} value={report.totalSettlements.toLocaleString()} />
          </div>
        </div>
      )}
    </div>
  );
}

function ReportCard({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ padding: 16, background: 'var(--bg)', borderRadius: 8 }}>
      <div style={{ fontSize: 11, color: 'var(--text-secondary)', fontWeight: 500, marginBottom: 4 }}>{label}</div>
      <div style={{ fontSize: 18, fontWeight: 700 }}>{value}</div>
    </div>
  );
}

function InfoTab({ data, t }: { data: Record<string, any>; t: (key: string) => string }) {
  const fields: { key: string; label: string }[] = [
    { key: 'legalName', label: t('merchantPortal.legalName') },
    { key: 'merchantCategoryCode', label: t('merchantPortal.merchantCategoryCode') },
    { key: 'registrationNumber', label: t('merchantPortal.registrationNumber') },
    { key: 'taxId', label: t('merchantPortal.taxId') },
    { key: 'email', label: 'Email' },
    { key: 'phone', label: 'Téléphone' },
    { key: 'website', label: 'Site Web' },
    { key: 'addressLine1', label: 'Adresse' },
    { key: 'city', label: 'Ville' },
    { key: 'postalCode', label: 'Code Postal' },
    { key: 'countryCode', label: 'Pays' },
    { key: 'riskLevel', label: t('merchantPortal.riskLevel') },
    { key: 'activationDate', label: t('merchantPortal.activationDate') },
    { key: 'settlementMethod', label: t('merchantPortal.settlementMethod') },
    { key: 'settlementCurrency', label: 'Devise Règlement' },
    { key: 'settlementAccountIban', label: t('merchantPortal.settlementAccountIban') },
    { key: 'settlementCycle', label: t('merchantPortal.settlementCycle') },
    { key: 'mdrPercentage', label: t('merchantPortal.mdrPercentage') },
    { key: 'mdrFixedFee', label: t('merchantPortal.mdrFixedFee') },
  ];

  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 12 }}>
      <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
        <div style={{ fontSize: 12, color: 'var(--text-secondary)', fontWeight: 500, marginBottom: 12 }}>
          {t('merchantPortal.merchantName')}
        </div>
        <div style={{ fontSize: 20, fontWeight: 700 }}>{data.tradingName}</div>
        <div style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 4 }}>
          {data.merchantCode}
        </div>
        <div style={{ marginTop: 12 }}>
          <StatusBadge status={data.status} />
        </div>
      </div>
      {fields.map(f => (
        <div key={f.key} style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', fontWeight: 500, marginBottom: 4 }}>
            {f.label}
          </div>
          <div style={{ fontSize: 15, fontWeight: 600, wordBreak: 'break-word' }}>
            {data[f.key] != null ? String(data[f.key]) : '-'}
          </div>
        </div>
      ))}
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const colorMap: Record<string, string> = {
    ACTIVE: '#22c55e', INACTIVE: '#6b7280', SUSPENDED: '#f59e0b', RETIRED: '#6b7280',
    PENDING: '#f59e0b', COMPLETED: '#22c55e', FAILED: '#ef4444', REJECTED: '#ef4444',
    CAPTURED: '#22c55e', AUTHORIZED: '#3b82f6', REFUNDED: '#a78bfa', CANCELLED: '#6b7280',
    INITIATED: '#f59e0b', PENDING_ONBOARDING: '#f59e0b', MALFUNCTION: '#ef4444',
    PAID: '#22c55e', CONFIRMED: '#3b82f6', DISPUTED: '#ef4444',
  };
  const bgColorMap: Record<string, string> = {
    ACTIVE: 'rgba(34,197,94,0.15)', INACTIVE: 'rgba(107,114,128,0.15)', SUSPENDED: 'rgba(245,158,11,0.15)', RETIRED: 'rgba(107,114,128,0.15)',
    PENDING: 'rgba(245,158,11,0.15)', COMPLETED: 'rgba(34,197,94,0.15)', FAILED: 'rgba(239,68,68,0.15)', REJECTED: 'rgba(239,68,68,0.15)',
    CAPTURED: 'rgba(34,197,94,0.15)', AUTHORIZED: 'rgba(59,130,246,0.15)', REFUNDED: 'rgba(167,139,250,0.15)', CANCELLED: 'rgba(107,114,128,0.15)',
    INITIATED: 'rgba(245,158,11,0.15)', PENDING_ONBOARDING: 'rgba(245,158,11,0.15)', MALFUNCTION: 'rgba(239,68,68,0.15)',
    PAID: 'rgba(34,197,94,0.15)', CONFIRMED: 'rgba(59,130,246,0.15)', DISPUTED: 'rgba(239,68,68,0.15)',
  };
  return (
    <span style={{
      padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
      background: bgColorMap[status] || 'rgba(107,114,128,0.15)',
      color: colorMap[status] || '#6b7280',
    }}>
      {status}
    </span>
  );
}
