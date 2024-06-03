import Cookies from 'js-cookie'

const state = {
  collapsed: Cookies.get('sideMenuStatus') === 1 ? true : false,
  user_id: Cookies.get('user_id'),
  user_name: localStorage.getItem('userName'),
  user_role:localStorage.getItem('role')
}

const getters = {
  collapsed: (state) => state.collapsed,
  user_id: (state) => state.user_id,
  user_name: (state) => state.user_name,
  user_role: (state) => state.user_role
}

const mutations = {
  taggleSideMenu (state) {
    state.collapsed = !state.collapsed
    if (state.collapsed) {
      Cookies.set('sideMenuStatus', 1)
      state.collapsed = true
    } else {
      Cookies.set('sideMenuStatus', 0)
      state.collapsed = false
    }
  },
  openSideMenu (state) {
    state.collapsed = false
    Cookies.set('sideMenuStatus', 0)
  },
  closeSideMenu (state) {
    state.collapsed = true
    Cookies.set('sideMenuStatus', 1)
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations
}
