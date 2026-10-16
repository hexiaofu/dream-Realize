<template>
  <wd-cell-group border title="图片与视频（选填）">
    <wd-cell vertical title="图片" label="最多 9 张，每张 10 MB">
      <wd-upload ref="imageUpload" v-model:file-list="images" accept="image" multiple :limit="9" :max-size="10 * 1024 * 1024"
        :extension="['.jpg', '.jpeg', '.png', '.gif', '.webp']" :disabled="disabled" :before-choose="beforeChoose"
        :upload-method="uploadImage" @oversize="notify('每张图片不能超过 10 MB')" @chooseerror="chooseError" @remove="remove" />
    </wd-cell>
    <wd-cell vertical title="视频" label="最多 1 个，100 MB；MP4 / WebM">
      <wd-upload ref="videoUpload" v-model:file-list="videos" accept="video" :limit="1" :max-size="100 * 1024 * 1024"
        :extension="['.mp4', '.webm']" :max-duration="300" :disabled="disabled" :before-choose="beforeChoose"
        :upload-method="uploadVideo" @oversize="notify('每个视频不能超过 100 MB')" @chooseerror="chooseError" @remove="remove" />
    </wd-cell>
    <wd-cell v-if="failed" title="有附件上传失败">
      <wd-button size="small" plain :disabled="disabled || active > 0" @click="retry">重试上传</wd-button>
    </wd-cell>
  </wd-cell-group>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { mediaUrl, notify, request, requireLogin, uploadMedia } from '../lib/api'
const props = defineProps({ modelValue: { type: Array, default: () => [] }, disabled: Boolean })
const emit = defineEmits(['update:modelValue', 'blocked'])
const images = ref([]), videos = ref([]), imageUpload = ref(), videoUpload = ref(), active = ref(0)
const tasks = new Map()
let disposed = false, sequence = 0
const all = computed(() => [...images.value, ...videos.value])
const failed = computed(() => all.value.some(file => file.status === 'fail'))
watch(() => props.modelValue, value => {
  const current = all.value.filter(file => file.media).map(file => file.media.id).join(',')
  if (current === value.map(file => file.id).join(',')) return
  const restored = value.map(media => ({ url: mediaUrl(media.url), name: media.url.split('/').pop(), status: 'success', media }))
  images.value = restored.filter(file => file.media.type === 'IMAGE')
  videos.value = restored.filter(file => file.media.type === 'VIDEO')
}, { immediate: true })
watch(all, files => emit('update:modelValue', files.filter(file => file.status === 'success' && file.media).map(file => file.media)), { deep: true })
watch(() => active.value > 0 || all.value.some(file => file.status !== 'success'), blocked => emit('blocked', blocked), { immediate: true })
function beforeChoose({ resolve }) { resolve(requireLogin() && !props.disabled) }
function chooseError({ error }) { if (!String(error?.errMsg || '').includes('cancel')) notify('无法读取文件，请检查相册或相机权限') }
async function remove({ file }) {
  if (!file.media) return
  try { await request('/media/' + file.media.id, 'DELETE') } catch { notify('已移出内容，服务器文件清理未完成') }
}
function upload(file, formData, options, kind) {
  const key = ++sequence
  active.value++
  const finish = () => { tasks.delete(key); active.value-- }
  const task = uploadMedia(file, kind, {
    success(media) {
      finish(); if (disposed) return
      file.media = media; file.url = mediaUrl(media.url); file.name = media.url.split('/').pop()
      options.onSuccess({ statusCode: 200, data: JSON.stringify({ code: 0, data: media }) }, file, formData)
    },
    error(error) {
      finish(); if (disposed) return
      options.onError({ errMsg: error.message, message: error.message }, file, formData); notify(error)
    },
    progress(event) { if (!disposed) options.onProgress(event, file) }
  })
  tasks.set(key, task)
  return task
}
const uploadImage = (file, data, options) => upload(file, data, options, 'IMAGE')
const uploadVideo = (file, data, options) => upload(file, data, options, 'VIDEO')
async function retry() {
  if (!requireLogin()) return
  all.value.filter(file => file.status === 'fail').forEach(file => { file.status = 'pending'; file.error = '' })
  await nextTick(); imageUpload.value.submit(); videoUpload.value.submit()
}
onBeforeUnmount(() => { disposed = true; tasks.forEach(task => task.abort()); tasks.clear() })
</script>
