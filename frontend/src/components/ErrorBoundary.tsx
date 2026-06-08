import { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          padding: 40, textAlign: 'center', maxWidth: 500, margin: '60px auto',
          background: 'var(--surface)', borderRadius: 12, border: '1px solid var(--border)',
        }}>
          <p style={{ fontSize: 40, marginBottom: 12 }}>⚠️</p>
          <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 8 }}>Une erreur est survenue</h2>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 16 }}>
            {this.state.error?.message}
          </p>
          <button onClick={() => window.location.reload()}
            style={{
              padding: '10px 20px', borderRadius: 8, border: 'none',
              background: 'var(--accent)', color: '#fff', fontWeight: 600, cursor: 'pointer',
            }}>
            Recharger la page
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
