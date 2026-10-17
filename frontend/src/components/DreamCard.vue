<template>
  <wd-card :title="dream.title" @click="go('/pages/dream/detail?id=' + dream.id)">
    <view class="row section">
      <wd-tag v-if="dream.hidden" type="danger">已下架</wd-tag>
      <wd-tag plain :type="dream.status === 'COMPLETED' ? 'success' : 'primary'">{{ label(statuses, dream.status) }}</wd-tag>
      <wd-tag type="default" plain>{{ label(helpTypes, dream.helpType) }}</wd-tag>
      <wd-tag v-if="dream.rewardType === 'FREE'" type="success" plain>期待无偿帮助</wd-tag>
    </view>
    <view v-if="dream.hidden" class="muted">{{ dream.moderationReason }}</view>
    <view v-if="dream.story" class="body-text clamp">{{ dream.story }}</view>
    <MediaGallery :media="dream.media" compact />
    <view v-if="dream.targetAmount" class="space" />
    <wd-text v-if="dream.targetAmount" :text="'心愿金额 ¥' + Number(dream.targetAmount).toLocaleString()" type="primary" bold />
    <view v-if="!dream.hidden" class="actions"><DreamReactions :dream="dream" show-comments /></view>
    <template #footer>
      <view class="row between">
        <view class="row muted"><UserAvatar :src="dream.authorAvatar" :size="28" /><text>{{ dream.authorName }}<text v-if="dream.city"> · {{ dream.city }}</text></text></view>
        <wd-text text="查看详情 →" type="primary" size="13px" />
      </view>
    </template>
  </wd-card>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { go, label, statuses, helpTypes } from '../lib/api'
import MediaGallery from './MediaGallery.vue'
import UserAvatar from './UserAvatar.vue'
import DreamReactions from './DreamReactions.vue'
defineProps({ dream: { type: Object, required: true } })
</script>
