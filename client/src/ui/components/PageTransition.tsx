import { useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'

/**
 * PageTransition
 *
 * Wraps page content with a subtle fade-in animation on every route change.
 * Animation timing and style lives in index.css (.page-enter).
 *
 * Centralised here so AppShell stays unaware of animation details.
 * To change the animation: edit index.css only — no component changes needed.
 */
export default function PageTransition({ children }: { children: ReactNode }) {
  const { pathname } = useLocation()
  return (
    <div key={pathname} className="page-enter">
      {children}
    </div>
  )
}
