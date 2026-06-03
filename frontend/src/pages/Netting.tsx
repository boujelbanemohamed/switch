import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import { DollarSign, ArrowUpCircle, ArrowDownCircle, MinusCircle, RefreshCw } from 'lucide-react';

interface NettingSession {
  id: string;
  sessionDate: string;
  status: string;
  totalGrossAmount: number;
  totalNetAmount: number;
  nettingEfficiency: number;
  currencyCode: string;
}

interface Position {
  id: string;
  participantId: string;
  grossDebit: number;
  grossCredit: number;
  netPosition: number;
  positionType: string;
  settlementStatus: string;
  settlementReference: string | null;
}

export function Netting() {
  const { t } = useTranslation();
  const [session, setSession] = useState<NettingSession | null>(null);
  const [positions, setPositions] = useState<Position[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchLatest = async () => {
    setLoading(true);
    try {
      const { data: s } = await api.get('/netting/latest');
      if (s) {
        setSession(s);
        const { data: pos } = await api.get(`/netting/${s.id}/positions`);
        setPositions(pos || []);
      }
    } catch { /* empty */ } finally { setLoading(false); }
  };

  useEffect(() => { fetchLatest(); }, []);

  const handleCalculate = async () => {
    try {
      const { data: s } = await api.post('/netting/calculate');
      setSession(s);
      const { data: pos } = await api.get(`/netting/${s.id}/positions`);
      setPositions(pos || []);
    } catch (e) { console.error(e); }
  };

  const handleConfirm = async () => {
    if (!session) return;
    const { data: s } = await api.post(`/netting/${session.id}/confirm`);
    setSession(s);
  };

  const handleSettle = async () => {
    if (!session) return;
    const { data: s } = await api.post(`/netting/${session.id}/settle`);
    setSession(s);
    const { data: pos } = await api.get(`/netting/${session.id}/positions`);
    setPositions(pos || []);
  };

  const posIcon = (type: string) => {
    switch (type) {
      case 'CREDIT': return <ArrowUpCircle className="w-4 h-4 text-green-500" />;
      case 'DEBIT': return <ArrowDownCircle className="w-4 h-4 text-red-500" />;
      default: return <MinusCircle className="w-4 h-4 text-gray-500" />;
    }
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{t('netting.title')}</h1>
        <div className="flex gap-2">
          <button onClick={handleCalculate}
            className="flex items-center gap-1 px-3 py-1.5 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm">
            <DollarSign className="w-4 h-4" /> {t('netting.calculate')}
          </button>
          {session?.status === 'CALCULATED' && (
            <button onClick={handleConfirm}
              className="flex items-center gap-1 px-3 py-1.5 bg-green-600 text-white rounded hover:bg-green-700 text-sm">
              {t('netting.confirm')}
            </button>
          )}
          {session?.status === 'CONFIRMED' && (
            <button onClick={handleSettle}
              className="flex items-center gap-1 px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm">
              {t('netting.settle')}
            </button>
          )}
          <button onClick={fetchLatest}
            className="flex items-center gap-1 px-3 py-1.5 bg-gray-600 text-white rounded hover:bg-gray-700 text-sm">
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>

      {loading ? (
        <p className="text-gray-500">{t('common.loading')}</p>
      ) : !session ? (
        <div className="bg-gray-900 rounded-lg border border-gray-700 p-8 text-center text-gray-500">
          {t('netting.noSession')}
        </div>
      ) : (
        <>
          <div className="grid grid-cols-4 gap-4">
            <div className="bg-gray-900 rounded-lg border border-gray-700 p-4">
              <p className="text-gray-400 text-xs">{t('netting.sessionDate')}</p>
              <p className="text-lg font-semibold">{session.sessionDate}</p>
            </div>
            <div className="bg-gray-900 rounded-lg border border-gray-700 p-4">
              <p className="text-gray-400 text-xs">{t('netting.totalGross')}</p>
              <p className="text-lg font-semibold">{session.totalGrossAmount?.toLocaleString()} {session.currencyCode}</p>
            </div>
            <div className="bg-gray-900 rounded-lg border border-gray-700 p-4">
              <p className="text-gray-400 text-xs">{t('netting.totalNet')}</p>
              <p className="text-lg font-semibold">{session.totalNetAmount?.toLocaleString()} {session.currencyCode}</p>
            </div>
            <div className="bg-gray-900 rounded-lg border border-gray-700 p-4">
              <p className="text-gray-400 text-xs">{t('netting.efficiency')}</p>
              <p className="text-lg font-semibold">{session.nettingEfficiency ?? '-'}%</p>
            </div>
          </div>

          <div className="bg-gray-900 rounded-lg border border-gray-700 overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-800 text-gray-400">
                <tr>
                  <th className="text-left p-3">{t('netting.participant')}</th>
                  <th className="text-right p-3">{t('netting.grossCredit')}</th>
                  <th className="text-right p-3">{t('netting.grossDebit')}</th>
                  <th className="text-right p-3">{t('netting.netPosition')}</th>
                  <th className="text-center p-3">{t('netting.positionType')}</th>
                  <th className="text-center p-3">{t('netting.settlementStatus')}</th>
                </tr>
              </thead>
              <tbody>
                {positions.map(p => (
                  <tr key={p.id} className="border-t border-gray-700 hover:bg-gray-800/50">
                    <td className="p-3 font-mono text-xs">{p.participantId.slice(0, 8)}...</td>
                    <td className="p-3 text-right">{p.grossCredit.toLocaleString()}</td>
                    <td className="p-3 text-right">{p.grossDebit.toLocaleString()}</td>
                    <td className="p-3 text-right font-semibold">{p.netPosition.toLocaleString()}</td>
                    <td className="p-3 flex justify-center">{posIcon(p.positionType)}</td>
                    <td className="p-3 text-center">
                      <span className={`px-2 py-0.5 rounded text-xs ${
                        p.settlementStatus === 'SETTLED' ? 'bg-green-900/50 text-green-400' :
                        p.settlementStatus === 'PENDING' ? 'bg-yellow-900/50 text-yellow-400' :
                        'bg-gray-700 text-gray-400'
                      }`}>{p.settlementStatus}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
