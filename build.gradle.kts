// IMPORTANT: You must explicitly import the extension function in Kotlin DSL
import org.gradle.plugin.compatibility.compatibility

//    id("org.gradle.plugin-compatibility") version "1.0.0" // or id("com.gradle.plugin-publish") version "2.1.0"

plugins {
    `java-gradle-plugin`
    id("org.gradle.plugin-compatibility") version "1.0.0"
    `kotlin-dsl`
    `maven-publish`
    signing
}

group = "org.exist-db"
version = "1.0.0"
val title = "EXpath XAR Plugin"
val desc = "Plugin for building expath XAR packages for eXist-db"

val javaVersion = 17

java {
    withSourcesJar()
    withJavadocJar()
}

gradlePlugin {
    plugins {
        create("xar") {
            id = "org.exist-db.plugin.xar"
            implementationClass = "org.existdb.plugin.XarPlugin"
            displayName = title
            description = desc
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "plugin.xar.gradle"
            from(components["java"])
            pom {
                name = title
                description = desc
                url = "http://github.com/exist-db/gradle-xar-plugin"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "line-o"
                        name = "Juri Leino"
                        email = "github@line-o.de"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/exist-db/gradle-xar-plugin.git"
                    developerConnection = "scm:git:ssh://github.com/exist-db/gradle-xar-plugin.git"
                    url = "http://github.com/exist-db/gradle-xar-plugin/"
                }
            }
        }
    }
}

signing {
    // useGpgCmd() does not work
    sign(publishing.publications["mavenJava"])
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
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

kotlin {
    jvmToolchain(javaVersion)
}
