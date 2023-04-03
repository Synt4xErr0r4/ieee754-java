# ieee754-java [![javadoc](https://img.shields.io/endpoint?label=javadoc&url=https://javadoc.syntaxerror.at/ieee754-java/%3Fbadge=true%26version=latest)](https://javadoc.syntaxerror.at/ieee754-java/latest)

A Java 19 library for converting between IEEE 754 binary floating point numbers and Java's BigDecimal

## Overview

This library can convert `java.math.BigDecimal`s into IEEE 754 binary representations. By default, binary16, binary32, binary64, binary128, binary256, and x87's extended precision (binary80) are supported.

## Getting started

In order to use the code, you can either [download the jar](https://github.com/Synt4xErr0r4/json5/releases/download/1.3.0/json5-1.3.0.jar), or use the Maven dependency:

```xml
<!-- Repository -->

<repository>
  <id>syntaxerror.at</id>
  <url>https://maven.syntaxerror.at</url>
</repository>

<!-- Dependency -->

<dependency>
  <groupId>at.syntaxerror</groupId>
  <artifactId>ieee754-java</artifactId>
  <version>1.0.0</version>
</dependency>
```

The library itself is located in the module `json5`.

## Usage

TODO

## Documentation

The JavaDoc for the latest version can be found [here](https://javadoc.syntaxerror.at/ieee754-java/latest).

## Credits

The style of this project is based on FirebirdSQL's [decimal-java](https://github.com/FirebirdSQL/decimal-java) library.

## License

This project is licensed under the [MIT License](https://github.com/Synt4xErr0r4/ieee754-java/blob/main/LICENSE)
