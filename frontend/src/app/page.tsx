'use client'
import { useState, useRef, useCallback } from 'react'
import { Camera, Upload, Wine, Loader2, Star, TrendingUp, AlertCircle, ChevronDown, ChevronUp } from 'lucide-react'

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
}

interface AnalyzeResponse {
  winner: WineResult
  all_wines: WineResult[]
  wines_identified: number
  wines_with_data: number
  error?: string
}

type Step = 'identifying' | 'researching' | 'recommending'
const STEPS: Step[] = ['identifying', 'researching', 'recommending']
const STEP_LABELS: Record<Step, string> = {
  identifying: 'Reading wine labels…',
  researching: 'Looking up prices and ratings…',
  recommending: 'Picking the best bottle…',
}

function formatPrice(p?: number) {
  return p ? `£${p.toFixed(2)}` : null
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
  const [showAll, setShowAll] = useState(false)
  const cameraRef = useRef<HTMLInputElement>(null)
  const galleryRef = useRef<HTMLInputElement>(null)

  const handleFile = (file: File) => {
    if (!file.type.startsWith('image/')) return
    setImageFile(file)
    setResult(null)
    setError(null)
    setShowAll(false)
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

      const timers = [
        setTimeout(() => setStep('researching'), 4000),
        setTimeout(() => setStep('recommending'), 10000),
      ]

      const res = await fetch(`${API}/api/analyze`, { method: 'POST', body: form })
      timers.forEach(clearTimeout)

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

  const reset = () => {
    setImage(null)
    setImageFile(null)
    setResult(null)
    setError(null)
    setShowAll(false)
  }

  return (
    <div className="max-w-lg mx-auto px-4 py-6 pb-10">

      {/* Header */}
      <div className="text-center mb-6">
        <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-red-800 text-white mb-3">
          <Wine size={22} />
        </div>
        <h1 className="text-2xl font-bold text-stone-900">WineSnap</h1>
        <p className="text-stone-500 mt-0.5 text-sm">Photo a wine shelf. Get the best bottle.</p>
      </div>

      {/* Image area */}
      {!image ? (
        <div
          className={`border-2 border-dashed rounded-2xl transition-colors mb-4 ${
            dragging ? 'border-red-500 bg-red-50' : 'border-stone-300 bg-white'
          }`}
          onDrop={onDrop}
          onDragOver={e => { e.preventDefault(); setDragging(true) }}
          onDragLeave={() => setDragging(false)}
        >
          {/* Camera + Gallery buttons — prominent on mobile */}
          <div className="grid grid-cols-2 gap-0 divide-x divide-stone-200">
            <button
              onClick={() => cameraRef.current?.click()}
              className="flex flex-col items-center gap-2 py-8 rounded-l-2xl hover:bg-red-50 transition-colors"
            >
              <Camera size={28} className="text-red-700" />
              <span className="text-sm font-medium text-stone-700">Take Photo</span>
              <span className="text-xs text-stone-400">Open camera</span>
            </button>
            <button
              onClick={() => galleryRef.current?.click()}
              className="flex flex-col items-center gap-2 py-8 rounded-r-2xl hover:bg-stone-50 transition-colors"
            >
              <Upload size={28} className="text-stone-500" />
              <span className="text-sm font-medium text-stone-700">Choose Photo</span>
              <span className="text-xs text-stone-400">From gallery</span>
            </button>
          </div>
          {/* Hidden inputs */}
          <input ref={cameraRef} type="file" accept="image/*" capture="environment" className="hidden"
            onChange={e => e.target.files?.[0] && handleFile(e.target.files[0])} />
          <input ref={galleryRef} type="file" accept="image/*" className="hidden"
            onChange={e => e.target.files?.[0] && handleFile(e.target.files[0])} />
        </div>
      ) : (
        <div className="relative mb-4">
          <img src={image} alt="Wine shelf" className="w-full rounded-2xl max-h-64 object-cover" />
          <button onClick={reset}
            className="absolute top-2 right-2 bg-black/60 text-white text-xs rounded-full px-3 py-1.5 hover:bg-black/80">
            Change
          </button>
        </div>
      )}

      {/* Preference */}
      <input
        value={preference}
        onChange={e => setPreference(e.target.value)}
        placeholder="What matters? e.g. good with steak, birthday gift, under £15"
        className="w-full border border-stone-200 rounded-xl px-4 py-3 text-sm bg-white focus:outline-none focus:border-red-400 mb-3"
      />

      {/* Analyze button */}
      <button
        onClick={analyze}
        disabled={!imageFile || loading}
        className="w-full py-4 rounded-xl font-semibold text-white bg-red-800 hover:bg-red-900 active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed transition-all flex items-center justify-center gap-2 text-base"
      >
        {loading ? (
          <>
            <Loader2 size={18} className="animate-spin" />
            <span>{step ? STEP_LABELS[step] : 'Analysing…'}</span>
          </>
        ) : (
          <>
            <Wine size={18} />
            Pick the best wine
          </>
        )}
      </button>

      {/* Step progress */}
      {loading && step && (
        <div className="flex justify-center gap-2 mt-3">
          {STEPS.map(s => (
            <div key={s} className={`h-1 rounded-full flex-1 transition-colors ${
              STEPS.indexOf(s) <= STEPS.indexOf(step) ? 'bg-red-700' : 'bg-stone-200'
            }`} />
          ))}
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="mt-5 flex items-start gap-3 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm">
          <AlertCircle size={16} className="shrink-0 mt-0.5" />
          {error}
        </div>
      )}

      {/* Results */}
      {result && (
        <div className="mt-6 space-y-4">

          {/* Winner card */}
          <div className="bg-white rounded-2xl border-2 border-red-700 p-5 shadow-sm">
            <div className="flex items-center gap-1.5 text-red-700 text-xs font-semibold mb-2 uppercase tracking-wide">
              <Star size={12} fill="currentColor" />
              Best pick
            </div>

            <h2 className="text-lg font-bold text-stone-900 leading-snug">
              {result.winner.winery} {result.winner.name}
              {result.winner.vintage && (
                <span className="text-stone-500 font-normal ml-1 text-base">{result.winner.vintage}</span>
              )}
            </h2>
            {result.winner.region && (
              <p className="text-xs text-stone-400 mt-0.5">{result.winner.region}</p>
            )}

            {/* Stats */}
            <div className="flex gap-4 mt-3">
              {result.winner.price && (
                <div>
                  <div className="text-lg font-bold text-stone-900">{formatPrice(result.winner.price)}</div>
                  <div className="text-xs text-stone-400">Price</div>
                </div>
              )}
              {result.winner.score && (
                <div>
                  <div className="text-lg font-bold text-stone-900">{result.winner.score}</div>
                  <div className="text-xs text-stone-400">Score/100</div>
                </div>
              )}
              {result.winner.value_score && (
                <div>
                  <div className="text-lg font-bold text-red-700">{result.winner.value_score.toFixed(1)}</div>
                  <div className="text-xs text-stone-400">Value</div>
                </div>
              )}
            </div>

            {result.winner.reason && (
              <p className="mt-3 text-sm text-stone-700 leading-relaxed">{result.winner.reason}</p>
            )}
            {result.winner.tasting_note && (
              <p className="mt-2 text-sm text-stone-500 italic">{result.winner.tasting_note}</p>
            )}
            {result.winner.value_tip && (
              <div className="mt-3 flex items-start gap-2 text-xs text-emerald-700 bg-emerald-50 rounded-lg px-3 py-2">
                <TrendingUp size={12} className="shrink-0 mt-0.5" />
                {result.winner.value_tip}
              </div>
            )}
          </div>

          {/* All wines */}
          {result.all_wines.length > 1 && (
            <div>
              <button
                onClick={() => setShowAll(v => !v)}
                className="flex items-center justify-between w-full text-sm text-stone-500 py-2"
              >
                <span>
                  All wines ({result.wines_identified} found
                  {result.wines_with_data < result.wines_identified
                    ? `, ${result.wines_with_data} with price/score data`
                    : ''})
                </span>
                {showAll ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
              </button>

              {showAll && (
                <div className="space-y-2">
                  {result.all_wines.map((w, i) => (
                    <div key={i} className="bg-white rounded-xl border border-stone-200 px-4 py-3 flex items-center justify-between gap-2">
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-stone-900 truncate">
                          {w.winery} {w.name} {w.vintage || ''}
                        </p>
                        <p className="text-xs text-stone-400 truncate">{w.region || '—'}</p>
                      </div>
                      <div className="flex flex-col items-end shrink-0 text-xs gap-0.5">
                        {w.price && <span className="font-medium text-stone-700">{formatPrice(w.price)}</span>}
                        {w.score && <span className="text-stone-400">{w.score}pts</span>}
                        {w.value_score && (
                          <span className={`font-bold ${i === 0 ? 'text-red-700' : 'text-stone-300'}`}>
                            {w.value_score.toFixed(1)}
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Try another */}
          <button onClick={reset} className="w-full py-3 rounded-xl text-sm font-medium text-stone-600 border border-stone-200 bg-white hover:bg-stone-50 active:scale-95 transition-all">
            Try another photo
          </button>
        </div>
      )}
    </div>
  )
}
