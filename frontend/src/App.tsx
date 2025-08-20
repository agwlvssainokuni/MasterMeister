import {BrowserRouter as Router, Routes, Route, Navigate} from 'react-router-dom'
import {AuthProvider} from './contexts/AuthContext'
import {ProtectedRoute} from './components/ProtectedRoute'
import {LoginPage} from './pages/LoginPage'
import {LogoutButton} from './components/LogoutButton'
import './App.css'

const DashboardPage = () => (
  <div style={{padding: '2rem'}}>
    <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem'}}>
      <h1>Dashboard</h1>
      <LogoutButton />
    </div>
    <p>Welcome to Master Meister Dashboard!</p>
  </div>
)

const App = () => {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </Router>
  )
}

export default App
