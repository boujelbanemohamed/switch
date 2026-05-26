import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ChevronDown, ChevronRight, Info, Target, BookOpen } from 'lucide-react';

interface SectionHeaderProps {
  sectionKey: string;
}

export function SectionHeader({ sectionKey }: SectionHeaderProps) {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState(false);

  const description = t(`${sectionKey}.sectionDescription`, { defaultValue: '' });
  const role = t(`${sectionKey}.sectionRole`, { defaultValue: '' });
  const usage = t(`${sectionKey}.sectionUsage`, { defaultValue: '' });

  if (!description && !role && !usage) return null;

  return (
    <div style={{ marginBottom: 24 }}>
      <button
        onClick={() => setExpanded(!expanded)}
        style={{
          display: 'flex', alignItems: 'center', gap: 8,
          background: 'var(--surface)', border: '1px solid var(--border)',
          borderRadius: 10, padding: '8px 16px', width: '100%',
          cursor: 'pointer', color: 'var(--text)', fontSize: 13, fontWeight: 500,
          transition: 'border-color 0.15s',
        }}
        onMouseEnter={e => (e.currentTarget.style.borderColor = '#3b82f6')}
        onMouseLeave={e => (e.currentTarget.style.borderColor = 'var(--border)')}
      >
        {expanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
        <Info size={14} style={{ opacity: 0.6 }} />
        <span>{expanded ? t('sectionInfo.toggleHide') : t('sectionInfo.toggle')}</span>
      </button>

      {expanded && (
        <div style={{
          marginTop: 8, padding: 20,
          background: 'var(--surface)', border: '1px solid var(--border)',
          borderTop: 'none', borderRadius: '0 0 10px 10px',
          borderTopLeftRadius: 0, borderTopRightRadius: 0,
        }}>
          {description && (
            <div style={{ marginBottom: role || usage ? 16 : 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
                <Info size={14} style={{ color: '#3b82f6' }} />
                <span style={{ fontSize: 13, fontWeight: 600, color: '#3b82f6' }}>{t('sectionInfo.description')}</span>
              </div>
              <p style={{ fontSize: 13, lineHeight: 1.6, color: 'var(--text-secondary)', margin: 0 }}>{description}</p>
            </div>
          )}

          {role && (
            <div style={{ marginBottom: usage ? 16 : 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
                <Target size={14} style={{ color: '#a855f7' }} />
                <span style={{ fontSize: 13, fontWeight: 600, color: '#a855f7' }}>{t('sectionInfo.role')}</span>
              </div>
              <p style={{ fontSize: 13, lineHeight: 1.6, color: 'var(--text-secondary)', margin: 0 }}>{role}</p>
            </div>
          )}

          {usage && (
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
                <BookOpen size={14} style={{ color: '#22c55e' }} />
                <span style={{ fontSize: 13, fontWeight: 600, color: '#22c55e' }}>{t('sectionInfo.usageGuide')}</span>
              </div>
              <p style={{ fontSize: 13, lineHeight: 1.6, color: 'var(--text-secondary)', margin: 0 }}>{usage}</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
