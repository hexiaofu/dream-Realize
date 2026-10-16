<script>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
export default { onLaunch() {} }
</script>
<style lang="scss">
/* 组件外观由 Wot 提供；此处仅为页面布局、排版和统一主题。 */
page { background: #f5f7f6; color: #263e39; font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif; font-size: 14px; }
.page-body { max-width: 720px; margin: auto; padding: 20px 16px 96px; }
.section { margin-bottom: 20px; }
.stack > * + * { margin-top: 14px; }
.row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.between { justify-content: space-between; }
.grow { flex: 1; min-width: 0; }
.heading { font-size: 24px; font-weight: 650; line-height: 1.5; margin: 0 0 8px; }
.subheading { font-size: 18px; font-weight: 600; line-height: 1.6; }
.muted { color: #7c8a86; font-size: 13px; line-height: 1.8; }
.body-text { line-height: 1.9; white-space: pre-wrap; overflow-wrap: anywhere; }
.clamp { display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.space { height: 16px; }
.center { text-align: center; }
.actions { margin-top: 20px; }
.message-content { padding: 12px 0; }
</style>
