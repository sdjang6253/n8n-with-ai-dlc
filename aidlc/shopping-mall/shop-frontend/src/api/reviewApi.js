import axios from 'axios'

const reviewApi = axios.create({ baseURL: 'http://localhost:18084' })

export const getReviews = (productId, params) =>
  reviewApi.get('/reviews', { params: { productId, ...params } })

export default reviewApi
