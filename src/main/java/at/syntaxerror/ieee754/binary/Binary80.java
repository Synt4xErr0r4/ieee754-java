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
 * This class implements the x87 extended precision (80-bit) floating point specification 
 * 
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings({ "serial" })
public final class Binary80 extends Binary<Binary80> {

	public static final FloatingFactory<Binary80> FACTORY = new Binary64Factory();
	public static final BinaryCodec<Binary80> CODEC = new BinaryCodec<>(15, 63, false, FACTORY);

	public static final Binary80 POSITIVE_INFINITY = new Binary80(POSITIVE, FloatingType.INFINITE);
	public static final Binary80 NEGATIVE_INFINITY = new Binary80(NEGATIVE, FloatingType.INFINITE);
	public static final Binary80 QUIET_NAN = new Binary80(POSITIVE, FloatingType.QUIET_NAN);
	public static final Binary80 SIGNALING_NAN = new Binary80(POSITIVE, FloatingType.SIGNALING_NAN);
	
	public static final Binary80 MAX_VALUE = CODEC.getMaxValue();
	public static final Binary80 MIN_VALUE = CODEC.getMinSubnormalValue();
	public static final Binary80 MIN_NORMAL = CODEC.getMinValue();

	private Binary80(int signum, BigDecimal value) {
		super(signum, value);
	}

	private Binary80(int signum, FloatingType type) {
		super(signum, type);
	}

	/** {@inheritDoc} */
	@Override
	public BinaryCodec<Binary80> getCodec() {
		return CODEC;
	}
	
	private static class Binary64Factory implements FloatingFactory<Binary80> {
		
		@Override
		public Binary80 create(int signum, BigDecimal value) {
			return new Binary80(signum, value);
		}
		
		@Override
		public Binary80 create(int signum, FloatingType type) {
			return new Binary80(signum, type);
		}
		
	}
	
}
