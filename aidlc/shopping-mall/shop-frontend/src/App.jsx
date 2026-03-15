import React from 'react'
import { Routes, Route, Link, useNavigate } from 'react-router-dom'
import ProductList from './pages/ProductList'
import ProductDetail from './pages/ProductDetail'
import Cart from './pages/Cart'
import Orders from './pages/Orders'
import Login from './pages/Login'
import Register from './pages/Register'
import Admin from './pages/Admin'

export default function App() {
  const navigate = useNavigate()
  const token = localStorage.getItem('token')

  const logout = () => {
    localStorage.removeItem('token')
    navigate('/login')
  }

  return (
    <div style={{ fontFamily: 'sans-serif', maxWidth: 1200, margin: '0 auto', padding: '0 16px' }}>
      <nav style={{ display: 'flex', gap: 16, padding: '12px 0', borderBottom: '1px solid #eee', marginBottom: 24, alignItems: 'center' }}>
        <Link to="/">상품목록</Link>
        {token ? (
          <>
            <Link to="/cart">장바구니</Link>
            <Link to="/orders">주문내역</Link>
            <button onClick={logout} style={{ cursor: 'pointer', background: 'none', border: 'none', color: 'blue' }}>로그아웃</button>
          </>
        ) : (
          <>
            <Link to="/login">로그인</Link>
            <Link to="/register">회원가입</Link>
          </>
        )}
        <span style={{ marginLeft: 'auto' }}>
          <Link to="/admin" style={{ color: '#888', fontSize: 13 }}>⚙ 관리자</Link>
        </span>
      </nav>
      <Routes>
        <Route path="/" element={<ProductList />} />
        <Route path="/products/:id" element={<ProductDetail />} />
        <Route path="/cart" element={<Cart />} />
        <Route path="/orders" element={<Orders />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/admin" element={<Admin />} />
      </Routes>
    </div>
  )
}
