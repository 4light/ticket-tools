// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import App from './App'
import VueRouter from 'vue-router'
import routers from './router/index'
import store from './store'
import Editor from 'bin-ace-editor'
import {traverseTree} from './utils/traverseTree'

import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import Vuex from 'vuex'

Vue.use(ElementUI, {size: 'small'})
Vue.component(Editor.name, Editor)
Vue.prototype.traverse = traverseTree;

Vue.use(VueRouter)
Vue.use(Vuex)
require('brace/mode/json')
require('brace/snippets/json')
require('brace/theme/chrome')

Vue.config.productionTip = false
const RouterConfig = {
  base: '/',
  mode: 'history',
  routes: routers
}
const router = new VueRouter(RouterConfig)
router.beforeEach((to, from, next) => {
  // 检查是否存在 token
  const authToken = localStorage.getItem("authorization");
  const role = localStorage.getItem("role");
  if (to.path === '/') {
    // 如果要访问的是登录页面，则直接放行
    if(authToken){
      next({
        path: "/task/list"
      });
    }else{
      next({
        path: '/login'
      });
    }
  }
  if (to.path === '/login'||to.path === '/') {
    // 如果要访问的是登录页面，则直接放行
    if(authToken){
      next({
        path: "/task/list"
      });
    }else{
      next();
    }
  } else {
    if (authToken) {
      // 如果存在 token，校验是否有权限
      if(to.meta.roles.includes(role)){
        next();
      }else{
        next({
          path: "/task/list"
        });
      }
    } else {
      // 如果不存在 token，则跳转到登录页，并将要访问的页面路径作为参数传递给登录页面
      next({
        path: '/login'
      });
    }
  }
});

document.addEventListener('DOMContentLoaded', () => {
  const app = new Vue({
    el: '#app',
    router: router,
    store: store,
    render: h => h(App)
  })
})
