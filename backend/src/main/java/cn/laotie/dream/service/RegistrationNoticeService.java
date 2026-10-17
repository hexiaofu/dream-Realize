package cn.laotie.dream.service;

import cn.laotie.dream.api.Requests;
import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.Sessions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;

/**
 * 注册声明的统一文本、版本核验及同意留痕；不收集额外设备或网络信息。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class RegistrationNoticeService {
    private final JdbcTemplate jdbc;
    private final ObjectNode document;
    private final String snapshot,version,sha256;

    public RegistrationNoticeService(JdbcTemplate jdbc,ObjectMapper json,Sessions sessions) throws IOException {
        this.jdbc=jdbc;
        try(InputStream input=new ClassPathResource("legal/registration-notice-v2026-10-16.json").getInputStream()) {
            document=(ObjectNode)json.readTree(input);
        }
        // 将完整文本序列化后摘要，避免源码换行符差异改变同一份声明。
        snapshot=json.writeValueAsString(document); version=document.path("version").asText(); sha256=sessions.hash(snapshot);
        document.put("sha256",sha256);
    }
    public ObjectNode current() { return document.deepCopy(); }
    public void validate(Requests.Register input) {
        Api.require(Boolean.TRUE.equals(input.noticeAccepted),400,"请阅读并同意免责声明与线下交流风险告知");
        Api.require(version.equals(input.noticeVersion) && sha256.equals(input.noticeSha256),412,"声明已更新，请重新阅读并同意");
    }
    // 由注册事务调用；成员、同意记录及会话一起提交或回滚。
    public void record(long memberId) {
        jdbc.update("INSERT INTO registration_consents(member_id,notice_version,notice_sha256,notice_snapshot) VALUES(?,?,?,?)",memberId,version,sha256,snapshot);
    }
}
