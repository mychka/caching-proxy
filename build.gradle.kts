plugins {
    java
}

group = "com.rogaikopyta"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks {
    test {
        useJUnitPlatform()
    }

    jar {
        manifest {
            // Make jar runnable.
            attributes["Main-Class"] = "com.rogaikopyta.cachingproxy.CachingProxyApp"
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
}
