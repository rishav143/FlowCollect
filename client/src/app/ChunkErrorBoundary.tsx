import { Component, type ReactNode } from 'react'

interface Props  { children: ReactNode }
interface State  { chunkError: boolean }

/**
 * Catches "Failed to fetch dynamically imported module" errors that happen
 * when a new Vercel deployment invalidates old chunk hashes while a user
 * still has the previous build loaded in their browser.
 *
 * On catch: hard-reload once so the browser picks up the new build.
 * A sessionStorage flag prevents an infinite reload loop if the error persists.
 */
export default class ChunkErrorBoundary extends Component<Props, State> {
  state: State = { chunkError: false }

  static getDerivedStateFromError(error: unknown): State | null {
    if (isChunkError(error)) return { chunkError: true }
    return null
  }

  componentDidCatch(error: unknown) {
    if (!isChunkError(error)) return

    const key = 'chunk_reload_attempted'
    if (sessionStorage.getItem(key)) return   // already tried once — stop loop

    sessionStorage.setItem(key, '1')
    window.location.reload()
  }

  render() {
    // While the reload is in-flight show nothing rather than a broken UI
    if (this.state.chunkError) return null
    return this.props.children
  }
}

function isChunkError(error: unknown): boolean {
  if (!(error instanceof Error)) return false
  const msg = error.message.toLowerCase()
  return (
    msg.includes('failed to fetch dynamically imported module') ||
    msg.includes('importing a module script failed') ||
    msg.includes('loading chunk') ||
    error.name === 'ChunkLoadError'
  )
}
