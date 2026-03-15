import axios from 'axios'

const userApi = axios.create({ baseURL: 'http://localhost:18083' })

export const register = (data) => userApi.post('/auth/register', data)
export const login = (data) => userApi.post('/auth/login', data)

export default userApi
