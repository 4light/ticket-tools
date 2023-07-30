<template>
  <div>
    <el-form :inline="true" :model="queryParam" style="margin-top: 2em">
      <el-form-item label="渠道">
        <el-input v-model="queryParam.channel" placeholder="渠道"></el-input>
      </el-form-item>
      <el-form-item label="电话号">
        <el-input v-model="queryParam.loginPhone" placeholder="电话号" clearable></el-input>
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
      </el-form-item>
    </el-form>
    <div>
      <el-table
        :data="taskData"
        :span-method="objectSpanMethod"
        border
        height="80vh"
        style="width: 100%; margin-top: 20px">
        <el-table-column
          prop="loginPhone"
          label="登陆手机号">
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
          prop="useDate"
          label="使用时间">
        </el-table-column>
        <el-table-column
          label="抢票结果">
          <template slot-scope="scope">
            <el-link class="el-icon-success" type="success" v-if="scope.row.done" :underline="false"></el-link>
            <el-link icon="el-icon-error" type="danger" v-if="!scope.row.done" :underline="false"></el-link>
          </template>
        </el-table-column>
        <el-table-column
          label="支付结果">
          <template slot-scope="scope">
            <el-link class="el-icon-success" type="success" v-if="scope.row.payment" :underline="false"></el-link>
            <el-link icon="el-icon-error" type="danger" v-if="!scope.row.payment" :underline="false"></el-link>
          </template>
        </el-table-column>
        <el-table-column
          prop="updateDate"
          label="过期时间">
        </el-table-column>
        <el-table-column label="操作">
          <template slot-scope="scope">
            <el-button
              round
              size="mini"
              title="已支付"
              type="primary" @click="updateState(scope.row,true)">已支付
            </el-button>
            <el-button
              round
              size="mini"
              title="未支付"
              type="primary" @click="updateState(scope.row,false)">未支付
            </el-button>
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
      <taskEditView @close="closeDialog" v-if="showDialog"></taskEditView>
    </el-dialog>
  </div>
</template>

<script>
import taskEditView from "./TaskEditView"
import axios from "axios";

export default {
  name: "TaskView",
  components: {
    taskEditView
  },
  mounted() {
    this.onSubmit()
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
      showDialog: false
    }
  },
  methods: {
    onSubmit() {
      this.queryParam.page=this.page
      axios.post("/ticket/task/list",this.queryParam).then(res=>{
        if(res.data.status!=0){
          this.$notify.error({
            title:'查询失败',
            message: res.data.msg,
            duration: 2000
          });
        }else{
          this.taskData=res.data.data.list
          this.page.total=res.data.data.total
          this.page.pageSize=res.data.data.pageSize
        }
      })
    },
    addTask() {
      this.showDialog = true
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
        case 0:
          return this.mergeCol("loginPhone", rowIndex);
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
    updateState(row,res){
      row.payment=res
      axios.post("/ticket/update/taskInfo",row).then(res=>{
        if(res.data.status!=0){
          this.$notify.error({
            title:'更新失败',
            message: res.data.msg,
            duration: 2000
          });
        }else{
          this.$notify.success({
            title:'更新成功',
            duration: 1000
          });
        }
      })
    },
    closeDialog(){
      this.showDialog=false
      this.onSubmit()
    }
  }
}
</script>

<style scoped>

</style>
