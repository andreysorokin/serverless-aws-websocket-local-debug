plugins {
    id("io.freefair.lombok") version "6.3.0"
    id("java")
}

group = "com.rewintous.syncstate"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}



dependencies {
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    implementation(platform("software.amazon.awssdk:bom:2.24.8"))
    implementation("software.amazon.awssdk:dynamodb-enhanced")
    implementation("software.amazon.awssdk:lambda")
    implementation("software.amazon.awssdk:apigatewayv2")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation(platform("com.amazonaws:aws-java-sdk-bom:1.12.638"))
    implementation("com.amazonaws:aws-java-sdk-apigatewayv2")
    implementation("com.amazonaws:aws-java-sdk-apigatewaymanagementapi")

    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.4")



    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Zip>("buildZip") {
    archiveFileName.set("aws-java-websocket-sync.zip")
    from(sourceSets["main"].output)
    into("lib") {
        from(configurations.runtimeClasspath)
    }
}

tasks.named("build") {
    dependsOn("buildZip")
}
