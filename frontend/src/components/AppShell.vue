<template>
  <wd-config-provider :theme-vars="theme">
    <wd-navbar :title="title" :left-arrow="back" :bordered="false" safe-area-inset-top @click-left="returnBack">
      <template v-if="!back" #left><wd-icon name="heart" size="22px" color="#238478" /></template>
    </wd-navbar>
    <view class="page-body"><slot /></view>
    <wd-tabbar v-if="active" :model-value="active" fixed safe-area-inset-bottom bordered @change="switchPage">
      <wd-tabbar-item name="discover" title="发现" icon="home" />
      <wd-tabbar-item name="stories" title="圆梦" icon="heart" />
      <wd-tabbar-item name="publish" title="发布" icon="add-circle" />
      <wd-tabbar-item name="inbox" title="消息" icon="chat" />
      <wd-tabbar-item name="me" title="我的" icon="user" />
    </wd-tabbar>
  </wd-config-provider>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
defineProps({ title: { type: String, default: '圆梦' }, active: String, back: Boolean })
const theme = { colorTheme: '#238478', colorSuccess: '#238478', buttonPrimaryBgColor: '#238478', tabbarActiveColor: '#238478', tabsNavLineBgColor: '#238478', cardRadius: '16px', cardMargin: '0 0 16px', cardTitleFs: '17px', cellGroupRadius: '16px' }
function switchPage({ value }) { uni.reLaunch({ url: `/pages/${value}/index` }) }
function returnBack() { if (getCurrentPages().length > 1) uni.navigateBack(); else uni.reLaunch({ url: '/pages/discover/index' }) }
</script>
