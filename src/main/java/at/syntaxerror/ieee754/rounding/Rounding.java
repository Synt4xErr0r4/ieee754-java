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
package at.syntaxerror.ieee754.rounding;

import java.math.BigDecimal;

import at.syntaxerror.ieee754.Floating;
import at.syntaxerror.ieee754.FloatingCodec;
import lombok.NonNull;

/**
 * This class is used for rounding {@link Floating IEEE 754 floating-point numbers}.
 * 
 * <p>
 * There are 5 predefined rounding modes:
 * 
 * <ul>
 * 	<li>{@link #TIES_EVEN round to nearest, ties to even}</li>
 * 	<li>{@link #TIES_AWAY round to nearest, ties away from zero}</li>
 * 	<li>{@link #TOWARD_ZERO round toward 0}</li>
 * 	<li>{@link #TOWARD_POSITIVE round toward +∞}</li>
 * 	<li>{@link #TOWARD_NEGATIVE round toward -∞}</li>
 * </ul>
 * 
 * {@link #DEFAULT_ROUNDING} controls the default rounding mode used by {@link FloatingCodec#encode(Floating)} and its descendents.
 * 
 * @author Thomas Kasper
 * 
 */
public abstract class Rounding {

	/**
	 * Round to nearest, ties to even.
	 * <p>
	 * Rounds to the nearest value. If the number falls midway, it is rounded to the nearest even value.
	 */
	public static final Rounding TIES_EVEN = RoundingImpl.TIES_EVEN.getInstance();
	
	/**
	 * Round to nearest, ties away from zero
	 * <p>
	 * Rounds to the nearest value. If the number falls midway, it is rounded to the
	 * nearest value above (positive numbers) or below (negative numbers).
	 */
	public static final Rounding TIES_AWAY = RoundingImpl.TIES_AWAY.getInstance();

	/**
	 * Round toward 0
	 * <p>
	 * Rounds toward zero. Effectively discards any decimal places.
	 */
	public static final Rounding TOWARD_ZERO = RoundingImpl.TOWARD_ZERO.getInstance();
	
	/**
	 * Round toward +∞
	 * <p>
	 * Rounds toward positive infinity, also known as rounding up or ceiling.
	 */
	public static final Rounding TOWARD_POSITIVE = RoundingImpl.TOWARD_POSITIVE.getInstance();

	/**
	 * Round toward -∞
	 * <p>
	 * Rounds toward negative infinity, also known as rounding down or floor.
	 */
	public static final Rounding TOWARD_NEGATIVE = RoundingImpl.TOWARD_NEGATIVE.getInstance();
	
	/**
	 * The rounding method used by {@link FloatingCodec#encode(at.syntaxerror.ieee754.Floating)} and its descendents.
	 */
	@NonNull
	public static Rounding DEFAULT_ROUNDING = Rounding.TIES_EVEN;
	
	/**
	 * Rounds a binary floating-point number. The method returns {@code true} when rounding up
	 * and {@code false} when rounding down.
	 * 
	 * <p>
	 * The sign bit specifies whether the number is negative.
	 * The guard bit is the least significant bit than can be stored (the ULP).
	 * The round bit is the bit following the guard bit.
	 * The sticky bit is the bitwise OR of all other following bits.
	 * 
	 * @param sign the sign bit
	 * @param guard the guard bit
	 * @param round the round bit
	 * @param sticky the sticky bit
	 * @return whether the number is rounded up
	 */
	public abstract boolean roundBinary(boolean sign, boolean guard, boolean round, boolean sticky);
	
	/**
	 * Rounds the {@link BigDecimal} (to 0 decimal places) according to the rounding mode's rules
	 * 
	 * @param value the input value
	 * @return the rounded value
	 */
	public abstract BigDecimal roundDecimal(BigDecimal value);
	
}
