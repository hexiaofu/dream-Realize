<template>
  <view class="notice-document">
    <view class="heading">{{ notice.title }}</view>
    <view class="muted section">版本 {{ notice.version }} · 生效日期 {{ notice.effectiveDate }}</view>
    <view v-for="section in notice.sections" :key="section.title" class="section">
      <view class="subheading">{{ section.title }}</view>
      <view v-for="paragraph in section.paragraphs" :key="paragraph.text" class="body-text section"><wd-text :text="paragraph.text" :bold="paragraph.important" color="#263e39" /></view>
    </view>
    <wd-cell-group border>
      <wd-cell title="运营方" :label="notice.operator" />
      <wd-cell title="联系邮箱" :label="notice.contactEmail" />
    </wd-cell-group>
  </view>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
defineProps({ notice: { type: Object, required: true } })
</script>
