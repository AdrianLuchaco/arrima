import { useRef, type ReactNode, type TouchEvent } from 'react'

export interface Tab {
  id: string
  label: string
  content: ReactNode
}

interface SwipeTabsProps {
  tabs: Tab[]
  active: string
  onChange: (id: string) => void
  label: string
}

const MIN_SWIPE_PX = 70

/**
 * Big tab buttons, plus a horizontal swipe on the content to move between neighbours
 * (Jugadores ↔ Equipos ↔ Pistas). Swipes that start inside something that scrolls sideways,
 * like the court grid, are left to that element.
 * Every tab is always in sight: when they don't fit in one row (five tabs on a phone), they wrap
 * onto a second one instead of hiding off the edge of the screen.
 */
export function SwipeTabs({ tabs, active, onChange, label }: SwipeTabsProps) {
  const start = useRef<{ x: number; y: number; ignore: boolean } | null>(null)
  const index = Math.max(0, tabs.findIndex((tab) => tab.id === active))

  function onTouchStart(event: TouchEvent) {
    const touch = event.touches[0]
    start.current = { x: touch.clientX, y: touch.clientY, ignore: insideHorizontalScroller(event.target as Element) }
  }

  function onTouchEnd(event: TouchEvent) {
    const origin = start.current
    start.current = null
    if (!origin || origin.ignore) return
    const touch = event.changedTouches[0]
    const dx = touch.clientX - origin.x
    const dy = touch.clientY - origin.y
    if (Math.abs(dx) < MIN_SWIPE_PX || Math.abs(dx) < 2 * Math.abs(dy)) return
    const next = dx < 0 ? index + 1 : index - 1
    if (next >= 0 && next < tabs.length) onChange(tabs[next].id)
  }

  return (
    <div>
      <div role="tablist" aria-label={label} className="sticky top-0 z-10 -mx-4 mb-4 flex flex-wrap gap-2 bg-gravel-50 px-4 py-2 shadow-[0_8px_10px_-8px_rgba(28,34,38,0.3)]">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            id={`tab-${tab.id}`}
            role="tab"
            type="button"
            aria-selected={tab.id === active}
            aria-controls={`panel-${tab.id}`}
            onClick={() => onChange(tab.id)}
            className={`min-h-12 shrink-0 grow rounded-2xl border-2 px-3 text-lg font-bold ${tab.id === active ? 'border-steel-800 bg-steel-800 text-white' : 'border-steel-400 bg-white text-steel-900'}`}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <div
        id={`panel-${tabs[index].id}`}
        role="tabpanel"
        aria-labelledby={`tab-${tabs[index].id}`}
        onTouchStart={onTouchStart}
        onTouchEnd={onTouchEnd}
        className="min-h-[50dvh]"
      >
        {tabs[index].content}
      </div>
    </div>
  )
}

function insideHorizontalScroller(element: Element | null): boolean {
  for (let node = element; node && node !== document.body; node = node.parentElement) {
    if (node.scrollWidth > node.clientWidth + 1 && getComputedStyle(node).overflowX !== 'visible') return true
  }
  return false
}
