import { useState, useEffect, useId } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import { SectionHeader } from '../components/SectionHeader';
import { FeeSchedulesHelp, SCHEDULE_TYPE_LABELS, SCHEDULE_STATUS_LABELS, APPLIES_TO_LABELS, CALC_METHOD_LABELS } from '../components/FeeSchedulesHelp';
import {
  Plus, Pencil, Trash2, ToggleLeft, ToggleRight,
  DollarSign, RefreshCw,
} from 'lucide-react';

interface FeeSchedule {
  id: string;
  name: string;
  description: string | null;
  scheduleType: string;
  status: string;
  priority: number;
  currencyCode: string;
  effectiveFrom: string;
  effectiveUntil: string | null;
  participantId: string | null;
  merchantId: string | null;
  appliesTo: string;
}

interface FeeRule {
  id: string;
  scheduleId: string;
  ruleName: string;
  ruleOrder: number;
  calcMethod: string;
  flatAmount: number | null;
  percentageRate: number | null;
  minAmount: number | null;
  maxAmount: number | null;
  brandFilter: string | null;
  cardTypeFilter: string | null;
  mccFilter: string | null;
}

export function FeeSchedules() {
  const { t } = useTranslation();
  const [schedules, setSchedules] = useState<FeeSchedule[]>([]);
  const [selectedSchedule, setSelectedSchedule] = useState<FeeSchedule | null>(null);
  const [rules, setRules] = useState<FeeRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [showRuleForm, setShowRuleForm] = useState(false);

  const fetchSchedules = async () => {
    setLoading(true);
    try {
      const data = await api.get<FeeSchedule[]>('/fees/schedules');
      setSchedules(data || []);
    } catch { setSchedules([]); } finally { setLoading(false); }
  };

  const fetchRules = async (id: string) => {
    try {
      const data = await api.get<FeeRule[]>(`/fees/schedules/${id}/rules`);
      setRules(data || []);
    } catch { setRules([]); }
  };

  useEffect(() => { fetchSchedules(); }, []);

  const selectSchedule = (s: FeeSchedule) => {
    setSelectedSchedule(s);
    fetchRules(s.id);
  };

  const toggleStatus = async (s: FeeSchedule) => {
    const endpoint = s.status === 'ACTIVE' ? 'deactivate' : 'activate';
    await api.post(`/fees/schedules/${s.id}/${endpoint}`);
    fetchSchedules();
    if (selectedSchedule?.id === s.id) selectSchedule(s);
  };

  const statusBadge = (s: string) => {
    const colors: Record<string, string> = {
      ACTIVE: 'bg-green-900/50 text-green-400',
      DRAFT: 'bg-yellow-900/50 text-yellow-400',
      INACTIVE: 'bg-gray-700 text-gray-400',
      ARCHIVED: 'bg-red-900/50 text-red-400',
    };
    return <span className={`px-2 py-0.5 rounded text-xs ${colors[s] || 'bg-gray-700 text-gray-400'}`}>{SCHEDULE_STATUS_LABELS[s] || s}</span>;
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <h1 className="text-2xl font-bold">{t('fees.title')}</h1>
          <FeeSchedulesHelp />
        </div>
        <button onClick={() => setShowForm(true)}
          className="flex items-center gap-1 px-3 py-1.5 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm">
          <Plus className="w-4 h-4" /> {t('fees.newSchedule')}
        </button>
      </div>

      <SectionHeader sectionKey="fees" />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-1">
          <div className="bg-gray-900 rounded-lg border border-gray-700 overflow-hidden">
            <div className="p-3 bg-gray-800 text-sm font-medium text-gray-300">{t('fees.schedules')}</div>
            <div className="divide-y divide-gray-700 max-h-[600px] overflow-y-auto">
              {loading ? (
                <div className="p-4 text-center text-gray-500">{t('common.loading')}</div>
              ) : schedules.length === 0 ? (
                <div className="p-4 text-center text-gray-500">{t('common.noData')}</div>
              ) : schedules.map(s => (
                <div key={s.id}
                  onClick={() => selectSchedule(s)}
                  className={`p-3 cursor-pointer hover:bg-gray-800/50 transition-colors ${
                    selectedSchedule?.id === s.id ? 'bg-gray-800 border-l-2 border-indigo-500' : ''
                  }`}>
                  <div className="flex items-center justify-between">
                    <p className="font-medium text-sm">{s.name}</p>
                    {statusBadge(s.status)}
                  </div>
                  <p className="text-xs text-gray-500 mt-1">{SCHEDULE_TYPE_LABELS[s.scheduleType] || s.scheduleType} · {s.currencyCode}</p>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="lg:col-span-2">
          {!selectedSchedule ? (
            <div className="bg-gray-900 rounded-lg border border-gray-700 p-8 text-center text-gray-500">
              {t('fees.selectSchedule')}
            </div>
          ) : (
            <div className="space-y-4">
              <div className="bg-gray-900 rounded-lg border border-gray-700 p-4">
                <div className="flex items-center justify-between mb-3">
                  <h2 className="font-semibold text-lg">{selectedSchedule.name}</h2>
                  <div className="flex gap-2">
                    <button onClick={() => toggleStatus(selectedSchedule)}
                      className="flex items-center gap-1 text-xs px-2 py-1 rounded bg-gray-700 hover:bg-gray-600">
                      {selectedSchedule.status === 'ACTIVE'
                        ? <><ToggleRight className="w-3 h-3" /> {t('fees.deactivate')}</>
                        : <><ToggleLeft className="w-3 h-3" /> {t('fees.activate')}</>}
                    </button>
                    <button onClick={() => setShowRuleForm(true)}
                      className="flex items-center gap-1 text-xs px-2 py-1 rounded bg-indigo-700 hover:bg-indigo-600">
                      <Plus className="w-3 h-3" /> {t('fees.addRule')}
                    </button>
                  </div>
                </div>
                <div className="grid grid-cols-3 gap-3 text-sm">
                  <div><span className="text-gray-500">{t('fees.type')}</span> <p>{SCHEDULE_TYPE_LABELS[selectedSchedule.scheduleType] || selectedSchedule.scheduleType}</p></div>
                  <div><span className="text-gray-500">{t('fees.priority')}</span> <p>{selectedSchedule.priority}</p></div>
                  <div><span className="text-gray-500">{t('fees.appliesTo')}</span> <p>{APPLIES_TO_LABELS[selectedSchedule.appliesTo] || selectedSchedule.appliesTo || 'ALL'}</p></div>
                  <div><span className="text-gray-500">{t('fees.effectiveFrom')}</span> <p>{selectedSchedule.effectiveFrom}</p></div>
                  <div><span className="text-gray-500">{t('fees.effectiveUntil')}</span> <p>{selectedSchedule.effectiveUntil || '-'}</p></div>
                  <div><span className="text-gray-500">{t('fees.currency')}</span> <p>{selectedSchedule.currencyCode}</p></div>
                </div>
              </div>

              <div className="bg-gray-900 rounded-lg border border-gray-700 overflow-hidden">
                <div className="p-3 bg-gray-800 text-sm font-medium text-gray-300 flex items-center justify-between">
                  <span>{t('fees.rules')} ({rules.length})</span>
                </div>
                <table className="w-full text-sm">
                  <thead className="bg-gray-800 text-gray-400">
                    <tr>
                      <th className="text-left p-2">#</th>
                      <th className="text-left p-2">{t('fees.ruleName')}</th>
                      <th className="text-left p-2">{t('fees.method')}</th>
                      <th className="text-right p-2">{t('fees.rate')}</th>
                      <th className="text-right p-2">{t('fees.flat')}</th>
                      <th className="text-right p-2">{t('fees.min')}</th>
                      <th className="text-right p-2">{t('fees.max')}</th>
                      <th className="text-center p-2">{t('fees.actions')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rules.map(r => (
                      <tr key={r.id} className="border-t border-gray-700 hover:bg-gray-800/50">
                        <td className="p-2 text-gray-500">{r.ruleOrder}</td>
                        <td className="p-2">{r.ruleName}</td>
                        <td className="p-2 text-gray-400">{CALC_METHOD_LABELS[r.calcMethod] || r.calcMethod}</td>
                        <td className="p-2 text-right">{r.percentageRate != null ? `${r.percentageRate}%` : '-'}</td>
                        <td className="p-2 text-right">{r.flatAmount != null ? r.flatAmount.toLocaleString() : '-'}</td>
                        <td className="p-2 text-right">{r.minAmount != null ? r.minAmount.toLocaleString() : '-'}</td>
                        <td className="p-2 text-right">{r.maxAmount != null ? r.maxAmount.toLocaleString() : '-'}</td>
                        <td className="p-2 text-center">
                          <button onClick={async () => { await api.delete(`/fees/rules/${r.id}`); fetchRules(selectedSchedule.id); }}
                            className="text-red-400 hover:text-red-300">
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>

      {showForm && (
        <Modal onClose={() => setShowForm(false)}>
          <ScheduleForm onSave={() => { setShowForm(false); fetchSchedules(); }} />
        </Modal>
      )}

      {showRuleForm && selectedSchedule && (
        <Modal onClose={() => setShowRuleForm(false)}>
          <RuleForm scheduleId={selectedSchedule.id}
            onSave={() => { setShowRuleForm(false); fetchRules(selectedSchedule.id); }} />
        </Modal>
      )}
    </div>
  );
}

function Modal({ children, onClose }: { children: React.ReactNode; onClose: () => void }) {
  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-gray-900 border border-gray-700 rounded-lg p-6 w-full max-w-lg max-h-[80vh] overflow-y-auto"
        onClick={e => e.stopPropagation()}>
        {children}
      </div>
    </div>
  );
}

function ScheduleForm({ onSave }: { onSave: () => void }) {
  const { t } = useTranslation();
  const [form, setForm] = useState({
    name: '', description: '', scheduleType: 'PROCESSING',
    priority: 0, currencyCode: 'TND',
    effectiveFrom: '', effectiveUntil: '', appliesTo: 'ALL',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await api.post('/fees/schedules', {
      ...form,
      priority: Number(form.priority),
      status: 'DRAFT',
    });
    onSave();
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <h3 className="font-semibold text-lg mb-4">{t('fees.newSchedule')}</h3>
      <Input label={t('fees.name')} value={form.name} onChange={v => setForm(p => ({ ...p, name: v }))} required />
      <Input label={t('fees.description')} value={form.description} onChange={v => setForm(p => ({ ...p, description: v }))} />
      <Select label={t('fees.type')} value={form.scheduleType}
        options={['INTERCHANGE', 'SCHEME', 'PROCESSING', 'CROSS_BORDER', 'CURRENCY_CONVERSION', 'ATM', 'FIXED', 'COMPOSITE']}
        optionLabels={SCHEDULE_TYPE_LABELS}
        onChange={v => setForm(p => ({ ...p, scheduleType: v }))} />
      <div className="grid grid-cols-2 gap-3">
        <Input label={t('fees.priority')} type="number" value={String(form.priority)}
          onChange={v => setForm(p => ({ ...p, priority: parseInt(v) || 0 }))} />
        <Input label={t('fees.currency')} value={form.currencyCode}
          onChange={v => setForm(p => ({ ...p, currencyCode: v }))} />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <Input label={t('fees.effectiveFrom')} type="date" value={form.effectiveFrom}
          onChange={v => setForm(p => ({ ...p, effectiveFrom: v }))} required />
        <Input label={t('fees.effectiveUntil')} type="date" value={form.effectiveUntil}
          onChange={v => setForm(p => ({ ...p, effectiveUntil: v }))} />
      </div>
      <Select label={t('fees.appliesTo')} value={form.appliesTo}
        options={['ALL', 'ISSUER', 'ACQUIRER', 'MERCHANT', 'PARTICIPANT']}
        optionLabels={APPLIES_TO_LABELS}
        onChange={v => setForm(p => ({ ...p, appliesTo: v }))} />
      <div className="flex justify-end gap-2 pt-2">
        <button type="submit" className="px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm">
          {t('fees.create')}
        </button>
      </div>
    </form>
  );
}

function RuleForm({ scheduleId, onSave }: { scheduleId: string; onSave: () => void }) {
  const { t } = useTranslation();
  const [form, setForm] = useState({
    ruleName: '', ruleOrder: 0, calcMethod: 'FLAT',
    flatAmount: '', percentageRate: '', minAmount: '', maxAmount: '',
    brandFilter: '', cardTypeFilter: '', mccFilter: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await api.post(`/fees/schedules/${scheduleId}/rules`, {
      ...form,
      ruleOrder: Number(form.ruleOrder),
      flatAmount: form.flatAmount ? Number(form.flatAmount) : null,
      percentageRate: form.percentageRate ? Number(form.percentageRate) : null,
      minAmount: form.minAmount ? Number(form.minAmount) : null,
      maxAmount: form.maxAmount ? Number(form.maxAmount) : null,
    });
    onSave();
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <h3 className="font-semibold text-lg mb-4">{t('fees.addRule')}</h3>
      <Input label={t('fees.ruleName')} value={form.ruleName} onChange={v => setForm(p => ({ ...p, ruleName: v }))} required />
      <div className="grid grid-cols-2 gap-3">
        <Input label={t('fees.order')} type="number" value={String(form.ruleOrder)}
          onChange={v => setForm(p => ({ ...p, ruleOrder: parseInt(v) || 0 }))} />
        <Select label={t('fees.method')} value={form.calcMethod}
          options={['FLAT', 'PERCENTAGE', 'TIERED', 'MIXED', 'INTERCHANGE_LOOKUP']}
          optionLabels={CALC_METHOD_LABELS}
          onChange={v => setForm(p => ({ ...p, calcMethod: v }))} />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <Input label={t('fees.flat')} type="number" value={form.flatAmount}
          onChange={v => setForm(p => ({ ...p, flatAmount: v }))} />
        <Input label={t('fees.ratePct')} type="number" value={form.percentageRate}
          onChange={v => setForm(p => ({ ...p, percentageRate: v }))} />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <Input label={t('fees.min')} type="number" value={form.minAmount}
          onChange={v => setForm(p => ({ ...p, minAmount: v }))} />
        <Input label={t('fees.max')} type="number" value={form.maxAmount}
          onChange={v => setForm(p => ({ ...p, maxAmount: v }))} />
      </div>
      <div className="grid grid-cols-3 gap-3">
        <Input label={t('fees.brandFilter')} value={form.brandFilter}
          onChange={v => setForm(p => ({ ...p, brandFilter: v }))} />
        <Input label={t('fees.cardTypeFilter')} value={form.cardTypeFilter}
          onChange={v => setForm(p => ({ ...p, cardTypeFilter: v }))} />
        <Input label={t('fees.mccFilter')} value={form.mccFilter}
          onChange={v => setForm(p => ({ ...p, mccFilter: v }))} />
      </div>
      <div className="flex justify-end gap-2 pt-2">
        <button type="submit" className="px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm">
          {t('fees.addRule')}
        </button>
      </div>
    </form>
  );
}

function Input({ label, value, onChange, type = 'text', required }: {
  label: string; value: string; onChange: (v: string) => void; type?: string; required?: boolean;
}) {
  const id = useId();
  return (
    <div>
      <label htmlFor={id} className="block text-xs text-gray-400 mb-1">{label}</label>
      <input id={id} type={type} value={value} onChange={e => onChange(e.target.value)} required={required}
        className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-indigo-500" />
    </div>
  );
}

function Select({ label, value, options, optionLabels, onChange }: {
  label: string; value: string; options: string[]; optionLabels?: Record<string, string>; onChange: (v: string) => void;
}) {
  return (
    <div>
      <label className="block text-xs text-gray-400 mb-1">{label}</label>
      <select value={value} onChange={e => onChange(e.target.value)}
        className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-indigo-500">
        {options.map(o => <option key={o} value={o}>{optionLabels?.[o] || o}</option>)}
      </select>
    </div>
  );
}
