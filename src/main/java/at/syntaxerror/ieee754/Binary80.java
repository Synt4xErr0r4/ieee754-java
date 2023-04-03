/**
 * 
 */
package at.syntaxerror.ieee754;

import java.math.BigDecimal;

/**
 * This class implements the x87 extended precision (80-bit) floating point specification 
 * 
 * @author SyntaxError404
 * 
 */
@SuppressWarnings("serial")
public class Binary80 extends Binary<Binary80> {

	public static final Binary80 POSITIVE_INFINITY = new Binary80(POSITIVE, BinaryType.INFINITE);
	public static final Binary80 NEGATIVE_INFINITY = new Binary80(NEGATIVE, BinaryType.INFINITE);
	public static final Binary80 QUIET_NAN = new Binary80(POSITIVE, BinaryType.QUIET_NAN);
	public static final Binary80 SIGNALING_NAN = new Binary80(POSITIVE, BinaryType.SIGNALING_NAN);

	public static final BinaryFactory<Binary80> FACTORY = new Binary64Factory();
	public static final BinaryCodec<Binary80> CODEC = new BinaryCodec<>(15, 63, false, FACTORY);
	
	public Binary80(int signum, BigDecimal value) {
		super(signum, value);
	}

	public Binary80(int signum, BinaryType type) {
		super(signum, type);
	}
	
	@Override
	public BinaryCodec<Binary80> getCodec() {
		return CODEC;
	}
	
	private static class Binary64Factory implements BinaryFactory<Binary80> {
		
		@Override
		public Binary80 create(int signum, BigDecimal value) {
			return new Binary80(signum, value);
		}
		
		@Override
		public Binary80 create(int signum, BinaryType type) {
			return new Binary80(signum, type);
		}
		
	}
	
}
