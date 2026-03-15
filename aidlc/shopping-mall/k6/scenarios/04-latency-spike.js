import http from 'k6/http'
import { sleep } from 'k6'

const PRODUCT_SERVICE = __ENV.PRODUCT_SERVICE || 'http://localhost:18081'
const ORDER_SERVICE   = __ENV.ORDER_SERVICE   || 'http://localhost:18082'
const USER_SERVICE    = __ENV.USER_SERVICE    || 'http://localhost:18083'
const REVIEW_SERVICE  = __ENV.REVIEW_SERVICE  || 'http://localhost:18084'

const SLOW_ENDPOINTS = [
  `${PRODUCT_SERVICE}/simulate/slow`,
  `${ORDER_SERVICE}/simulate/slow`,
  `${USER_SERVICE}/simulate/slow`,
  `${REVIEW_SERVICE}/simulate/slow`,
]

// 예상 알람: HighLatency (HTTP p99 > 2000ms)
export const options = {
  scenarios: {
    normal_traffic: {
      executor: 'constant-vus',
      vus: 6,
      duration: '3m',
      exec: 'normalFlow',
    },
    slow_traffic: {
      executor: 'constant-vus',
      vus: 4,
      duration: '3m',
      exec: 'slowFlow',
    },
  },
}

export function normalFlow() {
  http.get(`${PRODUCT_SERVICE}/products`)
  sleep(1)
}

export function slowFlow() {
  const endpoint = SLOW_ENDPOINTS[Math.floor(Math.random() * SLOW_ENDPOINTS.length)]
  http.get(endpoint)
  sleep(1)
}
