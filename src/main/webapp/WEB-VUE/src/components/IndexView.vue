<template>
  <el-tabs :tab-position="tabPosition" class="tabStyle">
<!--    <el-tab-pane label="用户管理">
      <userView></userView>
    </el-tab-pane>-->
    <el-tab-pane label="任务管理">
      <taskView></taskView>
    </el-tab-pane>
  </el-tabs>
</template>

<script>
import userView from "./UserView"
import taskView from "./TaskView"

export default {
  name: "indexView",
  components: {
    taskView,
    userView
  },
  data() {
    return {
      tabPosition: 'top'
    }
  }
}
</script>

<style>
 .tabStyle .el-tabs__header{
  margin-bottom: unset;
}
</style>
