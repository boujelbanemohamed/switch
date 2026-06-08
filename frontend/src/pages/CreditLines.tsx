import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import type { CreditLine, CreditStatement, InstallmentPlan, InstallmentEntry, CardAccount } from '../types';
import { CreditLinesHelp, CREDIT_LINE_LABELS, STATEMENT_LABELS, INSTALLMENT_LABELS } from '../components/CreditLinesHelp';

export function CreditLines() {
  const { t } = useTranslation();
  const [lines, setLines] = useState<CreditLine[]>([]);
  const [accounts, setAccounts] = useState<CardAccount[]>([]);
  const [selectedLine, setSelectedLine] = useState<CreditLine | null>(null);
  const [statements, setStatements] = useState<CreditStatement[]>([]);
  const [installments, setInstallments] = useState<InstallmentPlan[]>([]);
  const [entries, setEntries] = useState<InstallmentEntry[]>([]);
  const [tab, setTab] = useState<'details' | 'statements' | 'installments' | 'simulate'>('details');
  const [amount, setAmount] = useState('');
  const [ref, setRef] = useState('');
  const [loading, setLoading] = useState(false);
  const [simResult, setSimResult] = useState<any>(null);

  // Simulate form
  const [simBalance, setSimBalance] = useState('1000');
  const [simApr, setSimApr] = useState('18');
  const [simMinPct, setSimMinPct] = useState('5');
  const [simFloor, setSimFloor] = useState('10');

  // Installment form
  const [instAmount, setInstAmount] = useState('');
  const [instCount, setInstCount] = useState('6');
  const [instFee, setInstFee] = useState('0');
  const [instApr, setInstApr] = useState('');

  // Open credit line modal
  const [showOpenModal, setShowOpenModal] = useState(false);
  const [openForm, setOpenForm] = useState({
    cardAccountId: '', creditLimit: '5000', apr: '18',
    statementDay: '1', paymentDueDays: '20', minPaymentPct: '5', minPaymentFloor: '10',
  });

  useEffect(() => {
    api.issuing.accounts.list().then(data => {
      const list = (data as any).content ?? data as CardAccount[];
      setAccounts(list);
    });
    api.credit.lines.list().then(data => {
      const allLines = Array.isArray(data) ? data : (data as any)?.content ?? [];
      setLines(allLines as CreditLine[]);
    }).catch(() => {});
  }, []);

  function selectLine(line: CreditLine) {
    setSelectedLine(line);
    setTab('details');
    api.credit.lines.statements(line.id).then(setStatements).catch(() => {});
    api.credit.lines.installments(line.id).then(setInstallments).catch(() => {});
  }

  async function handleAuthorize() {
    if (!selectedLine || !amount) return;
    setLoading(true);
    try {
      const updated = await api.credit.lines.authorize(selectedLine.id, Number(amount));
      setLines(lines.map(l => l.id === updated.id ? updated : l));
      setSelectedLine(updated);
    } catch (e: any) { alert(e.message); }
    setLoading(false);
  }

  async function handlePurchase() {
    if (!selectedLine || !amount || !ref) return;
    setLoading(true);
    try {
      const updated = await api.credit.lines.purchase(selectedLine.id, Number(amount), ref);
      setLines(lines.map(l => l.id === updated.id ? updated : l));
      setSelectedLine(updated);
    } catch (e: any) { alert(e.message); }
    setLoading(false);
  }

  async function handlePayment() {
    if (!selectedLine || !amount) return;
    setLoading(true);
    try {
      const updated = await api.credit.lines.payment(selectedLine.id, Number(amount));
      setLines(lines.map(l => l.id === updated.id ? updated : l));
      setSelectedLine(updated);
      api.credit.lines.statements(selectedLine.id).then(setStatements).catch(() => {});
    } catch (e: any) { alert(e.message); }
    setLoading(false);
  }

  async function handleSimulate() {
    if (!selectedLine) return;
    try {
      const result = await api.credit.lines.simulate(
        selectedLine.id, Number(simBalance), Number(simApr), Number(simMinPct), Number(simFloor));
      setSimResult(result);
    } catch (e: any) { alert(e.message); }
  }

  async function handleGenerateStatement() {
    if (!selectedLine) return;
    try {
      const stmt = await api.credit.lines.generateStatement(selectedLine.id);
      setStatements([stmt, ...statements]);
    } catch (e: any) { alert(e.message); }
  }

  async function handleOpenLine() {
    setLoading(true);
    try {
      const line = await api.credit.lines.open({
        cardAccountId: openForm.cardAccountId,
        creditLimit: Number(openForm.creditLimit),
        apr: Number(openForm.apr),
        statementDay: Number(openForm.statementDay),
        paymentDueDays: Number(openForm.paymentDueDays),
        minPaymentPct: Number(openForm.minPaymentPct),
        minPaymentFloor: Number(openForm.minPaymentFloor),
      });
      setLines([...lines, line]);
      setShowOpenModal(false);
      selectLine(line);
    } catch (e: any) { alert(e.message); }
    setLoading(false);
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <h2 style={{ fontSize: 22, fontWeight: 700 }}>{t('credit.title')}</h2>
          <CreditLinesHelp />
        </div>
        <button onClick={() => setShowOpenModal(true)} style={{
          padding: '10px 20px', borderRadius: 8, border: 'none',
          background: '#3b82f6', color: 'white', cursor: 'pointer', fontSize: 13, fontWeight: 600,
        }}>{t('credit.openLine')}</button>
      </div>

      {lines.length === 0 && (
        <p style={{ color: 'var(--text-secondary)', padding: 20 }}>{t('credit.noLines')}</p>
      )}

      {/* Line cards */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 24 }}>
        {lines.map(line => (
          <div key={line.id} onClick={() => selectLine(line)}
               style={{
            background: selectedLine?.id === line.id ? 'var(--surface)' : 'var(--bg)',
            borderRadius: 12, padding: 20, border: selectedLine?.id === line.id ? '2px solid #3b82f6' : '1px solid var(--border)',
            cursor: 'pointer', display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 12,
          }}>
            <div>
              <p style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{t('credit.limit')}</p>
              <p style={{ fontSize: 18, fontWeight: 700 }}>{line.creditLimit.toLocaleString()} {line.currencyCode}</p>
            </div>
            <div>
              <p style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{t('credit.balance')}</p>
              <p style={{ fontSize: 18, fontWeight: 700 }}>{line.currentBalance.toLocaleString()}</p>
            </div>
            <div>
              <p style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{t('credit.available')}</p>
              <p style={{ fontSize: 18, fontWeight: 700, color: line.availableCredit > 0 ? '#22c55e' : '#ef4444' }}>
                {line.availableCredit.toLocaleString()}
              </p>
            </div>
            <div>
              <p style={{ fontSize: 11, color: 'var(--text-secondary)' }}>APR</p>
              <p style={{ fontSize: 18, fontWeight: 700 }}>{line.apr}%</p>
            </div>
            <div>
              <p style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{t('common.status')}</p>
              <p style={{ fontSize: 18, fontWeight: 700 }}>{CREDIT_LINE_LABELS[line.status] ?? line.status}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Detail panel */}
      {selectedLine && (
        <div style={{ background: 'var(--surface)', borderRadius: 12, padding: 20 }}>
          <div style={{ display: 'flex', gap: 12, marginBottom: 20, borderBottom: '1px solid var(--border)', paddingBottom: 12 }}>
            {(['details', 'statements', 'installments', 'simulate'] as const).map(tabKey => (
              <button key={tabKey} onClick={() => setTab(tabKey)}
                style={{
                  padding: '8px 16px', borderRadius: 6, border: 'none',
                  background: tab === tabKey ? '#3b82f6' : 'transparent',
                  color: tab === tabKey ? 'white' : 'var(--text-secondary)',
                  cursor: 'pointer', fontSize: 13, fontWeight: 600,
                }}>{t(`credit.tab_${tabKey}` as any)}</button>
            ))}
          </div>

          {tab === 'details' && (
            <div>
              <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
                <input value={amount} onChange={e => setAmount(e.target.value)} type="number" placeholder={t('credit.amountPlaceholder')}
                  style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', width: 120, fontSize: 13 }} />
                <input value={ref} onChange={e => setRef(e.target.value)} placeholder={t('credit.refPlaceholder')}
                  style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', width: 160, fontSize: 13 }} />
                <ActionBtn label={t('credit.authorize')} color="#3b82f6" onClick={handleAuthorize} disabled={loading || !amount} />
                <ActionBtn label={t('credit.purchase')} color="#8b5cf6" onClick={handlePurchase} disabled={loading || !amount || !ref} />
                <ActionBtn label={t('credit.payment')} color="#22c55e" onClick={handlePayment} disabled={loading || !amount} />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
                <StatCard title={t('credit.creditLimit')} value={`${selectedLine.creditLimit.toLocaleString()} ${selectedLine.currencyCode}`} />
                <StatCard title={t('credit.currentBalance')} value={`${selectedLine.currentBalance.toLocaleString()}`} />
                <StatCard title={t('credit.availableCredit')} value={`${selectedLine.availableCredit.toLocaleString()}`} color={selectedLine.availableCredit > 0 ? '#22c55e' : '#ef4444'} />
              </div>
            </div>
          )}

          {tab === 'statements' && (
            <div>
              <button onClick={handleGenerateStatement} style={{
                padding: '8px 16px', borderRadius: 6, border: 'none',
                background: '#3b82f6', color: 'white', cursor: 'pointer', fontSize: 13, fontWeight: 600, marginBottom: 16,
              }}>{t('credit.generateStatement')}</button>
              {statements.length === 0 ? (
                <p style={{ color: 'var(--text-secondary)' }}>{t('credit.noStatements')}</p>
              ) : (
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                      <th style={{ padding: '8px 12px', color: 'var(--text-secondary)' }}>{t('credit.statementDate')}</th>
                      <th style={{ padding: '8px 12px', color: 'var(--text-secondary)' }}>{t('credit.opening')}</th>
                      <th style={{ padding: '8px 12px', color: 'var(--text-secondary)' }}>{t('credit.purchases')}</th>
                      <th style={{ padding: '8px 12px', color: 'var(--text-secondary)' }}>{t('credit.interest')}</th>
                      <th style={{ padding: '8px 12px', color: 'var(--text-secondary)' }}>{t('credit.closing')}</th>
                      <th style={{ padding: '8px 12px', color: 'var(--text-secondary)' }}>{t('credit.minPayment')}</th>
                      <th style={{ padding: '8px 12px', color: 'var(--text-secondary)' }}>{t('credit.dueDate')}</th>
                      <th style={{ padding: '8px 12px', color: 'var(--text-secondary)' }}>{t('common.status')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {statements.map(s => (
                      <tr key={s.id} style={{ borderBottom: '1px solid var(--border)' }}>
                        <td style={{ padding: '10px 12px' }}>{s.statementDate}</td>
                        <td style={{ padding: '10px 12px' }}>{s.openingBalance.toLocaleString()}</td>
                        <td style={{ padding: '10px 12px' }}>{s.purchasesTotal.toLocaleString()}</td>
                        <td style={{ padding: '10px 12px' }}>{s.interestCharged.toLocaleString()}</td>
                        <td style={{ padding: '10px 12px', fontWeight: 700 }}>{s.closingBalance.toLocaleString()}</td>
                        <td style={{ padding: '10px 12px' }}>{s.minimumPayment.toLocaleString()}</td>
                        <td style={{ padding: '10px 12px' }}>{s.dueDate}</td>
                        <td style={{ padding: '10px 12px' }}>
                          <span style={{
                            background: s.status === 'PAID' ? '#22c55e33' : s.status === 'OVERDUE' ? '#ef444433' : '#3b82f633',
                            color: s.status === 'PAID' ? '#22c55e' : s.status === 'OVERDUE' ? '#ef4444' : '#3b82f6',
                            padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600,
                          }}>{STATEMENT_LABELS[s.status] ?? s.status}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}

          {tab === 'installments' && (
            <div>
              <div style={{ display: 'flex', gap: 12, marginBottom: 16, alignItems: 'center', flexWrap: 'wrap' }}>
                <input value={instAmount} onChange={e => setInstAmount(e.target.value)} type="number" placeholder={t('credit.amountPlaceholder')}
                  style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', width: 100, fontSize: 13 }} />
                <input value={instCount} onChange={e => setInstCount(e.target.value)} type="number" placeholder={t('credit.count')}
                  style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', width: 70, fontSize: 13 }} />
                <input value={instFee} onChange={e => setInstFee(e.target.value)} type="number" placeholder={t('credit.fee')}
                  style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', width: 70, fontSize: 13 }} />
                <input value={instApr} onChange={e => setInstApr(e.target.value)} type="number" placeholder={t('credit.installmentApr')}
                  style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', width: 70, fontSize: 13 }} />
                <ActionBtn label={t('credit.convert')} color="#8b5cf6" onClick={async () => {
                  if (!instAmount || !instCount) return;
                  await api.credit.lines.convertToInstallments(selectedLine.id, {
                    totalAmount: Number(instAmount), count: Number(instCount),
                    feeAmount: Number(instFee || 0), apr: instApr ? Number(instApr) : null,
                  });
                  api.credit.lines.installments(selectedLine.id).then(setInstallments);
                }} />
              </div>
              {installments.length === 0 ? (
                <p style={{ color: 'var(--text-secondary)' }}>{t('credit.noInstallments')}</p>
              ) : installments.map(plan => (
                <div key={plan.id} style={{ background: 'var(--bg)', borderRadius: 8, padding: 16, marginBottom: 12, border: '1px solid var(--border)' }}
                     onClick={async () => {
                       const ent = await api.credit.installmentPlans.entries(plan.id);
                       setEntries(ent);
                     }}>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 12, marginBottom: 8 }}>
                    <div><p style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{t('credit.totalAmount')}</p><p style={{ fontWeight: 700 }}>{plan.totalAmount}</p></div>
                    <div><p style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{t('credit.installmentAmount')}</p><p style={{ fontWeight: 700 }}>{plan.installmentAmount}</p></div>
                    <div><p style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{t('credit.count')}</p><p style={{ fontWeight: 700 }}>{plan.remainingCount}/{plan.installmentCount}</p></div>
                    <div><p style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{t('credit.fee')}</p><p style={{ fontWeight: 700 }}>{plan.feeAmount}</p></div>
                    <div><p style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{t('common.status')}</p><p style={{ fontWeight: 700 }}>{INSTALLMENT_LABELS[plan.status] ?? plan.status}</p></div>
                  </div>
                  {entries.length > 0 && entries[0].installmentPlanId === plan.id && (
                    <div>
                      <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 8 }}>{t('credit.entries')}</p>
                      {entries.map(e => (
                        <div key={e.id} style={{ display: 'flex', gap: 16, padding: '4px 0', fontSize: 12 }}>
                          <span>#{e.sequenceNumber}</span>
                          <span>{e.dueDate}</span>
                          <span style={{ fontWeight: 600 }}>{e.amount}</span>
                          <span style={{ color: e.paid ? '#22c55e' : '#f59e0b' }}>{e.paid ? t('credit.paid') : t('credit.pending')}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}

          {tab === 'simulate' && (
            <div>
              <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap', alignItems: 'center' }}>
                <div><p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 2 }}>{t('credit.balance')}</p>
                  <input value={simBalance} onChange={e => setSimBalance(e.target.value)} type="number"
                    style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', width: 100, fontSize: 13 }} /></div>
                <div><p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 2 }}>APR %</p>
                  <input value={simApr} onChange={e => setSimApr(e.target.value)} type="number"
                    style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', width: 70, fontSize: 13 }} /></div>
                <div><p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 2 }}>{t('credit.minPct')}</p>
                  <input value={simMinPct} onChange={e => setSimMinPct(e.target.value)} type="number"
                    style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', width: 70, fontSize: 13 }} /></div>
                <div><p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 2 }}>{t('credit.floor')}</p>
                  <input value={simFloor} onChange={e => setSimFloor(e.target.value)} type="number"
                    style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', width: 70, fontSize: 13 }} /></div>
                <ActionBtn label={t('credit.calculate')} color="#3b82f6" onClick={handleSimulate} />
              </div>
              {simResult && (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginTop: 12 }}>
                  <StatCard title={t('credit.monthlyRate')} value={`${simResult.monthlyRate}%`} />
                  <StatCard title={t('credit.interestCharged')} value={simResult.interestCharged.toLocaleString()} color="#f59e0b" />
                  <StatCard title={t('credit.minPayment')} value={simResult.minimumPayment.toLocaleString()} color="#22c55e" />
                  <StatCard title={t('credit.newBalance')} value={simResult.newBalance.toLocaleString()} color="#8b5cf6" />
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Open line modal */}
      {showOpenModal && (
        <div style={styles.overlay} onClick={() => setShowOpenModal(false)}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 20 }}>{t('credit.openLine')}</h3>
            <div style={{ display: 'grid', gap: 14 }}>
              <div><p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>{t('credit.cardAccount')}</p>
                <select style={styles.select} value={openForm.cardAccountId} onChange={e => setOpenForm({ ...openForm, cardAccountId: e.target.value })}>
                  <option value="">-- Select --</option>
                  {accounts.map(a => <option key={a.id} value={a.id}>{a.id.slice(0, 8)}... ({a.currencyCode})</option>)}
                </select>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={t('credit.creditLimit')}><input style={styles.input} value={openForm.creditLimit} onChange={e => setOpenForm({ ...openForm, creditLimit: e.target.value })} /></Field>
                <Field label="APR %"><input style={styles.input} value={openForm.apr} onChange={e => setOpenForm({ ...openForm, apr: e.target.value })} /></Field>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
                <Field label={t('credit.statementDay')}><input style={styles.input} value={openForm.statementDay} onChange={e => setOpenForm({ ...openForm, statementDay: e.target.value })} /></Field>
                <Field label={t('credit.paymentDueDays')}><input style={styles.input} value={openForm.paymentDueDays} onChange={e => setOpenForm({ ...openForm, paymentDueDays: e.target.value })} /></Field>
                <Field label={t('credit.minPct')}><input style={styles.input} value={openForm.minPaymentPct} onChange={e => setOpenForm({ ...openForm, minPaymentPct: e.target.value })} /></Field>
              </div>
              <Field label={t('credit.floor')}><input style={styles.input} value={openForm.minPaymentFloor} onChange={e => setOpenForm({ ...openForm, minPaymentFloor: e.target.value })} /></Field>
            </div>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 24 }}>
              <button onClick={() => setShowOpenModal(false)} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', color: 'var(--text)', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              }}>{t('common.cancel')}</button>
              <button onClick={handleOpenLine} disabled={loading || !openForm.cardAccountId} style={{
                padding: '10px 20px', borderRadius: 8, border: 'none',
                background: loading || !openForm.cardAccountId ? '#64748b' : '#3b82f6',
                color: 'white', cursor: loading ? 'not-allowed' : 'pointer', fontSize: 13, fontWeight: 600,
              }}>{loading ? t('common.loading') : t('credit.create')}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function ActionBtn({ label, color, onClick, disabled }: { label: string; color: string; onClick: () => void; disabled?: boolean }) {
  return (
    <button onClick={onClick} disabled={disabled} style={{
      padding: '8px 16px', borderRadius: 6, border: 'none',
      background: disabled ? '#64748b' : color, color: 'white',
      cursor: disabled ? 'not-allowed' : 'pointer', fontSize: 13, fontWeight: 600,
    }}>{label}</button>
  );
}

function StatCard({ title, value, color }: { title: string; value: string; color?: string }) {
  return (
    <div style={{ background: 'var(--bg)', borderRadius: 8, padding: '12px 16px', border: '1px solid var(--border)' }}>
      <p style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>{title}</p>
      <p style={{ fontSize: 20, fontWeight: 700, color: color || 'var(--text)' }}>{value}</p>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>{label}</p>
      {children}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  overlay: { position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 },
  modal: { background: 'var(--surface)', borderRadius: 16, padding: 24, width: 520, maxWidth: '90vw' },
  input: { width: '100%', padding: '8px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13, boxSizing: 'border-box' },
  select: { width: '100%', padding: '8px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 13, boxSizing: 'border-box' },
};
