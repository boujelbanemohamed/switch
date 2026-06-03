import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import { SectionHeader } from '../components/SectionHeader';
import type { AuthUser, RegisterRequest } from '../types';
import { Users as UsersIcon, Plus, Trash2, UserCog, AlertCircle } from 'lucide-react';

export function Users() {
  const { t } = useTranslation();
  const { user: currentUser, fetchUsers, updateUser, deleteUser, register } = useAuth();
  const [users, setUsers] = useState<AuthUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const data = await fetchUsers();
      setUsers(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm(t('backoffice.confirmDelete') || 'Delete this user?')) return;
    try {
      await deleteUser(id);
      setUsers(prev => prev.filter(u => u.id !== id));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed');
    }
  };

  const handleRoleChange = async (id: string, role: AuthUser['role']) => {
    try {
      const updated = await updateUser(id, { role } as Partial<AuthUser>);
      setUsers(prev => prev.map(u => u.id === id ? updated : u));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Update failed');
    }
  };

  const handleCreate = async (data: RegisterRequest) => {
    try {
      await register(data);
      setShowCreate(false);
      await loadUsers();
    } catch (err) {
      throw err;
    }
  };

  const roleColors: Record<string, string> = {
    ADMIN: '#ef4444',
    OPERATOR: '#f59e0b',
    ANALYST: '#3b82f6',
    AUDITOR: '#8b5cf6',
    VIEWER: '#6b7280',
    MERCHANT: '#22c55e',
  };

  const isAdmin = currentUser?.role === 'ADMIN';

  return (
    <div>
      <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24 }}>{t('auth.manageUsers')}</h2>
      <SectionHeader sectionKey="auth" />

      {error && (
        <div style={{
          display: 'flex', alignItems: 'center', gap: 8, padding: '10px 14px',
          background: 'rgba(239,68,68,0.1)', borderRadius: 8, margin: '12px 0 20px',
          fontSize: 13, color: '#ef4444',
        }}>
          <AlertCircle size={16} />
          {error}
        </div>
      )}

      {isAdmin && (
        <div style={{ marginTop: 16 }}>
          <button
            onClick={() => setShowCreate(true)}
            style={{
              display: 'flex', alignItems: 'center', gap: 6, padding: '8px 16px',
              borderRadius: 8, border: 'none', background: '#2563eb', color: '#fff',
              fontSize: 13, fontWeight: 600, cursor: 'pointer',
            }}
          >
            <Plus size={16} />
            {t('backoffice.createUser')}
          </button>
        </div>
      )}

      {showCreate && (
        <CreateUserForm
          onSave={handleCreate}
          onCancel={() => setShowCreate(false)}
        />
      )}

      {loading ? (
        <p style={{ color: 'var(--text-secondary)', padding: 20 }}>{t('common.loading')}</p>
      ) : (
        <div style={{ background: 'var(--surface)', borderRadius: 12, border: '1px solid var(--border)', overflow: 'hidden', marginTop: 24 }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)', background: 'var(--bg)' }}>
                <Th>{t('auth.username')}</Th>
                <Th>{t('auth.email')}</Th>
                <Th>{t('auth.role')}</Th>
                <Th>{t('backoffice.status')}</Th>
                <Th>{t('auth.lastLogin')}</Th>
                {isAdmin && <Th style={{ width: 140 }}>{t('common.actions')}</Th>}
              </tr>
            </thead>
            <tbody>
              {users.length === 0 ? (
                <tr>
                  <td colSpan={isAdmin ? 6 : 5} style={{ textAlign: 'center', padding: 32, color: 'var(--text-secondary)', fontSize: 14 }}>
                    {t('auth.noUsers')}
                  </td>
                </tr>
              ) : users.map(user => (
                <tr key={user.id} style={{ borderBottom: '1px solid var(--border)' }}>
                  <Td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div style={{
                        width: 32, height: 32, borderRadius: '50%',
                        background: 'linear-gradient(135deg, #60a5fa, #3b82f6)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontSize: 12, fontWeight: 700, color: '#fff',
                      }}>
                        {(user.displayName || user.username).charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <div style={{ fontSize: 14, fontWeight: 500, color: 'var(--text)' }}>
                          {user.displayName || user.username}
                        </div>
                        <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                          {user.username}
                        </div>
                      </div>
                    </div>
                  </Td>
                  <Td style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{user.email || '-'}</Td>
                  <Td>
                    {isAdmin ? (
                      <select
                        value={user.role}
                        onChange={e => handleRoleChange(user.id, e.target.value as AuthUser['role'])}
                        style={{
                          padding: '4px 8px', borderRadius: 6, border: '1px solid var(--border)',
                          background: 'var(--bg)', color: 'var(--text)', fontSize: 12, fontWeight: 600,
                          cursor: 'pointer',
                        }}
                      >
                        <option value="ADMIN">ADMIN</option>
                        <option value="OPERATOR">OPERATOR</option>
                        <option value="ANALYST">ANALYST</option>
                        <option value="AUDITOR">AUDITOR</option>
                        <option value="VIEWER">VIEWER</option>
                        <option value="MERCHANT">MERCHANT</option>
                      </select>
                    ) : (
                      <span style={{
                        display: 'inline-block', padding: '3px 8px', borderRadius: 6,
                        fontSize: 11, fontWeight: 600,
                        background: `${roleColors[user.role] || '#6b7280'}20`,
                        color: roleColors[user.role] || '#6b7280',
                      }}>
                        {user.role}
                      </span>
                    )}
                  </Td>
                  <Td>
                    <span style={{
                      display: 'inline-block', width: 8, height: 8, borderRadius: '50%',
                      background: user.enabled ? '#22c55e' : '#ef4444',
                      marginRight: 6,
                    }} />
                    {user.enabled ? t('backoffice.active') : t('backoffice.inactive')}
                  </Td>
                  <Td style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{user.lastLogin || '-'}</Td>
                  {isAdmin && (
                    <Td>
                      <button
                        onClick={() => handleDelete(user.id)}
                        disabled={user.username === currentUser?.username}
                        title={user.username === currentUser?.username ? t('backoffice.cannotDeleteSelf') : t('common.delete')}
                        style={{
                          padding: '6px 10px', borderRadius: 6, border: '1px solid var(--border)',
                          background: 'transparent', color: user.username === currentUser?.username ? '#6b7280' : '#ef4444',
                          cursor: user.username === currentUser?.username ? 'not-allowed' : 'pointer',
                          fontSize: 13,
                        }}
                      >
                        <Trash2 size={14} />
                      </button>
                    </Td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function CreateUserForm({ onSave, onCancel }: { onSave: (data: RegisterRequest) => Promise<void>; onCancel: () => void }) {
  const { t } = useTranslation();
  const [form, setForm] = useState<RegisterRequest>({ username: '', password: '', email: '', displayName: '', role: 'OPERATOR' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await onSave(form);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Creation failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{
      background: 'var(--surface)', borderRadius: 12, border: '1px solid var(--border)',
      padding: '1.5rem 2rem', marginTop: 20,
    }}>
      <h3 style={{ fontSize: 16, fontWeight: 600, color: 'var(--text)', marginBottom: 20, display: 'flex', alignItems: 'center', gap: 8 }}>
        <UserCog size={18} /> {t('backoffice.createUser')}
      </h3>

      {error && (
        <div style={{ padding: '8px 12px', background: 'rgba(239,68,68,0.1)', borderRadius: 8, marginBottom: 16, fontSize: 13, color: '#ef4444' }}>
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
          <div>
            <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 6 }}>{t('auth.username')}</label>
            <input value={form.username} onChange={e => setForm(f => ({ ...f, username: e.target.value }))} required
              style={{ width: '100%', padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 14, outline: 'none', boxSizing: 'border-box' }} />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 6 }}>{t('auth.email')}</label>
            <input type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} required
              style={{ width: '100%', padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 14, outline: 'none', boxSizing: 'border-box' }} />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 6 }}>{t('auth.displayName')}</label>
            <input value={form.displayName || ''} onChange={e => setForm(f => ({ ...f, displayName: e.target.value }))}
              style={{ width: '100%', padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 14, outline: 'none', boxSizing: 'border-box' }} />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 6 }}>{t('auth.password')}</label>
            <input type="password" value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} required
              style={{ width: '100%', padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 14, outline: 'none', boxSizing: 'border-box' }} />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 6 }}>{t('auth.role')}</label>
            <select value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value }))}
              style={{ width: '100%', padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text)', fontSize: 14, outline: 'none', boxSizing: 'border-box', cursor: 'pointer' }}>
              <option value="ADMIN">ADMIN</option>
              <option value="OPERATOR">OPERATOR</option>
              <option value="ANALYST">ANALYST</option>
              <option value="AUDITOR">AUDITOR</option>
              <option value="VIEWER">VIEWER</option>
              <option value="MERCHANT">MERCHANT</option>
            </select>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 10, marginTop: 24, justifyContent: 'flex-end' }}>
          <button type="button" onClick={onCancel}
            style={{ padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--text)', fontSize: 13, fontWeight: 600, cursor: 'pointer' }}>
            {t('common.cancel')}
          </button>
          <button type="submit" disabled={submitting}
            style={{ padding: '10px 20px', borderRadius: 8, border: 'none', background: submitting ? '#3b82f6' : '#2563eb', color: '#fff', fontSize: 13, fontWeight: 600, cursor: submitting ? 'not-allowed' : 'pointer', opacity: submitting ? 0.6 : 1 }}>
            {submitting ? t('common.loading') : t('common.create')}
          </button>
        </div>
      </form>
    </div>
  );
}

function Th({ children, style: extStyle }: { children: React.ReactNode; style?: React.CSSProperties }) {
  return (
    <th style={{ padding: '12px 16px', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: 0.5, textAlign: 'left', ...extStyle }}>
      {children}
    </th>
  );
}

function Td({ children, style: extStyle }: { children: React.ReactNode; style?: React.CSSProperties }) {
  return (
    <td style={{ fontSize: 14, color: 'var(--text)', ...extStyle }}>
      {children}
    </td>
  );
}
