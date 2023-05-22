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
import java.math.RoundingMode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This class implements the IEEE 754 {@link Rounding rounding modes}.
 * 
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
enum RoundingImpl {
	
	/*
	 * SGF  = significand
	 * DISC = discarded
	 * N    = sign bit
	 * G    = guard bit
	 * R    = round bit
	 * S    = sticky bit (bitwise OR)
	 * 
	 *  SGF   DISC
	 * +---+ +-------+
	 * ... G R SSSS...
	 * 
	 * --- round to nearest, ties to even ---
	 * 
	 * G R S
	 * 0 0 0 down (= 0)
	 * 0 0 1 down (< .5)
	 * 0 1 0 down (= .5, already even)
	 * 0 1 1 up   (> .5)
	 * 1 0 0 down (= 0)
	 * 1 0 1 down (< .5)
	 * 1 1 0 up   (= .5, not even yet)
	 * 1 1 1 up   (> .5)
	 * 
	 * shouldRoundUp = (R & S) | (G & R)
	 * 
	 * --- round to nearest, ties away from zero ---
	 * 
	 * G R S
	 * 0 0 0 down (= 0)
	 * 0 0 1 down (< .5)
	 * 0 1 0 up   (= .5, away from zero)
	 * 0 1 1 up   (> .5)
	 * 1 0 0 down (= 0)
	 * 1 0 1 down (< .5)
	 * 1 1 0 up   (= .5, away from zero)
	 * 1 1 1 up   (> .5)
	 * 
	 * shouldRoundUp = R
	 * 
	 * --- toward 0 ---
	 * 
	 * G R S
	 * 0 0 0 down (= 0)
	 * 0 0 1 down (< .5)
	 * 0 1 0 down (= .5)
	 * 0 1 1 down (> .5)
	 * 1 0 0 down (= 0)
	 * 1 0 1 down (< .5)
	 * 1 1 0 down (= .5)
	 * 1 1 1 down (> .5)
	 * 
	 * shouldRoundUp = 0
	 * 
	 * --- toward +inf ---
	 * 
	 * G R S
	 * 0 0 0 down    (= 0)
	 * 0 0 1 up/down (< .5, sign dependent: +/-)
	 * 0 1 0 up/down (= .5, sign dependent: +/-)
	 * 0 1 1 up/down (> .5, sign dependent: +/-)
	 * 1 0 0 down    (= 0)
	 * 1 0 1 up/down (< .5, sign dependent: +/-)
	 * 1 1 0 up/down (= .5, sign dependent: +/-)
	 * 1 1 1 up/down (> .5, sign dependent: +/-)
	 * 
	 * shouldRoundUp = !N & (R | S)
	 * 
	 * --- toward -inf ---
	 * 
	 * G R S
	 * 0 0 0 down    (= 0)
	 * 0 0 1 down/up (< .5, sign dependent: +/-)
	 * 0 1 0 down/up (= .5, sign dependent: +/-)
	 * 0 1 1 down/up (> .5, sign dependent: +/-)
	 * 1 0 0 down    (= 0)
	 * 1 0 1 down/up (< .5, sign dependent: +/-)
	 * 1 1 0 down/up (= .5, sign dependent: +/-)
	 * 1 1 1 down/up (> .5, sign dependent: +/-)
	 * 
	 * shouldRoundUp = N & (R | S)
	 */

	/** round to nearest, ties to even */
	TIES_EVEN(RoundingMode.HALF_EVEN) {
		
		@Override
		protected boolean round(boolean sign, boolean guard, boolean round, boolean sticky) {
			// round to nearest number; if the number falls midway, round to nearest even number
			return (guard && round) || (round && sticky);
		}
		
	},

	/** round to nearest, ties away from zero */
	TIES_AWAY(RoundingMode.HALF_UP) {
		
		@Override
		protected boolean round(boolean sign, boolean guard, boolean round, boolean sticky) {
			// round to nearest number; if the number falls midway, round to nearest number above (positive number) or below (negative numbers)
			return round;
		}
		
	},

	/** round toward zero */
	TOWARD_ZERO(RoundingMode.DOWN) {
		
		@Override
		protected boolean round(boolean sign, boolean guard, boolean round, boolean sticky) {
			// truncate
			return false;
		}
		
	},

	/** round toward positive infinity */
	TOWARD_POSITIVE(RoundingMode.CEILING) {
		
		@Override
		protected boolean round(boolean sign, boolean guard, boolean round, boolean sticky) {
			// round to nearest number above
			return !sign && (round || sticky);
		}
		
	},

	/** round toward negative infinity */
	TOWARD_NEGATIVE(RoundingMode.FLOOR) {
		
		@Override
		protected boolean round(boolean sign, boolean guard, boolean round, boolean sticky) {
			// round to nearest number below
			return sign && (round || sticky);
		}
		
	}
	
	;
	
	@Getter
	private final Rounding instance;
	
	private RoundingImpl(RoundingMode decimalMode) {
		instance = new Rounding() {
			
			@Override
			public BigDecimal roundDecimal(BigDecimal value) {
				return value.setScale(0, decimalMode);
			}
			
			@Override
			public boolean roundBinary(boolean sign, boolean guard, boolean round, boolean sticky) {
				return round(sign, guard, round, sticky);
			}
			
		};
	}
	
	protected abstract boolean round(boolean sign, boolean guard, boolean round, boolean sticky);
	
}
