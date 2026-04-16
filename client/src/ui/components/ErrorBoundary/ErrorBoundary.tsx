import { Component, type ErrorInfo, type ReactNode } from 'react'
import { RotateCcw, AlertTriangle } from 'lucide-react'

interface Props { children: ReactNode }
interface State { hasError: boolean }

/**
 * Top-level React error boundary.
 * Catches unexpected render / lifecycle errors that are NOT chunk-load errors
 * (ChunkErrorBoundary handles those). Shows a friendly fallback instead of
 * a blank page or raw exception dump.
 */
export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // Keep the real error accessible for debugging without exposing it to users.
    console.error('[ErrorBoundary] Uncaught error:', error, info.componentStack)
  }

  render() {
    if (this.state.hasError) {
      return <ErrorFallback />
    }
    return this.props.children
  }
}

function ErrorFallback() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-[#F4F7F9] dark:bg-[#0D1B2A] px-4">
      <div className="text-center max-w-xs">
        <div className="w-14 h-14 rounded-full bg-red-100 dark:bg-red-500/10 flex items-center justify-center mx-auto mb-5">
          <AlertTriangle size={24} className="text-red-500" strokeWidth={1.5} />
        </div>
        <h1 className="text-lg font-semibold text-[#0D1B2A] dark:text-white mb-2">
          Something went wrong
        </h1>
        <p className="text-sm text-c-muted mb-6">
          An unexpected error occurred. Reload the page to continue.
        </p>
        <button
          onClick={() => window.location.reload()}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          <RotateCcw size={14} strokeWidth={2.5} />
          Reload page
        </button>
      </div>
    </div>
  )
}
