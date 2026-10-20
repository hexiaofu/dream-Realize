package cn.laotie.dream;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.service.MemberService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 成员私信：会话唯一、参与者权限、未读与已读、账号限制。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DirectMessageIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MemberService members;
    private JsonNode call(MockHttpServletRequestBuilder request,Object body,int expected) throws Exception {
        if(body!=null) request.contentType("application/json").content(json.writeValueAsBytes(body));
        // 按 UTF-8 解码，才能断言中文昵称和消息内容。
        return json.readTree(mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }
    private JsonNode member(String name) throws Exception {
        JsonNode notice=call(get("/api/legal/registration-notice"),null,200);
        Map<String,Object> body=Api.map("username",name,"nickname",name+"昵称","password","Password123!",
            "noticeAccepted",true,"noticeVersion",notice.path("version").asText(),"noticeSha256",notice.path("sha256").asText());
        return call(post("/api/auth/register"),body,200);
    }
    private String bearer(JsonNode user) { return "Bearer "+user.path("token").asText(); }
    private long id(JsonNode user) { return user.path("user").path("id").asLong(); }
    private JsonNode start(JsonNode from,JsonNode to,int expected) throws Exception {
        return call(post("/api/direct/conversations").header("Authorization",bearer(from)),Api.map("memberId",id(to)),expected);
    }
    private String base(long conversation) { return "/api/direct/conversations/"+conversation; }

    @Test void onePairSharesOneConversationAndOnlyParticipantsCanUseIt() throws Exception {
        JsonNode alice=member("dm_alice"), bob=member("dm_bob"), carol=member("dm_carol");
        JsonNode opened=start(alice,bob,200);
        long conversation=opened.path("id").asLong();
        assertThat(opened.path("partnerId").asLong()).isEqualTo(id(bob));
        assertThat(opened.path("partnerName").asText()).isEqualTo("dm_bob昵称");
        assertThat(start(bob,alice,200).path("id").asLong()).isEqualTo(conversation);
        assertThat(start(alice,bob,200).path("id").asLong()).isEqualTo(conversation);

        call(post("/api/direct/conversations").header("Authorization",bearer(alice)),Api.map("memberId",id(alice)),400);
        call(post("/api/direct/conversations").header("Authorization",bearer(alice)),Api.map("memberId",999999),404);
        call(post("/api/direct/conversations"),Api.map("memberId",id(bob)),401);

        // 尚无消息的会话不出现在列表里。
        assertThat(call(get("/api/direct/conversations").header("Authorization",bearer(alice)),null,200).size()).isZero();

        call(get(base(conversation)).header("Authorization",bearer(carol)),null,403);
        call(get(base(conversation)+"/messages").header("Authorization",bearer(carol)),null,403);
        call(post(base(conversation)+"/messages").header("Authorization",bearer(carol)),Api.map("content","插话"),403);
        call(post(base(conversation)+"/messages").header("Authorization",bearer(alice)),Api.map("content","   "),400);
        call(get(base(999999)).header("Authorization",bearer(alice)),null,404);
    }

    @Test void unreadCountsAndReadPositionArePerParticipant() throws Exception {
        JsonNode alice=member("dm_reader_a"), bob=member("dm_reader_b");
        long conversation=start(alice,bob,200).path("id").asLong();
        JsonNode sent=call(post(base(conversation)+"/messages").header("Authorization",bearer(alice)),Api.map("content","  你好，想和你聊聊梦想  "),200);
        assertThat(sent.size()).isEqualTo(1);
        assertThat(sent.get(0).path("content").asText()).isEqualTo("你好，想和你聊聊梦想");
        assertThat(sent.get(0).path("mine").asBoolean()).isTrue();
        call(post(base(conversation)+"/messages").header("Authorization",bearer(alice)),Api.map("content","第二条"),200);

        JsonNode aliceList=call(get("/api/direct/conversations").header("Authorization",bearer(alice)),null,200);
        assertThat(aliceList.size()).isEqualTo(1);
        assertThat(aliceList.get(0).path("unread").asInt()).isZero();
        assertThat(aliceList.get(0).path("lastMine").asBoolean()).isTrue();

        JsonNode bobList=call(get("/api/direct/conversations").header("Authorization",bearer(bob)),null,200);
        assertThat(bobList.get(0).path("unread").asInt()).isEqualTo(2);
        assertThat(bobList.get(0).path("lastMessage").asText()).isEqualTo("第二条");
        assertThat(bobList.get(0).path("partnerName").asText()).isEqualTo("dm_reader_a昵称");

        JsonNode read=call(get(base(conversation)+"/messages").header("Authorization",bearer(bob)),null,200);
        assertThat(read.size()).isEqualTo(2);
        assertThat(read.get(0).path("mine").asBoolean()).isFalse();
        assertThat(read.get(0).path("senderName").asText()).isEqualTo("dm_reader_a昵称");
        assertThat(call(get("/api/direct/conversations").header("Authorization",bearer(bob)),null,200).get(0).path("unread").asInt()).isZero();

        // 翻看历史（before）不改变已读位置。
        call(post(base(conversation)+"/messages").header("Authorization",bearer(alice)),Api.map("content","第三条"),200);
        long oldest=read.get(0).path("id").asLong();
        assertThat(call(get(base(conversation)+"/messages?before="+(oldest+1)).header("Authorization",bearer(bob)),null,200).size()).isEqualTo(1);
        assertThat(call(get("/api/direct/conversations").header("Authorization",bearer(bob)),null,200).get(0).path("unread").asInt()).isEqualTo(1);
    }

    @Test void readOnlyAccountsCanReadButNotSendOrOpenNewConversations() throws Exception {
        JsonNode alice=member("dm_limit_a"), bob=member("dm_limit_b"), carol=member("dm_limit_c");
        long conversation=start(alice,bob,200).path("id").asLong();
        call(post(base(conversation)+"/messages").header("Authorization",bearer(alice)),Api.map("content","在吗"),200);
        long adminId=members.insertMember("dm_admin","DirectTest123!","审核员");
        jdbc.update("INSERT INTO administrators(member_id) VALUES(?)",adminId);
        jdbc.update("INSERT INTO member_restrictions(member_id,mode,reason,admin_id) VALUES(?,'READ_ONLY','限制',?)",id(bob),adminId);

        assertThat(call(get(base(conversation)+"/messages").header("Authorization",bearer(bob)),null,200).size()).isEqualTo(1);
        assertThat(start(bob,alice,200).path("id").asLong()).isEqualTo(conversation);
        call(post(base(conversation)+"/messages").header("Authorization",bearer(bob)),Api.map("content","回复"),403);
        start(bob,carol,403);
    }
}
