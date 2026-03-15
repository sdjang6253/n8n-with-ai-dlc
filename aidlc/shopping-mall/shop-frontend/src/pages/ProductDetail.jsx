import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getProduct } from '../api/productApi'
import { getReviews } from '../api/reviewApi'
import { addCartItem, submitReview } from '../api/orderApi'

const BTN = { background: '#0EA5E9', color: '#fff', border: 'none', borderRadius: 8, padding: '10px 22px', cursor: 'pointer', fontWeight: 600, fontSize: 15 }
const BTN_SM = { ...BTN, padding: '7px 16px', fontSize: 13 }
const BTN_GHOST = { background: 'none', border: '1.5px solid #0EA5E9', color: '#0EA5E9', borderRadius: 8, padding: '7px 16px', cursor: 'pointer', fontWeight: 600, fontSize: 13 }

export default function ProductDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [product, setProduct] = useState(null)
  const [reviews, setReviews] = useState([])
  const [qty, setQty] = useState(1)
  const [reviewForm, setReviewForm] = useState({ rating: 5, content: '', orderId: '' })
  const [reviewError, setReviewError] = useState('')
  const [cartMsg, setCartMsg] = useState('')

  useEffect(() => {
    getProduct(id).then(r => setProduct(r.data))
    getReviews(id).then(r => setReviews(r.data.content || []))
  }, [id])

  const handleAddCart = async () => {
    try {
      await addCartItem({ productId: Number(id), quantity: qty })
      setCartMsg('장바구니에 추가되었습니다 ✓')
    } catch (err) {
      setCartMsg(err.response?.data?.message || '오류 발생')
    }
  }

  const handleReview = async (e) => {
    e.preventDefault()
    setReviewError('')
    try {
      await submitReview({ productId: Number(id), orderId: Number(reviewForm.orderId), rating: reviewForm.rating, content: reviewForm.content })
      setReviewForm({ rating: 5, content: '', orderId: '' })
      getReviews(id).then(r => setReviews(r.data.content || []))
    } catch (err) {
      const status = err.response?.status
      if (status === 401) setReviewError('로그인이 필요합니다')
      else if (status === 403) setReviewError('구매 이력이 없습니다')
      else setReviewError(err.response?.data?.message || '오류 발생')
    }
  }

  if (!product) return <p style={{ color: '#64748B', padding: 32 }}>로딩 중...</p>

  return (
    <div>
      <button onClick={() => navigate(-1)} style={BTN_GHOST}>← 뒤로</button>

      <div style={{ display: 'flex', gap: 40, marginTop: 20, background: '#fff', borderRadius: 16, padding: 32, boxShadow: '0 2px 12px rgba(0,0,0,0.07)' }}>
        <img src={product.imageUrl} alt={product.name} style={{ width: 320, height: 320, objectFit: 'cover', borderRadius: 12 }} />
        <div style={{ flex: 1 }}>
          <p style={{ margin: '0 0 4px', color: '#0EA5E9', fontSize: 13, fontWeight: 600 }}>{product.category}</p>
          <h2 style={{ margin: '0 0 12px', fontSize: 24, color: '#1E293B' }}>{product.name}</h2>
          <p style={{ color: '#64748B', lineHeight: 1.7, marginBottom: 20 }}>{product.description}</p>
          <p style={{ fontSize: 28, fontWeight: 700, color: '#0EA5E9', margin: '0 0 20px' }}>{product.price?.toLocaleString()}원</p>
          {product.stock === 0 ? (
            <span style={{ background: '#F1F5F9', color: '#94A3B8', borderRadius: 8, padding: '10px 20px', fontWeight: 600 }}>품절</span>
          ) : (
            <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
              <input type="number" min={1} max={product.stock} value={qty}
                onChange={e => setQty(Number(e.target.value))}
                style={{ width: 64, border: '1.5px solid #BAE6FD', borderRadius: 8, padding: '8px 10px', fontSize: 15, textAlign: 'center' }} />
              <button onClick={handleAddCart} style={BTN}>장바구니 담기</button>
            </div>
          )}
          {cartMsg && <p style={{ color: '#0EA5E9', marginTop: 10, fontWeight: 600 }}>{cartMsg}</p>}
          <p style={{ color: '#94A3B8', fontSize: 13, marginTop: 12 }}>재고 {product.stock}개</p>
        </div>
      </div>

      <div style={{ marginTop: 32, background: '#fff', borderRadius: 16, padding: 32, boxShadow: '0 2px 12px rgba(0,0,0,0.07)' }}>
        <h3 style={{ margin: '0 0 20px', color: '#1E293B' }}>리뷰 ({reviews.length})</h3>

        {localStorage.getItem('token') && (
          <form onSubmit={handleReview} style={{ display: 'flex', flexDirection: 'column', gap: 10, maxWidth: 480, marginBottom: 28, background: '#F0F9FF', borderRadius: 10, padding: 20 }}>
            <label style={{ fontSize: 13, color: '#0369A1', fontWeight: 600 }}>주문 ID
              <input type="number" value={reviewForm.orderId} onChange={e => setReviewForm({ ...reviewForm, orderId: e.target.value })} required
                style={{ marginLeft: 8, border: '1.5px solid #BAE6FD', borderRadius: 6, padding: '5px 10px', width: 80 }} />
            </label>
            <label style={{ fontSize: 13, color: '#0369A1', fontWeight: 600 }}>평점
              <select value={reviewForm.rating} onChange={e => setReviewForm({ ...reviewForm, rating: Number(e.target.value) })}
                style={{ marginLeft: 8, border: '1.5px solid #BAE6FD', borderRadius: 6, padding: '5px 10px' }}>
                {[5,4,3,2,1].map(n => <option key={n} value={n}>{'⭐'.repeat(n)} {n}점</option>)}
              </select>
            </label>
            <textarea placeholder="리뷰 내용 (최대 500자)" maxLength={500} value={reviewForm.content}
              onChange={e => setReviewForm({ ...reviewForm, content: e.target.value })} required
              style={{ border: '1.5px solid #BAE6FD', borderRadius: 6, padding: '8px 12px', resize: 'vertical', minHeight: 80, fontSize: 14 }} />
            {reviewError && <p style={{ color: '#EF4444', margin: 0, fontSize: 13 }}>{reviewError}</p>}
            <button type="submit" style={BTN_SM}>리뷰 작성</button>
          </form>
        )}

        {reviews.length === 0 && <p style={{ color: '#94A3B8' }}>아직 리뷰가 없습니다.</p>}
        {reviews.map(r => (
          <div key={r.reviewId} style={{ borderBottom: '1px solid #F1F5F9', padding: '16px 0' }}>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 6 }}>
              <span style={{ color: '#F59E0B', fontSize: 16 }}>{'⭐'.repeat(r.rating)}</span>
              <span style={{ color: '#64748B', fontSize: 13 }}>{r.rating}점</span>
            </div>
            <p style={{ margin: '0 0 6px', color: '#334155', lineHeight: 1.6 }}>{r.content}</p>
            <small style={{ color: '#94A3B8' }}>{new Date(r.createdAt).toLocaleDateString()}</small>
          </div>
        ))}
      </div>
    </div>
  )
}
