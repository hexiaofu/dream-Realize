<script>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
export default { onLaunch() {} }
</script>
<style lang="scss">
/* 组件外观由 Wot 提供；此处为圆梦主题的页面布局、排版与统一动效。 */
page {
  --dream-primary: #6E56F8; --dream-primary-deep: #4B33C9; --dream-pink: #FF8DB8; --dream-gold: #FFB84D; --dream-mint: #20B486;
  --dream-ink: #2A2350; --dream-muted: #8A85A6; --dream-line: #ECE7FB; --dream-bg: #F7F4FF;
  --dream-gradient: linear-gradient(135deg, #6E56F8 0%, #9B6BFF 55%, #FF8DB8 100%);
  --dream-sky: linear-gradient(160deg, #24175E 0%, #4B33C9 42%, #8E62FF 72%, #FF9EC4 100%);
  --dream-glass: rgba(255, 255, 255, .26); --dream-glass-strong: rgba(255, 255, 255, .42); --dream-glass-line: rgba(255, 255, 255, .55); --dream-glass-blur: blur(6px) saturate(180%);
  --dream-ease: cubic-bezier(.22, 1, .36, 1); --dream-spring: cubic-bezier(.34, 1.56, .64, 1);
  background: var(--dream-bg); color: var(--dream-ink);
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif; font-size: 14px;
}
/* page-body 不设 z-index：保持在固定背景之上，同时内部弹层能盖过标签栏。 */
.page-body { position: relative; max-width: 720px; margin: auto; padding: 12px 16px 104px; animation: dream-page-in .55s var(--dream-ease) backwards; }
.section { margin-bottom: 20px; }
.stack > * + * { margin-top: 14px; }
.row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.between { justify-content: space-between; }
.grow { flex: 1; min-width: 0; }
.heading { font-size: 24px; font-weight: 700; line-height: 1.5; margin: 0 0 8px; }
.subheading { position: relative; font-size: 18px; font-weight: 700; line-height: 1.6; padding-left: 14px; }
.subheading::before { content: ''; position: absolute; left: 0; top: 50%; width: 6px; height: 18px; margin-top: -9px; border-radius: 6px; background: var(--dream-gradient); }
.muted { color: var(--dream-muted); font-size: 13px; line-height: 1.8; }
.body-text { line-height: 1.9; white-space: pre-wrap; overflow-wrap: anywhere; }
.clamp { display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.space { height: 16px; }
.center { text-align: center; }
.actions { margin-top: 20px; }
.message-content { padding: 12px 0; }
/* 仅供读屏与测试定位的文字，视觉上隐藏。 */
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; border: 0; }
.gradient-text { background: var(--dream-gradient); -webkit-background-clip: text; background-clip: text; color: transparent; }

/* ---------- 入场：页面、列表依次浮现 ---------- */
.stagger > view, .stagger > .wd-card { animation: dream-rise .6s var(--dream-ease) backwards; }
@for $i from 1 through 12 { .stagger > view:nth-child(#{$i}), .stagger > .wd-card:nth-child(#{$i}) { animation-delay: #{$i * 60}ms; } }
.fade-up { animation: dream-rise .6s var(--dream-ease) backwards; }
.pop-in { animation: dream-pop .5s var(--dream-spring) backwards; }

/* ---------- 卡片：柔和阴影、悬停抬升、按下回弹 ---------- */
.wd-card { position: relative; overflow: hidden; border: 1px solid rgba(110, 86, 248, .06);
  transition: transform .35s var(--dream-ease), box-shadow .35s var(--dream-ease); }
.wd-card:active { transform: scale(.985); }
@media (hover: hover) { .wd-card:hover { transform: translateY(-3px); box-shadow: 0 18px 40px rgba(84, 62, 190, .14) !important; } }
.wd-cell-group { border-radius: 18px; overflow: hidden; box-shadow: 0 8px 24px rgba(84, 62, 190, .06); animation: dream-rise .6s var(--dream-ease) backwards; }
.wd-cell { transition: background-color .25s ease; }
.wd-cell:active { background-color: #F3EFFF !important; }

/* ---------- 按钮：渐变、光泽扫过、按下涟漪 ---------- */
/* wd-button--active 是 Wot 的 hover-class，H5 与小程序都会在按下时加上。 */
.wd-button { overflow: hidden; transition: transform .3s var(--dream-spring), box-shadow .3s ease, opacity .3s ease !important; }
.wd-button.wd-button--active, .wd-button:active:not(.is-disabled) { transform: scale(.94); }
.wd-button::before { background: currentColor !important; border-radius: 999px !important; transform: translate(-50%, -50%) scale(0) !important; transition: transform .5s var(--dream-ease), opacity .6s ease; }
.wd-button.wd-button--active::before, .wd-button:active:not(.is-disabled):not(.is-text)::before { opacity: .2 !important; transform: translate(-50%, -50%) scale(1.05) !important; transition: transform .35s var(--dream-ease), opacity .05s; }
.wd-button.is-primary:not(.is-plain):not(.is-text):not(.is-disabled) { border-color: transparent !important; box-shadow: 0 8px 20px rgba(110, 86, 248, .32);
  background-image: linear-gradient(110deg, transparent 38%, rgba(255, 255, 255, .38) 50%, transparent 62%), var(--dream-gradient) !important;
  background-size: 260% 100%, 100% 100% !important; background-repeat: no-repeat; animation: dream-sheen 3.6s ease-in-out infinite; }
.wd-button.is-primary.is-disabled:not(.is-plain):not(.is-text) { background: #C9BFF5 !important; border-color: transparent !important; }
.wd-button.is-plain { background: rgba(255, 255, 255, .35) !important; }
.wd-button.is-loading { animation: dream-breathe 1.4s ease-in-out infinite !important; }
.wd-button.is-text { transition: transform .3s var(--dream-spring), opacity .2s ease !important; }

/* ---------- 标签、开关、进度条、头像 ---------- */
.wd-tag { border-radius: 999px !important; padding-left: 8px !important; padding-right: 8px !important; transition: transform .3s var(--dream-spring); animation: dream-pop .45s var(--dream-spring) backwards; }
.wd-switch { transition: background-color .35s var(--dream-ease) !important; }
.wd-progress__inner { background: var(--dream-gradient) !important; transition: width 1.2s var(--dream-ease) !important; }
.user-avatar { border-radius: 50%; transition: transform .35s var(--dream-spring); }
.user-avatar:active { transform: scale(.9) rotate(-6deg); }
.wd-img { transition: transform .35s var(--dream-ease), opacity .35s ease; }
.wd-img:active { transform: scale(.95); }
.wd-status-tip { animation: dream-rise .6s var(--dream-ease) backwards; }
.wd-status-tip__image { animation: dream-float 4s ease-in-out infinite; }
.wd-radio { transition: background-color .25s ease, transform .25s var(--dream-spring); }
.wd-radio:active { transform: scale(.98); }
.wd-radio.is-checked { background: rgba(110, 86, 248, .1); }
.wd-popup { overflow: hidden; }
/* 弹层内的 page-body 不建立层叠上下文，避免盖住关闭按钮。 */
.wd-popup .page-body { position: static; animation-duration: .45s; }

/* ---------- 毛玻璃：页面内的块半透明，透出底层极光 ---------- */
.page-body .wd-card { background: var(--dream-glass) !important; border-color: var(--dream-glass-line) !important;
  backdrop-filter: var(--dream-glass-blur); -webkit-backdrop-filter: var(--dream-glass-blur); }
/* 列表组内含选择器弹层（position: fixed），不能加 backdrop-filter，否则弹层会被困在组内。 */
.page-body .wd-cell-group { background: rgba(255, 255, 255, .3) !important; border: 1px solid var(--dream-glass-line); }
.page-body .wd-cell-group__title, .page-body .wd-cell-group__body, .page-body .wd-cell, .page-body .wd-input, .page-body .wd-textarea,
.page-body .wd-textarea__value, .page-body .wd-select-picker__cell { background: transparent !important; }
.wd-popup .page-body .wd-cell-group { background: rgba(255, 255, 255, .5) !important; }
/* 弹层本身也半透明，透出后面的页面；弹层是 fixed，加模糊不影响内部选择器。 */
/* 遮罩改为淡紫，避免白色毛玻璃叠在黑色遮罩上发灰。 */
.wd-overlay { background: rgba(58, 40, 140, .28) !important; }
.wd-popup { background: rgba(250, 247, 255, .8) !important; backdrop-filter: blur(20px) saturate(180%); -webkit-backdrop-filter: blur(20px) saturate(180%); }
.wd-popup .wd-cell, .wd-popup .wd-cell-group__title, .wd-popup .wd-cell-group__body, .wd-popup .wd-input, .wd-popup .wd-textarea, .wd-popup .wd-textarea__value { background: transparent !important; }
.page-body .wd-search__cover { background: transparent !important; }
.page-body .wd-segmented { background: rgba(255, 255, 255, .3) !important; }
.page-body .wd-upload__evoke { background: rgba(255, 255, 255, .3) !important; border: 1px dashed rgba(110, 86, 248, .25); border-radius: 12px; transition: transform .3s var(--dream-spring), background-color .3s ease; }
.page-body .wd-upload__evoke:active { transform: scale(.94); background: rgba(255, 255, 255, .8) !important; }
.page-body .wd-textarea__count { background: transparent !important; }

/* ---------- 卡片底部“详情”胶囊：光泽扫过，卡片悬停/按下时填满渐变、箭头前移 ---------- */
.more-pill { position: relative; overflow: hidden; display: inline-flex; align-items: center; gap: 8px; height: 32px; padding: 0 4px 0 14px; border-radius: 999px;
  background: rgba(110, 86, 248, .1); border: 1px solid rgba(110, 86, 248, .18); transition: background .35s var(--dream-ease), box-shadow .35s ease, transform .3s var(--dream-spring), border-color .35s ease; }
.more-pill::after { content: ''; position: absolute; top: 0; bottom: 0; left: -40%; width: 30%; pointer-events: none;
  background: linear-gradient(110deg, transparent, rgba(255, 255, 255, .75), transparent); animation: pill-sheen 3.8s ease-in-out infinite; }
.more-pill__text { font-size: 13px; font-weight: 650; letter-spacing: 1px; background: var(--dream-gradient); -webkit-background-clip: text; background-clip: text; color: transparent; }
.more-pill__arrow { display: flex; align-items: center; justify-content: center; width: 24px; height: 24px; border-radius: 50%; background: var(--dream-gradient);
  box-shadow: 0 4px 10px rgba(110, 86, 248, .35); transition: transform .4s var(--dream-spring), background .35s ease; animation: pill-nudge 2.4s ease-in-out infinite; }
.wd-card:active .more-pill, .more-pill:active { transform: scale(.95); background: var(--dream-gradient); border-color: transparent; box-shadow: 0 8px 18px rgba(110, 86, 248, .35); }
.wd-card:active .more-pill__text, .more-pill:active .more-pill__text { color: #fff; -webkit-text-fill-color: #fff; }
.wd-card:active .more-pill__arrow, .more-pill:active .more-pill__arrow { background: rgba(255, 255, 255, .25); transform: translateX(3px); animation: none; }
@media (hover: hover) {
  .wd-card:hover .more-pill { background: var(--dream-gradient); border-color: transparent; box-shadow: 0 8px 18px rgba(110, 86, 248, .35); }
  .wd-card:hover .more-pill__text { color: #fff; -webkit-text-fill-color: #fff; }
  .wd-card:hover .more-pill__arrow { background: rgba(255, 255, 255, .25); transform: translateX(3px); animation: none; }
}
@keyframes pill-sheen { 0%, 60% { left: -40%; } 100% { left: 130%; } }
@keyframes pill-nudge { 0%, 70%, 100% { transform: translateX(0); } 85% { transform: translateX(3px); } }

/* ---------- 骨架屏 ---------- */
.skeleton { position: relative; overflow: hidden; background: rgba(110, 86, 248, .1); border-radius: 10px; }
.skeleton::after { content: ''; position: absolute; top: 0; right: 0; bottom: 0; left: 0; transform: translateX(-100%);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, .75), transparent); animation: dream-shimmer 1.4s ease-in-out infinite; }

/* ---------- 动画关键帧 ---------- */
@keyframes dream-page-in { from { opacity: 0; transform: translateY(14px); } to { opacity: 1; transform: none; } }
@keyframes dream-rise { from { opacity: 0; transform: translateY(18px) scale(.98); } to { opacity: 1; transform: none; } }
@keyframes dream-pop { 0% { opacity: 0; transform: scale(.6); } 70% { opacity: 1; transform: scale(1.06); } 100% { transform: scale(1); } }
@keyframes dream-float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-8px); } }
@keyframes dream-twinkle { 0%, 100% { opacity: .25; transform: scale(.7); } 50% { opacity: 1; transform: scale(1.15); } }
@keyframes dream-shimmer { to { transform: translateX(100%); } }
@keyframes dream-sheen { 0%, 45% { background-position: 130% 0, 0 0; } 100% { background-position: -30% 0, 0 0; } }
@keyframes dream-breathe { 0%, 100% { opacity: 1; } 50% { opacity: .78; } }
@keyframes dream-spin { to { transform: rotate(360deg); } }
@keyframes dream-bounce { 0% { transform: scale(1); } 30% { transform: scale(1.35) rotate(-12deg); } 60% { transform: scale(.9) rotate(6deg); } 100% { transform: scale(1); } }
@keyframes dream-number { 0% { transform: translateY(-60%); opacity: 0; } 100% { transform: none; opacity: 1; } }
@keyframes dream-burst { 0% { opacity: 1; transform: translate(0, 0) scale(.4); } 100% { opacity: 0; transform: translate(var(--dx), var(--dy)) scale(1.1); } }

/* 系统开启“减少动态效果”时尊重用户设置。 */
@media (prefers-reduced-motion: reduce) {
  view, text, button, view::before, view::after, button::before { animation-duration: .01ms !important; animation-iteration-count: 1 !important; transition-duration: .01ms !important; }
}
</style>
