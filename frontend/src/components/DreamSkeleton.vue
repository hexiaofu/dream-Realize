<template>
  <view class="dream-skeleton stagger">
    <view v-for="index in count" :key="index" class="skeleton-card">
      <view class="skeleton skeleton-title" />
      <view class="row"><view class="skeleton skeleton-tag" /><view class="skeleton skeleton-tag" /></view>
      <view class="skeleton skeleton-line" /><view class="skeleton skeleton-line is-short" />
      <view class="row skeleton-footer"><view class="skeleton skeleton-avatar" /><view class="skeleton skeleton-name" /></view>
    </view>
  </view>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
defineProps({ count: { type: Number, default: 2 } })
</script>
<style scoped>
/* 首次加载时的卡片骨架，流光扫过表示内容即将出现。 */
.skeleton-card { padding: 20px; margin-bottom: 16px; border-radius: 20px; background: rgba(255, 255, 255, .26); border: 1px solid rgba(255, 255, 255, .55); box-shadow: 0 10px 30px rgba(84, 62, 190, .06); }
.skeleton-card > * + * { margin-top: 12px; }
.skeleton-title { width: 70%; height: 20px; }
.skeleton-tag { width: 64px; height: 20px; border-radius: 999px; }
.skeleton-line { height: 14px; }
.skeleton-line.is-short { width: 55%; }
.skeleton-footer { padding-top: 6px; }
.skeleton-avatar { width: 28px; height: 28px; border-radius: 50%; }
.skeleton-name { width: 90px; height: 14px; }
</style>
