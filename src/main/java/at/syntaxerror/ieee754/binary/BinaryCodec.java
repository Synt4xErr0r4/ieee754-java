/* MIT License
 * 
 * Copyright (c) 2023 Thomas Kasper
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package at.syntaxerror.ieee754.binary;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import at.syntaxerror.ieee754.FloatingCodec;
import at.syntaxerror.ieee754.FloatingFactory;
import at.syntaxerror.ieee754.FloatingType;
import at.syntaxerror.ieee754.rounding.Rounding;
import ch.obermuhlner.math.big.BigDecimalMath;
import lombok.NonNull;

/**
 * This class represents a codec capable of encoding and decoding IEEE 754 binary floating point numbers
 * as well as computing common values (such as NaN, maximum value, ...)
 * 
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings("unchecked")
public final class BinaryCodec<T extends Binary<T>> extends FloatingCodec<T> {

	private static final MathContext FLOOR = new MathContext(0, RoundingMode.FLOOR);
	private static final MathContext CONTEXT = new MathContext(800);
	
	private static final BigDecimal TWO = BigDecimal.valueOf(2);
	private static final BigDecimal LOG10_2 = BigDecimalMath.log10(TWO, new MathContext(604, RoundingMode.HALF_EVEN));

	private static final int MEMOIZE_POS_INF = 0;
	private static final int MEMOIZE_NEG_INF = 1;
	private static final int MEMOIZE_SMINVAL = 2;
	private static final int MEMOIZE_MIN_VAL = 3;
	private static final int MEMOIZE_MAX_VAL = 4;
	private static final int MEMOIZE_EPSILON = 5;
	private static final int MEMOIZE_EXRANGE = 6;
	private static final int MEMOIZE_EX10RNG = 7;
	private static final int MEMOIZE_DECDIGS = 8;
	private static final int MEMOIZE_POWEXP1 = 9;
	private static final int MEMOIZE_POWMANT = 10;
	
	private final int exponent;
	private final int significand;
	private final boolean implicit;
	
	private final FloatingFactory<T> factory;
	
	private final Map<Integer, Object> memoized = new HashMap<>();
	
	/**
	 * Creates a new binary codec
	 * 
	 * @param exponent the number of exponent bits ({@code > 0}, {@code < 32})
	 * @param significand the number of significand bits ({@code > 0})
	 * @param implicit whether there is an implicit significand bit
	 * @param factory the factory for creating {@link Binary} objects
	 */
	public BinaryCodec(int exponent, int significand, boolean implicit, @NonNull FloatingFactory<T> factory) {
		if(exponent < 1) throw new IllegalArgumentException("Illegal non-positive exponent size");
		if(significand < 1) throw new IllegalArgumentException("Illegal non-positive significand size");
 
		if(exponent > 31) throw new IllegalArgumentException("Exponent size is too big");

		this.exponent = exponent;
		this.significand = significand;
		this.implicit = implicit;
		this.factory = factory;
		
		initialize();
	}

	private <R> R memoize(int id, Supplier<R> generator) {
		Object value = memoized.get(id);
		
		if(value == null)
			memoized.put(id, value = generator.get());
		
		return (R) value;
	}
	
	/**
	 * Returns the number of bits occupied by the exponent
	 * 
	 * @return the number of exponent's bits 
	 */
	public int getExponentBits() {
		return exponent;
	}
	
	/**
	 * Returns the number of bits occupied by the significand
	 * 
	 * @return the number of significand's bits 
	 */
	public int getSignificandBits() {
		return significand;
	}
	
	/**
	 * Returns whether there is an implicit bit used for the binary representation
	 * 
	 * @return whether there is an implicit bit
	 */
	public boolean isImplicit() {
		return implicit;
	}
	
	/** {@inheritDoc} */
	@Override
	public BigInteger encode(T value) {
		if(!value.isFinite()) {
			
			if(value.isSignalingNaN())
				return getSignalingNaN(value.getSignum());
			
			if(value.isQuietNaN())
				return getQuietNaN(value.getSignum());
			
			if(value.isPositiveInfinity())
				return getPositiveInfinity();
			
			if(value.isNegativeInfinity())
				return getNegativeInfinity();
			
			assert false : "unreachable";
		}
		
		if(value.isZero())
			return getZero(value.getSignum());
		
		BigDecimal bigdec = value.getBigDecimal();
		
		// determine sign bit
		int sign = bigdec.signum() == -1 ? 1 : 0;
		
		bigdec = bigdec.abs();
		
		// strip fractional part
		BigInteger significand = bigdec.toBigInteger();
		
		// strip integer part
		BigDecimal fraction = bigdec.subtract(new BigDecimal(significand))
			.abs().stripTrailingZeros();
		
		// remove sign
		significand = significand.abs();

		// unbiased exponent, or -1 if exponent would be negative
		int exp = significand.bitLength() - 1;
		
		boolean negative = exp < 0;
		
		int zeros = 0;
		
		final int off = getOffset();
		final int eMin = getExponentRange().getKey();
		
		boolean requireRounding = false;
		boolean guard = false;
		boolean round = false;
		boolean sticky = false;
		
		if(fraction.compareTo(BigDecimal.ZERO) != 0) {
			// skip leading zeros. this heavily improves the performance for larger types
			
			zeros = BigDecimalMath.log2(
				BigDecimalMath.reciprocal(fraction, CONTEXT),
				CONTEXT
			).setScale(0, RoundingMode.CEILING).intValue() - 1;
			
			fraction = fraction.multiply(pow2(zeros)).stripTrailingZeros();

			significand = significand.shiftLeft(zeros);
			
			int bitCount = significand.bitLength();
			int threshold = this.significand + off;
			
			if(zeros > -eMin)
				bitCount += zeros + eMin;
			
			if(bitCount > threshold) {
				requireRounding = true;
				
				fraction = fraction.multiply(BigDecimal.TWO);

				round = fraction.intValue() != 0;
				
				if(round)
					fraction = fraction.subtract(BigDecimal.ONE);
				
				sticky = fraction.compareTo(BigDecimal.ZERO) != 0;
				
				fraction = BigDecimal.ZERO;
			}
		}
		
		/* - left-shift the significand by 1
		 * - multiply the fraction by two
		 * - bit-or the significand with the integer part of result (0 or 1)
		 * - remove the integer part from the result
		 * - repeat until the fraction is zero, or all significand bits are used up
		 * 
		 * while(fraction != 0) {
		 * 	   significand <<= 1;
		 *     fraction *= 2;
		 *     significand |= (int) fraction;
		 *     fraction -= (int) fraction;
		 * }
		 */
		while(fraction.compareTo(BigDecimal.ZERO) != 0) {
			fraction = fraction.multiply(TWO);
			
			int integerPart = fraction.intValue();
			int bitCount = significand.bitLength();
			int significandLength = bitCount;
			
			// when there are more zeros than -e_min, the number is subnormal, and the leading zeros are part of the significand
			if(zeros > -eMin)
				significandLength += zeros + eMin;
			
			// max number of bits reached
			if(significandLength > this.significand + off) {
				requireRounding = true;
				
				guard = significand.testBit(0);
				round = integerPart == 1;
				sticky = fraction.compareTo(BigDecimal.ONE) != 0;
				
				break;
			}

			significand = significand.shiftLeft(1);

			if(integerPart == 1) {
				significand = significand.or(BigInteger.ONE);
				fraction = fraction.subtract(BigDecimal.ONE);
			}
			else if(bitCount == 0)
				++zeros;
		}
		
		if(requireRounding && Rounding.DEFAULT_ROUNDING.roundBinary(sign == -1, guard, round, sticky)) {
			// round up
			int bits = significand.bitLength();
			
			significand = significand.add(BigInteger.ONE);
			
			int newBits = significand.bitLength();

			if(bits < newBits) { // overflow occured, adjust exponent
				
				if(newBits > this.significand + off)
					significand = significand.clearBit(newBits - 1); // clear overflown bit
				
				++exp;
				
				// if exponent is all 1s now, return Infinity
				if(exp == mask(this.exponent).intValue())
					return sign == -1
						? getNegativeInfinity()
						: getPositiveInfinity();
			}
		}
		
		int len = significand.bitLength();
		
		// fix exponent if < 1
		if(negative)
			exp -= zeros;
		
		if(exp < eMin) // subnormal
			return len == 0
				? getZero(sign)
				: BigInteger.valueOf(sign)
					.shiftLeft(this.exponent + this.significand + off)
					.or(significand.shiftLeft(this.significand - eMin + exp - len + 2));

		// clear most significant bit if number is implicit
		if(implicit && len > 0)
			significand = significand.clearBit(len - 1);

		BigInteger result = BigInteger.valueOf(sign) 
			// add bias to exponent
			.shiftLeft(this.exponent)
			.or(BigInteger.valueOf(exp + getBias()))
			// align significand to the left
			.shiftLeft(this.significand + off)
			.or(significand.shiftLeft(this.significand - len + 1));
		
		if(!implicit)
			result = result.or(BigInteger.ONE.shiftLeft(this.significand));
		
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public T decode(BigInteger value) {
		// extract sign (most significant bit)
		boolean sign = isNegative(value);
		
		// extract significand
		BigInteger significand = getFullSignificand(value);
		
		// extract (biased) exponent
		int exponent = getExponent(value).intValue();
		
		// calculate exponent bias
		int bias = getBias();
		
		boolean subnormal = false;
		
		// exponent is all 0s => signed zero, subnormals
		if(exponent == 0) {
			// significand == 0 => signed zero
			if(significand.compareTo(BigInteger.ZERO) == 0)
				return factory.create(sign ? -1 : +1, BigDecimal.ZERO);
			
			// significand != 0 => subnormal
			exponent = 1 - bias;
			subnormal = true;
			
			// when a number is subnormal, the implicit bit is 0 instead of 1
		}
		
		// exponent is all 1s => infinity, NaN
		else if(exponent == mask(this.exponent).intValue()) {
			// BigDecimal does have neither infinity nor NaN
			
			int signum = sign ? -1 : +1;
			
			if(isInfinity(value)) // significand is zero => infinity
				return factory.create(signum, FloatingType.INFINITE);
			
			return factory.create( // significand is not zero => NaN
				signum,
				isSignalingNaN(value)
					? FloatingType.SIGNALING_NAN // MSB of significand is 1 => sNaN
					: FloatingType.QUIET_NAN		// MSB of significand is 0 => qNaN
			); 
		}
		
		// make exponent unbiased
		else exponent -= bias;
		
		BigDecimal result = BigDecimal.ZERO;
		
		if(implicit) { // add implicit bit, unless subnormal
			if(!subnormal)
				result = result.add(BigDecimal.ONE);
		}
		
		else if(significand.testBit(this.significand)) // add explicit bit
			result = result.add(BigDecimal.ONE);
		
		for(int i = 0; i < this.significand; ++i)
			if(significand.testBit(this.significand - i - 1)) // if bit is set, add appropriate fraction
				result = result.add(pow2(-i - 1));
		
		// scale according to exponent
		result = result.multiply(pow2(exponent));
		
		if(sign) // add sign
			result = result.negate();
		
		return factory.create(sign ? -1 : +1, result);
	}
	
	// create a bit mask with n bits set (e.g. n=4 returns 0b1111)
	private BigInteger mask(int n) {
		return BigInteger.ONE.shiftLeft(n).subtract(BigInteger.ONE);
	}

	// computes 2^n
	private BigDecimal pow2(int n) {
		if(n == 0)
			return BigDecimal.ONE;
		
		if(n < 0)
			return BigDecimalMath.reciprocal(pow2(-n), CONTEXT);
		
		return new BigDecimal(
			BigInteger.ONE.shiftLeft(n),
			0
		).stripTrailingZeros();
	}
	
	// computes 2^(e_min-1)
	private BigDecimal pow2exp1() {
		return memoize(
			MEMOIZE_POWEXP1,
			() -> pow2(getExponentRange().getKey() - 1)
		);
	}
	
	// computes 2^-significand
	private BigDecimal pow2mant() {
		return memoize(
			MEMOIZE_POWMANT,
			() -> pow2(-significand)
		);
	}
	
	private int getOffset() {
		return implicit ? 0 : 1;
	}
	
	/**
	 * Returns the exponent bias
	 * 
	 * @return the bias
	 */
	public int getBias() {
		return (1 << (exponent - 1)) - 1;
	}

	/**
	 * Returns the value's unbiased exponent part
	 * 
	 * @param value the value
	 * @return the unbiased exponent
	 */
	public BigInteger getUnbiasedExponent(BigInteger value) {
		return getExponent(value).subtract(BigInteger.valueOf(getBias()));
	}

	/**
	 * Returns the value's exponent part
	 * 
	 * @param value the value
	 * @return the exponent
	 */
	public BigInteger getExponent(BigInteger value) {
		return value.shiftRight(significand + getOffset()).and(mask(exponent));
	}

	/**
	 * Returns the value's significand part.
	 * <p>If there is an explicit bit, it is not included
	 * 
	 * @param value the value
	 * @return the significand
	 */
	public BigInteger getSignificand(BigInteger value) {
		return value.and(mask(significand));
	}

	/**
	 * Returns the value's significand part.
	 * <p>If there is an explicit bit, it is included
	 * 
	 * @param value the value
	 * @return the significand
	 */
	public BigInteger getFullSignificand(BigInteger value) {
		return value.and(mask(significand + getOffset()));
	}

	/** {@inheritDoc} */
	@Override
	public boolean isPositive(BigInteger value) {
		return !isNegative(value);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isNegative(BigInteger value) {
		return value.testBit(exponent + significand + getOffset());
	}

	/** {@inheritDoc} */
	@Override
	public boolean isInfinity(BigInteger value) {
		if(getExponent(value).compareTo(mask(exponent)) != 0)
			return false;

		return getSignificand(value).compareTo(BigInteger.ZERO) == 0;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isPositiveInfinity(BigInteger value) {
		return isPositive(value) && isInfinity(value);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isNegativeInfinity(BigInteger value) {
		return isNegative(value) && isInfinity(value);
	}	

	/** {@inheritDoc} */
	@Override
	public BigInteger getPositiveInfinity() {
		return memoize(
			MEMOIZE_POS_INF,
			() -> BigInteger.ZERO
				.or(mask(exponent + getOffset()))
				.shiftLeft(significand)
		);
	}

	/** {@inheritDoc} */
	@Override
	public BigInteger getNegativeInfinity() {
		return memoize(
			MEMOIZE_NEG_INF,
			() -> BigInteger.ONE
				.shiftLeft(exponent + getOffset())
				.or(mask(exponent + getOffset()))
				.shiftLeft(significand)
		);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isNaN(BigInteger value) {
		if(getExponent(value).compareTo(mask(exponent)) != 0)
			return false;
		
		return getSignificand(value).compareTo(BigInteger.ZERO) != 0;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isQuietNaN(BigInteger value) {
		return isNaN(value)
			&& getSignificand(value).testBit(significand - 1);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isSignalingNaN(BigInteger value) {
		return isNaN(value)
			&& !getSignificand(value).testBit(significand - 1);
	}

	/** {@inheritDoc} */
	@Override
	public BigInteger getQuietNaN(int signum) {
		return BigInteger.ZERO
			.or(mask(exponent + getOffset()))
			.shiftLeft(1)
			.or(BigInteger.ONE)
			.shiftLeft(significand - 1)
			.or(BigInteger.ONE)
			.multiply(BigInteger.valueOf(signum));
	}

	/** {@inheritDoc} */
	@Override
	public BigInteger getSignalingNaN(int signum) {
		return BigInteger.ZERO
			.or(mask(exponent + getOffset()))
			.shiftLeft(significand)
			.or(BigInteger.ONE)
			.multiply(BigInteger.valueOf(signum));
	}

	/** {@inheritDoc} */
	@Override
	public BigInteger getNaN(int signum) {
		return BigInteger.ZERO
			.or(mask(exponent + significand + getOffset()))
			.multiply(BigInteger.valueOf(signum));
	}

	/** {@inheritDoc} */
	@Override
	public BigInteger getZero(int signum) {
		return signum == -1
			? BigInteger.ONE
				.shiftLeft(exponent + significand + getOffset())
			: BigInteger.ZERO;
	}

	/** {@inheritDoc} */
	@Override
	public T getMinSubnormalValue() {
		// 2^(e_min - 1) * 2^-p 	[e_min < 0]
		return memoize(
			MEMOIZE_SMINVAL,
			() -> factory.create(1, pow2exp1().multiply(pow2mant()))
		);
	}

	/** {@inheritDoc} */
	@Override
	public T getMinValue() {
		// 2^(e_min - 1) 	[e_min < 0]
		return memoize(
			MEMOIZE_MIN_VAL,
			() -> factory.create(pow2exp1())
		);
	}

	/** {@inheritDoc} */
	@Override
	public T getMaxValue() {
		// (2 - 2^-p) * 2^e_max 	[e_max > 0]
		return memoize(
			MEMOIZE_MAX_VAL,
			() -> factory.create(
				1,
				BigDecimal.TWO
					.subtract(pow2mant())
					.multiply(pow2(getExponentRange().getValue() - 1))
			)
		);
	}

	/** {@inheritDoc} */
	@Override
	public BigDecimal getEpsilon() {
		return memoize(
			MEMOIZE_EPSILON,
			() -> {
				BigInteger rawOne = BigInteger.ZERO
					.or(mask(exponent - 1))
					.shiftLeft(significand + getOffset());
				
				if(!implicit)
					rawOne = rawOne.or(BigInteger.ONE.shiftLeft(significand - 1 + getOffset()));
				
				// smallest number greater than 1
				BigDecimal one = decode(rawOne.or(BigInteger.ONE)).getBigDecimal();
				
				return one.subtract(BigDecimal.ONE);
			}
		);
	}

	/** {@inheritDoc} */
	@Override
	public Map.Entry<Integer, Integer> getExponentRange() {
		return memoize(
			MEMOIZE_EXRANGE,
			() -> {
				int bias = getBias();
				
				return Map.entry(
					2 - bias,
					(1 << exponent) - 1 - bias
				);
			}
		);
	}
	
	/**
	 * Returns the smallest and largest possible exponent so that 10 to the power of the exponent is a normalized number
	 * 
	 * @return the smallest and largest exponent
	 */
	public Map.Entry<Integer, Integer> get10ExponentRange() {
		return memoize(
			MEMOIZE_EX10RNG,
			() -> {
				BigDecimal min = getMinValue().getBigDecimal();
				BigDecimal max = getMaxValue().getBigDecimal();
				
				return Map.entry(
					min.precision() - min.scale() - 1, // ceil(log10(min))
					max.precision() - max.scale() - 1  // floor(log10(max))
				);
			}
		);
	}
	
	/**
	 * Computes the number of decimal digits that can be converted back and forth without loss of precision
	 * 
	 * @return the number of decimal digits
	 */
	public int getDecimalDigits() {
		// floor( (p - 1) * log10(b) )
		return memoize(
			MEMOIZE_DECDIGS,
			() -> BigDecimal.valueOf(significand - 1 + getOffset())
				.multiply(LOG10_2).round(FLOOR).intValue()
		);
	}
	
}
