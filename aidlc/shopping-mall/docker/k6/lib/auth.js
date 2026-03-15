import http from 'k6/http'

const USER_SERVICE = __ENV.USER_SERVICE || 'http://localhost:18083'

export function getToken(email, password) {
  const res = http.post(
    `${USER_SERVICE}/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' } }
  )
  if (res.status !== 200) {
    console.error(`Login failed: ${res.status} ${res.body}`)
    return null
  }
  return JSON.parse(res.body).accessToken
}
