package cn.laotie.dream.service;

import cn.laotie.dream.api.Requests;
import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.ModerationPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * 梦想检索、发布和进展管理。匿名信息在服务端投影时移除。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class DreamService {
    private final JdbcTemplate jdbc;
    private final MediaService media;
    private final ModerationPolicy policy;
    private final InteractionService interactions;
    private static final String SELECT = "SELECT d.*,m.nickname,"+AvatarService.filenameSql("m")+" avatar_filename,COALESCE(dm.hidden,FALSE) hidden,dm.reason moderation_reason,(SELECT COUNT(*) FROM help_requests h WHERE h.dream_id=d.id AND h.status='ACCEPTED') helper_count FROM dreams d JOIN members m ON m.id=d.member_id LEFT JOIN dream_moderation dm ON dm.dream_id=d.id ";
    public DreamService(JdbcTemplate jdbc,MediaService media,ModerationPolicy policy,InteractionService interactions) { this.jdbc=jdbc; this.media=media; this.policy=policy; this.interactions=interactions; }
    public Object list(String keyword,String category,String helpType,String status,int page,int size,Long viewer,boolean mine) {
        Api.require(page>=1 && page<=100000 && size>=1 && size<=30,400,"分页参数不正确");
        Api.require(keyword==null || keyword.length()<=100,400,"搜索词最多 100 字");
        StringBuilder where=new StringBuilder(" WHERE 1=1 "); List<Object> args=new ArrayList<>();
        if(mine) { Api.require(viewer!=null,401,"请先登录"); where.append(" AND d.member_id=? "); args.add(viewer); }
        else where.append(" AND ").append(ModerationPolicy.VISIBLE_DREAM);
        if(keyword!=null && !keyword.trim().isEmpty()) {
            where.append(" AND (d.title LIKE ? OR d.story LIKE ?) ");
            String q="%"+keyword.trim()+"%"; args.add(q); args.add(q);
        }
        if(category!=null && !category.isEmpty()) { where.append(" AND d.category=? "); args.add(category); }
        if(helpType!=null && !helpType.isEmpty()) { where.append(" AND d.help_type=? "); args.add(helpType); }
        if(status!=null && !status.isEmpty()) { where.append(" AND d.status=? "); args.add(status); }
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM dreams d"+where,Long.class,args.toArray());
        args.add(size); args.add((page-1)*size);
        List<Map<String,Object>> items=jdbc.query(SELECT+where+" ORDER BY d.created_at DESC,d.id DESC LIMIT ? OFFSET ?",(rs,row)->view(rs,viewer),args.toArray());
        media.addTo(items,false);
        interactions.addTo(items,viewer);
        for(Map<String,Object> item:items) if(Boolean.TRUE.equals(item.get("hidden"))) { item.put("story",""); item.put("media",Collections.emptyList()); }
        return Api.map("items",items,"total",total,"page",page,"size",size,"hasMore",(long)page*size<total);
    }
    private Map<String,Object> view(ResultSet rs,Long viewer) throws SQLException {
        boolean anonymous=rs.getBoolean("anonymous"); long owner=rs.getLong("member_id");
        Map<String,Object> value=Api.map("id",rs.getLong("id"),"title",rs.getString("title"),"story",rs.getString("story"),
            "category",rs.getString("category"),"helpType",rs.getString("help_type"),"rewardType",rs.getString("reward_type"),
            "contactMode",rs.getString("contact_mode"),"city",rs.getString("city"),"anonymous",anonymous,"authorName",anonymous?"匿名梦想家":rs.getString("nickname"),
            "authorAvatar",anonymous?"":AvatarService.url(rs.getString("avatar_filename")),
            "targetAmount",rs.getBigDecimal("target_amount"),"receivedAmount",rs.getBigDecimal("received_amount"),"status",rs.getString("status"),
            "helperCount",rs.getLong("helper_count"),"createdAt",rs.getTimestamp("created_at").toLocalDateTime().toString(),"updatedAt",rs.getTimestamp("updated_at").toLocalDateTime().toString(),"isOwner",viewer!=null && viewer==owner);
        if(!anonymous) value.put("authorId",owner);
        value.put("hidden",rs.getBoolean("hidden"));
        if(rs.getBoolean("hidden") && viewer!=null && viewer==owner) value.put("moderationReason",rs.getString("moderation_reason"));
        return value;
    }
    public Map<String,Object> detail(long id,Long viewer) {
        policy.dreamAvailable(id);
        List<Map<String,Object>> rows=jdbc.query(SELECT+" WHERE d.id=?",(rs,row)->view(rs,viewer),id);
        Api.require(!rows.isEmpty(),404,"这个梦想不存在");
        Map<String,Object> result=rows.get(0);
        media.addTo(rows,false);
        interactions.addTo(rows,viewer);
        List<Map<String,Object>> updates=jdbc.query("SELECT u.id,u.content,u.created_at FROM dream_updates u WHERE u.dream_id=? AND "+ModerationPolicy.VISIBLE_UPDATE+" ORDER BY u.id DESC",(rs,row)->Api.map("id",rs.getLong("id"),"content",rs.getString("content"),"createdAt",rs.getTimestamp("created_at").toLocalDateTime().toString()),id);
        media.addTo(updates,true);
        result.put("updates",updates);
        if(viewer!=null) {
            List<Long> requests=jdbc.query("SELECT id FROM help_requests WHERE dream_id=? AND helper_id=?",(rs,row)->rs.getLong(1),id,viewer);
            if(!requests.isEmpty()) result.put("myHelpId",requests.get(0));
        }
        return result;
    }
    @Transactional public Object create(long memberId,Requests.Dream input) {
        policy.writeAllowed(memberId);
        Api.require(input.targetAmount==null || "CASH".equals(input.helpType),400,"只有现金心愿可以设置目标金额");
        GeneratedKeyHolder keys=new GeneratedKeyHolder();
        jdbc.update(connection->{
            PreparedStatement ps=connection.prepareStatement("INSERT INTO dreams(member_id,title,story,category,help_type,reward_type,contact_mode,city,anonymous,target_amount) VALUES(?,?,?,?,?,?,?,?,?,?)",new String[]{"id"});
            ps.setLong(1,memberId); ps.setString(2,input.title.trim()); ps.setString(3,MemberService.clean(input.story)); ps.setString(4,input.category);
            ps.setString(5,input.helpType); ps.setString(6,input.rewardType); ps.setString(7,input.contactMode); ps.setString(8,MemberService.clean(input.city)); ps.setBoolean(9,input.anonymous); ps.setBigDecimal(10,input.targetAmount); return ps;
        },keys);
        long id=Objects.requireNonNull(keys.getKey()).longValue();
        media.attach(input.mediaIds,memberId,id,null);
        return detail(id,memberId);
    }
    public Map<String,Object> raw(long id,boolean lock) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM dreams WHERE id=?"+(lock?" FOR UPDATE":""),id);
        Api.require(!rows.isEmpty(),404,"这个梦想不存在"); policy.dreamAvailable(id); return rows.get(0);
    }
    @Transactional public Object progress(long id,long memberId,Requests.Progress input) {
        policy.writeAllowed(memberId);
        Map<String,Object> dream=raw(id,true);
        Api.require(((Number)dream.get("member_id")).longValue()==memberId,403,"只有发布者可以更新梦想");
        Api.require(input.receivedAmount==null || "CASH".equals(dream.get("help_type")),400,"只有现金心愿可以更新金额");
        GeneratedKeyHolder keys=new GeneratedKeyHolder();
        jdbc.update(connection->{
            PreparedStatement ps=connection.prepareStatement("INSERT INTO dream_updates(dream_id,content) VALUES(?,?)",new String[]{"id"});
            ps.setLong(1,id); ps.setString(2,input.content.trim()); return ps;
        },keys);
        media.attach(input.mediaIds,memberId,id,Objects.requireNonNull(keys.getKey()).longValue());
        jdbc.update("UPDATE dreams SET status=COALESCE(?,status),received_amount=COALESCE(?,received_amount),updated_at=CURRENT_TIMESTAMP WHERE id=?",input.status,input.receivedAmount,id);
        return detail(id,memberId);
    }
    public Object stats() {
        return Api.map("dreams",jdbc.queryForObject("SELECT COUNT(*) FROM dreams d WHERE "+ModerationPolicy.VISIBLE_DREAM,Long.class),
            "completed",jdbc.queryForObject("SELECT COUNT(*) FROM dreams d WHERE status='COMPLETED' AND "+ModerationPolicy.VISIBLE_DREAM,Long.class),
            "connections",jdbc.queryForObject("SELECT COUNT(*) FROM help_requests h JOIN dreams d ON d.id=h.dream_id WHERE h.status='ACCEPTED' AND "+ModerationPolicy.VISIBLE_DREAM,Long.class));
    }
    public Object publicProfile(long id,Long viewer) {
        List<Map<String,Object>> members=jdbc.query("SELECT nickname,bio,skills,city,"+AvatarService.filenameSql("m")+" avatar_filename FROM members m WHERE id=?",(rs,row)->Api.map("id",id,"nickname",rs.getString(1),"bio",rs.getString(2),"skills",rs.getString(3),"city",rs.getString(4),"avatarUrl",AvatarService.url(rs.getString("avatar_filename"))),id);
        Api.require(!members.isEmpty(),404,"成员不存在");
        Map<String,Object> profile=members.get(0);
        List<Map<String,Object>> dreams=jdbc.query(SELECT+" WHERE d.member_id=? AND d.anonymous=FALSE AND "+ModerationPolicy.VISIBLE_DREAM+" ORDER BY d.id DESC LIMIT 30",(rs,row)->view(rs,viewer),id);
        media.addTo(dreams,false);
        interactions.addTo(dreams,viewer);
        profile.put("dreams",dreams);
        return profile;
    }
}
