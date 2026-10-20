<template>
  <AppShell :title="conversation ? conversation.partnerName : '私信'" back>
    <wd-status-tip url-prefix="/static/status/" v-if="error && !conversation" image="network" :tip="error"><template #bottom><wd-button @click="load">重试</wd-button></template></wd-status-tip>
    <DreamLoading v-else-if="!conversation" text="正在打开私信" />
    <template v-else>
      <view class="direct-partner" hover-class="is-pressed" @click="go('/pages/profile/index?id=' + conversation.partnerId)">
        <UserAvatar :src="conversation.partnerAvatar" :size="40" />
        <view class="grow"><view class="partner-name">{{ conversation.partnerName }}</view><view class="muted">查看名片</view></view>
        <wd-icon name="arrow-right" size="16px" color="#B4AFCB" />
      </view>
      <view v-if="error" class="muted section">{{ error }}</view>
      <wd-button v-if="hasOlder" plain block size="small" :loading="loadingOlder" @click="older">更早的消息</wd-button>
      <view v-if="!messages.length" class="direct-empty">
        <view class="empty-orb"><wd-icon name="chat" size="26px" color="#fff" /></view>
        <view class="muted">打个招呼，开始你们的对话吧</view>
      </view>
      <wd-card v-for="message in messages" :key="message.id" :custom-class="message.mine ? 'bubble is-mine' : 'bubble'">
        <template #title><view class="row"><UserAvatar :src="message.senderAvatar" :size="28" /><text class="grow">{{ message.mine ? '我' : message.senderName }}</text></view></template>
        <view class="body-text">{{ message.content }}</view><template #footer><view class="muted">{{ date(message.createdAt) }}</view></template>
      </wd-card>
      <view :id="'direct-bottom'" />
      <wd-cell-group border class="actions">
        <wd-textarea v-model="content" :placeholder="restricted ? '账号已限制发布，暂时不能发送' : '输入私信内容'" :maxlength="2000" show-word-limit :disabled="restricted || sending" />
        <wd-cell><wd-button :loading="sending" :disabled="restricted || !content.trim()" @click="send">发送私信</wd-button></wd-cell>
      </wd-cell-group>
    </template>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { computed, nextTick, ref } from 'vue'
import { onHide, onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import UserAvatar from '../../components/UserAvatar.vue'
import DreamLoading from '../../components/DreamLoading.vue'
import { date, go, notify, request, session } from '../../lib/api'
const conversation = ref(null), messages = ref([]), content = ref(''), error = ref(''), sending = ref(false), hasOlder = ref(false), loadingOlder = ref(false)
const restricted = computed(() => session.user?.restriction?.mode === 'READ_ONLY')
let id, timer, refreshing = false, initialized = false
function merge(items) { const all = new Map([...messages.value, ...items].map(x => [x.id, x])); messages.value = [...all.values()].sort((a, b) => a.id - b.id) }
async function scrollToBottom() { await nextTick(); uni.pageScrollTo({ selector: '#direct-bottom', duration: 250 }) }
// 页面可见时每 5 秒拉取最新消息；读取最新一页会同时标记已读。
async function load() {
  if (!id || refreshing || !session.token) return
  refreshing = true
  try {
    const before = messages.value.length
    const [info, list] = await Promise.all([request('/direct/conversations/' + id), request(`/direct/conversations/${id}/messages`)])
    conversation.value = info; merge(list); error.value = ''
    if (!initialized) { hasOlder.value = list.length === 50; initialized = true; scrollToBottom() }
    else if (messages.value.length > before) scrollToBottom()
  } catch (e) { error.value = e.message } finally { refreshing = false }
}
async function older() {
  if (loadingOlder.value || !messages.value.length) return
  loadingOlder.value = true
  try { const data = await request(`/direct/conversations/${id}/messages?before=${messages.value[0].id}`); merge(data); hasOlder.value = data.length === 50 } catch (e) { notify(e) } finally { loadingOlder.value = false }
}
async function send() {
  if (sending.value || restricted.value || !content.value.trim()) return
  sending.value = true
  try { merge(await request(`/direct/conversations/${id}/messages`, 'POST', { content: content.value })); content.value = ''; scrollToBottom() }
  catch (e) { notify(e) } finally { sending.value = false }
}
onLoad(options => { id = options.id })
onShow(() => { if (!session.token) { go('/pages/auth/index'); return }; load(); clearInterval(timer); timer = setInterval(load, 5000) })
onHide(() => clearInterval(timer)); onUnload(() => clearInterval(timer))
</script>
<style scoped>
/* 私信：顶部对方名片入口，消息为左右气泡，与帮助交流保持一致。 */
.direct-partner { display: flex; align-items: center; gap: 12px; padding: 12px 16px; margin-bottom: 16px; border-radius: 18px; background: var(--dream-glass);
  border: 1px solid var(--dream-glass-line); transition: transform .3s var(--dream-spring); animation: dream-rise .5s var(--dream-ease) backwards; }
.direct-partner.is-pressed { transform: scale(.97); }
.partner-name { font-size: 16px; font-weight: 700; }
.direct-empty { padding: 36px 0 20px; text-align: center; animation: dream-rise .6s var(--dream-ease) backwards; }
.empty-orb { display: flex; align-items: center; justify-content: center; width: 56px; height: 56px; margin: 0 auto 12px; border-radius: 50%;
  background: var(--dream-gradient); box-shadow: 0 10px 24px rgba(110, 86, 248, .3); animation: dream-float 4s ease-in-out infinite; }
:deep(.bubble) { width: 82%; margin-right: auto !important; border-radius: 6px 20px 20px 20px !important; animation: bubble-in .45s var(--dream-spring) backwards; transform-origin: 0 0; }
:deep(.bubble.is-mine) { margin-left: auto !important; margin-right: 0 !important; border-radius: 20px 6px 20px 20px !important; transform-origin: 100% 0;
  background: linear-gradient(135deg, #7A62FA 0%, #A06EFF 100%) !important; color: #fff; box-shadow: 0 10px 24px rgba(110, 86, 248, .28) !important; }
:deep(.bubble.is-mine .wd-card__title-content), :deep(.bubble.is-mine .body-text) { color: #fff; }
:deep(.bubble.is-mine .muted) { color: rgba(255, 255, 255, .7); }
:deep(.bubble.is-mine .wd-card__footer) { border-top-color: rgba(255, 255, 255, .2); }
@keyframes bubble-in { from { opacity: 0; transform: scale(.7) translateY(10px); } to { opacity: 1; transform: none; } }
</style>
