package cn.laotie.dream.security;

import cn.laotie.dream.common.Api;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * 用户限制和内容可见性的统一服务端规则。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class ModerationPolicy {
    public static final String VISIBLE_DREAM="NOT EXISTS (SELECT 1 FROM dream_moderation dm WHERE dm.dream_id=d.id AND dm.hidden=TRUE)";
    public static final String VISIBLE_UPDATE="NOT EXISTS (SELECT 1 FROM update_moderation um WHERE um.update_id=u.id AND um.hidden=TRUE)";
    public static final String VISIBLE_COMMENT="c.deleted=FALSE AND NOT EXISTS (SELECT 1 FROM comment_moderation cm WHERE cm.comment_id=c.id AND cm.hidden=TRUE)";
    // f 为固定别名；任一引用被审核下架都会阻断文件，普通删除转发不影响来源图片。
    public static final String VISIBLE_COMMENT_MEDIA="NOT EXISTS (SELECT 1 FROM comment_attachments attachment JOIN dream_comments c ON c.id=attachment.comment_id JOIN dreams d ON d.id=c.dream_id WHERE attachment.media_id=f.id AND ((attachment.is_source=TRUE AND c.deleted=TRUE) OR EXISTS (SELECT 1 FROM comment_moderation cm WHERE cm.comment_id=c.id AND cm.hidden=TRUE) OR NOT ("+VISIBLE_DREAM+")))";
    private final JdbcTemplate jdbc;
    public ModerationPolicy(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public Map<String,Object> restriction(long id) {
        List<Map<String,Object>> rows=jdbc.query("SELECT mode,reason,expires_at,revision,(expires_at IS NOT NULL AND expires_at<=CURRENT_TIMESTAMP) expired FROM member_restrictions WHERE member_id=?",
            (rs,n)->rs.getBoolean("expired")?Api.map("mode","NORMAL","revision",rs.getInt("revision")):Api.map("mode",rs.getString(1),"reason",rs.getString(2),"expiresAt",rs.getTimestamp(3)==null?null:rs.getTimestamp(3).toLocalDateTime().toString(),"revision",rs.getInt("revision")),id);
        return rows.isEmpty()?Api.map("mode","NORMAL","revision",0):rows.get(0);
    }
    public void loginAllowed(long id,int code) {
        Map<String,Object> restriction=restriction(id);
        Api.require(!"BLOCKED".equals(restriction.get("mode")),code,"账号已封禁："+restriction.get("reason"));
    }
    // 写操作在事务内先锁成员行；管理员更新限制也使用该锁，避免与发布交错。
    public void writeAllowed(long id) {
        jdbc.queryForObject("SELECT id FROM members WHERE id=? FOR UPDATE",Long.class,id);
        Map<String,Object> restriction=restriction(id);
        Api.require("NORMAL".equals(restriction.get("mode")),403,"账号已限制发布："+restriction.get("reason"));
    }
    public boolean hiddenDream(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dream_moderation WHERE dream_id=? AND hidden=TRUE",Integer.class,id)>0;
    }
    public void dreamAvailable(long id) { Api.require(!hiddenDream(id),404,"内容已下架"); }
    public boolean hiddenMedia(Long dreamId,Long updateId) {
        return dreamId!=null && (hiddenDream(dreamId) || (updateId!=null && jdbc.queryForObject("SELECT COUNT(*) FROM update_moderation WHERE update_id=? AND hidden=TRUE",Integer.class,updateId)>0));
    }
    public boolean hiddenCommentMedia(String id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM media_files f WHERE f.id=? AND NOT ("+VISIBLE_COMMENT_MEDIA+")",Integer.class,id)>0;
    }
}
