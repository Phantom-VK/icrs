plugins {
    java
    id("org.springframework.boot") version "3.5.10"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.college"
version = "0.0.1-SNAPSHOT"
description = "College Grievance Redressal System backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
	mavenCentral()
}

extra["springAiVersion"] = "1.1.0"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.flywaydb:flyway-core")
	implementation("org.projectlombok:lombok:1.18.40")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok:1.18.40")
	implementation("org.springframework.ai:spring-ai-starter-model-transformers")
	implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("org.postgresql:postgresql:42.7.9")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("dev.langchain4j:langchain4j-open-ai:1.10.0")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register<JavaExec>("importGrievanceVectors") {
	group = "application"
	description = "Imports grievance documents from a JSON file into the pgvector store"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.college.icrs.tools.GrievanceVectorImportMain")
	javaLauncher.set(javaToolchains.launcherFor {
		languageVersion = JavaLanguageVersion.of(25)
	})
	dependsOn(tasks.named("classes"))

	project.findProperty("grievanceImportFile")?.toString()?.takeIf { it.isNotBlank() }?.let {
		systemProperty("grievanceImportFile", it)
	}
	project.findProperty("grievanceImportReplaceExisting")?.toString()?.takeIf { it.isNotBlank() }?.let {
		systemProperty("grievanceImportReplaceExisting", it)
	}
}
