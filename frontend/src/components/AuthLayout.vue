<template>
  <wd-config-provider :theme-vars="authTheme">
    <view class="auth-sky">
      <view class="auth-moon" /><view class="auth-cloud c1" /><view class="auth-cloud c2" />
      <view v-for="star in stars" :key="star.id" class="auth-star" :style="star.style" />
    </view>
    <wd-navbar left-arrow :bordered="false" safe-area-inset-top custom-class="auth-navbar" @click-left="returnBack" />
    <view class="auth-layout">
      <view class="auth-brand"><view class="auth-logo"><wd-icon name="heart-filled" size="22px" color="#fff" /></view><text>圆梦</text></view>
      <view class="auth-slogan">把梦想说出来，就有人陪你一起实现</view>
      <wd-card custom-class="auth-card">
        <view class="auth-heading">{{ title }}</view>
        <slot />
      </wd-card>
      <view class="auth-alternative"><text class="muted">{{ alternativeHint }}</text><wd-button type="text" :disabled="busy" @click="switchPage">{{ alternativeLabel }}</wd-button></view>
    </view>
  </wd-config-provider>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { theme } from '../lib/theme'
const props = defineProps({ title: String, alternativeHint: String, alternativeLabel: String, alternativeUrl: String, busy: Boolean })
const authTheme = {
  ...theme, navbarColor: '#fff', cardMargin: '0', cardPadding: '28px 24px', cardRadius: '28px',
  cardShadowColor: '0 24px 60px rgba(36, 23, 94, 0.28)', cellGroupInsertMargin: '0', cellGroupInsertRadius: '16px',
  inputCellBg: 'rgba(255, 255, 255, .55)', inputBg: 'transparent', inputCellPadding: '16px', inputPadding: '14px',
  inputCellLabelWidth: '58px', inputFs: '14px', inputIconColor: '#9B8FD6', buttonLargeHeight: '50px'
}
const stars = Array.from({ length: 18 }, (_, id) => ({ id, style: `left:${(id * 37) % 100}%;top:${(id * 53) % 320}px;animation-delay:${(id % 6) * .4}s;width:${2 + id % 3}px;height:${2 + id % 3}px` }))
function switchPage() { if (!props.busy) uni.redirectTo({ url: props.alternativeUrl }) }
function returnBack() { if (props.busy) return; if (getCurrentPages().length > 1) uni.navigateBack(); else uni.reLaunch({ url: '/pages/discover/index' }) }
</script>
<style scoped>
/* 登录注册：整屏星夜，月亮与云朵缓慢漂移，表单卡片自下浮起。 */
.auth-sky { position: fixed; z-index: 0; top: 0; left: 0; right: 0; height: 100vh; overflow: hidden; pointer-events: none; background: linear-gradient(180deg, #24175E 0%, #4B33C9 34%, #8E62FF 58%, #F7F4FF 82%); }
.auth-moon { position: absolute; top: 46px; right: 18px; width: 56px; height: 56px; border-radius: 50%; background: radial-gradient(circle at 35% 35%, #FFF6DD, #FFD58A 60%, #FFB84D); box-shadow: 0 0 50px rgba(255, 214, 140, .7); animation: dream-float 6s ease-in-out infinite; }
.auth-cloud { position: absolute; height: 34px; border-radius: 999px; background: rgba(255, 255, 255, .16); filter: blur(2px); animation: cloud-drift 18s ease-in-out infinite alternate; }
.auth-cloud.c1 { top: 150px; left: -20px; width: 140px; } .auth-cloud.c2 { top: 230px; right: -30px; width: 180px; animation-duration: 24s; }
@keyframes cloud-drift { from { transform: translateX(0); } to { transform: translateX(40px); } }
.auth-star { position: absolute; border-radius: 50%; background: #fff; box-shadow: 0 0 6px #fff; animation: dream-twinkle 2.8s ease-in-out infinite; }
:deep(.auth-navbar) { background: transparent !important; position: relative; z-index: 2; }
.auth-layout { position: relative; z-index: 1; max-width: 440px; margin: 0 auto; padding: 12px 20px 40px; }
.auth-brand { display: flex; align-items: center; justify-content: center; gap: 10px; color: #fff; font-size: 24px; font-weight: 700; letter-spacing: 4px; animation: dream-rise .6s var(--dream-ease) backwards; }
.auth-logo { display: flex; align-items: center; justify-content: center; width: 42px; height: 42px; border-radius: 50%; background: rgba(255, 255, 255, .18); border: 1px solid rgba(255, 255, 255, .4); animation: dream-breathe 2.4s ease-in-out infinite; }
.auth-slogan { margin: 10px 0 26px; text-align: center; color: rgba(255, 255, 255, .82); font-size: 13px; letter-spacing: 1px; animation: dream-rise .6s var(--dream-ease) .1s backwards; }
/* 卡片内的声明弹层是 fixed 定位，卡片不能有 backdrop-filter / transform。 */
:deep(.auth-card) { background: rgba(255, 255, 255, .62) !important; border: 1px solid rgba(255, 255, 255, .6); transform: none !important; animation: dream-rise .7s var(--dream-ease) .18s backwards; }
:deep(.auth-card .wd-cell-group) { box-shadow: none; }
:deep(.auth-card .wd-input) { transition: box-shadow .3s ease, background-color .3s ease; border-radius: 16px; }
:deep(.auth-card .wd-input:focus-within) { box-shadow: 0 0 0 2px rgba(110, 86, 248, .35); background: #fff; }
.auth-heading { margin-bottom: 26px; font-size: 26px; line-height: 1.4; font-weight: 700; }
.auth-alternative { display: flex; align-items: center; justify-content: center; gap: 8px; margin-top: 20px; animation: dream-rise .6s var(--dream-ease) .3s backwards; }
</style>
