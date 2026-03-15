import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCart, updateCartItem, removeCartItem, createOrder } from '../api/orderApi'

const BTN = { background: '#0EA5E9', color: '#fff', border: 'none', borderRadius: 8, padding: '10px 24px', cursor: 'pointer', fontWeight: 600, fontSize: 15 }
const BTN_SM = { background: 'none', border: '1px solid #E2E8F0', color: '#94A3B8', borderRadius: 6, padding: '5px 12px', cursor: 'pointer', fontSize: 13 }

export default function Cart() {
  const [cart, setCart] = useState({ items: [], totalPrice: 0 })
  const [msg, setMsg] = useState('')
  const navigate = useNavigate()

  const fetchCart = () => getCart().then(r => setCart(r.data)).catch(() => navigate('/login'))

  useEffect(() => { fetchCart() }, [])

  const handleUpdate = async (productId, quantity) => {
    await updateCartItem(productId, { productId, quantity })
    fetchCart()
  }

  const handleRemove = async (productId) => {
    await removeCartItem(productId)
    fetchCart()
  }

  const handleOrder = async () => {
    try {
      await createOrder()
      setMsg('주문이 완료되었습니다 ✓')
      fetchCart()
    } catch (err) {
      setMsg(err.response?.data?.message || '주문 실패')
    }
  }

  return (
    <div>
      <h2 style={{ color: '#1E293B', marginBottom: 20 }}>장바구니</h2>
      {cart.items.length === 0 ? (
        <div style={{ background: '#fff', borderRadius: 12, padding: 48, textAlign: 'center', color: '#94A3B8', boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
          장바구니가 비어 있습니다
        </div>
      ) : (
        <div style={{ background: '#fff', borderRadius: 12, padding: 24, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
          {cart.items.map(item => (
            <div key={item.productId} style={{ display: 'flex', gap: 16, alignItems: 'center', borderBottom: '1px solid #F1F5F9', padding: '14px 0' }}>
              <img src={item.imageUrl} alt={item.productName} style={{ width: 72, height: 72, objectFit: 'cover', borderRadius: 8 }} />
              <div style={{ flex: 1 }}>
                <p style={{ margin: '0 0 4px', fontWeight: 600, color: '#1E293B' }}>{item.productName}</p>
                <p style={{ margin: 0, color: '#64748B', fontSize: 14 }}>{item.price?.toLocaleString()}원</p>
              </div>
              <input type="number" min={1} max={item.stock} value={item.quantity}
                onChange={e => handleUpdate(item.productId, Number(e.target.value))}
                style={{ width: 60, border: '1.5px solid #BAE6FD', borderRadius: 6, padding: '6px 8px', textAlign: 'center' }} />
              <p style={{ minWidth: 80, textAlign: 'right', fontWeight: 600, color: '#0EA5E9' }}>{item.subtotal?.toLocaleString()}원</p>
              <button onClick={() => handleRemove(item.productId)} style={BTN_SM}>삭제</button>
            </div>
          ))}
          <div style={{ textAlign: 'right', marginTop: 20 }}>
            <p style={{ fontSize: 20, fontWeight: 700, color: '#1E293B', marginBottom: 12 }}>합계: <span style={{ color: '#0EA5E9' }}>{cart.totalPrice?.toLocaleString()}원</span></p>
            <button onClick={handleOrder} style={BTN}>주문하기</button>
          </div>
        </div>
      )}
      {msg && <p style={{ color: '#0EA5E9', marginTop: 12, fontWeight: 600 }}>{msg}</p>}
    </div>
  )
}
