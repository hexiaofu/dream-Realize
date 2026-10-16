package cn.laotie.dream.config;

import cn.laotie.dream.api.Requests;
import cn.laotie.dream.service.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 显式开启 demo profile 才会导入的示例数据，正式数据库默认没有演示账户。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Component
@Order(0)
@ConditionalOnProperty(name="app.demo-data",havingValue="true")
public class DemoData implements ApplicationRunner {
    private final JdbcTemplate jdbc; private final MemberService members; private final DreamService dreams;
    public DemoData(JdbcTemplate jdbc,MemberService members,DreamService dreams) { this.jdbc=jdbc; this.members=members; this.dreams=dreams; }
    @Override public void run(ApplicationArguments args) {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM members",Integer.class)>0) return;
        long a=members.insertMember("dreamer","Dream12345!","山间来信");
        long b=members.insertMember("helper","Dream12345!","小满");
        Requests.Profile p=new Requests.Profile(); p.nickname="小满"; p.bio="相信小小的善意，也能让生活发生变化。"; p.skills="摄影、简历修改、英语口语"; p.city="西宁"; members.update(b,p);
        add(a,"想带妈妈看一次真正的大海","妈妈总说电视里的海真漂亮。今年想陪她去青岛，想请去过的朋友分享适合长辈的路线。","FAMILY","EXPERIENCE",false,"西宁",null);
        add(b,"给社区的流浪猫做一个过冬的小屋","已经准备好了木板和图纸，想找一位会木工的朋友，一起把这个小屋搭起来。","LIFE","SKILL",false,"西宁",null);
        add(a,"想给自己的第一部短片配上声音","故事已经拍好了，还想找一位喜欢配音的伙伴，一起让这个关于家乡的小故事变得完整。","CREATION","SKILL",true,"",null);
        add(b,"和一个人一起，把英语重新捡起来","每天晚上 20 分钟，从聊今天发生的小事开始。希望找到愿意相互陪伴和鼓励的伙伴。","GROWTH","COMPANY",false,"线上",null);
        long done=add(a,"第一次站在舞台上，唱一首自己的歌","从写下第一句歌词，到第一次公开演唱，终于迈出了这一步。谢谢陪我排练的朋友。","CREATION","RESOURCE",false,"西宁",null);
        Requests.Progress update=new Requests.Progress(); update.content="昨晚完成了第一次演出。原来梦想真的可以从一个很小的行动开始，谢谢大家！"; update.status="COMPLETED"; dreams.progress(done,a,update);
    }
    @SuppressWarnings("unchecked") private long add(long owner,String title,String story,String category,String help,boolean anonymous,String city,BigDecimal amount) {
        Requests.Dream d=new Requests.Dream(); d.title=title; d.story=story; d.category=category; d.helpType=help; d.anonymous=anonymous; d.city=city; d.targetAmount=amount;
        return ((Number)((Map<String,Object>)dreams.create(owner,d)).get("id")).longValue();
    }
}
