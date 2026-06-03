import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import { SectionHeader } from '../components/SectionHeader';
import {
  Plus, Pencil, Trash2, ToggleLeft, ToggleRight,
  CreditCard, RefreshCw, ChevronDown, ChevronRight,
} from 'lucide-react';

interface CardProgram {
  id: string;
  name: string;
  description: string | null;
  programType: string;
  status: string;
  brand: string | null;
  startDate: string | null;
  endDate: string | null;
}

interface CardProduct {
  id: string;
  programId: string;
  name: string;
  description: string | null;
  productCode: string;
  cardType: string;
  cardBrand: string;
  cardNetwork: string | null;
  status: string;
  contactlessEnabled: boolean;
  onlineEnabled: boolean;
  internationalEnabled: boolean;
  ecommerceEnabled: boolean;
  atmEnabled: boolean;
  isRenewable: boolean;
  isVirtualSupported: boolean;
  dailyLimit: number | null;
  weeklyLimit: number | null;
  monthlyLimit: number | null;
  singleTxnLimit: number | null;
  annualFee: number | null;
  currencyCode: string;
}

export function CardPrograms() {
  const { t } = useTranslation();
  const [programs, setPrograms] = useState<CardProgram[]>([]);
  const [expandedProgram, setExpandedProgram] = useState<string | null>(null);
  const [products, setProducts] = useState<Record<string, CardProduct[]>>({});
  const [loading, setLoading] = useState(true);
  const [showProgramForm, setShowProgramForm] = useState(false);
  const [showProductForm, setShowProductForm] = useState(false);
  const [selectedProgram, setSelectedProgram] = useState<string | null>(null);

  const fetchPrograms = async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/issuing/programs');
      setPrograms(data || []);
    } catch { setPrograms([]); } finally { setLoading(false); }
  };

  const fetchProducts = async (programId: string) => {
    try {
      const { data } = await api.get(`/issuing/programs/${programId}/products`);
      setProducts(p => ({ ...p, [programId]: data || [] }));
    } catch { setProducts(p => ({ ...p, [programId]: [] })); }
  };

  useEffect(() => { fetchPrograms(); }, []);

  const toggleExpand = (id: string) => {
    if (expandedProgram === id) {
      setExpandedProgram(null);
    } else {
      setExpandedProgram(id);
      if (!products[id]) fetchProducts(id);
    }
  };

  const toggleProgramStatus = async (p: CardProgram) => {
    const ep = p.status === 'ACTIVE' ? 'deactivate' : 'activate';
    await api.post(`/issuing/programs/${p.id}/${ep}`);
    fetchPrograms();
  };

  const toggleProductStatus = async (p: CardProduct) => {
    const ep = p.status === 'ACTIVE' ? 'deactivate' : 'activate';
    await api.post(`/issuing/programs/products/${p.id}/${ep}`);
    if (expandedProgram) fetchProducts(expandedProgram);
  };

  const statusBadge = (s: string) => {
    const colors: Record<string, string> = {
      ACTIVE: 'bg-green-900/50 text-green-400',
      DRAFT: 'bg-yellow-900/50 text-yellow-400',
      INACTIVE: 'bg-gray-700 text-gray-400',
      ARCHIVED: 'bg-red-900/50 text-red-400',
    };
    return <span className={`px-2 py-0.5 rounded text-xs ${colors[s] || 'bg-gray-700 text-gray-400'}`}>{s}</span>;
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{t('cardPrograms.title')}</h1>
        <div className="flex gap-2">
          <button onClick={() => { setSelectedProgram(null); setShowProductForm(true); }}
            className="flex items-center gap-1 px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm">
            <CreditCard className="w-4 h-4" /> {t('cardPrograms.newProduct')}
          </button>
          <button onClick={() => setShowProgramForm(true)}
            className="flex items-center gap-1 px-3 py-1.5 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm">
            <Plus className="w-4 h-4" /> {t('cardPrograms.newProgram')}
          </button>
        </div>
      </div>

      <SectionHeader sectionKey="cardPrograms" />

      {loading ? (
        <p className="text-gray-500">{t('common.loading')}</p>
      ) : programs.length === 0 ? (
        <div className="bg-gray-900 rounded-lg border border-gray-700 p-8 text-center text-gray-500">
          {t('common.noData')}
        </div>
      ) : (
        <div className="space-y-3">
          {programs.map(p => (
            <div key={p.id} className="bg-gray-900 rounded-lg border border-gray-700">
              <div className="flex items-center justify-between p-4 cursor-pointer hover:bg-gray-800/50"
                onClick={() => toggleExpand(p.id)}>
                <div className="flex items-center gap-3">
                  {expandedProgram === p.id ? <ChevronDown className="w-4 h-4 text-gray-400" /> : <ChevronRight className="w-4 h-4 text-gray-400" />}
                  <div>
                    <p className="font-medium">{p.name}</p>
                    <p className="text-xs text-gray-500">{p.programType} {p.brand ? `· ${p.brand}` : ''}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {statusBadge(p.status)}
                  <button onClick={e => { e.stopPropagation(); toggleProgramStatus(p); }}
                    className="text-xs px-2 py-1 rounded bg-gray-700 hover:bg-gray-600">
                    {p.status === 'ACTIVE' ? <ToggleRight className="w-3 h-3" /> : <ToggleLeft className="w-3 h-3" />}
                  </button>
                  <button onClick={e => { e.stopPropagation(); setSelectedProgram(p.id); setShowProductForm(true); }}
                    className="text-xs px-2 py-1 rounded bg-gray-700 hover:bg-gray-600">
                    <Plus className="w-3 h-3" />
                  </button>
                </div>
              </div>

              {expandedProgram === p.id && (
                <div className="border-t border-gray-700">
                  {(!products[p.id] || products[p.id].length === 0) ? (
                    <div className="p-4 text-center text-gray-500 text-sm">{t('common.noData')}</div>
                  ) : (
                    <table className="w-full text-sm">
                      <thead className="bg-gray-800 text-gray-400">
                        <tr>
                          <th className="text-left p-2">{t('cardPrograms.productName')}</th>
                          <th className="text-left p-2">{t('cardPrograms.code')}</th>
                          <th className="text-left p-2">{t('cardPrograms.type')}</th>
                          <th className="text-left p-2">{t('cardPrograms.brand')}</th>
                          <th className="text-center p-2">{t('cardPrograms.status')}</th>
                          <th className="text-right p-2">{t('cardPrograms.annualFee')}</th>
                          <th className="text-center p-2">{t('cardPrograms.actions')}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {products[p.id]?.map(pr => (
                          <tr key={pr.id} className="border-t border-gray-700 hover:bg-gray-800/50">
                            <td className="p-2">{pr.name}</td>
                            <td className="p-2 text-gray-400 font-mono text-xs">{pr.productCode}</td>
                            <td className="p-2">{pr.cardType}</td>
                            <td className="p-2">{pr.cardBrand}</td>
                            <td className="p-2 text-center">{statusBadge(pr.status)}</td>
                            <td className="p-2 text-right">{pr.annualFee ? `${pr.annualFee} ${pr.currencyCode}` : '-'}</td>
                            <td className="p-2 text-center">
                              <button onClick={() => toggleProductStatus(pr)}
                                className="text-xs px-1.5 py-0.5 rounded bg-gray-700 hover:bg-gray-600 mr-1">
                                {pr.status === 'ACTIVE' ? <ToggleRight className="w-3 h-3" /> : <ToggleLeft className="w-3 h-3" />}
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {showProgramForm && (
        <Modal onClose={() => setShowProgramForm(false)}>
          <ProgramForm onSave={() => { setShowProgramForm(false); fetchPrograms(); }} />
        </Modal>
      )}

      {showProductForm && (
        <Modal onClose={() => setShowProductForm(false)}>
          <ProductForm programId={selectedProgram}
            onSave={() => { setShowProductForm(false); if (expandedProgram) fetchProducts(expandedProgram); fetchPrograms(); }} />
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

function ProgramForm({ onSave }: { onSave: () => void }) {
  const { t } = useTranslation();
  const [form, setForm] = useState({
    name: '', description: '', programType: 'CONSUMER', brand: '',
    startDate: '', endDate: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await api.post('/issuing/programs', form);
    onSave();
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <h3 className="font-semibold text-lg mb-4">{t('cardPrograms.newProgram')}</h3>
      <Input label={t('cardPrograms.name')} value={form.name} onChange={v => setForm(p => ({ ...p, name: v }))} required />
      <Input label={t('cardPrograms.description')} value={form.description} onChange={v => setForm(p => ({ ...p, description: v }))} />
      <div className="grid grid-cols-2 gap-3">
        <Select label={t('cardPrograms.programType')} value={form.programType}
          options={['CONSUMER', 'CORPORATE', 'STUDENT', 'PREMIUM', 'PLATINUM', 'SIGNATURE', 'BUSINESS', 'CLASSIC']}
          onChange={v => setForm(p => ({ ...p, programType: v }))} />
        <Input label={t('cardPrograms.brand')} value={form.brand} onChange={v => setForm(p => ({ ...p, brand: v }))} />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <Input label={t('cardPrograms.startDate')} type="date" value={form.startDate} onChange={v => setForm(p => ({ ...p, startDate: v }))} />
        <Input label={t('cardPrograms.endDate')} type="date" value={form.endDate} onChange={v => setForm(p => ({ ...p, endDate: v }))} />
      </div>
      <div className="flex justify-end gap-2 pt-2">
        <button type="submit" className="px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm">
          {t('cardPrograms.create')}
        </button>
      </div>
    </form>
  );
}

function ProductForm({ programId, onSave }: { programId: string | null; onSave: () => void }) {
  const { t } = useTranslation();
  const [programs, setPrograms] = useState<CardProgram[]>([]);
  const [form, setForm] = useState({
    programId: programId || '',
    name: '', description: '', productCode: '',
    cardType: 'DEBIT', cardBrand: 'VISA', cardNetwork: 'VISA_NET',
    currencyCode: 'TND', annualFee: '0',
    dailyLimit: '', weeklyLimit: '', monthlyLimit: '', singleTxnLimit: '',
    contactlessEnabled: true, onlineEnabled: true, internationalEnabled: false,
    ecommerceEnabled: true, atmEnabled: true, isRenewable: true, isReissuable: true, isVirtualSupported: true,
  });

  useEffect(() => {
    api.get('/issuing/programs').then(({ data }) => setPrograms(data || [])).catch(() => {});
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await api.post(`/issuing/programs/${form.programId}/products`, {
      ...form,
      annualFee: Number(form.annualFee),
      dailyLimit: form.dailyLimit ? Number(form.dailyLimit) : null,
      weeklyLimit: form.weeklyLimit ? Number(form.weeklyLimit) : null,
      monthlyLimit: form.monthlyLimit ? Number(form.monthlyLimit) : null,
      singleTxnLimit: form.singleTxnLimit ? Number(form.singleTxnLimit) : null,
    });
    onSave();
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <h3 className="font-semibold text-lg mb-4">{t('cardPrograms.newProduct')}</h3>
      <Select label={t('cardPrograms.program')} value={form.programId}
        options={programs.map(p => p.id)}
        optionLabels={programs.reduce((acc, p) => ({ ...acc, [p.id]: p.name }), {} as Record<string, string>)}
        onChange={v => setForm(p => ({ ...p, programId: v }))} />
      <div className="grid grid-cols-2 gap-3">
        <Input label={t('cardPrograms.productName')} value={form.name} onChange={v => setForm(p => ({ ...p, name: v }))} required />
        <Input label={t('cardPrograms.code')} value={form.productCode} onChange={v => setForm(p => ({ ...p, productCode: v }))} required />
      </div>
      <Input label={t('cardPrograms.description')} value={form.description} onChange={v => setForm(p => ({ ...p, description: v }))} />
      <div className="grid grid-cols-3 gap-3">
        <Select label={t('cardPrograms.type')} value={form.cardType}
          options={['DEBIT', 'CREDIT', 'PREPAID', 'CHARGE', 'VIRTUAL']}
          onChange={v => setForm(p => ({ ...p, cardType: v }))} />
        <Select label={t('cardPrograms.brand')} value={form.cardBrand}
          options={['VISA', 'MASTERCARD', 'AMEX', 'CB', 'VERVE', 'OTHER']}
          onChange={v => setForm(p => ({ ...p, cardBrand: v }))} />
        <Select label={t('cardPrograms.network')} value={form.cardNetwork}
          options={['VISA_NET', 'MASTERCARD_NET', 'CB_NET', 'AMEX_NET', 'VERVE_NET']}
          onChange={v => setForm(p => ({ ...p, cardNetwork: v }))} />
      </div>
      <div className="grid grid-cols-3 gap-3">
        <div className="flex items-center gap-2">
          <input type="checkbox" checked={form.contactlessEnabled}
            onChange={e => setForm(p => ({ ...p, contactlessEnabled: e.target.checked }))}
            className="rounded bg-gray-800 border-gray-600" />
          <span className="text-xs">{t('cardPrograms.contactless')}</span>
        </div>
        <div className="flex items-center gap-2">
          <input type="checkbox" checked={form.onlineEnabled}
            onChange={e => setForm(p => ({ ...p, onlineEnabled: e.target.checked }))}
            className="rounded bg-gray-800 border-gray-600" />
          <span className="text-xs">{t('cardPrograms.online')}</span>
        </div>
        <div className="flex items-center gap-2">
          <input type="checkbox" checked={form.ecommerceEnabled}
            onChange={e => setForm(p => ({ ...p, ecommerceEnabled: e.target.checked }))}
            className="rounded bg-gray-800 border-gray-600" />
          <span className="text-xs">{t('cardPrograms.ecommerce')}</span>
        </div>
        <div className="flex items-center gap-2">
          <input type="checkbox" checked={form.atmEnabled}
            onChange={e => setForm(p => ({ ...p, atmEnabled: e.target.checked }))}
            className="rounded bg-gray-800 border-gray-600" />
          <span className="text-xs">{t('cardPrograms.atm')}</span>
        </div>
        <div className="flex items-center gap-2">
          <input type="checkbox" checked={form.internationalEnabled}
            onChange={e => setForm(p => ({ ...p, internationalEnabled: e.target.checked }))}
            className="rounded bg-gray-800 border-gray-600" />
          <span className="text-xs">{t('cardPrograms.international')}</span>
        </div>
        <div className="flex items-center gap-2">
          <input type="checkbox" checked={form.isRenewable}
            onChange={e => setForm(p => ({ ...p, isRenewable: e.target.checked }))}
            className="rounded bg-gray-800 border-gray-600" />
          <span className="text-xs">{t('cardPrograms.renewable')}</span>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <Input label={t('cardPrograms.annualFee')} type="number" value={form.annualFee}
          onChange={v => setForm(p => ({ ...p, annualFee: v }))} />
        <Input label={t('cardPrograms.currency')} value={form.currencyCode}
          onChange={v => setForm(p => ({ ...p, currencyCode: v }))} />
      </div>
      <p className="text-xs text-gray-500 font-medium">{t('cardPrograms.defaultLimits')}</p>
      <div className="grid grid-cols-2 gap-3">
        <Input label={t('cardPrograms.dailyLimit')} type="number" value={form.dailyLimit}
          onChange={v => setForm(p => ({ ...p, dailyLimit: v }))} />
        <Input label={t('cardPrograms.weeklyLimit')} type="number" value={form.weeklyLimit}
          onChange={v => setForm(p => ({ ...p, weeklyLimit: v }))} />
        <Input label={t('cardPrograms.monthlyLimit')} type="number" value={form.monthlyLimit}
          onChange={v => setForm(p => ({ ...p, monthlyLimit: v }))} />
        <Input label={t('cardPrograms.singleTxnLimit')} type="number" value={form.singleTxnLimit}
          onChange={v => setForm(p => ({ ...p, singleTxnLimit: v }))} />
      </div>
      <div className="flex justify-end gap-2 pt-2">
        <button type="submit" className="px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm">
          {t('cardPrograms.create')}
        </button>
      </div>
    </form>
  );
}

function Input({ label, value, onChange, type = 'text', required }: {
  label: string; value: string; onChange: (v: string) => void; type?: string; required?: boolean;
}) {
  return (
    <div>
      <label className="block text-xs text-gray-400 mb-1">{label}</label>
      <input type={type} value={value} onChange={e => onChange(e.target.value)} required={required}
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
