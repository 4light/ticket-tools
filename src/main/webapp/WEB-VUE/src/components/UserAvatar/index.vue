<template>
  <el-dropdown class="user-avatar-wrapper" @command="handleCommand">
    <div class="avatar-box">
      <div>{{ this.user_name }}
        <i class="el-icon-caret-bottom"/>
      </div>
    </div>
    <el-dropdown-menu slot="dropdown">
      <el-dropdown-item command="loginOut">退出登录</el-dropdown-item>
    </el-dropdown-menu>
  </el-dropdown>
</template>

<script>
import Avatar from '../../assets/img/avatar.png'
import {mapGetters, mapMutations} from 'vuex'

export default {
  name: 'UserAvatar',
  data () {
    return {
      avatarSrc: Avatar,
      userName:this.getUser
    }
  },
  computed: {
    ...mapGetters('app', ['user_id', 'user_name']),
    ...mapMutations(['getUser','delToken'])
  },
  methods: {
    handleCommand (command) {
      if (command === 'loginOut') {
        this.loginOut()
      }
    },
    loginOut () {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.delToken
        this.$router.push('/login')
      })
    }
  }
}
</script>

<style lang="less">
.user-avatar-wrapper {
  float: left;
  width: 100px;
  padding: 3px 0 3px 0px;
  border-left: solid 1px #ddd;
  cursor: pointer;

  .avatar-box {
    outline: none;
  }

  .el-avatar--small {
    display: inline-block;
    vertical-align: middle;
    width: 32px;
    height: 32px;
    line-height: 32px;
  }

  i {
    display: inline-block;
    vertical-align: middle;
    margin-left: 2px;
  }
}
</style>
