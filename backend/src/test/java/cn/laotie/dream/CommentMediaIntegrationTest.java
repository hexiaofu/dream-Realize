package cn.laotie.dream;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.Sessions;
import cn.laotie.dream.service.MemberService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
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
import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.Cookie;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 图片评论、GIF 原始帧、收藏隔离、重用权限及来源下架的集成验证。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CommentMediaIntegrationTest {
    private static final Path DIRECTORY=createDirectory();
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MemberService members;
    @Autowired private Sessions sessions;
    private long ownerId,helperId,adminId,dreamId;
    private String owner,helper;
    private Cookie cookie;
    private static Path createDirectory() { try { return Files.createTempDirectory("dream-comment-media-"); } catch(IOException e) { throw new IllegalStateException(e); } }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("app.media-dir",()->DIRECTORY.toString()); }
    @AfterAll static void cleanFiles() throws IOException {
        try(Stream<Path> paths=Files.walk(DIRECTORY)) { for(Path path:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator) Files.deleteIfExists(path); }
    }
    private JsonNode call(MockHttpServletRequestBuilder builder,Object body,int code) throws Exception {
        if(body!=null) builder.contentType("application/json").content(json.writeValueAsBytes(body));
        return json.readTree(mvc.perform(builder).andExpect(status().is(code)).andReturn().getResponse().getContentAsByteArray()).path("data");
    }
    @BeforeEach void setup() throws Exception {
        ownerId=members.insertMember("cm_owner","CommentTest123!","图片作者"); owner="Bearer "+sessions.create(ownerId);
        helperId=members.insertMember("cm_helper","CommentTest123!","收藏者"); helper="Bearer "+sessions.create(helperId);
        adminId=members.insertMember("cm_admin","CommentTest123!","审核员"); jdbc.update("INSERT INTO administrators(member_id) VALUES(?)",adminId);
        String header=mvc.perform(post("/api/admin/login").header("X-Admin-Request","1").contentType("application/json").content(json.writeValueAsBytes(Api.map("username","cm_admin","password","CommentTest123!"))))
            .andExpect(status().isOk()).andReturn().getResponse().getHeader("Set-Cookie");
        cookie=new Cookie("dream-admin-session",Objects.requireNonNull(header).split(";",2)[0].split("=",2)[1]);
        dreamId=call(post("/api/dreams").header("Authorization",owner),Api.map("title","图片评论","category","LIFE","helpType","COMPANY","anonymous",true),200).path("id").asLong();
        String version=call(get("/api/admission/questions"),null,200).path("version").asText();
        for(long id:new long[]{ownerId,helperId}) jdbc.update("INSERT INTO comment_qualifications(member_id,exam_version,score) VALUES(?,?,90)",id,version);
        for(String token:Arrays.asList(owner,helper)) call(put(path()+"/reaction").header("Authorization",token),Api.map("reaction","SUPPORT"),200);
    }
    private String path() { return "/api/dreams/"+dreamId; }
    private byte[] picture(String format) throws IOException {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        if("gif".equals(format)) {
            ImageWriter writer=ImageIO.getImageWritersByFormatName("gif").next();
            try(ImageOutputStream stream=ImageIO.createImageOutputStream(output)) {
                writer.setOutput(stream); writer.prepareWriteSequence(null);
                for(int i=0;i<2;i++) { BufferedImage frame=new BufferedImage(12,12,BufferedImage.TYPE_INT_RGB); frame.setRGB(0,0,i==0?0xff0000:0x00ff00); writer.writeToSequence(new IIOImage(frame,null,null),null); }
                writer.endWriteSequence();
            } finally { writer.dispose(); }
        } else ImageIO.write(new BufferedImage(12,12,BufferedImage.TYPE_INT_RGB),format,output);
        return output.toByteArray();
    }
    private JsonNode upload(String token,String format) throws Exception { return upload(token,"IMAGE",picture(format)); }
    private JsonNode upload(String token,String kind,byte[] bytes) throws Exception {
        return call(multipart("/api/media").file(new MockMultipartFile("file","private-owner-name","application/octet-stream",bytes)).param("kind",kind).header("Authorization",token),null,200);
    }
    private JsonNode comment(String token,String text,int code,String... ids) throws Exception { return call(post(path()+"/comments").header("Authorization",token),Api.map("content",text,"mediaIds",Arrays.asList(ids)),code); }
    private JsonNode comments(String token) throws Exception { return call(get(path()+"/comments").header("Authorization",token),null,200).path("items"); }
    private JsonNode collect(String token,String id,int code) throws Exception { return call(put("/api/media-favorites/"+id).header("Authorization",token),null,code); }
    private JsonNode favorites(String token) throws Exception { return call(get("/api/media-favorites").header("Authorization",token),null,200); }
    private void moderate(String type,long id,boolean hidden) throws Exception { call(patch("/api/admin/contents/"+type+"/"+id).cookie(cookie).header("X-Admin-Request","1"),Api.map("hidden",hidden,"expectedHidden",!hidden,"reason","图片复核"),200); }

    @Test void emojiImageOnlyAndAnimatedGifKeepBytesAndAnonymousIdentity() throws Exception {
        byte[] gif=picture("gif"); JsonNode image=upload(owner,"IMAGE",gif),png=upload(owner,"png");
        comment(owner,"加油😀👍🎉",200,image.path("id").asText(),png.path("id").asText());
        JsonNode item=comments(helper).get(0);
        assertThat(item.path("content").asText()).isEqualTo("加油😀👍🎉");
        assertThat(item.path("authorReaction").asText()).isEqualTo("SUPPORT");
        assertThat(item.path("media").size()).isEqualTo(2);
        assertThat(item.path("media").get(0).path("contentType").asText()).isEqualTo("image/gif");
        assertThat(item.toString()).doesNotContain("authorId","member_id","图片作者","private-owner-name");
        mvc.perform(get(image.path("url").asText())).andExpect(status().isOk()).andExpect(content().contentType("image/gif")).andExpect(content().bytes(gif));
        JsonNode fresh=upload(helper,"png"); comment(helper,"",200,fresh.path("id").asText());
        assertThat(comments("").get(0).path("media").size()).isEqualTo(1);
        comment(helper,String.join("",Collections.nCopies(1000,"😀")),200);
        comment(helper,String.join("",Collections.nCopies(1001,"😀")),400);
    }
    @Test void favoritesArePersonalIdempotentAndReusableWithoutDeletingSharedFiles() throws Exception {
        JsonNode image=upload(owner,"gif"); String id=image.path("id").asText();
        collect(helper,id,403); collect("",id,401);
        comment(owner,"原图",200,id); long source=comments(owner).get(0).path("id").asLong();
        comment(helper,"直接偷用",403,id);
        collect(helper,id,200); collect(helper,id,200);
        assertThat(favorites(helper).path("total").asInt()).isEqualTo(1);
        assertThat(favorites(owner).path("total").asInt()).isZero();
        // 测试外层事务内，被拒绝请求的插入会在用例结束统一回滚；按来源编号定位。
        for(JsonNode entry:comments(helper)) if(entry.path("id").asLong()==source) assertThat(entry.path("media").get(0).path("favorite").asBoolean()).isTrue();
        comment(helper,"",200,id); long repost=comments(helper).get(0).path("id").asLong();
        call(delete(path()+"/comments/"+repost).header("Authorization",helper),null,200);
        mvc.perform(get(image.path("url").asText())).andExpect(status().isOk());
        call(delete("/api/media/"+id).header("Authorization",owner),null,409);
        call(delete("/api/media-favorites/"+id).header("Authorization",owner),null,200);
        assertThat(favorites(helper).path("total").asInt()).isEqualTo(1);
        call(delete(path()+"/comments/"+source).header("Authorization",owner),null,200);
        assertThat(favorites(helper).path("items").get(0).path("available").asBoolean()).isFalse();
        comment(helper,"来源已删除",404,id);
        call(delete("/api/media-favorites/"+id).header("Authorization",helper),null,200);
        assertThat(favorites(helper).path("total").asInt()).isZero();
    }
    @Test void mediaValidationAndCommentQualificationCannotBeBypassed() throws Exception {
        JsonNode image=upload(helper,"png"),foreign=upload(owner,"png"); String id=image.path("id").asText();
        comment(helper,"",400); comment(helper,"",400,id,id); comment(helper,"",400,(String)null);
        comment(helper,"",400,Collections.nCopies(10,id).toArray(new String[0]));
        comment(helper,"",403,foreign.path("id").asText());
        String video=upload(helper,"VIDEO",new byte[]{0x1a,0x45,(byte)0xdf,(byte)0xa3,(byte)0x87,0x42,(byte)0x82,(byte)0x84,'w','e','b','m',0}).path("id").asText();
        comment(helper,"",400,video); collect(helper,video,400);
        jdbc.update("DELETE FROM comment_qualifications WHERE member_id=?",helperId); comment(helper,"",403,id);
        jdbc.update("INSERT INTO comment_qualifications(member_id,exam_version,score) VALUES(?,?,90)",helperId,call(get("/api/admission/questions"),null,200).path("version").asText());
        call(put(path()+"/reaction").header("Authorization",helper),Api.map("reaction","NONE"),200); comment(helper,"",403,id);
        collect(helper,id,200);
        call(delete("/api/media/"+id).header("Authorization",helper),null,409);
        jdbc.update("INSERT INTO member_avatars(member_id,media_id) VALUES(?,?)",ownerId,foreign.path("id").asText());
        collect(owner,foreign.path("id").asText(),409);
        call(post("/api/dreams").header("Authorization",helper),Api.map("title","收藏不可改作梦想附件","category","LIFE","helpType","COMPANY","mediaIds",Collections.singletonList(id)),409);
        jdbc.update("INSERT INTO member_restrictions(member_id,mode,reason,admin_id) VALUES(?,'READ_ONLY','限制',?)",helperId,adminId);
        collect(helper,id,403); call(delete("/api/media-favorites/"+id).header("Authorization",helper),null,403); comment(helper,"",403,id);
    }
    @Test void moderationBlocksSourceBytesFavoritesAndRepostsWhileAdminCanReview() throws Exception {
        JsonNode image=upload(owner,"gif"); String id=image.path("id").asText();
        comment(owner,"原图",200,id); long source=comments(owner).get(0).path("id").asLong();
        collect(helper,id,200); comment(helper,"转发",200,id);
        long repost=comments(helper).get(0).path("id").asLong();
        moderate("COMMENT",repost,true);
        call(get(image.path("url").asText()),null,404);
        assertThat(favorites(helper).path("items").get(0).path("available").asBoolean()).isFalse();
        moderate("COMMENT",repost,false);
        mvc.perform(get(image.path("url").asText())).andExpect(status().isOk());
        moderate("COMMENT",source,true);
        call(get(image.path("url").asText()),null,404);
        assertThat(favorites(helper).path("items").get(0).path("url").asText()).isEmpty();
        assertThat(comments(helper).get(0).path("media").get(0).path("available").asBoolean()).isFalse();
        collect(helper,id,404); comment(helper,"绕过下架",404,id);
        JsonNode detail=call(get("/api/admin/contents/COMMENT/"+source).cookie(cookie),null,200);
        assertThat(detail.path("media").get(0).path("url").asText()).isEqualTo(image.path("url").asText());
        mvc.perform(get(image.path("url").asText().replace("/api/media/","/api/admin/media/")).cookie(cookie)).andExpect(status().isOk());
        moderate("COMMENT",source,false); assertThat(favorites(helper).path("items").get(0).path("available").asBoolean()).isTrue();
        moderate("DREAM",dreamId,true); call(get(image.path("url").asText()),null,404);
        assertThat(favorites(helper).path("items").get(0).path("available").asBoolean()).isFalse();
    }
}
