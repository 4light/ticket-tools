<template>
  <div class="login-page">
    <div class="login-container">
      <h2 style="display: flex;justify-content: center;">登 录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">用户名</label>
          <input type="text" id="username" v-model="username" required>
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input type="password" id="password" v-model="password" required>
        </div>
        <button type="submit">Login</button>
      </form>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import store from '../../store/index'

export default {
  data() {
    return {
      username: '',
      password: '',
      errorMessage: ''
    };
  },
  methods: {
    async handleLogin() {
      axios.post("/ticket/auth/login",{
        userName: this.username,
        pwd: this.password
      }).then(res=>{
        store.commit("setToken",res.headers.authorization)
        localStorage.setItem("authorization",res.headers.authorization)
        store.commit("setUserInfo",res.data)
        this.$router.push("/task/list")
        this.$notify.success({
          title: '成功',
          message: "欢迎"+res.data.nickName,
          duration: 5000
        });
      }).catch(res=>{
        this.$notify.error({
          title: '失败',
          message: "用户名密码错误",
          duration: 5000
        });
      })
    }
  }
};
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-image: url('../../assets/img/login-background2.png'); /* 更新图片路径 */
}

.login-container {
  width: 100%;
  max-width: 400px;
  padding: 20px;
  background-color: rgba(249, 249, 249, 0.9); /* 半透明背景 */
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
}

.form-group input {
  width: 100%;
  padding: 8px;
  box-sizing: border-box;
}

button {
  width: 100%;
  padding: 10px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:hover {
  background-color: #0056b3;
}

.error {
  color: red;
  margin-top: 10px;
}
</style>
