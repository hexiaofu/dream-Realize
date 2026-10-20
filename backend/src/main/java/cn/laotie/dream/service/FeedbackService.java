package cn.laotie.dream.service;

import cn.laotie.dream.api.AdminRequests;
import cn.laotie.dream.api.Requests;
import cn.laotie.dream.common.Api;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * 意见反馈：成员提交并查看处理进度，管理员筛选、标记状态并回复，处理动作写入操作日志。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class FeedbackService {
    /** 每个账号一小时内最多提交的条数，防止刷屏。 */
    static final int HOURLY_LIMIT=5;
    private static final Map<String,String> STATUS_LABELS=new HashMap<>();
    static { STATUS_LABELS.put("NEW","待处理"); STATUS_LABELS.put("PROCESSING","处理中"); STATUS_LABELS.put("RESOLVED","已处理"); }
    private final JdbcTemplate jdbc;
    public FeedbackService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private Map<String,Object> view(ResultSet rs) throws SQLException {
        return Api.map("id",rs.getLong("id"),"category",rs.getString("category"),"content",rs.getString("content"),"contact",rs.getString("contact"),
            "status",rs.getString("status"),"reply",rs.getString("reply"),"createdAt",rs.getTimestamp("created_at").toLocalDateTime().toString(),
            "updatedAt",rs.getTimestamp("updated_at").toLocalDateTime().toString());
    }
    @Transactional public Object submit(long viewer,Requests.Feedback input) {
        // 锁定成员行后再计数，并发提交也不会越过频率上限。
        jdbc.queryForObject("SELECT id FROM members WHERE id=? FOR UPDATE",Long.class,viewer);
        long recent=jdbc.queryForObject("SELECT COUNT(*) FROM member_feedback WHERE member_id=? AND created_at>?",Long.class,viewer,new java.sql.Timestamp(System.currentTimeMillis()-3600000L));
        Api.require(recent<HOURLY_LIMIT,429,"提交太频繁了，请稍后再试");
        String contact=input.contact==null?"":input.contact.trim();
        jdbc.update("INSERT INTO member_feedback(member_id,category,content,contact) VALUES(?,?,?,?)",viewer,input.category,input.content.trim(),contact);
        return mine(viewer);
    }
    /** 自己最近 20 条反馈及处理进度。 */
    public Object mine(long viewer) {
        return jdbc.query("SELECT * FROM member_feedback WHERE member_id=? ORDER BY id DESC LIMIT 20",(rs,n)->view(rs),viewer);
    }
    public Object adminList(String state,String category,String keyword,int page,int size) {
        Api.require(page>=1 && page<=100000 && size>=1 && size<=50,400,"分页参数不正确");
        Api.require(Arrays.asList("ALL","NEW","PROCESSING","RESOLVED").contains(state),400,"状态不正确");
        Api.require(Arrays.asList("ALL","SUGGESTION","BUG","EXPERIENCE","OTHER").contains(category),400,"反馈类型不正确");
        Api.require(keyword.length()<=100,400,"搜索词最多 100 字");
        String from=" FROM member_feedback f JOIN members m ON m.id=f.member_id LEFT JOIN members a ON a.id=f.admin_id";
        String where=" WHERE 1=1"; List<Object> args=new ArrayList<>();
        if(!"ALL".equals(state)) { where+=" AND f.status=?"; args.add(state); }
        if(!"ALL".equals(category)) { where+=" AND f.category=?"; args.add(category); }
        if(!keyword.trim().isEmpty()) { where+=" AND (f.content LIKE ? OR m.username LIKE ? OR m.nickname LIKE ?)"; for(int i=0;i<3;i++) args.add("%"+keyword.trim()+"%"); }
        long total=jdbc.queryForObject("SELECT COUNT(*)"+from+where,Long.class,args.toArray());
        args.add(size); args.add((page-1)*size);
        List<Map<String,Object>> items=jdbc.query("SELECT f.*,m.username,m.nickname,a.username admin_name"+from+where+" ORDER BY CASE f.status WHEN 'NEW' THEN 0 WHEN 'PROCESSING' THEN 1 ELSE 2 END,f.id DESC LIMIT ? OFFSET ?",(rs,n)->{
            Map<String,Object> item=view(rs);
            item.put("memberId",rs.getLong("member_id")); item.put("username",rs.getString("username")); item.put("nickname",rs.getString("nickname")); item.put("admin",rs.getString("admin_name"));
            return item;
        },args.toArray());
        return Api.map("items",items,"total",total,"page",page,"size",size);
    }
    @Transactional public Object handle(long id,long adminId,AdminRequests.Feedback input) {
        List<String> rows=jdbc.queryForList("SELECT status FROM member_feedback WHERE id=? FOR UPDATE",String.class,id);
        Api.require(!rows.isEmpty(),404,"反馈不存在");
        Api.require(rows.get(0).equals(input.expectedStatus),409,"反馈状态已变化，请刷新后操作");
        String reply=input.reply==null?"":input.reply.trim();
        jdbc.update("UPDATE member_feedback SET status=?,reply=?,admin_id=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",input.status,reply,adminId,id);
        String note=reply.isEmpty()?"标记为"+STATUS_LABELS.get(input.status):"回复："+reply;
        jdbc.update("INSERT INTO moderation_logs(admin_id,target_type,target_id,action,reason) VALUES(?,?,?,?,?)",adminId,"FEEDBACK",id,"FEEDBACK_"+input.status,note.length()>500?note.substring(0,500):note);
        return jdbc.queryForObject("SELECT * FROM member_feedback WHERE id=?",(rs,n)->view(rs),id);
    }
}
