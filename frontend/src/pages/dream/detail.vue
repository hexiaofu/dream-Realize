<template>
  <AppShell title="梦想详情" back>
    <wd-status-tip url-prefix="/static/status/" v-if="error" image="network" :tip="error"><template #bottom><wd-button @click="load">重试</wd-button></template></wd-status-tip>
    <view v-else-if="!dream" class="center"><wd-loading /></view>
    <view v-else class="dream-detail">
      <wd-card custom-class="dream-summary" custom-style="padding:20px">
        <view class="row dream-tags"><wd-tag plain>{{ label(categories, dream.category) }}</wd-tag><wd-tag type="success" plain>{{ label(statuses, dream.status) }}</wd-tag><wd-tag v-if="dream.isOwner" type="warning" plain>我的梦想</wd-tag></view>
        <view class="dream-title">{{ dream.title }}</view>
        <wd-cell :border="false" :clickable="!dream.anonymous" value-align="left" custom-style="--wot-cell-padding:0" @click="!dream.anonymous && go('/pages/profile/index?id=' + dream.authorId)">
          <view class="dream-author">
            <UserAvatar :src="dream.authorAvatar" :size="40" />
            <view class="grow"><wd-text :text="dream.authorName" bold color="#263e39" /><view class="muted dream-published">{{ date(dream.createdAt) }} 发布</view></view>
            <wd-icon v-if="!dream.anonymous" name="arrow-right" color="#a6b1ad" />
          </view>
        </wd-cell>
        <view v-if="dream.story" class="body-text dream-story">{{ dream.story }}</view>
        <MediaGallery :media="dream.media" />
        <view id="dream-reactions" class="actions"><DreamReactions :dream="dream" show-comments popup-comments @comments="commentsOpen = true" @change="updateInteractions" /></view>
      </wd-card>
      <wd-card title="帮助信息" custom-style="padding-bottom:8px">
        <wd-cell-group border custom-style="--wot-cell-padding:0;--wot-cell-title-color:#7c8a86;--wot-cell-value-color:#263e39">
          <wd-cell title="需要的帮助" title-width="96px" :value="label(helpTypes, dream.helpType)" />
          <wd-cell title="帮助条件" title-width="96px" :value="dream.rewardType === 'FREE' ? '期待无偿帮助' : '条件由双方协商'" />
          <wd-cell title="交流方式" title-width="96px" :value="label(contactModes, dream.contactMode)" />
          <wd-cell v-if="dream.city" title="所在城市" title-width="96px" :value="dream.city" />
          <wd-cell title="已接受的帮助" title-width="96px" :value="dream.helperCount + ' 份'" />
        </wd-cell-group>
      </wd-card>
      <wd-card v-if="dream.helpType === 'CASH'" title="现金心愿">
        <view class="row between section"><view><view class="muted">发布者记录已获得</view><wd-text :text="'¥ ' + dream.receivedAmount" type="primary" size="26px" bold /></view><view v-if="dream.targetAmount" class="muted">期望 ¥ {{ dream.targetAmount }}</view></view>
        <wd-progress v-if="dream.targetAmount" :percentage="Math.min(100, Math.round(dream.receivedAmount / dream.targetAmount * 100))" hide-text />
      </wd-card>
      <wd-card custom-class="dream-updates" custom-style="padding-bottom:16px">
        <template #title><view class="row between"><text>圆梦足迹</text><wd-button v-if="dream.isOwner" type="text" size="small" @click="openProgress">记录新进展</wd-button></view></template>
        <view v-if="!dream.updates.length" class="muted">暂无进展</view>
        <view v-for="(update, index) in dream.updates" :key="update.id">
          <wd-divider v-if="index" />
          <view class="muted">{{ date(update.createdAt) }}</view><view class="body-text">{{ update.content }}</view><MediaGallery :media="update.media" />
        </view>
      </wd-card>
      <view v-if="!helpOpen && !progressOpen && !commentsOpen" class="dream-action-bar"><view class="dream-action-content">
        <wd-button v-if="dream.isOwner" block size="large" @click="go('/pages/inbox/index?dreamId=' + dream.id)">查看收到的帮助</wd-button>
        <wd-button v-else-if="dream.myHelpId" block size="large" @click="go('/pages/chat/index?id=' + dream.myHelpId)">查看我的帮助与交流</wd-button>
        <wd-button v-else-if="['OPEN','IN_PROGRESS'].includes(dream.status)" block size="large" icon="heart" @click="openHelp">我愿意帮助</wd-button>
        <wd-button v-else block disabled>{{ dream.status === 'COMPLETED' ? '这个梦想已实现' : '梦想暂时停一停' }}</wd-button>
      </view></view>
    </view>
    <DreamComments v-if="dream" v-model="commentsOpen" :dream="dream" @change="updateInteractions" @choose-reaction="showReactions" />
    <wd-popup v-model="helpOpen" position="bottom" closable safe-area-inset-bottom>
      <view class="page-body"><view class="heading">提供帮助</view>
        <wd-cell-group border><wd-textarea v-model="help.offer" label="我能提供" placeholder="填写帮助内容与条件" :maxlength="1000" show-word-limit /><wd-select-picker type="radio" :show-confirm="false" v-model="help.rewardType" label="帮助条件" :columns="rewardTypes" /></wd-cell-group>
        <view class="actions"><wd-button block :loading="saving" @click="sendHelp">发送帮助意愿</wd-button></view>
      </view>
    </wd-popup>
    <wd-popup v-model="progressOpen" position="bottom" closable safe-area-inset-bottom>
      <scroll-view scroll-y style="max-height: 85vh"><view class="page-body"><view class="heading">记录进展</view>
        <wd-cell-group border><wd-textarea v-model="progress.content" label="新的进展" placeholder="填写进展内容" :maxlength="2000" show-word-limit /><wd-select-picker type="radio" :show-confirm="false" v-model="progress.status" label="梦想状态" :columns="statuses" /><wd-input v-if="dream?.helpType === 'CASH'" v-model="progress.amount" label="已获得金额" type="digit" placeholder="选填，累计金额" /></wd-cell-group>
        <view class="space" /><MediaUploader v-model="progress.media" :disabled="saving" @blocked="mediaBlocked = $event" />
        <view class="actions"><wd-button block :loading="saving" :disabled="mediaBlocked" @click="sendProgress">{{ mediaBlocked ? '请先完成附件上传' : '保存这段足迹' }}</wd-button></view>
      </view></scroll-view>
    </wd-popup>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { reactive, ref } from 'vue'
import { onHide, onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import UserAvatar from '../../components/UserAvatar.vue'
import MediaUploader from '../../components/MediaUploader.vue'
import MediaGallery from '../../components/MediaGallery.vue'
import DreamReactions from '../../components/DreamReactions.vue'
import DreamComments from '../../components/DreamComments.vue'
import { request, go, notify, requireLogin, label, date, categories, statuses, helpTypes, contactModes, rewardTypes } from '../../lib/api'
const dream = ref(null), error = ref(''), helpOpen = ref(false), progressOpen = ref(false), commentsOpen = ref(false), saving = ref(false)
const mediaBlocked = ref(false)
const help = reactive({ offer: '', rewardType: 'FREE' }), progress = reactive({ content: '', status: 'OPEN', amount: '', media: [] })
let id, focusComments = false, resumeComments = false, visible = false, loadSequence = 0
onLoad(options => { id = options.id; focusComments = options.comments === '1' })
onShow(() => { visible = true; load() })
onHide(() => { visible = false; resumeComments = resumeComments || commentsOpen.value; commentsOpen.value = false })
onUnload(() => { visible = false; loadSequence++ })
function updateInteractions(value) { Object.assign(dream.value, value) }
function showReactions() { uni.pageScrollTo({ selector: '#dream-reactions', duration: 250 }) }
async function load() {
  if (!id) return
  const sequence = ++loadSequence
  try {
    error.value = ''; const result = await request('/dreams/' + id)
    if (sequence !== loadSequence) return
    dream.value = result
    if (visible && (focusComments || resumeComments)) { commentsOpen.value = true; focusComments = false; resumeComments = false }
  } catch(e) { if (sequence === loadSequence) error.value = e.message }
}
function openHelp() { if (requireLogin()) helpOpen.value = true }
function openProgress() { progress.status = dream.value.status; progress.amount = String(dream.value.receivedAmount); progressOpen.value = true }
async function sendHelp() {
  if (saving.value) return
  if (!help.offer.trim()) return notify('先写下你能提供的帮助')
  saving.value = true
  try { const result = await request(`/dreams/${id}/helps`, 'POST', help); helpOpen.value = false; go('/pages/chat/index?id=' + result.id) }
  catch(e) { notify(e) } finally { saving.value = false }
}
async function sendProgress() {
  if (saving.value || mediaBlocked.value) return
  if (!progress.content.trim()) return notify('写下一点进展吧')
  if (dream.value.helpType === 'CASH' && progress.amount && !/^\d+(\.\d{1,2})?$/.test(progress.amount)) return notify('请填写正确的累计金额')
  saving.value = true
  try { dream.value = await request(`/dreams/${id}/updates`, 'POST', { content: progress.content, status: progress.status, mediaIds: progress.media.map(file => file.id), receivedAmount: dream.value.helpType === 'CASH' && progress.amount !== '' ? Number(progress.amount) : null }); progress.content = ''; progress.media = []; progressOpen.value = false }
  catch(e) { notify(e) } finally { saving.value = false }
}
</script>
<style scoped>
/* 内容排版与底部操作区；卡片、信息行和按钮沿用 Wot 组件。 */
.dream-detail { overflow-wrap: anywhere; padding-bottom: env(safe-area-inset-bottom); }
.dream-tags { margin-bottom: 12px; }
.dream-title { font-size: 20px; font-weight: 650; line-height: 1.5; color: #263e39; margin-bottom: 8px; text-wrap: balance; }
.dream-author { display: flex; align-items: center; gap: 10px; }
.dream-published { font-size: 12px; white-space: nowrap; }
.dream-story { margin-top: 12px; font-size: 15px; color: #263e39; }
.dream-action-bar { position: fixed; z-index: 20; right: 0; bottom: 0; left: 0; padding: 12px 16px calc(12px + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #edf1ef; }
.dream-action-content { max-width: 720px; margin: auto; }
</style>
