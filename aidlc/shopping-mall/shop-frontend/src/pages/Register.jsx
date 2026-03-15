import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { register } from '../api/userApi'

const INPUT = { border: '1.5px solid #BAE6FD', borderRadius: 8, padding: '10px 14px', fontSize: 15, outline: 'none', width: '100%', boxSizing: 'border-box' }
const BTN = { background: '#0EA5E9', color: '#fff', border: 'none', borderRadius: 8, padding: '12px', cursor: 'pointer', fontWeight: 700, fontSize: 15, width: '100%' }

export default function Register() {
  const [form, setForm] = useState({ email: '', password: '', name: '' })
  const [errors, setErrors] = useState({})
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setErrors({})
    try {
      await register(form)
      navigate('/login')
    } catch (err) {
      const data = err.response?.data
      if (data?.fields) setErrors(data.fields)
      else setErrors({ general: data?.message || '회원가입 실패' })
    }
  }

  return (
    <div style={{ maxWidth: 400, margin: '60px auto' }}>
      <div style={{ background: '#fff', borderRadius: 16, padding: 36, boxShadow: '0 4px 20px rgba(14,165,233,0.1)' }}>
        <h2 style={{ margin: '0 0 24px', color: '#1E293B', textAlign: 'center' }}>회원가입</h2>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <input type="text" placeholder="이름" value={form.name}
            onChange={e => setForm({ ...form, name: e.target.value })} required style={INPUT} />
          <input type="email" placeholder="이메일" value={form.email}
            onChange={e => setForm({ ...form, email: e.target.value })} required style={INPUT} />
          {errors.email && <p style={{ color: '#EF4444', margin: '-8px 0 0', fontSize: 13 }}>{errors.email}</p>}
          <input type="password" placeholder="비밀번호 (8자 이상)" value={form.password}
            onChange={e => setForm({ ...form, password: e.target.value })} required style={INPUT} />
          {errors.password && <p style={{ color: '#EF4444', margin: '-8px 0 0', fontSize: 13 }}>{errors.password}</p>}
          {errors.general && <p style={{ color: '#EF4444', margin: 0, fontSize: 13 }}>{errors.general}</p>}
          <button type="submit" style={BTN}>회원가입</button>
        </form>
      </div>
    </div>
  )
}
