import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'WineSnap — AI Wine Picker',
  description: 'Photo a wine shelf and get the best bottle for your budget.',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className={`${inter.className} bg-stone-50 text-stone-900 antialiased min-h-screen`}>
        {children}
      </body>
    </html>
  )
}
