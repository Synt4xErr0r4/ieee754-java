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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.Random;

import org.junit.jupiter.api.Test;

import at.syntaxerror.ieee754.decimal.Decimal;
import at.syntaxerror.ieee754.decimal.Decimal128;
import at.syntaxerror.ieee754.decimal.Decimal32;
import at.syntaxerror.ieee754.decimal.Decimal64;
import at.syntaxerror.ieee754.decimal.DecimalCodec;
import at.syntaxerror.ieee754.decimal.DecimalCoding;

/**
 * @author Thomas Kasper
 * 
 */
class DecimalTest {
	
	private static final Random RANDOM = new Random();
	
	private static final int RANDOM_COUNT = 25;
	
	private static final int POSITIVE = +1;
	private static final int NEGATIVE = -1;
	
	private static final DecimalCodec<?>[] CODECS = {
		Decimal32.CODEC,
		Decimal64.CODEC,
		Decimal128.CODEC
	};
	
	private static final String[] CODEC_NAMES = {
		"decimal32",
		"decimal64",
		"decimal128"
	};
	
	private static final int[] SIGNUMS = { NEGATIVE, POSITIVE };
	
	private static final BigInteger[][] INFINITIES = { // [0] = -Infinity, [1] = +Infinity
		// decimal32
		{
			hex("f800 0000"),
			hex("7800 0000"),
		},
		// decimal64
		{
			hex("f800 0000 0000 0000"),
			hex("7800 0000 0000 0000"),
		},
		// decimal128
		{
			hex("f800 0000 0000 0000 0000 0000 0000 0000"),
			hex("7800 0000 0000 0000 0000 0000 0000 0000"),
		},
	};
	
	private static final BigInteger[][] NANS = { // [0] = qNaN, [1] = sNaN	(sign bit clear)
		// decimal32
		{
			hex("7c00 0000"),
			hex("7e00 0000"),
		},
		// decimal64
		{
			hex("7c00 0000 0000 0000"),
			hex("7e00 0000 0000 0000"),
		},
		// decimal128
		{
			hex("7c00 0000 0000 0000 0000 0000 0000 0000"),
			hex("7e00 0000 0000 0000 0000 0000 0000 0000"),
		},
	};
	
	private static final BigInteger[][] ZEROS = { // [0] = -0, [1] = +0
		// decimal32
		{
			hex("8000 0000"),
			hex("0000 0000"),
		},
		// decimal64
		{
			hex("8000 0000 0000 0000"),
			hex("0000 0000 0000 0000"),
		},
		// decimal128
		{
			hex("8000 0000 0000 0000 0000 0000 0000 0000"),
			hex("0000 0000 0000 0000 0000 0000 0000 0000"),
		},
	};

	private static BigInteger hex(String hex) {
		return new BigInteger(hex.replace(" ", ""), 16);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static boolean compare(Decimal a, Decimal b) {
		return a.compareTo(b) == 0;
	}
	
	private static boolean compare(BigInteger a, BigInteger b) {
		return a.compareTo(b) == 0;
	}
	
	private static char getSignumChar(int signum) {
		return signum == NEGATIVE ? '-' : '+';
	}
	
	private static String formatCodec(DecimalCodec<?> codec) {
		for(int i = 0; i < CODECS.length; ++i)
			if(CODECS[i] == codec)
				return CODEC_NAMES[i] + "#" + Decimal.DEFAULT_CODING;
		
		return "<unknown codec>";
	}
	
	private void testCodings(Runnable test) {
		Decimal.DEFAULT_CODING = DecimalCoding.BINARY_INTEGER_DECIMAL;
		test.run();

		Decimal.DEFAULT_CODING = DecimalCoding.DENSLY_PACKED_DECIMAL;
		test.run();
	}
	
	private <T extends Decimal<T>> void testInfinity(DecimalCodec<T> codec, int signum, BigInteger expectation) {
		testCodings(() -> {
			BigInteger inf = signum == NEGATIVE
				? codec.getNegativeInfinity()
				: codec.getPositiveInfinity();
			
			assertTrue(
				compare(inf, expectation),
				getSignumChar(signum) + "Infinity encoding doesn't match (got 0x" + inf.toString(16) + ") @ " + formatCodec(codec)
			);
			
			T decoded = codec.decode(inf);
			
			assertTrue(
				signum == NEGATIVE
					? decoded.isNegativeInfinity()
					: decoded.isPositiveInfinity(),
				getSignumChar(signum) + "Infinity does not decode properly (got " + decoded + ") @ " + formatCodec(codec)
			);
			
			BigInteger encoded = decoded.encode();
			
			assertTrue(
				compare(encoded, expectation),
				getSignumChar(signum) + "Infinity re-encoding doesn't match (got 0x" + encoded.toString(16) + ") @ " + formatCodec(codec)
			);
		});
	}
	
	private <T extends Decimal<T>> void testNaN(DecimalCodec<T> codec, boolean quiet, BigInteger expectation) {
		testCodings(() -> {	
			BigInteger nan = quiet
				? codec.getQuietNaN(POSITIVE)
				: codec.getSignalingNaN(POSITIVE);
			
			String name = quiet ? "qNaN" : "sNaN";
			
			assertTrue(
				compare(nan, expectation),
				name + " encoding doesn't match (got 0x" + nan.toString(16) + ") @ " + formatCodec(codec)
			);
			
			T decoded = codec.decode(nan);
			
			assertTrue(
				decoded.isPositive()
					&& quiet
						? decoded.isQuietNaN()
						: decoded.isSignalingNaN(),
				name + " does not decode properly (got " + decoded + ") @ " + formatCodec(codec)
			);
			
			BigInteger encoded = decoded.encode();
			
			assertTrue(
				compare(encoded, expectation),
				name + " re-encoding doesn't match (got 0x" + encoded.toString(16) + ") @ " + formatCodec(codec)
			);
		});
	}
	
	private <T extends Decimal<T>> void testZero(DecimalCodec<T> codec, int signum, BigInteger expectation) {
		testCodings(() -> {	
			BigInteger zero = codec.getZero(signum);
			
			assertTrue(
				compare(zero, expectation),
				getSignumChar(signum) + "0 encoding doesn't match (got 0x" + zero.toString(16) + ") @ " + formatCodec(codec)
			);
			
			T decoded = codec.decode(zero);
			
			assertTrue(
				signum == NEGATIVE
					? decoded.isNegativeZero()
					: decoded.isPositiveZero(),
				getSignumChar(signum) + "0 does not decode properly (got " + decoded + ") @ " + formatCodec(codec)
			);
			
			BigInteger encoded = decoded.encode();
			
			assertTrue(
				compare(encoded, expectation),
				getSignumChar(signum) + "0 re-encoding doesn't match (got 0x" + encoded.toString(16) + ") @ " + formatCodec(codec)
			);
		});
	}
	
	private <T extends Decimal<T>> void testReencode(DecimalCodec<T> codec, BigInteger value) {
		BigInteger encoded = codec.decode(value).encode();
		
		assertTrue(
			compare(encoded, value),
			"0x" + value.toString(16) + " re-encoding doesn't match (got 0x" + encoded.toString(16) + ") @ " + formatCodec(codec)
		);
	}
	
	private void testRedecode(DecimalCodec<?> codec, Decimal<?> value) {
		testCodings(() -> {
			Decimal<?> decoded = codec.decode(value.encode());
			
			assertTrue(
				compare(value, decoded),
				value + " re-decoding doesn't match (got " + decoded + ") @ " + formatCodec(codec)
			);
		});
	}
	
	@Test
	void testInfinity() {
		for(int i = 0; i < CODECS.length; ++i)
			for(int j = 0; j < SIGNUMS.length; ++j)
				testInfinity(CODECS[i], SIGNUMS[j], INFINITIES[i][j]);
	}
	
	@Test
	void testNaN() {
		for(int i = 0; i < CODECS.length; ++i) {
			testNaN(CODECS[i], true, NANS[i][0]);
			testNaN(CODECS[i], false, NANS[i][1]);
		}
	}
	
	@Test
	void testZero() {
		for(int i = 0; i < CODECS.length; ++i)
			for(int j = 0; j < SIGNUMS.length; ++j)
				testZero(CODECS[i], SIGNUMS[j], ZEROS[i][j]);
	}
	
	@Test
	void testMinMax() {
		for(int i = 0; i < CODECS.length; ++i) {
			var codec = CODECS[i];
			
			testRedecode(codec, codec.getMaxValue());
			testRedecode(codec, codec.getMinValue());
			testRedecode(codec, codec.getMinSubnormalValue());
		}
	}
	
	@Test
	void testRandom() {
		for(int i = 0; i < CODECS.length; ++i) {
			var codec = CODECS[i];
			
			int sigBits = codec.getSignificandBits();
			int combBits = codec.getCombinationBits();
			int digits = codec.getSignificandDigits();
			int maxExp = (codec.getExponentRange().getValue() - 1) * 2;
			
			final var MASK = BigInteger.ONE.shiftLeft(sigBits).subtract(BigInteger.ONE);
			
			for(int j = 0; j < RANDOM_COUNT; ++j) {
				
				// random sign
				BigInteger sign = RANDOM.nextBoolean() ? BigInteger.ONE : BigInteger.ZERO;
				
				// random exponent
				int exp = RANDOM.nextInt(0, maxExp);
				
				// BID: avoid overflow
				BigInteger sigBID = BigInteger.ZERO;
				
				for(int k = 0; k < digits - 1; ++k)
					sigBID = sigBID
						.add(BigInteger.valueOf(RANDOM.nextLong(0, 10)))
						.multiply(BigInteger.TEN);
				
				sigBID = sigBID.add(BigInteger.valueOf(RANDOM.nextLong(1, 10)));
				
				// DPD: clear dont-care bits
				BigInteger sigDPD = BigInteger.ZERO;
				
				for(int k = 0; k < sigBits; k += 10) {
					int part = RANDOM.nextInt(0, 0x400);
					
					if((part & 0x6E) == 0x6E)
						part &= 0xFF;
					
					sigDPD = sigDPD
						.shiftLeft(10)
						.or(BigInteger.valueOf(part));
				}
				
				sigDPD = sigDPD.or(BigInteger.ONE); // make sure least significant digit is not 0
				
				/** assemble combination field **/
				
				int digit = sigBID.shiftRight(sigBits).intValue();
				sigBID = sigBID.and(MASK);
				
				boolean large = false;
				
				if(digit > 7) {
					large = true;
					digit &= 1;
				}
					
				// BID
				BigInteger num = sign.shiftLeft(combBits);
				
				if(large)
					num = num.or(
						BigInteger.valueOf(0b11)
							.shiftLeft(combBits - 3)
							.or(BigInteger.valueOf(exp))
							.shiftLeft(1)
							.or(BigInteger.valueOf(digit))
					);
				else num = num.or(
						BigInteger.valueOf(exp)
							.shiftLeft(3)
							.or(BigInteger.valueOf(digit))
					);
				
				num = num.shiftLeft(sigBits).or(sigBID);

				Decimal.DEFAULT_CODING = DecimalCoding.BINARY_INTEGER_DECIMAL;
				testReencode(codec, num);
				
				// DPD
				num = sign.shiftLeft(combBits);
				
				int expHi = exp >> (combBits - 5);
				int expLo = exp & ((1 << (combBits - 5)) - 1);

				if(large)
					num = num.or(
						BigInteger.valueOf(0b1100)
							.or(BigInteger.valueOf(expHi))
							.shiftLeft(1)
							.or(BigInteger.valueOf(digit))
							.shiftLeft(combBits - 5)
							.or(BigInteger.valueOf(expLo))
					);
				else num = num.or(
						BigInteger.valueOf(expHi)
							.shiftLeft(3)
							.or(BigInteger.valueOf(digit))
							.shiftLeft(combBits - 5)
							.or(BigInteger.valueOf(expLo))
					);
				
				num = num.shiftLeft(sigBits).or(sigDPD);

				Decimal.DEFAULT_CODING = DecimalCoding.DENSLY_PACKED_DECIMAL;
				testReencode(codec, num);
			}
		}
	}
	
	/*
	 * 1 001101   011 001 110 0   101 000 111 1
	 * 
	 * e-88	  1   9 1 2   		  9 8 5
	 * 
	 * 1 001101   001 011 111 1   001 100 111 1
	 * 
	 * e-88	  1   9 1 9			  1 8 9
	 */

}
