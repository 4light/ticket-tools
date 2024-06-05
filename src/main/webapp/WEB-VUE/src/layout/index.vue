<template>
  <div class="app-wrapper">
    <div class="side-container" :class="collapsed ? 'folded' : 'unfolded'">
      <div class="logo">
      </div>
      <SideMenu/>
    </div>
    <div class="main-container" :class="collapsed ? 'wider' : 'normal'">
      <div class="main-header">
        <HeaderBar/>
<!--        <TagsNav/>-->
      </div>
      <div class="main-content">
        <el-scrollbar wrap-class="scrollbar">
          <MainView/>
        </el-scrollbar>
      </div>
    </div>
  </div>
</template>

<script>
import {mapGetters, mapMutations} from 'vuex'
import HeaderBar from './components/HeaderBar/index'
import SideMenu from './components/SideMenu/index'
import TagsNav from './components/TagsView/index'
import MainView from './components/MainView/index'
import {get} from '../request'

const RESIZE_WIDTH = 1440

export default {
  name: 'Layout',
  components: {HeaderBar, SideMenu, TagsNav, MainView},
  computed: {
    ...mapGetters('app', ['collapsed', 'user_id', 'user_name']),
  },
  created () {
    this.handleResize()
  },
  beforeMount () {
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy () {
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    ...mapMutations('app', ['openSideMenu', 'closeSideMenu']),
    handleResize () {
      /*      const width = document.body.getBoundingClientRect().width
            if (width <= RESIZE_WIDTH) {
              this.closeSideMenu()
            } else {
              this.openSideMenu()
            } */
      this.closeSideMenu()
      this.getOptionList()
    },
    getOptionList(){/*
      get('/platform/common/loadAllOption').then(res => {
        if (res.status === 0) {
          this.$root.optionList = res.data
        } else {
          alert(res.data)
        }
      }).catch(error => {
        this.$message.error(error)
      })*/
    }
  }
}
</script>

<style lang="less">
@import "../assets/less/scroll-bar";

.app-wrapper {
  width: 100%;
  height: 100%;
  overflow: hidden;

  .side-container {
    float: left;
    height: 100vh;
    transition: width 0.5s;
    background-color: #263238;

    &.unfolded {
      width: 200px;

      .logo {
        width: 64px;
        height: 64px;
        padding: 10px;
        box-sizing: border-box;
      }
    }

    &.folded {
      width: 64px;

      .logo {
        width: 64px;
        height: 64px;
        padding: 10px;
        box-sizing: border-box;
      }
    }

    img {
      display: block;
      width: 100%;
      height: 100%;
    }
  }

  .main-container {
    float: left;
    height: 100vh;
    transition: width 0.5s;

    &.normal {
      width: calc(100% - 260px);
    }

    &.wider {
      width: calc(100% - 64px);
    }

    .main-content {
      .el-scrollbar {
        height: calc(100vh - 64px - 40px);
        .scroll-bar;

        .scrollbar {
          height: 100%;
          overflow-x: hidden;
        }
      }
    }
  }
}
</style>
