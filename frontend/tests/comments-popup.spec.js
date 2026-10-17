/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
const { test, expect } = require('@playwright/test')
const path = require('node:path')
const button = (scope, text) => scope.locator('.wd-button').filter({ hasText: new RegExp('^' + text + '$') })
const commentButton = page => page.locator('.dream-reactions .wd-button').filter({ hasText: /^评论/ })
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=', 'base64')
const gif = Buffer.from('R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7', 'base64')

// 只模拟接口验证弹层交互，不向本地业务库创建账号、评论或答题记录。
async function setup(page, options = {}) {
  const state = { qualified: false, reaction: 'NONE', items: [], sendAttempts: 0, failSend: false, uploads: [], favorites: [], uploadAttempts: 0, failUpload: false, ...options }
  const qualification = () => ({ passed: state.qualified, score: state.qualified ? 90 : 0, passScore: 90 })
  const dream = () => ({ id: 41, title: '评论弹层测试', story: '一起认真交流。', category: 'LIFE', status: 'OPEN', helpType: 'COMPANY', rewardType: 'FREE', contactMode: 'BOTH', city: '西宁', helperCount: 0, authorName: state.anonymousOwner ? '匿名梦想家' : '梦想发布者', authorId: state.anonymousOwner ? undefined : 21, anonymous: !!state.anonymousOwner, isOwner: !!state.anonymousOwner, createdAt: '2026-10-16T12:00:00', media: [], updates: [], myReaction: state.reaction, supportCount: state.reaction === 'SUPPORT' ? 1 : 0, disagreeCount: state.reaction === 'DISAGREE' ? 1 : 0, commentCount: state.items.length })
  await page.route(url => url.pathname.startsWith('/api/'), async route => {
    const url = new URL(route.request().url()), endpoint = url.pathname, method = route.request().method()
    const ok = data => route.fulfill({ json: { code: 0, data } })
    if (endpoint === '/api/auth/login') return ok({ token: 'popup-test-token', user: { id: 99, nickname: '评论成员', restriction: state.restricted ? { mode: 'READ_ONLY' } : null, commentQualification: qualification() } })
    if (endpoint === '/api/dreams') return ok({ items: [dream()], total: 1, hasMore: false })
    if (endpoint === '/api/dreams/41') return ok(dream())
    if (endpoint === '/api/dreams/41/reaction') { state.reaction = route.request().postDataJSON().reaction; return ok(dream()) }
    if (endpoint === '/api/dreams/41/comments' && method === 'GET') {
      const current = Number(url.searchParams.get('page') || 1)
      return ok({ items: state.items.slice((current - 1) * 20, current * 20), total: state.items.length, hasMore: state.items.length > current * 20, commentQualification: qualification() })
    }
    if (endpoint === '/api/dreams/41/comments' && method === 'POST') {
      state.sendAttempts++
      if (state.failSend) { state.failSend = false; return route.fulfill({ status: 503, json: { code: 503, message: '评论发送失败' } }) }
      const body = route.request().postDataJSON()
      state.items.unshift({ id: 1000 + state.sendAttempts, content: body.content, media: (body.mediaIds || []).map(id => ({ ...state.uploads.find(file => file.id === id), favorite: state.favorites.some(file => file.id === id) })), anonymous: body.anonymous, authorName: body.anonymous ? '匿名梦想家' : '评论成员', authorId: body.anonymous ? undefined : 99, isMine: true, isAuthor: !!state.anonymousOwner, authorReaction: state.reaction, createdAt: '2026-10-16T13:00:00' })
      return ok({ commentCount: state.items.length })
    }
    if (endpoint.startsWith('/api/dreams/41/comments/') && method === 'DELETE') { state.items = state.items.filter(item => item.id !== Number(endpoint.split('/').pop())); return ok({ commentCount: state.items.length }) }
    if (endpoint === '/api/admission/status') return ok(qualification())
    if (endpoint === '/api/admission/questions') return ok({ version: 'popup-fixture', total: 10, pointsPerQuestion: 10, passScore: 90, questions: [] })
    if (endpoint === '/api/media' && method === 'POST') {
      state.uploadAttempts++
      if (state.failUpload) { state.failUpload = false; return route.fulfill({ status: 503, json: { code: 503, message: '上传失败，请重试' } }) }
      const animated = route.request().postDataBuffer().includes(Buffer.from('.gif'))
      const id = '00000000-0000-4000-8000-' + String(state.uploadAttempts).padStart(12, '0')
      const file = { id, type: 'IMAGE', url: '/api/media/' + id + (animated ? '.gif' : '.png'), contentType: animated ? 'image/gif' : 'image/png', size: 68, available: true }
      state.uploads.push(file); return ok(file)
    }
    if (endpoint.startsWith('/api/media/') && method === 'GET') return route.fulfill({ contentType: endpoint.endsWith('.gif') ? 'image/gif' : 'image/png', body: endpoint.endsWith('.gif') ? gif : png })
    if (endpoint === '/api/media-favorites') return ok({ items: state.favorites.map(file => ({ ...file, favorite: true })), total: state.favorites.length, hasMore: false })
    if (endpoint.startsWith('/api/media-favorites/')) {
      const id = endpoint.split('/').pop()
      if (method === 'PUT' && !state.favorites.some(file => file.id === id)) state.favorites.push(state.uploads.find(file => file.id === id))
      if (method === 'DELETE') state.favorites = state.favorites.filter(file => file.id !== id)
      state.items.forEach(item => item.media?.forEach(file => { if (file.id === id) file.favorite = method === 'PUT' }))
      return ok(null)
    }
    throw new Error('未模拟的请求：' + method + ' ' + endpoint)
  })
  return state
}
async function signIn(page) {
  await page.locator('.wd-input').filter({ hasText: '账号' }).locator('input').fill('popup_member')
  await page.locator('.wd-input').filter({ hasText: '密码' }).locator('input').fill('PopupTest123!')
  await button(page, '登录圆梦').click()
  await expect(page).not.toHaveURL(/auth\/index/)
}
async function settled(page) {
  await expect.poll(async () => {
    const box = await page.locator('.dream-comments-popup').boundingBox()
    return box && Math.abs(box.y + box.height - page.viewportSize().height) < 2
  }).toBe(true)
}

test('评论弹层支持登录和答题返回、先表态、草稿保留及发送重试', async ({ page }) => {
  const state = await setup(page), errors = []; page.on('pageerror', error => errors.push(error.message))
  await page.goto('/#/pages/discover/index')
  await commentButton(page).click()
  await expect(page).toHaveURL(/dream\/detail\?id=41&comments=1/)
  const popup = page.locator('.dream-comments-popup'), comments = page.locator('.dream-comments')
  await expect(popup).toBeVisible(); await settled(page)
  await expect(page.locator('.dream-action-bar')).toHaveCount(0)
  await button(comments, '登录后评论').click()
  await expect(page).toHaveURL(/auth\/index/)
  await expect(popup).not.toBeVisible()
  await signIn(page)
  await expect(popup).toBeVisible()
  await expect(comments.locator('textarea')).toBeDisabled()
  state.qualified = true
  await button(comments, '去答题').click()
  await expect(page).toHaveURL(/admission\/index/)
  await expect(page.getByText('已获得评论资格', { exact: true })).toBeVisible()
  await expect(popup).not.toBeVisible()
  await button(page, '返回评论').click()
  await expect(popup).toBeVisible()
  await expect(comments.locator('textarea')).toBeDisabled()
  await button(comments, '先表态').click()
  await expect(popup).not.toBeVisible()
  await page.locator('.dream-reactions .wd-button').filter({ hasText: /^精神支持/ }).click()
  await commentButton(page).click()
  const editor = comments.locator('textarea')
  await expect(editor).toBeEnabled()
  await editor.fill('保留草稿，认真交流')
  await popup.locator('.wd-popup__close').click()
  await expect(popup).not.toBeVisible()
  await expect(page.locator('.dream-action-bar')).toBeVisible()
  await commentButton(page).click()
  await expect(editor).toHaveValue('保留草稿，认真交流')
  state.failSend = true
  await button(comments, '发表评论').click()
  await expect.poll(() => state.sendAttempts).toBe(1)
  await expect(button(comments, '发表评论')).not.toHaveClass(/is-loading/)
  await expect(editor).toHaveValue('保留草稿，认真交流')
  await button(comments, '发表评论').click()
  await expect(comments.locator('.comment-card')).toContainText('保留草稿，认真交流')
  await expect(comments.locator('.comment-card')).toContainText('精神支持')
  await expect(editor).toHaveValue('')
  await expect(comments.locator('.subheading')).toHaveText('评论 · 1')
  await button(comments.locator('.comment-card'), '删除').click()
  await page.locator('.uni-modal__btn_primary').click()
  await expect(comments.locator('.subheading')).toHaveText('评论 · 0')
  await expect(comments.getByText('暂无评论', { exact: true })).toBeVisible()
  expect(errors).toEqual([])
})

test('评论弹层窄屏计数横排、输入高度固定、列表独立滚动和分页', async ({ page }) => {
  await setup(page, { qualified: true, reaction: 'SUPPORT', items: Array.from({ length: 24 }, (_, i) => ({ id: i + 1, content: '这是第 ' + (i + 1) + ' 条评论。' + '一起把梦想慢慢实现。'.repeat(3), authorName: '评论成员 ' + (i + 1), authorReaction: i % 2 ? 'DISAGREE' : 'SUPPORT', createdAt: '2026-10-16T13:00:00' })) })
  await page.goto('/#/pages/auth/index'); await signIn(page)
  await page.goto('/#/pages/dream/detail?id=41&comments=1')
  const comments = page.locator('.dream-comments'), editor = comments.locator('textarea'), count = comments.locator('.comment-word-count')
  await expect(editor).toBeEnabled()
  for (const viewport of [{ width: 320, height: 568 }, { width: 390, height: 844 }, { width: 1440, height: 900 }]) {
    await page.setViewportSize(viewport); await settled(page)
    for (const length of [0, 12, 1000]) {
      await editor.fill('梦'.repeat(length))
      await expect.poll(async () => (await count.innerText()).replace(/\s/g, '')).toBe(length + '/1000')
      const box = await count.boundingBox(), area = await editor.boundingBox(), panel = await comments.boundingBox()
      expect(box.height).toBeLessThanOrEqual(21)
      expect(box.x).toBeGreaterThanOrEqual(panel.x)
      expect(box.x + box.width).toBeLessThanOrEqual(panel.x + panel.width)
      expect(area.y + area.height).toBeLessThanOrEqual(box.y)
      expect(area.height).toBeLessThanOrEqual(65)
      await expect(button(comments, '发表评论')).toBeInViewport()
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
    }
    if (viewport.width === 390) await page.screenshot({ path: path.resolve(__dirname, '../../.runtime/comments-popup-1000.png'), animations: 'disabled' })
  }
  const before = await comments.locator('.comments-composer').boundingBox()
  await button(comments, '更多评论').click()
  await expect(comments.locator('.comment-card')).toHaveCount(24)
  expect((await comments.locator('.comments-composer').boundingBox()).y).toBe(before.y)
  await expect(comments.locator('.subheading')).toBeInViewport()
  await expect(button(comments, '发表评论')).toBeInViewport()
  await expect(button(comments, '更多评论')).toHaveCount(0)
})

test('匿名作者评论保持匿名，受限账号不能输入', async ({ page }) => {
  await setup(page, { qualified: true, reaction: 'DISAGREE', anonymousOwner: true, restricted: true })
  await page.goto('/#/pages/auth/index'); await signIn(page)
  await page.goto('/#/pages/dream/detail?id=41&comments=1')
  const comments = page.locator('.dream-comments')
  await expect(comments.locator('textarea')).toBeDisabled()
  await expect(comments.locator('.wd-switch')).toHaveClass(/is-checked/)
  await expect(comments.locator('.wd-switch')).toHaveClass(/is-disabled/)
  await expect(button(comments, '发表评论')).toHaveClass(/is-disabled/)
  await expect(comments.getByText('账号已限制发布', { exact: true })).toBeVisible()
})

test('评论表情、图片和 GIF 上传重试、收藏持久化及再次发送', async ({ page }) => {
  const state = await setup(page, { qualified: true, reaction: 'SUPPORT', failUpload: true }), errors = []
  page.on('pageerror', e => errors.push(e.message))
  await page.goto('/#/pages/auth/index'); await signIn(page)
  await page.goto('/#/pages/dream/detail?id=41&comments=1')
  const comments = page.locator('.dream-comments'), picker = page.locator('.comment-media-picker'), editor = comments.locator('textarea')
  await expect(editor).toBeEnabled()
  await button(comments, '表情').click()
  await picker.getByText('👍', { exact: true }).click()
  await expect(editor).toHaveValue('👍')
  await expect.poll(async () => (await comments.locator('.comment-word-count').innerText()).replace(/\s/g, '')).toBe('1/1000')
  await button(comments, '图片').click()
  const chooser = page.waitForEvent('filechooser')
  await picker.locator('.wd-upload__evoke').click()
  await (await chooser).setFiles({ name: '鼓励.gif', mimeType: 'image/gif', buffer: gif })
  await expect(button(picker, '重试上传')).toBeVisible()
  await button(picker, '完成').click()
  await expect(button(comments, '发表评论')).toHaveClass(/is-disabled/)
  await button(comments, '查看上传').click()
  await button(picker, '重试上传').click()
  await expect(picker.locator('.wd-upload__picture img')).toHaveAttribute('src', /\.gif$/)
  await expect(picker.locator('.wd-upload__mask')).toHaveCount(0)
  await button(picker, '收藏').click()
  await expect(button(picker, '已收藏')).toBeVisible()
  const imageChooser = page.waitForEvent('filechooser')
  await picker.locator('.wd-upload__evoke').click()
  await (await imageChooser).setFiles({ name: 'photo.png', mimeType: 'image/png', buffer: png })
  await expect(picker.locator('.wd-upload__picture')).toHaveCount(2)
  await expect(picker.locator('.wd-upload__mask')).toHaveCount(0)
  await button(picker, '完成').click()
  await expect(comments.locator('.comment-draft-media .wd-img')).toHaveCount(2)
  await page.locator('.dream-comments-popup .wd-popup__close').click()
  await commentButton(page).click()
  await expect(comments.locator('.comment-draft-media .wd-img')).toHaveCount(2)
  await button(comments, '发表评论').click()
  const card = comments.locator('.comment-card').first()
  await expect(card).toContainText('👍'); await expect(card).toContainText('GIF')
  await expect(card.locator('.comment-media-item')).toHaveCount(2)
  await expect(comments.locator('.comment-draft-media')).toHaveCount(0)
  await button(card, '收藏').click()
  await expect(card.locator('.wd-button').filter({ hasText: '已收藏' })).toHaveCount(2)
  await page.reload(); await expect(comments.locator('.comment-card')).toHaveCount(1)
  await button(comments.locator('.comments-toolbar'), '收藏').click()
  await expect(picker.locator('.favorite-item')).toHaveCount(2)
  await button(picker, '使用 GIF').click()
  await expect(editor).toHaveValue('')
  await expect(button(comments, '发表评论')).not.toHaveClass(/is-disabled/)
  await button(comments, '发表评论').click()
  await expect(comments.locator('.comment-card')).toHaveCount(2)
  expect(state.items[0].content).toBe(''); expect(state.items[0].media[0].contentType).toBe('image/gif')
  await button(comments.locator('.comments-toolbar'), '收藏').click()
  await button(picker, '移除').first().click()
  await expect(picker.locator('.favorite-item')).toHaveCount(1)
  await button(picker, '完成').click()
  await expect(picker).not.toBeVisible()
  await expect(comments.locator('.comment-card').first().locator('.wd-img')).toBeVisible()
  await page.screenshot({ path: path.resolve(__dirname, '../../.runtime/comment-media.png'), animations: 'disabled' })
  expect(errors).toEqual([])
})

test('窄屏附件和收藏布局不溢出，表情不突破千字限制', async ({ page }) => {
  const state = await setup(page, { qualified: true, reaction: 'DISAGREE' })
  state.uploads = Array.from({ length: 9 }, (_, i) => ({ id: 'fixture-' + i, type: 'IMAGE', url: '/api/media/fixture-' + i + '.gif', contentType: 'image/gif', available: i !== 8 }))
  state.favorites = state.uploads
  state.items = [{ id: 1, content: '一起加油！', media: state.uploads, authorName: '评论成员', authorReaction: 'SUPPORT', createdAt: '2026-10-16T13:00:00' }]
  await page.setViewportSize({ width: 320, height: 568 })
  await page.goto('/#/pages/auth/index'); await signIn(page)
  await page.goto('/#/pages/dream/detail?id=41&comments=1')
  const comments = page.locator('.dream-comments'), picker = page.locator('.comment-media-picker'), editor = comments.locator('textarea')
  await expect(editor).toBeEnabled(); await editor.fill('字'.repeat(1000))
  await button(comments, '表情').click(); await picker.getByText('😀', { exact: true }).click()
  await expect(editor).toHaveValue('字'.repeat(1000))
  await button(comments.locator('.comments-toolbar'), '收藏').click()
  await expect(picker.getByText('图片已不可用', { exact: true })).toBeVisible()
  await expect(button(picker.locator('.favorite-item').last(), '使用 GIF')).toHaveClass(/is-disabled/)
  await button(picker, '使用 GIF').first().click()
  await expect(comments.locator('.comment-draft-media')).toBeVisible()
  await expect(button(comments, '发表评论')).toBeInViewport()
  expect((await comments.locator('.comment-word-count').boundingBox()).height).toBeLessThanOrEqual(21)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
})
