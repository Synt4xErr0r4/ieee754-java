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

import at.syntaxerror.ieee754.binary.Binary;
import at.syntaxerror.ieee754.decimal.Decimal;

/**
 * This class represents the base class for IEEE 754 {@link Binary binary} and {@link Decimal decimal} numbers.
 * 
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings("serial")
public abstract class Floating<T extends Floating<T>> extends Number implements Comparable<T> {
	
	public static final int POSITIVE = +1;
	public static final int NEGATIVE = -1;
	
	private final int signum;
	private final FloatingType type;
	private final BigDecimal value;
	
	private BigInteger encoded;

	/**
	 * Creates a new floating-point number with the given signum and value.
	 * <p>If the value exceeds the {@link FloatingCodec#getMaxValue() maximum value},
	 * the value becomes {@link #isInfinity() Infinity} with the respective sign.
	 * <p>If the value is less than the {@link FloatingCodec#getMinSubnormalValue() minimum value},
	 * the values becomes {@code 0}.
	 * 
	 * @param signum the signum (either {@code -1}, {@code 0}, or {@code 1}; must be the same as {@link BigDecimal#signum() the value's signum})
	 * @param value the value
	 */
	public Floating(int signum, BigDecimal value) {
		if(signum != -1 && signum != 1)
			throw new IllegalArgumentException("Signum out of range");
		
		this.signum = signum;
		
		if(value.compareTo(BigDecimal.ZERO) != 0 && signum != value.signum())
			throw new IllegalArgumentException("Signum mismatch");
		
		if(getCodec() == null || !getCodec().initialized) {
			this.value = value;
			this.type = FloatingType.FINITE;
			return;
		}
		
		if(value.abs().compareTo(getCodec().getMaxValue().getBigDecimal()) > 0) { // overflow => Infinity
			this.value = null;
			this.type = FloatingType.INFINITE;
		}
		
//		else if(value.abs().compareTo(getCodec().getMinSubnormalValue().getBigDecimal()) < 0) { // underflow => zero
//			this.value = BigDecimal.ZERO;
//			this.type = FloatingType.FINITE;
//		}
		
		else {
			this.value = value;
			this.type = FloatingType.FINITE;
		}
	}
	
	/**
	 * Creates a new special floating-point number.
	 * 
	 * @param signum the signum (either {@code -1} or {@code 1})
	 * @param type the special type (must not be {@link FloatingType#FINITE})
	 */
	public Floating(int signum, FloatingType type) {
		if(signum != -1 && signum != 1)
			throw new IllegalArgumentException("Signum out of range");
		
		if(type == FloatingType.FINITE)
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
		if(type != FloatingType.FINITE)
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
	 * Checks whether this number is {@link FloatingType#FINITE finite}
	 * 
	 * @return whether this number is finite
	 */
	public boolean isFinite() {
		return type == FloatingType.FINITE;
	}

	/**
	 * Checks whether this number is {@link FloatingType#SIGNALING_NAN sNaN} or {@link FloatingType#QUIET_NAN qNaN}
	 * 
	 * @return whether this number is {@code NaN}
	 */
	public boolean isNaN() {
		return type == FloatingType.SIGNALING_NAN || type == FloatingType.QUIET_NAN;
	}

	/**
	 * Checks whether this number is {@link FloatingType#SIGNALING_NAN sNaN}
	 * 
	 * @return whether this number is {@code sNaN}
	 */
	public boolean isSignalingNaN() {
		return type == FloatingType.SIGNALING_NAN;
	}

	/**
	 * Checks whether this number is {@link FloatingType#QUIET_NAN qNaN}
	 * 
	 * @return whether this number is {@code qNaN}
	 */
	public boolean isQuietNaN() {
		return type == FloatingType.QUIET_NAN;
	}

	/**
	 * Checks whether this number is {@link FloatingType#INFINITE Infinity}
	 * 
	 * @return whether this number is {@code Infinity}
	 */
	public boolean isInfinity() {
		return type == FloatingType.INFINITE;
	}

	/**
	 * Checks whether this number is {@link FloatingType#INFINITE +Infinity}
	 * 
	 * @return whether this number is {@code +Infinity}
	 */
	public boolean isPositiveInfinity() {
		return isInfinity() && isPositive();
	}

	/**
	 * Checks whether this number is {@link FloatingType#INFINITE -Infinity}
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
	@SuppressWarnings("unchecked")
	public BigInteger encode() {
		return encoded == null
			? encoded = getCodec().encode((T) this)
			: encoded;
	}
	
	/** {@inheritDoc} */
	@Override
	public int intValue() {
		if(type != FloatingType.FINITE)
			throw new UnsupportedOperationException("Cannot convert non-finite number to integer");
		
		return (int) longValue();
	}

	/** {@inheritDoc} */
	@Override
	public long longValue() {
		if(type != FloatingType.FINITE)
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
	 * Returns the {@link FloatingCodec codec} used for this floating-point number
	 * 
	 * @return the codec
	 */
	public abstract FloatingCodec<T> getCodec();
	
}
