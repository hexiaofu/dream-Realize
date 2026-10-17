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
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 评论问答分数边界、账号绑定、持久化及权限组合的接口测试。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CommentQualificationIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private MemberService members;
    @Autowired private Sessions sessions;
    @Autowired private JdbcTemplate jdbc;
    private long memberId,otherId,dreamId;
    private String token,other,version;
    private JsonNode call(MockHttpServletRequestBuilder builder,Object body,int code) throws Exception {
        if(body!=null) builder.contentType("application/json").content(json.writeValueAsBytes(body));
        return json.readTree(mvc.perform(builder).andExpect(status().is(code)).andReturn().getResponse().getContentAsByteArray()).path("data");
    }
    @BeforeEach void setup() throws Exception {
        memberId=members.insertMember("exam_member","ExamTest123!","答题成员"); token="Bearer "+sessions.create(memberId);
        otherId=members.insertMember("exam_other","ExamTest123!","另一成员"); other="Bearer "+sessions.create(otherId);
        version=call(get("/api/admission/questions"),null,200).path("version").asText();
        dreamId=dream();
    }
    private long dream() throws Exception { return call(post("/api/dreams").header("Authorization",token),Api.map("title","评论资格测试","category","LIFE","helpType","COMPANY"),200).path("id").asLong(); }
    private Map<String,Integer> answers(int mistakes) {
        int[] correct={1,2,0,1,2,0,1,2,0,1}; Map<String,Integer> answers=new LinkedHashMap<>();
        for(int i=0;i<correct.length;i++) answers.put(String.valueOf(i+1),i<mistakes?(correct[i]+1)%3:correct[i]);
        return answers;
    }
    private JsonNode exam(int mistakes) throws Exception { return call(post("/api/admission/check").header("Authorization",token),Api.map("answers",answers(mistakes),"examVersion",version),200); }
    private void react(String viewer,String reaction) throws Exception { call(put("/api/dreams/"+dreamId+"/reaction").header("Authorization",viewer),Api.map("reaction",reaction),200); }
    private void comment(String viewer,int status) throws Exception { call(post("/api/dreams/"+dreamId+"/comments").header("Authorization",viewer),Api.map("content","测试评论","score",100,"qualified",true,"memberId",memberId),status); }

    @Test void ninetyPassesEightyFailsAndQualificationBelongsToAuthenticatedMember() throws Exception {
        react(token,"SUPPORT"); comment(token,403);
        JsonNode failed=call(post("/api/admission/check").header("Authorization",token),Api.map("answers",answers(2),"examVersion",version,"score",100,"passed",true),200);
        assertThat(failed.path("score").asInt()).isEqualTo(80); assertThat(failed.path("passed").asBoolean()).isFalse();
        comment(token,403);
        JsonNode passed=call(post("/api/admission/check").header("Authorization",token),Api.map("answers",answers(1),"examVersion",version,"memberId",otherId),200);
        assertThat(passed.path("score").asInt()).isEqualTo(90); assertThat(passed.path("passed").asBoolean()).isTrue();
        assertThat(passed.hasNonNull("admissionToken")).isFalse(); comment(token,200);
        assertThat(call(get("/api/admission/status").header("Authorization",other),null,200).path("passed").asBoolean()).isFalse();
        react(other,"DISAGREE"); comment(other,403);
        assertThat(jdbc.queryForObject("SELECT score FROM comment_qualifications WHERE member_id=?",Integer.class,memberId)).isEqualTo(90);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comment_qualifications WHERE member_id=?",Integer.class,otherId)).isZero();
    }
    @Test void authenticationCompleteAnswersAndCurrentVersionAreRequired() throws Exception {
        assertThat(call(get("/api/admission/questions"),null,200).toString()).doesNotContain("correct","explanation");
        call(get("/api/admission/status"),null,401);
        call(post("/api/admission/check"),Api.map("answers",answers(0),"examVersion",version),401);
        call(post("/api/admission/check").header("Authorization",token),Api.map("answers",answers(0),"examVersion","old-version"),412);
        Map<String,Integer> incomplete=answers(0); incomplete.remove("10");
        call(post("/api/admission/check").header("Authorization",token),Api.map("answers",incomplete,"examVersion",version),400);
        incomplete.put("999",1);
        call(post("/api/admission/check").header("Authorization",token),Api.map("answers",incomplete,"examVersion",version),400);
        for(Integer invalid:Arrays.asList(-1,3,null)) {
            Map<String,Integer> wrong=answers(0); wrong.put("1",invalid);
            call(post("/api/admission/check").header("Authorization",token),Api.map("answers",wrong,"examVersion",version),400);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comment_qualifications",Integer.class)).isZero();
    }
    @Test void qualificationSurvivesSessionsAndRetakesButNeverOverridesOtherRestrictions() throws Exception {
        exam(1);
        String firstTime=call(get("/api/admission/status").header("Authorization",token),null,200).path("passedAt").asText();
        token="Bearer "+call(post("/api/auth/login"),Api.map("username","exam_member","password","ExamTest123!"),200).path("token").asText();
        assertThat(call(get("/api/me").header("Authorization",token),null,200).path("commentQualification").path("passed").asBoolean()).isTrue();
        dreamId=dream(); comment(token,403); react(token,"DISAGREE"); comment(token,200);
        exam(0); JsonNode failedRetake=exam(10);
        assertThat(failedRetake.path("passed").asBoolean()).isFalse();
        assertThat(failedRetake.path("qualification").path("passed").asBoolean()).isTrue();
        assertThat(failedRetake.path("qualification").path("passedAt").asText()).isEqualTo(firstTime);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comment_qualifications WHERE member_id=?",Integer.class,memberId)).isEqualTo(1);
        jdbc.update("INSERT INTO administrators(member_id) VALUES(?)",otherId);
        jdbc.update("INSERT INTO member_restrictions(member_id,mode,reason,admin_id) VALUES(?,'READ_ONLY','限发测试',?)",memberId,otherId);
        comment(token,403);
    }
    @Test void legacyCommentsRemainReadableAndRemovableWithoutPassingExam() throws Exception {
        jdbc.update("INSERT INTO dream_comments(dream_id,member_id,content) VALUES(?,?,'旧评论')",dreamId,memberId);
        JsonNode list=call(get("/api/dreams/"+dreamId+"/comments").header("Authorization",token),null,200);
        assertThat(list.path("commentQualification").path("passed").asBoolean()).isFalse();
        long id=list.path("items").get(0).path("id").asLong();
        assertThat(list.path("items").get(0).path("content").asText()).isEqualTo("旧评论");
        call(delete("/api/dreams/"+dreamId+"/comments/"+id).header("Authorization",other),null,403);
        call(delete("/api/dreams/"+dreamId+"/comments/"+id).header("Authorization",token),null,200);
        react(token,"SUPPORT"); comment(token,403);
    }
}
