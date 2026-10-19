<template>
  <view class="registration-notice">
      <DreamLoading v-if="loading" inline text="正在取来声明" />
      <view v-else-if="error"><view class="body-text">{{ error }}</view><wd-button type="text" @click="reload">重新加载声明</wd-button></view>
      <template v-else-if="notice">
        <view class="row between"><text>免责声明</text><wd-button type="text" size="small" :disabled="disabled" @click="opened = true">查看完整声明</wd-button></view>
        <view v-if="showConsent" class="actions"><wd-checkbox :model-value="modelValue" :disabled="disabled || !read" shape="square" @update:model-value="emit('update:modelValue', $event)">我已阅读并同意上述声明</wd-checkbox></view>
      </template>
    <wd-popup v-model="opened" custom-class="registration-notice-popup" position="bottom" :z-index="200" closable safe-area-inset-bottom>
      <scroll-view scroll-y style="max-height: 85vh"><view v-if="notice" class="page-body">
        <NoticeDocument :notice="notice" />
        <view class="actions"><wd-button block plain @click="read = true; opened = false">阅读完成</wd-button></view>
      </view></scroll-view>
    </wd-popup>
  </view>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import DreamLoading from './DreamLoading.vue'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import NoticeDocument from './NoticeDocument.vue'
import { request } from '../lib/api'
defineProps({ modelValue: Boolean, showConsent: Boolean, disabled: Boolean })
const emit = defineEmits(['update:modelValue', 'loaded'])
const notice = ref(null), loading = ref(false), error = ref(''), opened = ref(false), read = ref(false)
let sequence = 0, disposed = false
async function reload() {
  const version = ++sequence
  notice.value = null; loading.value = true; error.value = ''; opened.value = false; read.value = false
  emit('update:modelValue', false); emit('loaded', null)
  try {
    const result = await request('/legal/registration-notice')
    if (disposed || version !== sequence) return
    notice.value = result; emit('loaded', result)
  } catch(e) { if (!disposed && version === sequence) error.value = e.message }
  finally { if (!disposed && version === sequence) loading.value = false }
}
defineExpose({ reload })
onMounted(reload)
onBeforeUnmount(() => { disposed = true; sequence++ })
</script>
