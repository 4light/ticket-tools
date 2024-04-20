import Vue from 'vue'
import Router from 'vue-router'
import indexView from '../components/IndexView'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'indexView',
      component: indexView
    }
  ]
})
