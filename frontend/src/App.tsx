import {BrowserRouter as Router, Navigate, Route, Routes} from 'react-router-dom'
import {AuthProvider} from './contexts/AuthContext'
import {NotificationProvider} from './contexts/NotificationContext'
import {ProtectedRoute} from './components/ProtectedRoute'
import {AdminRoute} from './components/ProtectedRoute'
import {LoginPage} from './pages/LoginPage'
import {RegisterPage} from './pages/RegisterPage'
import {EmailConfirmationPage} from './pages/EmailConfirmationPage'
import {AdminDashboard} from './pages/AdminDashboard'
import {LogoutButton} from './components/LogoutButton'
import './App.css'

const DashboardPage = () => (
  <div style={{padding: '2rem'}}>
    <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem'}}>
      <h1>Dashboard</h1>
      <LogoutButton/>
    </div>
    <p>Welcome to Master Meister Dashboard!</p>
  </div>
)

const App = () => {
  return (
    <Router>
      <AuthProvider>
        <NotificationProvider>
          <Routes>
            <Route path="/login" element={<LoginPage/>}/>
            <Route path="/register" element={<RegisterPage/>}/>
            <Route path="/confirm-email" element={<EmailConfirmationPage/>}/>
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <DashboardPage/>
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin"
              element={
                <AdminRoute>
                  <AdminDashboard/>
                </AdminRoute>
              }
            />
            <Route path="/" element={<Navigate to="/dashboard" replace/>}/>
          </Routes>
        </NotificationProvider>
      </AuthProvider>
    </Router>
  )
}

export default App
