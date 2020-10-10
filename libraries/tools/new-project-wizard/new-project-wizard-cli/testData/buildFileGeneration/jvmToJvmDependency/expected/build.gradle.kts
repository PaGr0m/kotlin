group = "testGroupId"
version = "1.0-SNAPSHOT"



allprojects {
    repositories {
        maven {
            url = uri("KOTLIN_REPO")
        }
        mavenCentral()
    }
}