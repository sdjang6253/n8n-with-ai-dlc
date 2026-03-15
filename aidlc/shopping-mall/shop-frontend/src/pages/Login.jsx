import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../api/userApi'

const INPUT = { border: '1.5px solid #BAE6FD', borderRadius: 8, padding: '10px 14px', fontSize: 15, outline: 'none', width: '100%', boxSizing: 'border-box' }
const BTN = { background: '#0EA5E9', color: '#fff', border: 'none', borderRadius: 8, padding: '12px', cursor: 'pointer', fontWeight: 700, fontSize: 15, width: '100%' }

export default function Login() {
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      const res = await login(form)
      localStorage.setItem('token', res.data.accessToken)
      navigate('/')
    } catch (err) {
      setError(err.response?.data?.message || '로그인 실패')
    }
  }

  return (
    <div style={{ maxWidth: 400, margin: '60px auto' }}>
      <div style={{ background: '#fff', borderRadius: 16, padding: 36, boxShadow: '0 4px 20px rgba(14,165,233,0.1)' }}>
        <h2 style={{ margin: '0 0 24px', color: '#1E293B', textAlign: 'center' }}>로그인</h2>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <input type="email" placeholder="이메일" value={form.email}
            onChange={e => setForm({ ...form, email: e.target.value })} required style={INPUT} />
          <input type="password" placeholder="비밀번호" value={form.password}
            onChange={e => setForm({ ...form, password: e.target.value })} required style={INPUT} />
          {error && <p style={{ color: '#EF4444', margin: 0, fontSize: 13 }}>{error}</p>}
          <button type="submit" style={BTN}>로그인</button>
        </form>
        <p style={{ textAlign: 'center', color: '#94A3B8', fontSize: 13, marginTop: 16 }}>
          테스트 계정: alice@example.com / password123
        </p>
      </div>
    </div>
  )
}
