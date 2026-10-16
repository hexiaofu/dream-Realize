<template>
  <wd-cell title="头像" vertical>
    <view class="row">
      <UserAvatar :src="modelValue.url" :size="64" />
      <wd-button size="small" plain :loading="uploading" :disabled="disabled" @click="choose">更换头像</wd-button>
      <wd-button v-if="modelValue.id || failure" size="small" type="text" :disabled="disabled || uploading" @click="reset">恢复默认</wd-button>
    </view>
    <view v-if="uploading" class="actions"><wd-progress :percentage="progress" /></view>
    <view v-if="failure" class="row actions"><wd-text :text="failure" type="danger" /><wd-button type="text" size="small" :disabled="disabled" @click="upload">重试</wd-button></view>
  </wd-cell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { onBeforeUnmount, ref, watch } from 'vue'
import UserAvatar from './UserAvatar.vue'
import { notify, uploadMedia } from '../lib/api'
const props = defineProps({ modelValue: { type: Object, required: true }, disabled: Boolean })
const emit = defineEmits(['update:modelValue', 'blocked'])
const uploading = ref(false), progress = ref(0), failure = ref('')
let file, task, disposed = false, choosing = false
watch(() => uploading.value || Boolean(failure.value), blocked => emit('blocked', blocked), { immediate: true })
function choose() {
  if (props.disabled || uploading.value || choosing) return
  choosing = true
  uni.chooseImage({ count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'],
    success(result) {
      if (disposed) return
      const selected = result.tempFiles?.[0]
      if (selected?.size > 10 * 1024 * 1024) return notify('头像不能超过 10 MB')
      file = { url: selected?.path || result.tempFilePaths[0] }; upload()
    },
    fail(error) { if (!disposed && !String(error?.errMsg).includes('cancel')) notify('无法读取图片，请检查相册权限') },
    complete() { choosing = false }
  })
}
function upload() {
  if (!file || disposed || props.disabled || uploading.value) return
  uploading.value = true; progress.value = 0; failure.value = ''
  task = uploadMedia(file, 'IMAGE', {
    success(media) {
      if (disposed) return
      uploading.value = false; task = null
      emit('update:modelValue', { id: media.id, url: media.url })
    },
    error(error) { if (!disposed) { uploading.value = false; task = null; failure.value = error.message } },
    progress(event) { if (!disposed) progress.value = event.progress }
  })
}
function reset() { if (props.disabled || uploading.value) return; failure.value = ''; file = null; emit('update:modelValue', { id: '', url: '' }) }
onBeforeUnmount(() => { disposed = true; task?.abort(); emit('blocked', false) })
</script>
