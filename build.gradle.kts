import com.google.protobuf.gradle.id

plugins {
	id("java")
	id("checkstyle")
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.google.protobuf") version "0.9.6"
}
val springCloudVersion by extra("2025.1.3")

group = "org.spring.divas"
version = "0.0.1-SNAPSHOT"
description = "order-service"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

val checkstyleVersion = "13.7.0"
val protobufVersion = "4.36.2"
val grpcVersion = "1.84.1"
val logstashLogbackEncoderVersion = "9.0"
val resilience4jVersion = "2.4.0"
val wiremockVersion = "3.13.2"
val pactVersion = "4.7.5"

checkstyle {
	toolVersion = checkstyleVersion
	configFile = file("checks.xml")
}

repositories {
	mavenCentral()
}

dependencies {
	// Production dependencies - Spring Boot Starters & Framework
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	implementation("org.springframework.boot:spring-boot-starter-aspectj")
	implementation("org.springframework.boot:spring-boot-starter-kafka")
	implementation("org.springframework.boot:spring-boot-starter-opentelemetry")

	// Production dependencies - gRPC Client & Protobuf
	implementation("org.springframework.boot:spring-boot-starter-grpc-client")
	implementation("io.grpc:grpc-protobuf")
	implementation("io.grpc:grpc-stub")
	implementation("com.google.protobuf:protobuf-java:$protobufVersion")

	// Production dependencies - Resilience & Logging
	implementation("io.github.resilience4j:resilience4j-spring-boot4:$resilience4jVersion")
	implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")

	// Compile-only dependencies
	compileOnly("jakarta.annotation:jakarta.annotation-api")
	compileOnly("org.projectlombok:lombok")

	// Annotation processors
	annotationProcessor("org.projectlombok:lombok")

	// Runtime dependencies
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")

	// Test dependencies
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-grpc-client-test")
	testImplementation("org.springframework.kafka:spring-kafka-test")
	testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
	testImplementation("com.h2database:h2")
	testImplementation("au.com.dius.pact.provider:junit5:$pactVersion")

	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:$protobufVersion"
	}
	plugins {
		id("grpc") {
			artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
		}
	}
	generateProtoTasks {
		all().forEach { task ->
			task.plugins {
			}
		}
	}
}

tasks.named<Test>("test") {
	useJUnitPlatform()
	val pactFolder = System.getProperty(
		"pactfolder.path",
		project.file("../common/contract/pact").absolutePath
	)
	inputs.dir(pactFolder)
	systemProperty(
		"pactfolder.path",
		pactFolder
	)
}

tasks.named<Checkstyle>("checkstyleMain"){
	source = fileTree("src/main/java")
}
