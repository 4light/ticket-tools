
import app from './modules/app'
import tagsView from './modules/tagsView'
import Vue from 'vue'
import Vuex from 'vuex'
Vue.use(Vuex);
// 用Vuex.Store对象用来记录token
const store = new Vuex.Store({
  modules: {
    app,
    tagsView
  },
  state: {
    // 存储token
    authorization:"",
    userName:"", // 可选,
    role:""
  },
  getters: {
    getToken(state){
      return localStorage.getItem('authorization');
    }
  },
  mutations: {
    // 修改token，并将token存入localStorage
    setToken(state,authorization) {
      state.authorization = authorization;
      localStorage.setItem('authorization', authorization);
    },
    delToken(state) {
      state.authorization = "";
      state.userName=""
      app.state.user_name="";
      localStorage.removeItem("authorization");
      localStorage.removeItem("userName");
      localStorage.removeItem("userInfo");
      localStorage.removeItem("role");
    },
    // 可选
    setUserInfo(state, userInfo) {
      state.userName = userInfo.nickName;
      state.role=userInfo.role
      app.state.user_name=userInfo.nickName;
      localStorage.setItem('userName',userInfo.nickName)
      localStorage.setItem('userInfo',userInfo)
      localStorage.setItem('role',userInfo.role)
    },
    getUser(state) {
      return state.userName||localStorage.getItem('userName');
    },
    getRole(state){
      return state.role||localStorage.getItem('role');
    }
  },

  actions: {
    // removeToken: (context) => {
    // context.commit('setToken')
    // }
  },
});

export default store;
