/**
 * 
 */
package at.syntaxerror.ieee754;

import java.math.BigDecimal;

/**
 * This class implements the IEEE 754 binary128 floating point specification 
 * 
 * @author SyntaxError404
 * 
 */
@SuppressWarnings("serial")
public class Binary128 extends Binary<Binary128> {

	public static final Binary128 POSITIVE_INFINITY = new Binary128(POSITIVE, BinaryType.INFINITE);
	public static final Binary128 NEGATIVE_INFINITY = new Binary128(NEGATIVE, BinaryType.INFINITE);
	public static final Binary128 QUIET_NAN = new Binary128(POSITIVE, BinaryType.QUIET_NAN);
	public static final Binary128 SIGNALING_NAN = new Binary128(POSITIVE, BinaryType.SIGNALING_NAN);

	public static final BinaryFactory<Binary128> FACTORY = new Binary64Factory();
	public static final BinaryCodec<Binary128> CODEC = new BinaryCodec<>(15, 112, true, FACTORY);
	
	public Binary128(int signum, BigDecimal value) {
		super(signum, value);
	}

	public Binary128(int signum, BinaryType type) {
		super(signum, type);
	}
	
	@Override
	public BinaryCodec<Binary128> getCodec() {
		return CODEC;
	}
	
	private static class Binary64Factory implements BinaryFactory<Binary128> {
		
		@Override
		public Binary128 create(int signum, BigDecimal value) {
			return new Binary128(signum, value);
		}
		
		@Override
		public Binary128 create(int signum, BinaryType type) {
			return new Binary128(signum, type);
		}
		
	}
	
}
