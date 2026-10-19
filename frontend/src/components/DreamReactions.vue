<template>
  <view class="row dream-reactions" :class="{ 'is-compact': compact }" @click.stop>
    <view class="reaction-slot">
      <wd-button size="small" icon="thumb-up" :plain="state.myReaction !== 'SUPPORT'" :disabled="busy || restricted" :custom-class="burst === 'SUPPORT' ? 'is-bursting' : ''" @click="react('SUPPORT')"><text :class="{ 'sr-only': compact }">精神支持</text> <text :key="'s' + state.supportCount" class="reaction-count">{{ state.supportCount }}</text></wd-button>
      <view v-if="burst === 'SUPPORT'" class="reaction-burst"><text v-for="p in particles" :key="p.id" class="burst-particle" :style="p.style">{{ p.char }}</text></view>
    </view>
    <view class="reaction-slot">
      <wd-button size="small" type="info" icon="thumb-down" :plain="state.myReaction !== 'DISAGREE'" :disabled="busy || restricted" :custom-class="burst === 'DISAGREE' ? 'is-bursting' : ''" @click="react('DISAGREE')"><text :class="{ 'sr-only': compact }">不同意</text> <text :key="'d' + state.disagreeCount" class="reaction-count">{{ state.disagreeCount }}</text></wd-button>
    </view>
    <wd-button v-if="showComments" size="small" type="text" icon="chat" @click="openComments"><text :class="{ 'sr-only': compact }">评论</text> {{ dream.commentCount || 0 }}</wd-button>
  </view>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { computed, reactive, ref, watch } from 'vue'
import { go, notify, request, requireLogin, session } from '../lib/api'
const props = defineProps({ dream: { type: Object, required: true }, showComments: Boolean, popupComments: Boolean, compact: Boolean })
const emit = defineEmits(['change', 'comments'])
const state = reactive({ supportCount: 0, disagreeCount: 0, myReaction: 'NONE' }), busy = ref(false), burst = ref('')
const restricted = computed(() => session.user?.restriction?.mode === 'READ_ONLY')
// 表态成功时向四周散开的小心心与星星。
const particles = Array.from({ length: 8 }, (_, id) => {
  const angle = id / 8 * Math.PI * 2, distance = 26 + (id % 2) * 10
  return { id, char: id % 2 ? '✦' : '♥', style: `--dx:${Math.round(Math.cos(angle) * distance)}px;--dy:${Math.round(Math.sin(angle) * distance)}px;animation-delay:${id * 15}ms;color:${id % 3 ? '#FF8DB8' : '#FFB84D'}` }
})
let burstTimer
function openComments() {
  if (props.popupComments) emit('comments')
  else go('/pages/dream/detail?id=' + props.dream.id + '&comments=1')
}
watch(() => [props.dream.id, props.dream.supportCount, props.dream.disagreeCount, props.dream.myReaction, session.token], () => {
  Object.assign(state, { supportCount: props.dream.supportCount || 0, disagreeCount: props.dream.disagreeCount || 0, myReaction: session.token ? props.dream.myReaction || 'NONE' : 'NONE' })
}, { immediate: true })
async function react(value) {
  if (busy.value || !requireLogin()) return
  busy.value = true
  try {
    const result = await request(`/dreams/${props.dream.id}/reaction`, 'PUT', { reaction: state.myReaction === value ? 'NONE' : value })
    const reaction = { myReaction: result.myReaction, supportCount: result.supportCount, disagreeCount: result.disagreeCount }
    Object.assign(state, reaction); emit('change', reaction)
    if (result.myReaction === value) { clearTimeout(burstTimer); burst.value = ''; setTimeout(() => { burst.value = value; burstTimer = setTimeout(() => { burst.value = '' }, 700) }, 0) }
  } catch(e) { notify(e) } finally { busy.value = false }
}
</script>
<style scoped>
/* 表态按钮：选中时图标弹跳，数字自上滚入，支持时迸发心心。 */
.reaction-slot { position: relative; }
/* 卡片上的紧凑模式：文字仅供读屏，按钮只露出图标与数量。 */
.is-compact { gap: 8px; }
.is-compact :deep(.wd-button.is-small) { min-width: 0; padding: 0 12px; }
.is-compact :deep(.wd-button.is-text) { padding: 0 6px; }
.is-compact .reaction-count { margin-left: 0; }
.reaction-count { display: inline-block; margin-left: 2px; font-weight: 650; animation: dream-number .4s var(--dream-spring) backwards; }
:deep(.is-bursting) { animation: dream-pop .45s var(--dream-spring) backwards; }
:deep(.is-bursting .wd-icon) { animation: dream-bounce .6s var(--dream-spring) both; }
.reaction-burst { position: absolute; left: 22px; top: 50%; width: 0; height: 0; pointer-events: none; }
.burst-particle { position: absolute; left: -5px; top: -8px; font-size: 12px; animation: dream-burst .7s var(--dream-ease) both; }
</style>
