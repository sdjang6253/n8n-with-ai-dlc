import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProducts } from '../api/productApi'

const CATEGORIES = ['', '의류', '전자기기', '식품', '생활용품']

const BTN = {
  background: '#0EA5E9', color: '#fff', border: 'none',
  borderRadius: 6, padding: '8px 18px', cursor: 'pointer', fontWeight: 600, fontSize: 14,
}

const BTN_DISABLED = { ...BTN, background: '#BAE6FD', cursor: 'default' }

export default function ProductList() {
  const [products, setProducts] = useState([])
  const [category, setCategory] = useState('')
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const fetchProducts = async () => {
    const params = { page, size: 20 }
    if (category) params.category = category
    if (keyword) params.keyword = keyword
    const res = await getProducts(params)
    setProducts(res.data.content)
    setTotalPages(res.data.totalPages)
  }

  useEffect(() => { fetchProducts() }, [page, category])

  const handleSearch = (e) => {
    e.preventDefault()
    setPage(0)
    fetchProducts()
  }

  return (
    <div>
      {/* 검색/필터 바 */}
      <form onSubmit={handleSearch} style={{ display: 'flex', gap: 8, marginBottom: 24, background: '#fff', padding: '12px 16px', borderRadius: 10, boxShadow: '0 1px 4px rgba(0,0,0,0.06)' }}>
        <select value={category} onChange={e => { setCategory(e.target.value); setPage(0) }}
          style={{ border: '1.5px solid #BAE6FD', borderRadius: 6, padding: '7px 12px', fontSize: 14, color: '#0369A1', background: '#F0F9FF' }}>
          {CATEGORIES.map(c => <option key={c} value={c}>{c || '전체 카테고리'}</option>)}
        </select>
        <input placeholder="상품명 검색" value={keyword} onChange={e => setKeyword(e.target.value)}
          style={{ flex: 1, border: '1.5px solid #BAE6FD', borderRadius: 6, padding: '7px 12px', fontSize: 14, outline: 'none' }} />
        <button type="submit" style={BTN}>검색</button>
      </form>

      {/* 상품 그리드 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 20 }}>
        {products.map(p => (
          <Link key={p.id} to={`/products/${p.id}`} style={{ textDecoration: 'none', color: 'inherit' }}>
            <div style={{ background: '#fff', borderRadius: 12, overflow: 'hidden', boxShadow: '0 2px 8px rgba(0,0,0,0.07)', transition: 'transform 0.15s, box-shadow 0.15s' }}
              onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-4px)'; e.currentTarget.style.boxShadow = '0 8px 20px rgba(14,165,233,0.15)' }}
              onMouseLeave={e => { e.currentTarget.style.transform = 'none'; e.currentTarget.style.boxShadow = '0 2px 8px rgba(0,0,0,0.07)' }}>
              <img src={p.imageUrl} alt={p.name} style={{ width: '100%', height: 180, objectFit: 'cover' }} />
              <div style={{ padding: '12px 14px' }}>
                <p style={{ margin: '0 0 4px', fontWeight: 600, fontSize: 14, color: '#1E293B', lineHeight: 1.4 }}>{p.name}</p>
                <p style={{ margin: '0 0 6px', fontWeight: 700, fontSize: 16, color: '#0EA5E9' }}>{p.price?.toLocaleString()}원</p>
                {p.stock === 0
                  ? <span style={{ fontSize: 12, color: '#fff', background: '#94A3B8', borderRadius: 4, padding: '2px 8px' }}>품절</span>
                  : <span style={{ fontSize: 12, color: '#0EA5E9', background: '#E0F2FE', borderRadius: 4, padding: '2px 8px' }}>재고 {p.stock}개</span>
                }
              </div>
            </div>
          </Link>
        ))}
      </div>

      {/* 페이지네이션 */}
      <div style={{ display: 'flex', gap: 8, marginTop: 32, justifyContent: 'center', alignItems: 'center' }}>
        <button disabled={page === 0} onClick={() => setPage(p => p - 1)} style={page === 0 ? BTN_DISABLED : BTN}>이전</button>
        <span style={{ color: '#64748B', fontSize: 14 }}>{page + 1} / {totalPages || 1}</span>
        <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)} style={page >= totalPages - 1 ? BTN_DISABLED : BTN}>다음</button>
      </div>
    </div>
  )
}
