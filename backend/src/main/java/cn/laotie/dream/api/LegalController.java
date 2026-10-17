package cn.laotie.dream.api;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.service.RegistrationNoticeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 声明全文公开可读；版本和摘要与注册校验使用同一来源。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@RestController
public class LegalController {
    private final RegistrationNoticeService notice;
    public LegalController(RegistrationNoticeService notice) { this.notice=notice; }
    @GetMapping("/api/legal/registration-notice") public Object current() { return Api.ok(notice.current()); }
}
