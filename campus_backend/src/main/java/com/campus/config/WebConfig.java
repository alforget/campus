package com.campus.config;

import com.campus.interceptors.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Component
public class WebConfig implements WebMvcConfigurer {
    //添加拦截器
    @Autowired
    private LoginInterceptor loginInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor).excludePathPatterns(
                "/user/login","/user/register","/upload/**", "/swagger-ui/**",      // Swagger UI 静态资源
                "/v3/api-docs/**",     // Swagger 的 JSON 数据
                "/actuator/**", // Spring Boot 监控端点
                "/generate-2000-tokens") ;
    }
    //图片
    // 本地真实存放路径
    @Value("${real.path.avatar}")
    private String realPathAvatar;
    // 浏览器访问前缀（自定义）
    @Value("${web.prefix.avatar}")
    private String webPrefixAvatar;
    //logo
    // 本地真实存放路径
    @Value("${real.path.logo}")
    private String realPathLogo;
    // 浏览器访问前缀（自定义）
    @Value("${web.prefix.logo}")
    private String webPrefixLogo;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 含义：访问 /uploadAvatar/** 就映射到本地 E:/xxx/fileStore/avatar/
        registry.addResourceHandler(webPrefixAvatar + "**")
                .addResourceLocations("file:" + realPathAvatar);
        // 含义：访问 /uploadLogo/** 就映射到本地 E:/xxx/fileStore/logo/
        registry.addResourceHandler(webPrefixLogo + "**")
                .addResourceLocations("file:" + realPathLogo);
    }

}
