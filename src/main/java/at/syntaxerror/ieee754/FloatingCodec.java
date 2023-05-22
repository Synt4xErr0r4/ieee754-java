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
import java.util.Map;

/**
 * This class represents the base codec for encoding and decoding IEEE 754 floating point numbers.
 * See {@link at.syntaxerror.ieee754.binary.BinaryCodec BinaryCodec} and {@link at.syntaxerror.ieee754.decimal.DecimalCodec DecimalCodec}
 * for implementations.
 * 
 * @author Thomas Kasper
 * 
 */
public abstract class FloatingCodec<T extends Floating<T>> {

	/** @see #initialize() */
	boolean initialized = false;
	
	/**
	 * Initializes the codec, so that the max, min and
	 * subnormal min value are guaranteed to be accessible.
	 * 
	 * <p>
	 * Without this method, codecs which memoize those values
	 * might get stuck in an endless loop, since these methods
	 * are also called from within the
	 * {@link Floating#Floating(int, java.math.BigDecimal)}
	 * constructor
	 */
	protected final void initialize() {
		getMaxValue();
		getMinValue();
		getMinSubnormalValue();
		
		initialized = true;
	}
	
	/**
	 * Decodes the floating point's binary representation
	 * 
	 * @param value the binary representation
	 * @return the decoded floating point number
	 */
	public abstract T decode(BigInteger value);

	/**
	 * Encodes the floating point into its binary representation
	 * 
	 * @param value the floating point number
	 * @return the encoded binary representation
	 */
	public abstract BigInteger encode(T value);
	
	/**
	 * Checks if the value is positive
	 * 
	 * @param value the value
	 * @return whether the value is positive
	 */
	public abstract boolean isPositive(BigInteger value);

	/**
	 * Checks if the value is negative
	 * 
	 * @param value the value
	 * @return whether the value is negative
	 */
	public abstract boolean isNegative(BigInteger value);

	/**
	 * Checks if the value is {@code Infinity}'s binary representation
	 * 
	 * @param value the value
	 * @return whether the value is {@code Infinity}
	 */
	public abstract boolean isInfinity(BigInteger value);

	/**
	 * Checks if the value is {@code +Infinity}'s binary representation
	 * 
	 * @param value the value
	 * @return whether the value is {@code +Infinity}
	 */
	public abstract boolean isPositiveInfinity(BigInteger value);

	/**
	 * Checks if the value is {@code -Infinity}'s binary representation
	 * 
	 * @param value the value
	 * @return whether the value is {@code -Infinity}
	 */
	public abstract boolean isNegativeInfinity(BigInteger value);
	
	/**
	 * Returns {@code +Infinity}'s (like {@link Double#POSITIVE_INFINITY}) binary representation
	 * 
	 * @return {@code +Infinity}
	 */
	public abstract BigInteger getPositiveInfinity();

	/**
	 * Returns {@code -Infinity}'s (like {@link Double#NEGATIVE_INFINITY}) binary representation
	 * 
	 * @return {@code -Infinity}
	 */
	public abstract BigInteger getNegativeInfinity();

	/**
	 * Checks if the value is {@code NaN}'s binary representation
	 * 
	 * @param value the value
	 * @return whether the value is {@code NaN}
	 */
	public abstract boolean isNaN(BigInteger value);

	/**
	 * Checks if the value is {@code qNaN}'s binary representation
	 * 
	 * @param value the value
	 * @return whether the value is {@code qNaN}
	 */
	public abstract boolean isQuietNaN(BigInteger value);
	
	/**
	 * Checks if the value is {@code sNaN}'s binary representation
	 * 
	 * @param value the value
	 * @return whether the value is {@code sNaN}
	 */
	public abstract boolean isSignalingNaN(BigInteger value);
	
	/**
	 * Returns {@code qNaN} binary representation (on most processors)
	 * 
	 * @param signum the signum
	 * @return {@code qNaN}
	 */
	public abstract BigInteger getQuietNaN(int signum);

	/**
	 * Returns {@code sNaN}'s binary representation (on most processors)
	 * 
	 * @param signum the signum
	 * @return {@code sNaN}
	 */
	public abstract BigInteger getSignalingNaN(int signum);

	/**
	 * Returns {@code NaN} ({@code qNaN} on most processors)
	 * 
	 * @return {@code NaN}
	 */
	public abstract BigInteger getNaN(int signum);

	/**
	 * Returns (possibly negative) zero's binary representation
	 * 
	 * @return zero
	 */
	public abstract BigInteger getZero(int signum);
	
	/**
	 * Returns the smallest postive ({@code > 0}) subnormal value
	 * 
	 * @return the smallest postive value
	 */
	public abstract T getMinSubnormalValue();
	
	/**
	 * Returns the smallest postive ({@code > 0}) normalized value.
	 * 
	 * @return the smallest postive value
	 */
	public abstract T getMinValue();

	/**
	 * Returns the largest possible value
	 * 
	 * @return the largest value
	 */
	public abstract T getMaxValue();

	/**
	 * Returns the difference between 1 and the smallest number greater than 1
	 * 
	 * @return the difference 
	 */
	public abstract BigDecimal getEpsilon();

	/**
	 * Returns the smallest and largest possible exponent
	 * 
	 * @return the smallest and largest exponent
	 */
	public abstract Map.Entry<Integer, Integer> getExponentRange();
	
}
