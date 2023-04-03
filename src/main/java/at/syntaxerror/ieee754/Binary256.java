/**
 * 
 */
package at.syntaxerror.ieee754;

import java.math.BigDecimal;

/**
 * This class implements the IEEE 754 binary256 floating point specification 
 * 
 * @author SyntaxError404
 * 
 */
@SuppressWarnings("serial")
public class Binary256 extends Binary<Binary256> {

	public static final Binary256 POSITIVE_INFINITY = new Binary256(POSITIVE, BinaryType.INFINITE);
	public static final Binary256 NEGATIVE_INFINITY = new Binary256(NEGATIVE, BinaryType.INFINITE);
	public static final Binary256 QUIET_NAN = new Binary256(POSITIVE, BinaryType.QUIET_NAN);
	public static final Binary256 SIGNALING_NAN = new Binary256(POSITIVE, BinaryType.SIGNALING_NAN);

	public static final BinaryFactory<Binary256> FACTORY = new Binary64Factory();
	public static final BinaryCodec<Binary256> CODEC = new BinaryCodec<>(19, 236, true, FACTORY);
	
	public Binary256(int signum, BigDecimal value) {
		super(signum, value);
	}

	public Binary256(int signum, BinaryType type) {
		super(signum, type);
	}
	
	@Override
	public BinaryCodec<Binary256> getCodec() {
		return CODEC;
	}
	
	private static class Binary64Factory implements BinaryFactory<Binary256> {
		
		@Override
		public Binary256 create(int signum, BigDecimal value) {
			return new Binary256(signum, value);
		}
		
		@Override
		public Binary256 create(int signum, BinaryType type) {
			return new Binary256(signum, type);
		}
		
	}
	
}
