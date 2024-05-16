<template>
  <div style="height:85vh">
    <el-form ref="form" :model="form" label-width="80px">
      <!--      <el-form-item label="手机号">
              <el-input v-model="form.loginPhone" style="width: 30%"></el-input>
            </el-form-item>
            <el-form-item label="请求头">
              <el-input v-model="form.auth" type="textarea" style="width: 30%"></el-input>
            </el-form-item>-->
      <el-form-item label="渠道">
        <el-select v-model="form.channel" @change="changeChannel">
          <el-option
            v-for="item in channelList"
            :key="item.id"
            :label="item.channelName"
            :value="item.id">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="场次" v-if="form.channel==2">
        <el-checkbox-group v-model="session">
          <el-checkbox label="0">上午</el-checkbox>
          <el-checkbox label="1">下午</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="账号">
        <el-select v-model="form.userInfoId">
          <el-option
            v-for="item in currentUserIdList"
            :key="item.id"
            :label="item.userName"
            :value="item.id">
          </el-option>
        </el-select>
      </el-form-item>
      <!--      <el-form-item label="场馆">
              <el-select v-model="form.venue">
                <el-option
                  v-for="item in venueList"
                  :key="item.id"
                  :label="item.venueName"
                  :value="item.id">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="场次">
              <el-select v-model="form.session">
                <el-option
                  v-for="item in sessionList"
                  :key="item.id"
                  :label="item.sessionName"
                  :value="item.id">
                </el-option>
              </el-select>
            </el-form-item>-->
      <el-form-item label="使用时间">
        <el-date-picker type="date" placeholder="选择日期" v-model="form.useDate"
                        format="yyyy-MM-dd"></el-date-picker>
      </el-form-item>
      <el-form-item label="添加用户" style="height: 55vh">
        <el-button type="primary" @click="addUser" round v-if="!isAddUser">添加</el-button>
        <el-input
          type="textarea"
          :rows="2"
          placeholder="请输入内容"
          v-model="userText"
          v-if="isAddUser"
          style="width: 50%"
          @input="ok"
        >
        </el-input>
        <!--      <el-button type="primary" @click="ok" v-if="isAddUser" round>确定</el-button>-->
        <div v-if="showUserList">
          <el-table :data="userList" style="overflow:auto;height: 52vh">
            <el-table-column
              lable="序号"
              type="index"
              width="50">
            </el-table-column>
            <el-table-column
              prop="userName"
              label="姓名">
            </el-table-column>
            <el-table-column
              prop="IDCard"
              label="身份证号">
            </el-table-column>
            <el-table-column label="操作">
              <template slot-scope="scope">
                <el-button
                  round
                  size="mini"
                  title="已支付"
                  type="primary" @click="deleteUser(scope.$index)">删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-form-item>
      <el-form-item>
        <el-button type="warning" @click="close" round style="margin-left: 31vw">取消</el-button>
        <el-button type="primary" @click="onSubmit" round >保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "TaskEditView",
  props: {
    taskInfo: {}
  },
  data() {
    return {
      form: {
        "userId": null,
        "channel": null,
        "venue": 1,
        "session": 23,
        "userInfoId": null,
        "source": 0
      },
      channelList: [
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
      userIdList: [],
      currentUserIdList: [],
      venueList: [
        {
          "id": 1,
          "venueName": "主展厅"
        }
      ],
      sessionList: [
        {
          "id": 23,
          "sessionName": "全天场"
        }
      ],
      isAddUser: false,
      userText: "",
      userList: [],
      showUserList: false,
      session: ["0", "1"]
    }
  },
  /* watch: {
     'userText': 'getNewData',
   },*/
  methods: {
    edit() {
      this.form = this.taskInfo
      this.userList = this.taskInfo.userList
      this.showUserList = true
    },
    addUser() {
      this.isAddUser = true
      this.userText = ""
    },
    ok() {
      let regex = /(.+?)\s+(.+)/g;
      let list = [];
      let match;
      while ((match = regex.exec(this.userText)) !== null) {
        let name = match[1];
        let id = match[2];
        let obj = {
          userName: name,
          IDCard: id.toUpperCase()
        };
        list.push(obj);
      }
      this.userList = this.userList.concat(list)
      this.isAddUser = false
      this.showUserList = true
    },
    onSubmit() {
      if (this.form.channel == 0 && this.userList.length > 15) {
        this.$alert("最多只能添加15条，请检查", "添加失败")
        return
      }
      if (this.form.channel == 2) {
        if (this.session.length == 1) {
          this.form.session = parseInt(this.session[0])
        } else {
          this.form.session = null
        }
      }
      this.form.userList = this.userList
      axios.post("/ticket/add/taskInfo", this.form).then(res => {
        if (res.data.status != 0) {
          this.$notify.error({
            title: '保存失败',
            message: res.data.msg,
            duration: 2000
          });
        } else {
          this.$notify.success({
            title: '保存成功',
            duration: 1000
          });
          this.close()
        }
      })
    },
    deleteUser(index) {
      this.userList.splice(index, 1)
    },
    close() {
      this.$emit("close")
    },
    getUserIdList() {
      let queryParam = {
        userName: '',
        account: ''
      }
      axios.post("/ticket/user/list", queryParam).then(res => {
        if (res.data.status != 0) {
          this.$notify.error({
            title: '查询失败',
            message: res.data.msg,
            duration: 2000
          });
        } else {
          this.userIdList = res.data.data
          this.changeChannel()
        }
      })
    },
    changeChannel() {
      let newUserIdList = []
      for (let o of this.userIdList) {
        if (o.channel == this.form.channel) {
          newUserIdList.push(o)
        }
      }
      this.currentUserIdList = newUserIdList
    }
  }
}
</script>

<style scoped>

</style>
