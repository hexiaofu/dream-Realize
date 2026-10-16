package cn.laotie.dream.security;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;

/**
 * 随机 Bearer 会话：数据库仅保存 SHA-256 摘要，支持退出失效。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class Sessions {
    private final JdbcTemplate jdbc;
    private final ModerationPolicy policy;
    private final SecureRandom random = new SecureRandom();
    public Sessions(JdbcTemplate jdbc,ModerationPolicy policy) { this.jdbc = jdbc; this.policy=policy; }
    public String randomToken() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public String hash(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format("%02x", b & 255));
            return out.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    public String create(long userId) {
        String token = randomToken();
        jdbc.update("INSERT INTO sessions(token_hash, member_id, expires_at) VALUES(?,?,?)", hash(token), userId,
            new Timestamp(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));
        return token;
    }
    public Long optional(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isEmpty()) return null;
        Api.require(auth.startsWith("Bearer ") && auth.length() < 200, 401, "请重新登录");
        List<Long> ids = jdbc.query("SELECT member_id FROM sessions WHERE token_hash=? AND expires_at>CURRENT_TIMESTAMP",
            (rs, row) -> rs.getLong(1), hash(auth.substring(7)));
        if (ids.isEmpty()) throw new ApiException(401, "登录已过期，请重新登录");
        policy.loginAllowed(ids.get(0),401);
        return ids.get(0);
    }
    public long require(HttpServletRequest request) {
        Long id = optional(request); Api.require(id != null, 401, "请先登录，再参与圆梦"); return id;
    }
    public void logout(HttpServletRequest request) {
        require(request);
        jdbc.update("DELETE FROM sessions WHERE token_hash=?", hash(request.getHeader("Authorization").substring(7)));
    }
}
