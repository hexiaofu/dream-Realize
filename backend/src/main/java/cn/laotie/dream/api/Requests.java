package cn.laotie.dream.api;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 入参白名单及校验；内部持久化字段不会被请求体覆盖。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
public final class Requests {
    private Requests() { }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Exam {
        @NotNull(message="请完成所有题目") @Size(min=10, max=10, message="请完成全部 10 道题") public Map<String, Integer> answers;
        @NotBlank(message="请重新加载题目") @Size(max=40) public String examVersion;
    }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Register {
        @NotBlank(message="请输入账号") @Pattern(regexp="[a-zA-Z0-9_]{4,32}", message="账号应为 4–32 位字母、数字或下划线") public String username;
        @NotBlank(message="请输入密码") @Size(min=8,max=64,message="密码长度为 8–64 位") public String password;
        @NotBlank(message="请输入昵称") @Size(max=30,message="昵称最多 30 字") public String nickname;
        @NotNull(message="请阅读并同意免责声明与线下交流风险告知") @AssertTrue(message="请阅读并同意免责声明与线下交流风险告知") public Boolean noticeAccepted;
        @NotBlank(message="请重新加载注册声明") @Size(max=40) public String noticeVersion;
        @NotBlank(message="请重新加载注册声明") @Pattern(regexp="[a-f0-9]{64}",message="请重新加载注册声明") public String noticeSha256;
    }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Login {
        @NotBlank(message="请输入账号") @Size(max=40) public String username;
        @NotBlank(message="请输入密码") @Size(max=64) public String password;
    }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Profile {
        @Pattern(regexp="|[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}",message="头像编号无效") public String avatarId;
        @NotBlank(message="请输入昵称") @Size(max=30,message="昵称最多 30 字") public String nickname;
        @Size(max=500,message="自我介绍最多 500 字") public String bio = "";
        @Size(max=200,message="技能描述最多 200 字") public String skills = "";
        @Size(max=60,message="城市名称过长") public String city = "";
    }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Dream {
        @NotNull @Size(max=10,message="每次最多 9 张图片和 1 个视频")
        public List<@NotNull @Pattern(regexp="[a-f0-9-]{36}",message="附件编号无效") String> mediaIds = new ArrayList<>();
        public List<String> getMediaIds() { return mediaIds; }
        @NotBlank(message="请写下你的梦想") @Size(max=100,message="标题最多 100 字") public String title;
        @Size(max=5000,message="故事最多 5000 字") public String story = "";
        @NotBlank @Pattern(regexp="LIFE|GROWTH|FAMILY|CREATION|CAREER|OTHER",message="请选择梦想分类") public String category;
        @NotBlank @Pattern(regexp="CASH|GOODS|SKILL|EXPERIENCE|COMPANY|RESOURCE|OTHER",message="请选择需要的帮助") public String helpType;
        @NotBlank @Pattern(regexp="FREE|NEGOTIABLE",message="请选择帮助条件") public String rewardType = "FREE";
        @NotBlank @Pattern(regexp="ONLINE|OFFLINE|BOTH",message="请选择交流方式") public String contactMode = "BOTH";
        @Size(max=60,message="城市名称过长") public String city = "";
        public boolean anonymous;
        @DecimalMin(value="0.01",message="目标金额应大于零") @DecimalMax(value="999999999999.99",message="目标金额过大") @Digits(integer=12,fraction=2,message="金额最多两位小数") public BigDecimal targetAmount;
    }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Progress {
        @NotNull @Size(max=10,message="每次最多 9 张图片和 1 个视频")
        public List<@NotNull @Pattern(regexp="[a-f0-9-]{36}",message="附件编号无效") String> mediaIds = new ArrayList<>();
        public List<String> getMediaIds() { return mediaIds; }
        @NotBlank(message="请写下进展") @Size(max=2000,message="进展最多 2000 字") public String content;
        @Pattern(regexp="OPEN|IN_PROGRESS|COMPLETED|PAUSED",message="状态不正确") public String status;
        @DecimalMin(value="0",message="金额不能为负数") @DecimalMax(value="999999999999.99",message="金额过大") @Digits(integer=12,fraction=2,message="金额最多两位小数") public BigDecimal receivedAmount;
    }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Help {
        @NotBlank(message="请说明你能提供什么帮助") @Size(max=1000,message="帮助说明最多 1000 字") public String offer;
        @NotBlank @Pattern(regexp="FREE|NEGOTIABLE",message="请选择帮助条件") public String rewardType = "FREE";
    }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Decision {
        @NotBlank @Pattern(regexp="ACCEPTED|DECLINED|WITHDRAWN",message="操作不正确") public String status;
    }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Feedback {
        @NotBlank(message="请选择反馈类型") @Pattern(regexp="SUGGESTION|BUG|EXPERIENCE|OTHER",message="反馈类型不正确") public String category;
        @NotBlank(message="请填写反馈内容") @Size(max=1000,message="反馈内容最多 1000 字") public String content;
        @Size(max=100,message="联系方式最多 100 字") public String contact;
    }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class DirectStart {
        @NotNull(message="请选择私信对象") @Positive(message="成员编号无效") public Long memberId;
    }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Message {
        @NotBlank(message="请输入消息") @Size(max=2000,message="消息最多 2000 字") public String content;
    }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Reaction {
        @NotBlank @Pattern(regexp="SUPPORT|DISAGREE|NONE",message="请选择精神支持或不同意") public String reaction;
    }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Comment {
        @NotNull @Size(max=2000,message="评论最多 1000 字") public String content = "";
        @NotNull @Size(max=9,message="评论最多 9 张图片")
        public List<@NotNull @Pattern(regexp="[a-f0-9-]{36}",message="附件编号无效") String> mediaIds = new ArrayList<>();
        public List<String> getMediaIds() { return mediaIds; }
        @AssertTrue(message="请输入评论或选择图片") public boolean isNotEmpty() {
            return content!=null && (!content.trim().isEmpty() || (mediaIds!=null && !mediaIds.isEmpty()));
        }
        @AssertTrue(message="评论最多 1000 字") public boolean isWithinLimit() {
            return content==null || content.codePointCount(0,content.length())<=1000;
        }
        public boolean anonymous;
    }
}
