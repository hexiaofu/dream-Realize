package cn.laotie.dream.service;

import cn.laotie.dream.common.Api;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.util.*;

/**
 * 评论资格问答：服务端计分，达到 90 分后按账号持久保存资格。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Service
public class AdmissionService {
    public static final int PASS_SCORE=90;
    public static final String VERSION="comment-2026-10-16.1";
    private final JdbcTemplate jdbc;
    private final List<Map<String,Object>> questions = new ArrayList<>();
    private final int[] correct = {1,2,0,1,2,0,1,2,0,1};
    private final String[] explanations = {
        "每个人都可以表达自己的梦想。愿意就帮助，不感兴趣可以划过。",
        "及时说明变化，尊重对方的时间和安排。",
        "梦想由每个人自己定义，是否帮助由双方自愿决定。",
        "我们提倡无偿，允许事先讲清条件的有偿帮助。",
        "匿名是一种表达选择，尊重对方主动分享的范围。",
        "是否接受帮助、是否继续交流，都由当事人自主决定。",
        "双方可以自主交换联系方式，并在线下开展帮助。",
        "感谢和传播善意是自愿的，帮助不产生未约定的回报义务。",
        "可以表达不同意见，但应就事论事，不辱骂、不捏造事实。",
        "面对可疑付款要求，应停止转账、核实情况并保留证据，不提供密码或验证码。"
    };
    public AdmissionService(JdbcTemplate jdbc) {
        this.jdbc=jdbc;
        add("一个人的梦想很普通，你会怎么做？", "嘲笑他的梦想", "尊重他的表达，自愿选择是否帮助", "要求他换一个更高尚的梦想");
        add("答应帮助后，发现自己无法兑现，你会？", "直接消失", "继续承诺，之后再说", "尽早说明情况，与对方沟通调整");
        add("有人发布了一个你无法理解的愿望，你认为？", "可以坦率表达，帮助由他人自愿决定", "应该羞辱他", "所有人都必须帮助他");
        add("关于有偿帮助，哪项符合社区共识？", "先说免费，结束后收费", "提前说清条件，双方自主协商", "隐瞒费用让对方先接受");
        add("遇到匿名发布者，你会？", "追查并公开他的身份", "要求他公开所有经历", "尊重匿名，自愿交流");
        add("你提供帮助，对方拒绝了，你会？", "尊重他的选择", "持续要求他接受", "公开指责他不知感恩");
        add("关于线下圆梦，你认为？", "必须把所有聊天记录上传", "双方可以自主约定沟通和见面方式", "平台必须替双方决定见面安排");
        add("自己的梦想获得帮助后，你会？", "认为从此欠对方任何要求", "要求所有受助者强制回报", "自愿感谢，也可以把善意传递给别人");
        add("不同意别人的梦想时，你会如何评论？", "说明理由，就事论事并尊重对方", "辱骂对方，让他不敢再发言", "编造对方的经历来反驳");
        add("对方要求先交解冻费才提供帮助，你会？", "立即转账，避免错过机会", "停止转账，核实情况并保留证据", "把支付密码和验证码告诉对方");
    }
    private void add(String title, String... options) {
        questions.add(Api.map("id", String.valueOf(questions.size()+1), "title", title, "options", Arrays.asList(options)));
    }
    public Object questions() { return Api.map("questions",questions,"passScore",PASS_SCORE,"total",questions.size(),"pointsPerQuestion",10,"version",VERSION); }
    public Map<String,Object> status(long memberId) {
        List<Map<String,Object>> rows=jdbc.query("SELECT score,exam_version,passed_at FROM comment_qualifications WHERE member_id=? AND score>=?",(rs,n)->Api.map(
            "passed",true,"score",rs.getInt("score"),"passScore",PASS_SCORE,"passedVersion",rs.getString("exam_version"),"passedAt",rs.getTimestamp("passed_at").toLocalDateTime().toString()),memberId,PASS_SCORE);
        return rows.isEmpty()?Api.map("passed",false,"score",0,"passScore",PASS_SCORE):rows.get(0);
    }
    public void requirePassed(long memberId) { Api.require(Boolean.TRUE.equals(status(memberId).get("passed")),403,"请先完成评论问答并达到 90 分"); }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Object check(long memberId,String version,Map<String,Integer> answers) {
        Api.require(VERSION.equals(version),412,"题目已更新，请重新答题");
        Api.require(answers!=null && answers.size()==correct.length,400,"请完成全部 10 道题目");
        // 与评论写入共用成员行锁，重复提交不会重复发放或撤销已有资格。
        jdbc.queryForObject("SELECT id FROM members WHERE id=? FOR UPDATE",Long.class,memberId);
        List<Object> feedback = new ArrayList<>(); int right=0;
        for (int i=0;i<correct.length;i++) {
            Integer selected=answers.get(String.valueOf(i+1));
            Api.require(selected != null && selected>=0 && selected<=2,400,"请完成全部题目");
            boolean valid=selected==correct[i]; if(valid) right++;
            feedback.add(Api.map("id",String.valueOf(i+1),"correct",valid,"explanation",explanations[i]));
        }
        int score=right*10; boolean passed=score>=PASS_SCORE;
        if(passed && !Boolean.TRUE.equals(status(memberId).get("passed")))
            jdbc.update("INSERT INTO comment_qualifications(member_id,exam_version,score) VALUES(?,?,?)",memberId,VERSION,score);
        return Api.map("passed",passed,"score",score,"passScore",PASS_SCORE,"feedback",feedback,"qualification",status(memberId));
    }
}
