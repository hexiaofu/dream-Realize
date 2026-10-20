package cn.laotie.dream.api;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.Sessions;
import cn.laotie.dream.service.DirectMessageService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 成员私信接口，全部需要登录。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@RestController
@RequestMapping("/api/direct/conversations")
public class DirectMessageController {
    private final DirectMessageService direct;
    private final Sessions sessions;
    public DirectMessageController(DirectMessageService direct,Sessions sessions) { this.direct=direct; this.sessions=sessions; }
    @PostMapping public Object start(HttpServletRequest req,@Valid @RequestBody Requests.DirectStart body) { return Api.ok(direct.start(sessions.require(req),body)); }
    @GetMapping public Object list(HttpServletRequest req) { return Api.ok(direct.list(sessions.require(req))); }
    @GetMapping("/{id}") public Object detail(HttpServletRequest req,@PathVariable long id) { return Api.ok(direct.detail(id,sessions.require(req))); }
    @GetMapping("/{id}/messages") public Object messages(HttpServletRequest req,@PathVariable long id,@RequestParam(defaultValue="0") long before) { return Api.ok(direct.messages(id,sessions.require(req),before)); }
    @PostMapping("/{id}/messages") public Object send(HttpServletRequest req,@PathVariable long id,@Valid @RequestBody Requests.Message body) { return Api.ok(direct.send(id,sessions.require(req),body)); }
}
