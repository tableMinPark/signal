import axios from 'axios'

// url 호출 시 기본 값 셋팅
const api = axios.create({
  headers: { 'Content-type': 'application/json' }, // data type
})

// Add a request interceptor
api.interceptors.request.use(
  (config) => {
    const accessToken = sessionStorage.getItem('accessToken')
    if (accessToken && !config.headers.Authorization) {
      config.headers.Authorization = 'Bearer ' + accessToken
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(async function (response) {
  console.log(response)
  const { config, data } = response
  if (data.header.code === '801') {
    const originalRequest = config
    const refreshToken = sessionStorage.getItem('refreshToken')
    await axios
      .post(
        process.env.REACT_APP_API_URL + `/auth/refresh`,
        {},
        {
          headers: { RefreshToken: `Bearer ${refreshToken}` },
        }
      )
      .then((response) => {
        sessionStorage.setItem('accessToken', response.data.body.accessToken)
        sessionStorage.setItem('refreshToken', response.data.body.refreshToken)
        originalRequest.headers.Authorization = `Bearer ${response.data.body.accessToken}`
        return axios(originalRequest)
      })
      .catch((error) => {
        if (error.response.data.header.code === '802' && sessionStorage.getItem('accessToken')) {
          alert('재 로그인이 필요합니다.')
          sessionStorage.removeItem('accessToken')
          sessionStorage.removeItem('refreshToken')
          sessionStorage.removeItem('userEmail')
          sessionStorage.removeItem('username')
          sessionStorage.removeItem('nickname')
          sessionStorage.removeItem('userSeq')
          localStorage.removeItem('refreshToken')
          window.location.href = '/'
        }
      })
  }
})

export default api
