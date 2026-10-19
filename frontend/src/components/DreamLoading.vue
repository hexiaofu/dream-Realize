<template>
  <view class="dream-loading" :class="{ 'is-inline': inline }">
    <view class="loading-orbit">
      <view class="loading-moon" />
      <view class="loading-ring"><view class="loading-star" /></view>
      <view class="loading-ring is-reverse"><view class="loading-star is-pink" /></view>
    </view>
    <view v-if="text" class="loading-text">{{ text }}<text class="loading-dots"><text>.</text><text>.</text><text>.</text></text></view>
  </view>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
defineProps({ text: { type: String, default: '梦想正在赶来' }, inline: Boolean })
</script>
<style scoped>
/* 圆梦加载：月亮呼吸，两颗星星反向环绕。 */
.dream-loading { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 14px; padding: 28px 0; animation: dream-rise .45s var(--dream-ease) backwards; }
.dream-loading.is-inline { flex-direction: row; padding: 12px 0; gap: 10px; }
.loading-orbit { position: relative; width: 52px; height: 52px; }
.is-inline .loading-orbit { width: 30px; height: 30px; }
.loading-moon { position: absolute; top: 30%; right: 30%; bottom: 30%; left: 30%; border-radius: 50%; background: var(--dream-gradient); box-shadow: 0 0 18px rgba(110, 86, 248, .55); animation: dream-breathe 1.6s ease-in-out infinite; }
.loading-ring { position: absolute; top: 0; right: 0; bottom: 0; left: 0; border-radius: 50%; border: 1px dashed rgba(110, 86, 248, .25); animation: dream-spin 1.6s linear infinite; }
.loading-ring.is-reverse { top: 14%; right: 14%; bottom: 14%; left: 14%; animation-duration: 1.1s; animation-direction: reverse; border-color: rgba(255, 141, 184, .3); }
.loading-star { position: absolute; top: -4px; left: 50%; width: 8px; height: 8px; margin-left: -4px; border-radius: 50%; background: var(--dream-gold); box-shadow: 0 0 10px var(--dream-gold); }
.loading-star.is-pink { background: var(--dream-pink); box-shadow: 0 0 10px var(--dream-pink); width: 6px; height: 6px; top: -3px; margin-left: -3px; }
.loading-text { color: var(--dream-muted); font-size: 13px; letter-spacing: 1px; }
.loading-dots text { display: inline-block; animation: dream-twinkle 1.2s ease-in-out infinite; }
.loading-dots text:nth-child(2) { animation-delay: .2s; }
.loading-dots text:nth-child(3) { animation-delay: .4s; }
</style>
