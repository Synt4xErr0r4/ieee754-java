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
 * This class implements the IEEE 754 floating point specification for 512-bit numbers.
 * <p><b>Warning:</b> This format is not official and exists for demonstration purposes only.
 * 	Encoding/decoding might take <i>pretty</i> long, results might be inaccurate.
 * 
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings("serial")
public final class Binary512 extends Binary<Binary512> {

	public static final BinaryFactory<Binary512> FACTORY = new Binary64Factory();
	public static final BinaryCodec<Binary512> CODEC = new BinaryCodec<>(23, 488, true, FACTORY);

	public static final Binary512 POSITIVE_INFINITY = new Binary512(POSITIVE, BinaryType.INFINITE);
	public static final Binary512 NEGATIVE_INFINITY = new Binary512(NEGATIVE, BinaryType.INFINITE);
	public static final Binary512 QUIET_NAN = new Binary512(POSITIVE, BinaryType.QUIET_NAN);
	public static final Binary512 SIGNALING_NAN = new Binary512(POSITIVE, BinaryType.SIGNALING_NAN);
	
	public static final Binary512 MAX_VALUE = CODEC.getMaxValue();
	public static final Binary512 MIN_VALUE = CODEC.getMinSubnormalValue();
	public static final Binary512 MIN_NORMAL = CODEC.getMinValue();
	
	private Binary512(int signum, BigDecimal value) {
		super(signum, value);
	}

	private Binary512(int signum, BinaryType type) {
		super(signum, type);
	}

	/** {@inheritDoc} */
	@Override
	public BinaryCodec<Binary512> getCodec() {
		return CODEC;
	}
	
	private static class Binary64Factory implements BinaryFactory<Binary512> {
		
		@Override
		public Binary512 create(int signum, BigDecimal value) {
			return new Binary512(signum, value);
		}
		
		@Override
		public Binary512 create(int signum, BinaryType type) {
			return new Binary512(signum, type);
		}
		
	}
	
}
