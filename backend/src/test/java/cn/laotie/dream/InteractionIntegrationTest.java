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
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 互动幂等、评论分页归属、匿名保护与后台下架限制的集成验证。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class InteractionIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private MemberService members;
    @Autowired private Sessions sessions;
    @Autowired private JdbcTemplate jdbc;
    private long ownerId,helperId,adminId,dreamId;
    private String owner,helper;
    private Cookie cookie;
    private JsonNode call(MockHttpServletRequestBuilder builder,Object body,int code) throws Exception {
        if(body!=null) builder.contentType("application/json").content(json.writeValueAsBytes(body));
        return json.readTree(mvc.perform(builder).andExpect(status().is(code)).andReturn().getResponse().getContentAsByteArray()).path("data");
    }
    @BeforeEach void setup() throws Exception {
        ownerId=members.insertMember("social_owner","SocialTest123!","真实作者"); owner="Bearer "+sessions.create(ownerId);
        helperId=members.insertMember("social_helper","SocialTest123!","评论成员"); helper="Bearer "+sessions.create(helperId);
        adminId=members.insertMember("social_admin","SocialAdmin123!","审核员"); jdbc.update("INSERT INTO administrators(member_id) VALUES(?)",adminId);
        String header=mvc.perform(post("/api/admin/login").header("X-Admin-Request","1").contentType("application/json").content(json.writeValueAsBytes(Api.map("username","social_admin","password","SocialAdmin123!"))))
            .andExpect(status().isOk()).andReturn().getResponse().getHeader("Set-Cookie");
        cookie=new Cookie("dream-admin-session",Objects.requireNonNull(header).split(";",2)[0].split("=",2)[1]);
        dreamId=createDream(false);
        Map<String,Integer> answers=new LinkedHashMap<>(); int[] values={1,2,0,1,2,0,1,2,0,1};
        for(int i=0;i<values.length;i++) answers.put(String.valueOf(i+1),values[i]);
        String version=call(get("/api/admission/questions"),null,200).path("version").asText();
        for(String token:Arrays.asList(owner,helper)) call(post("/api/admission/check").header("Authorization",token),Api.map("answers",answers,"examVersion",version),200);
    }
    private long createDream(boolean anonymous) throws Exception {
        return call(post("/api/dreams").header("Authorization",owner),Api.map("title","互动梦想","category","LIFE","helpType","COMPANY","anonymous",anonymous),200).path("id").asLong();
    }
    private String path() { return "/api/dreams/"+dreamId; }
    private JsonNode react(String token,String value,int code) throws Exception { return call(put(path()+"/reaction").header("Authorization",token),Api.map("reaction",value),code); }
    private JsonNode comments(String token) throws Exception { return call(get(path()+"/comments").header("Authorization",token),null,200); }
    private long comment(String token,String content,boolean anonymous) throws Exception {
        call(post(path()+"/comments").header("Authorization",token),Api.map("content",content,"anonymous",anonymous),200);
        return comments(token).path("items").get(0).path("id").asLong();
    }
    private JsonNode moderate(String type,long id,boolean hidden,boolean expected,int code) throws Exception {
        return call(patch("/api/admin/contents/"+type+"/"+id).cookie(cookie).header("X-Admin-Request","1"),Api.map("hidden",hidden,"expectedHidden",expected,"reason","互动内容复核"),code);
    }
    @Test void reactionsAreExclusiveIdempotentAndPrivate() throws Exception {
        react("","SUPPORT",401); react(helper,"UNKNOWN",400);
        JsonNode first=react(helper,"SUPPORT",200); assertThat(first.path("supportCount").asInt()).isEqualTo(1);
        assertThat(react(helper,"SUPPORT",200).path("supportCount").asInt()).isEqualTo(1);
        JsonNode switched=react(helper,"DISAGREE",200);
        assertThat(switched.path("supportCount").asInt()).isZero(); assertThat(switched.path("disagreeCount").asInt()).isEqualTo(1);
        react(owner,"SUPPORT",200);
        JsonNode detail=call(get(path()).header("Authorization",helper),null,200);
        assertThat(detail.path("myReaction").asText()).isEqualTo("DISAGREE");
        assertThat(detail.path("supportCount").asInt()).isEqualTo(1);
        assertThat(call(get(path()),null,200).path("myReaction").asText()).isEqualTo("NONE");
        assertThat(call(get("/api/dreams").header("Authorization",helper),null,200).path("items").get(0).path("myReaction").asText()).isEqualTo("DISAGREE");
        assertThat(call(get("/api/members/"+ownerId),null,200).path("dreams").get(0).path("supportCount").asInt()).isEqualTo(1);
        assertThat(call(get("/api/members/"+ownerId).header("Authorization",helper),null,200).path("dreams").get(0).path("myReaction").asText()).isEqualTo("DISAGREE");
        assertThat(react(helper,"NONE",200).path("disagreeCount").asInt()).isZero();
        assertThat(react(helper,"NONE",200).path("supportCount").asInt()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dream_reactions WHERE dream_id=?",Integer.class,dreamId)).isEqualTo(1);
        call(put("/api/dreams/99999999/reaction").header("Authorization",helper),Api.map("reaction","SUPPORT"),404);
    }
    @Test void anonymousAuthorsAndOptionalAnonymousCommentersKeepTheirIdentityHidden() throws Exception {
        dreamId=createDream(true);
        react(owner,"SUPPORT",200); react(helper,"DISAGREE",200);
        String avatar=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO media_files(id,member_id,file_name,kind,content_type,size_bytes) VALUES(?,?,?,'IMAGE','image/png',1)",avatar,ownerId,avatar+".png");
        jdbc.update("INSERT INTO member_avatars(member_id,media_id) VALUES(?,?)",ownerId,avatar);
        comment(owner,"作者的匿名评论",false);
        for(String viewer:Arrays.asList("",helper,owner)) {
            JsonNode item=comments(viewer).path("items").get(0);
            assertThat(item.path("anonymous").asBoolean()).isTrue();
            assertThat(item.path("isAuthor").asBoolean()).isTrue();
            assertThat(item.path("authorAvatar").asText()).isEmpty();
            assertThat(item.path("authorReaction").asText()).isEqualTo("SUPPORT");
            assertThat(item.toString()).doesNotContain("真实作者","authorId",avatar,"member_id");
            assertThat(item.path("isMine").asBoolean()).isEqualTo(viewer.equals(owner));
        }
        comment(helper,"我想匿名留言",true);
        JsonNode anonymous=comments("").path("items").get(0);
        assertThat(anonymous.toString()).doesNotContain("评论成员","authorId");
        assertThat(anonymous.path("authorReaction").asText()).isEqualTo("DISAGREE");
        comment(helper,"公开评论",false);
        JsonNode visible=comments("").path("items").get(0);
        assertThat(visible.path("authorId").asLong()).isEqualTo(helperId);
        assertThat(visible.path("authorName").asText()).isEqualTo("评论成员");
    }
    @Test void commentsValidatePaginateAndOnlyAuthorCanDelete() throws Exception {
        call(post(path()+"/comments"),Api.map("content","访客"),401);
        call(post(path()+"/comments").header("Authorization",helper),Api.map("content","  "),400);
        call(post(path()+"/comments").header("Authorization",helper),Api.map("content",String.join("",Collections.nCopies(1001,"字"))),400);
        react(helper,"SUPPORT",200); react(owner,"DISAGREE",200);
        long first=comment(helper,"第一条",false),second=comment(owner,"第二条",false);
        JsonNode page=call(get(path()+"/comments?size=1"),null,200);
        assertThat(page.path("total").asInt()).isEqualTo(2); assertThat(page.path("hasMore").asBoolean()).isTrue();
        assertThat(page.path("items").get(0).path("id").asLong()).isEqualTo(second);
        assertThat(call(get(path()+"/comments?size=1&page=2"),null,200).path("items").get(0).path("id").asLong()).isEqualTo(first);
        call(get(path()+"/comments?size=51"),null,400);
        call(delete(path()+"/comments/"+first).header("Authorization",owner),null,403);
        long other=createDream(false);
        call(delete("/api/dreams/"+other+"/comments/"+first).header("Authorization",helper),null,404);
        JsonNode removed=call(delete(path()+"/comments/"+first).header("Authorization",helper),null,200);
        assertThat(removed.path("commentCount").asInt()).isEqualTo(1);
        call(delete(path()+"/comments/"+first).header("Authorization",helper),null,200);
        assertThat(comments("").toString()).doesNotContain("第一条");
        assertThat(call(get(path()),null,200).path("commentCount").asInt()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT content FROM dream_comments WHERE id=?",String.class,first)).isEqualTo("第一条");
    }
    @Test void moderationHidesCommentsAndCountersAndCannotRestoreDeletedComments() throws Exception {
        react(helper,"SUPPORT",200);
        long id=comment(helper,"待复核评论",false);
        call(get("/api/admin/contents?type=COMMENT").header("Authorization",helper),null,401);
        JsonNode entry=call(get("/api/admin/contents/COMMENT/"+id).cookie(cookie),null,200);
        assertThat(entry.path("memberId").asLong()).isEqualTo(helperId);
        assertThat(entry.path("media").size()).isZero();
        moderate("COMMENT",id,true,false,200);
        assertThat(comments("").path("total").asInt()).isZero();
        assertThat(call(get(path()),null,200).path("commentCount").asInt()).isZero();
        assertThat(call(get("/api/dreams"),null,200).path("items").get(0).path("commentCount").asInt()).isZero();
        JsonNode hidden=call(get("/api/admin/contents?type=COMMENT&state=HIDDEN&keyword=待复核").cookie(cookie),null,200);
        assertThat(hidden.path("total").asInt()).isEqualTo(1);
        moderate("COMMENT",id,false,false,409); moderate("COMMENT",id,false,true,200);
        assertThat(comments("").path("total").asInt()).isEqualTo(1);
        moderate("DREAM",dreamId,true,false,200);
        call(get(path()+"/comments"),null,404); react(helper,"SUPPORT",404);
        call(post(path()+"/comments").header("Authorization",helper),Api.map("content","下架后评论"),404);
        assertThat(call(get("/api/admin/contents?type=COMMENT&state=VISIBLE").cookie(cookie),null,200).path("total").asInt()).isZero();
        moderate("DREAM",dreamId,false,true,200);
        moderate("COMMENT",id,true,false,200);
        call(delete(path()+"/comments/"+id).header("Authorization",helper),null,200);
        moderate("COMMENT",id,false,true,409);
        assertThat(call(get("/api/admin/contents/COMMENT/"+id).cookie(cookie),null,200).path("deleted").asBoolean()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM moderation_logs WHERE target_type='COMMENT' AND target_id=?",Integer.class,id)).isEqualTo(3);
    }
    @Test void restrictionsBlockReactionsAndCommentsOnServer() throws Exception {
        jdbc.update("INSERT INTO member_restrictions(member_id,mode,reason,admin_id) VALUES(?,'READ_ONLY','测试限制',?)",helperId,adminId);
        react(helper,"SUPPORT",403);
        call(post(path()+"/comments").header("Authorization",helper),Api.map("content","受限评论"),403);
        assertThat(comments(helper).path("total").asInt()).isZero();
        jdbc.update("UPDATE member_restrictions SET expires_at=? WHERE member_id=?",new java.sql.Timestamp(System.currentTimeMillis()-10000),helperId);
        react(helper,"SUPPORT",200); comment(helper,"解除后评论",false);
        jdbc.update("UPDATE member_restrictions SET mode='BLOCKED',expires_at=NULL WHERE member_id=?",helperId);
        react(helper,"NONE",401);
    }
    @Test void commentsRequireOwnReactionToSameDreamAndDisplayCurrentAttitude() throws Exception {
        call(post(path()+"/comments").header("Authorization",helper),Api.map("content","未表态评论"),403);
        react(owner,"SUPPORT",200);
        long other=createDream(false);
        call(put("/api/dreams/"+other+"/reaction").header("Authorization",helper),Api.map("reaction","DISAGREE"),200);
        call(post(path()+"/comments").header("Authorization",helper),Api.map("content","伪造表态","reaction","SUPPORT","memberId",ownerId),403);
        assertThat(comments("").path("total").asInt()).isZero();

        react(helper,"SUPPORT",200);
        long id=comment(helper,"表态后评论",false);
        assertThat(comments("").path("items").get(0).path("authorReaction").asText()).isEqualTo("SUPPORT");
        react(helper,"DISAGREE",200);
        assertThat(comments("").path("items").get(0).path("authorReaction").asText()).isEqualTo("DISAGREE");
        comment(helper,"不同意也能评论",true);
        assertThat(comments("").path("items").get(0).path("authorReaction").asText()).isEqualTo("DISAGREE");

        react(helper,"NONE",200);
        call(post(path()+"/comments").header("Authorization",helper),Api.map("content","取消表态后评论"),403);
        JsonNode current=comments("");
        assertThat(current.path("total").asInt()).isEqualTo(2);
        for(JsonNode item:current.path("items")) assertThat(item.path("authorReaction").asText()).isEqualTo("NONE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dream_comments WHERE dream_id=?",Integer.class,dreamId)).isEqualTo(2);
        call(delete(path()+"/comments/"+id).header("Authorization",helper),null,200);
        react(helper,"SUPPORT",200); comment(helper,"重新表态后评论",false);
    }
}
