package org.existdb.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile

internal class XarPluginTest {

    private fun setupProject(projectDir: File, buildGradle: String) {
        File(projectDir, "settings.gradle.kts").writeText("rootProject.name = \"xar-integration-test\"")
        File(projectDir, "build.gradle.kts").writeText(buildGradle)
        val mainJava = File(projectDir, "src/main/java/org/example/Main.java")
        mainJava.parentFile.mkdirs()
        mainJava.writeText("""
            package org.example;
            public class Main { public static void main(String[] args) {} }
        """.trimIndent())
    }

    @Test
    fun `build passes for valid minimal config`(@TempDir projectDir: File) {
        setupProject(projectDir, """
            plugins {
                `java`
                id("org.exist-db.plugin.xar")
            }
            group = "org.example"
            version = "1.0.0"

            xar {
                namespace = "org.example.xar"
                javaClass = "org.example.Main"
                target = "/db/apps"
                description = "A minimal test package"
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("build", "--stacktrace")
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":makeXar")?.outcome)

        val xarFile = File(projectDir, "build/libs/xar-integration-test-1.0.0.xar")
        assertTrue(xarFile.exists(), "XAR archive should be created")
    }

    @Test
    fun `fails when javaClass is missing for application type`(@TempDir projectDir: File) {
        setupProject(projectDir, """
            plugins {
                `java`
                id("org.exist-db.plugin.xar")
            }
            group = "org.example"
            version = "1.0.0"

            xar {
                namespace = "org.example.xar"
                target = "/db/apps"
                description = "Missing javaClass"
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("build", "--stacktrace")
            .buildAndFail()

        assertTrue(result.output.contains("XAR plugin requires 'javaClass' to be set"))
    }

    @Test
    fun `fails when namespace is missing for application type`(@TempDir projectDir: File) {
        setupProject(projectDir, """
            plugins { `java`; id("org.exist-db.plugin.xar") }
            group = "org.example"
            version = "1.0.0"

            xar {
                javaClass = "org.example.Main"
                target = "/db/apps"
                description = "Missing namespace"
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("build", "--stacktrace")
            .buildAndFail()

        assertTrue(result.output.contains("XAR plugin requires 'namespace' to be set"))
    }

    @Test
    fun `fails when target is missing for application type`(@TempDir projectDir: File) {
        setupProject(projectDir, """
            plugins { `java`; id("org.exist-db.plugin.xar") }
            group = "org.example"
            version = "1.0.0"

            xar {
                namespace = "org.example.xar"
                javaClass = "org.example.Main"
                description = "Missing target"
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("build", "--stacktrace")
            .buildAndFail()

        assertTrue(result.output.contains("XAR plugin: 'target' must be set for application packages"))
    }

    @Test
    fun `xar contains project jar and complete descriptors`(@TempDir projectDir: File) {
        setupProject(projectDir, """
            import org.existdb.plugin.PackageType.LIBRARY
            import org.existdb.plugin.exist6only

            plugins {
                `java`
                id("org.exist-db.plugin.xar")
            }
            group = "org.example"
            version = "1.2.3"

            xar {
                namespace = "//example.org/ns/test"
                javaClass = "org.example.Main"
                type = LIBRARY
                processorDependencies = listOf( exist6only )
            }
        """)

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("build", "--stacktrace")
            .build()

        ZipFile(File(projectDir, "build/libs/xar-integration-test-1.2.3.xar")).use { xar ->
            assertNotNull(xar.getEntry("content/xar-integration-test-1.2.3.jar"), "project jar should be packaged")

            val existXml = xar.getInputStream(xar.getEntry("exist.xml")).reader().readText()
            assertTrue(existXml.contains("<jar>xar-integration-test-1.2.3.jar</jar>"), existXml)

            val expathPkg = xar.getInputStream(xar.getEntry("expath-pkg.xml")).reader().readText()
            assertTrue(expathPkg.contains("""name="//example.org/ns/test""""), expathPkg)
            assertTrue(expathPkg.contains("""abbrev="xar-integration-test""""), expathPkg)
            assertTrue(expathPkg.contains("""version="1.2.3""""), expathPkg)
            assertTrue(expathPkg.contains("""processor="http://exist-db.org""""), expathPkg)
        }
    }
}
