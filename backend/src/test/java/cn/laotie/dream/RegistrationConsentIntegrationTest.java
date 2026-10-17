package cn.laotie.dream;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.Sessions;
import cn.laotie.dream.service.MemberService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 注册同意的强制校验、文本版本、留痕归属和旧账号兼容性。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RegistrationConsentIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Sessions sessions;
    @Autowired private MemberService members;
    private JsonNode call(MockHttpServletRequestBuilder builder,Object body,int code) throws Exception {
        if(body!=null) builder.contentType("application/json").content(json.writeValueAsBytes(body));
        return json.readTree(mvc.perform(builder).andExpect(status().is(code)).andReturn().getResponse().getContentAsByteArray()).path("data");
    }
    private JsonNode notice() throws Exception { return call(get("/api/legal/registration-notice"),null,200); }
    private Map<String,Object> registration(String name) throws Exception {
        JsonNode doc=notice();
        return Api.map("username",name,"nickname","测试成员","password","NoticeTest123!",
            "noticeAccepted",true,"noticeVersion",doc.path("version").asText(),"noticeSha256",doc.path("sha256").asText());
    }
    @Test void publicNoticeIncludesCompleteTermsAndMatchingDigest() throws Exception {
        ObjectNode doc=(ObjectNode)notice(); String digest=doc.remove("sha256").asText();
        assertThat(sessions.hash(json.writeValueAsString(doc))).isEqualTo(digest);
        assertThat(doc.path("operator").asText()).isEqualTo("青海老铁软件有限公司");
        assertThat(doc.path("contactEmail").asText()).isEqualTo("1499058156@qq.com");
        assertThat(doc.path("sections").size()).isEqualTo(8);
        assertThat(doc.toString()).contains("人身损害","重大过失","不免除平台","未成年人","私下交易");
    }
    @Test void missingDeclinedAndStaleConsentsAreRejectedWithoutCreatingAccount() throws Exception {
        Map<String,Object> body=registration("notice_gate");
        Object version=body.get("noticeVersion"),hash=body.get("noticeSha256");
        body.remove("noticeAccepted"); call(post("/api/auth/register"),body,400);
        body.put("noticeAccepted",null); call(post("/api/auth/register"),body,400);
        body.put("noticeAccepted",false); call(post("/api/auth/register"),body,400);
        body.put("noticeAccepted",true); body.remove("noticeVersion"); call(post("/api/auth/register"),body,400);
        body.put("noticeVersion",version); body.remove("noticeSha256"); call(post("/api/auth/register"),body,400);
        body.put("noticeSha256",hash); body.put("noticeVersion","old-version"); call(post("/api/auth/register"),body,412);
        body.put("noticeVersion",version); body.put("noticeSha256",String.join("",Collections.nCopies(64,"0"))); call(post("/api/auth/register"),body,412);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM members WHERE username='notice_gate'",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM registration_consents",Integer.class)).isZero();
        body.put("noticeSha256",hash); call(post("/api/auth/register"),body,200);
    }
    @Test void successfulRegistrationRecordsServerTextIdentityAndTime() throws Exception {
        Map<String,Object> body=registration("notice_record");
        body.put("memberId",999999); body.put("noticeSnapshot","伪造声明"); body.put("acceptedAt","2000-01-01");
        long before=System.currentTimeMillis();
        JsonNode registered=call(post("/api/auth/register"),body,200); long id=registered.path("user").path("id").asLong();
        Map<String,Object> record=jdbc.queryForMap("SELECT * FROM registration_consents WHERE member_id=?",id);
        assertThat(record.get("notice_version")).isEqualTo(body.get("noticeVersion"));
        assertThat(record.get("notice_sha256")).isEqualTo(body.get("noticeSha256"));
        String snapshot=(String)record.get("notice_snapshot");
        assertThat(sessions.hash(snapshot)).isEqualTo(record.get("notice_sha256"));
        assertThat(snapshot).doesNotContain("伪造声明","NoticeTest123!");
        ObjectNode document=(ObjectNode)notice(); document.remove("sha256");
        assertThat(json.readTree(snapshot)).isEqualTo(document);
        assertThat(((java.sql.Timestamp)record.get("accepted_at")).getTime()).isGreaterThanOrEqualTo(before-1000);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM registration_consents",Integer.class)).isEqualTo(1);
        assertThat(call(get("/api/members/"+id),null,200).toString()).doesNotContain("notice_snapshot","noticeSha256","accepted_at");
        call(post("/api/auth/register"),body,409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM registration_consents",Integer.class)).isEqualTo(1);
    }
    @Test void failedRegistrationAndExistingLoginDoNotFabricateConsent() throws Exception {
        long id=members.insertMember("existing_member","NoticeTest123!","已有成员");
        Map<String,Object> body=registration("existing_member");
        call(post("/api/auth/register"),body,409);
        call(post("/api/auth/login"),Api.map("username","existing_member","password","NoticeTest123!"),200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM registration_consents WHERE member_id=?",Integer.class,id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comment_qualifications WHERE member_id=?",Integer.class,id)).isZero();
    }
}
