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
package at.syntaxerror.ieee754.binary;

import java.math.BigDecimal;

import at.syntaxerror.ieee754.FloatingFactory;
import at.syntaxerror.ieee754.FloatingType;

/**
 * This class implements the IEEE 754 binary256 floating point specification 
 * 
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings({ "serial" })
public final class Binary256 extends Binary<Binary256> {

	public static final FloatingFactory<Binary256> FACTORY = new Binary64Factory();
	public static final BinaryCodec<Binary256> CODEC = new BinaryCodec<>(19, 236, true, FACTORY);

	public static final Binary256 POSITIVE_INFINITY = new Binary256(POSITIVE, FloatingType.INFINITE);
	public static final Binary256 NEGATIVE_INFINITY = new Binary256(NEGATIVE, FloatingType.INFINITE);
	public static final Binary256 QUIET_NAN = new Binary256(POSITIVE, FloatingType.QUIET_NAN);
	public static final Binary256 SIGNALING_NAN = new Binary256(POSITIVE, FloatingType.SIGNALING_NAN);
	
	public static final Binary256 MAX_VALUE = CODEC.getMaxValue();
	public static final Binary256 MIN_VALUE = CODEC.getMinSubnormalValue();
	public static final Binary256 MIN_NORMAL = CODEC.getMinValue();

	private Binary256(int signum, BigDecimal value) {
		super(signum, value);
	}

	private Binary256(int signum, FloatingType type) {
		super(signum, type);
	}

	/** {@inheritDoc} */
	@Override
	public BinaryCodec<Binary256> getCodec() {
		return CODEC;
	}
	
	private static class Binary64Factory implements FloatingFactory<Binary256> {
		
		@Override
		public Binary256 create(int signum, BigDecimal value) {
			return new Binary256(signum, value);
		}
		
		@Override
		public Binary256 create(int signum, FloatingType type) {
			return new Binary256(signum, type);
		}
		
	}
	
}
