// IMPORTANT: You must explicitly import the extension function in Kotlin DSL
import org.gradle.plugin.compatibility.compatibility

plugins {
    id("com.gradle.plugin-publish") version "2.1.1"
    `kotlin-dsl`
}

group = "org.exist-db"
version = "1.0.0"
val title = "EXpath XAR Plugin"
val desc = "Plugin for building expath XAR packages for eXist-db"

java {
    withSourcesJar()
    withJavadocJar()
}

gradlePlugin {
    website = "https://github.com/eXist-db/gradle-xar-plugin"
    vcsUrl = "https://github.com/eXist-db/gradle-xar-plugin.git"
    plugins {
        create("xar") {
            id = "org.exist-db.plugin.xar"
            implementationClass = "org.existdb.plugin.XarPlugin"
            displayName = title
            description = desc
            tags = listOf("exist-db", "xar", "expath")
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.2")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
