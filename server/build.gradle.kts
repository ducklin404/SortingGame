plugins {
    id("java")
    id("application") // ⚡ phải thêm dòng này để dùng mainClass.set()
}

group = "group10"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // 🔗 Kết nối các module khác
    implementation(project(":common"))
    implementation(project(":persistence"))

    // 🧩 Thư viện hỗ trợ JSON cho trao đổi dữ liệu TCP
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.json:json:20240303")
    // 🧪 Kiểm thử
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    // ⚙️ Class có hàm main() để chạy server
    mainClass.set("group10.server.ServerMain")
}

tasks.test {
    useJUnitPlatform()
}
