package cn.laotie.dream.config;

import cn.laotie.dream.service.MemberService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 仅在未设置管理员时，使用部署环境提供的凭证创建首个管理员。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Component
@Order(1)
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final MemberService members;
    @Value("${app.admin-username:}") private String username;
    @Value("${app.admin-password:}") private String password;
    public AdminBootstrap(JdbcTemplate jdbc,MemberService members) { this.jdbc=jdbc; this.members=members; }
    @Override @Transactional public void run(ApplicationArguments args) {
        if(username.isEmpty() && password.isEmpty()) return;
        if(jdbc.queryForObject("SELECT COUNT(*) FROM administrators",Integer.class)>0) return;
        if(!username.matches("[a-zA-Z0-9_]{4,32}") || password.length()<12 || password.length()>64 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new IllegalStateException("管理员账号需要 4–32 位字母数字下划线；密码需要 12–64 位，UTF-8 不超过 72 字节");
        String normalized=username.toLowerCase(Locale.ROOT);
        if(jdbc.queryForObject("SELECT COUNT(*) FROM members WHERE username=?",Integer.class,normalized)>0)
            throw new IllegalStateException("管理员初始化账号已被普通用户占用，请使用独立账号");
        long id=members.insertMember(normalized,password,"管理员");
        jdbc.update("INSERT INTO administrators(member_id) VALUES(?)",id);
    }
}
