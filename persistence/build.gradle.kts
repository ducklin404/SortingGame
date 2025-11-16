plugins {
    id("java")
}

group = "group10"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("org.slf4j:slf4j-api:2.0.12")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.12")
    implementation("org.mindrot:jbcrypt:0.4")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("io.github.cdimascio:dotenv-java:3.2.0")
}

tasks.test {
    useJUnitPlatform()
}