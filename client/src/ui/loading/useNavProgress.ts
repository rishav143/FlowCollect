import { useNavigation } from 'react-router-dom'

/**
 * useNavProgress
 *
 * Returns `true` while React Router is navigating to a new route
 * (fetching a lazy route chunk or running loaders).
 *
 * Works with `createBrowserRouter` + React Router `lazy` route property.
 * Drives TopLoadingBar — no page component needs to know about loading state.
 *
 * Low-coupling: swap the implementation here (e.g. add debounce) without
 * touching any UI component.
 */
export function useNavProgress(): boolean {
  return useNavigation().state !== 'idle'
}
