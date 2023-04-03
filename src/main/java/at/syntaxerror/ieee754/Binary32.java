/**
 * 
 */
package at.syntaxerror.ieee754;

import java.math.BigDecimal;

/**
 * This class implements the IEEE 754 binary32 floating point specification 
 * 
 * @author SyntaxError404
 * 
 */
@SuppressWarnings("serial")
public class Binary32 extends Binary<Binary32> {

	public static final Binary32 POSITIVE_INFINITY = new Binary32(POSITIVE, BinaryType.INFINITE);
	public static final Binary32 NEGATIVE_INFINITY = new Binary32(NEGATIVE, BinaryType.INFINITE);
	public static final Binary32 QUIET_NAN = new Binary32(POSITIVE, BinaryType.QUIET_NAN);
	public static final Binary32 SIGNALING_NAN = new Binary32(POSITIVE, BinaryType.SIGNALING_NAN);

	public static final BinaryFactory<Binary32> FACTORY = new Binary32Factory();
	public static final BinaryCodec<Binary32> CODEC = new BinaryCodec<>(8, 23, true, FACTORY);
	
	public Binary32(int signum, BigDecimal value) {
		super(signum, value);
	}

	public Binary32(int signum, BinaryType type) {
		super(signum, type);
	}
	
	@Override
	public BinaryCodec<Binary32> getCodec() {
		return CODEC;
	}
	
	private static class Binary32Factory implements BinaryFactory<Binary32> {
		
		@Override
		public Binary32 create(int signum, BigDecimal value) {
			return new Binary32(signum, value);
		}
		
		@Override
		public Binary32 create(int signum, BinaryType type) {
			return new Binary32(signum, type);
		}
		
	}
	
}
