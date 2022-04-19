plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
    // if $$$getFont$$$ duplicate method error is thrown add '--rerun-tasks' option to gradle command
    id("io.github.file5.guidesigner") version "1.0.2"
}

group = "io.lightbeat"
version = "1.6.2-SNAPSHOT"
description = "LightBeat"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("io.lightbeat.LightBeat")
}

repositories {
    mavenLocal()
    maven { url = uri("https://repo.maven.apache.org/maven2/") }
}

dependencies {
    implementation("io.github.zeroone3010:yetanotherhueapi:2.7.0-SNAPSHOT") // github.com/Kakifrucht/yetanotherhueapi -> install to maven local
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("org.slf4j:jul-to-slf4j:1.7.36")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("com.github.wendykierp:JTransforms:3.1:with-dependencies")
    implementation("com.github.weisj:darklaf-core:2.7.3")
    implementation("org.jitsi:libjitsi:1.1-4-gfb6b03ff")
    implementation("com.jetbrains.intellij.java:java-gui-forms-rt:222.1149")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.mockito:mockito-all:1.10.19")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.lightbeat.LightBeat"
        attributes["Implementation-Version"] = project.version
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileJava {
    options.encoding = "UTF-8"
}
