package cn.laotie.dream.service;

import cn.laotie.dream.common.Api;
import cn.laotie.dream.security.Sessions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.util.*;

/**
 * 社区入门考试，答案仅在服务端判定；通过凭证限时且仅能使用一次。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class AdmissionService {
    private final JdbcTemplate jdbc;
    private final Sessions sessions;
    private final List<Map<String,Object>> questions = new ArrayList<>();
    private final int[] correct = {1,2,0,1,2,0,1,2};
    private final String[] explanations = {
        "每个人都可以表达自己的梦想。愿意就帮助，不感兴趣可以划过。",
        "及时说明变化，尊重对方的时间和安排。",
        "梦想由每个人自己定义，是否帮助由双方自愿决定。",
        "我们提倡无偿，允许事先讲清条件的有偿帮助。",
        "匿名是一种表达选择，尊重对方主动分享的范围。",
        "是否接受帮助、是否继续交流，都由当事人自主决定。",
        "双方可以自主交换联系方式，并在线下开展帮助。",
        "感谢和传播善意是自愿的，帮助不产生未约定的回报义务。"
    };
    public AdmissionService(JdbcTemplate jdbc, Sessions sessions) {
        this.jdbc=jdbc; this.sessions=sessions;
        add("一个人的梦想很普通，你会怎么做？", "嘲笑他的梦想", "尊重他的表达，自愿选择是否帮助", "要求他换一个更高尚的梦想");
        add("答应帮助后，发现自己无法兑现，你会？", "直接消失", "继续承诺，之后再说", "尽早说明情况，与对方沟通调整");
        add("有人发布了一个你无法理解的愿望，你认为？", "可以坦率表达，帮助由他人自愿决定", "应该羞辱他", "所有人都必须帮助他");
        add("关于有偿帮助，哪项符合社区共识？", "先说免费，结束后收费", "提前说清条件，双方自主协商", "隐瞒费用让对方先接受");
        add("遇到匿名发布者，你会？", "追查并公开他的身份", "要求他公开所有经历", "尊重匿名，自愿交流");
        add("你提供帮助，对方拒绝了，你会？", "尊重他的选择", "持续要求他接受", "公开指责他不知感恩");
        add("关于线下圆梦，你认为？", "必须把所有聊天记录上传", "双方可以自主约定沟通和见面方式", "平台必须替双方决定见面安排");
        add("自己的梦想获得帮助后，你会？", "认为从此欠对方任何要求", "要求所有受助者强制回报", "自愿感谢，也可以把善意传递给别人");
    }
    private void add(String title, String... options) {
        questions.add(Api.map("id", String.valueOf(questions.size()+1), "title", title, "options", Arrays.asList(options)));
    }
    public Object questions() { return Api.map("questions", questions, "passScore", 100, "total", questions.size()); }
    public Object check(Map<String,Integer> answers) {
        List<Object> feedback = new ArrayList<>(); int right=0;
        for (int i=0;i<correct.length;i++) {
            Integer selected=answers.get(String.valueOf(i+1));
            Api.require(selected != null && selected>=0 && selected<=2,400,"请完成全部题目");
            boolean valid=selected==correct[i]; if(valid) right++;
            feedback.add(Api.map("id",String.valueOf(i+1),"correct",valid,"explanation",explanations[i]));
        }
        String token=null;
        if (right==correct.length) {
            token=sessions.randomToken();
            jdbc.update("INSERT INTO admission_passes(token_hash,expires_at) VALUES(?,?)", sessions.hash(token),new Timestamp(System.currentTimeMillis()+30*60*1000));
        }
        return Api.map("passed", right==correct.length, "score", right*100/correct.length, "feedback",feedback,"admissionToken",token);
    }
}
