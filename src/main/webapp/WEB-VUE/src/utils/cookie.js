import Cookies from 'js-cookie'

const TokenKey = 'ADMIN_DESIGN_KEY'

export function getToken () {
  return Cookies.get(TokenKey)
}

export function setToken (token) {
  return Cookies.set(TokenKey, token)
}

export function removeToken () {
  Cookies.remove('user_id')
  Cookies.remove('user_name')
  return true
}
