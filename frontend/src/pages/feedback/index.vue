<template>
  <AppShell title="意见反馈" back>
    <view class="feedback-intro">
      <view class="intro-icon"><wd-icon name="edit-outline" size="20px" color="#fff" /></view>
      <view class="grow"><view class="intro-title">说说你的想法</view><view class="muted">每一条建议我们都会认真阅读，帮圆梦变得更好</view></view>
    </view>
    <view class="feedback-form">
      <view class="form-label">反馈类型</view>
      <view class="chip-row">
        <view v-for="item in categories" :key="item.value" class="type-chip" :class="{ 'is-selected': form.category === item.value }" hover-class="is-pressed" @click="form.category = item.value">
          <text class="chip-emoji">{{ item.emoji }}</text>{{ item.label }}
        </view>
      </view>
      <view class="form-label">反馈内容</view>
      <wd-textarea v-model="form.content" placeholder="遇到了什么问题，或者希望增加什么功能？" :maxlength="1000" show-word-limit auto-height :disabled="saving" custom-class="feedback-textarea" />
      <view class="form-label">联系方式（选填）</view>
      <wd-input v-model="form.contact" placeholder="微信、邮箱或手机号，方便我们进一步沟通" :maxlength="100" clearable :disabled="saving" custom-class="feedback-input" />
      <view class="actions"><wd-button block size="large" :loading="saving" :disabled="!form.content.trim()" @click="submit">提交反馈</wd-button></view>
    </view>

    <view class="row between section mine-head"><view class="subheading">我的反馈</view><text v-if="items.length" class="muted">最近 {{ items.length }} 条</text></view>
    <DreamLoading v-if="loading && !items.length" inline text="正在取回你的反馈" />
    <view v-else-if="!items.length" class="muted center mine-empty">还没有提交过反馈</view>
    <view class="stagger" :key="listVersion">
      <view v-for="item in items" :key="item.id" class="mine-card">
        <view class="row between"><text class="mine-type">{{ categoryLabel(item.category) }}</text><text class="status-pill" :class="'is-' + item.status.toLowerCase()">{{ statusLabel[item.status] }}</text></view>
        <view class="body-text mine-content">{{ item.content }}</view>
        <view v-if="item.reply" class="mine-reply"><text class="reply-label">官方回复</text>{{ item.reply }}</view>
        <view class="muted mine-time">{{ date(item.createdAt) }}</view>
      </view>
    </view>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import DreamLoading from '../../components/DreamLoading.vue'
import { date, notify, request, requireLogin, session } from '../../lib/api'
const categories = [
  { value: 'SUGGESTION', label: '功能建议', emoji: '💡' },
  { value: 'BUG', label: '问题反馈', emoji: '🐞' },
  { value: 'EXPERIENCE', label: '内容与体验', emoji: '🌈' },
  { value: 'OTHER', label: '其他', emoji: '💬' }
]
const statusLabel = { NEW: '待处理', PROCESSING: '处理中', RESOLVED: '已处理' }
const form = reactive({ category: 'SUGGESTION', content: '', contact: '' })
const items = ref([]), loading = ref(false), saving = ref(false), listVersion = ref(0)
const categoryLabel = value => categories.find(item => item.value === value)?.label || value
async function load() {
  if (!session.token || loading.value) return
  loading.value = true
  try { items.value = await request('/feedback/mine') } catch (e) { notify(e) } finally { loading.value = false }
}
async function submit() {
  if (saving.value || !requireLogin()) return
  if (!form.content.trim()) return notify('先写下你的想法吧')
  saving.value = true
  try {
    items.value = await request('/feedback', 'POST', { ...form }); listVersion.value++
    form.content = ''; form.contact = ''
    uni.showToast({ title: '感谢你的反馈', icon: 'success' })
  } catch (e) { notify(e) } finally { saving.value = false }
}
onShow(() => { if (requireLogin()) load() })
</script>
<style scoped>
/* 意见反馈：顶部说明、类型胶囊、表单，以及带处理状态与官方回复的历史记录。 */
.feedback-intro { display: flex; align-items: center; gap: 14px; padding: 16px 18px; margin-bottom: 16px; border-radius: 20px; background: var(--dream-glass);
  border: 1px solid var(--dream-glass-line); animation: dream-rise .5s var(--dream-ease) backwards; }
.intro-icon { display: flex; align-items: center; justify-content: center; flex-shrink: 0; width: 42px; height: 42px; border-radius: 14px; background: var(--dream-gradient); box-shadow: 0 8px 18px rgba(110, 86, 248, .3); }
.intro-title { font-size: 17px; font-weight: 700; }
.feedback-form { padding: 16px; border-radius: 20px; background: var(--dream-glass); border: 1px solid var(--dream-glass-line); animation: dream-rise .55s var(--dream-ease) .05s backwards; }
.form-label { margin: 4px 0 10px; font-size: 13px; font-weight: 650; color: var(--dream-muted); letter-spacing: 1px; }
.form-label + .chip-row, .feedback-form :deep(.feedback-textarea), .feedback-form :deep(.feedback-input) { margin-bottom: 16px; }
.chip-row { display: flex; flex-wrap: wrap; gap: 8px; }
.type-chip { display: flex; align-items: center; gap: 5px; padding: 8px 14px; border-radius: 999px; font-size: 13px; color: var(--dream-ink);
  background: rgba(255, 255, 255, .45); border: 1px solid rgba(255, 255, 255, .7); transition: background .3s ease, color .3s ease, box-shadow .3s ease, transform .3s var(--dream-spring); }
.type-chip.is-pressed { transform: scale(.92); }
.type-chip.is-selected { color: #fff; background: var(--dream-gradient); border-color: transparent; box-shadow: 0 6px 16px rgba(110, 86, 248, .3); }
.chip-emoji { font-size: 14px; }
.feedback-form :deep(.feedback-textarea), .feedback-form :deep(.feedback-input) { border-radius: 14px; background: rgba(255, 255, 255, .5) !important; padding: 10px 12px; }
.mine-head { margin-top: 24px; }
.mine-empty { padding: 16px 0 8px; }
.mine-card { padding: 14px 16px; margin-bottom: 12px; border-radius: 18px; background: var(--dream-glass); border: 1px solid var(--dream-glass-line); }
.mine-type { font-size: 14px; font-weight: 700; color: var(--dream-ink); }
.status-pill { padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 600; }
.status-pill.is-new { color: #E8930C; background: rgba(255, 184, 77, .18); }
.status-pill.is-processing { color: var(--dream-primary); background: rgba(110, 86, 248, .12); }
.status-pill.is-resolved { color: var(--dream-mint); background: rgba(32, 180, 134, .14); }
.mine-content { margin-top: 8px; font-size: 14px; }
.mine-reply { margin-top: 10px; padding: 10px 12px; border-radius: 12px; font-size: 13px; line-height: 1.7; color: var(--dream-ink); background: rgba(110, 86, 248, .08); border-left: 3px solid var(--dream-primary); }
.reply-label { display: block; margin-bottom: 2px; font-size: 12px; font-weight: 700; color: var(--dream-primary); }
.mine-time { margin-top: 6px; font-size: 12px; }
</style>
