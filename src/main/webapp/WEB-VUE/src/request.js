// 封装axios
import axios from 'axios'
// vuex
import store from './store/index'
import { Message } from 'element-ui';


// 使用自定义的配置文件发送请求
const instance = axios.create({
  baseURL: '',
  timeout: 80000
});
// 添加请求拦截器
instance.interceptors.request.use(function (config) {
  // 每次发送请求之前判断vuex中是否存在token
  // 如果存在，则统一在http请求的header都加上token，这样后台根据token判断你的登录情况
  // 即使本地存在token，也有可能token是过期的，所以在响应拦截器中要对返回状态进行判断
  const token = localStorage.getItem('authorization');
  if (token) {
    // 已经登录成功，统一添加token
    config.headers.Authorization = `${token}`
  }
  // token && (config.headers.Authorization = token);
  return config;
}, function (error) {
  // 对请求错误做些什么
  return Promise.reject(error);
});

// 添加响应拦截器
instance.interceptors.response.use(function (response) {
  if (response.status === 200) {
    return Promise.resolve(response);
  } else {
    return Promise.reject(response);
  }
}, function (error) {
  // 对响应错误做点什么
  if (error.response.status) {
    switch (error.response.status) {
      //用户名密码错误
      case 401:
        window.location.reload();
        break;
      case 403:
        // 清除token
        localStorage.removeItem('authorization');
        store.commit('delToken', null);
        window.location.reload();
        break;
      // 其他错误，直接抛出错误提示
      default:
    }
    return Promise.reject(error.response);
  }
});

/**
 * get方法，对应get请求
 * @param {String} url [请求的url地址]
 * @param {Object} params [请求时携带的参数]
 */
export function get(url, params) {
  return new Promise((resolve, reject) => {
    instance.get(url, {
      params: params
    })
      .then(res => {
        resolve(res.data);
      })
      .catch(err => {
        reject(err.data)
      })
  });
}
/**
 * post方法，对应post请求
 * @param {String} url [请求的url地址]
 * @param {Object} params [请求时携带的参数]
 */
export function post(url, params) {
  return new Promise((resolve, reject) => {
    instance.post(url, params)
      .then(res => {
        resolve(res.data);
      })
      .catch(err => {
        reject(err.data)
      })
  });
}
