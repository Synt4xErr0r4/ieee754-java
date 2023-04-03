/**
 * 
 */
package at.syntaxerror.ieee754;

import java.math.BigDecimal;

/**
 * This class implements the IEEE 754 binary16 floating point specification 
 * 
 * @author SyntaxError404
 * 
 */
@SuppressWarnings("serial")
public class Binary16 extends Binary<Binary16> {

	public static final Binary16 POSITIVE_INFINITY = new Binary16(POSITIVE, BinaryType.INFINITE);
	public static final Binary16 NEGATIVE_INFINITY = new Binary16(NEGATIVE, BinaryType.INFINITE);
	public static final Binary16 QUIET_NAN = new Binary16(POSITIVE, BinaryType.QUIET_NAN);
	public static final Binary16 SIGNALING_NAN = new Binary16(POSITIVE, BinaryType.SIGNALING_NAN);

	public static final BinaryFactory<Binary16> FACTORY = new Binary64Factory();
	public static final BinaryCodec<Binary16> CODEC = new BinaryCodec<>(5, 10, true, FACTORY);
	
	public Binary16(int signum, BigDecimal value) {
		super(signum, value);
	}

	public Binary16(int signum, BinaryType type) {
		super(signum, type);
	}
	
	@Override
	public BinaryCodec<Binary16> getCodec() {
		return CODEC;
	}
	
	private static class Binary64Factory implements BinaryFactory<Binary16> {
		
		@Override
		public Binary16 create(int signum, BigDecimal value) {
			return new Binary16(signum, value);
		}
		
		@Override
		public Binary16 create(int signum, BinaryType type) {
			return new Binary16(signum, type);
		}
		
	}
	
}
