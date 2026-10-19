<template>
  <view class="guest-prompt">
    <view class="guest-orb"><view class="guest-ring" /><wd-icon :name="icon" size="30px" color="#fff" /></view>
    <view class="guest-title">{{ title }}</view>
    <view class="muted">{{ hint }}</view>
    <view class="actions"><wd-button block size="large" @click="go('/pages/auth/index')">登录 / 加入社区</wd-button></view>
  </view>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { go } from '../lib/api'
defineProps({ title: String, hint: String, icon: { type: String, default: 'heart-filled' } })
</script>
<style scoped>
/* 访客引导：漂浮的渐变光球与扩散光环。 */
.guest-prompt { padding: 48px 24px 24px; text-align: center; animation: dream-rise .6s var(--dream-ease) backwards; }
.guest-orb { position: relative; display: flex; align-items: center; justify-content: center; width: 76px; height: 76px; margin: 0 auto 20px; border-radius: 50%;
  background: var(--dream-gradient); box-shadow: 0 14px 30px rgba(110, 86, 248, .35); animation: dream-float 4s ease-in-out infinite; }
.guest-ring { position: absolute; top: -10px; right: -10px; bottom: -10px; left: -10px; border-radius: 50%; border: 2px solid rgba(155, 107, 255, .35); animation: guest-ripple 2.4s ease-out infinite; }
@keyframes guest-ripple { from { transform: scale(.8); opacity: 1; } to { transform: scale(1.4); opacity: 0; } }
.guest-title { font-size: 19px; font-weight: 700; margin-bottom: 6px; }
</style>
