import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getOrders } from '../api/orderApi'

const STATUS_COLOR = { '주문완료': '#0EA5E9' }

export default function Orders() {
  const [orders, setOrders] = useState([])
  const navigate = useNavigate()

  useEffect(() => {
    getOrders().then(r => setOrders(r.data)).catch(() => navigate('/login'))
  }, [])

  return (
    <div>
      <h2 style={{ color: '#1E293B', marginBottom: 20 }}>주문 내역</h2>
      {orders.length === 0 ? (
        <div style={{ background: '#fff', borderRadius: 12, padding: 48, textAlign: 'center', color: '#94A3B8', boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
          주문 내역이 없습니다
        </div>
      ) : orders.map(order => (
        <div key={order.orderId} style={{ background: '#fff', borderRadius: 12, padding: 20, marginBottom: 16, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <span style={{ fontWeight: 600, color: '#1E293B' }}>주문번호 #{order.orderId}</span>
            <span style={{ background: '#E0F2FE', color: STATUS_COLOR[order.status] || '#64748B', borderRadius: 6, padding: '3px 10px', fontSize: 13, fontWeight: 600 }}>{order.status}</span>
            <span style={{ color: '#94A3B8', fontSize: 13 }}>{new Date(order.createdAt).toLocaleDateString()}</span>
          </div>
          {order.items.map(item => (
            <div key={item.productId} style={{ display: 'flex', gap: 8, padding: '8px 0', borderTop: '1px solid #F1F5F9', alignItems: 'center' }}>
              <span style={{ flex: 1, color: '#334155' }}>{item.productName}</span>
              <span style={{ color: '#64748B', fontSize: 14 }}>{item.quantity}개</span>
              <span style={{ fontWeight: 600, color: '#0EA5E9', minWidth: 80, textAlign: 'right' }}>{(item.price * item.quantity).toLocaleString()}원</span>
            </div>
          ))}
          <p style={{ textAlign: 'right', fontWeight: 700, fontSize: 16, color: '#1E293B', marginTop: 10, marginBottom: 0 }}>
            합계: <span style={{ color: '#0EA5E9' }}>{order.totalPrice?.toLocaleString()}원</span>
          </p>
        </div>
      ))}
    </div>
  )
}
