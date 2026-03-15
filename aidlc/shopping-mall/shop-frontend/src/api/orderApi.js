import axios from 'axios'

const orderApi = axios.create({ baseURL: 'http://localhost:18082' })

orderApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export const getCart = () => orderApi.get('/cart')
export const addCartItem = (data) => orderApi.post('/cart/items', data)
export const updateCartItem = (productId, data) => orderApi.put(`/cart/items/${productId}`, data)
export const removeCartItem = (productId) => orderApi.delete(`/cart/items/${productId}`)
export const createOrder = () => orderApi.post('/orders')
export const getOrders = () => orderApi.get('/orders')
export const submitReview = (data) => orderApi.post('/reviews', data)

export default orderApi
