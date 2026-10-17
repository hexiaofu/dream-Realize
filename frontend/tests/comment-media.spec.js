/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
const { test, expect } = require('@playwright/test')
const apiBase = process.env.COMMENT_MEDIA_TEST_URL
const button = (scope, text) => scope.locator('.wd-button').filter({ hasText: new RegExp('^' + text + '$') })
// 指向独立测试服务，真实上传与持久化；不会向默认业务库写入测试内容。
test('真实 MySQL 评论附件、GIF 收藏与刷新后再次发送', async ({ page, request }) => {
  test.skip(!apiBase, '设置 COMMENT_MEDIA_TEST_URL 指向独立测试服务')
  const call = async (path, method = 'GET', data, token) => {
    const response = await request.fetch(apiBase + path, { method, data, headers: token ? { Authorization: 'Bearer ' + token } : {} })
    expect(response.ok(), await response.text()).toBeTruthy()
    const body = await response.json(); expect(body.code).toBe(0); return body.data
  }
  const username = 'media_ui_' + Date.now(), password = 'MediaUiTest123!'
  const notice = await call('/legal/registration-notice')
  const member = await call('/auth/register', 'POST', { username, password, nickname: '图片评论测试', noticeAccepted: true, noticeVersion: notice.version, noticeSha256: notice.sha256 })
  const questions = await call('/admission/questions')
  await call('/admission/check', 'POST', { examVersion: questions.version, answers: Object.fromEntries([1, 2, 0, 1, 2, 0, 1, 2, 0, 1].map((answer, i) => [String(i + 1), answer])) }, member.token)
  const dream = await call('/dreams', 'POST', { title: '图片评论与表情包验证', category: 'LIFE', helpType: 'COMPANY', anonymous: true }, member.token)
  await call('/dreams/' + dream.id + '/reaction', 'PUT', { reaction: 'SUPPORT' }, member.token)
  // 前端原样访问真实测试接口，仅改目的地址；不替换响应内容。
  await page.route(url => url.pathname.startsWith('/api/'), async route => {
    const url = new URL(route.request().url())
    const response = await route.fetch({ url: apiBase + url.pathname.slice(4) + url.search })
    await route.fulfill({ response })
  })
  await page.goto('/#/pages/auth/index')
  await page.locator('.wd-input').filter({ hasText: '账号' }).locator('input').fill(username)
  await page.locator('.wd-input').filter({ hasText: '密码' }).locator('input').fill(password)
  await button(page, '登录圆梦').click()
  await expect(page).not.toHaveURL(/auth\/index/)
  await page.goto('/#/pages/dream/detail?id=' + dream.id + '&comments=1')
  const comments = page.locator('.dream-comments'), picker = page.locator('.comment-media-picker')
  await expect(comments.locator('textarea')).toBeEnabled()
  await button(comments, '表情').click(); await picker.getByText('🎉', { exact: true }).click()
  await button(comments, '图片').click()
  const chooser = page.waitForEvent('filechooser')
  await picker.locator('.wd-upload__evoke').click()
  await (await chooser).setFiles([
    { name: 'encourage.gif', mimeType: 'image/gif', buffer: Buffer.from('R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7', 'base64') },
    { name: 'photo.png', mimeType: 'image/png', buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=', 'base64') }
  ])
  await expect(picker.locator('.wd-upload__picture')).toHaveCount(2)
  await expect(picker.locator('.wd-upload__mask')).toHaveCount(0)
  await button(picker, '完成').click()
  await button(comments, '发表评论').click()
  const card = comments.locator('.comment-card').first()
  await expect(card).toContainText('🎉'); await expect(card).toContainText('GIF')
  await expect(card).toContainText('匿名梦想家')
  await button(card, '收藏').first().click()
  await expect(button(card, '已收藏')).toBeVisible()
  await page.reload()
  await button(comments.locator('.comments-toolbar'), '收藏').click()
  await expect(picker.locator('.favorite-item')).toHaveCount(1)
  await button(picker, '使用 GIF').click(); await button(comments, '发表评论').click()
  await expect(comments.locator('.comment-card')).toHaveCount(2)
  const saved = await call('/dreams/' + dream.id + '/comments')
  expect(saved.items[0].content).toBe(''); expect(saved.items[0].media[0].contentType).toBe('image/gif')
  expect(saved.items[0].authorId).toBeUndefined()
  expect((await call('/media-favorites', 'GET', undefined, member.token)).total).toBe(1)
  // 非收藏附件不可冒用，请求失败应完整回滚，不留下空白评论。
  const rejected = await request.post(apiBase + '/dreams/' + dream.id + '/comments', { headers: { Authorization: 'Bearer ' + member.token }, data: { content: '', mediaIds: [saved.items[1].media[1].id] } })
  expect(rejected.status()).toBe(403)
  expect((await call('/dreams/' + dream.id + '/comments')).total).toBe(2)
})
