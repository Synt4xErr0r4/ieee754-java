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
package at.syntaxerror.ieee754.decimal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import at.syntaxerror.ieee754.FloatingCodec;
import at.syntaxerror.ieee754.FloatingFactory;
import at.syntaxerror.ieee754.FloatingType;
import at.syntaxerror.ieee754.binary.Binary;
import at.syntaxerror.ieee754.rounding.Rounding;
import lombok.NonNull;

/**
 * This class represents a codec capable of encoding and decoding IEEE 754 decimal floating point numbers
 * as well as computing common values (such as NaN, maximum value, ...)
 * 
 * @author Thomas Kasper
 * 
 */
public class DecimalCodec<T extends Decimal<T>> extends FloatingCodec<T> {

	private static final BigInteger MASK_SPECIAL = BigInteger.valueOf(0b111100);
	private static final BigInteger MASK_HIGH = BigInteger.valueOf(0b110000);

	private static final BigInteger MASK_INFINITY = BigInteger.valueOf(0b11110);
	private static final BigInteger MASK_NAN = BigInteger.valueOf(0b11111);
	private static final BigInteger MASK_NEGATIVE = BigInteger.valueOf(0b100000);
	
	private static final BigInteger THOUSAND = BigInteger.valueOf(1000);
	private static final BigInteger COMBINATION_LARGE_DPD = BigInteger.valueOf(0b11000);
	private static final BigInteger COMBINATION_LARGE_BID = BigInteger.valueOf(0b11);

	private static final int MEMOIZE_EXPBIAS = 0;
	private static final int MEMOIZE_SIGDIGS = 1;
	private static final int MEMOIZE_POS_INF = 2;
	private static final int MEMOIZE_NEG_INF = 3;
	private static final int MEMOIZE_SMINVAL = 4;
	private static final int MEMOIZE_MIN_VAL = 5;
	private static final int MEMOIZE_MAX_VAL = 6;
	private static final int MEMOIZE_EXRANGE = 7;
	private static final int MEMOIZE_EPSILON = 8;
	
	private final int combination;
	private final int significand;
	
	private final FloatingFactory<T> factory;
	
	private final Map<Integer, Object> memoized = new HashMap<>();
	
	/**
	 * Creates a new decimal codec
	 * 
	 * @param combination the number of combination bits ({@code > 0}, {@code < 32})
	 * @param significand the number of significand bits ({@code > 0})
	 * @param factory the factory for creating {@link Binary} objects
	 */
	public DecimalCodec(int combination, int significand, @NonNull FloatingFactory<T> factory) {
		if(combination < 6) throw new IllegalArgumentException("Illegal combination size < 6");
		if(significand < 1) throw new IllegalArgumentException("Illegal non-positive significand size");

		if((significand % 10) != 0) throw new IllegalArgumentException("Significand size must be a multiple of 10");
		
		if(combination > 31) throw new IllegalArgumentException("Combination size is too big");

		this.combination = combination;
		this.significand = significand;
		this.factory = factory;

		// memoize values
		getSignificandDigits();
		getBias();
		initialize();
	}

	@SuppressWarnings("unchecked")
	private <R> R memoize(int id, Supplier<R> generator) {
		Object value = memoized.get(id);
		
		if(value == null)
			memoized.put(id, value = generator.get());
		
		return (R) value;
	}
	
	/**
	 * Returns the number of bits occupied by the combination field
	 * 
	 * @return the number of combination field's bits 
	 */
	public int getCombinationBits() {
		return combination;
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
	 * Encodes the floating point into its binary representation using the representation method specified by {@link Decimal#DEFAULT_CODING}.
	 * 
	 * @param value the floating point number
	 * @return the encoded binary representation
	 */
	@Override
	public BigInteger encode(T value) {
		return Decimal.DEFAULT_CODING == DecimalCoding.DENSLY_PACKED_DECIMAL
				? encodeDPD(value)
				: encodeBID(value);
	}

	/**
	 * Encodes the floating point into its binary representation using the binary integer decimal representation method
	 * 
	 * @param value the floating point number
	 * @return the encoded binary representation
	 */
	public BigInteger encodeBID(T value) {
		EncodeInfo info = encodeCommon(value);
		
		if(info.special())
			return info.value();

		BigInteger unscaled = info.value().abs();
		BigInteger encoded = unscaled.and(mask(significand));
		
		int mostSignificant = unscaled.shiftRight(significand).intValue();
		
		int scale = info.scale() + getBias();
		int shift = 0;
		
		BigInteger combination;
		
		if(mostSignificant > 7) {
			combination = COMBINATION_LARGE_BID;
			shift = 1;
		}
		else {
			combination = BigInteger.ZERO;
			shift = 3;
		}
		
		combination = combination.shiftLeft(this.combination - 3)
			.or(BigInteger.valueOf(scale))
			.shiftLeft(shift)
			.or(BigInteger.valueOf(mostSignificant & 7));
		
		encoded = encoded.or(combination.shiftLeft(significand));
		
		if(value.isNegative())
			encoded = encoded.or(BigInteger.ONE.shiftLeft(significand + this.combination));
		
		return encoded;
	}

	/**
	 * Encodes the floating point into its binary representation using the densly packed decimal representation method
	 * 
	 * @param value the floating point number
	 * @return the encoded binary representation
	 */
	public BigInteger encodeDPD(T value) {
		EncodeInfo info = encodeCommon(value);
		
		if(info.special())
			return info.value();
		
		BigInteger unscaled = info.value().abs();
		BigInteger encoded = BigInteger.ZERO;
		
		for(int i = 0; i < significand; i += 10) {
			int[] digs = getLeastSignificantDigits(unscaled);
			
			unscaled = unscaled.divide(THOUSAND);
			
			encoded = encoded.or(
				BigInteger.valueOf(
					encodeDPDBlock(digs[2], digs[1], digs[0])
				).shiftLeft(i)
			);
		}
		
		int mostSignificant = getLeastSignificantDigits(unscaled)[0];
		
		int scale = info.scale() + getBias();
		
		BigInteger expHigh = BigInteger.valueOf(scale >> (combination - 5));
		BigInteger expLow = BigInteger.valueOf(scale).and(mask(combination - 5));
		
		BigInteger combination;
		
		if(mostSignificant > 7)
			combination = COMBINATION_LARGE_DPD.or(expHigh.shiftLeft(1));
		else combination = expHigh.shiftLeft(3);
		
		combination = combination.or(BigInteger.valueOf(mostSignificant & 7))
			.shiftLeft(this.combination - 5)
			.or(expLow);
		
		encoded = encoded.or(combination.shiftLeft(significand));
		
		if(value.isNegative())
			encoded = encoded.or(BigInteger.ONE.shiftLeft(significand + this.combination));
		
		return encoded;
	}
	
	// returns the 3 least significant base-10 digits
	private int[] getLeastSignificantDigits(BigInteger value) {
		int[] digs = new int[3];
		
		for(int i = 0; i < 3; ++i) {
			// least significant digit = value - 10 * floor(value / 10)
			
			BigInteger cleared = value.divide(BigInteger.TEN);
			
			digs[i] = value.subtract(cleared.multiply(BigInteger.TEN)).intValue();
			
			value = cleared;
		}
		
		return digs;
	}
	
	private static final int[] DPD_MASKS = {
		// s = small digit (0-7), l = large digit (8-9)
		0b0000000,	// sss 		xx 0xx
		0b0001000,	// ssl		xx 100
		0b0001010,	// sls		xx 101
		0b1001110,	// sll		10 111
		0b0001100,	// lss		xx 110
		0b0101110,	// lsl		01 111
		0b0001110,	// lls		00 111
		0b1101110,	// lll		11 111
	};

	private int encodeDPDBlock(int a, int b, int c) {
		boolean largeA = a > 7;
		boolean largeB = b > 7;
		boolean largeC = c > 7;
		
		a &= 7;
		b &= 7;
		c &= 7;
		
		int encoded = a << 7;
		
		if(!largeA && !largeB && !largeC)
			return encoded| (b << 4) | c;
		
		encoded |= (c & 1) | ((b & 1) << 4);

		if(!largeC)
			encoded |= (c & 0b110) << (largeA ? 7 : 4);
		
		if(!largeB)
			encoded |= (b & 0b110) << (largeA && largeC ? 7 : 4);

		return encoded | DPD_MASKS[
			(largeA ? 0b100 : 0) |
			(largeB ? 0b010 : 0) |
			(largeC ? 0b001 : 0)
		];
	}
	
	private EncodeInfo encodeCommon(T value) {
		BigInteger result;
		int scale = 0;
		boolean special;
		
		if(!value.isFinite() || value.isZero()) {
			special = true;
			
			int signum = value.getSignum();
			
			if(value.isPositiveInfinity())
				result = getPositiveInfinity();
			
			else if(value.isNegativeInfinity())
				result = getNegativeInfinity();
			
			else if(value.isQuietNaN())
				result = getQuietNaN(signum);
				
			else if(value.isSignalingNaN())
				result = getSignalingNaN(signum);
			
			else result = getZero(signum);
		}
		
		else {
			special = false;
			
			BigDecimal bigdec = value.getBigDecimal().stripTrailingZeros();
			
			scale = bigdec.scale();
			int prec = bigdec.precision();
			int max = getSignificandDigits();
			
			if(prec > max) { // truncate least significant digits
				bigdec = truncateLeastSignificant(bigdec, prec - max);
				
				scale = bigdec.scale();
				prec = bigdec.precision();
			}
			
			result = bigdec.unscaledValue();
			scale = -scale;
			
			int maxExp = getExponentSpan() >> 1;
			int minExp = 2 - maxExp - max;
			
			if(scale > maxExp) { // overflow => infinity
				special = true;
				result = value.getSignum() == -1
					? getNegativeInfinity()
					: getPositiveInfinity();
			}
			else if(scale < 2 - maxExp - max) {
				// truncate least significant digits
				bigdec = truncateLeastSignificant(bigdec, minExp - scale);
				
				result = bigdec.unscaledValue();
				scale = -bigdec.scale();
			}
		}
		
		return new EncodeInfo(special, result, scale);
	}
	
	// truncate the n least significant digits of the value
	private BigDecimal truncateLeastSignificant(BigDecimal value, int n) {
		BigDecimal precise = new BigDecimal(
			value.unscaledValue()
			.divide(BigInteger.TEN.pow(n - 1))
		).divide(BigDecimal.TEN);
		
		return new BigDecimal(
			Rounding.DEFAULT_ROUNDING
				.roundDecimal(precise)
				.toBigInteger(),
			value.scale() - n
		).stripTrailingZeros();
	}

	/**
	 * Decodes the floating point's binary representation using the representation method specified by {@link Decimal#DEFAULT_CODING}.
	 * 
	 * @param value the binary representation
	 * @return the decoded floating point number
	 */
	@Override
	public T decode(BigInteger value) {
		return Decimal.DEFAULT_CODING == DecimalCoding.DENSLY_PACKED_DECIMAL
			? decodeDPD(value)
			: decodeBID(value);
	}

	/**
	 * Decodes the floating point's binary representation using the binary integer decimal representation method
	 * 
	 * @param value the binary representation
	 * @return the decoded floating point number
	 */
	public T decodeBID(BigInteger value) {
		// extract sign (most significant bit)
		boolean sign = isNegative(value);
		
		// extract combination field
		BigInteger combination = getCombination(value);
		
		// extract 6 MSB of combination
		BigInteger combId = combination.shiftRight(this.combination - 6);
		
		// extract significand
		BigInteger significand = getSignificand(value);
		
		var info = decodeCommon(combId, sign);
		
		if(info.special())
			return info.specialValue();
		
		int exponent;
		int digit;
		
		BigInteger exponentMask = mask(this.combination - 3);
		
		if(info.high()) { // extract MSB for significand (1 bit, with implicit '100' prepended) and exponent
			digit = 0b1000 | combination.and(BigInteger.ONE).intValue();
			
			exponent = combination.shiftRight(1)
				.and(exponentMask)
				.intValue();
		}
		else { // extract 3 MSB for significand and exponent
			digit = combination
				.and(BigInteger.valueOf(0b111))
				.intValue();

			exponent = combination.shiftRight(3)
				.and(exponentMask)
				.intValue();
		}
		
		significand = BigInteger.valueOf(digit)
			.shiftLeft(this.significand)
			.or(significand);
		
		if(getDigits(significand) > getSignificandDigits())
			significand = BigInteger.ZERO; // treat significand as 0 if out of range
		
		// 10^(exponent-bias) * significand
		BigDecimal result = new BigDecimal(significand)
			.multiply(pow10(exponent - getBias()))
			.stripTrailingZeros();
		
		if(sign)
			result = result.negate();
		
		return factory.create(sign ? -1 : +1, result);
	}

	/**
	 * Decodes the floating point's binary representation using the densly packed decimal representation method
	 * 
	 * @param value the binary representation
	 * @return the decoded floating point number
	 */
	public T decodeDPD(BigInteger value) {
		// extract sign (most significant bit)
		boolean sign = isNegative(value);
		
		// extract combination field
		BigInteger combination = getCombination(value);
		
		// extract 6 MSB of combination
		BigInteger combId = combination.shiftRight(this.combination - 6);
		
		// extract significand
		BigInteger significand = getSignificand(value);
		
		var info = decodeCommon(combId, sign);
		
		if(info.special())
			return info.specialValue();
		
		int exponent;
		int digit;
		
		BigInteger exponentMask = mask(this.combination - 5);
		
		if(info.high()) { // extract first significand digit (1 bit, range 8 to 9) and exponent
			digit = 0b1000 | combId.shiftRight(1).and(BigInteger.ONE).intValue();
			
			exponent = combination
				.and(exponentMask)
				.or(
					combination
						.shiftRight(this.combination - 4)
						.and(BigInteger.valueOf(0b11))
						.shiftLeft(this.combination - 5)
				)
				.intValue();
		}
		else { // extract first significand digit (3 bits) and exponent
			digit = combId
				.shiftRight(1)
				.and(BigInteger.valueOf(0b111))
				.intValue();

			exponent = combination
				.and(exponentMask)
				.or(
					combination
						.shiftRight(this.combination - 2)
						.shiftLeft(this.combination - 5)
				)
				.intValue();
		}
		
		BigInteger trueSignificand = BigInteger.valueOf(digit);
		
		for(int i = 0; i < this.significand; i += 10)
			trueSignificand = trueSignificand
				.multiply(BigInteger.valueOf(1000))
				.add(BigInteger.valueOf(
					decodeDPDBlock(
						significand.shiftRight(this.significand - i - 10)
							.and(mask(10))
							.intValue()
					)
				));
		
		// 10^(exponent-bias) * significand
		BigDecimal result = new BigDecimal(trueSignificand)
			.multiply(pow10(exponent - getBias()))
			.stripTrailingZeros();
		
		if(sign)
			result = result.negate();
		
		return factory.create(sign ? -1 : +1, result);
	}
	
	private int decodeDPDBlock(int block) {
		if((block & 0b1000) == 0)
			return (block & 7)
				+ 10 * ((block >> 4) & 7)
				+ 100 * ((block >> 7) & 7);
		
		int c = block & 1;
		int b = (block >> 4) & 1;
		int a = (block >> 7) & 1;

		int modeC = (block >> 1) & 3;
		int modeB = (block >> 5) & 3;
		
		int part1 = modeB << 1;
		int part2 = ((block >> 8) & 3) << 1;
		
		if(modeC == 1)
			c |= part1;
		else if(modeC == 2 || (modeC == 3 && modeB == 0))
			c |= part2;
		else c |= 8;
		
		if((modeC & 1) == 0)
			b |= part1;
		else if(modeC == 3 && modeB == 1)
			b |= part2;
		else b |= 8;
		
		if((modeC & 2) == 0 || (modeC == 3 && modeB == 2))
			a |= part2;
		else a |= 8;
		
		return 100 * a + 10 * b + c;
	}
	
	// decode NaN and Infinity
	private DecodeInfo<T> decodeCommon(BigInteger combination, boolean sign) {
		
		boolean high = false;
		boolean special = false;
		
		T specialValue = null;
		
		// if combination's 4 MSB are 1111, the value is Infinity or NaN
		if(combination.and(MASK_SPECIAL).compareTo(MASK_SPECIAL) == 0) {
			special = true;
			
			int signum = sign ? -1 : +1;
			
			if(combination.testBit(1)) // 5th MSB is 1 ==> NaN
				specialValue = factory.create(
					signum,
					combination.testBit(0) // 6th MSB is 1 ==> sNaN
						? FloatingType.SIGNALING_NAN
						: FloatingType.QUIET_NAN
				);
			
			else specialValue = factory.create(signum, FloatingType.INFINITE);
		}
		else high = combination.and(MASK_HIGH).compareTo(MASK_HIGH) == 0;
		
		return new DecodeInfo<T>(high, special, specialValue);
	}
	
	// create a bit mask with n bits set (e.g. n=4 returns 0b1111)
	private BigInteger mask(int n) {
		return BigInteger.ONE.shiftLeft(n).subtract(BigInteger.ONE);
	}
	
	// computes 10^n
	private BigDecimal pow10(int n) {
		if(n == 0)
			return BigDecimal.ONE;
		
		if(n < 0)
			return BigDecimal.ONE.divide(pow10(-n));
		
		return BigDecimal.TEN.pow(n);
	}
	
	// returns the number of digits
	private int getDigits(BigInteger number) {
		// floor(log10(number))
		
		BigDecimal dec = new BigDecimal(number);
		
		dec = dec.stripTrailingZeros();
		return dec.precision() - dec.scale();
	}
	
	// returns the number of encodable exponents
	private int getExponentSpan() {
		return BigInteger.TWO.pow(combination - 5)
			.multiply(BigInteger.valueOf(3))
			.intValue();
	}
	
	/**
	 * Returns the exponent bias
	 * 
	 * @return the bias
	 */
	public int getBias() {
		// floor(log10(10 * 2^significand - 1)) + floor(3 * 2^(combination - 5) / 2) - 2
		return memoize(
			MEMOIZE_EXPBIAS,
			() -> BigInteger.valueOf(getSignificandDigits() - 2) // floor(log10(...)) - 2
				.add(BigInteger.valueOf(getExponentSpan() >> 1))
				.intValue()
		);
	}
	
	/**
	 * Returns the maximum number of decimal digits for the significand
	 * 
	 * @return the maximum number of decimal digits
	 */
	public int getSignificandDigits() {
		return memoize(
			MEMOIZE_SIGDIGS,
			() -> 1 + significand / 10 * 3
		);
	}

	/**
	 * Returns the value's combination part
	 * 
	 * @param value the value
	 * @return the combination
	 */
	public BigInteger getCombination(BigInteger value) {
		return value.shiftRight(significand).and(mask(combination));
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

	/** {@inheritDoc} */
	@Override
	public boolean isPositive(BigInteger value) {
		return !isNegative(value);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isNegative(BigInteger value) {
		return value.testBit(significand + combination);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isInfinity(BigInteger value) {
		return getCombination(value)
			.shiftRight(combination - 5)
			.compareTo(MASK_INFINITY) == 0;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isPositiveInfinity(BigInteger value) {
		return isPositive(value)
			&& isInfinity(value);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isNegativeInfinity(BigInteger value) {
		return isNegative(value)
			&& isInfinity(value);
	}

	/** {@inheritDoc} */
	@Override
	public BigInteger getPositiveInfinity() {
		return memoize(
			MEMOIZE_POS_INF,
			() -> MASK_INFINITY
				.shiftLeft(combination - 5 + significand)
		);
	}

	/** {@inheritDoc} */
	@Override
	public BigInteger getNegativeInfinity() {
		return memoize(
			MEMOIZE_NEG_INF,
			() -> MASK_INFINITY
				.or(MASK_NEGATIVE)
				.shiftLeft(combination - 5 + significand)
		);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isNaN(BigInteger value) {
		return getCombination(value)
			.shiftRight(combination - 5)
			.compareTo(MASK_NAN) == 0;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isQuietNaN(BigInteger value) {
		return isNaN(value)
			&& !value.testBit(significand + combination - 6);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isSignalingNaN(BigInteger value) {
		return isNaN(value)
			&& value.testBit(significand + combination - 6);
	}

	/** {@inheritDoc} */
	@Override
	public BigInteger getQuietNaN(int signum) {
		return MASK_NAN
			.or(signum == -1 ? MASK_NEGATIVE : BigInteger.ZERO)
			.shiftLeft(combination - 5 + significand);
	}

	/** {@inheritDoc} */
	@Override
	public BigInteger getSignalingNaN(int signum) {
		return MASK_NAN
			.or(signum == -1 ? MASK_NEGATIVE : BigInteger.ZERO)
			.shiftLeft(1)
			.or(BigInteger.ONE)
			.shiftLeft(combination - 6 + significand);
	}

	/** {@inheritDoc} */
	@Override
	public BigInteger getNaN(int signum) {
		return getQuietNaN(signum);
	}

	/** {@inheritDoc} */
	@Override
	public BigInteger getZero(int signum) {
		return signum == -1
			? MASK_NEGATIVE.shiftLeft(combination - 5 + significand)
			: BigInteger.ZERO;
	}

	/** {@inheritDoc} */
	@Override
	public T getMinSubnormalValue() {
		return memoize(
			MEMOIZE_SMINVAL,
			() -> decodeBID(BigInteger.ONE)
		);
	}

	/** {@inheritDoc} */
	@Override
	public T getMinValue() {
		return memoize(
			MEMOIZE_MIN_VAL,
			() -> decodeDPD(BigInteger.ONE.shiftLeft(significand + combination - 5))
		);
	}

	/** {@inheritDoc} */
	@Override
	public T getMaxValue() {
		return memoize(
			MEMOIZE_MAX_VAL,
			() -> decodeDPD(
				BigInteger.valueOf(0b111)
					.shiftLeft(significand + combination - 3)
					.or(mask(significand + combination - 4))
			)
		);
	}

	/** {@inheritDoc} */
	@Override
	public BigDecimal getEpsilon() {
		return memoize(
			MEMOIZE_EPSILON,
			() -> {
				int bias = getBias() - getSignificandDigits() + 1;
				
				int lo = bias & mask(combination - 5).intValue();
				int hi = bias >> (combination - 5);
				
				// smallest number larger than 1
				BigInteger value = BigInteger.valueOf(hi)
					.shiftLeft(3)
					.or(BigInteger.ONE)
					.shiftLeft(combination - 5)
					.or(BigInteger.valueOf(lo))
					.shiftLeft(significand)
					.or(BigInteger.ONE);
				
				return decodeDPD(value)
					.getBigDecimal()
					.subtract(BigDecimal.ONE);
			}
		);
	}

	/** {@inheritDoc} */
	@Override
	public Map.Entry<Integer, Integer> getExponentRange() {
		return memoize(
			MEMOIZE_EXRANGE,
			() -> {
				int span = getExponentSpan() >> 1;
				
				return Map.entry(
					2 - span,
					1 + span
				);
			}
		);
	}

	private static record EncodeInfo(boolean special, BigInteger value, int scale) { }
	
	private static record DecodeInfo<T>(boolean high, boolean special, T specialValue) { }

}
