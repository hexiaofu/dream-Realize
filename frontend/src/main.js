/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { createSSRApp } from 'vue'
import App from './App.vue'
export function createApp() { return { app: createSSRApp(App) } }
