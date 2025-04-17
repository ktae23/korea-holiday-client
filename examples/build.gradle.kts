plugins {
    application
}
application {
    mainClass.set("org.example.App")
}
dependencies {
    implementation(project(":lib"))
}