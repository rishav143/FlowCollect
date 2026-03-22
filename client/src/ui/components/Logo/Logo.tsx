interface LogoProps {
  /** 'dark' = Flow in ink, Collect in electric blue (for light backgrounds)
   *  'light' = Flow in white, Collect in electric blue (for dark backgrounds) */
  variant?: 'dark' | 'light'
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

const sizeClass = {
  sm: 'text-base',
  md: 'text-xl',
  lg: 'text-2xl',
}

const flowColor = {
  dark:  'text-[#0D1B2A] dark:text-white',
  light: 'text-white',
}

export default function Logo({ variant = 'dark', size = 'md', className = '' }: LogoProps) {
  return (
    <span
      className={`font-bold tracking-tight select-none ${sizeClass[size]} ${flowColor[variant]} ${className}`}
    >
      Flow<span className="text-[#29B6F6]">Collect</span>
    </span>
  )
}
