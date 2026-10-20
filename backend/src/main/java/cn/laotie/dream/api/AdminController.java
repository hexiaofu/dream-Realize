package cn.laotie.dream.api;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.AdminAuth;
import cn.laotie.dream.service.AdminService;
import cn.laotie.dream.service.FeedbackService;
import cn.laotie.dream.service.MediaService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.*;
import javax.validation.Valid;

/**
 * 所有后台查询和变更均要求独立管理员会话。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminAuth auth;
    private final AdminService admin;
    private final MediaService media;
    private final FeedbackService feedback;
    public AdminController(AdminAuth auth,AdminService admin,MediaService media,FeedbackService feedback) { this.auth=auth; this.admin=admin; this.media=media; this.feedback=feedback; }
    @ModelAttribute public void noCache(HttpServletResponse response) { response.setHeader("Cache-Control","no-store"); }
    @PostMapping("/login") public Object login(@Valid @RequestBody Requests.Login body,HttpServletRequest req,HttpServletResponse res) { return Api.ok(auth.login(body,req,res)); }
    @PostMapping("/logout") public Object logout(HttpServletRequest req,HttpServletResponse res) { auth.logout(req,res); return Api.ok(true); }
    @GetMapping("/me") public Object me(HttpServletRequest req) { return Api.ok(auth.me(auth.require(req))); }
    @GetMapping("/overview") public Object overview(HttpServletRequest req) { auth.require(req); return Api.ok(admin.overview()); }
    @GetMapping("/contents") public Object contents(HttpServletRequest req,@RequestParam(defaultValue="DREAM") String type,@RequestParam(defaultValue="") String keyword,@RequestParam(defaultValue="ALL") String state,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        auth.require(req); return Api.ok(admin.contents(type,keyword,state,page,size));
    }
    @GetMapping("/contents/{type}/{id}") public Object content(HttpServletRequest req,@PathVariable String type,@PathVariable long id) { auth.require(req); return Api.ok(admin.content(type,id)); }
    @PatchMapping("/contents/{type}/{id}") public Object moderate(HttpServletRequest req,@PathVariable String type,@PathVariable long id,@Valid @RequestBody AdminRequests.Content body) { return Api.ok(admin.moderate(type,id,auth.require(req),body)); }
    @GetMapping("/members") public Object members(HttpServletRequest req,@RequestParam(defaultValue="") String keyword,@RequestParam(defaultValue="ALL") String state,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        auth.require(req); return Api.ok(admin.members(keyword,state,page,size));
    }
    @PatchMapping("/members/{id}/restriction") public Object restrict(HttpServletRequest req,@PathVariable long id,@Valid @RequestBody AdminRequests.Restriction body) { return Api.ok(admin.restrict(id,auth.require(req),body)); }
    @GetMapping("/feedback") public Object feedback(HttpServletRequest req,@RequestParam(defaultValue="ALL") String state,@RequestParam(defaultValue="ALL") String category,@RequestParam(defaultValue="") String keyword,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        auth.require(req); return Api.ok(feedback.adminList(state,category,keyword,page,size));
    }
    @PatchMapping("/feedback/{id}") public Object handleFeedback(HttpServletRequest req,@PathVariable long id,@Valid @RequestBody AdminRequests.Feedback body) { return Api.ok(feedback.handle(id,auth.require(req),body)); }
    @GetMapping("/logs") public Object logs(HttpServletRequest req,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) { auth.require(req); return Api.ok(admin.logs(page,size)); }
    @GetMapping("/media/{filename:.+}") public ResponseEntity<Resource> media(HttpServletRequest req,@PathVariable String filename) { auth.require(req); return media.readForAdmin(filename); }
}
