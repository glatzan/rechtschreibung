import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    id("org.jetbrains.compose") version "1.8.2"
}

group = "de.rechtschreibung"
// Version per `-PappVersion=1.2.3` überschreibbar (CI setzt sie aus dem Git-Tag). MSI verlangt x.y.z.
version = providers.gradleProperty("appVersion").getOrElse("1.0.0")

// Gradle-Testworker (und jpackage) kommen unter Windows nicht mit Umlauten im Pfad zurecht.
// Liegt das Projekt in so einem Ordner, wird außerhalb gebaut; `dist` kopiert das Ergebnis zurück.
if (projectDir.absolutePath.any { it.code > 127 }) {
    val base = System.getenv("LOCALAPPDATA") ?: System.getProperty("java.io.tmpdir")
    layout.buildDirectory.set(file("$base/rechtschreibung-build"))
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("org.languagetool:language-de:6.6")

    testImplementation(kotlin("test"))
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "2g"
}

// Lizenztexte landen im ausgelieferten Paket unter app/resources/.
val legalResources = layout.buildDirectory.dir("legal-resources")
val copyLegal by tasks.registering(Sync::class) {
    from("LICENSE", "THIRD-PARTY-NOTICES.md")
    from("licenses") { into("licenses") }
    into(legalResources.map { it.dir("common") })
}

compose.desktop {
    application {
        mainClass = "de.rechtschreibung.MainKt"
        jvmArgs += listOf("-Xmx1536m", "-Dfile.encoding=UTF-8")

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "Rechtschreibung"
            packageVersion = project.version.toString()
            description = "Offline-Rechtschreib- und Grammatikprüfung für Deutsch"
            appResourcesRootDir.set(legalResources)
            licenseFile.set(project.file("LICENSE"))
            vendor = "Rechtschreibung"
            // Ermittelt mit `gradlew suggestRuntimeModules`; hält die eingebaute Laufzeit klein.
            modules("java.compiler", "java.instrument", "java.naming", "java.scripting", "java.sql", "jdk.management", "jdk.unsupported")

            windows {
                iconFile.set(project.file("packaging/icon.ico"))
                menu = true
                shortcut = true
                dirChooser = true
                menuGroup = "Rechtschreibung"
                upgradeUuid = "6f1c2a3e-8b4d-4e7a-9c55-2d7e1f0a9b31"
            }
        }
    }
}

tasks.matching { it.name == "prepareAppResources" }.configureEach { dependsOn(copyLegal) }

// Portabler Ordner als ZIP.
val portableZip by tasks.registering(Zip::class) {
    group = "distribution"
    description = "Programmordner mit eingebauter Laufzeit als ZIP packen"
    dependsOn("createDistributable")
    from(layout.buildDirectory.dir("compose/binaries/main/app"))
    archiveFileName.set("Rechtschreibung-${project.version}-portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main/zip"))
}

// Baut Programmordner, ZIP und MSI und legt alles in <Projekt>/dist ab.
tasks.register<Sync>("dist") {
    group = "distribution"
    description = "Programmordner, Portable-ZIP und MSI nach dist/ kopieren"
    dependsOn("createDistributable", "packageMsi", portableZip)
    from(layout.buildDirectory.dir("compose/binaries/main/app"))
    from(layout.buildDirectory.dir("compose/binaries/main/msi"))
    from(layout.buildDirectory.dir("compose/binaries/main/zip"))
    into(project.layout.projectDirectory.dir("dist"))
}
