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
package at.syntaxerror.ieee754.unittest;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import at.syntaxerror.ieee754.rounding.Rounding;

/**
 * @author Thomas Kasper
 * 
 */
class RoundingTest {
	
	private static final Rounding[] MODES = {
		Rounding.TIES_EVEN,
		Rounding.TIES_AWAY,
		Rounding.TOWARD_ZERO,
		Rounding.TOWARD_POSITIVE,
		Rounding.TOWARD_NEGATIVE
	};
	
	private static final String[] MODE_NAMES = {
		"ties even",
		"ties away",
		"toward 0",
		"toward +inf",
		"toward -inf"
	};
	
	// positive even: 50.25, 50.50, 50.75
	// positive odd:  51.25, 51.50, 51.75
	private static final BigDecimal PE25 = new BigDecimal("50.25");
	private static final BigDecimal PE50 = new BigDecimal("50.5");
	private static final BigDecimal PE75 = new BigDecimal("50.75");
	private static final BigDecimal PO25 = new BigDecimal("51.25");
	private static final BigDecimal PO50 = new BigDecimal("51.5");
	private static final BigDecimal PO75 = new BigDecimal("51.75");

	// nearest positive values (low, mid, high)
	private static final BigDecimal PL = new BigDecimal("50");
	private static final BigDecimal PM = new BigDecimal("51");
	private static final BigDecimal PH = new BigDecimal("52");

	// negative even: -50.25, -50.50, -50.75
	// negative odd:  -51.25, -51.50, -51.75
	private static final BigDecimal NE25 = new BigDecimal("-50.25");
	private static final BigDecimal NE50 = new BigDecimal("-50.5");
	private static final BigDecimal NE75 = new BigDecimal("-50.75");
	private static final BigDecimal NO25 = new BigDecimal("-51.25");
	private static final BigDecimal NO50 = new BigDecimal("-51.5");
	private static final BigDecimal NO75 = new BigDecimal("-51.75");

	// nearest negative values (low, mid, high)
	private static final BigDecimal NL = new BigDecimal("-50");
	private static final BigDecimal NM = new BigDecimal("-51");
	private static final BigDecimal NH = new BigDecimal("-52");
	
	private static final BigDecimal[][][] DECIMAL_TESTS = {
		// [a][b][c]: a: mode; b: test sample; c: 0=input, 1=output
		
		{ // ties even
			{ PE25, PL }, // nearest = down
			{ PE50, PL }, // even    = down
			{ PE75, PM }, // nearest = up
			{ PO25, PM }, // nearest = down
			{ PO50, PH }, // even    = up
			{ PO75, PH }, // nearest = up
			{ NE25, NL }, // nearest = down
			{ NE50, NL }, // even    = down
			{ NE75, NM }, // nearest = up
			{ NO25, NM }, // nearest = down
			{ NO50, NH }, // even    = up
			{ NO75, NH }, // nearest = up
		},
		
		{ // ties away
			{ PE25, PL }, // nearest = down
			{ PE50, PM }, // away    = up
			{ PE75, PM }, // nearest = up
			{ PO25, PM }, // nearest = down
			{ PO50, PH }, // away    = up
			{ PO75, PH }, // nearest = up
			{ NE25, NL }, // nearest = down
			{ NE50, NM }, // away    = up
			{ NE75, NM }, // nearest = up
			{ NO25, NM }, // nearest = down
			{ NO50, NH }, // away    = up
			{ NO75, NH }, // nearest = up
		},
		
		{ // toward 0
			{ PE25, PL },
			{ PE50, PL },
			{ PE75, PL },
			{ PO25, PM },
			{ PO50, PM },
			{ PO75, PM },
			{ NE25, NL },
			{ NE50, NL },
			{ NE75, NL },
			{ NO25, NM },
			{ NO50, NM },
			{ NO75, NM },
		},
		
		{ // toward +inf
			{ PE25, PM },
			{ PE50, PM },
			{ PE75, PM },
			{ PO25, PH },
			{ PO50, PH },
			{ PO75, PH },
			{ NE25, NL },
			{ NE50, NL },
			{ NE75, NL },
			{ NO25, NM },
			{ NO50, NM },
			{ NO75, NM },
		},
		
		{ // toward -inf
			{ PE25, PL },
			{ PE50, PL },
			{ PE75, PL },
			{ PO25, PM },
			{ PO50, PM },
			{ PO75, PM },
			{ NE25, NM },
			{ NE50, NM },
			{ NE75, NM },
			{ NO25, NH },
			{ NO50, NH },
			{ NO75, NH },
		}
	};

	private static final boolean[][][] BINARY_TESTS = {
		// [a][b][c]: a: mode; b: test sample; c: 0=sign, 1=guard, 2=round, 3=sticky, 4=output
		
		{ // ties even
			{ false, false, false, false, false, },
			{ false, false, false, true,  false, },
			{ false, false, true,  false, false, },
			{ false, false, true,  true,  true,  },
			{ false, true,  false, false, false, },
			{ false, true,  false, true,  false, },
			{ false, true,  true,  false, true,  },
			{ false, true,  true,  true,  true,  },
			{ true,  false, false, false, false, },
			{ true,  false, false, true,  false, },
			{ true,  false, true,  false, false, },
			{ true,  false, true,  true,  true,  },
			{ true,  true,  false, false, false, },
			{ true,  true,  false, true,  false, },
			{ true,  true,  true,  false, true,  },
			{ true,  true,  true,  true,  true,  },
		},
		
		{ // ties away
			{ false, false, false, false, false, },
			{ false, false, false, true,  false, },
			{ false, false, true,  false, true,  },
			{ false, false, true,  true,  true,  },
			{ false, true,  false, false, false, },
			{ false, true,  false, true,  false, },
			{ false, true,  true,  false, true,  },
			{ false, true,  true,  true,  true,  },
			{ true,  false, false, false, false, },
			{ true,  false, false, true,  false, },
			{ true,  false, true,  false, true,  },
			{ true,  false, true,  true,  true,  },
			{ true,  true,  false, false, false, },
			{ true,  true,  false, true,  false, },
			{ true,  true,  true,  false, true,  },
			{ true,  true,  true,  true,  true,  },
		},
		
		{ // toward 0
			{ false, false, false, false, false, },
			{ false, false, false, true,  false, },
			{ false, false, true,  false, false, },
			{ false, false, true,  true,  false, },
			{ false, true,  false, false, false, },
			{ false, true,  false, true,  false, },
			{ false, true,  true,  false, false, },
			{ false, true,  true,  true,  false, },
			{ true,  false, false, false, false, },
			{ true,  false, false, true,  false, },
			{ true,  false, true,  false, false, },
			{ true,  false, true,  true,  false, },
			{ true,  true,  false, false, false, },
			{ true,  true,  false, true,  false, },
			{ true,  true,  true,  false, false, },
			{ true,  true,  true,  true,  false, },
		},
		
		{ // toward +inf
			{ false, false, false, false, false, },
			{ false, false, false, true,  true,  },
			{ false, false, true,  false, true,  },
			{ false, false, true,  true,  true,  },
			{ false, true,  false, false, false, },
			{ false, true,  false, true,  true,  },
			{ false, true,  true,  false, true,  },
			{ false, true,  true,  true,  true,  },
			{ true,  false, false, false, false, },
			{ true,  false, false, true,  false, },
			{ true,  false, true,  false, false, },
			{ true,  false, true,  true,  false, },
			{ true,  true,  false, false, false, },
			{ true,  true,  false, true,  false, },
			{ true,  true,  true,  false, false, },
			{ true,  true,  true,  true,  false, },
		},
		
		{ // toward -inf
			{ false, false, false, false, false, },
			{ false, false, false, true,  false, },
			{ false, false, true,  false, false, },
			{ false, false, true,  true,  false, },
			{ false, true,  false, false, false, },
			{ false, true,  false, true,  false, },
			{ false, true,  true,  false, false, },
			{ false, true,  true,  true,  false, },
			{ true,  false, false, false, false, },
			{ true,  false, false, true,  true,  },
			{ true,  false, true,  false, true,  },
			{ true,  false, true,  true,  true,  },
			{ true,  true,  false, false, false, },
			{ true,  true,  false, true,  true,  },
			{ true,  true,  true,  false, true,  },
			{ true,  true,  true,  true,  true,  },
		}
	};
	
	private void testMode(int id) {
		Rounding mode = MODES[id];
		String name = MODE_NAMES[id];
		
		// Decimal tests
		
		BigDecimal[][] decimalTests = DECIMAL_TESTS[id];
		
		for(int i = 0; i < decimalTests.length; ++i) {
			BigDecimal[] test = decimalTests[i];
			
			BigDecimal rounded = mode.roundDecimal(test[0]);
			
			assertEquals(
				rounded.compareTo(test[1]), 0,
				"Rounding mode " + name + " failed for decimal test " + i + ": Expected " + test[1] + ", got " + rounded + " instead"
			);
		}
		
		// Binary tests
		
		boolean[][] binaryTests = BINARY_TESTS[id];
		
		for(int i = 0; i < binaryTests.length; ++i) {
			boolean[] test = binaryTests[i];
			
			boolean round = mode.roundBinary(test[0], test[1], test[2], test[3]);
			
			assertTrue(
				"Rounding mode " + name + " failed for binary test " + i + ": Expected " + test[4] + ", got " + round + " instead",
				round == test[4]
			);
		}
	}
	
	@Test
	void testModes() {
		for(int i = 0; i < MODES.length; ++i)
			testMode(i);
	}

}
