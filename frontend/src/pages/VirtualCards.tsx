import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import { SectionHeader } from '../components/SectionHeader';
import {
  Plus, Play, Pause, XCircle, RefreshCw, CreditCard,
} from 'lucide-react';

interface VirtualCard {
  id: string;
  fundingCardId: string | null;
  cardProductId: string | null;
  cardholderId: string;
  externalId: string | null;
  panSuffix: string;
  expiryDate: string;
  status: string;
  usageType: string;
  nameOnCard: string | null;
  amountLimit: number | null;
  amountUsed: number;
  currencyCode: string;
  merchantLocked: string | null;
  maxTransactions: number | null;
  transactionCount: number;
  expiresAt: string | null;
  activatedAt: string | null;
  cancelReason: string | null;
}

export function VirtualCards() {
  const { t } = useTranslation();
  const [cards, setCards] = useState<VirtualCard[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<string>('ALL');
  const [showForm, setShowForm] = useState(false);

  const fetchCards = async (status?: string) => {
    setLoading(true);
    try {
      const url = status && status !== 'ALL'
        ? `/issuing/virtual-cards/by-status/${status}`
        : '/issuing/virtual-cards';
      const data = await api.get<VirtualCard[]>(url);
      setCards(data || []);
    } catch { setCards([]); } finally { setLoading(false); }
  };

  useEffect(() => { fetchCards(); }, []);

  const changeFilter = (s: string) => {
    setFilter(s);
    fetchCards(s);
  };

  const updateStatus = async (id: string, action: string) => {
    try {
      await api.post(`/issuing/virtual-cards/${id}/${action}`);
      fetchCards(filter);
    } catch (e) { console.error(e); }
  };

  const statusBadge = (s: string) => {
    const colors: Record<string, string> = {
      ACTIVE: 'bg-green-900/50 text-green-400',
      PENDING_ACTIVATION: 'bg-yellow-900/50 text-yellow-400',
      SUSPENDED: 'bg-orange-900/50 text-orange-400',
      CONSUMED: 'bg-blue-900/50 text-blue-400',
      EXPIRED: 'bg-gray-700 text-gray-400',
      CANCELLED: 'bg-red-900/50 text-red-400',
    };
    return <span className={`px-2 py-0.5 rounded text-xs ${colors[s] || 'bg-gray-700 text-gray-400'}`}>{s}</span>;
  };

  const usageIcon = (type: string) => {
    switch (type) {
      case 'SINGLE_USE': return <span className="text-xs bg-indigo-900/50 text-indigo-400 px-1.5 py-0.5 rounded">1x</span>;
      case 'MULTI_USE': return <span className="text-xs bg-blue-900/50 text-blue-400 px-1.5 py-0.5 rounded">Nx</span>;
      case 'RECURRING': return <span className="text-xs bg-purple-900/50 text-purple-400 px-1.5 py-0.5 rounded">R</span>;
      default: return null;
    }
  };

  const filters = ['ALL', 'ACTIVE', 'PENDING_ACTIVATION', 'SUSPENDED', 'CONSUMED', 'EXPIRED', 'CANCELLED'];

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{t('virtualCards.title')}</h1>
        <div className="flex gap-2">
          <button onClick={() => setShowForm(true)}
            className="flex items-center gap-1 px-3 py-1.5 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm">
            <Plus className="w-4 h-4" /> {t('virtualCards.create')}
          </button>
        </div>
      </div>

      <SectionHeader sectionKey="virtualCards" />

      <div className="flex gap-2">
        {filters.map(f => (
          <button key={f} onClick={() => changeFilter(f)}
            className={`text-xs px-2.5 py-1 rounded-full transition-colors ${
              filter === f ? 'bg-indigo-600 text-white' : 'bg-gray-800 text-gray-400 hover:bg-gray-700'
            }`}>
            {f === 'ALL' ? t('virtualCards.all') : f}
          </button>
        ))}
      </div>

      {loading ? (
        <p className="text-gray-500">{t('common.loading')}</p>
      ) : cards.length === 0 ? (
        <div className="bg-gray-900 rounded-lg border border-gray-700 p-8 text-center text-gray-500">
          {t('common.noData')}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {cards.map(c => (
            <div key={c.id} className="bg-gray-900 rounded-lg border border-gray-700 p-4 space-y-3 hover:border-gray-600 transition-colors">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <CreditCard className="w-4 h-4 text-indigo-400" />
                  <span className="font-mono text-sm">****{c.panSuffix}</span>
                  {usageIcon(c.usageType)}
                </div>
                {statusBadge(c.status)}
              </div>

              {c.nameOnCard && <p className="text-xs text-gray-400">{c.nameOnCard}</p>}

              <div className="grid grid-cols-2 gap-2 text-xs">
                <div>
                  <span className="text-gray-500">{t('virtualCards.expires')}</span>
                  <p>{c.expiryDate}</p>
                </div>
                <div>
                  <span className="text-gray-500">{t('virtualCards.limit')}</span>
                  <p>{c.amountLimit ? `${c.amountLimit.toLocaleString()} ${c.currencyCode}` : '-'}</p>
                </div>
                <div>
                  <span className="text-gray-500">{t('virtualCards.used')}</span>
                  <p>{c.amountUsed.toLocaleString()} {c.currencyCode}</p>
                </div>
                <div>
                  <span className="text-gray-500">{t('virtualCards.txns')}</span>
                  <p>{c.transactionCount}{c.maxTransactions ? ` / ${c.maxTransactions}` : ''}</p>
                </div>
              </div>

              {c.merchantLocked && (
                <div className="text-xs text-gray-500">
                  <span className="text-gray-500">{t('virtualCards.merchant')}:</span> {c.merchantLocked}
                </div>
              )}

              <div className="flex gap-1 pt-1 border-t border-gray-700">
                {c.status === 'PENDING_ACTIVATION' && (
                  <button onClick={() => updateStatus(c.id, 'activate')}
                    className="flex items-center gap-1 text-xs px-2 py-1 rounded bg-green-700 hover:bg-green-600">
                    <Play className="w-3 h-3" /> {t('virtualCards.activate')}
                  </button>
                )}
                {c.status === 'ACTIVE' && (
                  <button onClick={() => updateStatus(c.id, 'suspend')}
                    className="flex items-center gap-1 text-xs px-2 py-1 rounded bg-orange-700 hover:bg-orange-600">
                    <Pause className="w-3 h-3" /> {t('virtualCards.suspend')}
                  </button>
                )}
                {c.status === 'SUSPENDED' && (
                  <button onClick={() => updateStatus(c.id, 'resume')}
                    className="flex items-center gap-1 text-xs px-2 py-1 rounded bg-green-700 hover:bg-green-600">
                    <Play className="w-3 h-3" /> {t('virtualCards.resume')}
                  </button>
                )}
                {(c.status === 'ACTIVE' || c.status === 'SUSPENDED') && (
                  <button onClick={() => updateStatus(c.id, 'cancel')}
                    className="flex items-center gap-1 text-xs px-2 py-1 rounded bg-red-700 hover:bg-red-600">
                    <XCircle className="w-3 h-3" /> {t('virtualCards.cancel')}
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {showForm && (
        <Modal onClose={() => setShowForm(false)}>
          <CreateVirtualCardForm onSave={() => { setShowForm(false); fetchCards(filter); }} />
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

function CreateVirtualCardForm({ onSave }: { onSave: () => void }) {
  const { t } = useTranslation();
  const [form, setForm] = useState({
    cardholderId: '', fundingCardId: '', externalId: '',
    nameOnCard: '', usageType: 'MULTI_USE',
    amountLimit: '', currencyCode: 'TND',
    merchantLocked: '', maxTransactions: '',
    panSuffix: '0000', expiryDate: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await api.post('/issuing/virtual-cards', {
      ...form,
      panHash: 'placeholder_' + Date.now(),
      cvvHash: 'placeholder',
      expiryDate: form.expiryDate || '2030-12-31',
      amountLimit: form.amountLimit ? Number(form.amountLimit) : null,
      maxTransactions: form.maxTransactions ? Number(form.maxTransactions) : null,
    });
    onSave();
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <h3 className="font-semibold text-lg mb-4">{t('virtualCards.create')}</h3>
      <div className="grid grid-cols-2 gap-3">
        <Input label={t('virtualCards.cardholderId')} value={form.cardholderId}
          onChange={v => setForm(p => ({ ...p, cardholderId: v }))} required />
        <Input label={t('virtualCards.externalId')} value={form.externalId}
          onChange={v => setForm(p => ({ ...p, externalId: v }))} />
      </div>
      <Input label={t('virtualCards.nameOnCard')} value={form.nameOnCard}
        onChange={v => setForm(p => ({ ...p, nameOnCard: v }))} />
      <div className="grid grid-cols-2 gap-3">
        <Select label={t('virtualCards.usageType')} value={form.usageType}
          options={['SINGLE_USE', 'MULTI_USE', 'RECURRING']}
          onChange={v => setForm(p => ({ ...p, usageType: v }))} />
        <Input label={t('virtualCards.currency')} value={form.currencyCode}
          onChange={v => setForm(p => ({ ...p, currencyCode: v }))} />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <Input label={t('virtualCards.amountLimit')} type="number" value={form.amountLimit}
          onChange={v => setForm(p => ({ ...p, amountLimit: v }))} />
        <Input label={t('virtualCards.maxTransactions')} type="number" value={form.maxTransactions}
          onChange={v => setForm(p => ({ ...p, maxTransactions: v }))} />
      </div>
      <Input label={t('virtualCards.merchantLocked')} value={form.merchantLocked}
        onChange={v => setForm(p => ({ ...p, merchantLocked: v }))} />
      <div className="flex justify-end gap-2 pt-2">
        <button type="submit" className="px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm">
          {t('virtualCards.create')}
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

function Select({ label, value, options, onChange }: {
  label: string; value: string; options: string[]; onChange: (v: string) => void;
}) {
  return (
    <div>
      <label className="block text-xs text-gray-400 mb-1">{label}</label>
      <select value={value} onChange={e => onChange(e.target.value)}
        className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-indigo-500">
        {options.map(o => <option key={o} value={o}>{o}</option>)}
      </select>
    </div>
  );
}
