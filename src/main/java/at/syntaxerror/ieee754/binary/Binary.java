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

/**
 * This class is the base class for implementing IEEE 754 floating point specifications 
 * 
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings({ "unchecked", "serial" })
public abstract class Binary<T extends Binary<T>> extends Number implements Comparable<T> {
	
	public static final int POSITIVE = +1;
	public static final int NEGATIVE = -1;
	
	private final int signum;
	private final BinaryType type;
	private final BigDecimal value;
	
	private BigInteger encoded;

	/**
	 * 
	 * @param signum
	 * @param value
	 * @param unchecked
	 * @deprecated internal use only
	 */
	@Deprecated
	public Binary(int signum, BigDecimal value, boolean unchecked) {
		if(signum != -1 && signum != 1)
			throw new IllegalArgumentException("Signum out of range");
		
		this.signum = signum;
		
		if(value.compareTo(BigDecimal.ZERO) != 0 && signum != value.signum())
			throw new IllegalArgumentException("Signum mismatch");
		
		if(unchecked) {
			this.value = value;
			this.type = BinaryType.FINITE;
			return;
		}
		
		if(value.compareTo(getCodec().getMaxValue().getBigDecimal()) > 0) { // overflow => Infinity
			this.value = null;
			this.type = BinaryType.INFINITE;
		}
		
		else if(value.compareTo(getCodec().getMinSubnormalValue().getBigDecimal()) < 0) { // underflow => zero
			this.value = BigDecimal.ZERO;
			this.type = BinaryType.FINITE;
		}
		
		else {
			this.value = value;
			this.type = BinaryType.FINITE;
		}
	}
	
	/**
	 * Creates a new binary floating-point number with the given signum and value.
	 * <p>If the value exceeds the {@link BinaryCodec#getMaxValue() maximum value},
	 * the value becomes {@link #isInfinity() Infinity} with the respective sign.
	 * <p>If the value is less than the {@link BinaryCodec#getMinSubnormalValue() minimum value},
	 * the values becomes {@code 0}.
	 * 
	 * @param signum the signum (either {@code -1}, {@code 0}, or {@code 1}; must be the same as {@link BigDecimal#signum() the value's signum})
	 * @param value the value
	 */
	public Binary(int signum, BigDecimal value) {
		this(signum, value, false);
	}
	
	/**
	 * Creates a new special binary floating-point number.
	 * 
	 * @param signum the signum (either {@code -1} or {@code 1})
	 * @param type the special type (must not be {@link BinaryType#FINITE})
	 */
	public Binary(int signum, BinaryType type) {
		if(signum != -1 && signum != 1)
			throw new IllegalArgumentException("Signum out of range");
		
		if(type == BinaryType.FINITE)
			throw new IllegalArgumentException("Constructor not suitable for finite type");
		
		this.signum = signum;
		this.type = type;
		this.value = null;
	}
	
	/**
	 * Returns the {@link BigDecimal} stored. Fails if the number is not {@link #isFinite() finite}.
	 * 
	 * @return the stored {@link BigDecimal}
	 */
	public BigDecimal getBigDecimal() {
		if(type != BinaryType.FINITE)
			throw new UnsupportedOperationException("BigDecimal is not supported for non-finite types");
		
		return value;
	}
	
	/**
	 * Returns the signum, which is either {@code -1} (negative), {@code 0} (zero), or {@code 1} (positive)
	 * 
	 * @return the signum
	 */
	public int getSignum() {
		return signum;
	}
	
	/**
	 * Checks whether this number is positive
	 * 
	 * @return whether this number is positive
	 */
	public boolean isPositive() {
		return signum == POSITIVE;
	}

	/**
	 * Checks whether this number is negative
	 * 
	 * @return whether this number is negative
	 */
	public boolean isNegative() {
		return signum == NEGATIVE;
	}

	/**
	 * Checks whether this number is {@link BinaryType#FINITE finite}
	 * 
	 * @return whether this number is finite
	 */
	public boolean isFinite() {
		return type == BinaryType.FINITE;
	}

	/**
	 * Checks whether this number is {@link BinaryType#SIGNALING_NAN sNaN} or {@link BinaryType#QUIET_NAN qNaN}
	 * 
	 * @return whether this number is {@code NaN}
	 */
	public boolean isNaN() {
		return type == BinaryType.SIGNALING_NAN || type == BinaryType.QUIET_NAN;
	}

	/**
	 * Checks whether this number is {@link BinaryType#SIGNALING_NAN sNaN}
	 * 
	 * @return whether this number is {@code sNaN}
	 */
	public boolean isSignalingNaN() {
		return type == BinaryType.SIGNALING_NAN;
	}

	/**
	 * Checks whether this number is {@link BinaryType#QUIET_NAN qNaN}
	 * 
	 * @return whether this number is {@code qNaN}
	 */
	public boolean isQuietNaN() {
		return type == BinaryType.QUIET_NAN;
	}

	/**
	 * Checks whether this number is {@link BinaryType#INFINITE Infinity}
	 * 
	 * @return whether this number is {@code Infinity}
	 */
	public boolean isInfinity() {
		return type == BinaryType.INFINITE;
	}

	/**
	 * Checks whether this number is {@link BinaryType#INFINITE +Infinity}
	 * 
	 * @return whether this number is {@code +Infinity}
	 */
	public boolean isPositiveInfinity() {
		return isInfinity() && isPositive();
	}

	/**
	 * Checks whether this number is {@link BinaryType#INFINITE -Infinity}
	 * 
	 * @return whether this number is {@code -Infinity}
	 */
	public boolean isNegativeInfinity() {
		return isInfinity() && isNegative();
	}

	/**
	 * Checks whether this number is {@code 0}
	 * 
	 * @return whether this number is {@code 0}
	 */
	public boolean isZero() {
		return value.compareTo(BigDecimal.ZERO) == 0;
	}

	/**
	 * Checks whether this number is {@code +0}.
	 * <p><b>Note:</b> The {@link #getSignum() signum} value {@code 0} also classifies as positive in this case
	 * 
	 * @return whether this number is {@code +0}
	 */
	public boolean isPositiveZero() {
		return isZero() && !isNegative();
	}

	/**
	 * Checks whether this number is {@code -0}
	 * 
	 * @return whether this number is {@code -0}
	 */
	public boolean isNegativeZero() {
		return isNegative() && isNegative();
	}
	
	/**
	 * Encodes this number into its binary representation
	 * 
	 * @return the binary representation
	 */
	public BigInteger encode() {
		return encoded == null
			? encoded = getCodec().encode((T) this)
			: encoded;
	}
	
	/** {@inheritDoc} */
	@Override
	public int intValue() {
		if(type != BinaryType.FINITE)
			throw new UnsupportedOperationException("Cannot convert non-finite number to integer");
		
		return (int) intValue();
	}

	/** {@inheritDoc} */
	@Override
	public long longValue() {
		if(type != BinaryType.FINITE)
			throw new UnsupportedOperationException("Cannot convert non-finite number to integer");
		
		return (long) doubleValue();
	}

	/** {@inheritDoc} */
	@Override
	public float floatValue() {
		return (float) doubleValue();
	}

	/** {@inheritDoc} */
	@Override
	public double doubleValue() {
		switch(type) {
		case INFINITE:
			return isPositiveInfinity()
				? Double.POSITIVE_INFINITY
				: Double.NEGATIVE_INFINITY;
			
		case FINITE:
			return value.doubleValue();
			
		default:
			return Double.NaN;
		}
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(T o) {
		return isFinite() && o.isFinite()
			? value.compareTo(o.getBigDecimal())
			: Double.compare(doubleValue(), o.doubleValue());
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		// same string representation as double and float
		switch(type) {
		case INFINITE:
			return (isNegative() ? "-" : "") + "Infinity";
		case FINITE:
			return getBigDecimal().toString();
		default:
			return "NaN";
		}
	}
	
	/**
	 * Returns the {@link BinaryCodec codec} used for this binary floating-point number
	 * 
	 * @return the codec
	 */
	public abstract BinaryCodec<T> getCodec();
	
}
