# Integration guide

This article describes how to integrate the library in your project, with Gradle / Maven / Jars or use SNAPSHOT builds.

Current version is [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.mapsforge/vtm/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.mapsforge/vtm)

## Gradle

### Core
```groovy
compile 'org.mapsforge:vtm:[CURRENT-VERSION]'
compile 'org.mapsforge:vtm-themes:[CURRENT-VERSION]'
compile 'org.slf4j:slf4j-api:1.7.21'
```

### Android
```groovy
compile 'org.mapsforge:vtm-android:[CURRENT-VERSION]'
compile 'org.mapsforge:vtm-android:[CURRENT-VERSION]:natives-armeabi'
compile 'org.mapsforge:vtm-android:[CURRENT-VERSION]:natives-armeabi-v7a'
compile 'org.mapsforge:vtm-android:[CURRENT-VERSION]:natives-x86'
compile 'com.caverock:androidsvg:1.2.2-beta-1'
```

### iOS

Detailed iOS instructions can be found [here](ios.md).

### Desktop
```groovy
compile 'org.mapsforge:vtm-gdx:[CURRENT-VERSION]'
compile 'org.mapsforge:vtm-desktop:[CURRENT-VERSION]'
compile 'org.mapsforge:vtm-desktop:[CURRENT-VERSION]:natives-linux'
compile 'org.mapsforge:vtm-desktop:[CURRENT-VERSION]:natives-osx'
compile 'org.mapsforge:vtm-desktop:[CURRENT-VERSION]:natives-windows'
compile 'com.badlogicgames.gdx:gdx:1.9.4'
compile 'com.badlogicgames.gdx:gdx-platform:1.9.4:natives-desktop'
compile 'com.badlogicgames.gdx:gdx-backend-lwjgl:1.9.4'
compile 'org.lwjgl.lwjgl:lwjgl:2.9.3'
compile 'org.lwjgl.lwjgl:lwjgl-platform:2.9.3:natives-linux'
compile 'org.lwjgl.lwjgl:lwjgl-platform:2.9.3:natives-osx'
compile 'org.lwjgl.lwjgl:lwjgl-platform:2.9.3:natives-windows'
compile 'com.metsci.ext.com.kitfox.svg:svg-salamander:0.1.19'
```

## Snapshots

We publish regularly SNAPSHOT builds to Sonatype OSS Repository Hosting.

You need to add the repository:
```groovy
repositories {
    maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
}
```

And declare the dependencies like:
```groovy
compile 'org.mapsforge:vtm:master-SNAPSHOT'
...
```

## Maven

The dependencies for Maven are declared in a similar way. For example:

```xml
<dependency>
    <groupId>org.mapsforge</groupId>
    <artifactId>vtm</artifactId>
    <version>[CURRENT-VERSION]</version>
</dependency>
```

## Jars

You can find release and snapshot jars (regular and with dependencies) in [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.mapsforge%22) and [Sonatype OSS Repository Hosting](https://oss.sonatype.org/content/repositories/snapshots/org/mapsforge/).

Third party jars can be found at their respective sites or in Maven Central repository.
