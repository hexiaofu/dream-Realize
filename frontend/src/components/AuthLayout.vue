<template>
  <wd-config-provider :theme-vars="theme">
    <wd-navbar left-arrow :bordered="false" safe-area-inset-top @click-left="returnBack" />
    <view class="auth-layout">
      <view class="auth-brand"><wd-icon name="heart-filled" size="20px" color="#238478" /><text>圆梦</text></view>
      <wd-card>
        <view class="auth-heading">{{ title }}</view>
        <slot />
      </wd-card>
      <view class="auth-alternative"><text class="muted">{{ alternativeHint }}</text><wd-button type="text" :disabled="busy" @click="switchPage">{{ alternativeLabel }}</wd-button></view>
    </view>
  </wd-config-provider>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
const props = defineProps({ title: String, alternativeHint: String, alternativeLabel: String, alternativeUrl: String, busy: Boolean })
const theme = {
  colorTheme: '#238478', colorSuccess: '#238478', buttonPrimaryBgColor: '#238478',
  navbarBackground: '#f5f7f6', cardMargin: '0', cardPadding: '28px 24px', cardRadius: '24px',
  cardShadowColor: '0 8px 32px rgba(38, 62, 57, 0.04)', cardContentColor: '#263e39',
  cellGroupInsertMargin: '0', cellGroupInsertRadius: '12px',
  inputCellBg: '#f5f7f6', inputBg: 'transparent', inputCellPadding: '16px', inputPadding: '14px',
  inputCellLabelWidth: '58px', inputFs: '14px', inputIconColor: '#7c8a86',
  buttonLargeHeight: '48px', buttonLargeRadius: '12px'
}
function switchPage() { if (!props.busy) uni.redirectTo({ url: props.alternativeUrl }) }
function returnBack() { if (props.busy) return; if (getCurrentPages().length > 1) uni.navigateBack(); else uni.reLaunch({ url: '/pages/discover/index' }) }
</script>
<style scoped>
.auth-layout { max-width: 440px; margin: 0 auto; padding: 36px 20px 40px; }
.auth-brand { display: flex; align-items: center; justify-content: center; gap: 8px; margin-bottom: 28px; font-size: 19px; font-weight: 600; }
.auth-heading { margin-bottom: 28px; font-size: 26px; line-height: 1.4; font-weight: 650; }
.auth-alternative { display: flex; align-items: center; justify-content: center; gap: 8px; margin-top: 20px; }
</style>
