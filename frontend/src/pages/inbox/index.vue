<template>
  <AppShell title="消息" active="inbox">
    <GuestPrompt v-if="!session.token" title="登录后查看圆梦交流" hint="你收到和提供的每一份帮助，都会在这里相遇" icon="chat" />
    <template v-else>
      <view class="inbox-tabs">
        <view class="inbox-tab" :class="{ 'is-active': tab === 'help' }" hover-class="is-pressed" @click="tab = 'help'">帮助交流</view>
        <view class="inbox-tab" :class="{ 'is-active': tab === 'direct' }" hover-class="is-pressed" @click="tab = 'direct'">私信<text v-if="directUnread" :key="directUnread" class="tab-badge">{{ directUnread > 99 ? '99+' : directUnread }}</text></view>
      </view>
      <view v-if="tab === 'direct'" class="direct-list">
        <wd-status-tip url-prefix="/static/status/" v-if="directError" image="network" :tip="directError"><template #bottom><wd-button @click="loadDirect">重试</wd-button></template></wd-status-tip>
        <wd-status-tip url-prefix="/static/status/" v-else-if="directLoaded && !directItems.length" image="content" tip="还没有私信，去成员的名片页发起吧" />
        <view class="stagger">
          <view v-for="item in directItems" :key="item.id" class="direct-row" hover-class="is-pressed" @click="go('/pages/direct/index?id=' + item.id)">
            <view class="inbox-avatar"><UserAvatar :src="item.partnerAvatar" :size="44" /></view>
            <view class="direct-text">
              <view class="row between"><text class="direct-name">{{ item.partnerName }}</text><text class="direct-time">{{ shortTime(item.updatedAt) }}</text></view>
              <view class="row between"><text class="direct-preview">{{ item.lastMine ? '我：' : '' }}{{ item.lastMessage }}</text><text v-if="item.unread" class="unread-badge">{{ item.unread > 99 ? '99+' : item.unread }}</text></view>
            </view>
          </view>
        </view>
      </view>
      <template v-else>
      <wd-segmented v-model:value="filter" :options="['全部', '收到的帮助', '我提供的帮助']" custom-class="inbox-segmented" />
      <view class="space" />
      <view v-if="dreamId" class="muted section">当前梦想的消息</view>
      <wd-status-tip url-prefix="/static/status/" v-if="error" image="network" :tip="error"><template #bottom><wd-button @click="load">重试</wd-button></template></wd-status-tip>
      <wd-status-tip url-prefix="/static/status/" v-else-if="!loading && !filtered.length" image="content" tip="暂无消息" />
      <DreamSkeleton v-if="loading && !items.length" />
      <view class="stagger" :key="filter">
        <wd-card v-for="item in filtered" :key="item.id" :custom-class="'inbox-card' + (item.status === 'PENDING' ? ' is-pending' : '')" @click="go('/pages/chat/index?id=' + item.id)">
          <template #title><view class="row"><view class="inbox-avatar"><UserAvatar :src="item.partnerAvatar" :size="36" /><view v-if="item.status === 'PENDING'" class="pending-dot" /></view><text class="grow">{{ item.partnerName }}</text></view></template>
          <view class="row section"><wd-tag :type="item.status === 'ACCEPTED' ? 'success' : 'primary'" plain>{{ helpStatus(item.status) }}</wd-tag><wd-tag plain type="default">{{ item.isOwner ? '收到的帮助' : '我提供的帮助' }}</wd-tag></view>
          <view class="inbox-title section">{{ item.dreamTitle }}</view><view class="muted clamp">{{ item.offer }}</view>
          <template #footer><view class="row between"><text class="muted">{{ item.status === 'PENDING' ? '等待你们开始交流' : '继续你们的对话' }}</text><view class="more-pill"><text class="more-pill__text">进入交流</text><view class="more-pill__arrow"><wd-icon name="arrow-right" size="12px" color="#fff" /></view></view></view></template>
        </wd-card>
      </view>
      <view class="muted center" v-if="items.length >= 100">展示最近的 100 条帮助交流</view>
      </template>
    </template>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { ref, computed } from 'vue'
import { onLoad, onShow, onHide, onUnload } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import UserAvatar from '../../components/UserAvatar.vue'
import DreamSkeleton from '../../components/DreamSkeleton.vue'
import GuestPrompt from '../../components/GuestPrompt.vue'
import { request, session, go, helpStatus } from '../../lib/api'
const items = ref([]), filter = ref('全部'), error = ref(''), loading = ref(false), dreamId = ref('')
const tab = ref('help'), directItems = ref([]), directError = ref(''), directLoaded = ref(false)
const directUnread = computed(() => directItems.value.reduce((sum, item) => sum + (item.unread || 0), 0))
async function loadDirect() {
  if (!session.token) return
  try { directItems.value = await request('/direct/conversations'); directError.value = '' } catch (e) { directError.value = e.message } finally { directLoaded.value = true }
}
// 今天显示时分，其余显示月-日。
function shortTime(value) {
  if (!value) return ''
  const text = value.replace('T', ' '), today = new Date(), pad = n => String(n).padStart(2, '0')
  return text.slice(0, 10) === `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}` ? text.slice(11, 16) : text.slice(5, 10)
}
let timer
const filtered = computed(() => items.value.filter(x => (!dreamId.value || String(x.dreamId) === dreamId.value) && (filter.value === '全部' || (filter.value === '收到的帮助' ? x.isOwner : !x.isOwner))))
async function load() { if (!session.token || loading.value) return; loading.value = true; try { items.value = await request('/helps'); error.value = '' } catch(e) { error.value = e.message } finally { loading.value = false } }
onLoad(options => { dreamId.value = options.dreamId || ''; if (options.tab === 'direct') tab.value = 'direct' })
onShow(() => { load(); loadDirect(); clearInterval(timer); timer = setInterval(() => { load(); loadDirect() }, 10000) })
onHide(() => clearInterval(timer)); onUnload(() => clearInterval(timer))
</script>
<style scoped>
/* 待回应的交流带呼吸红点，提醒尽快回复。 */
:deep(.inbox-segmented) { border-radius: 999px !important; animation: dream-rise .5s var(--dream-ease) backwards; }
:deep(.inbox-segmented .wd-segmented__item) { border-radius: 999px !important; transition: color .3s ease; }
:deep(.inbox-segmented .wd-segmented__item--active) { border-radius: 999px !important; box-shadow: 0 4px 12px rgba(110, 86, 248, .18); transition: transform .4s var(--dream-ease) !important; }
.inbox-avatar { position: relative; }
/* 顶部切换：帮助交流 / 私信，私信显示未读总数。 */
.inbox-tabs { display: flex; gap: 6px; padding: 4px; margin-bottom: 16px; border-radius: 999px; background: rgba(255, 255, 255, .3); border: 1px solid var(--dream-glass-line); }
.inbox-tab { position: relative; flex: 1; padding: 9px 0; border-radius: 999px; text-align: center; font-size: 14px; font-weight: 600; color: var(--dream-muted); transition: all .35s var(--dream-ease); }
.inbox-tab.is-active { color: #fff; background: var(--dream-gradient); box-shadow: 0 6px 16px rgba(110, 86, 248, .3); }
.inbox-tab.is-pressed { transform: scale(.96); }
.tab-badge, .unread-badge { display: inline-block; min-width: 18px; height: 18px; padding: 0 5px; border-radius: 999px; box-sizing: border-box; text-align: center; font-size: 11px; line-height: 18px; font-weight: 600; color: #fff; background: #FF5C7C; animation: dream-pop .45s var(--dream-spring) backwards; }
.tab-badge { margin-left: 6px; vertical-align: 1px; }
/* 私信会话行：头像、昵称与时间、最后一条消息与未读数。 */
.direct-row { display: flex; align-items: center; gap: 12px; padding: 14px 16px; margin-bottom: 10px; border-radius: 18px; background: var(--dream-glass);
  border: 1px solid var(--dream-glass-line); transition: transform .3s var(--dream-spring), background-color .25s ease; }
.direct-row.is-pressed { transform: scale(.98); background: rgba(255, 255, 255, .45); }
.direct-text { flex: 1; min-width: 0; }
.direct-text .row { flex-wrap: nowrap; }
.direct-name { font-size: 15px; font-weight: 700; color: var(--dream-ink); overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.direct-time { flex-shrink: 0; font-size: 12px; color: #B4AFCB; }
.direct-preview { flex: 1; min-width: 0; margin-top: 4px; font-size: 13px; color: var(--dream-muted); overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.inbox-title { font-size: 16px; font-weight: 700; line-height: 1.5; }
.pending-dot { position: absolute; top: 0; right: 0; width: 10px; height: 10px; border-radius: 50%; background: var(--dream-pink); border: 2px solid #fff; animation: dream-breathe 1.4s ease-in-out infinite; }
:deep(.inbox-card.is-pending) { border-color: rgba(255, 141, 184, .35); }
</style>
