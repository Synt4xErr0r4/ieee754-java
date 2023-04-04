# ieee754-java [![javadoc](https://img.shields.io/endpoint?label=javadoc&url=https://javadoc.syntaxerror.at/ieee754-java/%3Fbadge=true%26version=latest)](https://javadoc.syntaxerror.at/ieee754-java/latest)

A Java 19 library for converting between IEEE 754 binary floating-point numbers and Java's BigDecimal

## Overview

This library can convert `java.math.BigDecimal`s into IEEE 754 binary representations. By default, binary16, binary32, binary64, binary128, binary256, and x87's extended precision (binary80) are supported. There are also 3 larger types (binary512, binary1024, binary2048) available, these are, however, most likely impractial for any purpose due to their *extremely* long encoding/decoding times.

## Getting started

In order to use the code, you can either [download the jar](https://github.com/Synt4xErr0r4/ieee754-java/releases/download/1.0.0/ieee754-java-1.0.0.jar), or use the Maven dependency:

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

The library itself is located in the module `ieee754`.

## Usage

### Existing types

There are several predefined types:

- `Binary32` (`float`)
- `Binary64` (`double`)
- `Binary80` (x87 extended precision, like `long double` in C on Linux)
- `Binary128`
- `Binary256`
- `Binary512`*
- `Binary1024`*
- `Binary2048`*

\* These types are for demonstration purposes only, their parameters do not follow any official IEEE 754 standard, encoding/decoding might take *very* long and result might not be accurate. Use with caution.  

For any of the predefined types, there is a static field called `FACTORY`, which is used to create classes of their respective type:

- `create(int signum, BinaryType type)`
- `create(int signum, BigDecimal value)`
- `create(BigDecimal value)`
- `create(Number value)`

The following code, for example, creates a new 32-bit floating-point number holding the value `3.14159`:

```java
import at.syntaxerror.ieee754.Binary32;

/* ... */

Binary32 value = Binary32.FACTORY.create(3.14159);
```

Now, the function `encode` in `Binary32` can be used to get the number's binary representation:

```java
import java.math.BigInteger;

/* ... */

BigInteger bin = value.encode();
```

The binary representation can also be decoded. The `BinaryCodec` class, accessible via `Binary32`'s `CODEC` field, provides the method `decode`:

```java
Binary32 decoded = Binary32.CODEC.decode(bin);
```

The value can then be retrieved for further computations as a `BigDecimal` via the `getBigDecimal` method:

```java
BigDecimal bigdec = decoded.getBigDecimal();
```

This is applicable to all predefined types; Also, any class inheriting from `Binary<T>` has access to various helper methods, which are listed in the [JavaDoc](https://javadoc.syntaxerror.at/ieee754-java/latest/at/syntaxerror/ieee754/Binary.html).

### Custom Types

You can also create custom floating-point types:

When extending `Binary<T>`, you need to implement the method `getCodec`, which returns a `BinaryCodec<T>`. This codec can be created by using its constructor, which takes the number of exponent bits, mantissa bits, whether there is an implicit bit, and a `BinaryFactory<T>`. The factory is an interface where you need to implement constructors for your new class.

Take a look at the various predefined types to see how they are implemented.

### Note

Probably due to rounding errors, `Double.MIN_VALUE` and `Binary64.MIN_VALUE` do *not* have the same value. Trying to encode `Double.MIN_VALUE` will result in an binary representation for the value `0`.

## Documentation

The JavaDoc for the latest version can be found [here](https://javadoc.syntaxerror.at/ieee754-java/latest).

## Dependencies

This project makes use of the following dependencies:

- [Project Lombok](https://projectlombok.org/)
- [big-math](https://github.com/eobermuhlner/big-math)

## Credits

The style of this project is based on FirebirdSQL's [decimal-java](https://github.com/FirebirdSQL/decimal-java) library.

## License

This project is licensed under the [MIT License](https://github.com/Synt4xErr0r4/ieee754-java/blob/main/LICENSE)
