<template>
  <router-view />
</template>

<script setup>
// 根组件：仅承载路由出口
</script>

<style>
html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', Arial, sans-serif;
  background-color: #f0f2f5;
}

* {
  box-sizing: border-box;
}
</style>
