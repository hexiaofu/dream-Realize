/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
async function registrationConsent(request, api) {
  const response = await request.get(api + '/legal/registration-notice')
  if (!response.ok()) throw new Error('注册声明未能加载')
  const notice = (await response.json()).data
  return { noticeAccepted: true, noticeVersion: notice.version, noticeSha256: notice.sha256 }
}
async function passCommentExam(request, api, headers) {
  const exam = (await (await request.get(api + '/admission/questions')).json()).data
  const answers = Object.fromEntries([1, 2, 0, 1, 2, 0, 1, 2, 0, 1].map((value, i) => [i + 1, value]))
  const response = await request.post(api + '/admission/check', { headers, data: { answers, examVersion: exam.version } })
  if (!response.ok() || !(await response.json()).data.passed) throw new Error('评论资格问答未通过')
}
module.exports = { registrationConsent, passCommentExam }
