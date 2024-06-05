<template>
  <div>
    <el-form :inline="true" :model="queryParam" style="margin-top: 2em">
      <el-form-item label="渠道">
        <el-select v-model="queryParam.channel" clearable>
          <el-option
            v-for="item in channelList"
            :key="item.id"
            :label="item.channelName"
            :value="item.id">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="账号">
        <el-select v-model="queryParam.userInfoId" clearable>
          <el-option
            v-for="item in userIdList"
            :key="item.id"
            :label="item.userName"
            :value="item.id">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="使用时间">
        <el-date-picker
          v-model="queryParam.useDate"
          type="date"
          placeholder="选择日期"
          format="yyyy-MM-dd"
        >
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSubmit" size="small" round>查询</el-button>
        <el-button type="primary" @click="addTask" size="small" round>新建任务</el-button>
        <!--        <el-button type="primary" @click="getMsg" size="small" round>查询验证码</el-button>-->
      </el-form-item>
    </el-form>
    <div>
      <el-table
        :data="taskData"
        ref="multipleTable"
        border
        height="80vh"
        style="width: 100%; margin-top: 20px"
        :span-method="objectSpanMethod"
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          prop="taskId"
          label="任务Id"
          :width="80"
        >
        </el-table-column>
        <el-table-column
          prop="account"
          label="账号">
        </el-table-column>
        <el-table-column
          prop="channel"
          label="渠道"
          :width="80"
        >
          <template slot-scope="{ row }">
            <div>{{ channelObj[row.channel] }}</div>
          </template>
        </el-table-column>
        <el-table-column
          prop="userName"
          label="姓名">
        </el-table-column>
        <el-table-column
          prop="IDCard"
          label="身份证号">
        </el-table-column>
        <el-table-column
          prop="price"
          label="票价"
          :width="80"
        >
        </el-table-column>
        <el-table-column
          prop="useDate"
          label="使用时间">
        </el-table-column>
        <el-table-column
          :width="80"
          label="抢票结果">
          <template slot-scope="scope">
            <el-link class="el-icon-success" type="success" v-if="scope.row.done" :underline="false"></el-link>
            <el-link icon="el-icon-error" type="danger" v-if="!scope.row.done" :underline="false"></el-link>
          </template>
        </el-table-column>
        <el-table-column
          :width="80"
          label="支付结果">
          <template slot-scope="scope">
            <el-link class="el-icon-success" type="success" v-if="scope.row.payment&&scope.row.channel==0"
                     :underline="false"></el-link>
            <el-link icon="el-icon-error" type="danger" v-if="!scope.row.payment&&scope.row.channel==0"
                     :underline="false"></el-link>
            <p v-if="scope.row.channel!=0">--</p>
          </template>
        </el-table-column>
        <el-table-column
          prop="updateDate"
          label="过期时间">
          <template slot-scope="scope">
            {{ addDate(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column
          type="selection"
          width="55">
        </el-table-column>
        <el-table-column label="操作" prop="option">
          <template slot-scope="scope">
            <el-link
              type="primary" @click="getTask(scope.row.taskId)">编辑
            </el-link>
            <el-link
              type="danger" @click="deleteTask(scope.row.taskId)">删除
            </el-link>
            <el-link type="success" @click="pay" v-if="scope.row.channel==0||scope.row.channel==2">支付</el-link>
            <el-link
              type="danger" @click="init">重置
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
      style="height: 50em;overflow: unset"
    >
      <taskEditView @close="closeDialog" v-if="showDialog" :taskInfo="taskInfo" ref="taskEditView"></taskEditView>
    </el-dialog>
    <el-dialog
      :visible.sync="showPayDialog"
      style="height: 50em;overflow: unset"
      width="20%"
    >
      <div id="qrcodeImg" style="text-align: center" v-if="showPayPic"></div>
    </el-dialog>
    <audio
      ref="audio"
    >
      <source src="../../../static/ding.mp3" />
    </audio>
  </div>
</template>

<script>
import taskEditView from "./TaskEditView"
import QRCode from 'qrcodejs2';
import {get,post} from '../../request'


export default {
  name: "TaskView",
  components: {
    taskEditView
  },
  created() {
    this.currentUser = Date.now()
    this.initWebSocket()
  },
  mounted() {
    this.onSubmit()
    this.getUserIdList()
  },
  data() {
    return {
      queryParam: {
        channel: '',
        phone: '',
        useDate: ''
      },
      taskData: [],
      page: {
        pageNum: 1,
        pageSize: 30,
        total: 0
      },
      showDialog: false,
      msg: "",
      websocketCount: -1,
      //查询条件
      queryCondition: {
        type: "message",
      },
      ticketId: [],
      selectTicket: [],
      payInfo: {},
      payUrl: "",
      showPayDialog: false,
      taskInfo: {},
      number: 0,
      currentUser: '',
      channelObj: {
        "0": "科技馆",
        "1": "毛纪",
        "2": "故宫",
        "3": "国博",
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
        },
        {
          "id": 3,
          "channelName": "国博"
        }
      ],
      showPayPic: false,
      userIdList: []
    }
  },
  watch: {
    showPayDialog: function () {
      setTimeout(() => {
        this.qrcode();
      }, 1000)
      /*      //materielId为需要监听的data
            this.$nextTick(function () {
              if (this.showPayDialog) {
                //这里写方法
                this.qrcode(this.payUrl);
              }
            });*/
    },
  },
  methods: {
    initWebSocket() {
      //let ws = 'ws://8.140.16.73/api/pushMessage/' + this.currentUser
      let userName=localStorage.getItem("userName")
      let ws = `ws://localhost:8082/api/pushMessage/${userName}`
      this.websock = new WebSocket(ws)
      this.websock.onmessage = this.websocketOnMessage
      this.websock.onopen = this.websocketOnOpen
      this.websock.onerror = this.websocketOnError
      this.websock.onclose = this.websocketClose
    },
    // websocket连接后发送数据(send发送)
    websocketOnOpen() {
      console.log('websock已打开')
    },
    // 连接建立失败重连
    websocketOnError() {
      this.initWebSocket()
    },
    // 数据接收
    websocketOnMessage(e) {
      /*console.log(this.websock.readyState)
      this.$message.error(e.data)
      console.log(e.data)*/
      let res = JSON.parse(e.data)
      this.$notify({
        title: res.title,
        dangerouslyUseHTMLString: true,
        message: res.msg,
        duration: res.time,
        type: 'success'
      });
      if(res){
        this.doDing()
      }
      this.onSubmit()
      this.websocketClose()
    },
    // 数据发送
    websocketSend(Data) {
      this.websock.send(Data)
    },
    doDing(){
      this.$refs.audio.play();
    },
    // 关闭
    websocketClose(e) {
      this.initWebSocket()
      console.log('断开连接', e)
    },
    onSubmit() {
      this.queryParam.page = this.page
      post('/ticket/task/list',this.queryParam).then(res => {
        if (res.status != 0) {
          this.$notify.error({
            title: '查询失败',
            message: res.msg,
            duration: 2000
          });
        } else {
          this.taskData = res.data.list
          this.page.total = res.data.total
          this.page.pageSize = res.data.pageSize
        }
      })
    },
    getUserIdList() {
      let queryParam = {
        userName: '',
        account: ''
      }
      post('/ticket/account/list',queryParam).then(res => {
        if (res.status != 0) {
          this.$notify.error({
            title: '查询用户列表失败',
            message: res.msg,
            duration: 2000
          });
        } else {
          this.userIdList = res.data
        }
      })
    },
    addTask() {
      this.showDialog = true
      setTimeout(() => {
        this.$refs.taskEditView.getUserIdList()
      }, 1000)
    },
    mergeCol(id, rowIndex) {
      // 合并单元格
      // id：属性名
      // rowIndex：行索引值
      var idName = this.taskData[rowIndex][id]; // 获取当前单元格的值
      if (rowIndex > 0) {
        // 判断是不是第一行
        // eslint-disable-next-line eqeqeq
        if (this.taskData[rowIndex][id] != this.taskData[rowIndex - 1][id]) {
          // 先判断当前单元格的值是不是和上一行的值相等
          var i = rowIndex;
          var num = 0; // 定义一个变量i，用于记录行索引值并进行循环，num用于计数
          while (i < this.taskData.length) {
            // 当索引值小于table的数组长度时，循环执行
            if (this.taskData[i][id] === idName) {
              // 判断循环的单元格的值是不是和当前行的值相等
              i++; // 如果相等，则索引值加1
              num++; // 合并的num计数加1
            } else {
              i = this.taskData.length; // 如果不相等，将索引值设置为table的数组长度，跳出循环
            }
          }
          this.number = num
          return {
            rowspan: num, // 最终将合并的行数返回
            colspan: 1,
          };
        } else {
          return {
            rowspan: 0, // 如果相等，则将rowspan设置为0
            colspan: 1,
          };
        }
      } else {
        // 如果是第一行，则直接返回
        let i = rowIndex;
        let num = 0;
        while (i < this.taskData.length) {
          // 当索引值小于table的数组长度时，循环执行
          if (this.taskData[i][id] === idName) {
            i++;
            num++;
          } else {
            i = this.taskData.length;
          }
        }
        this.number = num
        return {
          rowspan: num,
          colspan: 1,
        };
      }
    },
    objectSpanMethod({row, column, rowIndex, columnIndex}) {
      // 合并单元格
      switch (
        columnIndex // 将列索引作为判断值
        ) {
        // 通过传递不同的列索引和需要合并的属性名，可以实现不同列的合并（索引0,1 指的是页面上的0,1）
        /*case 0:
          return this.mergeCol("account", rowIndex);
        case 1:
          return this.mergeCol("account", rowIndex);*/
        case 11:
          return this.mergeCol("taskId", rowIndex)
      }
    },
    handleSizeChange(val) {
      this.page.pageSize = val
      this.onSubmit()
    },
    handleCurrentChange(val) {
      this.page.pageNum = val
      this.onSubmit()
    },
    getTask(taskId) {
      get("/ticket/get/detail",
        {
          taskId: taskId
        }
      ).then(res => {
        this.taskInfo = res.data
        this.showDialog = true
        setTimeout(() => {
          this.$refs.taskEditView.getUserIdList();
          this.$refs.taskEditView.changeChannel();
          this.$refs.taskEditView.edit();
        }, 200)
      })
    },
    deleteTask(taskId) {
      get('/ticket/delete',{
        taskId: taskId
      }
      ).then(res => {
        if (res.status != 0) {
          this.$notify.error({
            title: '删除失败',
            message: res.msg,
            duration: 2000
          });
        } else {
          this.onSubmit()
          this.$notify.success({
            title: '删除成功',
            duration: 1000
          });
        }
      })
    },
    getMsg() {
      if (!this.queryParam.loginPhone) {
        this.$alert("请输入电话号")
        return
      }
      get("/ticket/phone/msg",
        {
          phoneNum: this.queryParam.loginPhone
        }
      ).then(res => {
        this.$alert(res.data.data, "短信内容", {
          confirmButtonText: '确定',
        })
      })
    },
    closeDialog() {
      this.showDialog = false
      this.onSubmit()
    },
    handleSelectionChange(val) {
      this.selectTicket = val
      let currentTaskId = 0
      for (let item of this.selectTicket) {
        if (currentTaskId == 0) {
          currentTaskId = item.taskId
        }
        /*if (item.done !== true) {
          this.$alert("该订单还未抢到票!")
          return;
        }*/
        if (item.taskId != currentTaskId) {
          this.$alert("只能选择同一个批次下的订单!")
          let lastElement = val[val.length - 1];
          this.$refs.multipleTable.toggleRowSelection(lastElement, false);
          val.pop()
          this.selectTicket = val
          break;
        }
      }
    },
    qrcode() {  // 前端根据 URL 生成微信支付二维码
      return new QRCode('qrcodeImg', {
        width: 250,
        height: 250,
        text: this.payUrl,
        colorDark: '#000',
        colorLight: '#fff'
      })
    },
    pay() {
      this.showPayPic = false
      this.payUrl = ""
      let payParam = {}
      let ticketList = []
      let taskDetailIds = []
      let childrenCount = 0
      for (let item of this.selectTicket) {
        taskDetailIds.push(item.id)
        payParam.taskId = item.taskId
        payParam.authorization = item.authorization
        payParam.date = item.useDate
        payParam.loginPhone = item.account
        payParam.userName = item.userName
        payParam.IDCard = item.IDCard
        payParam.orderId = item.orderId
        if (item.childrenTicket == true) {
          childrenCount += 1
        }
        let ticketItem = {}
        ticketItem.id = item.ticketId
        ticketList.push(ticketItem)
      }
      payParam.taskDetailIds = taskDetailIds
      payParam.ticketInfoList = ticketList
      payParam.childTicketNum = childrenCount
      payParam.ticketNum = this.selectTicket.length
      post("/ticket/pay", payParam).then(res => {
        if (res.data.status != 0) {
          this.$notify.error({
            title: '失败',
            message: res.data.msg,
            duration: 2000
          });
        } else {
          if (res.data.data && res.data.data != "") {
            this.showPayDialog = true;
            this.payUrl = res.data.data
            this.showPayDialog = true;
            this.showPayPic = true
            this.qrcode(this.payUrl)
          }
        }
      })
    },
    init() {
      if (this.selectTicket.length <= 0) {
        this.$alert("需勾选要重置的订单")
        return;
      }
      let req = []
      for (let item of this.selectTicket) {
        let payParam = {}
        payParam.id = item.id
        req.push(payParam)
      }
      post("ticket/init/task", req).then(res => {
        if (res.data.status != 0) {
          this.$notify.error({
            title: '失败',
            message: res.data.msg,
            duration: 2000
          });
        } else {
          this.$notify.success({
            title: '重置成功',
            duration: 1000
          });
          this.onSubmit()
        }
      })
    },
    addDate(row) {
      let nowDate = row.updateDate
      if (!nowDate || row.channel == 3) {
        return
      }
      let current = new Date(nowDate)
      let newDate
      if (row.channel == 2) {
        newDate = current.setMinutes(current.getMinutes() + 30)
      }
      if (row.channel == 0) {
        newDate = current.setMinutes(current.getMinutes() + 15)
      }
      let rd = new Date(newDate);
      let y = rd.getFullYear();
      let M = rd.getMonth() + 1;
      let d = rd.getDate();
      let H = rd.getHours();
      let m = rd.getMinutes()
      let s = rd.getSeconds()
      if (M < 10) {
        M = "0" + M;
      }
      if (d < 10) {
        d = "0" + d;
      }
      if (row.channel != 1) {
        return (y + "-" + M + "-" + d + " " + H + ":" + m + ":" + s)
      }
      return ""
    }
  }
}
</script>

<style scoped>

</style>
