package cn.laotie.dream;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.service.MemberService;
import cn.laotie.dream.security.Sessions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.Cookie;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 后管权限、下架传播、媒体保护、禁发封禁、自动到期和审计日志集成测试。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AdminIntegrationTest {
    private static final Path DIRECTORY=createDirectory();
    private static final String PASSWORD="AdminTestPassword123!";
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private MemberService members;
    @Autowired private Sessions sessions;
    @Autowired private JdbcTemplate jdbc;
    private long adminId,ownerId,helperId;
    private Cookie cookie;
    private String owner,helper;
    private static Path createDirectory() {
        try { return Files.createTempDirectory("dream-admin-test-"); } catch(IOException e) { throw new IllegalStateException(e); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("app.media-dir",()->DIRECTORY.toString()); }
    @AfterAll static void cleanFiles() throws IOException {
        try(Stream<Path> paths=Files.walk(DIRECTORY)) { for(Path path:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator) Files.deleteIfExists(path); }
    }
    private JsonNode call(MockHttpServletRequestBuilder builder,Object body,int statusCode) throws Exception {
        if(body!=null) builder.contentType("application/json").content(json.writeValueAsBytes(body));
        return json.readTree(mvc.perform(builder).andExpect(status().is(statusCode)).andReturn().getResponse().getContentAsByteArray()).path("data");
    }
    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder) { return builder.cookie(cookie).header("X-Admin-Request","1"); }
    @BeforeEach void accounts() throws Exception {
        adminId=members.insertMember("mod_admin",PASSWORD,"审核员"); jdbc.update("INSERT INTO administrators(member_id) VALUES(?)",adminId);
        ownerId=members.insertMember("mod_owner",PASSWORD,"发布者"); owner="Bearer "+sessions.create(ownerId);
        helperId=members.insertMember("mod_helper",PASSWORD,"帮助者"); helper="Bearer "+sessions.create(helperId);
        String header=mvc.perform(post("/api/admin/login").header("X-Admin-Request","1").contentType("application/json")
            .content(json.writeValueAsBytes(Api.map("username","mod_admin","password",PASSWORD)))).andExpect(status().isOk()).andReturn().getResponse().getHeader("Set-Cookie");
        assertThat(header).contains("HttpOnly","SameSite=Strict","Path=/api/admin");
        cookie=new Cookie("dream-admin-session",Objects.requireNonNull(header).split(";",2)[0].split("=",2)[1]);
    }
    private JsonNode dream(String token,boolean anonymous,String... media) throws Exception {
        return call(post("/api/dreams").header("Authorization",token),Api.map("title","待审核梦想","story","内容审核测试","category","OTHER","helpType","SKILL","anonymous",anonymous,"mediaIds",Arrays.asList(media)),200);
    }
    private JsonNode upload(String token) throws Exception {
        ByteArrayOutputStream png=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"png",png);
        return call(multipart("/api/media").file(new MockMultipartFile("file","test.png","image/png",png.toByteArray())).param("kind","IMAGE").header("Authorization",token),null,200);
    }
    private JsonNode moderate(String type,long id,boolean hidden,boolean expected,int code) throws Exception {
        return call(asAdmin(patch("/api/admin/contents/"+type+"/"+id)),Api.map("hidden",hidden,"expectedHidden",expected,"reason","违规内容处理"),code);
    }
    private JsonNode restrict(long id,String mode,int revision,int code) throws Exception {
        return call(asAdmin(patch("/api/admin/members/"+id+"/restriction")),Api.map("mode",mode,"durationHours",24,"expectedRevision",revision,"reason","违规发布"),code);
    }
    @Test void adminSessionsAreSeparateAndWritesRequireCsrfHeader() throws Exception {
        call(get("/api/admin/members").header("Authorization",owner),null,401);
        call(post("/api/admin/login").header("X-Admin-Request","1"),Api.map("username","mod_owner","password",PASSWORD),401);
        call(post("/api/admin/login"),Api.map("username","mod_admin","password",PASSWORD),403);
        call(patch("/api/admin/members/"+ownerId+"/restriction").cookie(cookie),Api.map("mode","READ_ONLY","expectedRevision",0,"reason","测试"),403);
        assertThat(call(asAdmin(get("/api/admin/members")),null,200).toString()).doesNotContain("password_hash","token_hash");
        call(asAdmin(post("/api/admin/logout")),null,200);
        call(asAdmin(get("/api/admin/overview")),null,401);
    }
    @Test void hidingDreamRemovesAllPublicRoutesAndExistingMediaLinks() throws Exception {
        JsonNode media=upload(owner); long id=dream(owner,false,media.path("id").asText()).path("id").asLong();
        long help=call(post("/api/dreams/"+id+"/helps").header("Authorization",helper),Api.map("offer","提供帮助"),200).path("id").asLong();
        moderate("DREAM",id,true,false,200);
        assertThat(call(get("/api/dreams"),null,200).path("total").asInt()).isZero();
        call(get("/api/dreams/"+id),null,404);
        assertThat(call(get("/api/members/"+ownerId),null,200).path("dreams").size()).isZero();
        assertThat(call(get("/api/stats"),null,200).path("dreams").asInt()).isZero();
        assertThat(call(get("/api/helps").header("Authorization",helper),null,200).size()).isZero();
        call(get("/api/helps/"+help).header("Authorization",helper),null,404);
        call(post("/api/helps/"+help+"/messages").header("Authorization",helper),Api.map("content","不能绕过下架"),404);
        call(post("/api/dreams/"+id+"/updates").header("Authorization",owner),Api.map("content","不能继续更新"),404);
        JsonNode mine=call(get("/api/dreams?mine=true").header("Authorization",owner),null,200).path("items").get(0);
        assertThat(mine.path("hidden").asBoolean()).isTrue(); assertThat(mine.path("media").size()).isZero();
        call(get(media.path("url").asText()),null,404);
        mvc.perform(asAdmin(get(media.path("url").asText().replace("/api/media/","/api/admin/media/")))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        assertThat(call(asAdmin(get("/api/admin/contents/DREAM/"+id)),null,200).path("body").asText()).isEqualTo("内容审核测试");
        moderate("DREAM",id,false,true,200);
        mvc.perform(get(media.path("url").asText())).andExpect(status().isOk());
        call(get("/api/dreams/"+id),null,200);
    }
    @Test void updateModerationIsIndependentAndCannotBeBypassedThroughParentRestore() throws Exception {
        long id=dream(owner,true).path("id").asLong(); JsonNode media=upload(owner);
        long update=call(post("/api/dreams/"+id+"/updates").header("Authorization",owner),Api.map("content","进展内容","mediaIds",Collections.singletonList(media.path("id").asText())),200).path("updates").get(0).path("id").asLong();
        moderate("UPDATE",update,true,false,200); moderate("DREAM",id,true,false,200); moderate("DREAM",id,false,true,200);
        assertThat(call(get("/api/dreams/"+id),null,200).path("updates").size()).isZero(); call(get(media.path("url").asText()),null,404);
        JsonNode adminView=call(asAdmin(get("/api/admin/contents/UPDATE/"+update)),null,200);
        assertThat(adminView.path("username").asText()).isEqualTo("mod_owner");
        assertThat(call(get("/api/dreams/"+id),null,200).toString()).doesNotContain("mod_owner","authorId");
        moderate("UPDATE",update,false,true,200);
        assertThat(call(get("/api/dreams/"+id),null,200).path("updates").size()).isEqualTo(1);
    }
    @Test void readOnlyBlocksEveryPublishingPathAndExpiresAutomatically() throws Exception {
        long id=dream(owner,false).path("id").asLong(); long other=dream(helper,false).path("id").asLong();
        long thread=call(post("/api/dreams/"+id+"/helps").header("Authorization",helper),Api.map("offer","帮助"),200).path("id").asLong();
        restrict(ownerId,"READ_ONLY",0,200);
        call(get("/api/me").header("Authorization",owner),null,200);
        call(post("/api/auth/login"),Api.map("username","mod_owner","password",PASSWORD),200);
        call(post("/api/dreams").header("Authorization",owner),Api.map("title","新梦想","category","OTHER","helpType","SKILL"),403);
        call(post("/api/dreams/"+id+"/updates").header("Authorization",owner),Api.map("content","进展"),403);
        call(post("/api/dreams/"+other+"/helps").header("Authorization",owner),Api.map("offer","帮助"),403);
        call(post("/api/helps/"+thread+"/messages").header("Authorization",owner),Api.map("content","消息"),403);
        call(patch("/api/helps/"+thread).header("Authorization",owner),Api.map("status","ACCEPTED"),403);
        call(put("/api/me").header("Authorization",owner),Api.map("nickname","修改名片"),403);
        call(multipart("/api/media").file(new MockMultipartFile("file","file.png","image/png",new byte[]{1})).param("kind","IMAGE").header("Authorization",owner),null,403);
        jdbc.update("UPDATE member_restrictions SET expires_at=? WHERE member_id=?",new Timestamp(System.currentTimeMillis()-60000),ownerId);
        assertThat(call(get("/api/me").header("Authorization",owner),null,200).path("restriction").path("mode").asText()).isEqualTo("NORMAL");
        dream(owner,false);
    }
    @Test void blockingRevokesSessionsAndLiftingDoesNotResurrectOldTokens() throws Exception {
        restrict(ownerId,"BLOCKED",0,200);
        call(get("/api/me").header("Authorization",owner),null,401);
        call(post("/api/auth/login"),Api.map("username","mod_owner","password",PASSWORD),403);
        restrict(ownerId,"NORMAL",1,200);
        call(get("/api/me").header("Authorization",owner),null,401);
        call(post("/api/auth/login"),Api.map("username","mod_owner","password",PASSWORD),200);
        restrict(adminId,"BLOCKED",0,403);
        restrict(ownerId,"BLOCKED",2,200);
        jdbc.update("UPDATE member_restrictions SET expires_at=? WHERE member_id=?",new Timestamp(System.currentTimeMillis()-60000),ownerId);
        call(post("/api/auth/login"),Api.map("username","mod_owner","password",PASSWORD),200);
    }
    @Test void reasonsStaleChangesAndAuditAreEnforced() throws Exception {
        long id=dream(owner,false).path("id").asLong();
        call(asAdmin(patch("/api/admin/contents/DREAM/"+id)),Api.map("hidden",true,"expectedHidden",false,"reason","  "),400);
        moderate("DREAM",id,true,false,200); moderate("DREAM",id,false,false,409);
        restrict(ownerId,"READ_ONLY",0,200); restrict(ownerId,"BLOCKED",0,409);
        JsonNode logs=call(asAdmin(get("/api/admin/logs")),null,200); assertThat(logs.path("total").asInt()).isEqualTo(2);
        assertThat(logs.path("items").get(0).path("admin").asText()).isEqualTo("mod_admin");
        call(asAdmin(get("/api/admin/contents?page=0")),null,400);
        call(asAdmin(get("/api/admin/members?size=1000")),null,400);
        call(asAdmin(get("/api/admin/contents?type=SQL")),null,400);
    }
}
