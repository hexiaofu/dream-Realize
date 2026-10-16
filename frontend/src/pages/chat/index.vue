<template>
  <AppShell title="圆梦交流" back>
    <wd-status-tip url-prefix="/static/status/" v-if="error && !thread" image="network" :tip="error"><template #bottom><wd-button @click="load">重试</wd-button></template></wd-status-tip>
    <template v-if="thread">
      <wd-card>
        <template #title><view class="row"><UserAvatar :src="thread.partnerAvatar" :size="36" /><text class="grow">{{ thread.partnerName }}</text></view></template>
        <wd-text :text="thread.dreamTitle" bold @click="go('/pages/dream/detail?id=' + thread.dreamId)" />
        <view class="row actions"><wd-tag :type="thread.status === 'ACCEPTED' ? 'success' : 'primary'">{{ helpStatus(thread.status) }}</wd-tag><wd-tag plain>{{ thread.rewardType === 'FREE' ? '无偿帮助' : '条件待双方协商' }}</wd-tag></view>
        <view class="body-text actions">{{ thread.offer }}</view>
        <template v-if="thread.status === 'PENDING'" #footer>
          <view class="row">
            <template v-if="thread.isOwner"><wd-button :loading="deciding" size="small" @click="decide('ACCEPTED')">接受帮助</wd-button><wd-button :disabled="deciding" size="small" plain @click="decide('DECLINED')">婉拒</wd-button></template>
            <wd-button v-else :loading="deciding" size="small" plain @click="decide('WITHDRAWN')">撤回意愿</wd-button>
          </view>
        </template>
      </wd-card>
      <view class="row between section"><view class="subheading">你们的对话</view><wd-button type="text" size="small" @click="load">刷新消息</wd-button></view>
      <view v-if="error" class="muted section">{{ error }}</view>
      <wd-button v-if="hasOlder" plain block size="small" :loading="loadingOlder" @click="older">更早的消息</wd-button>
      <wd-status-tip url-prefix="/static/status/" v-if="!messages.length" image="content" tip="暂无消息" />
      <wd-card v-for="message in messages" :key="message.id">
        <template #title><view class="row"><UserAvatar :src="message.senderAvatar" :size="28" /><text class="grow">{{ message.mine ? '我' : message.senderName }}</text></view></template>
        <view class="body-text">{{ message.content }}</view><template #footer><view class="muted">{{ date(message.createdAt) }}</view></template>
      </wd-card>
      <wd-cell-group v-if="['PENDING','ACCEPTED'].includes(thread.status)" border>
        <wd-textarea v-model="content" placeholder="输入消息" :maxlength="2000" show-word-limit />
        <wd-cell><wd-button :loading="sending" :disabled="!content.trim()" @click="send">发送消息</wd-button></wd-cell>
      </wd-cell-group>
    </template>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { ref } from 'vue'
import { onLoad, onShow, onHide, onUnload } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import UserAvatar from '../../components/UserAvatar.vue'
import { request, notify, go, date, helpStatus, session } from '../../lib/api'
const thread = ref(null), messages = ref([]), content = ref(''), error = ref(''), sending = ref(false), deciding = ref(false), hasOlder = ref(false), loadingOlder = ref(false)
let id, timer, refreshing = false, initialized = false
function merge(items) { const all = new Map([...messages.value, ...items].map(x => [x.id, x])); messages.value = [...all.values()].sort((a,b) => a.id - b.id) }
async function load() {
  if (!id || refreshing || !session.token) return
  refreshing = true
  try { const [info, list] = await Promise.all([request('/helps/' + id), request(`/helps/${id}/messages`)]); thread.value = info; merge(list); if (!initialized) { hasOlder.value = list.length === 50; initialized = true }; error.value = '' }
  catch(e) { error.value = e.message } finally { refreshing = false }
}
async function older() { if (loadingOlder.value) return; loadingOlder.value = true; try { const data = await request(`/helps/${id}/messages?before=${messages.value[0].id}`); merge(data); hasOlder.value = data.length === 50 } catch(e) { notify(e) } finally { loadingOlder.value = false } }
async function send() {
  if (sending.value || !content.value.trim()) return
  sending.value = true
  try { merge(await request(`/helps/${id}/messages`, 'POST', { content: content.value })); content.value = '' }
  catch(e) { notify(e) } finally { sending.value = false }
}
async function decide(status) {
  if (deciding.value) return
  deciding.value = true
  try { thread.value = await request('/helps/' + id, 'PATCH', { status }) } catch(e) { notify(e) } finally { deciding.value = false }
}
onLoad(options => id = options.id)
onShow(() => { if (!session.token) { go('/pages/auth/index'); return }; load(); clearInterval(timer); timer = setInterval(load, 5000) })
onHide(() => clearInterval(timer)); onUnload(() => clearInterval(timer))
</script>
