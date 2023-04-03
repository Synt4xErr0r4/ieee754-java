/**
 * 
 */
package at.syntaxerror.ieee754;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

import ch.obermuhlner.math.big.BigDecimalMath;
import lombok.NonNull;

/**
 * This class represents a codec capable of encoding and decoding IEEE 754 binary floating point numbers
 * as well as computing common values (such as NaN, maximum value, ...)
 * 
 * @author SyntaxError404
 * 
 */
public class BinaryCodec<T extends Binary<T>> {
	
	private static final MathContext CTX = MathContext.UNLIMITED;
	
	private static final BigDecimal TWO = BigDecimal.valueOf(2);

	private static final MathContext CEIL = new MathContext(0, RoundingMode.CEILING);
	private static final MathContext FLOOR = new MathContext(0, RoundingMode.FLOOR);
	
	private static final BigDecimal LOG10_2 = BigDecimalMath.log10(TWO, MathContext.DECIMAL128);

	private final int mantissa;
	private final int exponent;
	private final boolean implicit;
	
	private final BinaryFactory<T> factory;
	
	/**
	 * Creates a new binary codec
	 * 
	 * @param mantissa the number of mantissa bits
	 * @param exponent the number of exponent bits
	 * @param implicit whether there is an implicit mantissa bit
	 * @param factory the factory for creating {@link Binary} objects
	 */
	public BinaryCodec(int mantissa, int exponent, boolean implicit, @NonNull BinaryFactory<T> factory) {
		if(exponent < 1) throw new IllegalArgumentException("Illegal non-positive exponent size");
		if(mantissa < 1) throw new IllegalArgumentException("Illegal non-positive mantissa size");
 
		if(exponent > 30) throw new IllegalArgumentException("Exponent size is too big");
		if(mantissa > 512) throw new IllegalArgumentException("Mantissa size is too big");
		
		this.mantissa = mantissa;
		this.exponent = exponent;
		this.implicit = implicit;
		this.factory = factory;
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
			return BigInteger.ZERO;
		
		// determine sign bit
		int sign = bigdec.signum() == -1 ? 1 : 0;
		
		// strip fractional part
		BigInteger mantissa = bigdec.toBigInteger();
		
		// strip integer part
		BigDecimal fraction = bigdec.subtract(new BigDecimal(mantissa, CTX), CTX).abs();
		
		// remove sign
		mantissa = mantissa.abs();

		// unbiased exponent, or 0 if exponent would be negative
		int exp = mantissa.bitLength();
		
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
		else if(exponent == mask(exponent).intValue()) {
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

	// computes 2^n where n is < 0 [equivalent to 1/(2^(-n)) = 1/(1<<(-n))]
	private BigDecimal pow2negative(int n) {
		return BigDecimal.ONE.divide(new BigDecimal(BigInteger.ONE.shiftLeft(n), CTX));
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
	 * Checks if the value is Negative
	 * 
	 * @param value the value
	 * @return whether the value is Negative
	 */
	public boolean isNegative(BigInteger value) {
		return value.testBit(exponent + mantissa);
	}

	/**
	 * Checks if the value is Infinity
	 * 
	 * @param value the value
	 * @return whether the value is Infinity
	 */
	public boolean isInfinity(BigInteger value) {
		int exponent = getExponent(value).intValue();
		
		if(exponent != mask(exponent).intValue())
			return false;

		return getMantissa(value).compareTo(BigInteger.ZERO) == 0;
	}

	/**
	 * Checks if the value is +Infinity
	 * 
	 * @param value the value
	 * @return whether the value is +Infinity
	 */
	public boolean isPositiveInfinity(BigInteger value) {
		return !isNegative(value) && isInfinity(value);
	}

	/**
	 * Checks if the value is -Infinity
	 * 
	 * @param value the value
	 * @return whether the value is -Infinity
	 */
	public boolean isNegativeInfinity(BigInteger value) {
		return isNegative(value) && isInfinity(value);
	}	
	
	/**
	 * returns positive infinity (like Double#POSITIVE_INFINITY)
	 * 
	 * @return positive infinity
	 */
	public BigInteger getPositiveInfinity() {
		return BigInteger.ZERO
			.shiftLeft(exponent)
			.or(mask(exponent))
			.shiftLeft(mantissa);
	}

	/**
	 * Returns positive infinity (like Double#NEGATIVE_INFINITY)
	 * 
	 * @return positive infinity
	 */
	public BigInteger getNegativeInfinity() {
		return BigInteger.ONE
			.shiftLeft(exponent)
			.or(mask(exponent))
			.shiftLeft(mantissa);
	}

	/**
	 * Checks if the value is NaN
	 * 
	 * @param value the value
	 * @return whether the value is NaN
	 */
	public boolean isNaN(BigInteger value) {
		int exponent = getExponent(value).intValue();
		
		if(exponent != mask(exponent).intValue())
			return false;
		
		return getMantissa(value).compareTo(BigInteger.ZERO) != 0;
	}

	/**
	 * Checks if the value is qNaN
	 * 
	 * @param value the value
	 * @return whether the value is qNaN
	 */
	public boolean isQuietNaN(BigInteger value) {
		return isNaN(value)
			&& getMantissa(value).testBit(mantissa - 1);
	}
	
	/**
	 * Checks if the value is sNaN
	 * 
	 * @param value the value
	 * @return whether the value is sNaN
	 */
	public boolean isSignalingNaN(BigInteger value) {
		return isNaN(value)
			&& !getMantissa(value).testBit(mantissa - 1);
	}
	
	/**
	 * Returns qNaN (on most processors)
	 * 
	 * @param signum the signum
	 * @return qNaN
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
	 * Returns sNaN (on most processors)
	 * 
	 * @param signum the signum
	 * @return sNaN
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
	 * Returns NaN
	 * 
	 * @return NaN
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
	 * Returns the smallest postive value for the given s
	 * 
	 * @return the smallest postive value
	 */
	public T getMinValue() {
		return decode(
			BigInteger.ONE
				.shiftLeft(mantissa)
		);
	}

	/**
	 * Returns the largest value for the given s
	 * 
	 * @return the largest value
	 */
	public T getMaxValue() {
		return decode(
			BigInteger.ZERO
				.shiftLeft(exponent)
				.or(mask(exponent - 1))
				.shiftLeft(mantissa + 1)
				.or(mask(mantissa))
		);
	}
	
	/**
	 * Returns the difference between 1 and the smallest number greater than 1
	 * 
	 * @return the difference
	 */
	public BigDecimal getEpsilon() {
		BigInteger rawOne = BigInteger.ZERO
			.or(mask(exponent - 1))
			.shiftLeft(mantissa);
		
		if(!implicit)
			rawOne = rawOne.or(BigInteger.ONE.shiftLeft(mantissa - 1));
		
		// smallest number greater than 1
		BigDecimal one = decode(rawOne.or(BigInteger.ONE)).getBigDecimal();
		
		return one.subtract(BigDecimal.ONE);
	}
	
	/**
	 * Returns the smallest and largest exponent
	 * 
	 * @return the smallest and largest exponent
	 */
	public Map.Entry<Integer, Integer> getExponentRange() {
		int bias = getBias();
		
		return Map.entry(
			2 - bias,
			(1 << exponent) - 1 - bias
		);
	}
	
	/**
	 * Returns the smallest and largest exponent so that 10 to the power of the exponent is a normalized number
	 * 
	 * @return the smallest and largest exponent
	 */
	public Map.Entry<Integer, Integer> get10ExponentRange() {
		Map.Entry<Integer, Integer> range = getExponentRange();
		
		// 2^(e_min - 1) 	[e_min < 0]
		BigDecimal min = pow2negative(1 - range.getKey());
		
		// (1 - 2^-p) * 2^e_max 	[e_max > 0]
		BigDecimal max = BigDecimal.ONE.subtract(pow2negative(mantissa), CTX)
			.multiply(new BigDecimal(BigInteger.ONE.shiftLeft(range.getValue()), CTX));
		
		return Map.entry(
			BigDecimalMath.log10(min, MathContext.DECIMAL128).round(CEIL).intValue(), // log10 and round down
			BigDecimalMath.log10(max, MathContext.DECIMAL128).round(FLOOR).intValue() // log10 and round up
		);
	}
	
	/**
	 * Computes the number of decimal digits that can be converted back and forth without precision loss
	 * 
	 * @return the number of decimal digits
	 */
	public int getDecimalDigits() {
		// floor( (p - 1) * log10(b) )
		return BigDecimal.valueOf(mantissa - 1).multiply(LOG10_2).round(FLOOR).intValue();
	}
	
}
