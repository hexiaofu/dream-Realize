<template>
  <wd-card :title="dream.title" :custom-class="'dream-card is-' + (dream.status || 'OPEN').toLowerCase()" @click="go('/pages/dream/detail?id=' + dream.id)">
    <view v-if="dream.hidden" class="section"><wd-tag type="danger">已下架</wd-tag></view>
    <view v-if="dream.status === 'COMPLETED'" class="completed-badge"><wd-icon name="star-on" size="12px" color="#fff" /> 已圆梦</view>
    <view v-if="dream.hidden" class="muted">{{ dream.moderationReason }}</view>
    <view v-if="dream.story" class="body-text clamp">{{ dream.story }}</view>
    <MediaGallery :media="dream.media" compact />
    <view v-if="dream.targetAmount" class="space" />
    <wd-text v-if="dream.targetAmount" :text="'心愿金额 ¥' + Number(dream.targetAmount).toLocaleString()" type="primary" bold />
    <view v-if="!dream.hidden" class="actions"><DreamReactions :dream="dream" show-comments compact /></view>
    <template #footer>
      <view class="card-footer">
        <view class="card-author"><UserAvatar :src="dream.authorAvatar" :size="34" /><view class="card-author__meta"><text class="card-author__name">{{ dream.authorName }}</text><view v-if="dream.city" class="card-author__city"><wd-icon name="location" size="11px" color="#A9A4C2" />{{ dream.city }}</view></view></view>
        <view class="more-pill"><text class="more-pill__text">详情</text><view class="more-pill__arrow"><wd-icon name="arrow-right" size="12px" color="#fff" /></view></view>
      </view>
    </template>
  </wd-card>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { go } from '../lib/api'
import MediaGallery from './MediaGallery.vue'
import UserAvatar from './UserAvatar.vue'
import DreamReactions from './DreamReactions.vue'
defineProps({ dream: { type: Object, required: true } })
</script>
<style scoped>
/* 左上角按梦想状态透出一团柔光（紫 / 金 / 灰），代替左侧色条；已圆梦另带金色徽章。 */
.dream-card::before { content: ''; position: absolute; top: -70px; left: -60px; width: 200px; height: 170px; border-radius: 50%; pointer-events: none; z-index: -1;
  background: radial-gradient(circle, rgba(155, 107, 255, .26), rgba(155, 107, 255, 0) 70%); animation: dream-breathe 5s ease-in-out infinite; }
.dream-card.is-completed::before { background: radial-gradient(circle, rgba(255, 196, 107, .34), rgba(255, 196, 107, 0) 70%); }
.dream-card.is-paused::before { background: radial-gradient(circle, rgba(170, 164, 194, .25), rgba(170, 164, 194, 0) 70%); }
.dream-card :deep(.wd-card__title-content) { font-weight: 700; line-height: 1.5; }
.dream-card.is-completed :deep(.wd-card__title-content) { padding-right: 34px; }
/* 底部作者：头像、昵称、城市同一行垂直居中，长昵称省略。 */
.card-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.card-author { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; }
.card-author__meta { display: flex; align-items: center; gap: 8px; min-width: 0; line-height: 1.35; }
.card-author__name { font-size: 14px; font-weight: 600; color: var(--dream-ink); overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.card-author__city { display: flex; align-items: center; gap: 2px; flex-shrink: 0; font-size: 12px; color: var(--dream-muted); }
.completed-badge { position: absolute; top: 16px; right: -32px; width: 112px; padding: 3px 0; transform: rotate(45deg); text-align: center; font-size: 11px; color: #fff;
  background: linear-gradient(90deg, #FFC66B, #FF9E5E); box-shadow: 0 4px 10px rgba(255, 158, 94, .35); animation: ribbon-in .7s var(--dream-spring) .3s backwards; }
@keyframes ribbon-in { from { opacity: 0; transform: translate(30px, -30px) rotate(45deg); } to { opacity: 1; transform: rotate(45deg); } }
</style>
