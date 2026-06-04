pluginManagement {
    repositories {
        mavenLocal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral {
            content {
                // Exclude to make sure we use dependency from mavenLocal
                excludeGroupByRegex("com\\.revenuecat.*")
            }
        }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google {
            content {
                // Exclude to make sure we use dependency from mavenLocal
                excludeGroupByRegex("com\\.revenuecat.*")
            }
        }
        mavenCentral {
            content {
                // Exclude to make sure we use dependency from mavenLocal
                excludeGroupByRegex("com\\.revenuecat.*")
            }
        }
    }
}

rootProject.name = "SDKSizeTesting"
include(":app")
