package cn.laotie.dream.service;

import cn.laotie.dream.common.Api;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/**
 * 头像使用成员本人上传且尚未发布的图片；由个人资料事务统一保存。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class AvatarService {
    private final JdbcTemplate jdbc;
    public AvatarService(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    // memberAlias 仅来自服务端固定的 SQL 别名，不接受请求参数。
    public static String filenameSql(String memberAlias) {
        return "(SELECT f.file_name FROM member_avatars a JOIN media_files f ON f.id=a.media_id WHERE a.member_id="+memberAlias+".id)";
    }
    public static String url(String filename) { return filename==null?"":"/api/media/"+filename; }

    public void update(long memberId,String mediaId) {
        // 兼容旧客户端：不传字段保留头像，空字符串恢复默认。
        if(mediaId==null) return;
        if(!mediaId.isEmpty()) {
            List<Map<String,Object>> files=jdbc.queryForList("SELECT member_id,kind,dream_id FROM media_files WHERE id=? FOR UPDATE",mediaId);
            Api.require(!files.isEmpty(),400,"头像不存在，请重新上传");
            Map<String,Object> file=files.get(0);
            Api.require(((Number)file.get("member_id")).longValue()==memberId,403,"只能使用自己上传的头像");
            Api.require("IMAGE".equals(file.get("kind")),400,"头像请选择图片");
            Api.require(file.get("dream_id")==null,409,"该图片已用于发布内容，请重新上传头像");
        }
        jdbc.update("DELETE FROM member_avatars WHERE member_id=?",memberId);
        if(!mediaId.isEmpty()) jdbc.update("INSERT INTO member_avatars(member_id,media_id) VALUES(?,?)",memberId,mediaId);
    }
}
