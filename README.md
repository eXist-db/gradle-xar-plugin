# Gradle XAR Plugin

A Gradle plugin for building [EXpath XAR packages](http://expath.org/spec/pkg/20120509) with exist-db specific extensions.

Two files, repo.xml and exist.xml, which are needed to put JARs on the classpath and run package specific preparation and cleanup scripts.
If you want to learn more about the XAR format extensions, please refer to the [eXist-db EXpath Package Manager documentation](http://exist-db.org/exist/apps/doc/repo#expath-pkg).

## Usage

Apply the plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("org.exist-db.plugin") version "1.0.0"
}

import org.exist-db.plugin.XarExtension
import org.exist-db.plugin.PackageType
import org.exist-db.plugin.exist6only
```

Configure the XAR properties:

```kotlin
xar {
    namespace = "//myorg/ns/myapp"
    javaClass = "com.myorg.myapp.MyModule"
    abbrev = "myapp"
    title = "My Application"
    type = Package.APPLICATION
    target = "myapp"
    home = "https://github.com/myorg/myapp"
    requiredJars = listOf(
        "some-dependency-1.0.jar",
        "another-dependency-2.0.jar"
    )
    processorDependencies = listOf( exist6only )
    packageDependencies = listOf( VersionedDependency("templating", "", "1.2", "1") )
}
```

Run the task:

```bash
./gradlew build
```

All tasks will be executed

First `createXarResources` will make sure that the XAR output directory exists.
Then `copyXarResources` will copy all static resources from `src/main/xar-resources`.
`createExpathPackageDescriptor` will generate the expath-pkg.xml file, `createRepoXml` generates repo.xml, and `createExistXml` generates the exist.xml and copies the JARs required at runtime to the content folder.
Finally `makeXar` zips the XAR archive `build/libs/myapp-<version>.xar`.

### Processor Dependency shortcuts

You may have noticed these two lines in the usage.

```kotlin
import org.exist-db.plugin.exist6only
```

and later

```kotlin
    processorDependencies = listOf( exist6only )
```

These are named exports to ease the process of signalling for which processor this package is compatibile with. The above will set both max and min version for the eXist-db processor to 6.

There are a few more of these named processorDependencies exported.

```kotlin
public val exist640orLater = VersionedDependency("exist", "", "6.4.0", "")
public val exist6only = VersionedDependency("exist", "", "6", "6")
public val exist7orLater = VersionedDependency("exist", "", "7", "")
```

## Requirements

- Gradle 8+
- Java 17+

## Tasks

Following tasks are available when the plugin is used

- `createXarResources`: Creates the XAR staging directory
- `copyXarResources`: Copies XAR resources
- `createExpathPackageDescriptor`: Generates expath-pkg.xml
- `createRepoXml`: Generates repo.xml
- `createExistXml`: Generates exist.xml and copies JARs
- `makeXar`: Creates the XAR package

## Extension Properties

- `namespace`: EXPath package namespace (required)
- `javaClass`: Main Java class for the module (required)
- `target`: name of the collection in /db/apps this package should be installed in (required for applications, optional for libraries)
- `version`: EXPath package version (defaults to project version)
- `abbrev`: Package abbreviation (default: project name)
- `title`: Package title (default: project description or name)
- `version`: Package version (default: project version)
- `type`: Package.LIBRARY or Package.APPLICATION
- `home`: Home URL (default: empty)
- `status`: Package status (default: empty)
- `license`: Package license (default: empty)
- `requiredJars`: List of JARs that have to be package within the XAR and are needed at runtime (default: empty)
- `processorDependencies`: List of processors and their versions, that this package can run on (default: empty)
- `packageDependencies`: List of packages and their versions this package depends on

## Development

In order to use a version you are currently developing
publishing to your local maven repository is a easy way to test changes on local projects.

Publish to local .m2

```sh
./gradlew publishToMavenLocal
```

That's it, now your projects will use your local development version of the plugin.
