package cn.laotie.dream.api;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.Sessions;
import cn.laotie.dream.service.*;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * REST API，公开浏览与需要登录的写操作显式分离。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@RestController
@RequestMapping("/api")
public class ApiController {
    private final AdmissionService admission;
    private final MemberService members;
    private final DreamService dreams;
    private final HelpService helps;
    private final Sessions sessions;
    private final InteractionService interactions;
    public ApiController(AdmissionService admission,MemberService members,DreamService dreams,HelpService helps,Sessions sessions,InteractionService interactions) {
        this.admission=admission; this.members=members; this.dreams=dreams; this.helps=helps; this.sessions=sessions; this.interactions=interactions;
    }
    @GetMapping("/health") public Object health() { return Api.ok(Api.map("status","UP")); }
    @GetMapping("/admission/questions") public Object questions() { return Api.ok(admission.questions()); }
    @GetMapping("/admission/status") public Object admissionStatus(HttpServletRequest req) { return Api.ok(admission.status(sessions.require(req))); }
    @PostMapping("/admission/check") public Object check(HttpServletRequest req,@Valid @RequestBody Requests.Exam body) { return Api.ok(admission.check(sessions.require(req),body.examVersion,body.answers)); }
    @PostMapping("/auth/register") public Object register(@Valid @RequestBody Requests.Register body) { return Api.ok(members.register(body)); }
    @PostMapping("/auth/login") public Object login(@Valid @RequestBody Requests.Login body) { return Api.ok(members.login(body)); }
    @PostMapping("/auth/logout") public Object logout(HttpServletRequest req) { sessions.logout(req); return Api.ok(true); }
    @GetMapping("/me") public Object me(HttpServletRequest req) { return Api.ok(members.me(sessions.require(req))); }
    @PutMapping("/me") public Object profile(HttpServletRequest req,@Valid @RequestBody Requests.Profile body) { return Api.ok(members.update(sessions.require(req),body)); }
    @GetMapping("/members/{id}") public Object member(HttpServletRequest req,@PathVariable long id) { return Api.ok(dreams.publicProfile(id,sessions.optional(req))); }
    @GetMapping("/stats") public Object stats() { return Api.ok(dreams.stats()); }
    @GetMapping("/dreams") public Object list(HttpServletRequest req,@RequestParam(defaultValue="") String keyword,@RequestParam(defaultValue="") String category,
        @RequestParam(defaultValue="") String helpType,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size,@RequestParam(defaultValue="false") boolean mine) {
        return Api.ok(dreams.list(keyword,category,helpType,status,page,size,sessions.optional(req),mine));
    }
    @GetMapping("/dreams/{id}") public Object detail(HttpServletRequest req,@PathVariable long id) { return Api.ok(dreams.detail(id,sessions.optional(req))); }
    @PostMapping("/dreams") public Object create(HttpServletRequest req,@Valid @RequestBody Requests.Dream body) { return Api.ok(dreams.create(sessions.require(req),body)); }
    @PostMapping("/dreams/{id}/updates") public Object progress(HttpServletRequest req,@PathVariable long id,@Valid @RequestBody Requests.Progress body) { return Api.ok(dreams.progress(id,sessions.require(req),body)); }
    @PostMapping("/dreams/{id}/helps") public Object help(HttpServletRequest req,@PathVariable long id,@Valid @RequestBody Requests.Help body) { return Api.ok(helps.apply(id,sessions.require(req),body)); }
    @PutMapping("/dreams/{id}/reaction") public Object react(HttpServletRequest req,@PathVariable long id,@Valid @RequestBody Requests.Reaction body) { return Api.ok(interactions.react(id,sessions.require(req),body)); }
    @GetMapping("/dreams/{id}/comments") public Object comments(HttpServletRequest req,@PathVariable long id,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) { return Api.ok(interactions.comments(id,sessions.optional(req),page,size)); }
    @PostMapping("/dreams/{id}/comments") public Object comment(HttpServletRequest req,@PathVariable long id,@Valid @RequestBody Requests.Comment body) { return Api.ok(interactions.comment(id,sessions.require(req),body)); }
    @DeleteMapping("/dreams/{id}/comments/{commentId}") public Object removeComment(HttpServletRequest req,@PathVariable long id,@PathVariable long commentId) { return Api.ok(interactions.removeComment(id,commentId,sessions.require(req))); }
    @GetMapping("/helps") public Object helps(HttpServletRequest req) { return Api.ok(helps.list(sessions.require(req))); }
    @GetMapping("/helps/{id}") public Object helpDetail(HttpServletRequest req,@PathVariable long id) { return Api.ok(helps.detail(id,sessions.require(req))); }
    @PatchMapping("/helps/{id}") public Object decide(HttpServletRequest req,@PathVariable long id,@Valid @RequestBody Requests.Decision body) { return Api.ok(helps.decision(id,sessions.require(req),body)); }
    @GetMapping("/helps/{id}/messages") public Object messages(HttpServletRequest req,@PathVariable long id,@RequestParam(defaultValue="0") long before) { return Api.ok(helps.messages(id,sessions.require(req),before)); }
    @PostMapping("/helps/{id}/messages") public Object send(HttpServletRequest req,@PathVariable long id,@Valid @RequestBody Requests.Message body) { return Api.ok(helps.send(id,sessions.require(req),body)); }
}
