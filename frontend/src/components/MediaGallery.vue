<template>
  <view v-if="media.length" class="actions">
    <view v-if="images.length" class="row">
      <wd-img v-for="(file, index) in images" :key="file.id" :src="mediaUrl(file.url)" width="88" height="88" radius="8" mode="aspectFill" lazy-load @click.stop="preview(index)" />
    </view>
    <template v-if="compact">
      <view v-for="file in videos" :key="file.id" class="muted actions"><wd-icon name="play-circle" /> 包含视频 · 点击查看</view>
    </template>
    <video v-for="file in compact ? [] : videos" :key="file.id" class="media-video actions" :src="mediaUrl(file.url)" controls :autoplay="false" :show-mute-btn="true" object-fit="contain" @error="notify('视频暂时无法播放，请检查网络或使用兼容格式')" />
  </view>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { computed } from 'vue'
import { mediaUrl, notify } from '../lib/api'
const props = defineProps({ media: { type: Array, default: () => [] }, compact: Boolean })
const images = computed(() => props.media.filter(file => file.type === 'IMAGE').slice(0, props.compact ? 3 : 9))
const videos = computed(() => props.media.filter(file => file.type === 'VIDEO'))
function preview(index) {
  uni.previewImage({ current: index, urls: props.media.filter(file => file.type === 'IMAGE').map(file => mediaUrl(file.url)) })
}
</script>
<style scoped>
/* 原生视频仅设置容器宽度，其他外观使用 Wot 组件。 */
.media-video { display: block; width: 100%; height: 220px; }
</style>
