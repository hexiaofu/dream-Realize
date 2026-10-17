<template>
  <wd-popup :model-value="modelValue" position="bottom" :z-index="100" :closable="!sending && deleting === null" :close-on-click-modal="!sending && deleting === null" safe-area-inset-bottom custom-class="dream-comments-popup" custom-style="max-width:752px;margin:0 auto;border-radius:20px 20px 0 0;background:#f5f7f6" @update:model-value="emit('update:modelValue', $event)" @after-leave="afterClose">
    <view class="dream-comments">
      <view class="row between comments-header"><view class="subheading">评论 · {{ total }}</view><wd-button type="text" size="small" :loading="loading" @click="load(true)">刷新评论</wd-button></view>
      <scroll-view scroll-y :scroll-into-view="scrollTarget" class="comments-scroll">
        <view class="comments-list">
          <view :id="'comments-start-' + dream.id" />
          <view v-if="error" class="row section"><wd-text :text="error" type="danger" /><wd-button type="text" size="small" @click="load(true)">重试</wd-button></view>
          <wd-card v-for="comment in comments" :key="comment.id" custom-class="comment-card">
            <template #title><view class="row">
              <UserAvatar :src="comment.authorAvatar" :size="32" />
              <wd-text :text="comment.authorName" bold @click="!comment.anonymous && go('/pages/profile/index?id=' + comment.authorId)" />
              <wd-tag v-if="comment.isAuthor" plain>作者</wd-tag>
              <wd-tag :type="comment.authorReaction === 'SUPPORT' ? 'success' : 'default'" :icon="comment.authorReaction === 'SUPPORT' ? 'thumb-up' : comment.authorReaction === 'DISAGREE' ? 'thumb-down' : ''" plain>{{ comment.authorReaction === 'SUPPORT' ? '精神支持' : comment.authorReaction === 'DISAGREE' ? '不同意' : '未表态' }}</wd-tag>
            </view></template>
            <view v-if="comment.content" class="body-text">{{ comment.content }}</view>
            <CommentMediaGallery :files="comment.media" :restricted="restricted" />
            <template #footer><view class="row between"><text class="muted">{{ date(comment.createdAt) }}</text><wd-button v-if="comment.isMine" type="text" size="small" :disabled="deleting !== null || restricted" @click="remove(comment)">删除</wd-button></view></template>
          </wd-card>
          <view v-if="loading" class="center"><wd-loading /></view>
          <view v-else-if="!error && !comments.length" class="muted center">暂无评论</view>
          <wd-button v-if="hasMore" block plain :loading="loading" @click="load(false)">更多评论</wd-button>
        </view>
      </scroll-view>
      <view class="comments-composer">
        <template v-if="session.token">
          <wd-textarea :model-value="content" :placeholder="!qualified ? '答题满90分后可评论' : hasReacted ? '写评论' : '先表态，再评论'" :maxlength="-1" fixed no-border :cursor-spacing="20" custom-style="--wot-textarea-padding:0;--wot-textarea-cell-padding:0" :disabled="!canCompose" @update:model-value="updateContent" @blur="cursor = $event.cursor" />
          <view class="comment-word-count muted">{{ contentLength }}/1000</view>
          <scroll-view v-if="attachments.length" scroll-x class="comment-draft-media"><view class="draft-media-row"><wd-img v-for="file in attachments" :key="file.id" :src="mediaUrl(file.url)" width="48px" height="48px" radius="6px" mode="aspectFit" @click="picker.open('images')" /></view></scroll-view>
          <view class="row comments-toolbar">
            <wd-button type="text" size="small" :disabled="!canCompose" @click="picker.open('emoji')">表情</wd-button>
            <wd-button type="text" size="small" icon="picture" :disabled="!canCompose" @click="picker.open('images')">{{ uploadsBlocked ? '查看上传' : attachments.length ? '图片 ' + attachments.length : '图片' }}</wd-button>
            <wd-button type="text" size="small" icon="star" :disabled="!canCompose" @click="picker.open('favorites')">收藏</wd-button>
          </view>
          <view class="row between comments-controls">
            <view class="row"><wd-switch v-model="anonymous" :disabled="sending || protectedOwner || restricted || !qualified || !hasReacted" size="20px" /><text class="muted">匿名评论</text></view>
            <wd-button v-if="!qualified && !restricted" size="small" plain @click="takeExam">去答题</wd-button>
            <wd-button v-else-if="!hasReacted && !restricted" size="small" plain @click="chooseReaction">先表态</wd-button>
            <wd-button v-else size="small" :loading="sending" :disabled="(!content.trim() && !attachments.length) || uploadsBlocked || !canCompose" @click="send">发表评论</wd-button>
          </view>
          <view v-if="restricted" class="muted actions">账号已限制发布</view>
        </template>
        <wd-button v-else block plain @click="requireLogin">登录后评论</wd-button>
      </view>
    </view>
  </wd-popup>
  <CommentMediaPicker v-if="session.token" :key="draftVersion" ref="picker" :disabled="!canCompose" :visible="modelValue" @change="attachments = $event" @blocked="uploadsBlocked = $event" @emoji="insertEmoji" />
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import UserAvatar from './UserAvatar.vue'
import CommentMediaGallery from './CommentMediaGallery.vue'
import CommentMediaPicker from './CommentMediaPicker.vue'
import { rememberFavorites } from '../lib/commentMedia'
import { date, go, mediaUrl, notify, request, requireLogin, session } from '../lib/api'
const props = defineProps({ dream: { type: Object, required: true }, modelValue: Boolean }), emit = defineEmits(['change', 'update:modelValue', 'choose-reaction'])
const comments = ref([]), total = ref(0), hasMore = ref(false), loading = ref(false), error = ref('')
const content = ref(''), anonymous = ref(false), sending = ref(false), deleting = ref(null)
const contentLength = computed(() => Array.from(content.value).length)
const attachments = ref([]), uploadsBlocked = ref(false), picker = ref(), draftVersion = ref(0), cursor = ref(null)
const scrollTarget = ref('')
const qualification = ref(null), qualified = computed(() => qualification.value?.passed === true)
const restricted = computed(() => session.user?.restriction?.mode === 'READ_ONLY')
const hasReacted = computed(() => ['SUPPORT', 'DISAGREE'].includes(props.dream.myReaction))
const protectedOwner = computed(() => props.dream.isOwner && props.dream.anonymous)
const canCompose = computed(() => !sending.value && !restricted.value && qualified.value && hasReacted.value)
let page = 1, sequence = 0, disposed = false, pendingReaction = false
watch(protectedOwner, value => { if (value) anonymous.value = true }, { immediate: true })
watch(() => [props.modelValue, props.dream, session.token], ([opened, currentDream, token], previous) => {
  if (!previous || previous[1]?.id !== currentDream.id || previous[2] !== token) {
    sequence++; content.value = ''; comments.value = []; qualification.value = null; anonymous.value = protectedOwner.value; loading.value = false
    resetMedia()
  }
  total.value = props.dream.commentCount || 0
  if (opened) load(true)
}, { immediate: true })
watch(() => props.dream.myReaction, value => {
  comments.value.forEach(comment => { if (comment.isMine) comment.authorReaction = value || 'NONE' })
})
function chooseReaction() { pendingReaction = true; emit('update:modelValue', false) }
function afterClose() { if (pendingReaction) { pendingReaction = false; emit('choose-reaction') } }
function takeExam() { if (requireLogin()) go('/pages/admission/index?dreamId=' + props.dream.id) }
function resetMedia() { draftVersion.value++; attachments.value = []; uploadsBlocked.value = false; cursor.value = null }
async function updateContent(value) {
  content.value = value
  const limited = Array.from(value).slice(0, 1000).join('')
  if (limited !== value) { await nextTick(); if (content.value === value) content.value = limited }
}
function insertEmoji(emoji) {
  if (!canCompose.value) return
  if (Array.from(content.value + emoji).length > 1000) { notify('评论最多 1000 字'); return }
  const position = Number.isInteger(cursor.value) && cursor.value >= 0 ? Math.min(cursor.value, content.value.length) : content.value.length
  content.value = content.value.slice(0, position) + emoji + content.value.slice(position)
  cursor.value = position + emoji.length
}
async function load(reset) {
  if (disposed || (!reset && loading.value)) return
  const version = ++sequence, next = reset ? 1 : page + 1, reaction = props.dream.myReaction
  loading.value = true; error.value = ''
  if (reset) scrollTarget.value = ''
  try {
    const result = await request(`/dreams/${props.dream.id}/comments?page=${next}`)
    if (disposed || version !== sequence) return
    qualification.value = result.commentQualification || null
    rememberFavorites(result.items.flatMap(comment => comment.media || []))
    comments.value = reset ? result.items : [...new Map([...comments.value, ...result.items].map(x => [x.id, x])).values()]
    if (reaction !== props.dream.myReaction) comments.value.forEach(comment => { if (comment.isMine) comment.authorReaction = props.dream.myReaction || 'NONE' })
    total.value = result.total; hasMore.value = result.hasMore; page = next
    if (reset) scrollTarget.value = 'comments-start-' + props.dream.id
    emit('change', { commentCount: result.total })
  } catch(e) { if (version === sequence && !disposed) error.value = e.message }
  finally { if (version === sequence && !disposed) loading.value = false }
}
async function send() {
  if (sending.value || uploadsBlocked.value || (!content.value.trim() && !attachments.value.length) || !requireLogin()) return
  if (restricted.value) return
  if (!qualified.value) { takeExam(); return }
  if (!hasReacted.value) { chooseReaction(); return }
  sending.value = true
  try {
    const result = await request(`/dreams/${props.dream.id}/comments`, 'POST', { content: content.value, anonymous: protectedOwner.value || anonymous.value, mediaIds: attachments.value.map(file => file.id) })
    content.value = ''; resetMedia(); emit('change', { commentCount: result.commentCount }); await load(true)
  } catch(e) { notify(e) } finally { sending.value = false }
}
function remove(comment) {
  if (deleting.value !== null) return
  uni.showModal({ title: '删除评论', content: '确定删除这条评论吗？', confirmText: '删除', cancelText: '取消', success: async result => {
    if (!result.confirm || deleting.value !== null) return
    deleting.value = comment.id
    try { const result = await request(`/dreams/${props.dream.id}/comments/${comment.id}`, 'DELETE'); emit('change', { commentCount: result.commentCount }); await load(true) }
    catch(e) { notify(e) } finally { deleting.value = null }
  } })
}
onBeforeUnmount(() => { disposed = true; sequence++ })
</script>
<style scoped>
/* 弹层内部滚动布局，输入框与按钮使用 Wot。 */
.dream-comments { display: flex; flex-direction: column; height: 82vh; max-height: 760px; }
.comments-header { flex-shrink: 0; padding: 16px 48px 16px 16px; border-bottom: 1px solid #edf1ef; }
.comments-scroll { flex: 1; min-height: 0; height: 0; }
.comments-list { padding: 16px; }
.comments-composer { flex-shrink: 0; padding: 12px 16px 16px; background: #fff; border-top: 1px solid #edf1ef; }
.comments-controls { margin-top: 8px; }
.comments-toolbar { gap: 20px; }
.comment-draft-media { margin: 4px 0; white-space: nowrap; }
.draft-media-row { display: flex; gap: 8px; }
.draft-media-row :deep(.wd-img) { flex-shrink: 0; }
.comments-composer :deep(.wd-textarea__inner) { height: 64px; }
/* 独立计数避免组件按 UTF-16 截断表情，前后端统一按 Unicode 字符计数。 */
.comment-word-count { line-height: 20px; height: 20px; text-align: right; white-space: nowrap; word-break: normal; overflow-wrap: normal; }
</style>
