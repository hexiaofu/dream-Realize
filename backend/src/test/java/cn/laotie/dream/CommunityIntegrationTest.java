package cn.laotie.dream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import cn.laotie.dream.common.Api;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 注册、隐私边界、权限、状态转换及数据持久化的端到端接口测试。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CommunityIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    private JsonNode call(MockHttpServletRequestBuilder request,Object body,int expected) throws Exception {
        if(body!=null) request.contentType("application/json").content(json.writeValueAsBytes(body));
        return json.readTree(mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString()).path("data");
    }
    private JsonNode member(String name) throws Exception {
        return call(post("/api/auth/register"),registration(name),200);
    }
    private Map<String,Object> registration(String name) throws Exception {
        JsonNode notice=call(get("/api/legal/registration-notice"),null,200);
        return Api.map("username",name,"nickname",name+"昵称","password","Password123!",
            "noticeAccepted",true,"noticeVersion",notice.path("version").asText(),"noticeSha256",notice.path("sha256").asText());
    }
    private String bearer(JsonNode user) { return "Bearer "+user.path("token").asText(); }
    private JsonNode dream(JsonNode user,boolean anonymous) throws Exception {
        return call(post("/api/dreams").header("Authorization",bearer(user)),Api.map("title","我想获得现金帮助","story","可以坦率表达的梦想","category","OTHER","helpType","CASH","targetAmount",1000,"anonymous",anonymous),200);
    }
    @Test void registrationOnlyNeedsConsentAndDoesNotGrantCommentQualification() throws Exception {
        JsonNode questions=call(get("/api/admission/questions"),null,200);
        assertThat(questions.toString()).doesNotContain("correct","explanation");
        assertThat(questions.path("total").asInt()).isEqualTo(10);
        assertThat(questions.path("passScore").asInt()).isEqualTo(90);
        Map<String,Object> body=registration("member1");
        body.put("admissionToken","forged"); body.put("commentQualification",Api.map("passed",true));
        JsonNode member=call(post("/api/auth/register"),body,200);
        assertThat(member.path("user").path("commentQualification").path("passed").asBoolean()).isFalse();
        assertThat(dream(member,false).path("id").asLong()).isPositive();
    }
    @Test void anonymousIdentityNeverLeaksAcrossDiscoveryProfileHelpAndChat() throws Exception {
        JsonNode owner=member("owner1"), helper=member("helper1"), third=member("third1");
        long id=dream(owner,true).path("id").asLong();
        JsonNode publicDream=call(get("/api/dreams/"+id),null,200);
        assertThat(publicDream.toString()).doesNotContain("owner1", "authorId", "member_id");
        assertThat(call(get("/api/dreams"),null,200).toString()).doesNotContain("owner1");
        JsonNode publicProfile=call(get("/api/members/"+owner.path("user").path("id").asLong()),null,200);
        assertThat(publicProfile.path("dreams").size()).isZero();
        assertThat(call(get("/api/dreams?mine=true").header("Authorization",bearer(owner)),null,200).path("items").size()).isEqualTo(1);
        JsonNode help=call(post("/api/dreams/"+id+"/helps").header("Authorization",bearer(helper)),Api.map("offer","我愿意帮忙","rewardType","FREE"),200);
        long helpId=help.path("id").asLong(); assertThat(help.toString()).doesNotContain("owner1","owner_id");
        call(post("/api/helps/"+helpId+"/messages").header("Authorization",bearer(owner)),Api.map("content","谢谢你的帮助"),200);
        assertThat(call(get("/api/helps/"+helpId+"/messages").header("Authorization",bearer(helper)),null,200).toString()).doesNotContain("owner1","sender_id");
        call(get("/api/helps/"+helpId).header("Authorization",bearer(third)),null,403);
        call(get("/api/helps/"+helpId+"/messages").header("Authorization",bearer(third)),null,403);
        call(post("/api/helps/"+helpId+"/messages").header("Authorization",bearer(third)),Api.map("content","不该发送"),403);
    }
    @Test void helpLifecycleAndOwnerPermissionsAreEnforced() throws Exception {
        JsonNode owner=member("owner2"), helper=member("helper2"); long id=dream(owner,false).path("id").asLong();
        call(post("/api/dreams/"+id+"/helps").header("Authorization",bearer(owner)),Api.map("offer","自己帮自己"),400);
        long helpId=call(post("/api/dreams/"+id+"/helps").header("Authorization",bearer(helper)),Api.map("offer","条件双方沟通","rewardType","NEGOTIABLE"),200).path("id").asLong();
        call(post("/api/dreams/"+id+"/helps").header("Authorization",bearer(helper)),Api.map("offer","重复"),409);
        call(patch("/api/helps/"+helpId).header("Authorization",bearer(helper)),Api.map("status","ACCEPTED"),403);
        call(patch("/api/helps/"+helpId).header("Authorization",bearer(owner)),Api.map("status","ACCEPTED"),200);
        assertThat(call(get("/api/dreams/"+id),null,200).path("status").asText()).isEqualTo("IN_PROGRESS");
        call(patch("/api/helps/"+helpId).header("Authorization",bearer(owner)),Api.map("status","DECLINED"),409);
        call(post("/api/dreams/"+id+"/updates").header("Authorization",bearer(helper)),Api.map("content","越权","status","COMPLETED"),403);
        JsonNode updated=call(post("/api/dreams/"+id+"/updates").header("Authorization",bearer(owner)),Api.map("content","谢谢，梦想实现了","status","COMPLETED","receivedAmount",1000),200);
        assertThat(updated.path("updates").size()).isEqualTo(1); assertThat(updated.path("receivedAmount").asInt()).isEqualTo(1000);
        JsonNode other=member("other2");
        call(post("/api/dreams/"+id+"/helps").header("Authorization",bearer(other)),Api.map("offer","已结束"),409);
    }
    @Test void declinedAndWithdrawnThreadsAreReadOnly() throws Exception {
        JsonNode owner=member("owner3"), helper=member("helper3"); long id=dream(owner,true).path("id").asLong();
        long help=call(post("/api/dreams/"+id+"/helps").header("Authorization",bearer(helper)),Api.map("offer","帮助"),200).path("id").asLong();
        call(patch("/api/helps/"+help).header("Authorization",bearer(helper)),Api.map("status","WITHDRAWN"),200);
        call(post("/api/helps/"+help+"/messages").header("Authorization",bearer(owner)),Api.map("content","不能再发"),409);
        call(get("/api/helps/"+help+"/messages").header("Authorization",bearer(helper)),null,200);
    }
    @Test void validationAuthAndLogoutWork() throws Exception {
        call(post("/api/dreams"),Api.map("title","未登录","category","OTHER","helpType","CASH"),401);
        JsonNode owner=member("owner4");
        call(post("/api/dreams").header("Authorization",bearer(owner)),Api.map("title","负数","category","OTHER","helpType","CASH","targetAmount",-1),400);
        call(post("/api/dreams").header("Authorization",bearer(owner)),Api.map("title","   ","category","OTHER","helpType","CASH"),400);
        call(get("/api/dreams?size=9999"),null,400);
        call(post("/api/auth/login"),Api.map("username","owner4","password","incorrect"),401);
        call(post("/api/auth/logout").header("Authorization",bearer(owner)),null,200);
        call(get("/api/me").header("Authorization",bearer(owner)),null,401);
        JsonNode logged=call(post("/api/auth/login"),Api.map("username","owner4","password","Password123!"),200);
        assertThat(logged.path("token").asText()).isNotEmpty();
    }
    @Test void publicSearchAndProfileHideCredentials() throws Exception {
        JsonNode owner=member("owner5"); dream(owner,false);
        JsonNode profile=call(get("/api/members/"+owner.path("user").path("id").asLong()),null,200);
        assertThat(profile.toString()).doesNotContain("username","password","token");
        assertThat(call(get("/api/dreams?helpType=CASH&keyword=现金"),null,200).path("total").asInt()).isEqualTo(1);
        assertThat(call(get("/api/dreams?keyword=不存在"),null,200).path("total").asInt()).isZero();
    }
}
