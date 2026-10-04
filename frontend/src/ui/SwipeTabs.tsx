import { useEffect, useRef, type ReactNode, type TouchEvent } from 'react'

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
 * With five tabs they don't fit on a phone: the row scrolls sideways and keeps the active one in view.
 */
export function SwipeTabs({ tabs, active, onChange, label }: SwipeTabsProps) {
  const start = useRef<{ x: number; y: number; ignore: boolean } | null>(null)
  const index = Math.max(0, tabs.findIndex((tab) => tab.id === active))
  const tabList = useRef<HTMLDivElement>(null)
  const firstScroll = useRef(true)

  useEffect(() => {
    const row = tabList.current
    const button = row?.querySelector('[aria-selected="true"]')
    if (!row || !button) return
    // Only the row scrolls (scrollIntoView could also move the page).
    const rowBox = row.getBoundingClientRect()
    const box = button.getBoundingClientRect()
    if (box.left < rowBox.left || box.right > rowBox.right) {
      // On arrival it simply appears in place; when changing tabs it slides.
      row.scrollBy({ left: box.left - rowBox.left - 16, behavior: firstScroll.current ? 'instant' : 'smooth' })
    }
    firstScroll.current = false
  }, [active])

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
      <div
        ref={tabList}
        role="tablist"
        aria-label={label}
        className="sticky top-0 z-10 -mx-4 mb-4 flex gap-2 overflow-x-auto bg-gravel-50 px-4 py-2 [scrollbar-width:none]"
      >
        {tabs.map((tab) => (
          <button
            key={tab.id}
            id={`tab-${tab.id}`}
            role="tab"
            type="button"
            aria-selected={tab.id === active}
            aria-controls={`panel-${tab.id}`}
            onClick={() => onChange(tab.id)}
            className={`min-h-14 shrink-0 grow rounded-2xl border-2 px-3 text-lg font-bold ${tab.id === active ? 'border-steel-800 bg-steel-800 text-white' : 'border-steel-400 bg-white text-steel-900'}`}
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
