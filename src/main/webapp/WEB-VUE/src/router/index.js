import Vue from 'vue'
import Router from 'vue-router'
import taskView from '../components/TaskView'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'taskView',
      component: taskView
    }
  ]
})
