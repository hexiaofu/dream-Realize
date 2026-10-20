package cn.laotie.dream.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 前后端分离跨域设置，可通过环境变量指定实际前端域名。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.cors-origins}") private String origins;
    @Override public void addCorsMappings(CorsRegistry registry) {
        // 使用来源模式，本地开发可写 http://localhost:[*] 适配 HBuilderX 等工具自动分配的端口；精确域名照常生效。
        registry.addMapping("/api/**").allowedOriginPatterns(origins.split(","))
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type", "Range", "X-Admin-Request").allowCredentials(true)
            .exposedHeaders("Accept-Ranges", "Content-Range", "Content-Length").maxAge(3600);
    }
}
