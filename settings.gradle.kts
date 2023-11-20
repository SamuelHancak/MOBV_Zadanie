pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
val properties = java.util.Properties()
file("local.properties").inputStream().use { properties.load(it) }

val mapboxPrivateKey: String? =
    properties.getProperty("sk.eyJ1IjoieGhhbmNha3MiLCJhIjoiY2xvNnhqNXN1MDBmMDJxa2FldmdidzFvOCJ9.YvIUgB2hLqE48THLMiU_SA")


dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")

            credentials {
                username = "mapbox"
                password = mapboxPrivateKey
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

rootProject.name = "MOBV Zadanie"
include(":app")
