rootProject.name = "SortingGame"

// Khai báo các module con
include("client")
include("server")
include("common")
include("persistence")

// Cho phép Gradle tải plugin JavaFX, PostgreSQL,...
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
