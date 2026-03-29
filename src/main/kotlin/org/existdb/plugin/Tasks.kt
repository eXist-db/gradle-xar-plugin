package org.existdb.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty

import org.gradle.api.provider.Property

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

abstract class ValidateXarExtensionTask : DefaultTask() {
    @get:Input abstract val javaClass: Property<String>
    @get:Input abstract val namespace: Property<String>
    @get:Input abstract val target: Property<String>
    @get:Input abstract val type: Property<PackageType>

    @TaskAction
    fun validate() {
        if (javaClass.get().isEmpty()) {
            throw GradleException("XAR plugin requires 'javaClass' to be set in the 'xar' extension")
        }
        if (namespace.get().isEmpty()) {
            throw GradleException("XAR plugin requires 'namespace' to be set in the 'xar' extension")
        }
        if (type.get() == PackageType.APPLICATION && target.get().isEmpty()) {
            throw GradleException("XAR plugin: 'target' must be set for application packages")
        }
    }
}

abstract class CreateExpathPackageDescriptorTask : DefaultTask() {
    @get:Input abstract val javaClass: Property<String>
    @get:Input abstract val namespace: Property<String>
    @get:Input abstract val target: Property<String>
    @get:Input abstract val version: Property<String>
    @get:Input abstract val title: Property<String>
    @get:Input abstract val home: Property<String>
    @get:Input abstract val abbrev: Property<String>

    @get:Input abstract val processorDependencies: ListProperty<String>
    @get:Input abstract val packageDependencies: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        outputFile.get().asFile.writeText(
            """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://expath.org/ns/pkg"
         xmlns:xs="http://www.w3.org/2001/XMLSchema"
         name="${namespace}"
         abbrev="${abbrev.get()}"
         version="${version.get()}"
         spec="1.0">
   <title>${title.get()}</title>
   <home>${home.get()}</home>
   ${processorDependencies.get().joinToString("\n")}
   ${packageDependencies.get().joinToString("\n")}
</package>"""
        )
    }

    fun renderDependency(versionedDependency: VersionedDependency, attribute: String):String {
        val versionAttributes = listOfNotNull(
            versionedDependency.version.takeIf { it.isNotEmpty() }?.let { """semver="$it"""" },
            versionedDependency.minVersion.takeIf { it.isNotEmpty() }?.let { """semver-min="$it"""" },
            versionedDependency.maxVersion.takeIf { it.isNotEmpty() }?.let { """semver-max="$it"""" }
        ).joinToString(" ")
        return """<dependency $attribute="${versionedDependency.name}" $versionAttributes />"""
    }
}

abstract class CreateRepoXmlTask : DefaultTask() {
    @get:Input abstract val home: Property<String>
    @get:Input abstract val prepare: Property<String>
    @get:Input abstract val finish: Property<String>
    @get:Input abstract val cleanup: Property<String>
    @get:Input abstract val pkgDesc: Property<String>
    @get:Input abstract val author: Property<String>
    @get:Input abstract val type: Property<PackageType>
    @get:Input abstract val target: Property<String>
    @get:Input abstract val status: Property<String>
    @get:Input abstract val license: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val home = home.get().ifEmpty { project.findProperty("project.url") as? String ?: "" }
        val scripts = listOfNotNull(
            prepare.get().takeIf { it.isNotEmpty() }?.let { """<prepare>$it</prepare>""" },
            finish.get().takeIf { it.isNotEmpty() }?.let { """<finish>$it</finish>""" },
            cleanup.get().takeIf { it.isNotEmpty() }?.let { """<cleanup>$it</cleanup>""" }
        ).joinToString("\n")

        val target = if (target.get().isNotEmpty()) {
            """<target>${target.get()}</target>"""
        } else ""

        outputFile.get().asFile.writeText(
            """<?xml version="1.0" encoding="UTF-8"?>
<meta xmlns="http://exist-db.org/xquery/repo"
      xmlns:repo="http://exist-db.org/xquery/repo"
      xmlns:xs="http://www.w3.org/2001/XMLSchema">
   <description>${pkgDesc.get()}</description>
   <author>${author.get()}</author>
   <type>${type.get()}</type>
   $target
   <website>$home</website>
   <status>${status.get()}</status>
   <license>${license.get()}</license>
   <copyright>true</copyright>
   $scripts
</meta>"""
        )
    }
}

abstract class CreateExistXmlTask : DefaultTask() {
    @get:Input abstract val javaClass: Property<String>
    @get:Input abstract val namespace: Property<String>
    @get:Input abstract val export: Property<Boolean>
    @get:Input abstract val requiredJars: ListProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val runtimeClasspath: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val projectJar: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:OutputDirectory
    abstract val contentDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val allRequiredJars = requiredJars.get().toSet()

        val jarFiles = (runtimeClasspath.files + projectJar.files).toList()
        val selectedJarFiles = allRequiredJars.mapNotNull { name ->
            jarFiles.find { it.name == name }
        }

        if (selectedJarFiles.size != allRequiredJars.size) {
            val missing = allRequiredJars - selectedJarFiles.map { it.name }.toSet()
            throw GradleException("Missing required JAR(s) for XAR: $missing")
        }
        val jarDeps = allRequiredJars.joinToString("\n") { "   <jar>$it</jar>" }

        val export = if (export.get()) """   <java>
        <namespace>${namespace.get()}</namespace>
        <class>${javaClass.get()}</class>
    </java>""" else ""

        outputFile.get().asFile.writeText(
            """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://exist-db.org/ns/expath-pkg"
         xmlns:xs="http://www.w3.org/2001/XMLSchema">
$export$jarDeps
</package>"""
        )

        contentDir.get().asFile.mkdirs()
        selectedJarFiles.forEach { jarFile ->
            val target = contentDir.file(jarFile.name).get().asFile
            jarFile.copyTo(target, overwrite = true)
        }
    }
}