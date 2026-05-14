'use client'
import { useState, useRef, useCallback } from 'react'
import { Upload, Wine, Loader2, Star, TrendingUp, AlertCircle } from 'lucide-react'

const API = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8001'

interface WineResult {
  name: string
  winery: string
  vintage?: number
  region?: string
  price?: number
  score?: number
  value_score?: number
  reason?: string
  tasting_note?: string
  value_tip?: string
  visible_price?: number
}

interface AnalyzeResponse {
  winner: WineResult
  all_wines: WineResult[]
  wines_identified: number
  wines_with_data: number
  error?: string
}

type Step = 'identifying' | 'researching' | 'recommending'

const STEP_LABELS: Record<Step, string> = {
  identifying: 'Reading wine labels…',
  researching: 'Looking up prices and ratings…',
  recommending: 'Picking the best bottle…',
}

function formatPrice(p?: number) {
  if (!p) return null
  return `£${p.toFixed(2)}`
}

export default function Home() {
  const [image, setImage] = useState<string | null>(null)
  const [imageFile, setImageFile] = useState<File | null>(null)
  const [preference, setPreference] = useState('best price/quality ratio')
  const [loading, setLoading] = useState(false)
  const [step, setStep] = useState<Step | null>(null)
  const [result, setResult] = useState<AnalyzeResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [dragging, setDragging] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  const handleFile = (file: File) => {
    if (!file.type.startsWith('image/')) return
    setImageFile(file)
    setResult(null)
    setError(null)
    const reader = new FileReader()
    reader.onload = e => setImage(e.target?.result as string)
    reader.readAsDataURL(file)
  }

  const onDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setDragging(false)
    const file = e.dataTransfer.files[0]
    if (file) handleFile(file)
  }, [])

  const analyze = async () => {
    if (!imageFile) return
    setLoading(true)
    setError(null)
    setResult(null)

    try {
      setStep('identifying')
      const form = new FormData()
      form.append('image', imageFile)
      form.append('preference', preference)

      // Simulate step progression
      setTimeout(() => setStep('researching'), 3000)
      setTimeout(() => setStep('recommending'), 8000)

      const res = await fetch(`${API}/api/analyze`, { method: 'POST', body: form })
      const data = await res.json()

      if (!res.ok) throw new Error(data.detail || 'Analysis failed')
      setResult(data)
    } catch (e: any) {
      setError(e.message)
    } finally {
      setLoading(false)
      setStep(null)
    }
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-12">
      {/* Header */}
      <div className="text-center mb-10">
        <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-red-700 text-white mb-4">
          <Wine size={28} />
        </div>
        <h1 className="text-3xl font-bold text-stone-900 mb-2">WineSnap</h1>
        <p className="text-stone-500 text-lg">Photo a wine shelf. Get the best bottle.</p>
      </div>

      {/* Upload zone */}
      <div
        className={`relative border-2 border-dashed rounded-2xl transition-all cursor-pointer mb-4 ${
          dragging ? 'border-red-500 bg-red-50' : 'border-stone-300 hover:border-red-400 bg-white'
        } ${image ? 'p-2' : 'p-10'}`}
        onClick={() => !image && inputRef.current?.click()}
        onDrop={onDrop}
        onDragOver={e => { e.preventDefault(); setDragging(true) }}
        onDragLeave={() => setDragging(false)}
      >
        {image ? (
          <div className="relative">
            <img src={image} alt="Wine shelf" className="w-full rounded-xl max-h-72 object-cover" />
            <button
              onClick={e => { e.stopPropagation(); setImage(null); setImageFile(null); setResult(null) }}
              className="absolute top-2 right-2 bg-black/60 text-white rounded-full px-3 py-1 text-xs hover:bg-black/80"
            >
              Change
            </button>
          </div>
        ) : (
          <div className="text-center">
            <Upload size={32} className="mx-auto text-stone-400 mb-3" />
            <p className="font-medium text-stone-700">Drop a wine shelf photo here</p>
            <p className="text-sm text-stone-400 mt-1">or click to browse · JPG, PNG, WEBP</p>
          </div>
        )}
        <input ref={inputRef} type="file" accept="image/*" className="hidden" onChange={e => e.target.files?.[0] && handleFile(e.target.files[0])} />
      </div>

      {/* Preference input */}
      <input
        value={preference}
        onChange={e => setPreference(e.target.value)}
        placeholder="What matters to you? (e.g. best price/quality, good with steak, birthday gift)"
        className="w-full border border-stone-200 rounded-xl px-4 py-3 text-sm bg-white focus:outline-none focus:border-red-400 mb-4"
      />

      {/* Analyze button */}
      <button
        onClick={analyze}
        disabled={!imageFile || loading}
        className="w-full py-3.5 rounded-xl font-semibold text-white bg-red-700 hover:bg-red-800 disabled:opacity-40 disabled:cursor-not-allowed transition-colors flex items-center justify-center gap-2"
      >
        {loading ? (
          <>
            <Loader2 size={18} className="animate-spin" />
            {step ? STEP_LABELS[step] : 'Analyzing…'}
          </>
        ) : (
          <>
            <Wine size={18} />
            Pick the best wine
          </>
        )}
      </button>

      {/* Error */}
      {error && (
        <div className="mt-6 flex items-start gap-3 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm">
          <AlertCircle size={16} className="shrink-0 mt-0.5" />
          {error}
        </div>
      )}

      {/* Results */}
      {result && !error && (
        <div className="mt-8 space-y-6">
          {/* Winner card */}
          <div className="bg-white rounded-2xl border-2 border-red-700 p-6 shadow-sm">
            <div className="flex items-center gap-2 text-red-700 text-sm font-semibold mb-3">
              <Star size={14} fill="currentColor" />
              Best pick for "{preference}"
            </div>
            <h2 className="text-xl font-bold text-stone-900">
              {result.winner.winery} {result.winner.name}
              {result.winner.vintage && <span className="text-stone-500 font-normal ml-1">{result.winner.vintage}</span>}
            </h2>
            {result.winner.region && <p className="text-sm text-stone-500 mt-0.5">{result.winner.region}</p>}

            <div className="flex items-center gap-4 mt-4">
              {result.winner.price && (
                <div className="text-center">
                  <div className="text-xl font-bold text-stone-900">{formatPrice(result.winner.price)}</div>
                  <div className="text-xs text-stone-400">Price</div>
                </div>
              )}
              {result.winner.score && (
                <div className="text-center">
                  <div className="text-xl font-bold text-stone-900">{result.winner.score}</div>
                  <div className="text-xs text-stone-400">Score / 100</div>
                </div>
              )}
              {result.winner.value_score && (
                <div className="text-center">
                  <div className="text-xl font-bold text-red-700">{result.winner.value_score.toFixed(1)}</div>
                  <div className="text-xs text-stone-400">Value score</div>
                </div>
              )}
            </div>

            {result.winner.reason && (
              <p className="mt-4 text-sm text-stone-700 leading-relaxed">{result.winner.reason}</p>
            )}
            {result.winner.tasting_note && (
              <p className="mt-2 text-sm text-stone-500 italic">{result.winner.tasting_note}</p>
            )}
            {result.winner.value_tip && (
              <div className="mt-3 flex items-start gap-2 text-sm text-emerald-700 bg-emerald-50 rounded-lg px-3 py-2">
                <TrendingUp size={14} className="shrink-0 mt-0.5" />
                {result.winner.value_tip}
              </div>
            )}
          </div>

          {/* All wines comparison */}
          {result.all_wines.length > 1 && (
            <div>
              <h3 className="text-sm font-semibold text-stone-500 uppercase tracking-wide mb-3">
                All wines found ({result.wines_identified} identified, {result.wines_with_data} with data)
              </h3>
              <div className="space-y-2">
                {result.all_wines.map((w, i) => (
                  <div key={i} className="bg-white rounded-xl border border-stone-200 px-4 py-3 flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <p className="font-medium text-stone-900 truncate">{w.winery} {w.name} {w.vintage || ''}</p>
                      <p className="text-xs text-stone-400">{w.region || ''}</p>
                    </div>
                    <div className="flex items-center gap-3 shrink-0 text-sm">
                      {w.price && <span className="text-stone-700">{formatPrice(w.price)}</span>}
                      {w.score && <span className="text-stone-500">{w.score}pts</span>}
                      {w.value_score && (
                        <span className={`font-semibold ${i === 0 ? 'text-red-700' : 'text-stone-400'}`}>
                          {w.value_score.toFixed(1)}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
