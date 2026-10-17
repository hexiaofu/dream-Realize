<template>
  <AppShell title="免责声明" back>
    <view v-if="loading" class="center"><wd-loading /></view>
    <wd-status-tip v-else-if="error" url-prefix="/static/status/" image="network" :tip="error"><template #bottom><wd-button @click="load">重试</wd-button></template></wd-status-tip>
    <NoticeDocument v-else-if="notice" :notice="notice" />
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import NoticeDocument from '../../components/NoticeDocument.vue'
import { request } from '../../lib/api'
const notice = ref(null), loading = ref(false), error = ref('')
async function load() {
  if (loading.value) return
  loading.value = true; error.value = ''
  try { notice.value = await request('/legal/registration-notice') }
  catch(e) { error.value = e.message } finally { loading.value = false }
}
onLoad(load)
</script>
