import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import {
  Clock, Play, CheckCircle, XCircle, RefreshCw,
} from 'lucide-react';

interface BatchJob {
  id: string;
  jobName: string;
  jobType: string;
  status: string;
  scheduledAt: string;
  startedAt: string | null;
  completedAt: string | null;
  recordsProcessed: number | null;
  recordsFailed: number | null;
  errorMessage: string | null;
  triggeredBy: string | null;
}

export function Batch() {
  const { t } = useTranslation();
  const [jobs, setJobs] = useState<BatchJob[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchJobs = async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/batch/history');
      setJobs(data || []);
    } catch {
      setJobs([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchJobs(); }, []);

  const triggerJob = async (endpoint: string) => {
    try {
      await api.post(`/batch/${endpoint}`);
      fetchJobs();
    } catch (e) {
      console.error('Failed to trigger job', e);
    }
  };

  const statusIcon = (s: string) => {
    switch (s) {
      case 'COMPLETED': return <CheckCircle className="w-4 h-4 text-green-500" />;
      case 'FAILED': return <XCircle className="w-4 h-4 text-red-500" />;
      case 'RUNNING': return <RefreshCw className="w-4 h-4 text-blue-500 animate-spin" />;
      default: return <Clock className="w-4 h-4 text-yellow-500" />;
    }
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{t('batch.title')}</h1>
        <div className="flex gap-2">
          <button onClick={() => triggerJob('eod')}
            className="flex items-center gap-1 px-3 py-1.5 bg-orange-600 text-white rounded hover:bg-orange-700 text-sm">
            <Play className="w-4 h-4" /> EOD
          </button>
          <button onClick={() => triggerJob('bod')}
            className="flex items-center gap-1 px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm">
            <Play className="w-4 h-4" /> BOD
          </button>
          <button onClick={fetchJobs}
            className="flex items-center gap-1 px-3 py-1.5 bg-gray-600 text-white rounded hover:bg-gray-700 text-sm">
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>

      <div className="bg-gray-900 rounded-lg border border-gray-700 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-800 text-gray-400">
            <tr>
              <th className="text-left p-3">{t('batch.jobName')}</th>
              <th className="text-left p-3">{t('batch.type')}</th>
              <th className="text-center p-3">{t('batch.status')}</th>
              <th className="text-right p-3">{t('batch.processed')}</th>
              <th className="text-right p-3">{t('batch.failed')}</th>
              <th className="text-right p-3">{t('batch.scheduledAt')}</th>
              <th className="text-right p-3">{t('batch.completedAt')}</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="text-center p-4 text-gray-500">{t('common.loading')}</td></tr>
            ) : jobs.length === 0 ? (
              <tr><td colSpan={7} className="text-center p-4 text-gray-500">{t('common.noData')}</td></tr>
            ) : jobs.map(j => (
              <tr key={j.id} className="border-t border-gray-700 hover:bg-gray-800/50">
                <td className="p-3">{j.jobName}</td>
                <td className="p-3 text-gray-400">{j.jobType}</td>
                <td className="p-3 flex justify-center">{statusIcon(j.status)}</td>
                <td className="p-3 text-right">{j.recordsProcessed ?? '-'}</td>
                <td className="p-3 text-right">{j.recordsFailed ?? '-'}</td>
                <td className="p-3 text-right text-gray-400">{new Date(j.scheduledAt).toLocaleString()}</td>
                <td className="p-3 text-right text-gray-400">
                  {j.completedAt ? new Date(j.completedAt).toLocaleString() : '-'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {jobs.some(j => j.status === 'FAILED') && (
          <div className="p-3 bg-red-900/20 border-t border-red-800">
            {jobs.filter(j => j.status === 'FAILED').map(j => (
              <p key={j.id} className="text-red-400 text-xs">{j.jobName}: {j.errorMessage}</p>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
