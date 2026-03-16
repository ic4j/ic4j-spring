## IC4J Spring Library 

Java Spring is a comprehensive framework for building modern, enterprise-level applications, providing robust support for dependency injection, aspect-oriented programming, and web applications. 

<a href="https://spring.io/">
https://spring.io/
</a>

The IC4J Spring library allows native execution of Internet Computer smart contracts from Spring application. This library enables Java Spring developers to seamlessly integrate ICP canisters into their Spring applications, enhancing functionality and interoperability. Additionally, the library comes with predefined Internet Identity and Management Spring services, further simplifying the development process.

##To add IC4J Spring library to your Java project use Maven or Gradle import from Maven Central.

<a href="https://search.maven.org/artifact/org.ic4j/ic4j-spring/0.8.0/jar">
https://search.maven.org/artifact/org.ic4j/ic4j-spring/0.8.0/jar
</a>

```
<dependency>
  <groupId>org.ic4j</groupId>
  <artifactId>ic4j-spring</artifactId>
  <version>0.8.0</version>
</dependency>
```

```
implementation 'org.ic4j:ic4j-spring:0.8.0'
```

# Build

You need JDK 8+ to build IC4J Spring Library .

## Publishing

The Maven Central and local Maven release flow now follows the same NMCP-based mechanism used by `ic4j-agent`.

Use [DEPLOY.md](/Users/roman/Projects/eclipse-workspace/ic4j-spring/DEPLOY.md) for the full release process, including:

- loading Central Portal credentials from `~/.m2/maven-central.properties`
- running `scripts/release-preflight.sh`
- dry-running `publishAggregationToCentralPortal` and `publishAggregationToCentralSnapshots`
- publishing with `build-export.gradle`

For a local Maven install:

```bash
source scripts/load-maven-env.sh
gradle -b build-export.gradle --no-daemon -PcentralUsername="$CENTRAL_PORTAL_USERNAME" -PcentralPassword="$CENTRAL_PORTAL_PASSWORD" publishToMavenLocal --console=plain
```
