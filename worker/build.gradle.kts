// No Spring here, deliberately: a framework in an async consumer only inflates
// cold start and package size. See PROJECT.md section 5.
dependencies {
    implementation(project(":shared"))
}
