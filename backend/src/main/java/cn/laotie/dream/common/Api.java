package cn.laotie.dream.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 接口响应与有序视图工具；业务层只返回明确挑选的字段。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
public final class Api {
    private Api() { }
    public static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }
    public static Map<String, Object> ok(Object data) { return map("code", 0, "message", "ok", "data", data); }
    public static void require(boolean condition, int status, String message) {
        if (!condition) throw new ApiException(status, message);
    }
}
