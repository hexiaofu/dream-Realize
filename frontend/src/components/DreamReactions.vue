<template>
  <view class="row dream-reactions" @click.stop>
    <wd-button size="small" icon="thumb-up" :plain="state.myReaction !== 'SUPPORT'" :disabled="busy || restricted" @click="react('SUPPORT')">精神支持 {{ state.supportCount }}</wd-button>
    <wd-button size="small" type="info" icon="thumb-down" :plain="state.myReaction !== 'DISAGREE'" :disabled="busy || restricted" @click="react('DISAGREE')">不同意 {{ state.disagreeCount }}</wd-button>
    <wd-button v-if="showComments" size="small" type="text" icon="chat" @click="openComments">评论 {{ dream.commentCount || 0 }}</wd-button>
  </view>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { computed, reactive, ref, watch } from 'vue'
import { go, notify, request, requireLogin, session } from '../lib/api'
const props = defineProps({ dream: { type: Object, required: true }, showComments: Boolean, popupComments: Boolean })
const emit = defineEmits(['change', 'comments'])
const state = reactive({ supportCount: 0, disagreeCount: 0, myReaction: 'NONE' }), busy = ref(false)
const restricted = computed(() => session.user?.restriction?.mode === 'READ_ONLY')
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
  } catch(e) { notify(e) } finally { busy.value = false }
}
</script>
