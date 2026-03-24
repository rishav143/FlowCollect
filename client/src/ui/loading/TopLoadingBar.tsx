import { useEffect, useRef, useState } from 'react'

/**
 * TopLoadingBar
 *
 * A fixed, 2px progress bar shown at the very top of the viewport.
 * Driven by the `active` prop — this component owns all animation logic.
 *
 * Lifecycle:
 *  active=true  → appear at 0%, animate to ~72% (slow, faked progress)
 *  active=false → snap to 100%, then fade out and reset
 *
 * To change the color: update the `bg-brand-blue` class only.
 * To change timing: adjust the transition strings in the style prop.
 */
export default function TopLoadingBar({ active }: { active: boolean }) {
  const [width, setWidth]     = useState(0)
  const [visible, setVisible] = useState(false)
  const hideTimer = useRef<ReturnType<typeof setTimeout>>()

  useEffect(() => {
    clearTimeout(hideTimer.current)

    if (active) {
      // Mount at 0%, then animate to target on next two frames
      // (double-rAF ensures the browser paints width:0 before starting transition)
      setVisible(true)
      setWidth(0)
      requestAnimationFrame(() =>
        requestAnimationFrame(() => setWidth(72))
      )
    } else {
      // Snap to 100% to signal completion, then hide
      setWidth(100)
      hideTimer.current = setTimeout(() => {
        setVisible(false)
        setWidth(0)
      }, 350)
    }

    return () => clearTimeout(hideTimer.current)
  }, [active])

  if (!visible) return null

  return (
    <div
      aria-hidden="true"
      className="fixed top-0 inset-x-0 z-[9999] h-[2px] pointer-events-none"
    >
      <div
        style={{
          width: `${width}%`,
          transition: width >= 100
            ? 'width 0.2s ease-out'
            : 'width 2.5s cubic-bezier(0.05, 0.8, 0.5, 1)',
        }}
        className="h-full bg-brand-blue"
      />
    </div>
  )
}
