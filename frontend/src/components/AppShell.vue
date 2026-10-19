<template>
  <wd-config-provider :theme-vars="theme">
    <view class="dream-sky">
      <view class="sky-aurora is-violet" /><view class="sky-aurora is-pink" /><view class="sky-aurora is-gold" /><view class="sky-aurora is-blue" /><view class="sky-aurora is-rose" />
      <view v-for="star in stars" :key="star.id" class="sky-star" :style="star.style" />
    </view>
    <wd-navbar :title="title" :left-arrow="back" :bordered="false" safe-area-inset-top custom-class="dream-navbar" @click-left="returnBack">
      <template v-if="!back" #left><view class="brand-mark"><wd-icon name="heart-filled" size="16px" color="#fff" /></view></template>
    </wd-navbar>
    <view class="page-body"><slot /></view>
    <wd-tabbar v-if="active" :model-value="active" fixed :bordered="false" safe-area-inset-bottom custom-class="dream-tabbar" @change="switchPage">
      <wd-tabbar-item name="discover" title="发现" icon="home" />
      <wd-tabbar-item name="stories" title="圆梦" icon="star" />
      <wd-tabbar-item name="publish" title="发布">
        <template #icon><view class="publish-orb" :class="{ 'is-active': active === 'publish' }"><wd-icon name="add" size="24px" color="#fff" /></view></template>
      </wd-tabbar-item>
      <wd-tabbar-item name="inbox" title="消息" icon="chat" />
      <wd-tabbar-item name="me" title="我的" icon="user" />
    </wd-tabbar>
  </wd-config-provider>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { theme } from '../lib/theme'
defineProps({ title: { type: String, default: '圆梦' }, active: String, back: Boolean })
// 固定的星点坐标，避免每次渲染跳动。
const stars = [[8, 18, 4, 0], [22, 62, 3, 1.2], [37, 30, 5, .6], [52, 74, 3, 2], [64, 14, 4, 1.6], [78, 48, 6, .3], [90, 26, 3, 2.4], [14, 86, 3, 1], [46, 96, 4, 2.8], [84, 88, 3, .9], [70, 120, 4, 1.8], [28, 130, 3, 2.2], [12, 260, 3, .4], [60, 330, 4, 1.4], [88, 420, 3, 2.6], [34, 500, 4, .8], [74, 580, 3, 1.9], [18, 660, 4, 2.3]]
  .map(([left, top, size, delay], id) => ({ id, style: `left:${left}%;top:${top}px;width:${size}px;height:${size}px;animation-delay:${delay}s` }))
function switchPage({ value }) { uni.reLaunch({ url: `/pages/${value}/index` }) }
function returnBack() { if (getCurrentPages().length > 1) uni.navigateBack(); else uni.reLaunch({ url: '/pages/discover/index' }) }
</script>
<style scoped>
/* 顶部极光与星点：固定在内容背后，缓慢漂移、闪烁。 */
.dream-sky { position: fixed; z-index: 0; top: 0; left: 0; right: 0; bottom: 0; overflow: hidden; pointer-events: none;
  background: linear-gradient(170deg, #DCD0FF 0%, #EFE6FF 38%, #FFE3EF 70%, #E6DCFF 100%); }
.sky-aurora { position: absolute; border-radius: 50%; filter: blur(48px); opacity: .85; animation: aurora-drift 14s ease-in-out infinite alternate; }
.sky-aurora.is-violet { width: 300px; height: 240px; top: -80px; left: -80px; background: #B9A8FF; }
.sky-aurora.is-pink { width: 260px; height: 220px; top: -40px; right: -90px; background: #FFC2DA; animation-duration: 18s; }
.sky-aurora.is-gold { width: 200px; height: 160px; top: 34%; left: 30%; background: #FFE2B0; opacity: .5; animation-duration: 22s; }
.sky-aurora.is-blue { width: 260px; height: 220px; top: 55%; left: -100px; background: #A9C4FF; opacity: .45; animation-duration: 20s; }
.sky-aurora.is-rose { width: 280px; height: 240px; bottom: -80px; right: -90px; background: #E3B5FF; opacity: .5; animation-duration: 24s; }
.sky-star { position: absolute; border-radius: 50%; background: #fff; box-shadow: 0 0 8px 2px rgba(155, 107, 255, .55); animation: dream-twinkle 3s ease-in-out infinite; }
@keyframes aurora-drift { from { transform: translate(0, 0) scale(1); } to { transform: translate(30px, 20px) scale(1.15); } }

:deep(.dream-navbar) { background: transparent !important; position: relative; z-index: 2; }
:deep(.dream-navbar .wd-navbar__title) { font-weight: 700; letter-spacing: 1px; animation: dream-rise .5s var(--dream-ease) backwards; }
.brand-mark { display: flex; align-items: center; justify-content: center; width: 30px; height: 30px; border-radius: 50%; background: var(--dream-gradient); box-shadow: 0 6px 16px rgba(110, 86, 248, .4); animation: brand-beat 2.4s ease-in-out infinite; }
@keyframes brand-beat { 0%, 100% { transform: scale(1); } 15% { transform: scale(1.14); } 30% { transform: scale(.96); } 45% { transform: scale(1.06); } 60% { transform: scale(1); } }

/* 标签栏：毛玻璃底，选中图标弹跳，中间发布按钮浮起并发光。 */
/* 标签栏放在弹层遮罩（z-index 10）之下，弹出选择器时被遮住。 */
:deep(.dream-tabbar) { z-index: 9 !important; background: rgba(255, 255, 255, .5) !important; backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 -6px 24px rgba(84, 62, 190, .08) !important; border-radius: 22px 22px 0 0; }
/* 宽屏下标签栏与内容同宽居中。 */
@media (min-width: 760px) { :deep(.dream-tabbar) { left: 0 !important; right: 0 !important; max-width: 720px; margin: 0 auto; border-radius: 22px 22px 0 0; } }
:deep(.dream-tabbar .wd-tabbar-item__body) { transition: transform .35s var(--dream-spring); }
:deep(.dream-tabbar .wd-tabbar-item:active .wd-tabbar-item__body) { transform: scale(.86); }
:deep(.dream-tabbar .wd-tabbar-item__body-icon.is-active) { animation: dream-bounce .6s var(--dream-spring) both; }
:deep(.dream-tabbar .wd-tabbar-item__body-title.is-active) { font-weight: 650; }
.publish-orb { display: flex; align-items: center; justify-content: center; width: 46px; height: 46px; margin-top: -22px; margin-bottom: 2px; border-radius: 50%;
  background: var(--dream-gradient); border: 4px solid #fff; box-shadow: 0 8px 20px rgba(110, 86, 248, .45); animation: orb-glow 2.6s ease-in-out infinite; transition: transform .35s var(--dream-spring); }
.publish-orb.is-active { transform: scale(1.08); border-color: #FFE7B8; }
@keyframes orb-glow { 0%, 100% { box-shadow: 0 8px 20px rgba(110, 86, 248, .45), 0 0 0 0 rgba(155, 107, 255, .35); } 50% { box-shadow: 0 8px 20px rgba(110, 86, 248, .45), 0 0 0 10px rgba(155, 107, 255, 0); } }
</style>
