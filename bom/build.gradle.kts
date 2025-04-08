plugins {
  kotlin("jvm")
}

if (!project.properties["ANDROID_VARIANT_TO_PUBLISH"].toString().contains("customEntitlementComputation")) {
  apply(plugin = "com.vanniktech.maven.publish")
}

dependencies {
  constraints {
    api(project(":purchases"))
    api(project(":ui:revenuecatui"))
    api(project(":ui:debugview"))
    api(project(":feature:amazon"))
  }
}