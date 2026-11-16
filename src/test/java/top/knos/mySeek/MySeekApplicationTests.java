package top.knos.mySeek;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 上下文加载测试：需要本地可用的 MariaDB 与 Redis。
 * 默认禁用，避免在无依赖环境下阻塞构建；准备好依赖后可移除 @Disabled 执行。
 */
@SpringBootTest
@Disabled("需要 MariaDB + Redis 环境")
class MySeekApplicationTests {

	@Test
	void contextLoads() {
	}

}
