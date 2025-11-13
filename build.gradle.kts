group = "group10"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
}

/*
 * Task chạy cả server và client theo đúng thứ tự:
 * 1) Chạy server trước
 * 2) Khi server chạy xong → chạy client
 */
tasks.register("runAll") {
    group = "application"
    description = "Run server first, then client"

    dependsOn(":server:run")
    finalizedBy(":client:run")
}
