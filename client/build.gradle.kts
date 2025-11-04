plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.14"
}

group = "group10"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.openjfx:javafx-controls:21")
    implementation("org.openjfx:javafx-fxml:21")
    implementation("org.openjfx:javafx-graphics:21")
    implementation(project(":persistence"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("group10.client.ui.LeaderboardApp")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

tasks.test {
    useJUnitPlatform()
}
