plugins {
	java
	id("org.springframework.boot") version "4.0.9"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "top.knos"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	maven { url = uri("https://maven.aliyun.com/nexus/content/groups/public/") }
	maven { url = uri("https://maven.aliyun.com/repository/spring/") }
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
}

// 统一由 BOM 管理版本号，避免各依赖版本漂移
extra["springAiVersion"] = "2.0.2"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	// 数据库结构变更统一由 Flyway 管理（生产 ddl-auto=validate）
	implementation("org.flywaydb:flyway-core")
	runtimeOnly("org.flywaydb:flyway-mysql")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.ai:spring-ai-starter-model-ollama")

	// Spring AI OpenAI 支持（用于豆包和千问）
	implementation("org.springframework.ai:spring-ai-starter-model-openai")
	// Spring AI 2.0 的 OpenAI 集成基于官方 openai-java SDK，需要显式引入 OkHttp 客户端实现
	implementation("com.openai:openai-java-client-okhttp:4.80.0")

	// Spring AI Vector Store - 使用 Redis 作为向量数据库
	implementation("org.springframework.ai:spring-ai-starter-vector-store-redis")

	// 文档解析
	implementation("org.apache.tika:tika-core:4.2.0")
	implementation("org.apache.tika:tika-parsers-standard-package:4.2.0")

	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-mail")

	// Lettuce 连接池（spring.data.redis.lettuce.pool.* 配置生效所需）
	implementation("org.apache.commons:commons-pool2")

	// Spring AI 2.0 的 RedisVectorStore 依赖 Jedis 7.4+ 的统一客户端 redis.clients.jedis.RedisClient，
	// 而 Spring Boot 4.0 的 BOM 默认锁定 7.0.0（无该类），此处提升版本以启用向量库自动装配
	runtimeOnly("redis.clients:jedis:7.4.1")

	// Demo 环境：H2 文件数据库（替代 MariaDB），无需外部数据库进程
	runtimeOnly("com.h2database:h2")

	// Demo 环境：内嵌 Luban-RDS（兼容 Redis 协议，替代 Redis Stack）
	implementation("com.janeluo:luban-rds-spring-boot-starter:1.0.26")

	//jwt
	implementation("io.jsonwebtoken:jjwt-api:0.13.0")
	implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
	implementation("io.jsonwebtoken:jjwt-jackson:0.13.0")

	implementation("org.apache.commons:commons-lang3:3.22.0")
	implementation("org.apache.commons:commons-collections4:4.6.0")

	//验证码
	implementation("io.github.guwan:lead-captcha:0.0.2")
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
	}
	dependencies {
		// 覆盖 Spring Boot BOM 中锁定的 jedis 7.0.0，保证 redis.clients.jedis.RedisClient 存在
		dependency("redis.clients:jedis:7.4.1")
	}
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
	useJUnitPlatform()
	testLogging {
		events("passed", "skipped", "failed")
	}
}
