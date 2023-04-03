/**
 * 
 */
package at.syntaxerror.ieee754;

import java.math.BigDecimal;

/**
 * This class implements the IEEE 754 binary64 floating point specification 
 * 
 * @author SyntaxError404
 * 
 */
@SuppressWarnings("serial")
public class Binary64 extends Binary<Binary64> {

	public static final Binary64 POSITIVE_INFINITY = new Binary64(POSITIVE, BinaryType.INFINITE);
	public static final Binary64 NEGATIVE_INFINITY = new Binary64(NEGATIVE, BinaryType.INFINITE);
	public static final Binary64 QUIET_NAN = new Binary64(POSITIVE, BinaryType.QUIET_NAN);
	public static final Binary64 SIGNALING_NAN = new Binary64(POSITIVE, BinaryType.SIGNALING_NAN);

	public static final BinaryFactory<Binary64> FACTORY = new Binary64Factory();
	public static final BinaryCodec<Binary64> CODEC = new BinaryCodec<>(11, 52, true, FACTORY);
	
	public Binary64(int signum, BigDecimal value) {
		super(signum, value);
	}

	public Binary64(int signum, BinaryType type) {
		super(signum, type);
	}
	
	@Override
	public BinaryCodec<Binary64> getCodec() {
		return CODEC;
	}
	
	private static class Binary64Factory implements BinaryFactory<Binary64> {
		
		@Override
		public Binary64 create(int signum, BigDecimal value) {
			return new Binary64(signum, value);
		}
		
		@Override
		public Binary64 create(int signum, BinaryType type) {
			return new Binary64(signum, type);
		}
		
	}
	
}
