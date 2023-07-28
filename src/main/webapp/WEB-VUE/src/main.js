// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
/* eslint-disable */
import Vue from 'vue'
import App from './App'
import routers from './router'

import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'

Vue.config.productionTip = false
Vue.use(ElementUI, {size: 'small'})

/*const RouterConfig = {
  base: '/',
  mode: 'history',
  routes: routers
}
const router = new VueRouter(RouterConfig)*/

document.addEventListener('DOMContentLoaded', () => {
new Vue({
    el: '#app',
    router: routers,
    data: function () {
      return {
        optionList: {}
      }
    },
    render: h => h(App)
  })
})
