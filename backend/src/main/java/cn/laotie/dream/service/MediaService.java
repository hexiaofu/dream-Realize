package cn.laotie.dream.service;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.common.ApiException;
import cn.laotie.dream.security.ModerationPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * 图片和视频本地存储；随机文件名、类型校验和附件归属校验。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class MediaService {
    private static final long IMAGE_LIMIT=10L*1024*1024, VIDEO_LIMIT=100L*1024*1024;
    private final JdbcTemplate jdbc;
    private final Path root;
    private final ModerationPolicy policy;
    public MediaService(JdbcTemplate jdbc,@Value("${app.media-dir:./uploads}") String directory,ModerationPolicy policy) throws IOException {
        this.jdbc=jdbc;
        this.policy=policy;
        root=Paths.get(directory).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }
    @Transactional(rollbackFor=Exception.class) public Map<String,Object> upload(long memberId,String expectedKind,MultipartFile file) throws IOException {
        policy.writeAllowed(memberId);
        Api.require("IMAGE".equals(expectedKind)||"VIDEO".equals(expectedKind),400,"请选择图片或视频");
        Api.require(!file.isEmpty(),400,"文件不能为空");
        Api.require(file.getSize()<=("IMAGE".equals(expectedKind)?IMAGE_LIMIT:VIDEO_LIMIT),413,
            "IMAGE".equals(expectedKind)?"每张图片不能超过 10 MB":"每个视频不能超过 100 MB");
        String id=UUID.randomUUID().toString();
        Path temporary=Files.createTempFile(root,"upload-",".tmp"), stored=null;
        try {
            try(InputStream input=file.getInputStream()) { Files.copy(input,temporary,StandardCopyOption.REPLACE_EXISTING); }
            String extension=detect(temporary,expectedKind);
            String contentType=contentType(extension), filename=id+"."+extension;
            stored=root.resolve(filename);
            Files.move(temporary,stored);
            jdbc.update("INSERT INTO media_files(id,member_id,file_name,kind,content_type,size_bytes) VALUES(?,?,?,?,?,?)",
                id,memberId,filename,expectedKind,contentType,Files.size(stored));
            return view(Api.map("id",id,"file_name",filename,"kind",expectedKind,"content_type",contentType,"size_bytes",Files.size(stored)));
        } catch(IOException|RuntimeException e) {
            if(stored!=null) Files.deleteIfExists(stored);
            throw e;
        } finally { Files.deleteIfExists(temporary); }
    }
    private String detect(Path path,String kind) throws IOException {
        byte[] head=new byte[(int)Math.min(Files.size(path),4096)];
        try(DataInputStream input=new DataInputStream(Files.newInputStream(path))) { input.readFully(head); }
        String ext=null;
        if("IMAGE".equals(kind)) {
            if(starts(head,0,0xff,0xd8,0xff)) ext="jpg";
            else if(starts(head,0,137,80,78,71,13,10,26,10)) ext="png";
            else if(ascii(head,0,6).matches("GIF8[79]a")) ext="gif";
            else if(ascii(head,0,4).equals("RIFF") && ascii(head,8,4).equals("WEBP") &&
                Arrays.asList("VP8 ","VP8L","VP8X").contains(ascii(head,12,4)) && head.length>=30) ext="webp";
            Api.require(ext!=null,415,"图片支持 JPG、PNG、GIF、WebP 格式");
            if(!"webp".equals(ext)) {
                try(ImageInputStream input=ImageIO.createImageInputStream(path.toFile())) {
                    Iterator<ImageReader> readers=ImageIO.getImageReaders(input);
                    Api.require(readers.hasNext(),415,"图片文件无法识别");
                    ImageReader reader=readers.next();
                    try {
                        reader.setInput(input);
                        long pixels=(long)reader.getWidth(0)*reader.getHeight(0);
                        Api.require(pixels>0 && pixels<=50000000,400,"图片分辨率过大，请压缩后上传");
                    } finally { reader.dispose(); }
                } catch(IOException e) { throw new ApiException(415,"图片文件损坏，请重新选择"); }
            }
        } else {
            if(ascii(head,4,4).equals("ftyp") && isMp4(path)) ext="mp4";
            else if(starts(head,0,0x1a,0x45,0xdf,0xa3) &&
                new String(head,StandardCharsets.ISO_8859_1).contains("\u0042\u0082\u0084webm")) ext="webm";
            Api.require(ext!=null,415,"视频支持 MP4、WebM 格式，请转换后上传");
        }
        return ext;
    }
    private boolean isMp4(Path path) throws IOException {
        // 验证 ISO BMFF 顶层容器，排除只有 ftyp 文件头的伪造文件。
        try(RandomAccessFile file=new RandomAccessFile(path.toFile(),"r")) {
            boolean movie=false,data=false; long position=0,length=file.length();
            while(position+8<=length) {
                file.seek(position); long size=Integer.toUnsignedLong(file.readInt());
                byte[] type=new byte[4]; file.readFully(type); String box=new String(type,StandardCharsets.US_ASCII);
                long header=8;
                if(size==1) { if(position+16>length) return false; size=file.readLong(); header=16; }
                if(size==0) size=length-position;
                if(size<header || size>length-position) return false;
                if("moov".equals(box)) movie=size>header;
                if("mdat".equals(box)) data=size>header;
                position+=size;
            }
            return movie && data && position==length;
        }
    }
    private boolean starts(byte[] data,int offset,int... signature) {
        if(data.length<offset+signature.length) return false;
        for(int i=0;i<signature.length;i++) if((data[offset+i]&255)!=signature[i]) return false;
        return true;
    }
    private String ascii(byte[] data,int offset,int length) {
        return data.length>=offset+length?new String(data,offset,length,StandardCharsets.US_ASCII):"";
    }
    private String contentType(String extension) {
        if("jpg".equals(extension)) return "image/jpeg";
        return ("mp4".equals(extension)||"webm".equals(extension)?"video/":"image/")+extension;
    }
    private Map<String,Object> view(Map<String,Object> row) {
        return Api.map("id",row.get("id"),"type",row.get("kind"),"url","/api/media/"+row.get("file_name"),
            "contentType",row.get("content_type"),"size",row.get("size_bytes"));
    }
    public ResponseEntity<Resource> read(String filename) {
        return read(filename,false);
    }
    public ResponseEntity<Resource> readForAdmin(String filename) { return read(filename,true); }
    private ResponseEntity<Resource> read(String filename,boolean admin) {
        Api.require(filename.matches("[a-f0-9-]{36}\\.(jpg|png|gif|webp|mp4|webm)"),404,"文件不存在");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT content_type,dream_id,update_id FROM media_files WHERE file_name=?",filename);
        Path path=root.resolve(filename);
        Api.require(!rows.isEmpty() && Files.isRegularFile(path),404,"文件不存在");
        Map<String,Object> row=rows.get(0);
        Long dreamId=row.get("dream_id")==null?null:((Number)row.get("dream_id")).longValue();
        Long updateId=row.get("update_id")==null?null:((Number)row.get("update_id")).longValue();
        Api.require(admin || !policy.hiddenMedia(dreamId,updateId),404,"文件已下架");
        // ResourceHttpMessageConverter/ResourceRegionHttpMessageConverter 负责 Range，支持视频拖动播放。
        return ResponseEntity.ok().header("Content-Type",String.valueOf(rows.get(0).get("content_type")))
            .header("X-Content-Type-Options","nosniff").header("Cache-Control","no-store")
            .header("Content-Disposition","inline; filename=\""+filename+"\"")
            .body(new FileSystemResource(path));
    }
    @Transactional public void remove(String id,long memberId) throws IOException {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT member_id,dream_id,file_name FROM media_files WHERE id=? FOR UPDATE",id);
        Api.require(!rows.isEmpty(),404,"附件不存在");
        Map<String,Object> row=rows.get(0);
        Api.require(((Number)row.get("member_id")).longValue()==memberId,403,"只能移除自己上传的附件");
        Api.require(row.get("dream_id")==null,409,"已经发布的附件不能从上传区移除");
        requireNotAvatar(id);
        Files.deleteIfExists(root.resolve(String.valueOf(row.get("file_name"))));
        jdbc.update("DELETE FROM media_files WHERE id=?",id);
    }
    @Transactional public void attach(List<String> ids,long memberId,long dreamId,Long updateId) {
        Api.require(ids!=null && ids.size()<=10,400,"每次最多 9 张图片和 1 个视频");
        Api.require(new HashSet<>(ids).size()==ids.size(),400,"附件不能重复");
        List<String> sorted=new ArrayList<>(ids); Collections.sort(sorted);
        int images=0,videos=0;
        for(String id:sorted) {
            List<Map<String,Object>> rows=jdbc.queryForList("SELECT member_id,dream_id,kind FROM media_files WHERE id=? FOR UPDATE",id);
            Api.require(!rows.isEmpty(),400,"附件不存在，请重新上传");
            Map<String,Object> row=rows.get(0);
            Api.require(((Number)row.get("member_id")).longValue()==memberId,403,"只能使用自己上传的附件");
            Api.require(row.get("dream_id")==null,409,"附件已经发布，请重新上传");
            requireNotAvatar(id);
            if("IMAGE".equals(row.get("kind"))) images++; else videos++;
        }
        Api.require(images<=9 && videos<=1,400,"每次最多 9 张图片和 1 个视频");
        for(int i=0;i<ids.size();i++) jdbc.update("UPDATE media_files SET dream_id=?,update_id=?,sort_order=? WHERE id=?",dreamId,updateId,i,ids.get(i));
    }
    private void requireNotAvatar(String id) {
        Api.require(jdbc.queryForList("SELECT member_id FROM member_avatars WHERE media_id=? FOR UPDATE",id).isEmpty(),409,"图片正在用作头像，请重新上传附件");
    }
    public void addTo(List<Map<String,Object>> entries,boolean updates) {
        if(entries.isEmpty()) return;
        Map<Long,List<Map<String,Object>>> grouped=new HashMap<>();
        List<Object> ids=new ArrayList<>();
        for(Map<String,Object> entry:entries) {
            long id=((Number)entry.get("id")).longValue(); ids.add(id);
            List<Map<String,Object>> media=new ArrayList<>(); grouped.put(id,media); entry.put("media",media);
        }
        String column=updates?"update_id":"dream_id";
        String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
        for(Map<String,Object> row:jdbc.queryForList("SELECT * FROM media_files WHERE "+column+" IN ("+placeholders+")"+
                (updates?"":" AND update_id IS NULL")+" ORDER BY sort_order",ids.toArray())) {
            grouped.get(((Number)row.get(column)).longValue()).add(view(row));
        }
    }
}
