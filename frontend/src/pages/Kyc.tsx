import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../services/api';
import {
  FileText, CheckCircle, XCircle, Clock, Upload, Shield,
  RefreshCw, Plus, Search,
} from 'lucide-react';

interface KycDocument {
  id: string;
  cardholderId: string;
  documentType: string;
  documentNumber: string | null;
  issuingCountry: string | null;
  expiryDate: string | null;
  verificationStatus: string;
  verifiedBy: string | null;
  verifiedAt: string | null;
  rejectionReason: string | null;
  createdAt: string;
}

interface KycVerification {
  id: string;
  cardholderId: string;
  verificationType: string;
  status: string;
  requestedLevel: number;
  verifiedBy: string | null;
  verifiedAt: string | null;
  notes: string | null;
  rejectionReason: string | null;
}

interface Cardholder {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  kycLevel: number | null;
  status: string;
}

export function Kyc() {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState<'documents' | 'verifications' | 'cardholders'>('documents');
  const [documents, setDocuments] = useState<KycDocument[]>([]);
  const [verifications, setVerifications] = useState<KycVerification[]>([]);
  const [cardholders, setCardholders] = useState<Cardholder[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedDocument, setSelectedDocument] = useState<KycDocument | null>(null);
  const [showUploadForm, setShowUploadForm] = useState(false);
  const [showVerifyForm, setShowVerifyForm] = useState(false);
  const [verifyTarget, setVerifyTarget] = useState<{ type: 'doc' | 'ver'; id: string } | null>(null);

  useEffect(() => {
    if (activeTab === 'documents') fetchDocuments();
    else if (activeTab === 'verifications') fetchVerifications();
    else fetchCardholders();
  }, [activeTab]);

  const fetchDocuments = async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/kyc/documents');
      setDocuments(data || []);
    } catch { setDocuments([]); } finally { setLoading(false); }
  };

  const fetchVerifications = async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/kyc/verifications');
      setVerifications(data || []);
    } catch { setVerifications([]); } finally { setLoading(false); }
  };

  const fetchCardholders = async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/issuing/cardholders');
      setCardholders(data || []);
    } catch { setCardholders([]); } finally { setLoading(false); }
  };

  const verifyDoc = async (approved: boolean) => {
    if (!verifyTarget) return;
    await api.post(`/kyc/documents/${verifyTarget.id}/verify?approved=${approved}`);
    setShowVerifyForm(false);
    setVerifyTarget(null);
    fetchDocuments();
  };

  const completeVerification = async (id: string, approved: boolean) => {
    const verifiedBy = 'operator';
    await api.post(`/kyc/verifications/${id}/complete?approved=${approved}&verifiedBy=${verifiedBy}`);
    fetchVerifications();
  };

  const statusBadge = (s: string) => {
    const colors: Record<string, string> = {
      VERIFIED: 'bg-green-900/50 text-green-400',
      ACTIVE: 'bg-green-900/50 text-green-400',
      PENDING: 'bg-yellow-900/50 text-yellow-400',
      IN_PROGRESS: 'bg-blue-900/50 text-blue-400',
      REJECTED: 'bg-red-900/50 text-red-400',
      EXPIRED: 'bg-gray-700 text-gray-400',
      DRAFT: 'bg-yellow-900/50 text-yellow-400',
      INACTIVE: 'bg-gray-700 text-gray-400',
    };
    return <span className={`px-2 py-0.5 rounded text-xs ${colors[s] || 'bg-gray-700 text-gray-400'}`}>{s}</span>;
  };

  const tabs = [
    { id: 'documents', label: t('kyc.documents'), icon: FileText },
    { id: 'verifications', label: t('kyc.verifications'), icon: Shield },
    { id: 'cardholders', label: t('kyc.cardholders'), icon: Search },
  ] as const;

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{t('kyc.title')}</h1>
        {activeTab === 'documents' && (
          <button onClick={() => setShowUploadForm(true)}
            className="flex items-center gap-1 px-3 py-1.5 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm">
            <Upload className="w-4 h-4" /> {t('kyc.uploadDocument')}
          </button>
        )}
      </div>

      <div className="flex gap-1 bg-gray-800 rounded-lg p-1 w-fit">
        {tabs.map(t => (
          <button key={t.id} onClick={() => setActiveTab(t.id)}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded text-sm transition-colors ${
              activeTab === t.id ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:text-white'
            }`}>
            <t.icon className="w-4 h-4" />
            {t.label}
          </button>
        ))}
      </div>

      {loading ? (
        <p className="text-gray-500">{t('common.loading')}</p>
      ) : activeTab === 'documents' ? (
        <div className="bg-gray-900 rounded-lg border border-gray-700 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-800 text-gray-400">
              <tr>
                <th className="text-left p-3">{t('kyc.documentType')}</th>
                <th className="text-left p-3">{t('kyc.documentNumber')}</th>
                <th className="text-center p-3">{t('kyc.status')}</th>
                <th className="text-right p-3">{t('kyc.verifiedBy')}</th>
                <th className="text-right p-3">{t('kyc.createdAt')}</th>
                <th className="text-center p-3">{t('kyc.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {documents.length === 0 ? (
                <tr><td colSpan={6} className="text-center p-4 text-gray-500">{t('common.noData')}</td></tr>
              ) : documents.map(d => (
                <tr key={d.id} className="border-t border-gray-700 hover:bg-gray-800/50">
                  <td className="p-3">{d.documentType}</td>
                  <td className="p-3 text-gray-400">{d.documentNumber || '-'}</td>
                  <td className="p-3 text-center">{statusBadge(d.verificationStatus)}</td>
                  <td className="p-3 text-right text-gray-400">{d.verifiedBy || '-'}</td>
                  <td className="p-3 text-right text-gray-400">{new Date(d.createdAt).toLocaleDateString()}</td>
                  <td className="p-3 text-center">
                    {d.verificationStatus === 'PENDING' && (
                      <button onClick={() => { setVerifyTarget({ type: 'doc', id: d.id }); setShowVerifyForm(true); }}
                        className="text-xs px-2 py-1 rounded bg-gray-700 hover:bg-gray-600">
                        {t('kyc.review')}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : activeTab === 'verifications' ? (
        <div className="bg-gray-900 rounded-lg border border-gray-700 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-800 text-gray-400">
              <tr>
                <th className="text-left p-3">{t('kyc.verificationType')}</th>
                <th className="text-center p-3">{t('kyc.requestedLevel')}</th>
                <th className="text-center p-3">{t('kyc.status')}</th>
                <th className="text-right p-3">{t('kyc.verifiedBy')}</th>
                <th className="text-right p-3">{t('kyc.createdAt')}</th>
                <th className="text-center p-3">{t('kyc.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {verifications.length === 0 ? (
                <tr><td colSpan={6} className="text-center p-4 text-gray-500">{t('common.noData')}</td></tr>
              ) : verifications.map(v => (
                <tr key={v.id} className="border-t border-gray-700 hover:bg-gray-800/50">
                  <td className="p-3">{v.verificationType}</td>
                  <td className="p-3 text-center">{t('kyc.level')} {v.requestedLevel}</td>
                  <td className="p-3 text-center">{statusBadge(v.status)}</td>
                  <td className="p-3 text-right text-gray-400">{v.verifiedBy || '-'}</td>
                  <td className="p-3 text-right text-gray-400">{new Date(v.createdAt).toLocaleDateString()}</td>
                  <td className="p-3 text-center">
                    {v.status === 'PENDING' || v.status === 'IN_PROGRESS' ? (
                      <div className="flex gap-1 justify-center">
                        <button onClick={() => completeVerification(v.id, true)}
                          className="text-xs px-2 py-1 rounded bg-green-700 hover:bg-green-600">
                          <CheckCircle className="w-3 h-3 inline" />
                        </button>
                        <button onClick={() => completeVerification(v.id, false)}
                          className="text-xs px-2 py-1 rounded bg-red-700 hover:bg-red-600">
                          <XCircle className="w-3 h-3 inline" />
                        </button>
                      </div>
                    ) : v.rejectionReason && (
                      <span className="text-xs text-red-400">{v.rejectionReason}</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="bg-gray-900 rounded-lg border border-gray-700 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-800 text-gray-400">
              <tr>
                <th className="text-left p-3">{t('kyc.name')}</th>
                <th className="text-left p-3">{t('kyc.email')}</th>
                <th className="text-center p-3">{t('kyc.kycLevel')}</th>
                <th className="text-center p-3">{t('kyc.status')}</th>
                <th className="text-center p-3">{t('kyc.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {cardholders.length === 0 ? (
                <tr><td colSpan={5} className="text-center p-4 text-gray-500">{t('common.noData')}</td></tr>
              ) : cardholders.map(ch => (
                <tr key={ch.id} className="border-t border-gray-700 hover:bg-gray-800/50">
                  <td className="p-3">{ch.firstName} {ch.lastName}</td>
                  <td className="p-3 text-gray-400">{ch.email}</td>
                  <td className="p-3 text-center">
                    <span className={`px-2 py-0.5 rounded text-xs font-mono ${
                      (ch.kycLevel ?? 0) >= 3 ? 'bg-green-900/50 text-green-400' :
                      (ch.kycLevel ?? 0) >= 1 ? 'bg-yellow-900/50 text-yellow-400' :
                      'bg-gray-700 text-gray-400'
                    }`}>L{ch.kycLevel ?? 0}</span>
                  </td>
                  <td className="p-3 text-center">{statusBadge(ch.status)}</td>
                  <td className="p-3 text-center">
                    <button onClick={async () => {
                      await api.get(`/kyc/documents?cardholderId=${ch.id}`).then(({ data }) => setDocuments(data || []));
                      setActiveTab('documents');
                    }}
                      className="text-xs px-2 py-1 rounded bg-gray-700 hover:bg-gray-600">
                      {t('kyc.viewDocs')}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showVerifyForm && verifyTarget && (
        <Modal onClose={() => { setShowVerifyForm(false); setVerifyTarget(null); }}>
          <div className="space-y-4">
            <h3 className="font-semibold text-lg">{t('kyc.reviewDocument')}</h3>
            <p className="text-sm text-gray-400">{t('kyc.reviewPrompt')}</p>
            <div className="flex gap-3">
              <button onClick={() => verifyDoc(true)}
                className="flex-1 px-4 py-2 bg-green-700 text-white rounded hover:bg-green-600">
                <CheckCircle className="w-4 h-4 inline mr-1" /> {t('kyc.approve')}
              </button>
              <button onClick={() => verifyDoc(false)}
                className="flex-1 px-4 py-2 bg-red-700 text-white rounded hover:bg-red-600">
                <XCircle className="w-4 h-4 inline mr-1" /> {t('kyc.reject')}
              </button>
            </div>
          </div>
        </Modal>
      )}

      {showUploadForm && (
        <Modal onClose={() => setShowUploadForm(false)}>
          <UploadForm onSave={() => { setShowUploadForm(false); fetchDocuments(); }} />
        </Modal>
      )}
    </div>
  );
}

function Modal({ children, onClose }: { children: React.ReactNode; onClose: () => void }) {
  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-gray-900 border border-gray-700 rounded-lg p-6 w-full max-w-md"
        onClick={e => e.stopPropagation()}>
        {children}
      </div>
    </div>
  );
}

function UploadForm({ onSave }: { onSave: () => void }) {
  const { t } = useTranslation();
  const [form, setForm] = useState({
    cardholderId: '', documentType: 'PASSPORT', documentNumber: '',
    issuingCountry: 'TN', expiryDate: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await api.post('/kyc/documents', form);
    onSave();
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <h3 className="font-semibold text-lg mb-4">{t('kyc.uploadDocument')}</h3>
      <Input label={t('kyc.cardholderId')} value={form.cardholderId}
        onChange={v => setForm(p => ({ ...p, cardholderId: v }))} required />
      <div className="grid grid-cols-2 gap-3">
        <Select label={t('kyc.documentType')} value={form.documentType}
          options={['PASSPORT', 'NATIONAL_ID', 'DRIVING_LICENSE', 'RESIDENCE', 'PROOF_OF_ADDRESS', 'INCOME_STATEMENT', 'BANK_STATEMENT']}
          onChange={v => setForm(p => ({ ...p, documentType: v }))} />
        <Input label={t('kyc.issuingCountry')} value={form.issuingCountry}
          onChange={v => setForm(p => ({ ...p, issuingCountry: v }))} />
      </div>
      <Input label={t('kyc.documentNumber')} value={form.documentNumber}
        onChange={v => setForm(p => ({ ...p, documentNumber: v }))} />
      <Input label={t('kyc.expiryDate')} type="date" value={form.expiryDate}
        onChange={v => setForm(p => ({ ...p, expiryDate: v }))} />
      <div className="flex justify-end gap-2 pt-2">
        <button type="submit" className="px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm">
          {t('kyc.upload')}
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
