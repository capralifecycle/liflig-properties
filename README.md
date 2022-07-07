# liflig-properties

Library for fetching properties from local files and AWS Parameter Store and Secrets Manager. This library also contains type- and null-safe extension methods to `java.util.Properties` for Kotlin.

For properties loading order, see the documentation in the code https://github.com/capralifecycle/liflig-properties/blob/master/src/main/kotlin/no/liflig/properties/PropertiesLoader.kt#L11-L29

`liflig-cdk` has built-in support for creating secrets and properties that `liflig-properties` can load.

This library is currently only distributed in Liflig internal repositories.

## Contributing

This project follows
https://confluence.capraconsulting.no/x/fckBC

To check build before pushing:

```bash
mvn verify
```

The CI server will automatically release new version for builds on master.
