package cn.laotie.dream.service;

import cn.laotie.dream.api.AdminRequests;
import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.ModerationPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.sql.Timestamp;
import java.util.*;

/**
 * 后台内容查询、下架恢复、成员限制和不可编辑的处理日志。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class AdminService {
    private final JdbcTemplate jdbc;
    private final MediaService media;
    private final ModerationPolicy policy;
    private final CommentMediaService commentMedia;
    public AdminService(JdbcTemplate jdbc,MediaService media,ModerationPolicy policy,CommentMediaService commentMedia) { this.jdbc=jdbc; this.media=media; this.policy=policy; this.commentMedia=commentMedia; }
    private void page(int page,int size) { Api.require(page>=1 && page<=100000 && size>=1 && size<=50,400,"分页参数不正确"); }
    private boolean child(String type) { Api.require(Arrays.asList("DREAM","UPDATE","COMMENT").contains(type),400,"内容类型不正确"); return !"DREAM".equals(type); }
    private String table(String type) { return "COMMENT".equals(type)?"dream_comments":"UPDATE".equals(type)?"dream_updates":"dreams"; }
    private String moderationTable(String type) { return "COMMENT".equals(type)?"comment_moderation":"UPDATE".equals(type)?"update_moderation":"dream_moderation"; }
    private String moderationColumn(String type) { return "COMMENT".equals(type)?"comment_id":"UPDATE".equals(type)?"update_id":"dream_id"; }
    private Map<String,Object> result(List<Map<String,Object>> items,long total,int page,int size) { return Api.map("items",items,"total",total,"page",page,"size",size); }
    public Object overview() {
        return Api.map("dreams",jdbc.queryForObject("SELECT COUNT(*) FROM dreams",Long.class),
            "hidden",jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM dream_moderation WHERE hidden=TRUE)+(SELECT COUNT(*) FROM update_moderation WHERE hidden=TRUE)+(SELECT COUNT(*) FROM comment_moderation cm JOIN dream_comments c ON c.id=cm.comment_id WHERE cm.hidden=TRUE AND c.deleted=FALSE)",Long.class),
            "members",jdbc.queryForObject("SELECT COUNT(*) FROM members m WHERE NOT EXISTS (SELECT 1 FROM administrators a WHERE a.member_id=m.id)",Long.class),
            "restricted",jdbc.queryForObject("SELECT COUNT(*) FROM member_restrictions WHERE mode<>'NORMAL' AND (expires_at IS NULL OR expires_at>CURRENT_TIMESTAMP)",Long.class),
            "feedbackNew",jdbc.queryForObject("SELECT COUNT(*) FROM member_feedback WHERE status='NEW'",Long.class));
    }
    private String contentFrom(String type) {
        if("COMMENT".equals(type)) return " FROM dream_comments u JOIN dreams d ON d.id=u.dream_id JOIN members m ON m.id=u.member_id LEFT JOIN comment_moderation c ON c.comment_id=u.id ";
        return "UPDATE".equals(type)?" FROM dream_updates u JOIN dreams d ON d.id=u.dream_id JOIN members m ON m.id=d.member_id LEFT JOIN update_moderation c ON c.update_id=u.id ":
            " FROM dreams d JOIN members m ON m.id=d.member_id LEFT JOIN dream_moderation c ON c.dream_id=d.id ";
    }
    private List<Map<String,Object>> contentRows(String type,String where,Object... args) {
        boolean child=child(type),comment="COMMENT".equals(type); String alias=child?"u":"d";
        String anonymous=comment?"(u.anonymous=TRUE OR (d.anonymous=TRUE AND u.member_id=d.member_id))":"d.anonymous";
        return jdbc.query("SELECT "+alias+".id,d.id dream_id,d.title,"+(child?"u.content":"d.story")+" body,m.id member_id,m.username,m.nickname,"+anonymous+" anonymous,"+(comment?"u.deleted":"FALSE")+" deleted,"+alias+".created_at,COALESCE(c.hidden,FALSE) hidden,c.reason,"+
            "EXISTS(SELECT 1 FROM dream_moderation parent WHERE parent.dream_id=d.id AND parent.hidden=TRUE) parent_hidden"+contentFrom(type)+where,(rs,n)->Api.map(
            "id",rs.getLong("id"),"type",type,"dreamId",rs.getLong("dream_id"),"title",rs.getString("title"),"body",rs.getString("body"),
            "memberId",rs.getLong("member_id"),"username",rs.getString("username"),"nickname",rs.getString("nickname"),"anonymous",rs.getBoolean("anonymous"),
            "hidden",rs.getBoolean("hidden"),"deleted",rs.getBoolean("deleted"),"parentHidden",child && rs.getBoolean("parent_hidden"),"reason",rs.getString("reason"),"createdAt",rs.getTimestamp("created_at").toLocalDateTime().toString()),args);
    }
    public Object contents(String type,String keyword,String state,int page,int size) {
        boolean child=child(type); page(page,size);
        Api.require(keyword.length()<=100,400,"搜索词最多 100 字");
        Api.require(Arrays.asList("ALL","VISIBLE","HIDDEN").contains(state),400,"状态不正确");
        String where=" WHERE 1=1"; List<Object> args=new ArrayList<>();
        if(!keyword.trim().isEmpty()) {
            where+=" AND (d.title LIKE ? OR "+(child?"u.content":"d.story")+" LIKE ? OR m.username LIKE ? OR m.nickname LIKE ?)";
            for(int i=0;i<4;i++) args.add("%"+keyword.trim()+"%");
        }
        if(!"ALL".equals(state)) {
            String hidden="(COALESCE(c.hidden,FALSE)=TRUE"+(child?" OR NOT "+ModerationPolicy.VISIBLE_DREAM:"")+("COMMENT".equals(type)?" OR u.deleted=TRUE":"")+")";
            where+=" AND "+("HIDDEN".equals(state)?hidden:"NOT "+hidden);
        }
        long total=jdbc.queryForObject("SELECT COUNT(*)"+contentFrom(type)+where,Long.class,args.toArray());
        args.add(size); args.add((page-1)*size);
        return result(contentRows(type,where+" ORDER BY "+(child?"u":"d")+".id DESC LIMIT ? OFFSET ?",args.toArray()),total,page,size);
    }
    public Object content(String type,long id) {
        boolean child=child(type);
        List<Map<String,Object>> rows=contentRows(type," WHERE "+(child?"u":"d")+".id=?",id);
        Api.require(!rows.isEmpty(),404,"内容不存在");
        if("COMMENT".equals(type)) commentMedia.addTo(rows,null,true); else media.addTo(rows,child);
        Map<String,Object> item=rows.get(0);
        item.put("restriction",policy.restriction(((Number)item.get("memberId")).longValue()));
        return item;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) public Object moderate(String type,long id,long adminId,AdminRequests.Content input) {
        boolean child=child(type);
        List<Long> ids=jdbc.query("SELECT "+(child?"dream_id":"id")+" FROM "+table(type)+" WHERE id=?",(rs,n)->rs.getLong(1),id);
        Api.require(!ids.isEmpty(),404,"内容不存在");
        jdbc.queryForObject("SELECT id FROM dreams WHERE id=? FOR UPDATE",Long.class,ids.get(0));
        if("COMMENT".equals(type)) Api.require(!jdbc.queryForObject("SELECT deleted FROM dream_comments WHERE id=? FOR UPDATE",Boolean.class,id),409,"评论已被作者删除");
        String table=moderationTable(type),column=moderationColumn(type);
        List<Boolean> state=jdbc.query("SELECT hidden FROM "+table+" WHERE "+column+"=?",(rs,n)->rs.getBoolean(1),id);
        boolean hidden=!state.isEmpty() && state.get(0);
        Api.require(input.expectedHidden==hidden,409,"内容状态已变化，请刷新后操作");
        Api.require(input.hidden!=hidden,409,"内容已经是该状态");
        if(state.isEmpty()) jdbc.update("INSERT INTO "+table+"("+column+",hidden,reason,admin_id) VALUES(?,?,?,?)",id,input.hidden,input.reason.trim(),adminId);
        else jdbc.update("UPDATE "+table+" SET hidden=?,reason=?,admin_id=?,updated_at=CURRENT_TIMESTAMP WHERE "+column+"=?",input.hidden,input.reason.trim(),adminId,id);
        log(adminId,type,id,input.hidden?"HIDE":"RESTORE",input.reason,null);
        return content(type,id);
    }
    public Object members(String keyword,String state,int page,int size) {
        page(page,size); Api.require(keyword.length()<=100,400,"搜索词最多 100 字");
        Api.require(Arrays.asList("ALL","NORMAL","READ_ONLY","BLOCKED").contains(state),400,"状态不正确");
        String effective="CASE WHEN r.expires_at IS NOT NULL AND r.expires_at<=CURRENT_TIMESTAMP THEN 'NORMAL' ELSE COALESCE(r.mode,'NORMAL') END";
        String from=" FROM members m LEFT JOIN member_restrictions r ON r.member_id=m.id";
        String where=" WHERE 1=1"; List<Object> args=new ArrayList<>();
        if(!keyword.trim().isEmpty()) { where+=" AND (m.username LIKE ? OR m.nickname LIKE ?)"; args.add("%"+keyword.trim()+"%"); args.add("%"+keyword.trim()+"%"); }
        if(!"ALL".equals(state)) { where+=" AND "+effective+"=?"; args.add(state); }
        long total=jdbc.queryForObject("SELECT COUNT(*)"+from+where,Long.class,args.toArray()); args.add(size); args.add((page-1)*size);
        List<Map<String,Object>> items=jdbc.query("SELECT m.id,m.username,m.nickname,m.created_at,EXISTS(SELECT 1 FROM administrators a WHERE a.member_id=m.id) admin,"+effective+" mode,r.reason,r.expires_at,COALESCE(r.revision,0) revision"+from+where+" ORDER BY m.id DESC LIMIT ? OFFSET ?",(rs,n)->Api.map(
            "id",rs.getLong("id"),"username",rs.getString("username"),"nickname",rs.getString("nickname"),"admin",rs.getBoolean("admin"),"mode",rs.getString("mode"),"reason","NORMAL".equals(rs.getString("mode"))?null:rs.getString("reason"),
            "expiresAt","NORMAL".equals(rs.getString("mode"))||rs.getTimestamp("expires_at")==null?null:rs.getTimestamp("expires_at").toLocalDateTime().toString(),"revision",rs.getInt("revision"),"createdAt",rs.getTimestamp("created_at").toLocalDateTime().toString()),args.toArray());
        return result(items,total,page,size);
    }
    @Transactional public Object restrict(long id,long adminId,AdminRequests.Restriction input) {
        List<Long> ids=jdbc.query("SELECT id FROM members WHERE id=? FOR UPDATE",(rs,n)->rs.getLong(1),id);
        Api.require(!ids.isEmpty(),404,"成员不存在");
        Api.require(jdbc.queryForObject("SELECT COUNT(*) FROM administrators WHERE member_id=?",Integer.class,id)==0,403,"不能限制管理员账号");
        Map<String,Object> current=policy.restriction(id); int revision=((Number)current.get("revision")).intValue();
        Api.require(input.expectedRevision==revision,409,"账号限制已变化，请刷新后操作");
        Api.require(!"NORMAL".equals(input.mode) || !"NORMAL".equals(current.get("mode")),409,"账号当前没有限制");
        Timestamp until="NORMAL".equals(input.mode)||input.durationHours==null?null:new Timestamp(System.currentTimeMillis()+input.durationHours*3600000L);
        if(revision==0) jdbc.update("INSERT INTO member_restrictions(member_id,mode,reason,expires_at,admin_id,revision) VALUES(?,?,?,?,?,1)",id,input.mode,input.reason.trim(),until,adminId);
        else jdbc.update("UPDATE member_restrictions SET mode=?,reason=?,expires_at=?,admin_id=?,revision=revision+1,updated_at=CURRENT_TIMESTAMP WHERE member_id=?",input.mode,input.reason.trim(),until,adminId,id);
        if("BLOCKED".equals(input.mode)) jdbc.update("DELETE FROM sessions WHERE member_id=?",id);
        log(adminId,"MEMBER",id,input.mode,input.reason,until); return policy.restriction(id);
    }
    private void log(long adminId,String type,long id,String action,String reason,Timestamp until) {
        jdbc.update("INSERT INTO moderation_logs(admin_id,target_type,target_id,action,reason,expires_at) VALUES(?,?,?,?,?,?)",adminId,type,id,action,reason.trim(),until);
    }
    public Object logs(int page,int size) {
        page(page,size);
        List<Map<String,Object>> rows=jdbc.query("SELECT l.*,m.username FROM moderation_logs l JOIN members m ON m.id=l.admin_id ORDER BY l.id DESC LIMIT ? OFFSET ?",(rs,n)->Api.map(
            "id",rs.getLong("id"),"admin",rs.getString("username"),"targetType",rs.getString("target_type"),"targetId",rs.getLong("target_id"),"action",rs.getString("action"),"reason",rs.getString("reason"),
            "expiresAt",rs.getTimestamp("expires_at")==null?null:rs.getTimestamp("expires_at").toLocalDateTime().toString(),"createdAt",rs.getTimestamp("created_at").toLocalDateTime().toString()),size,(page-1)*size);
        return result(rows,jdbc.queryForObject("SELECT COUNT(*) FROM moderation_logs",Long.class),page,size);
    }
}
