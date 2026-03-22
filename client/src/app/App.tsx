import { useEffect } from 'react'
import Providers from './Providers'
import Router    from './Router'
import { useUIStore } from '@/store/ui.store'

function ThemeApplier() {
  const theme = useUIStore((s) => s.theme)
  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
  }, [theme])
  return null
}

export default function App() {
  return (
    <Providers>
      <ThemeApplier />
      <Router />
    </Providers>
  )
}
