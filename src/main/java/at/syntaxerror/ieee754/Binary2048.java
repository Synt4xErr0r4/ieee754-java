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
 * This class implements the IEEE 754 floating point specification for 2048-bit numbers.
 * <p><b>Warning:</b> This format is not official and exists for demonstration purposes only.
 * 	Encoding/decoding might take <i>extremely</i> long, results might be inaccurate.
 * 
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings("serial")
public final class Binary2048 extends Binary<Binary2048> {

	public static final BinaryFactory<Binary2048> FACTORY = new Binary64Factory();
	public static final BinaryCodec<Binary2048> CODEC = new BinaryCodec<>(31, 2016, true, FACTORY);

	public static final Binary2048 POSITIVE_INFINITY = new Binary2048(POSITIVE, BinaryType.INFINITE);
	public static final Binary2048 NEGATIVE_INFINITY = new Binary2048(NEGATIVE, BinaryType.INFINITE);
	public static final Binary2048 QUIET_NAN = new Binary2048(POSITIVE, BinaryType.QUIET_NAN);
	public static final Binary2048 SIGNALING_NAN = new Binary2048(POSITIVE, BinaryType.SIGNALING_NAN);
	
	public static final Binary2048 MAX_VALUE = CODEC.getMaxValue();
	public static final Binary2048 MIN_VALUE = CODEC.getMinSubnormalValue();
	public static final Binary2048 MIN_NORMAL = CODEC.getMinValue();
	
	private Binary2048(int signum, BigDecimal value) {
		super(signum, value);
	}

	private Binary2048(int signum, BinaryType type) {
		super(signum, type);
	}

	/** {@inheritDoc} */
	@Override
	public BinaryCodec<Binary2048> getCodec() {
		return CODEC;
	}
	
	private static class Binary64Factory implements BinaryFactory<Binary2048> {
		
		@Override
		public Binary2048 create(int signum, BigDecimal value) {
			return new Binary2048(signum, value);
		}
		
		@Override
		public Binary2048 create(int signum, BinaryType type) {
			return new Binary2048(signum, type);
		}
		
	}
	
}
