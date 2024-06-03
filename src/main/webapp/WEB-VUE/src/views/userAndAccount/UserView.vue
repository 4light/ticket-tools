<template>
  <div>
    <el-form :inline="true" :model="queryParam" style="margin-top: 2em">
      <el-form-item label="用户名">
        <el-input v-model="queryParam.userName" placeholder="用户名" clearable></el-input>
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
          label="用户名">
        </el-table-column>
        <el-table-column
          prop="role"
          label="角色">
        </el-table-column>
        <el-table-column
          label="是否有效">
          <template slot-scope="scope">
            <el-link class="el-icon-success" type="success" v-if="!scope.row.status" :underline="false"></el-link>
            <el-link icon="el-icon-error" type="danger" v-if="scope.row.status" :underline="false"></el-link>
          </template>
        </el-table-column>
        <el-table-column
          prop="createDate"
          label="创建时间">
        </el-table-column>
        <el-table-column label="操作" prop="option">
          <template slot-scope="scope">
            <el-link
              type="primary" @click="editUser(scope.row)">编辑
            </el-link>
            <el-link
              type="danger" @click="frozenUser(scope.row)" v-if="!scope.row.status">禁用
            </el-link>
            <el-link
              type="danger" @click="frozenUser(scope.row)" v-if="scope.row.status">启用
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
        <el-form-item label="名称">
          <el-input v-model="formData.nickName" class="inputStyle"></el-input>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="formData.role">
            <el-option
              v-for="item in roleList"
              :key="item.role"
              :label="item.roleName"
              :value="item.role">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="formData.userName" class="inputStyle"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="formData.pwd" class="inputStyle"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSubmit" round style="margin-left: 20vw">保存</el-button>
          <el-button type="warning" @click="closeForm" round>取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script>
import {get, post} from '../../request'

export default {
  name: "UserView",
  data() {
    return {
      queryParam: {
        userName: '',
        account: ''
      },
      roleList: [
        {
          "role": "admin",
          "roleName": "管理员"
        },
        {
          "role": "user",
          "roleName": "普通用户"
        }
      ],
      channelObj: {
        "0": "科技馆",
        "1": "毛纪",
        "2": "故宫",
        "3": "国博"
      },
      userData: [],
      page: {
        pageNum: 1,
        pageSize: 30,
        total: 0
      },
      showDialog: false,
      labelPosition: 'right',
      formData: {},
    }
  },
  mounted() {
    this.queryUser()
  },
  methods: {
    queryUser() {
      this.queryParam.page = this.page
      post('/ticket/user/list',this.queryParam).then(res => {
        if (res.status != 0) {
          this.$notify.error({
            title: '查询失败',
            message: res.msg,
            duration: 2000
          });
        } else {
          this.userData = res.data.list
          this.page.total = res.data.total
          this.page.pageSize = res.data.pageSize
        }
      })
    },
    addUser() {
      this.formData = {}
      this.showDialog = true

    },
    editUser(row) {
      this.showDialog = true
      this.formData = row
    },
    deleteUser(id) {
      post("/ticket/user/update",{
        id: id,
        yn:1
      }).then(res => {
        if (res.status != 0) {
          this.$notify.error({
            title: '失败',
            message: res.msg,
            duration: 2000
          });
        } else {
          this.showDialog = false
          this.queryUser()
        }
      })
    },
    frozenUser(row) {
      row.status = row.status == 0 ? 1 : 0;
      post("/ticket/user/update", row).then(res => {
        if (res.status != 0) {
          this.$notify.error({
            title: '失败',
            message: res.msg,
            duration: 2000
          });
        } else {
          this.queryUser()
        }
      })
    },
    handleSizeChange() {

    },
    handleCurrentChange() {

    },
    onSubmit() {
      if (!this.formData.id) {
        post("/ticket/user/add",this.formData).then(res => {
          if (res.status != 0) {
            this.$notify.error({
              title: '失败',
              message: res.msg,
              duration: 2000
            });
          } else {
            this.showDialog = false
            this.queryUser()
          }
        })
      } else {
        post("/ticket/user/update",this.formData).then(res => {
          if (res.status != 0) {
            this.$notify.error({
              title: '失败',
              message: res.msg,
              duration: 2000
            });
          } else {
            this.showDialog = false
            this.queryUser()
          }
        })
      }
    },
    closeForm() {
      this.showDialog = false
    }
  }
}
</script>

<style scoped>
.inputStyle {
  width: 50%;
}
</style>
