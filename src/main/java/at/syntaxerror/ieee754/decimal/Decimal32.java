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
package at.syntaxerror.ieee754.decimal;

import java.math.BigDecimal;

import at.syntaxerror.ieee754.FloatingFactory;
import at.syntaxerror.ieee754.FloatingType;

/**
 * This class implements the IEEE 754 binary32 floating point specification 
 * 
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings({ "serial" })
public final class Decimal32 extends Decimal<Decimal32> {
	
	public static final FloatingFactory<Decimal32> FACTORY = new Binary32Factory();
	public static final DecimalCodec<Decimal32> CODEC = new DecimalCodec<>(11, 20, FACTORY);
	
	public static final Decimal32 POSITIVE_INFINITY = new Decimal32(POSITIVE, FloatingType.INFINITE);
	public static final Decimal32 NEGATIVE_INFINITY = new Decimal32(NEGATIVE, FloatingType.INFINITE);
	public static final Decimal32 QUIET_NAN = new Decimal32(POSITIVE, FloatingType.QUIET_NAN);
	public static final Decimal32 SIGNALING_NAN = new Decimal32(POSITIVE, FloatingType.SIGNALING_NAN);
	
	public static final Decimal32 MAX_VALUE = CODEC.getMaxValue();
	public static final Decimal32 MIN_VALUE = CODEC.getMinSubnormalValue();
	public static final Decimal32 MIN_NORMAL = CODEC.getMinValue();

	private Decimal32(int signum, BigDecimal value) {
		super(signum, value);
	}

	private Decimal32(int signum, FloatingType type) {
		super(signum, type);
	}

	/** {@inheritDoc} */
	@Override
	public DecimalCodec<Decimal32> getCodec() {
		return CODEC;
	}
	
	private static class Binary32Factory implements FloatingFactory<Decimal32> {
		
		@Override
		public Decimal32 create(int signum, BigDecimal value) {
			return new Decimal32(signum, value);
		}
		
		@Override
		public Decimal32 create(int signum, FloatingType type) {
			return new Decimal32(signum, type);
		}
		
	}
	
}
