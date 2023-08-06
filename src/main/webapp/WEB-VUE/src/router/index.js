import Vue from 'vue'
import Router from 'vue-router'
import taskView from '../components/TaskView'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/home',
      name: 'taskView',
      component: taskView
    }
  ]
})
