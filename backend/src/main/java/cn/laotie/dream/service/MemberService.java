package cn.laotie.dream.service;

import cn.laotie.dream.api.Requests;
import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.Sessions;
import cn.laotie.dream.security.ModerationPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * 注册、登录和个人名片；密码使用 BCrypt 哈希。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class MemberService {
    private final JdbcTemplate jdbc;
    private final Sessions sessions;
    private final ModerationPolicy policy;
    private final AvatarService avatars;
    private final RegistrationNoticeService notice;
    private final AdmissionService admission;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public MemberService(JdbcTemplate jdbc, Sessions sessions,ModerationPolicy policy,AvatarService avatars,RegistrationNoticeService notice,AdmissionService admission) { this.jdbc=jdbc; this.sessions=sessions; this.policy=policy; this.avatars=avatars; this.notice=notice; this.admission=admission; }
    @Transactional public Object register(Requests.Register input) {
        notice.validate(input);
        Api.require(input.password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72,400,"密码 UTF-8 编码长度最多 72 字节");
        Api.require(jdbc.queryForObject("SELECT COUNT(*) FROM members WHERE username=?",Integer.class,input.username.toLowerCase(Locale.ROOT))==0,409,"账号已被使用");
        long id=insertMember(input.username.toLowerCase(Locale.ROOT),input.password,input.nickname.trim());
        notice.record(id);
        return Api.map("token",sessions.create(id),"user",me(id));
    }
    public long insertMember(String username,String password,String nickname) {
        GeneratedKeyHolder keys=new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps=connection.prepareStatement("INSERT INTO members(username,password_hash,nickname) VALUES(?,?,?)",new String[]{"id"});
            ps.setString(1,username); ps.setString(2,encoder.encode(password)); ps.setString(3,nickname); return ps;
        },keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }
    @Transactional public Object login(Requests.Login input) {
        Api.require(input.password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72,401,"账号或密码不正确");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,password_hash FROM members WHERE username=?",input.username.toLowerCase(Locale.ROOT));
        String hash=rows.isEmpty()?"$2a$10$7EqJtq98hPqEX7fNZaFWoOhiJXnVyIuoU1QK1Gzp63z3FQhL4kVbG":(String)rows.get(0).get("password_hash");
        boolean matched=encoder.matches(input.password,hash);
        Api.require(!rows.isEmpty() && matched,401,"账号或密码不正确");
        long id=((Number)rows.get(0).get("id")).longValue();
        jdbc.queryForObject("SELECT id FROM members WHERE id=? FOR UPDATE",Long.class,id);
        policy.loginAllowed(id,403);
        return Api.map("token",sessions.create(id),"user",me(id));
    }
    public Map<String,Object> me(long id) {
        Map<String,Object> member=jdbc.queryForObject("SELECT m.*,a.media_id,f.file_name FROM members m LEFT JOIN member_avatars a ON a.member_id=m.id LEFT JOIN media_files f ON f.id=a.media_id WHERE m.id=?",(rs,row)->Api.map(
            "id",rs.getLong("id"),"username",rs.getString("username"),"nickname",rs.getString("nickname"),"bio",rs.getString("bio"),"skills",rs.getString("skills"),"city",rs.getString("city"),
            "avatarId",clean(rs.getString("media_id")),"avatarUrl",AvatarService.url(rs.getString("file_name"))),id);
        member.put("restriction",policy.restriction(id)); member.put("commentQualification",admission.status(id)); return member;
    }
    @Transactional public Object update(long id,Requests.Profile input) {
        policy.writeAllowed(id);
        avatars.update(id,input.avatarId);
        jdbc.update("UPDATE members SET nickname=?,bio=?,skills=?,city=? WHERE id=?",input.nickname.trim(),clean(input.bio),clean(input.skills),clean(input.city),id);
        return me(id);
    }
    public static String clean(String value) { return value==null?"":value.trim(); }
}
