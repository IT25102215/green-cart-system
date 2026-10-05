package com.greencart.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import java.nio.file.Paths;
@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Value("${app.upload.dir}") private String uploadDir;
  @Override public void addResourceHandlers(ResourceHandlerRegistry r){
    String location = "file:" + Paths.get(uploadDir).toAbsolutePath().toString() + "/";
    r.addResourceHandler("/uploads/**").addResourceLocations(location);
  }
}
