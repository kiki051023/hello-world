package com.example.student.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationConfigurationTest {

    @Test
    void mybatisPlusPropertiesAreAtTheRootConfigurationNamespace() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));
        Properties properties = factory.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("mybatis-plus.mapper-locations"))
                .isEqualTo("classpath*:/mapper/**/*.xml");
        assertThat(properties.getProperty("spring.mybatis-plus.mapper-locations"))
                .isNull();
    }
}
