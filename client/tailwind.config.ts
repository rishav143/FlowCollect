import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        brand: {
          navy:       '#1B2838',
          teal:       '#2E7A8E',
          blue:       '#29B6F6',
          sky:        '#4FC3F7',
          'dark-teal':'#1E5A6A',
          slate:      '#8A9BAE',
          cloud:      '#F4F7F9',
          ink:        '#0D1B2A',
        },
        status: {
          overdue: '#EF4444',
          due:     '#F59E0B',
          paid:    '#22C55E',
          draft:   '#8A9BAE',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      opacity: {
        8:  '0.08',
        12: '0.12',
      },
    },
  },
  plugins: [],
} satisfies Config
