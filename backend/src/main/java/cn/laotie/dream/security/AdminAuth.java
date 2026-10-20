package cn.laotie.dream.security;

import cn.laotie.dream.api.Requests;
import cn.laotie.dream.common.Api;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;

/**
 * 后台独立 HttpOnly 会话；写请求自定义头用于阻止跨站表单请求。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class AdminAuth {
    private static final String COOKIE="dream-admin-session";
    private final JdbcTemplate jdbc;
    private final Sessions sessions;
    private final boolean secure;
    private final String cookiePath;
    // 反向代理带路径前缀时（如 /dream）浏览器看到的是 /dream/api/admin，Cookie 路径需随之配置。
    public AdminAuth(JdbcTemplate jdbc,Sessions sessions,@Value("${app.admin-cookie-secure:false}") boolean secure,@Value("${app.admin-cookie-path:/api/admin}") String cookiePath) { this.jdbc=jdbc; this.sessions=sessions; this.secure=secure; this.cookiePath=cookiePath; }
    public void checkWrite(HttpServletRequest request) { Api.require("1".equals(request.getHeader("X-Admin-Request")),403,"无效的后台操作请求"); }
    private String token(HttpServletRequest request) {
        if(request.getCookies()!=null) for(Cookie cookie:request.getCookies()) if(COOKIE.equals(cookie.getName())) return cookie.getValue();
        return "";
    }
    private void cookie(HttpServletResponse response,String value,long maxAge) {
        response.addHeader("Set-Cookie",ResponseCookie.from(COOKIE,value).httpOnly(true).secure(secure).sameSite("Strict").path(cookiePath).maxAge(maxAge).build().toString());
    }
    @Transactional public Object login(Requests.Login input,HttpServletRequest request,HttpServletResponse response) {
        checkWrite(request);
        Api.require(input.password.getBytes(StandardCharsets.UTF_8).length<=72,401,"账号或密码不正确");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT m.id,m.password_hash FROM members m JOIN administrators a ON a.member_id=m.id WHERE m.username=?",input.username.toLowerCase(Locale.ROOT));
        String hash=rows.isEmpty()?"$2a$10$7EqJtq98hPqEX7fNZaFWoOhiJXnVyIuoU1QK1Gzp63z3FQhL4kVbG":String.valueOf(rows.get(0).get("password_hash"));
        boolean matches=new BCryptPasswordEncoder().matches(input.password,hash);
        Api.require(!rows.isEmpty() && matches,401,"管理员账号或密码不正确");
        long id=((Number)rows.get(0).get("id")).longValue();
        jdbc.update("DELETE FROM admin_sessions WHERE token_hash=? OR expires_at<=CURRENT_TIMESTAMP",sessions.hash(token(request)));
        String value=sessions.randomToken();
        jdbc.update("INSERT INTO admin_sessions(token_hash,member_id,expires_at) VALUES(?,?,?)",sessions.hash(value),id,new Timestamp(System.currentTimeMillis()+8L*60*60*1000));
        cookie(response,value,8*60*60); return me(id);
    }
    public long require(HttpServletRequest request) {
        if(!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) checkWrite(request);
        String value=token(request); Api.require(!value.isEmpty() && value.length()<200,401,"请登录管理后台");
        List<Long> ids=jdbc.query("SELECT s.member_id FROM admin_sessions s JOIN administrators a ON a.member_id=s.member_id WHERE s.token_hash=? AND s.expires_at>CURRENT_TIMESTAMP",(rs,n)->rs.getLong(1),sessions.hash(value));
        Api.require(!ids.isEmpty(),401,"后台登录已过期"); return ids.get(0);
    }
    public Object me(long id) { return jdbc.queryForObject("SELECT id,username,nickname FROM members WHERE id=?",(rs,n)->Api.map("id",rs.getLong(1),"username",rs.getString(2),"nickname",rs.getString(3)),id); }
    public void logout(HttpServletRequest request,HttpServletResponse response) {
        checkWrite(request); jdbc.update("DELETE FROM admin_sessions WHERE token_hash=?",sessions.hash(token(request))); cookie(response,"",0);
    }
}
