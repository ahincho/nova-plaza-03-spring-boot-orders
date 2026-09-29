pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        // El plugin de Nova se publica en GitHub Packages, que pide credenciales incluso para leer.
        // En CI llega NOVA_PACKAGES_READ_TOKEN; en local alcanza un GITHUB_TOKEN con read:packages.
        maven {
            name = "NovaSpringBootGradlePlugin"
            url = uri("https://maven.pkg.github.com/ahincho/nova-java-16-spring-boot-gradle-plugin")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("NOVA_PACKAGES_READ_TOKEN") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

rootProject.name = "plaza-orders"
