import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import { SectionHeader } from '../components/SectionHeader';
import { CARD_BRAND_LABELS, CARD_TYPE_LABELS } from '../components/BinTablesHelp';
import { InterchangeFeesHelp, REGION_LABELS } from '../components/InterchangeFeesHelp';
import { Plus, Pencil, Trash2, RefreshCw } from 'lucide-react';

interface InterchangeFee {
  id: string;
  brand: string;
  cardType: string;
  region: string;
  mcc: string;
  flatFee: number;
  percentageFee: number;
}

export function InterchangeFees() {
  const { t } = useTranslation();
  const [fees, setFees] = useState<InterchangeFee[]>([]);
  const [loading, setLoading] = useState(true);
  const [editId, setEditId] = useState<string | null>(null);
  const [editForm, setEditForm] = useState<Partial<InterchangeFee>>({});
  const [showCreate, setShowCreate] = useState(false);

  const fetchFees = async () => {
    setLoading(true);
    try {
      const data = await api.clearing.interchange.list();
      setFees(Array.isArray(data) ? data : []);
    } catch {
      setFees([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchFees(); }, []);

  const startEdit = (fee: InterchangeFee) => {
    setEditId(fee.id);
    setEditForm({ ...fee });
  };

  const cancelEdit = () => {
    setEditId(null);
    setEditForm({});
  };

  const saveEdit = async (id: string) => {
    await api.clearing.interchange.update(id, editForm);
    setEditId(null);
    setEditForm({});
    fetchFees();
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm(t('interchange.deleteConfirm'))) return;
    await api.clearing.interchange.delete(id);
    fetchFees();
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <h1 className="text-2xl font-bold">{t('interchange.title')}</h1>
          <InterchangeFeesHelp />
        </div>
        <div className="flex gap-2">
          <button onClick={fetchFees}
            className="flex items-center gap-1 px-3 py-1.5 bg-gray-700 text-white rounded hover:bg-gray-600 text-sm">
            <RefreshCw className="w-4 h-4" /> {t('common.refresh')}
          </button>
          <button onClick={() => setShowCreate(true)}
            className="flex items-center gap-1 px-3 py-1.5 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm">
            <Plus className="w-4 h-4" /> {t('interchange.newFee')}
          </button>
        </div>
      </div>

      <SectionHeader sectionKey="interchange" />

      <div className="bg-gray-900 rounded-lg border border-gray-700 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-800 text-gray-400">
            <tr>
              <th className="text-left p-3">{t('interchange.brand')}</th>
              <th className="text-left p-3">{t('interchange.cardType')}</th>
              <th className="text-left p-3">{t('interchange.region')}</th>
              <th className="text-left p-3">{t('interchange.mcc')}</th>
              <th className="text-right p-3">{t('interchange.flatFee')}</th>
              <th className="text-right p-3">{t('interchange.percentageFee')}</th>
              <th className="text-center p-3">{t('interchange.actions')}</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="p-4 text-center text-gray-500">{t('common.loading')}</td></tr>
            ) : fees.length === 0 ? (
              <tr><td colSpan={7} className="p-4 text-center text-gray-500">{t('common.noData')}</td></tr>
            ) : fees.map(fee => (
              <tr key={fee.id} className="border-t border-gray-700 hover:bg-gray-800/50">
                {editId === fee.id ? (
                  <>
                    <td className="p-2">
                      <select value={editForm.brand || ''} onChange={e => setEditForm(p => ({ ...p, brand: e.target.value }))}
                        className="w-full bg-gray-800 border border-gray-700 rounded px-2 py-1 text-sm">
                        <option value="">-- Brand --</option>
                        {Object.entries(CARD_BRAND_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                      </select>
                    </td>
                    <td className="p-2">
                      <select value={editForm.cardType || ''} onChange={e => setEditForm(p => ({ ...p, cardType: e.target.value }))}
                        className="w-full bg-gray-800 border border-gray-700 rounded px-2 py-1 text-sm">
                        <option value="">-- Type --</option>
                        {Object.entries(CARD_TYPE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                      </select>
                    </td>
                    <td className="p-2">
                      <select value={editForm.region || ''} onChange={e => setEditForm(p => ({ ...p, region: e.target.value }))}
                        className="w-full bg-gray-800 border border-gray-700 rounded px-2 py-1 text-sm">
                        <option value="">-- Region --</option>
                        {Object.entries(REGION_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                      </select>
                    </td>
                    <td className="p-2">
                      <input value={editForm.mcc || ''} onChange={e => setEditForm(p => ({ ...p, mcc: e.target.value }))}
                        className="w-full bg-gray-800 border border-gray-700 rounded px-2 py-1 text-sm" />
                    </td>
                    <td className="p-2">
                      <input type="number" step="0.001" value={editForm.flatFee ?? ''}
                        onChange={e => setEditForm(p => ({ ...p, flatFee: parseFloat(e.target.value) || 0 }))}
                        className="w-full bg-gray-800 border border-gray-700 rounded px-2 py-1 text-sm text-right" />
                    </td>
                    <td className="p-2">
                      <input type="number" step="0.0001" value={editForm.percentageFee ?? ''}
                        onChange={e => setEditForm(p => ({ ...p, percentageFee: parseFloat(e.target.value) || 0 }))}
                        className="w-full bg-gray-800 border border-gray-700 rounded px-2 py-1 text-sm text-right" />
                    </td>
                    <td className="p-2 text-center">
                      <div className="flex justify-center gap-1">
                        <button onClick={() => saveEdit(fee.id)}
                          className="text-green-400 hover:text-green-300 px-1">
                          {t('common.save')}
                        </button>
                        <button onClick={cancelEdit}
                          className="text-gray-400 hover:text-gray-300 px-1">
                          {t('common.cancel')}
                        </button>
                      </div>
                    </td>
                  </>
                ) : (
                  <>
                    <td className="p-3 font-medium">{CARD_BRAND_LABELS[fee.brand] || fee.brand}</td>
                    <td className="p-3">{CARD_TYPE_LABELS[fee.cardType] || fee.cardType}</td>
                    <td className="p-3">{REGION_LABELS[fee.region] || fee.region}</td>
                    <td className="p-3 text-gray-400">{fee.mcc}</td>
                    <td className="p-3 text-right">{fee.flatFee.toFixed(3)}</td>
                    <td className="p-3 text-right">{fee.percentageFee.toFixed(4)}%</td>
                    <td className="p-3 text-center">
                      <div className="flex justify-center gap-2">
                        <button onClick={() => startEdit(fee)}
                          className="text-blue-400 hover:text-blue-300">
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button onClick={() => handleDelete(fee.id)}
                          className="text-red-400 hover:text-red-300">
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showCreate && (
        <CreateModal onClose={() => setShowCreate(false)} onSaved={() => { setShowCreate(false); fetchFees(); }} />
      )}
    </div>
  );
}

function CreateModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const { t } = useTranslation();
  const [form, setForm] = useState({
    brand: '', cardType: '', region: '', mcc: '',
    flatFee: '', percentageFee: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await api.post('/clearing/interchange/configure', {
      brand: form.brand,
      cardType: form.cardType,
      region: form.region,
      mcc: form.mcc || '*',
      flatFee: form.flatFee || '0',
      percentageFee: form.percentageFee || '0',
    });
    onSaved();
  };

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-gray-900 border border-gray-700 rounded-lg p-6 w-full max-w-md" onClick={e => e.stopPropagation()}>
        <form onSubmit={handleSubmit} className="space-y-3">
          <h3 className="font-semibold text-lg mb-4">{t('interchange.newFee')}</h3>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-gray-400 mb-1">{t('interchange.brand')}</label>
              <select required value={form.brand} onChange={e => setForm(p => ({ ...p, brand: e.target.value }))}
                className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm">
                <option value="">-- Brand --</option>
                {Object.entries(CARD_BRAND_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs text-gray-400 mb-1">{t('interchange.cardType')}</label>
              <select required value={form.cardType} onChange={e => setForm(p => ({ ...p, cardType: e.target.value }))}
                className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm">
                <option value="">-- Type --</option>
                {Object.entries(CARD_TYPE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-gray-400 mb-1">{t('interchange.region')}</label>
              <select required value={form.region} onChange={e => setForm(p => ({ ...p, region: e.target.value }))}
                className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm">
                <option value="">-- Region --</option>
                {Object.entries(REGION_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs text-gray-400 mb-1">{t('interchange.mcc')}</label>
              <input value={form.mcc} onChange={e => setForm(p => ({ ...p, mcc: e.target.value }))}
                placeholder="*"
                className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-gray-400 mb-1">{t('interchange.flatFee')}</label>
              <input type="number" step="0.001" value={form.flatFee}
                onChange={e => setForm(p => ({ ...p, flatFee: e.target.value }))}
                className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm" />
            </div>
            <div>
              <label className="block text-xs text-gray-400 mb-1">{t('interchange.percentageFee')}</label>
              <input type="number" step="0.0001" value={form.percentageFee}
                onChange={e => setForm(p => ({ ...p, percentageFee: e.target.value }))}
                className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm" />
            </div>
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose}
              className="px-4 py-2 bg-gray-700 text-white rounded hover:bg-gray-600 text-sm">
              {t('common.cancel')}
            </button>
            <button type="submit"
              className="px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm">
              {t('common.create')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
