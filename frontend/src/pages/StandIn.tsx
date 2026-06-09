import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import { SectionHeader } from '../components/SectionHeader';
import { StandInHelp, STANDIN_DECISION_LABELS, STANDIN_REASON_LABELS } from '../components/StandInHelp';
import { Plus, Pencil, Trash2, RefreshCw } from 'lucide-react';

interface StandInRuleItem {
  id: string;
  issuerParticipantId: string | null;
  cardBrand: string;
  enabled: boolean;
  maxAmount: number;
  dailyCountLimit: number;
  dailyAmountLimit: number;
  allowedMcc: string;
  declineIfNoRule: boolean;
  createdAt: string;
}

interface StandInAuthorizationItem {
  id: string;
  transactionId: string;
  cardSuffix: string | null;
  issuerParticipantId: string | null;
  amount: number;
  currencyCode: string;
  decision: 'APPROVED' | 'DECLINED';
  reason: string | null;
  reconciled: boolean;
  authorizedAt: string;
}

export function StandIn() {
  const { t } = useTranslation();
  const [rules, setRules] = useState<StandInRuleItem[]>([]);
  const [auths, setAuths] = useState<StandInAuthorizationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [editId, setEditId] = useState<string | null>(null);
  const [editForm, setEditForm] = useState<Partial<StandInRuleItem>>({});
  const [showCreate, setShowCreate] = useState(false);
  const [pendingCount, setPendingCount] = useState(0);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [rulesData, authsData, countData] = await Promise.all([
        api.standin.rules.list(),
        api.standin.authorizations.list(),
        api.standin.pendingCount(),
      ]);
      setRules(Array.isArray(rulesData) ? rulesData : []);
      setAuths(Array.isArray(authsData) ? authsData : []);
      setPendingCount(countData?.count ?? 0);
    } catch {
      setRules([]);
      setAuths([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const startEdit = (rule: StandInRuleItem) => {
    setEditId(rule.id);
    setEditForm({ ...rule });
  };

  const cancelEdit = () => {
    setEditId(null);
    setEditForm({});
  };

  const saveEdit = async (id: string) => {
    await api.standin.rules.update(id, editForm);
    setEditId(null);
    setEditForm({});
    fetchData();
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm(t('standIn.deleteConfirm'))) return;
    await api.standin.rules.delete(id);
    fetchData();
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold">{t('standIn.title')}</h1>
            <StandInHelp />
          </div>
          {pendingCount > 0 && (
            <p className="text-sm text-amber-400 mt-1">
              {pendingCount} {t('standIn.pendingReconciliation')}
            </p>
          )}
        </div>
        <div className="flex gap-2">
          <button onClick={fetchData}
            className="flex items-center gap-1 px-3 py-1.5 bg-gray-700 text-white rounded hover:bg-gray-600 text-sm">
            <RefreshCw className="w-4 h-4" /> {t('common.refresh')}
          </button>
          <button onClick={() => setShowCreate(true)}
            className="flex items-center gap-1 px-3 py-1.5 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm">
            <Plus className="w-4 h-4" /> {t('standIn.newRule')}
          </button>
        </div>
      </div>

      <SectionHeader sectionKey="standIn" />

      <div className="bg-gray-900 rounded-lg border border-gray-700 overflow-hidden">
        <div className="px-4 py-2 bg-gray-800 text-gray-400 font-semibold text-sm border-b border-gray-700">
          {t('standIn.rules')}
        </div>
        <table className="w-full text-sm">
          <thead className="bg-gray-800 text-gray-400">
            <tr>
              <th className="text-left p-3">{t('standIn.issuer')}</th>
              <th className="text-left p-3">{t('standIn.cardBrand')}</th>
              <th className="text-center p-3">{t('standIn.enabled')}</th>
              <th className="text-right p-3">{t('standIn.maxAmount')}</th>
              <th className="text-right p-3">{t('standIn.dailyCount')}</th>
              <th className="text-right p-3">{t('standIn.dailyAmount')}</th>
              <th className="text-center p-3">{t('standIn.actions')}</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="p-4 text-center text-gray-500">{t('common.loading')}</td></tr>
            ) : rules.length === 0 ? (
              <tr><td colSpan={7} className="p-4 text-center text-gray-500">{t('common.noData')}</td></tr>
            ) : rules.map(rule => (
              <tr key={rule.id} className="border-t border-gray-700 hover:bg-gray-800/50">
                {editId === rule.id ? (
                  <>
                    <td className="p-2">
                      <input value={editForm.issuerParticipantId || ''}
                        onChange={e => setEditForm(p => ({ ...p, issuerParticipantId: e.target.value || null }))}
                        className="w-full bg-gray-800 border border-gray-700 rounded px-2 py-1 text-sm" />
                    </td>
                    <td className="p-2">
                      <input value={editForm.cardBrand || ''}
                        onChange={e => setEditForm(p => ({ ...p, cardBrand: e.target.value }))}
                        className="w-full bg-gray-800 border border-gray-700 rounded px-2 py-1 text-sm" />
                    </td>
                    <td className="p-2 text-center">
                      <input type="checkbox" checked={editForm.enabled ?? true}
                        onChange={e => setEditForm(p => ({ ...p, enabled: e.target.checked }))}
                        className="accent-indigo-500" />
                    </td>
                    <td className="p-2">
                      <input type="number" step="0.001" value={editForm.maxAmount ?? ''}
                        onChange={e => setEditForm(p => ({ ...p, maxAmount: parseFloat(e.target.value) || 0 }))}
                        className="w-full bg-gray-800 border border-gray-700 rounded px-2 py-1 text-sm text-right" />
                    </td>
                    <td className="p-2">
                      <input type="number" value={editForm.dailyCountLimit ?? ''}
                        onChange={e => setEditForm(p => ({ ...p, dailyCountLimit: parseInt(e.target.value) || 0 }))}
                        className="w-full bg-gray-800 border border-gray-700 rounded px-2 py-1 text-sm text-right" />
                    </td>
                    <td className="p-2">
                      <input type="number" step="0.001" value={editForm.dailyAmountLimit ?? ''}
                        onChange={e => setEditForm(p => ({ ...p, dailyAmountLimit: parseFloat(e.target.value) || 0 }))}
                        className="w-full bg-gray-800 border border-gray-700 rounded px-2 py-1 text-sm text-right" />
                    </td>
                    <td className="p-2 text-center">
                      <div className="flex justify-center gap-1">
                        <button onClick={() => saveEdit(rule.id)}
                          className="text-green-400 hover:text-green-300 px-1">{t('common.save')}</button>
                        <button onClick={cancelEdit}
                          className="text-gray-400 hover:text-gray-300 px-1">{t('common.cancel')}</button>
                      </div>
                    </td>
                  </>
                ) : (
                  <>
                    <td className="p-3 font-medium">{rule.issuerParticipantId || t('standIn.global')}</td>
                    <td className="p-3">{rule.cardBrand}</td>
                    <td className="p-3 text-center">
                      <span className={`px-2 py-0.5 rounded text-xs font-medium ${rule.enabled ? 'bg-green-900 text-green-300' : 'bg-red-900 text-red-300'}`}>
                        {rule.enabled ? t('common.yes') : t('common.no')}
                      </span>
                    </td>
                    <td className="p-3 text-right">{rule.maxAmount.toFixed(3)}</td>
                    <td className="p-3 text-right">{rule.dailyCountLimit}</td>
                    <td className="p-3 text-right">{rule.dailyAmountLimit.toFixed(3)}</td>
                    <td className="p-3 text-center">
                      <div className="flex justify-center gap-2">
                        <button onClick={() => startEdit(rule)}
                          className="text-blue-400 hover:text-blue-300">
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button onClick={() => handleDelete(rule.id)}
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

      <div className="bg-gray-900 rounded-lg border border-gray-700 overflow-hidden">
        <div className="px-4 py-2 bg-gray-800 text-gray-400 font-semibold text-sm border-b border-gray-700">
          {t('standIn.authorizations')} {pendingCount > 0 && <span className="text-amber-400">({pendingCount} {t('standIn.pending').toLowerCase()})</span>}
        </div>
        <table className="w-full text-sm">
          <thead className="bg-gray-800 text-gray-400">
            <tr>
              <th className="text-left p-3">{t('standIn.transactionId')}</th>
              <th className="text-left p-3">{t('standIn.cardSuffix')}</th>
              <th className="text-right p-3">{t('standIn.amount')}</th>
              <th className="text-left p-3">{t('standIn.currency')}</th>
              <th className="text-center p-3">{t('standIn.decision')}</th>
              <th className="text-left p-3">{t('standIn.reason')}</th>
              <th className="text-center p-3">{t('standIn.reconciled')}</th>
              <th className="text-left p-3">{t('standIn.date')}</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={8} className="p-4 text-center text-gray-500">{t('common.loading')}</td></tr>
            ) : auths.length === 0 ? (
              <tr><td colSpan={8} className="p-4 text-center text-gray-500">{t('common.noData')}</td></tr>
            ) : auths.map(auth => (
              <tr key={auth.id} className="border-t border-gray-700 hover:bg-gray-800/50">
                <td className="p-3 text-xs font-mono">{auth.transactionId}</td>
                <td className="p-3">{auth.cardSuffix || '-'}</td>
                <td className="p-3 text-right">{auth.amount.toFixed(3)}</td>
                <td className="p-3">{auth.currencyCode}</td>
                <td className="p-3 text-center">
                  <span className={`px-2 py-0.5 rounded text-xs font-medium ${auth.decision === 'APPROVED' ? 'bg-green-900 text-green-300' : 'bg-red-900 text-red-300'}`}>
                    {STANDIN_DECISION_LABELS[auth.decision] ?? auth.decision}
                  </span>
                </td>
                <td className="p-3 text-gray-400 text-xs">{(STANDIN_REASON_LABELS[auth.reason ?? ''] ?? auth.reason) || '-'}</td>
                <td className="p-3 text-center">
                  {auth.reconciled ? (
                    <span className="text-green-400 text-xs">{t('common.yes')}</span>
                  ) : (
                    <span className="text-amber-400 text-xs">{t('common.no')}</span>
                  )}
                </td>
                <td className="p-3 text-xs text-gray-400">{new Date(auth.authorizedAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showCreate && (
        <CreateModal onClose={() => setShowCreate(false)} onSaved={() => { setShowCreate(false); fetchData(); }} />
      )}
    </div>
  );
}

function CreateModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const { t } = useTranslation();
  const [form, setForm] = useState({
    issuerParticipantId: '',
    cardBrand: 'ALL',
    enabled: true,
    maxAmount: '1000',
    dailyCountLimit: '5',
    dailyAmountLimit: '5000',
    allowedMcc: '*',
    declineIfNoRule: true,
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await api.standin.rules.create({
      issuerParticipantId: form.issuerParticipantId || null,
      cardBrand: form.cardBrand,
      enabled: form.enabled,
      maxAmount: parseFloat(form.maxAmount) || 0,
      dailyCountLimit: parseInt(form.dailyCountLimit) || 5,
      dailyAmountLimit: parseFloat(form.dailyAmountLimit) || 0,
      allowedMcc: form.allowedMcc,
      declineIfNoRule: form.declineIfNoRule,
    });
    onSaved();
  };

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-gray-900 border border-gray-700 rounded-lg p-6 w-full max-w-md" onClick={e => e.stopPropagation()}>
        <form onSubmit={handleSubmit} className="space-y-3">
          <h3 className="font-semibold text-lg mb-4">{t('standIn.newRule')}</h3>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-gray-400 mb-1">{t('standIn.issuer')}</label>
              <input value={form.issuerParticipantId}
                onChange={e => setForm(p => ({ ...p, issuerParticipantId: e.target.value }))}
                placeholder="UUID (vide = global)"
                className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm" />
            </div>
            <div>
              <label className="block text-xs text-gray-400 mb-1">{t('standIn.cardBrand')}</label>
              <input value={form.cardBrand} onChange={e => setForm(p => ({ ...p, cardBrand: e.target.value }))}
                className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-gray-400 mb-1">{t('standIn.maxAmount')}</label>
              <input type="number" step="0.001" value={form.maxAmount}
                onChange={e => setForm(p => ({ ...p, maxAmount: e.target.value }))}
                className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm" />
            </div>
            <div>
              <label className="block text-xs text-gray-400 mb-1">{t('standIn.dailyCount')}</label>
              <input type="number" value={form.dailyCountLimit}
                onChange={e => setForm(p => ({ ...p, dailyCountLimit: e.target.value }))}
                className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-gray-400 mb-1">{t('standIn.dailyAmount')}</label>
              <input type="number" step="0.001" value={form.dailyAmountLimit}
                onChange={e => setForm(p => ({ ...p, dailyAmountLimit: e.target.value }))}
                className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm" />
            </div>
            <div>
              <label className="block text-xs text-gray-400 mb-1">{t('standIn.allowedMcc')}</label>
              <input value={form.allowedMcc} onChange={e => setForm(p => ({ ...p, allowedMcc: e.target.value }))}
                className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-1.5 text-sm" />
            </div>
          </div>
          <div className="flex items-center gap-4">
            <label className="flex items-center gap-2 text-sm text-gray-300">
              <input type="checkbox" checked={form.enabled}
                onChange={e => setForm(p => ({ ...p, enabled: e.target.checked }))}
                className="accent-indigo-500" />
              {t('standIn.enabled')}
            </label>
            <label className="flex items-center gap-2 text-sm text-gray-300">
              <input type="checkbox" checked={form.declineIfNoRule}
                onChange={e => setForm(p => ({ ...p, declineIfNoRule: e.target.checked }))}
                className="accent-indigo-500" />
              {t('standIn.declineIfNoRule')}
            </label>
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
