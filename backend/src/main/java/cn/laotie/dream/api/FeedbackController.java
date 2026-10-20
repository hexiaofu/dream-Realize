package cn.laotie.dream.api;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.Sessions;
import cn.laotie.dream.service.FeedbackService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 成员意见反馈接口，需要登录；限制发布的账号同样可以反馈。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final FeedbackService feedback;
    private final Sessions sessions;
    public FeedbackController(FeedbackService feedback,Sessions sessions) { this.feedback=feedback; this.sessions=sessions; }
    @PostMapping public Object submit(HttpServletRequest req,@Valid @RequestBody Requests.Feedback body) { return Api.ok(feedback.submit(sessions.require(req),body)); }
    @GetMapping("/mine") public Object mine(HttpServletRequest req) { return Api.ok(feedback.mine(sessions.require(req))); }
}
