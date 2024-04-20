<template>
  <div>
    <el-form :inline="true" :model="queryParam" style="margin-top: 2em">
      <el-form-item label="用户名">
        <el-input v-model="queryParam.userName" placeholder="用户名"></el-input>
      </el-form-item>
      <el-form-item label="电话号">
        <el-input v-model="queryParam.phoneNum" placeholder="电话号" clearable></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="queryUser" size="small" round>查询</el-button>
        <el-button type="primary" @click="addUser" size="small" round>新建用户</el-button>
      </el-form-item>
    </el-form>
    <div>
      <el-table
        :data="userData"
        border
        height="80vh"
        style="width: 100%; margin-top: 20px"
      >
        <el-table-column
          prop="userName"
          label="姓名">
        </el-table-column>
        <el-table-column
          prop="phoneNum"
          label="手机号">
        </el-table-column>
        <el-table-column
          prop="channel"
          label="渠道">
        </el-table-column>
        <el-table-column
          prop="account"
          label="账号">
        </el-table-column>
        <el-table-column
          label="是否禁用">
          <template slot-scope="scope">
            <el-link class="el-icon-success" type="success" v-if="scope.row.yn" :underline="false"></el-link>
            <el-link icon="el-icon-error" type="danger" v-if="!scope.row.yn" :underline="false"></el-link>
          </template>
        </el-table-column>
        <el-table-column label="操作" prop="option">
          <template slot-scope="scope">
            <el-link
              type="primary" @click="editUser(scope.row.id)">编辑
            </el-link>
            <el-link
              type="danger" @click="frozenUser(scope.row)">禁用
            </el-link>
            <el-link
              type="danger" @click="deleteUser(scope.row.id)">删除
            </el-link>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        :current-page="page.pageNum"
        :page-size="30"
        :page-sizes="[30,50, 100]"
        :total="page.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    <el-dialog
      top="2vh"
      :visible.sync="showDialog"
      style="height: 50em;overflow: unset;"
    >
      <el-form :label-position="labelPosition" label-width="80px" :model="formData" style="margin-left: 5vw">
        <el-form-item label="渠道">
          <el-select v-model="formData.channel">
            <el-option
              v-for="item in channelList"
              :key="item.id"
              :label="item.channelName"
              :value="item.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="formData.userName" class="inputStyle"></el-input>
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="formData.account" class="inputStyle"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="formData.pwd" class="inputStyle"></el-input>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="formData.nickName" class="inputStyle"></el-input>
        </el-form-item>
        <el-form-item label="身份证号" v-if="formData.channel==2">
          <el-input v-model="formData.idCard" class="inputStyle"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSubmit" round style="margin-left: 20vw">创建</el-button>
          <el-button type="warning" @click="closeForm" round>取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "UserView",
  data() {
    return {
      queryParam: {
        userName: '',
        phoneNum: ''
      },
      channelList:[
        {
          "id": 0,
          "channelName": "科技馆"
        },
        {
          "id": 1,
          "channelName": "毛纪"
        },
        {
          "id": 2,
          "channelName": "故宫"
        }
      ],
      userData: [],
      page: {
        pageNum: 1,
        pageSize: 30,
        total: 0
      },
      showDialog: false,
      labelPosition: 'right',
      formData:{
      }
    }
  },
  mounted() {
    this.queryUser()
  },
  methods: {
    queryUser() {
      this.queryParam.page = this.page
      axios.post("/ticket/user/list", this.queryParam).then(res => {
        if (res.data.status != 0) {
          this.$notify.error({
            title: '查询失败',
            message: res.data.msg,
            duration: 2000
          });
        } else {
          this.taskData = res.data.data.list
          this.page.total = res.data.data.total
          this.page.pageSize = res.data.data.pageSize
        }
      })
    },
    addUser() {
      this.formData={}
      this.showDialog=true

    },
    editUser(id) {

    },
    deleteUser(id) {

    },
    frozenUser(id) {

    },
    handleSizeChange() {

    },
    handleCurrentChange() {

    },
    onSubmit(){

    },
    closeForm(){
      this.showDialog=false
    }
  }
}
</script>

<style scoped>
.inputStyle{
  width: 50%;
}
</style>
