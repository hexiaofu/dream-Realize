package cn.laotie.dream.api;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.Sessions;
import cn.laotie.dream.service.MediaService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 登录上传、随机地址访问；响应不包含上传者身份和原始文件名。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@RestController
@RequestMapping("/api/media")
public class MediaController {
    private final MediaService media;
    private final Sessions sessions;
    public MediaController(MediaService media,Sessions sessions) { this.media=media; this.sessions=sessions; }
    @PostMapping(consumes="multipart/form-data") public Object upload(@RequestParam("file") MultipartFile file,
            @RequestParam("kind") String kind,HttpServletRequest request) throws IOException {
        return Api.ok(media.upload(sessions.require(request),kind,file));
    }
    @GetMapping("/{filename:.+}") public ResponseEntity<Resource> read(@PathVariable String filename) { return media.read(filename); }
    @DeleteMapping("/{id}") public Object remove(@PathVariable String id,HttpServletRequest request) throws IOException {
        media.remove(id,sessions.require(request)); return Api.ok(null);
    }
}
