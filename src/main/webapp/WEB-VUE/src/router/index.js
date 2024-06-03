import Layout from '../layout'
import Login from '../views/userAndAccount/Login'

const routers = [
  {
    path: '/login',
    name: 'config',
    component: Login,
  },
  {
    path: '/manage',
    name: 'Layout',
    component: Layout,
    meta: {
      title: '管理中心',
      icon: 'el-icon-user',
      requiresAuth: true,
      roles: ['admin']
    },
    children: [
      {
        path: '/manage/user',
        name: 'user',
        component: () => import('../views/userAndAccount/UserView'),
        meta: {
          title: '用户管理',
          icon: 'el-icon-s-custom',
          fixed: true,
          requiresAuth: true,
          roles: ['admin']
        }
      },
      {
      path: '/manage/account',
      name: 'account',
      component: () => import('../views/userAndAccount/AccountView'),
      meta: {
        title: '购票账号管理',
        icon: 'el-icon-user',
        requiresAuth: true,
        roles: ['admin'],
        fixed: true
        }
      }
    ]
  },
  {
    path: '/task',
    name: 'config',
    component: Layout,
    meta: {
      title: '任务管理',
      icon: 'el-icon-s-order',
      requiresAuth: true,
      roles: ['admin','user']
    },
    children: [{
      path: '/task/list',
      name: 'list',
      component: () => import('../views/ticket/TaskView'),
      meta: {
        title: '任务列表',
        requiresAuth: true,
        roles: ['admin','user']
      }
    }]
  }
]


export default routers
