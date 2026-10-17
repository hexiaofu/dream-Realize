package cn.laotie.dream.api;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.Sessions;
import cn.laotie.dream.service.CommentMediaService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
@RestController
@RequestMapping("/api/media-favorites")
public class MediaFavoriteController {
    private final CommentMediaService media;
    private final Sessions sessions;
    public MediaFavoriteController(CommentMediaService media,Sessions sessions) { this.media=media; this.sessions=sessions; }
    @GetMapping public Object list(HttpServletRequest req,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="30") int size) {
        return Api.ok(media.favorites(sessions.require(req),page,size));
    }
    @PutMapping("/{id}") public Object collect(HttpServletRequest req,@PathVariable String id) { return Api.ok(media.collect(sessions.require(req),id)); }
    @DeleteMapping("/{id}") public Object remove(HttpServletRequest req,@PathVariable String id) { media.uncollect(sessions.require(req),id); return Api.ok(null); }
}
