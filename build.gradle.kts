plugins {
    id("java")
}

group = "cn.com.thebackrooms"
version = "1.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks {
    jar {
        manifest {
            attributes(
                "Premain-Class" to "cn.com.thebackrooms.loader.TheBackroomsLoader",
                "Can-Redefine-Classes" to "true",
                "Can-Retransform-Classes" to "true"
            )
        }
    }
}
