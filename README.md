# liflig-properties

Library for fetching properties from local files and AWS Parameter Store and Secrets Manager. This library also contains type- and null-safe extension methods to `java.util.Properties` for Kotlin.

`liflig-cdk` has built-in support for creating secrets and properties, allowing `liflig-properties` to load these properties when a ECS service starts.
A ECS service defined through `liflig-cdk`, will automatically get the environment value`SSM_PREFIX`. This prefix is the prefix for
all properties in the AWS Parameter Store that the application should use, which are given in the definition of the
ECS service. This includes values that are defined directly, and
references to other AWS resources (including secrets in Secrets Manager).

When the service starts, this library fetches all properties for the application, including properties from AWS Parameter Store with the prefix
`SSM_PREFIX`. Properties are loaded in the following order, and properties loaded in later steps, overwrite properties loaded in earlier steps:

1. application.properties (from classpath)
2. Parameters from AWS Parameter Store matching the prefix `SSM_PREFIX`
3. overrides.properties (from working directory)
4. application-test.properties (from classpath)
5. overrides-test.properties (from working directory)

This library is currently only distributed in Liflig internal repositories.

## Contributing

### Build

To check build before pushing:

```bash
mvn verify
```

The CI server will automatically release new version for builds on master.

### Lint

Lint code:

```bash
mvn spotless:check
```

Fix lint errors:

```bash
mvn spotless:apply
```
