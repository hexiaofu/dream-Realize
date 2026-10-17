<template>
  <wd-popup v-model="opened" position="bottom" :z-index="130" closable root-portal safe-area-inset-bottom custom-class="comment-media-picker" custom-style="max-width:752px;margin:0 auto;border-radius:20px 20px 0 0">
    <view class="picker-heading subheading">{{ panel === 'emoji' ? '表情' : panel === 'images' ? '图片 · ' + files.length + '/9' : '我的收藏' }}</view>
    <scroll-view scroll-y class="picker-scroll">
      <view class="picker-body">
        <wd-grid v-if="panel === 'emoji'" :column="6" :border="false" clickable>
          <wd-grid-item v-for="emoji in emojis" :key="emoji" use-slot @itemclick="chooseEmoji(emoji)"><text class="emoji-choice">{{ emoji }}</text></wd-grid-item>
        </wd-grid>
        <template v-if="panel === 'images'">
          <wd-upload ref="uploader" v-model:file-list="files" accept="image" multiple :limit="9" :max-size="10 * 1024 * 1024" :size-type="['original']" :extension="['.jpg', '.jpeg', '.png', '.gif', '.webp']" :disabled="disabled" :upload-method="upload" :before-choose="beforeChoose" @oversize="notify('每张图片不能超过 10 MB')" @chooseerror="chooseError">
            <template #preview-cover="{ file }"><view v-if="file.media" class="upload-favorite"><wd-button type="text" size="small" :disabled="disabled" :loading="!!favoriteBusy[file.media.id]" @click.stop="setFavorite(file.media, !isFavorite(file.media))">{{ isFavorite(file.media) ? '已收藏' : '收藏' }}</wd-button></view></template>
          </wd-upload>
          <view class="row between"><text class="muted">图片 / GIF，每张 10 MB</text><wd-button v-if="failed" type="text" size="small" :disabled="disabled || active > 0" @click="retry">重试上传</wd-button></view>
        </template>
        <template v-if="panel === 'favorites'">
          <view v-if="error" class="row"><wd-text :text="error" type="danger" /><wd-button type="text" size="small" @click="loadFavorites(true)">重试</wd-button></view>
          <view class="favorite-grid">
            <view v-for="file in favorites" :key="file.id" class="favorite-item">
              <wd-img v-if="file.available !== false" :src="mediaUrl(file.url)" width="100%" height="80px" radius="8px" mode="aspectFit" @click="preview(file)" />
              <view v-else class="muted unavailable-favorite">图片已不可用</view>
              <view class="row favorite-actions"><wd-button size="small" plain :disabled="disabled || file.available === false || selected(file)" @click="chooseFavorite(file)">{{ selected(file) ? '已选' : file.contentType === 'image/gif' ? '使用 GIF' : '使用' }}</wd-button><wd-button type="text" size="small" :loading="!!favoriteBusy[file.id]" :disabled="disabled" @click="removeFavorite(file)">移除</wd-button></view>
            </view>
          </view>
          <view v-if="loading" class="center"><wd-loading /></view>
          <view v-else-if="!error && !favorites.length" class="muted center">暂无收藏</view>
          <wd-button v-if="hasMore" block plain :loading="loading" @click="loadFavorites(false)">更多收藏</wd-button>
        </template>
      </view>
    </scroll-view>
    <view class="picker-footer"><wd-button block @click="opened = false">完成</wd-button></view>
  </wd-popup>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { mediaUrl, notify, request, requireLogin, uploadMedia } from '../lib/api'
import { favoriteBusy, favoriteStates, isFavorite, rememberFavorites, setFavorite } from '../lib/commentMedia'
const props = defineProps({ disabled: Boolean, visible: Boolean })
const emit = defineEmits(['change', 'blocked', 'emoji'])
const opened = ref(false), panel = ref('emoji'), files = ref([]), uploader = ref(), active = ref(0)
const favorites = ref([]), loading = ref(false), hasMore = ref(false), error = ref('')
const failed = computed(() => files.value.some(file => file.status === 'fail'))
const emojis = ['😀', '😊', '🥰', '😍', '🤩', '😎', '😂', '🤣', '🥹', '😭', '😮', '🤔', '😅', '😔', '😤', '🙃', '🫡', '🤗', '👍', '👎', '👏', '🙌', '🙏', '💪', '❤️', '💛', '💚', '💙', '🔥', '✨', '🎉', '🌈', '🌻', '🍀', '🎁', '🚀']
let disposed = false, page = 1, version = 0, taskId = 0
const tasks = new Map()
watch(files, value => emit('change', value.filter(file => file.status === 'success' && file.media).map(file => file.media)), { deep: true })
watch(() => active.value > 0 || files.value.some(file => file.status !== 'success'), value => emit('blocked', value), { immediate: true })
watch(() => props.visible, value => { if (!value) opened.value = false })
watch(() => files.value.filter(file => file.fromFavorite && favoriteStates[file.media.id] === false).map(file => file.media.id).join(','), removed => {
  if (removed) files.value = files.value.filter(file => !file.fromFavorite || favoriteStates[file.media.id] !== false)
})
function open(value) { if (props.disabled || !requireLogin()) return; panel.value = value; opened.value = true; if (value === 'favorites') loadFavorites(true) }
defineExpose({ open })
function chooseEmoji(emoji) { if (!props.disabled) { emit('emoji', emoji); opened.value = false } }
function beforeChoose({ resolve }) { resolve(!props.disabled && requireLogin()) }
function chooseError({ error }) { if (!String(error?.errMsg || '').includes('cancel')) notify('无法读取图片，请检查相册权限') }
function selected(file) { return files.value.some(item => item.media?.id === file.id) }
function chooseFavorite(file) {
  if (props.disabled || selected(file) || file.available === false) return
  if (files.value.length >= 9) { notify('评论最多 9 张图片'); return }
  files.value.push({ url: mediaUrl(file.url), name: file.url.split('/').pop(), status: 'success', media: file, fromFavorite: true })
  opened.value = false
}
function preview(file) { uni.previewImage({ urls: [mediaUrl(file.url)] }) }
async function removeFavorite(file) { if (await setFavorite(file, false)) await loadFavorites(true) }
async function loadFavorites(reset) {
  if (disposed || (!reset && loading.value)) return
  const current = ++version, next = reset ? 1 : page + 1
  loading.value = true; error.value = ''
  try {
    const data = await request('/media-favorites?page=' + next)
    if (disposed || current !== version) return
    favorites.value = reset ? data.items : [...favorites.value, ...data.items]
    rememberFavorites(data.items); hasMore.value = data.hasMore; page = next
  } catch (e) { if (!disposed && current === version) error.value = e.message }
  finally { if (!disposed && current === version) loading.value = false }
}
function upload(file, formData, options) {
  const key = ++taskId
  active.value++
  const finish = () => { tasks.delete(key); active.value-- }
  const task = uploadMedia(file, 'IMAGE', {
    success(media) { finish(); if (disposed) return; file.media = media; file.url = mediaUrl(media.url); file.name = media.url.split('/').pop(); options.onSuccess({ statusCode: 200, data: JSON.stringify({ code: 0, data: media }) }, file, formData) },
    error(error) { finish(); if (disposed) return; options.onError({ errMsg: error.message, message: error.message }, file, formData); notify(error) },
    progress(event) { if (!disposed) options.onProgress(event, file) }
  })
  tasks.set(key, task); return task
}
async function retry() {
  if (props.disabled || !requireLogin()) return
  files.value.filter(file => file.status === 'fail').forEach(file => { file.status = 'pending'; file.error = '' })
  await nextTick(); uploader.value?.submit()
}
// 移出草稿只解除本次选择，避免删除已收藏或被其他评论使用的文件。
onBeforeUnmount(() => { disposed = true; version++; tasks.forEach(task => task.abort()); tasks.clear() })
</script>
<style scoped>
.picker-heading { padding: 20px 48px 16px 20px; }
.picker-scroll { height: 40vh; max-height: 360px; }
.picker-body { padding: 8px 16px 16px; }
.picker-footer { padding: 12px 16px 16px; border-top: 1px solid #edf1ef; }
.emoji-choice { font-size: 26px; line-height: 40px; }
.favorite-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px 8px; margin-bottom: 12px; }
.favorite-item { min-width: 0; }
.favorite-actions { justify-content: center; flex-wrap: wrap; gap: 0; }
.unavailable-favorite { height: 80px; display: flex; align-items: center; text-align: center; }
.upload-favorite { position: absolute; bottom: 0; left: 0; right: 0; text-align: center; background: #ffffffeb; }
</style>
