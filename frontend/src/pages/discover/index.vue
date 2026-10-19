<template>
  <AppShell active="discover" title="圆梦">
    <view class="discover-search"><wd-search v-model="keyword" placeholder="搜索梦想" hide-cancel @search="reload" @clear="reload" /></view>
    <wd-tabs v-model="category" slidable animated @change="reload">
      <wd-tab v-for="item in categories" :key="item.value" :name="item.value" :title="item.label" />
    </wd-tabs>
    <view class="space" />
    <view class="row between section">
      <view class="subheading">梦想广场</view>
      <view class="filter-toggle" :class="{ 'is-open': showFilters, 'is-active': activeFilters }" hover-class="is-pressed" @click="showFilters = !showFilters">
        <wd-icon name="filter" size="15px" /><text>筛选</text>
        <text v-if="activeFilters" :key="activeFilters" class="filter-badge">{{ activeFilters }}</text>
      </view>
    </view>
    <view class="filter-panel" :class="{ 'is-open': showFilters }">
      <view class="filter-inner">
        <view class="filter-group">
          <view class="filter-title">我能提供</view>
          <view class="chip-row">
            <view v-for="item in helpOptions" :key="item.value" class="filter-chip" :class="{ 'is-selected': helpType === item.value }" hover-class="is-pressed" @click="pick('help', item.value)">
              <text v-if="item.emoji" class="chip-emoji">{{ item.emoji }}</text>{{ item.label }}
            </view>
          </view>
        </view>
        <view class="filter-group">
          <view class="filter-title">梦想状态</view>
          <view class="chip-row">
            <view v-for="item in statusOptions" :key="item.value" class="filter-chip" :class="['is-' + (item.value || 'all').toLowerCase(), { 'is-selected': status === item.value }]" hover-class="is-pressed" @click="pick('status', item.value)">
              <view v-if="item.value" class="chip-dot" />{{ item.label }}
            </view>
          </view>
        </view>
        <view class="filter-footer">
          <text class="muted">共找到 <text class="filter-total">{{ total }}</text> 个梦想</text>
          <view class="filter-reset" :class="{ 'is-disabled': !activeFilters }" hover-class="is-pressed" @click="resetFilters"><wd-icon name="refresh" size="13px" />重置</view>
        </view>
      </view>
    </view>
    <wd-status-tip url-prefix="/static/status/" v-if="error" image="network" :tip="error"><template #bottom><wd-button size="small" @click="reload">重新加载</wd-button></template></wd-status-tip>
    <wd-status-tip url-prefix="/static/status/" v-else-if="!loading && !items.length" image="search" tip="暂无匹配的梦想" />
    <DreamSkeleton v-if="loading && !items.length" />
    <view class="stagger" :key="listVersion"><DreamCard v-for="dream in items" :key="dream.id" :dream="dream" /></view>
    <view class="center section"><DreamLoading v-if="loading && items.length" inline text="更多梦想正在赶来" /><wd-button v-else-if="hasMore" plain block @click="loadMore">加载更多</wd-button><view v-else-if="items.length" class="list-end">· 已经看到最远的星星了 ·</view></view>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { computed, ref } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import DreamCard from '../../components/DreamCard.vue'
import DreamLoading from '../../components/DreamLoading.vue'
import DreamSkeleton from '../../components/DreamSkeleton.vue'
import { request, query, categories, helpTypes, statuses } from '../../lib/api'
const items = ref([]), keyword = ref(''), category = ref(''), helpType = ref(''), status = ref('')
const showFilters = ref(false), loading = ref(false), hasMore = ref(false), error = ref(''), listVersion = ref(0), total = ref(0)
const helpEmoji = { EXPERIENCE: '💡', SKILL: '🛠️', CASH: '💰', GOODS: '🎁', COMPANY: '🤝', RESOURCE: '🔗', OTHER: '✨' }
const helpOptions = [{ value: '', label: '全部' }, ...helpTypes.map(item => ({ ...item, emoji: helpEmoji[item.value] }))]
const statusOptions = [{ value: '', label: '全部' }, ...statuses]
const activeFilters = computed(() => (helpType.value ? 1 : 0) + (status.value ? 1 : 0))
function pick(kind, value) {
  const target = kind === 'help' ? helpType : status
  if (target.value === value) return
  target.value = value; reload()
}
function resetFilters() { if (!activeFilters.value) return; helpType.value = ''; status.value = ''; reload() }
let page = 1, generation = 0
async function load(reset = false) {
  if (!reset && loading.value) return
  const current = ++generation
  loading.value = true; error.value = ''
  const requestedPage = reset ? 1 : page + 1
  try {
    const result = await request('/dreams?' + query({ page: requestedPage, keyword: keyword.value, category: category.value, helpType: helpType.value, status: status.value }))
    if (current !== generation) return
    if (reset) listVersion.value++
    items.value = reset ? result.items : [...items.value, ...result.items]; hasMore.value = result.hasMore; total.value = result.total; page = requestedPage
  } catch (e) { if (current === generation) error.value = e.message }
  finally { if (current === generation) loading.value = false }
}
function reload() { load(true) }
function loadMore() { if (hasMore.value && !loading.value) load() }
onShow(reload)
onReachBottom(loadMore)
</script>
<style scoped>
.discover-search :deep(.wd-search) { padding: 0 0 6px; background: transparent; }
.discover-search :deep(.wd-search__block) { background: var(--dream-glass-strong) !important; backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); box-shadow: 0 6px 18px rgba(84, 62, 190, .08); transition: box-shadow .3s ease; }
.discover-search :deep(.wd-search__block:focus-within) { box-shadow: 0 0 0 3px rgba(110, 86, 248, .18), 0 6px 18px rgba(84, 62, 190, .1); }
/* 分类栏吸顶，滚动浏览时随时切换。 */
:deep(.wd-tabs) { position: sticky; top: 0; z-index: 10; margin: 0 -16px; padding: 0 8px; background: rgba(247, 244, 255, .5) !important; backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); }
:deep(.wd-tabs__nav) { background: transparent !important; }
:deep(.wd-tabs__nav-item) { transition: color .3s ease, transform .3s var(--dream-spring); }
:deep(.wd-tabs__nav-item.is-active) { font-weight: 700; transform: scale(1.06); }
:deep(.wd-tabs__line) { border-radius: 4px; background: var(--dream-gradient) !important; }
/* 筛选按钮：有生效条件时显示角标；展开时图标翻转。 */
.filter-toggle { position: relative; display: flex; align-items: center; gap: 4px; padding: 6px 14px; border-radius: 999px; font-size: 13px; color: var(--dream-primary);
  background: rgba(255, 255, 255, .35); border: 1px solid rgba(110, 86, 248, .2); transition: all .3s var(--dream-ease); }
.filter-toggle :deep(.wd-icon) { transition: transform .4s var(--dream-spring); }
.filter-toggle.is-open :deep(.wd-icon) { transform: rotate(180deg); }
.filter-toggle.is-open, .filter-toggle.is-active { background: var(--dream-gradient); border-color: transparent; color: #fff; box-shadow: 0 6px 16px rgba(110, 86, 248, .3); }
.filter-toggle.is-pressed { transform: scale(.92); }
.filter-badge { position: absolute; top: -6px; right: -6px; min-width: 18px; height: 18px; padding: 0 4px; border-radius: 999px; box-sizing: border-box; text-align: center;
  font-size: 11px; line-height: 16px; color: var(--dream-primary-deep); background: #FFD58A; border: 1px solid #fff; animation: dream-pop .45s var(--dream-spring) backwards; }
/* 面板：高度与透明度同时过渡，内容自上而下依次浮现。 */
.filter-panel { max-height: 0; opacity: 0; overflow: hidden; transform: translateY(-8px); transition: max-height .5s var(--dream-ease), opacity .35s ease, transform .45s var(--dream-ease), margin .4s ease; }
.filter-panel.is-open { max-height: 520px; opacity: 1; transform: none; margin-bottom: 18px; }
.filter-inner { padding: 16px; border-radius: 20px; background: var(--dream-glass); border: 1px solid var(--dream-glass-line); box-shadow: 0 10px 30px rgba(84, 62, 190, .08); }
.filter-group + .filter-group { margin-top: 16px; }
.filter-group { opacity: 0; transform: translateY(8px); transition: opacity .4s ease, transform .45s var(--dream-ease); }
.is-open .filter-group { opacity: 1; transform: none; }
.is-open .filter-group:nth-child(2) { transition-delay: .08s; }
.filter-title { margin-bottom: 10px; font-size: 13px; font-weight: 650; color: var(--dream-muted); letter-spacing: 1px; }
.chip-row { display: flex; flex-wrap: wrap; gap: 8px; }
.filter-chip { display: flex; align-items: center; gap: 5px; padding: 7px 14px; border-radius: 999px; font-size: 13px; color: var(--dream-ink);
  background: rgba(255, 255, 255, .45); border: 1px solid rgba(255, 255, 255, .7); transition: background .3s ease, color .3s ease, box-shadow .3s ease, transform .3s var(--dream-spring); }
.filter-chip.is-pressed { transform: scale(.92); }
.filter-chip.is-selected { color: #fff; background: var(--dream-gradient); border-color: transparent; box-shadow: 0 6px 16px rgba(110, 86, 248, .3); animation: chip-pop .45s var(--dream-spring); }
@keyframes chip-pop { 0% { transform: scale(.9); } 60% { transform: scale(1.08); } 100% { transform: scale(1); } }
.chip-emoji { font-size: 14px; }
.chip-dot { width: 7px; height: 7px; border-radius: 50%; background: #9B6BFF; }
.filter-chip.is-in_progress .chip-dot { background: #FF8DB8; }
.filter-chip.is-completed .chip-dot { background: #FFB84D; }
.filter-chip.is-paused .chip-dot { background: #B4AFCB; }
.filter-chip.is-selected .chip-dot { background: #fff; }
.filter-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 16px; padding-top: 12px; border-top: 1px dashed rgba(110, 86, 248, .18); }
.filter-total { color: var(--dream-primary); font-weight: 700; }
.filter-reset { display: flex; align-items: center; gap: 4px; font-size: 13px; color: var(--dream-primary); transition: opacity .3s ease, transform .3s var(--dream-spring); }
.filter-reset.is-disabled { opacity: .35; }
.filter-reset.is-pressed { transform: scale(.9); }
.list-end { color: #B4AFCB; font-size: 12px; letter-spacing: 2px; padding: 8px 0; animation: dream-rise .6s var(--dream-ease) backwards; }
</style>
