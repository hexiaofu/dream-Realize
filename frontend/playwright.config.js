/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
const { defineConfig } = require('@playwright/test')
module.exports = defineConfig({
  testDir: './tests', timeout: 45000, expect: { timeout: 10000 }, workers: 1,
  use: { baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:18173', viewport: { width: 390, height: 844 }, screenshot: 'only-on-failure',
    launchOptions: process.env.PW_EXECUTABLE_PATH ? { executablePath: process.env.PW_EXECUTABLE_PATH } : {} },
  reporter: 'list'
})
