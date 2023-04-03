/**
 * 
 */
package at.syntaxerror.ieee754;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * This class is the base class for implementing IEEE 754 floating point specifications 
 * 
 * @author SyntaxError404
 * 
 */
@SuppressWarnings({ "unchecked", "serial" })
public abstract class Binary<T extends Binary<T>> extends Number implements Comparable<T> {
	
	public static final int POSITIVE = +1;
	public static final int NEGATIVE = -1;
	
	private final int signum;
	private final BinaryType type;
	private final BigDecimal value;
	
	public Binary(int signum, BigDecimal value) {
		if(signum != -1 && signum != 1)
			throw new IllegalArgumentException("Signum out of range");
		
		this.signum = signum;
		this.value = value;
		this.type = BinaryType.FINITE;
		
		if(value.compareTo(BigDecimal.ZERO) != 0 && signum != value.signum())
			throw new IllegalArgumentException("Signum mismatch");
	}
	
	public Binary(int signum, BinaryType type) {
		if(signum != -1 && signum != 1)
			throw new IllegalArgumentException("Signum out of range");
		
		if(type == BinaryType.FINITE)
			throw new IllegalArgumentException("Constructor not suitable for finite type");
		
		this.signum = signum;
		this.type = type;
		this.value = null;
	}

	public BigDecimal getBigDecimal() {
		if(type != BinaryType.FINITE)
			throw new UnsupportedOperationException("BigDecimal is not supported for non-finite types");
		
		return value;
	}
	
	public int getSignum() {
		return signum;
	}
	
	public boolean isPositive() {
		return signum == POSITIVE;
	}
	
	public boolean isNegative() {
		return signum == NEGATIVE;
	}
	
	public boolean isFinite() {
		return type == BinaryType.FINITE;
	}
	
	public boolean isNaN() {
		return type == BinaryType.SIGNALING_NAN || type == BinaryType.QUIET_NAN;
	}

	public boolean isSignalingNaN() {
		return type == BinaryType.SIGNALING_NAN;
	}

	public boolean isQuietNaN() {
		return type == BinaryType.QUIET_NAN;
	}
	
	public boolean isInfinity() {
		return type == BinaryType.INFINITE;
	}

	public boolean isPositiveInfinity() {
		return isInfinity() && isPositive();
	}

	public boolean isNegativeInfinity() {
		return isInfinity() && isNegative();
	}
	
	public boolean isZero() {
		return value.compareTo(BigDecimal.ZERO) == 0;
	}
	
	public boolean isPositiveZero() {
		return value.compareTo(BigDecimal.ZERO) == 0;
	}
	
	public BigInteger encode() {
		return getCodec().encode((T) this);
	}
	
	@Override
	public int intValue() {
		if(type != BinaryType.FINITE)
			throw new UnsupportedOperationException("Cannot convert non-finite number to integer");
		
		return (int) intValue();
	}
	
	@Override
	public long longValue() {
		if(type != BinaryType.FINITE)
			throw new UnsupportedOperationException("Cannot convert non-finite number to integer");
		
		return (long) doubleValue();
	}
	
	@Override
	public float floatValue() {
		return (float) doubleValue();
	}
	
	@Override
	public double doubleValue() {
		switch(type) {
		case INFINITE:
			return isPositiveInfinity()
				? Double.POSITIVE_INFINITY
				: Double.NEGATIVE_INFINITY;
			
		case SIGNALING_NAN:
		case QUIET_NAN:
			return Double.NaN;
			
		default:
			return value.doubleValue();
		}
	}
	
	@Override
	public int compareTo(T o) {
		return isFinite() && o.isFinite()
			? value.compareTo(o.getBigDecimal())
			: Double.compare(doubleValue(), o.doubleValue());
	}
	
	public abstract BinaryCodec<T> getCodec();
	
}
