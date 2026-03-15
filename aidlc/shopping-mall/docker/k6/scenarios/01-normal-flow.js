import http from 'k6/http'
import { check, sleep } from 'k6'
import { getToken } from '../lib/auth.js'

const USER_SERVICE    = __ENV.USER_SERVICE    || 'http://localhost:18083'
const PRODUCT_SERVICE = __ENV.PRODUCT_SERVICE || 'http://localhost:18081'
const ORDER_SERVICE   = __ENV.ORDER_SERVICE   || 'http://localhost:18082'
const REVIEW_SERVICE  = __ENV.REVIEW_SERVICE  || 'http://localhost:18084'

// VU별로 다른 계정 사용 (race condition 방지)
const USERS = [
  'alice@example.com',
  'bob@example.com',
  'carol@example.com',
  'dave@example.com',
  'eve@example.com',
]

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '1m',  target: 5 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed:   ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
  },
}

export default function () {
  // 1. 로그인 (VU 인덱스로 계정 분산)
  const email = USERS[(__VU - 1) % USERS.length]
  const token = getToken(email, 'password123')
  if (!token) return
  const authHeaders = { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } }

  // 2. 상품 목록 조회
  const productsRes = http.get(`${PRODUCT_SERVICE}/products?page=0&size=20`)
  check(productsRes, { 'products 200': r => r.status === 200 })
  const products = JSON.parse(productsRes.body).content || []
  if (products.length === 0) return
  sleep(0.5)

  // 3. 상품 상세 조회 (재고 있는 상품 선택)
  const inStock = products.filter(p => p.stock > 5)
  if (inStock.length === 0) return
  const product = inStock[Math.floor(Math.random() * inStock.length)]
  const detailRes = http.get(`${PRODUCT_SERVICE}/products/${product.id}`)
  check(detailRes, { 'product detail 200': r => r.status === 200 })
  sleep(0.5)

  // 4. 장바구니 추가
  const addRes = http.post(
    `${ORDER_SERVICE}/cart/items`,
    JSON.stringify({ productId: product.id, quantity: 1 }),
    authHeaders
  )
  check(addRes, { 'cart add 200': r => r.status === 200 })
  sleep(0.5)

  // 5. 주문 생성 (장바구니가 비어있을 수 있으므로 재확인 후 시도)
  const cartCheck = http.get(`${ORDER_SERVICE}/cart`, authHeaders)
  const cartData = JSON.parse(cartCheck.body)
  if (!cartData.items || cartData.items.length === 0) {
    // 재추가
    http.post(
      `${ORDER_SERVICE}/cart/items`,
      JSON.stringify({ productId: product.id, quantity: 1 }),
      authHeaders
    )
    sleep(0.3)
  }
  const orderRes = http.post(`${ORDER_SERVICE}/orders`, null, authHeaders)
  check(orderRes, { 'order created 201': r => r.status === 201 })
  sleep(0.5)

  // 6. 주문 내역 조회
  const ordersRes = http.get(`${ORDER_SERVICE}/orders`, authHeaders)
  check(ordersRes, { 'orders 200': r => r.status === 200 })
  sleep(0.5)

  // 7. 리뷰 목록 조회
  const reviewsRes = http.get(`${REVIEW_SERVICE}/reviews?productId=${product.id}`)
  check(reviewsRes, { 'reviews 200': r => r.status === 200 })

  sleep(1)
}
