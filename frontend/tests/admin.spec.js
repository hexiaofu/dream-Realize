/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
const { test, expect } = require('@playwright/test')
const fs = require('fs')
const path = require('path')
const api = process.env.API_TEST_URL || 'http://127.0.0.1:18080/api'
const adminUrl = process.env.ADMIN_TEST_URL || 'http://127.0.0.1:18174'
const accessFile = path.resolve(__dirname, '../../.runtime/admin-access.json')
const local = fs.existsSync(accessFile) ? JSON.parse(fs.readFileSync(accessFile, 'utf8')) : {}
const username = process.env.ADMIN_TEST_USERNAME || local.username
const password = process.env.ADMIN_TEST_PASSWORD || local.password

test('后台：下架恢复、图片复核、限发封禁、解除限制与操作日志', async ({ page, request }) => {
  test.skip(!username || !password, '需要配置独立测试后台账号')
  test.setTimeout(90000)
  await page.setViewportSize({ width: 1440, height: 1000 })
  const errors = []; page.on('pageerror', error => errors.push(error.message))
  const loginName = 'modui' + Date.now().toString(36), memberPassword = 'ModerationTest123!'
  const answers = Object.fromEntries([1, 2, 0, 1, 2, 0, 1, 2].map((value, i) => [i + 1, value]))
  const exam = await (await request.post(api + '/admission/check', { data: { answers } })).json()
  const registered = await (await request.post(api + '/auth/register', { data: { username: loginName, nickname: '后台测试成员', password: memberPassword, admissionToken: exam.data.admissionToken } })).json()
  const member = registered.data, headers = { Authorization: 'Bearer ' + member.token }
  expect((await request.get(api + '/admin/members', { headers })).status()).toBe(401)
  await page.goto(adminUrl)
  await page.locator('.el-form-item').filter({ hasText: '管理员账号' }).locator('input').fill(username)
  await page.locator('.el-form-item').filter({ hasText: /^密码/ }).locator('input').fill(password)
  await page.getByRole('button', { name: '登录后台', exact: true }).click()
  await expect(page.getByRole('menuitem', { name: '内容管理' })).toBeVisible()
  const image = await page.evaluate(() => {
    const canvas = document.createElement('canvas'); canvas.width = 120; canvas.height = 80
    const ctx = canvas.getContext('2d'); ctx.fillStyle = '#238478'; ctx.fillRect(0, 0, 120, 80)
    return canvas.toDataURL('image/png').split(',')[1]
  })
  const uploaded = await (await request.post(api + '/media', { headers, multipart: { kind: 'IMAGE', file: { name: 'moderation.png', mimeType: 'image/png', buffer: Buffer.from(image, 'base64') } } })).json()
  const title = '后台验证梦想 ' + Date.now()
  const dream = await (await request.post(api + '/dreams', { headers, data: { title, story: '用于验证内容下架与恢复。', category: 'CREATION', helpType: 'SKILL', anonymous: true, mediaIds: [uploaded.data.id] } })).json()
  const id = dream.data.id
  fs.mkdirSync(path.resolve(__dirname, '../../.runtime'), { recursive: true })
  fs.appendFileSync(path.resolve(__dirname, '../../.runtime/admin-e2e-fixtures.jsonl'), JSON.stringify({ memberId: member.user.id, username: loginName, dreamId: id }) + '\n')
  const keyword = page.locator('.filters .el-form-item').filter({ hasText: '关键词' }).locator('input')
  await keyword.fill(title); await page.getByRole('button', { name: '查询', exact: true }).click()
  let row = page.locator('.el-table__body-wrapper tr').filter({ hasText: title }).first()
  await expect(row).toBeVisible()
  await row.getByRole('button', { name: '查看', exact: true }).click()
  const drawer = page.getByRole('dialog', { name: '内容详情' })
  await expect(drawer.getByText(loginName, { exact: false })).toBeVisible()
  await expect.poll(() => drawer.locator('.el-image img').evaluate(img => img.naturalWidth)).toBeGreaterThan(0)
  await drawer.getByRole('button', { name: '下架内容', exact: true }).click()
  async function confirm(reason) {
    const dialog = page.locator('.el-dialog').filter({ visible: true })
    await dialog.locator('textarea').fill(reason)
    await dialog.getByRole('button', { name: '确认处理', exact: true }).click()
    await expect(dialog).not.toBeVisible()
  }
  await confirm('E2E：核查违规内容')
  expect((await request.get(api + '/dreams/' + id)).status()).toBe(404)
  expect((await request.get(api.replace(/\/api$/, '') + uploaded.data.url)).status()).toBe(404)
  // 下架后，后台仍然使用管理员 cookie 读取原始附件。
  const protectedUrl = adminUrl + uploaded.data.url.replace('/api/media/', '/api/admin/media/')
  expect((await page.request.get(protectedUrl)).ok()).toBeTruthy()
  expect((await request.get(protectedUrl)).status()).toBe(401)
  await drawer.getByRole('button', { name: '恢复内容', exact: true }).click()
  await confirm('E2E：复核通过恢复')
  expect((await request.get(api + '/dreams/' + id)).ok()).toBeTruthy()
  await drawer.getByRole('button', { name: '管理该账号', exact: true }).click()
  row = page.locator('.el-table__body-wrapper tr').filter({ hasText: loginName }).first()
  await row.getByRole('button', { name: '设置限制', exact: true }).click()
  await confirm('E2E：限制发布七天')
  await expect(row.getByText('限制发布', { exact: true })).toBeVisible()
  expect((await request.post(api + '/dreams', { headers, data: { title: '受限发布', category: 'OTHER', helpType: 'SKILL' } })).status()).toBe(403)
  await row.getByRole('button', { name: '设置限制', exact: true }).click()
  await page.locator('.el-dialog').filter({ visible: true }).locator('.el-radio').filter({ hasText: '封禁登录' }).click()
  await expect(page.getByRole('radio', { name: '封禁登录' })).toBeChecked()
  await confirm('E2E：封禁登录七天')
  expect((await request.get(api + '/me', { headers })).status()).toBe(401)
  expect((await request.post(api + '/auth/login', { data: { username: loginName, password: memberPassword } })).status()).toBe(403)
  await row.getByRole('button', { name: '解除', exact: true }).click()
  await confirm('E2E：解除账号限制')
  expect((await request.post(api + '/auth/login', { data: { username: loginName, password: memberPassword } })).ok()).toBeTruthy()
  await page.getByRole('menuitem', { name: '操作日志' }).click()
  await expect(page.locator('.el-table__body-wrapper tr').filter({ hasText: '账号 #' + member.user.id }).filter({ hasText: 'E2E：解除账号限制' })).toBeVisible()
  await expect(page.locator('.el-table__body-wrapper tr').filter({ hasText: '梦想 #' + id }).filter({ hasText: 'E2E：核查违规内容' })).toBeVisible()
  await page.reload()
  await expect(page.getByRole('menuitem', { name: '内容管理' })).toBeVisible()
  expect(errors).toEqual([])
  await page.getByRole('button', { name: '退出登录', exact: true }).click()
  await expect(page.getByRole('button', { name: '登录后台', exact: true })).toBeVisible()
  expect((await page.request.get(adminUrl + '/api/admin/me')).status()).toBe(401)
})
