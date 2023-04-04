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
package at.syntaxerror.ieee754;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

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
public final class BinaryCodec<T extends Binary<T>> {

	private static final MathContext BIGCTX = new MathContext(607, RoundingMode.HALF_EVEN);
	private static final MathContext CTX = MathContext.UNLIMITED;
	private static final MathContext FLOOR = new MathContext(0, RoundingMode.FLOOR);
	
	private static final BigDecimal TWO = BigDecimal.valueOf(2);
	private static final BigDecimal LOG10_2 = BigDecimalMath.log10(TWO, BIGCTX);

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
	private final int mantissa;
	private final boolean implicit;
	
	private final BinaryFactory<T> factory;
	
	private final Map<Integer, Object> memoized = new HashMap<>();
	
	/**
	 * Creates a new binary codec
	 * 
	 * @param mantissa the number of mantissa bits ({@code > 0})
	 * @param exponent the number of exponent bits ({@code > 0}, {@code < 32})
	 * @param implicit whether there is an implicit mantissa bit
	 * @param factory the factory for creating {@link Binary} objects
	 */
	public BinaryCodec(int exponent, int mantissa, boolean implicit, @NonNull BinaryFactory<T> factory) {
		if(exponent < 1) throw new IllegalArgumentException("Illegal non-positive exponent size");
		if(mantissa < 1) throw new IllegalArgumentException("Illegal non-positive mantissa size");
 
		if(exponent > 31) throw new IllegalArgumentException("Exponent size is too big");

		this.exponent = exponent;
		this.mantissa = mantissa;
		this.implicit = implicit;
		this.factory = factory;
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
	 * Returns the number of bits occupied by the mantissa
	 * 
	 * @return the number of mantissa's bits 
	 */
	public int getMantissaBits() {
		return mantissa;
	}
	
	/**
	 * Returns whether there is an implicit bit used for the binary representation
	 * 
	 * @return whether there is an implicit bit
	 */
	public boolean isImplicit() {
		return implicit;
	}
	
	/**
	 * Encodes the floating point into its byte representation
	 * 
	 * @param value the floating point number
	 * @return the encoded byte representation
	 */
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
			
		}
		
		BigDecimal bigdec = value.getBigDecimal();
		
		if(bigdec.compareTo(BigDecimal.ZERO) == 0)
			return getZero(0);
		
		BigDecimal absdec = bigdec.abs();
		
		// determine sign bit
		int sign = bigdec.signum() == -1 ? 1 : 0;
		
		if(absdec.compareTo(getMaxValue().getBigDecimal()) > 0) // out of range => Infinity
			return sign == -1
				? getNegativeInfinity()
				: getPositiveInfinity();
		
		if(absdec.compareTo(getMinSubnormalValue().getBigDecimal()) < 0) // out of range => 0
			return getZero(sign);
		
		// strip fractional part
		BigInteger mantissa = bigdec.toBigInteger();
		
		// strip integer part
		BigDecimal fraction = bigdec.subtract(new BigDecimal(mantissa, CTX), CTX).abs();
		
		// remove sign
		mantissa = mantissa.abs();

		// unbiased exponent, or 0 if exponent would be negative
		int exp = mantissa.bitLength();
		
		boolean subnormal = false;
		
		if(absdec.compareTo(getMinValue().getBigDecimal()) < 0) {
			// subnormal
			
			subnormal = true;
			exp = 0;
			
			// remove factor 2^-e_min
			fraction = fraction.multiply(
				BigDecimalMath.pow(
					BigDecimal.TWO,
					1 - getExponentRange().getKey(),
					BIGCTX
				),
				BIGCTX
			);
		}
		
		int n = 0;
		
		/* - left-shift the mantissa by 1
		 * - multiply the fraction by two
		 * - bit-or the mantissa with the integer part of result (0 or 1)
		 * - remove the integer part from the result
		 * - repeat until the fraction is zero, or all mantissa bits are used up
		 * 
		 * while(fraction != 0) {
		 * 	   mantissa <<= 1;
		 *     fraction *= 2;
		 *     mantissa |= (int) fraction;
		 *     fraction -= (int) fraction;
		 * }
		 */
		while(fraction.compareTo(BigDecimal.ZERO) != 0) {
			fraction = fraction.multiply(TWO, CTX);
			
			int integerPart = fraction.intValue();
			
			// max number of bits reached
			if(n > this.mantissa - exp) {
				
				// if next bit would have been a 1, round up
				if(integerPart == 1) {
					
					// don't round if last bit was 0 and next bits would have been 1 without any other following non-zero bits (1000000...)
					if(!mantissa.testBit(0) && fraction.compareTo(BigDecimal.ONE) == 0)
						break;
					
					mantissa = mantissa.add(BigInteger.ONE);
				}
				
				break;
			}
			
			++n;

			mantissa = mantissa.shiftLeft(1);

			if(integerPart == 1) {
				mantissa = mantissa.or(BigInteger.ONE);
				fraction = fraction.subtract(BigDecimal.ONE, CTX);
			}
		}
		
		if(subnormal)
			return mantissa.compareTo(BigInteger.ZERO) == 0
				? getZero(sign)
				: BigInteger.valueOf(sign)
					.shiftLeft(this.exponent + this.mantissa)
					.or(mantissa.shiftLeft(this.mantissa - n));
		
		// fix exponent (negative exponent)
		if(exp == 0)
			exp = mantissa.bitLength() - n - 1;
		else --exp;

		int off = 0;
		
		// clear most significant bit if it is implicit
		if(implicit) {
			int bit = mantissa.bitLength() - 1;
			
			if(bit >= 0)
				mantissa = mantissa.clearBit(bit);
		}
		else off = 1;
		
		return BigInteger.valueOf(sign) 
			// add bias to exponent
			.shiftLeft(this.exponent)
			.or(BigInteger.valueOf(exp + getBias()))
			// align mantissa to the left
			.shiftLeft(this.mantissa)
			.or(mantissa.shiftLeft(this.mantissa - n - exp - off));
	}
	
	/**
	 * Decodes the floating point's byte representation
	 * 
	 * @param value the byte representation
	 * @return the decoded floating point number
	 */
	public T decode(BigInteger value) {
		// extract sign (most significant bit)
		boolean sign = isNegative(value);
		
		// extract mantissa
		BigInteger mantissa = getMantissa(value);
		
		// extract (biased) exponent
		int exponent = getExponent(value).intValue();
		
		// calculate exponent bias
		int bias = getBias();
		
		boolean subnormal = false;
		
		// exponent is all 0s => signed zero, subnormals
		if(exponent == 0) {
			// mantissa == 0 => signed zero
			if(mantissa.compareTo(BigInteger.ZERO) == 0)
				return factory.create(BigDecimal.ZERO); // BigDecimal doesn't have signed zeros
			
			// mantissa != 0 => subnormal
			exponent = 1 - bias;
			subnormal = true;
			
			// when a number is subnormal, the implicit bit is 0 instead of 1
		}
		
		// exponent is all 1s => infinity, NaN
		else if(exponent == mask(this.exponent).intValue()) {
			// BigDecimal does have neither infinity nor NaN
			
			int signum = sign ? 1 : -1;
			
			if(mantissa.compareTo(BigInteger.ZERO) == 0) // mantissa is zero => infinity
				return factory.create(signum, BinaryType.INFINITE);
			
			return factory.create( // mantissa is not zero => NaN
				signum,
				mantissa.testBit(this.mantissa - 1)
					? BinaryType.SIGNALING_NAN // MSB of mantissa is 1 => sNaN
					: BinaryType.QUIET_NAN		// MSB of mantissa is 0 => qNaN
			); 
		}
		
		// make exponent unbiased
		else exponent -= bias;
		
		// integer part's offset. offset is decreased by 1 when most significant bit is implicit
		int off = this.mantissa - exponent - (implicit ? 0 : 1);
		
		// extract integer part from mantissa
		BigInteger integer = mantissa.shiftRight(off);
		
		BigDecimal result = null;
		
		if(implicit && !subnormal) { // add implicit bit, unless subnormal
			if(exponent < 0) // add 1/(2^(-exponent)) [effectively equal to 2^exponent]
				result = new BigDecimal(integer, CTX)
					.add(pow2negative(-exponent), CTX);
			
			// add 2^exponent
			else integer = integer.or(BigInteger.ONE.shiftLeft(exponent));
		}
		
		if(result == null)
			result = new BigDecimal(integer, CTX);

		for(int i = 0; i < off; ++i)
			if(mantissa.testBit(off - i - 1)) // if bit is set, add appropriate fraction
				result = result.add(pow2negative(i + 1), CTX);
		
		if(sign) // add sign
			result = result.negate(CTX);
		
		return factory.create(result);
	}
	
	// create a bit mask with n bits set (e.g. n=4 returns 0b1111)
	private BigInteger mask(int n) {
		return BigInteger.ONE.shiftLeft(n).subtract(BigInteger.ONE);
	}

	// computes 2^-n where n is positive
	private BigDecimal pow2negative(int n) {
		return BigDecimalMath.pow(BigDecimal.TWO, -n, BIGCTX);
		// return BigDecimal.ONE.divide(new BigDecimal(BigInteger.ONE.shiftLeft(n)), getDecimalDigits() * 2, RoundingMode.HALF_UP);
	}
	
	// computes 2^(e_min-1)
	private BigDecimal pow2exp1() {
		return memoize(
			MEMOIZE_POWEXP1,
			() -> pow2negative(1 - getExponentRange().getKey())
		);
	}
	
	// computes 2^-mantissa
	private BigDecimal pow2mant() {
		return memoize(
			MEMOIZE_POWMANT,
			() -> pow2negative(mantissa)
		);
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
		return value.shiftRight(mantissa).and(mask(exponent));
	}

	/**
	 * Returns the value's mantissa part
	 * 
	 * @param value the value
	 * @return the mantissa
	 */
	public BigInteger getMantissa(BigInteger value) {
		return value.and(mask(mantissa));
	}

	/**
	 * Checks if the value is positive
	 * 
	 * @param value the value
	 * @return whether the value is positive
	 */
	public boolean isPositive(BigInteger value) {
		return !isNegative(value);
	}

	/**
	 * Checks if the value is negative
	 * 
	 * @param value the value
	 * @return whether the value is negative
	 */
	public boolean isNegative(BigInteger value) {
		return value.testBit(exponent + mantissa);
	}

	/**
	 * Checks if the value is {@code Infinity}'s binary representation
	 * 
	 * @param value the value
	 * @return whether the value is {@code Infinity}
	 */
	public boolean isInfinity(BigInteger value) {
		if(getExponent(value).compareTo(mask(exponent)) != 0)
			return false;

		return getMantissa(value).compareTo(BigInteger.ZERO) == 0;
	}

	/**
	 * Checks if the value is {@code +Infinity}'s binary representation
	 * 
	 * @param value the value
	 * @return whether the value is {@code +Infinity}
	 */
	public boolean isPositiveInfinity(BigInteger value) {
		return isPositive(value) && isInfinity(value);
	}

	/**
	 * Checks if the value is {@code -Infinity}'s binary representation
	 * 
	 * @param value the value
	 * @return whether the value is {@code -Infinity}
	 */
	public boolean isNegativeInfinity(BigInteger value) {
		return isNegative(value) && isInfinity(value);
	}	
	
	/**
	 * Returns {@code +Infinity}'s (like {@link Double#POSITIVE_INFINITY}) binary representation
	 * 
	 * @return {@code +Infinity}
	 */
	public BigInteger getPositiveInfinity() {
		return memoize(
			MEMOIZE_POS_INF,
			() -> BigInteger.ZERO
				.shiftLeft(exponent)
				.or(mask(exponent))
				.shiftLeft(mantissa)
		);
	}

	/**
	 * Returns {@code -Infinity}'s (like {@link Double#NEGATIVE_INFINITY}) binary representation
	 * 
	 * @return {@code -Infinity}
	 */
	public BigInteger getNegativeInfinity() {
		return memoize(
			MEMOIZE_NEG_INF,
			() -> BigInteger.ONE
				.shiftLeft(exponent)
				.or(mask(exponent))
				.shiftLeft(mantissa)
		);
	}

	/**
	 * Checks if the value is {@code NaN}'s binary representation
	 * 
	 * @param value the value
	 * @return whether the value is {@code NaN}
	 */
	public boolean isNaN(BigInteger value) {
		if(getExponent(value).compareTo(mask(exponent)) != 0)
			return false;
		
		return getMantissa(value).compareTo(BigInteger.ZERO) != 0;
	}

	/**
	 * Checks if the value is {@code qNaN}'s binary representation
	 * 
	 * @param value the value
	 * @return whether the value is {@code qNaN}
	 */
	public boolean isQuietNaN(BigInteger value) {
		return isNaN(value)
			&& getMantissa(value).testBit(mantissa - 1);
	}
	
	/**
	 * Checks if the value is {@code sNaN}'s binary representation
	 * 
	 * @param value the value
	 * @return whether the value is {@code sNaN}
	 */
	public boolean isSignalingNaN(BigInteger value) {
		return isNaN(value)
			&& !getMantissa(value).testBit(mantissa - 1);
	}
	
	/**
	 * Returns {@code qNaN} binary representation (on most processors)
	 * 
	 * @param signum the signum
	 * @return {@code qNaN}
	 */
	public BigInteger getQuietNaN(int signum) {
		return BigInteger.ZERO
			.shiftLeft(exponent)
			.or(mask(exponent))
			.shiftLeft(1)
			.or(BigInteger.ONE)
			.shiftLeft(mantissa - 1)
			.or(BigInteger.ONE)
			.multiply(BigInteger.valueOf(signum));
	}

	/**
	 * Returns {@code sNaN}'s binary representation (on most processors)
	 * 
	 * @param signum the signum
	 * @return {@code sNaN}
	 */
	public BigInteger getSignalingNaN(int signum) {
		return BigInteger.ZERO
			.shiftLeft(exponent)
			.or(mask(exponent))
			.shiftLeft(mantissa)
			.or(BigInteger.ONE)
			.multiply(BigInteger.valueOf(signum));
	}

	/**
	 * Returns {@code NaN} ({@code qNaN} on most processor; with all mantissa bits set)
	 * 
	 * @return {@code NaN}
	 */
	public BigInteger getNaN(int signum) {
		return BigInteger.ZERO
			.shiftLeft(exponent)
			.or(mask(exponent))
			.shiftLeft(mantissa)
			.or(mask(mantissa))
			.multiply(BigInteger.valueOf(signum));
	}

	/**
	 * Returns (possibly negative) zero's binary representation
	 * 
	 * @return zero
	 */
	public BigInteger getZero(int signum) {
		return signum == -1
			? BigInteger.ONE
				.shiftLeft(exponent + mantissa)
			: BigInteger.ZERO;
	}
	
	/**
	 * Returns the smallest postive ({@code > 0}) subnormal value
	 * 
	 * @return the smallest postive value
	 */
	public T getMinSubnormalValue() {
		// 2^(e_min - 1) * 2^-p 	[e_min < 0]
		return memoize(
			MEMOIZE_SMINVAL,
			() -> factory.create(pow2exp1().multiply(pow2mant()))
		);
	}
	
	/**
	 * Returns the smallest postive ({@code > 0}) normalized value.
	 * 
	 * @return the smallest postive value
	 */
	public T getMinValue() {
		// 2^(e_min - 1) 	[e_min < 0]
		return memoize(
			MEMOIZE_MIN_VAL,
			() -> factory.create(pow2exp1())
		);
	}

	/**
	 * Returns the largest possible value
	 * 
	 * @return the largest value
	 */
	public T getMaxValue() {
		// (2 - 2^-p) * 2^e_max 	[e_max > 0]
		return memoize(
			MEMOIZE_MAX_VAL,
			() -> factory.create(
				BigDecimal.TWO
					.subtract(pow2mant())
					.multiply(
						BigDecimalMath.pow(
							BigDecimal.TWO,
							getExponentRange().getValue() - 1,
							BIGCTX
						)
					)
			)
		);
	}
	
	/**
	 * Returns the difference between 1 and the smallest number greater than 1
	 * 
	 * @return the difference 
	 */
	public BigDecimal getEpsilon() {
		return memoize(
			MEMOIZE_EPSILON,
			() -> {
				BigInteger rawOne = BigInteger.ZERO
					.or(mask(exponent - 1))
					.shiftLeft(mantissa);
				
				if(!implicit)
					rawOne = rawOne.or(BigInteger.ONE.shiftLeft(mantissa - 1));
				
				// smallest number greater than 1
				BigDecimal one = decode(rawOne.or(BigInteger.ONE)).getBigDecimal();
				
				return one.subtract(BigDecimal.ONE);
			}
		);
	}
	
	/**
	 * Returns the smallest and largest possible exponent
	 * 
	 * @return the smallest and largest exponent
	 */
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
					max.precision() - max.scale() - 1  // ceil(log10(max))
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
			() -> BigDecimal.valueOf(mantissa - 1).multiply(LOG10_2).round(FLOOR).intValue()
		);
	}
	
}
