package cn.laotie.dream.service;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.ModerationPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.util.*;

/**
 * 评论图片、GIF 与个人收藏，沿用原始文件并追溯来源的审核状态。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class CommentMediaService {
    private final JdbcTemplate jdbc;
    private final ModerationPolicy policy;
    public CommentMediaService(JdbcTemplate jdbc,ModerationPolicy policy) { this.jdbc=jdbc; this.policy=policy; }

    private Map<String,Object> image(String id) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM media_files WHERE id=? FOR UPDATE",id);
        Api.require(!rows.isEmpty(),404,"图片不存在");
        Map<String,Object> file=rows.get(0);
        Api.require("IMAGE".equals(file.get("kind")),400,"评论仅支持图片和 GIF");
        Api.require(file.get("dream_id")==null && jdbc.queryForObject("SELECT COUNT(*) FROM member_avatars WHERE media_id=?",Integer.class,id)==0,409,"请选择评论图片或重新上传");
        Api.require(!policy.hiddenCommentMedia(id),404,"图片已不可用");
        return file;
    }
    private boolean published(String id) { return jdbc.queryForObject("SELECT COUNT(*) FROM comment_attachments WHERE media_id=? AND is_source=TRUE",Integer.class,id)>0; }
    private boolean favorite(long member,String id) { return jdbc.queryForObject("SELECT COUNT(*) FROM media_favorites WHERE member_id=? AND media_id=?",Integer.class,member,id)>0; }
    private Map<String,Object> view(Map<String,Object> file,boolean available) {
        return Api.map("id",file.get("id"),"type","IMAGE","url",available?"/api/media/"+file.get("file_name"):"",
            "contentType",file.get("content_type"),"size",file.get("size_bytes"),"available",available);
    }
    // 调用方已在事务中锁定成员及目标梦想，按编号加锁避免多附件顺序不同造成死锁。
    public void attach(List<String> ids,long member,long comment) {
        Api.require(ids.size()<=9 && new HashSet<>(ids).size()==ids.size(),400,"最多 9 张图片，请勿重复选择");
        List<String> sorted=new ArrayList<>(ids); Collections.sort(sorted);
        Set<String> sources=new HashSet<>();
        for(String id:sorted) {
            Map<String,Object> file=image(id); boolean source=published(id);
            if(!source) Api.require(((Number)file.get("member_id")).longValue()==member,403,"只能使用自己的上传或收藏");
            else Api.require(favorite(member,id),403,"请先收藏图片再发送");
            if(!source) sources.add(id);
        }
        for(int i=0;i<ids.size();i++) jdbc.update("INSERT INTO comment_attachments(comment_id,media_id,sort_order,is_source) VALUES(?,?,?,?)",comment,ids.get(i),i,sources.contains(ids.get(i)));
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Object collect(long member,String id) {
        policy.writeAllowed(member); Map<String,Object> file=image(id);
        Api.require(published(id) || ((Number)file.get("member_id")).longValue()==member,403,"不能收藏他人未发布的图片");
        if(!favorite(member,id)) {
            Api.require(jdbc.queryForObject("SELECT COUNT(*) FROM media_favorites WHERE member_id=?",Integer.class,member)<500,400,"收藏已满，请先移除一些图片");
            jdbc.update("INSERT INTO media_favorites(member_id,media_id) VALUES(?,?)",member,id);
        }
        Map<String,Object> item=view(file,true); item.put("favorite",true); return item;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void uncollect(long member,String id) {
        policy.writeAllowed(member);
        jdbc.update("DELETE FROM media_favorites WHERE member_id=? AND media_id=?",member,id);
    }
    public Object favorites(long member,int page,int size) {
        Api.require(page>=1 && page<=100000 && size>=1 && size<=50,400,"分页参数不正确");
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM media_favorites WHERE member_id=?",Long.class,member);
        List<Map<String,Object>> items=new ArrayList<>();
        for(Map<String,Object> row:jdbc.queryForList("SELECT f.*,"+ModerationPolicy.VISIBLE_COMMENT_MEDIA+" available FROM media_favorites fav JOIN media_files f ON f.id=fav.media_id WHERE fav.member_id=? ORDER BY fav.created_at DESC,f.id DESC LIMIT ? OFFSET ?",member,size,(page-1)*size)) {
            Map<String,Object> item=view(row,bool(row.get("available"))); item.put("favorite",true); items.add(item);
        }
        return Api.map("items",items,"total",total,"hasMore",(long)page*size<total);
    }
    private boolean bool(Object value) { return Boolean.TRUE.equals(value)||(value instanceof Number && ((Number)value).intValue()!=0); }
    public void addTo(List<Map<String,Object>> entries,Long viewer,boolean admin) {
        if(entries.isEmpty()) return;
        Map<Long,List<Map<String,Object>>> grouped=new HashMap<>();
        for(Map<String,Object> entry:entries) {
            List<Map<String,Object>> files=new ArrayList<>(); entry.put("media",files); grouped.put(((Number)entry.get("id")).longValue(),files);
        }
        List<Object> args=new ArrayList<>(); args.add(viewer==null?-1L:viewer); args.addAll(grouped.keySet());
        String placeholders=String.join(",",Collections.nCopies(grouped.size(),"?"));
        for(Map<String,Object> row:jdbc.queryForList("SELECT f.*,a.comment_id,"+ModerationPolicy.VISIBLE_COMMENT_MEDIA+" available,EXISTS(SELECT 1 FROM media_favorites fav WHERE fav.media_id=f.id AND fav.member_id=?) favorite FROM comment_attachments a JOIN media_files f ON f.id=a.media_id WHERE a.comment_id IN ("+placeholders+") ORDER BY a.sort_order",args.toArray())) {
            Map<String,Object> item=view(row,admin || bool(row.get("available"))); item.put("favorite",bool(row.get("favorite")));
            grouped.get(((Number)row.get("comment_id")).longValue()).add(item);
        }
    }
}
