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
 * This class implements the IEEE 754 binary64 floating point specification 
 * 
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings({ "serial" })
public final class Decimal64 extends Decimal<Decimal64> {
	
	public static final FloatingFactory<Decimal64> FACTORY = new Binary32Factory();
	public static final DecimalCodec<Decimal64> CODEC = new DecimalCodec<>(13, 50, FACTORY);
	
	public static final Decimal64 POSITIVE_INFINITY = new Decimal64(POSITIVE, FloatingType.INFINITE);
	public static final Decimal64 NEGATIVE_INFINITY = new Decimal64(NEGATIVE, FloatingType.INFINITE);
	public static final Decimal64 QUIET_NAN = new Decimal64(POSITIVE, FloatingType.QUIET_NAN);
	public static final Decimal64 SIGNALING_NAN = new Decimal64(POSITIVE, FloatingType.SIGNALING_NAN);
	
	public static final Decimal64 MAX_VALUE = CODEC.getMaxValue();
	public static final Decimal64 MIN_VALUE = CODEC.getMinSubnormalValue();
	public static final Decimal64 MIN_NORMAL = CODEC.getMinValue();

	private Decimal64(int signum, BigDecimal value) {
		super(signum, value);
	}

	private Decimal64(int signum, FloatingType type) {
		super(signum, type);
	}

	/** {@inheritDoc} */
	@Override
	public DecimalCodec<Decimal64> getCodec() {
		return CODEC;
	}
	
	private static class Binary32Factory implements FloatingFactory<Decimal64> {
		
		@Override
		public Decimal64 create(int signum, BigDecimal value) {
			return new Decimal64(signum, value);
		}
		
		@Override
		public Decimal64 create(int signum, FloatingType type) {
			return new Decimal64(signum, type);
		}
		
	}
	
}
