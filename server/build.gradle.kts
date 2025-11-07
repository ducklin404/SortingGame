plugins {
    id("application")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation(project(":persistence"))
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.json:json:20240303") // 🟢 Thêm dòng này
}

application {
    mainClass.set("group10.server.ServerMain")
}
