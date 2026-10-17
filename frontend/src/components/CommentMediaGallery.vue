<template>
  <view v-if="files.length" class="comment-media-grid">
    <view v-for="file in files" :key="file.id" class="comment-media-item">
      <template v-if="file.available !== false">
        <wd-img :src="mediaUrl(file.url)" width="100%" height="88px" radius="8px" mode="aspectFit" @click="preview(file)" />
        <view class="row media-caption"><wd-tag v-if="file.contentType === 'image/gif'" plain>GIF</wd-tag><wd-button type="text" size="small" :icon="isFavorite(file) ? 'star-filled' : 'star'" :loading="!!favoriteBusy[file.id]" :disabled="restricted" @click="setFavorite(file, !isFavorite(file))">{{ isFavorite(file) ? '已收藏' : '收藏' }}</wd-button></view>
      </template>
      <view v-else class="muted unavailable-media">图片已不可用</view>
    </view>
  </view>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { mediaUrl } from '../lib/api'
import { favoriteBusy, isFavorite, setFavorite } from '../lib/commentMedia'
const props = defineProps({ files: { type: Array, default: () => [] }, restricted: Boolean })
function preview(file) { uni.previewImage({ urls: props.files.filter(x => x.available !== false).map(x => mediaUrl(x.url)), current: mediaUrl(file.url) }) }
</script>
<style scoped>
.comment-media-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; margin-top: 12px; }
.comment-media-item { min-width: 0; }
.media-caption { justify-content: center; gap: 3px; flex-wrap: wrap; }
.unavailable-media { padding: 20px 4px; text-align: center; }
</style>
