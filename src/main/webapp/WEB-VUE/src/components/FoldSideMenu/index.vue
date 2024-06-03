<template>
  <div class="fold-side-menu" :class="{'vertical': collapsed}" @click="taggleSideMenu">
<!--    <SvgIcon class="icon" icon-class="home" />-->
    <el-button v-if="collapsed" :title="collapsed" icon="el-icon-s-unfold"></el-button>
    <el-button v-if="!collapsed" :title="collapsed" icon="el-icon-s-fold"></el-button>
  </div>
</template>

<script>
import { mapGetters, mapMutations } from 'vuex'
import SvgIcon from '../SvgIcon/index'

export default {
  name: 'FoldSideMenu',
  components: { SvgIcon },
  computed: {
    ...mapGetters('app', ['collapsed'])
  },
  methods: {
    ...mapMutations('app', ['taggleSideMenu'])
  }
}
</script>

<style lang="less">
.fold-side-menu {
  float: left;
  width: 44px;
  height: 33px;
  padding: 1px;
  cursor: pointer;
  &:hover {
    color: #409eff;
  }
  .icon {
    width: 22px;
    height: 22px;
    font-size: 100px;
    vertical-align: middle;
    transition: transform .2s ease;
  }
  &.vertical {
    .icon {
      transform: rotateZ(90deg);
      transition: transform .2s ease;
    }
  }
}
</style>
