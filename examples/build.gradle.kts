plugins {
    application
}
application {
    mainClass.set("org.example.HolidaysDemoApp")
}
dependencies {
    implementation(project(":lib"))
}