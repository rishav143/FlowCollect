import { useEffect, useState } from 'react'

export type Region = 'IN' | 'GLOBAL' | 'loading'

export function useGeoRegion(): Region {
  const [region, setRegion] = useState<Region>('loading')

  useEffect(() => {
    // Primary: ipapi.co
    fetch('https://ipapi.co/json/')
      .then((r) => r.json())
      .then((data) => {
        if (data.country_code) {
          setRegion(data.country_code === 'IN' ? 'IN' : 'GLOBAL')
        } else {
          throw new Error('no country_code')
        }
      })
      .catch(() => {
        // Fallback: ip-api.com
        fetch('http://ip-api.com/json/?fields=countryCode')
          .then((r) => r.json())
          .then((data) => {
            setRegion(data.countryCode === 'IN' ? 'IN' : 'GLOBAL')
          })
          .catch(() => setRegion('GLOBAL'))
      })
  }, [])

  return region
}
