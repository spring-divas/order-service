plugins {
	id("java")
	id("checkstyle")
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "org.spring.divas"
version = "0.0.1-SNAPSHOT"
description = "order-service"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

checkstyle {
	toolVersion = "13.7.0"
	configFile = file("checks.xml")
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation ("org.springframework.boot:spring-boot-starter-web")
	testImplementation ("org.springframework.boot:spring-boot-starter-test")

	implementation("org.springframework.boot:spring-boot-starter-actuator")

	runtimeOnly("org.postgresql:postgresql")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("com.h2database:h2")
}

tasks.named<Test>("test") {
	useJUnitPlatform()
}