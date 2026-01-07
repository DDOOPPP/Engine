plugins {
    id("java")
}

group = "org.gi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":API"))
    implementation(project(":Util"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.zaxxer:HikariCP:6.3.0")
}

tasks.test {
    useJUnitPlatform()
}