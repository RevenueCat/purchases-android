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
        // GitHub Packages for purchases-core Rust library (transitive dependency)
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/RevenueCat/purchases-core")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: ""
            }
            content {
                // Only fetch purchases-core from GitHub Packages
                includeModule("com.revenuecat.purchases", "purchases-core")
                includeModule("com.revenuecat.purchases", "purchases-core-android")
                includeModule("com.revenuecat.purchases", "purchases-core-jvm")
            }
        }
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
