package cn.laotie.dream.service;

import cn.laotie.dream.api.Requests;
import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.ModerationPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * 成员之间的私信：一对成员共用一个会话，仅双方可读写；发送受账号限制约束，已读位置按双方分别记录。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class DirectMessageService {
    private final JdbcTemplate jdbc;
    private final ModerationPolicy policy;
    private static final String COLUMNS="SELECT c.*,l.nickname low_name,h.nickname high_name,"+AvatarService.filenameSql("l")+" low_avatar,"+AvatarService.filenameSql("h")+" high_avatar";
    private static final String FROM=" FROM direct_conversations c JOIN members l ON l.id=c.member_low JOIN members h ON h.id=c.member_high ";
    public DirectMessageService(JdbcTemplate jdbc,ModerationPolicy policy) { this.jdbc=jdbc; this.policy=policy; }
    private long number(Map<String,Object> row,String key) { return ((Number)row.get(key)).longValue(); }
    private boolean isLow(Map<String,Object> row,long viewer) { return number(row,"member_low")==viewer; }
    private Map<String,Object> raw(long id,long viewer) {
        List<Map<String,Object>> rows=jdbc.queryForList(COLUMNS+FROM+" WHERE c.id=?",id);
        Api.require(!rows.isEmpty(),404,"私信会话不存在"); Map<String,Object> row=rows.get(0);
        Api.require(number(row,"member_low")==viewer || number(row,"member_high")==viewer,403,"仅双方可以查看这段私信"); return row;
    }
    private Map<String,Object> view(Map<String,Object> row,long viewer) {
        boolean low=isLow(row,viewer);
        return Api.map("id",row.get("id"),"partnerId",row.get(low?"member_high":"member_low"),"partnerName",row.get(low?"high_name":"low_name"),
            "partnerAvatar",AvatarService.url((String)row.get(low?"high_avatar":"low_avatar")),"updatedAt",row.get("updated_at").toString());
    }
    /** 打开（或首次创建）与某位成员的会话；并发创建依赖唯一约束兜底。 */
    @Transactional public Object start(long viewer,Requests.DirectStart input) {
        long partner=input.memberId;
        Api.require(partner!=viewer,400,"不能给自己发私信");
        Api.require(jdbc.queryForObject("SELECT COUNT(*) FROM members WHERE id=?",Integer.class,partner)>0,404,"成员不存在");
        long low=Math.min(viewer,partner), high=Math.max(viewer,partner);
        List<Long> existing=jdbc.queryForList("SELECT id FROM direct_conversations WHERE member_low=? AND member_high=?",Long.class,low,high);
        if(existing.isEmpty()) {
            policy.writeAllowed(viewer);
            try { jdbc.update("INSERT INTO direct_conversations(member_low,member_high) VALUES(?,?)",low,high); }
            catch(DuplicateKeyException ignored) { /* 对方同时发起，沿用已建会话。 */ }
            existing=jdbc.queryForList("SELECT id FROM direct_conversations WHERE member_low=? AND member_high=?",Long.class,low,high);
        }
        return detail(existing.get(0),viewer);
    }
    public Object detail(long id,long viewer) { return view(raw(id,viewer),viewer); }
    /** 会话列表只显示已有消息的会话，附最后一条消息和未读数，最近 100 个。 */
    public Object list(long viewer) {
        List<Map<String,Object>> rows=jdbc.queryForList(COLUMNS+",(SELECT content FROM direct_messages m WHERE m.conversation_id=c.id ORDER BY m.id DESC LIMIT 1) last_content,"
            +"(SELECT sender_id FROM direct_messages m WHERE m.conversation_id=c.id ORDER BY m.id DESC LIMIT 1) last_sender,"
            +"(SELECT COUNT(*) FROM direct_messages m WHERE m.conversation_id=c.id AND m.sender_id<>? AND m.id>CASE WHEN c.member_low=? THEN c.low_read_id ELSE c.high_read_id END) unread"
            +FROM+"WHERE (c.member_low=? OR c.member_high=?) AND EXISTS (SELECT 1 FROM direct_messages m WHERE m.conversation_id=c.id) ORDER BY c.updated_at DESC,c.id DESC LIMIT 100",viewer,viewer,viewer,viewer);
        List<Object> items=new ArrayList<>();
        for(Map<String,Object> row:rows) {
            Map<String,Object> item=view(row,viewer);
            item.put("lastMessage",row.get("last_content")); item.put("lastMine",row.get("last_sender")!=null && number(row,"last_sender")==viewer);
            item.put("unread",number(row,"unread")); items.add(item);
        }
        return items;
    }
    /** 读取最新一页时把自己的已读位置推进到最新消息。 */
    public Object messages(long id,long viewer,long before) {
        Map<String,Object> row=raw(id,viewer);
        long low=number(row,"member_low");
        Map<Long,Object[]> senders=new HashMap<>();
        senders.put(low,new Object[]{row.get("low_name"),AvatarService.url((String)row.get("low_avatar"))});
        senders.put(number(row,"member_high"),new Object[]{row.get("high_name"),AvatarService.url((String)row.get("high_avatar"))});
        List<Map<String,Object>> result=jdbc.query("SELECT id,sender_id,content,created_at FROM direct_messages WHERE conversation_id=? AND id<? ORDER BY id DESC LIMIT 50",(rs,index)->{
            long sender=rs.getLong("sender_id"); Object[] who=senders.get(sender);
            return Api.map("id",rs.getLong("id"),"content",rs.getString("content"),"mine",sender==viewer,"senderName",who[0],"senderAvatar",who[1],
                "createdAt",rs.getTimestamp("created_at").toLocalDateTime().toString());
        },id,before<=0?Long.MAX_VALUE:before);
        if(before<=0 && !result.isEmpty()) markRead(id,low==viewer,((Number)result.get(0).get("id")).longValue());
        Collections.reverse(result); return result;
    }
    private void markRead(long id,boolean low,long messageId) {
        jdbc.update("UPDATE direct_conversations SET "+(low?"low_read_id":"high_read_id")+"=GREATEST("+(low?"low_read_id":"high_read_id")+",?) WHERE id=?",messageId,id);
    }
    @Transactional public Object send(long id,long viewer,Requests.Message input) {
        policy.writeAllowed(viewer);
        raw(id,viewer);
        jdbc.queryForObject("SELECT id FROM direct_conversations WHERE id=? FOR UPDATE",Long.class,id);
        jdbc.update("INSERT INTO direct_messages(conversation_id,sender_id,content) VALUES(?,?,?)",id,viewer,input.content.trim());
        jdbc.update("UPDATE direct_conversations SET updated_at=CURRENT_TIMESTAMP WHERE id=?",id);
        return messages(id,viewer,0);
    }
}
