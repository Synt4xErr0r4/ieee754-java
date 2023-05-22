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

import at.syntaxerror.ieee754.binary.Binary;
import at.syntaxerror.ieee754.binary.Binary128;
import at.syntaxerror.ieee754.binary.Binary16;
import at.syntaxerror.ieee754.binary.Binary256;
import at.syntaxerror.ieee754.binary.Binary32;
import at.syntaxerror.ieee754.binary.Binary64;
import at.syntaxerror.ieee754.binary.Binary80;
import at.syntaxerror.ieee754.binary.BinaryCodec;

/**
 * @author Thomas Kasper
 * 
 */
class BinaryTest {
	
	private static final Random RANDOM = new Random();
	
	private static final int RANDOM_COUNT = 25;
	
	private static final int POSITIVE = +1;
	private static final int NEGATIVE = -1;
	
	private static final BinaryCodec<?>[] CODECS = {
		Binary16.CODEC,
		Binary32.CODEC,
		Binary64.CODEC,
		Binary80.CODEC,
		Binary128.CODEC,
		Binary256.CODEC
	};
	
	private static final String[] CODEC_NAMES = {
		"binary16",
		"binary32",
		"binary64",
		"binary80",
		"binary128",
		"binary256"
	};
	
	private static final int[] SIGNUMS = { NEGATIVE, POSITIVE };
	
	private static final BigInteger[][] INFINITIES = { // [0] = -Infinity, [1] = +Infinity
		// binary16
		{
			hex("fc00"),
			hex("7c00"),
		},
		// binary32
		{
			hex("ff80 0000"),
			hex("7f80 0000"),
		},
		// binary64
		{
			hex("fff0 0000 0000 0000"),
			hex("7ff0 0000 0000 0000"),
		},
		// binary80
		{
			hex("ffff 8000 0000 0000 0000"),
			hex("7fff 8000 0000 0000 0000")
		},
		// binary128
		{
			hex("ffff 0000 0000 0000 0000 0000 0000 0000"),
			hex("7fff 0000 0000 0000 0000 0000 0000 0000"),
		},
		// binary256
		{
			hex("ffff f000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000"),
			hex("7fff f000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000"),
		},
	};
	
	private static final BigInteger[][] NANS = { // [0] = qNaN, [1] = sNaN	(sign bit clear, LSB set)
		// binary16
		{
			hex("7e01"),
			hex("7c01"),
		},
		// binary32
		{
			hex("7fc0 0001"),
			hex("7f80 0001"),
		},
		// binary64
		{
			hex("7ff8 0000 0000 0001"),
			hex("7ff0 0000 0000 0001"),
		},
		// binary80
		{
			hex("7fff C000 0000 0000 0001"),
			hex("7fff 8000 0000 0000 0001")
		},
		// binary128
		{
			hex("7fff 8000 0000 0000 0000 0000 0000 0001"),
			hex("7fff 0000 0000 0000 0000 0000 0000 0001"),
		},
		// binary256
		{
			hex("7fff f800 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0001"),
			hex("7fff f000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0001"),
		},
	};
	
	private static final BigInteger[][] ZEROS = { // [0] = -0, [1] = +0
		// binary16
		{
			hex("8000"),
			hex("0000"),
		},
		// binary32
		{
			hex("8000 0000"),
			hex("0000 0000"),
		},
		// binary64
		{
			hex("8000 0000 0000 0000"),
			hex("0000 0000 0000 0000"),
		},
		// binary80
		{
			hex("8000 0000 0000 0000 0000"),
			hex("0000 0000 0000 0000 0000")
		},
		// binary128
		{
			hex("8000 0000 0000 0000 0000 0000 0000 0000"),
			hex("0000 0000 0000 0000 0000 0000 0000 0000"),
		},
		// binary256
		{
			hex("8000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000"),
			hex("0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000"),
		},
	};

	private static BigInteger hex(String hex) {
		return new BigInteger(hex.replace(" ", ""), 16);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static boolean compare(Binary a, Binary b) {
		return a.compareTo(b) == 0;
	}
	
	private static boolean compare(BigInteger a, BigInteger b) {
		return a.compareTo(b) == 0;
	}
	
	private static char getSignumChar(int signum) {
		return signum == NEGATIVE ? '-' : '+';
	}
	
	private static String formatCodec(BinaryCodec<?> codec) {
		for(int i = 0; i < CODECS.length; ++i)
			if(CODECS[i] == codec)
				return CODEC_NAMES[i];
		
		return "<unknown codec>";
	}
	
	private <T extends Binary<T>> void testInfinity(BinaryCodec<T> codec, int signum, BigInteger expectation) {
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
	}
	
	private <T extends Binary<T>> void testNaN(BinaryCodec<T> codec, boolean quiet, BigInteger expectation) {
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
	}
	
	private <T extends Binary<T>> void testZero(BinaryCodec<T> codec, int signum, BigInteger expectation) {
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
	}
	
	private <T extends Binary<T>> void testReencode(BinaryCodec<T> codec, BigInteger value) {
		BigInteger encoded = codec.decode(value).encode();
		
		assertTrue(
			compare(encoded, value),
			"0x" + value.toString(16) + " re-encoding doesn't match (got 0x" + encoded.toString(16) + ") @ " + formatCodec(codec)
		);
	}
	
	private void testRedecode(BinaryCodec<?> codec, Binary<?> value) {
		Binary<?> decoded = codec.decode(value.encode());
		
		assertTrue(
			compare(value, decoded),
			value + " re-decoding doesn't match (got " + decoded + ") @ " + formatCodec(codec)
		);
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
			
			int sig = codec.getSignificandBits();
			int exp = codec.getExponentBits();
			
			for(int j = 0; j < RANDOM_COUNT; ++j) {
				
				// random sign
				BigInteger bigint = RANDOM.nextBoolean() ? BigInteger.ONE : BigInteger.ZERO;
				
				bigint = bigint.shiftLeft(exp) // random exponent
					.or(BigInteger.valueOf(RANDOM.nextLong(0, (1L << exp) - 1)));
				
				// add explicit bit so that the number is normalized. reencoding the number always yields a normalized number and the encodings would not match otherwise 
				if(!codec.isImplicit())
					bigint = bigint.shiftLeft(1)
						.or(BigInteger.ONE);

				for(int off = 0; off < sig; off += 32) { // random significand
					long part = RANDOM.nextLong() & 0xFFFFFFFFL;
					
					int len = sig - off * 32;
					
					if(len < 32)
						part &= (1L << len) - 1L;
					else len = 32;
					
					bigint = bigint.shiftLeft(len)
						.or(BigInteger.valueOf(part));
				}
				
				assertTrue(bigint.signum() > -1);
				
				testReencode(codec, bigint);
			}
		}
	}

}
