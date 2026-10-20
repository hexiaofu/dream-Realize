package cn.laotie.dream;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.Sessions;
import cn.laotie.dream.service.MemberService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 意见反馈：提交校验、频率限制、本人进度、后台筛选处理与操作日志。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class FeedbackIntegrationTest {
    private static final String PASSWORD="FeedbackTest123!";
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private MemberService members;
    @Autowired private Sessions sessions;
    @Autowired private JdbcTemplate jdbc;
    private long adminId,memberId;
    private String member,other;
    private Cookie cookie;
    private JsonNode call(MockHttpServletRequestBuilder request,Object body,int expected) throws Exception {
        if(body!=null) request.contentType("application/json").content(json.writeValueAsBytes(body));
        // 按 UTF-8 解码，才能断言中文内容与回复。
        return json.readTree(mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }
    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder) { return builder.cookie(cookie).header("X-Admin-Request","1"); }
    private JsonNode submit(String token,String content,int expected) throws Exception {
        return call(post("/api/feedback").header("Authorization",token),Api.map("category","SUGGESTION","content",content,"contact","  wx: dreamer  "),expected);
    }
    @BeforeEach void accounts() throws Exception {
        adminId=members.insertMember("fb_admin",PASSWORD,"运营"); jdbc.update("INSERT INTO administrators(member_id) VALUES(?)",adminId);
        memberId=members.insertMember("fb_member",PASSWORD,"反馈者"); member="Bearer "+sessions.create(memberId);
        other="Bearer "+sessions.create(members.insertMember("fb_other",PASSWORD,"旁观者"));
        String header=mvc.perform(post("/api/admin/login").header("X-Admin-Request","1").contentType("application/json")
            .content(json.writeValueAsBytes(Api.map("username","fb_admin","password",PASSWORD)))).andExpect(status().isOk()).andReturn().getResponse().getHeader("Set-Cookie");
        cookie=new Cookie("dream-admin-session",Objects.requireNonNull(header).split(";",2)[0].split("=",2)[1]);
    }

    @Test void submissionValidatesInputAndIsVisibleOnlyToItsAuthor() throws Exception {
        call(post("/api/feedback"),Api.map("category","BUG","content","无法登录"),401);
        call(post("/api/feedback").header("Authorization",member),Api.map("category","WRONG","content","内容"),400);
        call(post("/api/feedback").header("Authorization",member),Api.map("category","BUG","content","   "),400);
        JsonNode mine=submit(member,"  希望可以按城市筛选梦想  ",200);
        assertThat(mine.size()).isEqualTo(1);
        assertThat(mine.get(0).path("content").asText()).isEqualTo("希望可以按城市筛选梦想");
        assertThat(mine.get(0).path("contact").asText()).isEqualTo("wx: dreamer");
        assertThat(mine.get(0).path("status").asText()).isEqualTo("NEW");
        assertThat(call(get("/api/feedback/mine").header("Authorization",other),null,200).size()).isZero();
    }

    @Test void eachAccountIsLimitedPerHourAndReadOnlyAccountsMayStillSubmit() throws Exception {
        for(int i=1;i<=5;i++) submit(member,"第 "+i+" 条建议",200);
        submit(member,"第 6 条建议",429);
        submit(other,"其他账号不受影响",200);
        jdbc.update("INSERT INTO member_restrictions(member_id,mode,reason,admin_id) VALUES(?,'READ_ONLY','限制',?)",memberId,adminId);
        jdbc.update("DELETE FROM member_feedback WHERE member_id=?",memberId);
        assertThat(submit(member,"我想申诉限制",200).size()).isEqualTo(1);
    }

    @Test void adminsFilterReplyAndEveryActionIsLogged() throws Exception {
        long id=submit(member,"评论图片偶尔加载失败",200).get(0).path("id").asLong();
        call(get("/api/admin/feedback"),null,401);
        call(get("/api/admin/feedback").header("Authorization",member),null,401);
        assertThat(call(asAdmin(get("/api/admin/overview")),null,200).path("feedbackNew").asLong()).isEqualTo(1);

        JsonNode list=call(asAdmin(get("/api/admin/feedback?state=NEW&keyword=图片")),null,200);
        assertThat(list.path("total").asLong()).isEqualTo(1);
        JsonNode row=list.path("items").get(0);
        assertThat(row.path("username").asText()).isEqualTo("fb_member");
        assertThat(row.path("nickname").asText()).isEqualTo("反馈者");
        assertThat(call(asAdmin(get("/api/admin/feedback?state=RESOLVED")),null,200).path("total").asLong()).isZero();
        call(asAdmin(get("/api/admin/feedback?state=WRONG")),null,400);

        call(asAdmin(patch("/api/admin/feedback/"+id)),Api.map("status","RESOLVED","expectedStatus","PROCESSING","reply","已修复"),409);
        call(asAdmin(patch("/api/admin/feedback/"+id)),Api.map("status","PROCESSING","expectedStatus","NEW"),200);
        JsonNode resolved=call(asAdmin(patch("/api/admin/feedback/"+id)),Api.map("status","RESOLVED","expectedStatus","PROCESSING","reply","  已在新版本修复，感谢反馈  "),200);
        assertThat(resolved.path("status").asText()).isEqualTo("RESOLVED");
        assertThat(resolved.path("reply").asText()).isEqualTo("已在新版本修复，感谢反馈");
        call(asAdmin(patch("/api/admin/feedback/999999")),Api.map("status","RESOLVED","expectedStatus","NEW"),404);

        JsonNode mine=call(get("/api/feedback/mine").header("Authorization",member),null,200);
        assertThat(mine.get(0).path("status").asText()).isEqualTo("RESOLVED");
        assertThat(mine.get(0).path("reply").asText()).isEqualTo("已在新版本修复，感谢反馈");
        assertThat(call(asAdmin(get("/api/admin/overview")),null,200).path("feedbackNew").asLong()).isZero();

        JsonNode logs=call(asAdmin(get("/api/admin/logs")),null,200).path("items");
        assertThat(logs.get(0).path("targetType").asText()).isEqualTo("FEEDBACK");
        assertThat(logs.get(0).path("action").asText()).isEqualTo("FEEDBACK_RESOLVED");
        assertThat(logs.get(0).path("reason").asText()).isEqualTo("回复：已在新版本修复，感谢反馈");
        assertThat(logs.get(1).path("reason").asText()).isEqualTo("标记为处理中");
    }
}
