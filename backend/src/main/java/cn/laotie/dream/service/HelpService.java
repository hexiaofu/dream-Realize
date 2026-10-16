package cn.laotie.dream.service;

import cn.laotie.dream.api.Requests;
import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.ModerationPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * 帮助申请与双方会话，参与者校验、状态转换和匿名身份统一在服务端处理。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class HelpService {
    private final JdbcTemplate jdbc;
    private final DreamService dreams;
    private final ModerationPolicy policy;
    private static final String SELECT="SELECT h.*,d.title,d.member_id owner_id,d.anonymous,d.status dream_status,o.nickname owner_name,m.nickname helper_name,"+AvatarService.filenameSql("o")+" owner_avatar,"+AvatarService.filenameSql("m")+" helper_avatar FROM help_requests h JOIN dreams d ON d.id=h.dream_id JOIN members o ON o.id=d.member_id JOIN members m ON m.id=h.helper_id ";
    public HelpService(JdbcTemplate jdbc,DreamService dreams,ModerationPolicy policy) { this.jdbc=jdbc; this.dreams=dreams; this.policy=policy; }
    @Transactional public Object apply(long dreamId,long viewer,Requests.Help input) {
        policy.writeAllowed(viewer);
        Map<String,Object> dream=dreams.raw(dreamId,true);
        Api.require(((Number)dream.get("member_id")).longValue()!=viewer,400,"可以在自己的梦想里更新进展，无需申请帮助");
        Api.require(Arrays.asList("OPEN","IN_PROGRESS").contains(dream.get("status")),409,"这个梦想目前不接受新的帮助");
        Api.require(jdbc.queryForObject("SELECT COUNT(*) FROM help_requests WHERE dream_id=? AND helper_id=?",Integer.class,dreamId,viewer)==0,409,"你已发起过帮助，请在消息中查看");
        GeneratedKeyHolder keys=new GeneratedKeyHolder();
        jdbc.update(connection->{
            PreparedStatement ps=connection.prepareStatement("INSERT INTO help_requests(dream_id,helper_id,offer,reward_type) VALUES(?,?,?,?)",new String[]{"id"});
            ps.setLong(1,dreamId); ps.setLong(2,viewer); ps.setString(3,input.offer.trim()); ps.setString(4,input.rewardType); return ps;
        },keys);
        return detail(Objects.requireNonNull(keys.getKey()).longValue(),viewer);
    }
    private Map<String,Object> raw(long id,long viewer) {
        List<Map<String,Object>> rows=jdbc.queryForList(SELECT+" WHERE h.id=?",id);
        Api.require(!rows.isEmpty(),404,"帮助申请不存在"); Map<String,Object> row=rows.get(0);
        Api.require(number(row,"owner_id")==viewer || number(row,"helper_id")==viewer,403,"仅双方可以查看这段交流"); policy.dreamAvailable(number(row,"dream_id")); return row;
    }
    private long number(Map<String,Object> row,String key) { return ((Number)row.get(key)).longValue(); }
    private boolean anonymous(Map<String,Object> row) { Object value=row.get("anonymous"); return Boolean.TRUE.equals(value) || (value instanceof Number && ((Number)value).intValue()!=0); }
    private String ownerName(Map<String,Object> row) { return anonymous(row)?"匿名梦想家":(String)row.get("owner_name"); }
    private String ownerAvatar(Map<String,Object> row) { return anonymous(row)?"":AvatarService.url((String)row.get("owner_avatar")); }
    private Map<String,Object> view(Map<String,Object> row,long viewer) {
        boolean owner=number(row,"owner_id")==viewer;
        return Api.map("id",row.get("id"),"dreamId",row.get("dream_id"),"dreamTitle",row.get("title"),"offer",row.get("offer"),
            "rewardType",row.get("reward_type"),"status",row.get("status"),"dreamStatus",row.get("dream_status"),"isOwner",owner,
            "partnerAvatar",owner?AvatarService.url((String)row.get("helper_avatar")):ownerAvatar(row),
            "partnerName",owner?row.get("helper_name"):ownerName(row),"ownerName",ownerName(row),"helperName",row.get("helper_name"),
            "helperId",row.get("helper_id"),"anonymous",anonymous(row),"createdAt",row.get("created_at").toString());
    }
    public Object list(long viewer) {
        List<Map<String,Object>> rows=jdbc.queryForList(SELECT+" WHERE (d.member_id=? OR h.helper_id=?) AND "+ModerationPolicy.VISIBLE_DREAM+" ORDER BY h.updated_at DESC,h.id DESC LIMIT 100",viewer,viewer);
        List<Object> items=new ArrayList<>(); for(Map<String,Object> row:rows) items.add(view(row,viewer)); return items;
    }
    public Object detail(long id,long viewer) { return view(raw(id,viewer),viewer); }
    @Transactional public Object decision(long id,long viewer,Requests.Decision input) {
        policy.writeAllowed(viewer);
        Map<String,Object> row=raw(id,viewer);
        boolean owner=number(row,"owner_id")==viewer;
        Api.require("WITHDRAWN".equals(input.status)?!owner:owner,403,"没有操作这条申请的权限");
        // 与发布者更新进展使用同一梦想行锁，避免完成之后又接受帮助。
        Map<String,Object> dream=dreams.raw(number(row,"dream_id"),true);
        if("ACCEPTED".equals(input.status)) Api.require(Arrays.asList("OPEN","IN_PROGRESS").contains(dream.get("status")),409,"梦想当前暂停或已完成");
        int changed=jdbc.update("UPDATE help_requests SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='PENDING'",input.status,id);
        Api.require(changed==1,409,"申请状态已变化，请刷新");
        if("ACCEPTED".equals(input.status)) jdbc.update("UPDATE dreams SET status='IN_PROGRESS',updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='OPEN'",number(row,"dream_id"));
        return detail(id,viewer);
    }
    public Object messages(long id,long viewer,long before) {
        Map<String,Object> row=raw(id,viewer);
        List<Map<String,Object>> result=jdbc.query("SELECT id,sender_id,content,created_at FROM messages WHERE help_id=? AND id<? ORDER BY id DESC LIMIT 50",(rs,index)->{
            long sender=rs.getLong("sender_id");
            return Api.map("id",rs.getLong("id"),"content",rs.getString("content"),"mine",sender==viewer,
                "senderAvatar",sender==number(row,"owner_id")?ownerAvatar(row):AvatarService.url((String)row.get("helper_avatar")),
                "senderName",sender==number(row,"owner_id")?ownerName(row):row.get("helper_name"),"createdAt",rs.getTimestamp("created_at").toLocalDateTime().toString());
        },id,before<=0?Long.MAX_VALUE:before);
        Collections.reverse(result); return result;
    }
    @Transactional public Object send(long id,long viewer,Requests.Message input) {
        policy.writeAllowed(viewer);
        Map<String,Object> row=raw(id,viewer);
        dreams.raw(number(row,"dream_id"),true);
        // 锁定申请后重新检查状态，拒绝/撤回与发送不会交错越过状态边界。
        String status=jdbc.queryForObject("SELECT status FROM help_requests WHERE id=? FOR UPDATE",String.class,id);
        Api.require(Arrays.asList("PENDING","ACCEPTED").contains(status),409,"这段交流已结束，可以查看历史消息");
        jdbc.update("INSERT INTO messages(help_id,sender_id,content) VALUES(?,?,?)",id,viewer,input.content.trim());
        jdbc.update("UPDATE help_requests SET updated_at=CURRENT_TIMESTAMP WHERE id=?",id);
        return messages(id,viewer,0);
    }
}
