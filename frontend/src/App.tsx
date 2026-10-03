import { HomePage } from './pages/HomePage'
import { ServerGate } from './server/ServerGate'

export default function App() {
  return (
    <ServerGate>
      <HomePage />
    </ServerGate>
  )
}
