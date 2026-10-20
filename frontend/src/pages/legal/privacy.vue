<template>
  <AppShell title="隐私政策" back>
    <NoticeDocument :notice="privacyPolicy" />
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import AppShell from '../../components/AppShell.vue'
import NoticeDocument from '../../components/NoticeDocument.vue'
import { privacyPolicy } from '../../lib/privacy'
</script>
