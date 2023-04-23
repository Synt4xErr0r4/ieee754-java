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

/**
 * This class implements the IEEE 754 binary64 floating point specification 
 * 
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings({ "serial", "deprecation" })
public final class Binary64 extends Binary<Binary64> {

	public static final BinaryFactory<Binary64> FACTORY = new Binary64Factory();
	public static final BinaryCodec<Binary64> CODEC = new BinaryCodec<>(11, 52, true, FACTORY);

	public static final Binary64 POSITIVE_INFINITY = new Binary64(POSITIVE, BinaryType.INFINITE);
	public static final Binary64 NEGATIVE_INFINITY = new Binary64(NEGATIVE, BinaryType.INFINITE);
	public static final Binary64 QUIET_NAN = new Binary64(POSITIVE, BinaryType.QUIET_NAN);
	public static final Binary64 SIGNALING_NAN = new Binary64(POSITIVE, BinaryType.SIGNALING_NAN);
	
	public static final Binary64 MAX_VALUE = CODEC.getMaxValue();
	public static final Binary64 MIN_VALUE = CODEC.getMinSubnormalValue();
	public static final Binary64 MIN_NORMAL = CODEC.getMinValue();

	private Binary64(int signum, BigDecimal value, boolean unchecked) {
		super(signum, value, true);
	}
	
	private Binary64(int signum, BigDecimal value) {
		super(signum, value);
	}

	private Binary64(int signum, BinaryType type) {
		super(signum, type);
	}

	/** {@inheritDoc} */
	@Override
	public BinaryCodec<Binary64> getCodec() {
		return CODEC;
	}
	
	private static class Binary64Factory implements BinaryFactory<Binary64> {
		
		@Override
		public Binary64 createUnchecked(int signum, BigDecimal value) {
			return new Binary64(signum, value, true);
		}
		
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
