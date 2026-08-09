package dev.handsup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;

// spring-data-elasticsearch 5.4.2가 core 모듈 테스트 클래스패스에서 spring-data-commons 3.2.0과
// 묶여 NoClassDefFoundError(CachingValueExpressionEvaluatorFactory)를 유발하므로 테스트 컨텍스트에서만 제외한다.
@SpringBootApplication(exclude = ElasticsearchDataAutoConfiguration.class)
public class CoreTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreTestApplication.class, args);
    }
}