# ieee754-java [![javadoc](https://img.shields.io/endpoint?label=javadoc&url=https://javadoc.syntaxerror.at/ieee754-java/%3Fbadge=true%26version=latest)](https://javadoc.syntaxerror.at/ieee754-java/latest) ![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/Synt4xErr0r4/ieee754-java/maven.yml)

A Java 19 library for converting between IEEE 754 binary and decimal floating-point numbers and Java's BigDecimal

## Overview

This library can convert `java.math.BigDecimal`s into IEEE 754 binary representations. By default, binary16, binary32, binary64, binary128, binary256, x87's extended precision (binary80), decimal32, decimal64 and decimal128 are supported. There are also 3 larger types (binary512, binary1024, binary2048) available, these are, however, most likely impractial for any purpose due to their *extremely* long encoding/decoding times.

## Getting started

In order to use the code, you can either [download the jar](https://github.com/Synt4xErr0r4/ieee754-java/releases/download/2.0.1/ieee754-java-2.1.1.jar), or use the Maven dependency:

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
  <version>2.1.1</version>
</dependency>
```

The library itself is located in the module `ieee754java`.

## Usage

### Existing types

There are several predefined types:

- `Binary16` (half precision)
- `Binary32` (single precision, like `float`)
- `Binary64` (double precision, like `double`)
- `Binary80` (x87 extended precision, like C's `long double` on Linux)
- `Binary128` (quadruple precision)
- `Binary256` (octuple precision)
- `Binary512`*
- `Binary1024`*
- `Binary2048`*
- `Decimal32`
- `Decimal64`
- `Decimal128`

\* These types are for demonstration purposes only, their parameters do not follow any official IEEE 754 standard, encoding/decoding might take *very* long and result might not be accurate. Use with caution.  

For any of the predefined types, there is a static field called `FACTORY`, which is used to create classes of their respective type:

- `create(int signum, BinaryType type)`
- `create(int signum, BigDecimal value)` (only useful for signed zeros)
- `create(BigDecimal value)`
- `create(Number value)`

The following code, for example, creates a new 32-bit floating-point number holding the value `3.14159`:

```java
import at.syntaxerror.ieee754.binary.Binary32;

/* ... */

Binary32 value = Binary32.FACTORY.create(3.14159);
```

*Note: using native `float` and `double` might lead to inaccurate results due to rounding errors. It is recommended to use `BigDecimal` instead.*

Now, the function `encode` in `Binary32` can be used to get the number's binary representation:

```java
import java.math.BigInteger;

/* ... */

BigInteger bin = value.encode();
```

The binary representation can also be decoded. The `FloatingCodec` class, accessible via `Binary32`'s `CODEC` field, provides the method `decode`:

```java
Binary32 decoded = Binary32.CODEC.decode(bin);
```

The value can then be retrieved for further computations as a `BigDecimal` via the `getBigDecimal` method:

```java
BigDecimal bigdec = decoded.getBigDecimal();
```

This is applicable to all predefined types; also, any class inheriting from `Floating<T>` (or its subclasses `Binary<T>` and `Decimal<T>`) has access to various helper methods, which are listed in the [JavaDoc](https://javadoc.syntaxerror.at/ieee754-java/latest/ieee754java/at/syntaxerror/ieee754/Floating.html).

### Decimal Encoding

There are two ways IEEE 754 decimal floating-point numbers can be encoded:

- BID (binary integer decimal) format
- DPD (densly packed decimal) format

Both formats encode the same range of numbers.

The `DecimalCodec<T>` class also provides separate encoding/decoding methods for the two modes:

- `encodeBID(T value)`
- `encodeDPD(T value)`
- `decodeBID(BigInteger value)`
- `decodeDPD(BigInteger value)`

Similar methods are available in `Decimal<T>`:

- `encodeBID()`
- `encodeDPD()`

Methods that do not have a suffix (`BID` or `DPD`) call one of these methods depending on the default mode.
By default, `BID` is used.  
This can be changed by settings the `DEFAULT_CODING` field in the `Decimal` class
to either `DecimalCoding.BINARY_INTEGER_DECIMAL` or `DecimalCoding.DENSLY_PACKED_DECIMAL`.

### Custom Types

You can also create custom floating-point types:

When extending `Floating<T>` (or the subclass `Binary<T>` or `Decimal<T>`), you need to implement the method `getCodec`, which returns a `FloatingCodec<T>`. The `FloatingCodec<T>`, however, is an abstract class. If you want to implement a `Binary`-like codec, use the `BinaryCodec<T>` class instead. For `Decimal`-like codecs, `DecimalCodec<T>` exists. These codecs can be created by using their constructors.

The `BinaryCodec<T>` constructor takes the number of exponent bits, significand bits, whether there is an implicit bit and a `FloatingFactory<T>`.  
The `DecimalCodec<T>` constructor takes the number of combination field bits, significand bits and a `FloatingFactory<T>`

The factory is an interface where you need to implement constructors for your new class.

Take a look at the various predefined types to see how they are implemented.

### Rounding

Some numbers cannot be encoded with full precision. In such cases, rounding is performed.

There are five IEEE 754 rounding modes:

- `TIES_EVEN`: round to nearest, ties to even
  - rounds to the nearest value
  - if the number falls midway, it is rounded to the nearest even value.
- `TIES_AWAY`: round to nearest, ties away from 0
  - rounds to the nearest value
  - if the number falls midway, it is rounded to the nearest value above (positive numbers) or below (negative numbers).
- `TOWARD_ZERO`: round toward 0 (aka. truncating)
- `TOWARD_POSITIVE`: round toward +∞ (aka. rounding up, ceiling)
- `TOWARD_NEGATIVE`: round toward -∞ (aka. rounding down, floor)

The default rounding mode used for encoding is `TIES_EVEN`. This can be changed by altering the `DEFAULT_ROUNDING` field
in the `Rounding` class, e.g.:

```java
import at.syntaxerror.ieee754.rounding.Rounding;

/* ... */

Rounding.DEFAULT_ROUNDING = Rounding.TOWARD_ZERO;
```

## Documentation

The JavaDoc for the latest version can be found [here](https://javadoc.syntaxerror.at/ieee754-java/latest).

## Changelog

### 2.1.1

- Fixed bug with values around the minimum subnormal value becoming 0

### 2.1.0

- Added support for different rounding modes
- Improved performance for very small numbers

### 2.0.0

- Added decimal floating-point formats `Decimal32`, `Decimal64` and `Decimal128`

### 1.0.0

- Added binary floating-point formats `Binary16`, `Binary32`, `Binary64`, `Binary80`, `Binary128`, `Binary256`, `Binary512`, `Binary1024` and `Binary2048`

## Dependencies

This project makes use of the following dependencies:

- [Project Lombok](https://projectlombok.org/)
- [big-math](https://github.com/eobermuhlner/big-math)

## Credits

The style of this project is based on FirebirdSQL's [decimal-java](https://github.com/FirebirdSQL/decimal-java) library.

## License

This project is licensed under the [MIT License](https://github.com/Synt4xErr0r4/ieee754-java/blob/main/LICENSE)
