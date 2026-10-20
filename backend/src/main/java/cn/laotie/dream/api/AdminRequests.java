package cn.laotie.dream.api;

import javax.validation.constraints.*;

/**
 * 后台处理入参，不接收客户端传入的管理员身份。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
public final class AdminRequests {
    private AdminRequests() { }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Content {
        @NotNull public Boolean hidden;
        @NotNull public Boolean expectedHidden;
        @NotBlank(message="请填写处理原因") @Size(max=500,message="处理原因最多 500 字") public String reason;
    }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Feedback {
        @NotBlank @Pattern(regexp="NEW|PROCESSING|RESOLVED",message="处理状态不正确") public String status;
        @NotBlank @Pattern(regexp="NEW|PROCESSING|RESOLVED") public String expectedStatus;
        @Size(max=500,message="回复最多 500 字") public String reply;
    }
    /** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
    public static class Restriction {
        @NotBlank @Pattern(regexp="NORMAL|READ_ONLY|BLOCKED") public String mode;
        @Min(1) @Max(8760) public Integer durationHours;
        @NotNull @Min(0) public Integer expectedRevision;
        @NotBlank(message="请填写处理原因") @Size(max=500,message="处理原因最多 500 字") public String reason;
    }
}
