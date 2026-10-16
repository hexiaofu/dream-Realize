package cn.laotie.dream;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.service.MemberService;
import cn.laotie.dream.security.Sessions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 头像保存、默认头像、昵称同步、归属和匿名隐私的接口回归。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ProfileIntegrationTest {
    private static final Path DIRECTORY=createDirectory();
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private MemberService members;
    @Autowired private Sessions sessions;
    @Autowired private JdbcTemplate jdbc;
    private static Path createDirectory() {
        try { return Files.createTempDirectory("dream-profile-test-"); } catch(IOException e) { throw new IllegalStateException(e); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("app.media-dir",()->DIRECTORY.toString()); }
    @AfterAll static void cleanFiles() throws IOException {
        try(Stream<Path> paths=Files.walk(DIRECTORY)) {
            for(Path path:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator) Files.deleteIfExists(path);
        }
    }
    private JsonNode call(MockHttpServletRequestBuilder builder,Object body,int statusCode) throws Exception {
        if(body!=null) builder.contentType("application/json").content(json.writeValueAsBytes(body));
        return json.readTree(mvc.perform(builder).andExpect(status().is(statusCode)).andReturn().getResponse().getContentAsByteArray()).path("data");
    }
    private long member(String name) { return members.insertMember(name,"Profile123!",name); }
    private String token(long id) { return "Bearer "+sessions.create(id); }
    private JsonNode picture(String token) throws Exception {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB),"png",output);
        return call(multipart("/api/media").file(new MockMultipartFile("file","avatar.png","image/png",output.toByteArray()))
            .param("kind","IMAGE").header("Authorization",token),null,200);
    }
    private JsonNode profile(String token,Object body,int code) throws Exception { return call(put("/api/me").header("Authorization",token),body,code); }
    private JsonNode dream(String token,boolean anonymous,String... mediaIds) throws Exception {
        return call(post("/api/dreams").header("Authorization",token),Api.map("title","头像测试梦想","category","LIFE","helpType","COMPANY","anonymous",anonymous,"mediaIds",Arrays.asList(mediaIds)),200);
    }
    @Test void profilePersistsAndCanRestoreDefaultWithoutChangingAccount() throws Exception {
        long owner=member("profile_owner"); String token=token(owner);
        JsonNode original=call(get("/api/me").header("Authorization",token),null,200);
        assertThat(original.path("avatarUrl").asText()).isEmpty();
        JsonNode image=picture(token); String url=image.path("url").asText();
        profile(token,Api.map("nickname","  新昵称  ","bio","关于我","skills","摄影","city","西宁","avatarId",image.path("id").asText(),"id",123,"username","forged"),200);
        JsonNode me=call(get("/api/me").header("Authorization",token),null,200);
        assertThat(me.path("nickname").asText()).isEqualTo("新昵称");
        assertThat(me.path("username").asText()).isEqualTo("profile_owner");
        assertThat(me.path("id").asLong()).isEqualTo(owner);
        assertThat(me.path("avatarUrl").asText()).isEqualTo(url);
        assertThat(me.path("bio").asText()).isEqualTo("关于我");
        assertThat(me.path("city").asText()).isEqualTo("西宁");
        JsonNode login=call(post("/api/auth/login"),Api.map("username","profile_owner","password","Profile123!"),200);
        assertThat(login.path("user").path("avatarUrl").asText()).isEqualTo(url);
        assertThat(call(get("/api/members/"+owner),null,200).path("avatarUrl").asText()).isEqualTo(url);
        JsonNode publicDream=dream(token,false);
        assertThat(publicDream.path("authorAvatar").asText()).isEqualTo(url);
        // 旧客户端未提供 avatarId 时保留头像，并使已经发布的梦想读取最新昵称。
        profile(token,Api.map("nickname","再次修改"),200);
        JsonNode refreshed=call(get("/api/dreams/"+publicDream.path("id").asLong()),null,200);
        assertThat(refreshed.path("authorName").asText()).isEqualTo("再次修改");
        assertThat(refreshed.path("authorAvatar").asText()).isEqualTo(url);
        JsonNode reset=profile(token,Api.map("nickname","再次修改","avatarId",""),200);
        assertThat(reset.path("avatarId").asText()).isEmpty();
        assertThat(call(get("/api/members/"+owner),null,200).path("avatarUrl").asText()).isEmpty();
    }
    @Test void avatarMustBeOwnedImageAndCannotBeDeletedOrPublishedWhileInUse() throws Exception {
        String owner=token(member("profile_file_owner")),other=token(member("profile_other"));
        JsonNode image=picture(owner); String id=image.path("id").asText();
        profile(other,Api.map("nickname","冒用","avatarId",id),403);
        profile(owner,Api.map("nickname","无效","avatarId","https://example.com/a.png"),400);
        profile(owner,Api.map("nickname","无效","avatarId",UUID.randomUUID().toString()),400);
        JsonNode video=call(multipart("/api/media").file(new MockMultipartFile("file","clip.webm","video/webm",new byte[]{0x1a,0x45,(byte)0xdf,(byte)0xa3,(byte)0x87,0x42,(byte)0x82,(byte)0x84,'w','e','b','m',0})).param("kind","VIDEO").header("Authorization",owner),null,200);
        profile(owner,Api.map("nickname","无效","avatarId",video.path("id").asText()),400);
        profile(owner,Api.map("nickname","有效","avatarId",id),200);
        call(delete("/api/media/"+id).header("Authorization",owner),null,409);
        call(post("/api/dreams").header("Authorization",owner),Api.map("title","复用头像","category","LIFE","helpType","COMPANY","mediaIds",Collections.singletonList(id)),409);
        JsonNode published=picture(owner); dream(owner,true,published.path("id").asText());
        profile(owner,Api.map("nickname","不能保存","avatarId",published.path("id").asText()),409);
        JsonNode me=call(get("/api/me").header("Authorization",owner),null,200);
        assertThat(me.path("nickname").asText()).isEqualTo("有效");
        assertThat(me.path("avatarId").asText()).isEqualTo(id);
        profile(owner,Api.map("nickname","   ","avatarId",""),400);
        call(put("/api/me"),Api.map("nickname","访客"),401);
    }
    @Test void anonymousDreamAndConversationNeverExposeOwnersAvatar() throws Exception {
        long ownerId=member("profile_private"),helperId=member("profile_helper"); String owner=token(ownerId),helper=token(helperId);
        JsonNode ownerImage=picture(owner),helperImage=picture(helper);
        profile(owner,Api.map("nickname","真实昵称","avatarId",ownerImage.path("id").asText()),200);
        profile(helper,Api.map("nickname","帮助者","avatarId",helperImage.path("id").asText()),200);
        long dreamId=dream(owner,true).path("id").asLong();
        JsonNode detail=call(get("/api/dreams/"+dreamId),null,200);
        assertThat(detail.path("authorAvatar").asText()).isEmpty();
        assertThat(detail.toString()).doesNotContain("真实昵称",ownerImage.path("id").asText(),"authorId");
        assertThat(call(get("/api/dreams"),null,200).toString()).doesNotContain(ownerImage.path("id").asText());
        assertThat(call(get("/api/members/"+ownerId),null,200).path("dreams").size()).isZero();
        JsonNode thread=call(post("/api/dreams/"+dreamId+"/helps").header("Authorization",helper),Api.map("offer","我能帮忙"),200);
        long helpId=thread.path("id").asLong();
        assertThat(thread.path("partnerAvatar").asText()).isEmpty();
        assertThat(call(get("/api/helps").header("Authorization",helper),null,200).toString()).doesNotContain(ownerImage.path("id").asText());
        assertThat(call(get("/api/helps/"+helpId).header("Authorization",owner),null,200).path("partnerAvatar").asText()).isEqualTo(helperImage.path("url").asText());
        call(post("/api/helps/"+helpId+"/messages").header("Authorization",owner),Api.map("content","谢谢"),200);
        for(String viewer:Arrays.asList(owner,helper)) {
            JsonNode messages=call(get("/api/helps/"+helpId+"/messages").header("Authorization",viewer),null,200);
            assertThat(messages.get(0).path("senderAvatar").asText()).isEmpty();
            assertThat(messages.toString()).doesNotContain(ownerImage.path("id").asText(),"真实昵称");
        }
    }
    @Test void restrictedMembersCannotChangeAvatarOrNickname() throws Exception {
        long id=member("profile_restricted"),admin=member("profile_admin"); String token=token(id);
        JsonNode image=picture(token);
        jdbc.update("INSERT INTO administrators(member_id) VALUES(?)",admin);
        jdbc.update("INSERT INTO member_restrictions(member_id,mode,reason,admin_id) VALUES(?,'READ_ONLY','测试限制',?)",id,admin);
        profile(token,Api.map("nickname","禁止修改","avatarId",image.path("id").asText()),403);
        JsonNode me=call(get("/api/me").header("Authorization",token),null,200);
        assertThat(me.path("nickname").asText()).isEqualTo("profile_restricted");
        assertThat(me.path("avatarId").asText()).isEmpty();
        jdbc.update("UPDATE member_restrictions SET mode='BLOCKED' WHERE member_id=?",id);
        profile(token,Api.map("nickname","禁止修改","avatarId",""),401);
    }
}
