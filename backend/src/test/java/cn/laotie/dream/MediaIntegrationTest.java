package cn.laotie.dream;

import cn.laotie.dream.common.Api;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
 * 上传校验、附件归属、匿名响应、进展附件和视频 Range 读取测试。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MediaIntegrationTest {
    private static final Path DIRECTORY=createDirectory();
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    private static Path createDirectory() {
        try { return Files.createTempDirectory("dream-media-test-"); } catch(IOException e) { throw new IllegalStateException(e); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("app.media-dir",()->DIRECTORY.toString()); }
    @AfterAll static void cleanFiles() throws IOException {
        try(Stream<Path> paths=Files.walk(DIRECTORY)) {
            for(Path path:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator) Files.deleteIfExists(path);
        }
    }
    private JsonNode call(MockHttpServletRequestBuilder builder,Object body,int statusCode) throws Exception {
        if(body!=null) builder.contentType("application/json").content(json.writeValueAsBytes(body));
        return json.readTree(mvc.perform(builder).andExpect(status().is(statusCode)).andReturn().getResponse().getContentAsString()).path("data");
    }
    private String member(String name) throws Exception {
        JsonNode notice=call(get("/api/legal/registration-notice"),null,200);
        return "Bearer "+call(post("/api/auth/register"),Api.map("username",name,"nickname",name,"password","MediaTest123!",
            "noticeAccepted",true,"noticeVersion",notice.path("version").asText(),"noticeSha256",notice.path("sha256").asText()),200).path("token").asText();
    }
    private byte[] picture() throws IOException {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(12,12,BufferedImage.TYPE_INT_RGB),"png",output); return output.toByteArray();
    }
    private byte[] video() {
        // EBML WebM header;容器读取测试仅验证原始字节和 Range，不模拟转码。
        return new byte[]{0x1a,0x45,(byte)0xdf,(byte)0xa3,(byte)0x87,0x42,(byte)0x82,(byte)0x84,'w','e','b','m',0};
    }
    private JsonNode upload(String token,String kind,String name,byte[] content,int statusCode) throws Exception {
        return call(multipart("/api/media").file(new MockMultipartFile("file",name,"application/octet-stream",content))
            .param("kind",kind).header("Authorization",token),null,statusCode);
    }
    private Map<String,Object> dreamBody(String... ids) {
        return Api.map("title","记录梦想的图片与视频","category","CREATION","helpType","SKILL","anonymous",true,"mediaIds",Arrays.asList(ids));
    }
    @Test void uploadRejectsUnsignedRequestsFakeFormatsAndOversizedImages() throws Exception {
        String token=member("media_validation");
        upload("","IMAGE","valid.png",picture(),401);
        upload(token,"IMAGE","fake.png","<svg onload='alert(1)'/>".getBytes("UTF-8"),415);
        upload(token,"VIDEO","fake.mp4",picture(),415);
        upload(token,"IMAGE","large.png",new byte[10*1024*1024+1],413);
        upload(token,"IMAGE","empty.png",new byte[0],400);
        call(multipart("/api/media").param("kind","IMAGE").header("Authorization",token),null,400);
    }
    @Test void anonymousDreamPersistsMediaAndVideoSupportsRange() throws Exception {
        String token=member("media_owner"); byte[] png=picture(),webm=video();
        JsonNode image=upload(token,"IMAGE","private-name-with-owner.png",png,200);
        JsonNode clip=upload(token,"VIDEO","movie.webm",webm,200);
        JsonNode dream=call(post("/api/dreams").header("Authorization",token),dreamBody(image.path("id").asText(),clip.path("id").asText()),200);
        long id=dream.path("id").asLong();
        JsonNode detail=call(get("/api/dreams/"+id),null,200);
        assertThat(detail.path("media").size()).isEqualTo(2);
        assertThat(detail.toString()).doesNotContain("media_owner","private-name-with-owner","member_id","authorId");
        assertThat(call(get("/api/dreams"),null,200).path("items").get(0).path("media").size()).isEqualTo(2);
        mvc.perform(get(image.path("url").asText())).andExpect(status().isOk()).andExpect(content().contentType("image/png"))
            .andExpect(header().string("X-Content-Type-Options","nosniff")).andExpect(content().bytes(png));
        mvc.perform(get(clip.path("url").asText()).header("Range","bytes=0-7")).andExpect(status().isPartialContent())
            .andExpect(header().string("Content-Range","bytes 0-7/"+webm.length)).andExpect(content().bytes(Arrays.copyOf(webm,8)));
        mvc.perform(get(clip.path("url").asText()).header("Range","bytes=99999-")).andExpect(status().isRequestedRangeNotSatisfiable());
    }
    @Test void attachmentsMustBelongToAuthorAndCannotBeDuplicatedOrReused() throws Exception {
        String owner=member("media_owner2"),other=member("media_other2");
        String id=upload(owner,"IMAGE","photo.png",picture(),200).path("id").asText();
        call(post("/api/dreams").header("Authorization",other),dreamBody(id),403);
        call(post("/api/dreams").header("Authorization",owner),dreamBody(id,id),400);
        call(post("/api/dreams").header("Authorization",owner),dreamBody(id),200);
        call(post("/api/dreams").header("Authorization",owner),dreamBody(id),409);
        call(post("/api/dreams").header("Authorization",owner),dreamBody((String)null),400);
    }
    @Test void progressMediaStaysOnItsUpdateAndOnlyOneVideoIsAllowed() throws Exception {
        String owner=member("media_progress");
        long dreamId=call(post("/api/dreams").header("Authorization",owner),dreamBody(),200).path("id").asLong();
        String image=upload(owner,"IMAGE","step.png",picture(),200).path("id").asText();
        JsonNode detail=call(post("/api/dreams/"+dreamId+"/updates").header("Authorization",owner),Api.map("content","有了新的进展","mediaIds",Collections.singletonList(image)),200);
        assertThat(detail.path("media").size()).isZero();
        assertThat(detail.path("updates").get(0).path("media").get(0).path("id").asText()).isEqualTo(image);
        String first=upload(owner,"VIDEO","one.webm",video(),200).path("id").asText();
        String second=upload(owner,"VIDEO","two.webm",video(),200).path("id").asText();
        call(post("/api/dreams").header("Authorization",owner),dreamBody(first,second),400);
    }
    @Test void onlyOwnerCanRemoveUnpublishedUploads() throws Exception {
        String owner=member("media_remove"),other=member("media_remove_other");
        JsonNode image=upload(owner,"IMAGE","photo.png",picture(),200);
        String path="/api/media/"+image.path("id").asText();
        call(delete(path).header("Authorization",other),null,403);
        call(delete(path).header("Authorization",owner),null,200);
        call(get(image.path("url").asText()),null,404);
        JsonNode posted=upload(owner,"IMAGE","posted.png",picture(),200);
        call(post("/api/dreams").header("Authorization",owner),dreamBody(posted.path("id").asText()),200);
        call(delete("/api/media/"+posted.path("id").asText()).header("Authorization",owner),null,409);
    }
}
