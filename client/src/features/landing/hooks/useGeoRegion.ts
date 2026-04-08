import { useEffect, useState } from 'react'

export type Region = 'IN' | 'GLOBAL' | 'loading'

export function useGeoRegion(): Region {
  const [region, setRegion] = useState<Region>('loading')

  useEffect(() => {
    fetch('https://ipapi.co/json/')
      .then((r) => r.json())
      .then((data) => {
        setRegion(data.country_code === 'IN' ? 'IN' : 'GLOBAL')
      })
      .catch(() => setRegion('GLOBAL'))
  }, [])

  return region
}
