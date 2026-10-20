package cn.laotie.dream;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 部署在反向代理路径前缀之后时，后台会话 Cookie 路径随 app.admin-cookie-path 配置。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@SpringBootTest(properties="app.admin-cookie-path=/dream/api/admin")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AdminCookiePathIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private MemberService members;
    @Autowired private JdbcTemplate jdbc;
    @Test void loginCookieUsesConfiguredPath() throws Exception {
        long adminId=members.insertMember("path_admin","PathAdminTest123!","运营");
        jdbc.update("INSERT INTO administrators(member_id) VALUES(?)",adminId);
        String header=mvc.perform(post("/api/admin/login").header("X-Admin-Request","1").contentType("application/json")
            .content(json.writeValueAsBytes(Api.map("username","path_admin","password","PathAdminTest123!")))).andExpect(status().isOk()).andReturn().getResponse().getHeader("Set-Cookie");
        assertThat(header).contains("Path=/dream/api/admin","HttpOnly","SameSite=Strict");
    }
}
