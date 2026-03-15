import http from 'k6/http'
import { sleep } from 'k6'

const PRODUCT_SERVICE = __ENV.PRODUCT_SERVICE || 'http://localhost:18081'
const ORDER_SERVICE   = __ENV.ORDER_SERVICE   || 'http://localhost:18082'
const USER_SERVICE    = __ENV.USER_SERVICE    || 'http://localhost:18083'
const REVIEW_SERVICE  = __ENV.REVIEW_SERVICE  || 'http://localhost:18084'

const ERROR_ENDPOINTS = [
  `${PRODUCT_SERVICE}/simulate/error`,
  `${ORDER_SERVICE}/simulate/error`,
  `${USER_SERVICE}/simulate/error`,
  `${REVIEW_SERVICE}/simulate/error`,
]

// 예상 알람: IstioHigh5xxErrorRate (5xx 에러율 > 10%)
export const options = {
  scenarios: {
    normal_traffic: {
      executor: 'constant-vus',
      vus: 7,
      duration: '3m',
      exec: 'normalFlow',
    },
    error_traffic: {
      executor: 'constant-vus',
      vus: 3,
      duration: '3m',
      exec: 'errorFlow',
    },
  },
}

export function normalFlow() {
  http.get(`${PRODUCT_SERVICE}/products`)
  sleep(1)
}

export function errorFlow() {
  const endpoint = ERROR_ENDPOINTS[Math.floor(Math.random() * ERROR_ENDPOINTS.length)]
  http.get(endpoint)
  sleep(1)
}
