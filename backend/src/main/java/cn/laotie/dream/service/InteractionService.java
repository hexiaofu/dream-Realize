package cn.laotie.dream.service;

import cn.laotie.dream.api.Requests;
import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.ModerationPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * 精神支持、不同意和评论；成员与梦想行锁保证写入和下架不会交错。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class InteractionService {
    private final JdbcTemplate jdbc;
    private final ModerationPolicy policy;
    private final AdmissionService admission;
    private final CommentMediaService media;
    public InteractionService(JdbcTemplate jdbc,ModerationPolicy policy,AdmissionService admission,CommentMediaService media) { this.jdbc=jdbc; this.policy=policy; this.admission=admission; this.media=media; }

    private Map<String,Object> dream(long id,boolean lock) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT member_id,anonymous FROM dreams WHERE id=?"+(lock?" FOR UPDATE":""),id);
        Api.require(!rows.isEmpty(),404,"这个梦想不存在");
        policy.dreamAvailable(id); return rows.get(0);
    }
    private boolean bool(Object value) { return Boolean.TRUE.equals(value)||(value instanceof Number && ((Number)value).intValue()!=0); }

    public void addTo(List<Map<String,Object>> entries,Long viewer) {
        if(entries.isEmpty()) return;
        Map<Long,Map<String,Object>> grouped=new HashMap<>();
        for(Map<String,Object> item:entries) {
            item.put("supportCount",0L); item.put("disagreeCount",0L); item.put("commentCount",0L); item.put("myReaction","NONE");
            if(!Boolean.TRUE.equals(item.get("hidden"))) grouped.put(((Number)item.get("id")).longValue(),item);
        }
        if(grouped.isEmpty()) return;
        List<Object> ids=new ArrayList<>(grouped.keySet()); String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
        for(Map<String,Object> row:jdbc.queryForList("SELECT dream_id,reaction,COUNT(*) total FROM dream_reactions WHERE dream_id IN ("+placeholders+") GROUP BY dream_id,reaction",ids.toArray())) {
            grouped.get(((Number)row.get("dream_id")).longValue()).put("SUPPORT".equals(row.get("reaction"))?"supportCount":"disagreeCount",row.get("total"));
        }
        for(Map<String,Object> row:jdbc.queryForList("SELECT c.dream_id,COUNT(*) total FROM dream_comments c WHERE c.dream_id IN ("+placeholders+") AND "+ModerationPolicy.VISIBLE_COMMENT+" GROUP BY c.dream_id",ids.toArray())) {
            grouped.get(((Number)row.get("dream_id")).longValue()).put("commentCount",row.get("total"));
        }
        if(viewer!=null) {
            List<Object> args=new ArrayList<>(ids); args.add(viewer);
            for(Map<String,Object> row:jdbc.queryForList("SELECT dream_id,reaction FROM dream_reactions WHERE dream_id IN ("+placeholders+") AND member_id=?",args.toArray()))
                grouped.get(((Number)row.get("dream_id")).longValue()).put("myReaction",row.get("reaction"));
        }
    }
    private Map<String,Object> summary(long dreamId,long viewer) {
        Map<String,Object> item=Api.map("id",dreamId);
        addTo(Collections.singletonList(item),viewer); return item;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) public Object react(long dreamId,long viewer,Requests.Reaction input) {
        policy.writeAllowed(viewer); dream(dreamId,true);
        // 使用目标状态而非服务端切换；重复请求保持幂等，一人仅有一条反应。
        jdbc.update("DELETE FROM dream_reactions WHERE dream_id=? AND member_id=?",dreamId,viewer);
        if(!"NONE".equals(input.reaction)) jdbc.update("INSERT INTO dream_reactions(dream_id,member_id,reaction) VALUES(?,?,?)",dreamId,viewer,input.reaction);
        return summary(dreamId,viewer);
    }
    public Object comments(long dreamId,Long viewer,int page,int size) {
        Api.require(page>=1 && page<=100000 && size>=1 && size<=50,400,"分页参数不正确");
        Map<String,Object> parent=dream(dreamId,false);
        long owner=((Number)parent.get("member_id")).longValue(); boolean anonymousOwner=bool(parent.get("anonymous"));
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM dream_comments c WHERE c.dream_id=? AND "+ModerationPolicy.VISIBLE_COMMENT,Long.class,dreamId);
        List<Map<String,Object>> items=jdbc.query("SELECT c.id,c.member_id,c.content,c.anonymous,c.created_at,m.nickname,COALESCE(r.reaction,'NONE') author_reaction,"+AvatarService.filenameSql("m")+" avatar_filename FROM dream_comments c JOIN members m ON m.id=c.member_id LEFT JOIN dream_reactions r ON r.dream_id=c.dream_id AND r.member_id=c.member_id WHERE c.dream_id=? AND "+ModerationPolicy.VISIBLE_COMMENT+" ORDER BY c.id DESC LIMIT ? OFFSET ?",(rs,n)->{
            long author=rs.getLong("member_id"); boolean anonymous=rs.getBoolean("anonymous") || (author==owner && anonymousOwner);
            Map<String,Object> item=Api.map("id",rs.getLong("id"),"content",rs.getString("content"),"anonymous",anonymous,"isAuthor",author==owner,
                "authorName",anonymous?"匿名梦想家":rs.getString("nickname"),"authorAvatar",anonymous?"":AvatarService.url(rs.getString("avatar_filename")),
                "authorReaction",rs.getString("author_reaction"),"isMine",viewer!=null && viewer==author,"createdAt",rs.getTimestamp("created_at").toLocalDateTime().toString());
            if(!anonymous) item.put("authorId",author); return item;
        },dreamId,size,(page-1)*size);
        media.addTo(items,viewer,false);
        return Api.map("items",items,"total",total,"page",page,"size",size,"hasMore",(long)page*size<total,"commentQualification",viewer==null?null:admission.status(viewer));
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) public Object comment(long dreamId,long viewer,Requests.Comment input) {
        policy.writeAllowed(viewer); Map<String,Object> parent=dream(dreamId,true);
        admission.requirePassed(viewer);
        // 与表态共用成员、梦想行锁；只认可当前用户对当前梦想的有效表态。
        Api.require(jdbc.queryForObject("SELECT COUNT(*) FROM dream_reactions WHERE dream_id=? AND member_id=? AND reaction IN ('SUPPORT','DISAGREE')",Integer.class,dreamId,viewer)>0,403,"请先选择精神支持或不同意");
        boolean anonymous=input.anonymous || (((Number)parent.get("member_id")).longValue()==viewer && bool(parent.get("anonymous")));
        Api.require(!input.content.trim().isEmpty() || !input.mediaIds.isEmpty(),400,"请输入评论或选择图片");
        KeyHolder key=new GeneratedKeyHolder();
        jdbc.update(connection->{
            PreparedStatement statement=connection.prepareStatement("INSERT INTO dream_comments(dream_id,member_id,content,anonymous) VALUES(?,?,?,?)",new String[]{"id"});
            statement.setLong(1,dreamId); statement.setLong(2,viewer); statement.setString(3,input.content.trim()); statement.setBoolean(4,anonymous); return statement;
        },key);
        media.attach(input.mediaIds,viewer,key.getKey().longValue());
        return summary(dreamId,viewer);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) public Object removeComment(long dreamId,long id,long viewer) {
        policy.writeAllowed(viewer); dream(dreamId,true);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT member_id,deleted FROM dream_comments WHERE dream_id=? AND id=? FOR UPDATE",dreamId,id);
        Api.require(!rows.isEmpty(),404,"评论不存在");
        Api.require(((Number)rows.get(0).get("member_id")).longValue()==viewer,403,"只能删除自己的评论");
        // 保留原文供后台复核，公开接口和计数都排除已删除评论。
        jdbc.update("UPDATE dream_comments SET deleted=TRUE WHERE id=?",id);
        return summary(dreamId,viewer);
    }
}
