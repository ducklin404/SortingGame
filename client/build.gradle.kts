plugins {
    id("java")
}

group = "group10"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(project(":common"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.0")
    implementation("com.formdev:flatlaf:3.4")
    implementation("com.formdev:flatlaf-intellij-themes:3.4")

}

tasks.test {
    useJUnitPlatform()
}