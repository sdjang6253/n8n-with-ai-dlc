import axios from 'axios'

const productApi = axios.create({ baseURL: 'http://localhost:18081' })

export const getProducts = (params) => productApi.get('/products', { params })
export const getProduct = (id) => productApi.get(`/products/${id}`)

export default productApi
