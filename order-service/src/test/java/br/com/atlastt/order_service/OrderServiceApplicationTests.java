package br.com.atlastt.order_service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Precisa de infra real (Postgres/RabbitMQ/Eureka). Fora do `mvn test`; veja docs/TESTES.md.
@Tag("integration")
@SpringBootTest
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
