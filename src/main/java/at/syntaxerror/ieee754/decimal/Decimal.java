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
import java.math.BigInteger;

import at.syntaxerror.ieee754.Floating;
import at.syntaxerror.ieee754.FloatingType;
import lombok.NonNull;

/**
 * This class is the base class for implementing IEEE 754 decimal floating point specifications 
 * 
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings({ "serial" })
public abstract class Decimal<T extends Decimal<T>> extends Floating<T> {

	/**
	 * The coding method used by {@link Decimal#encode()} and {@link DecimalCodec#encode(Decimal)}
	 */
	@NonNull
	public static DecimalCoding DEFAULT_CODING = DecimalCoding.BINARY_INTEGER_DECIMAL;
	
	private BigInteger encodedDPD;
	private BigInteger encodedBIP;
	
	public Decimal(int signum, FloatingType type) {
		super(signum, type);
	}

	public Decimal(int signum, BigDecimal value) {
		super(signum, value);
	}

	/**
	 * Encodes this number into its binary representation using the representation method specified by {@link #DEFAULT_CODING}.
	 * 
	 * @return the binary representation
	 */
	@Override
	public BigInteger encode() {
		return DEFAULT_CODING == DecimalCoding.DENSLY_PACKED_DECIMAL
			? encodeDPD()
			: encodeBID();
	}

	/**
	 * Encodes this number into its binary representation using the densly packed decimal representation method
	 * 
	 * @return the binary representation
	 */
	@SuppressWarnings("unchecked")
	public BigInteger encodeDPD() {
		return encodedDPD == null
			? encodedDPD = ((DecimalCodec<T>) getCodec()).encodeDPD((T) this)
			: encodedDPD;
	}

	/**
	 * Encodes this number into its binary representation using the binary integer decimal representation method
	 * 
	 * @return the binary representation
	 */
	@SuppressWarnings("unchecked")
	public BigInteger encodeBID() {
		return encodedBIP == null
			? encodedBIP = ((DecimalCodec<T>) getCodec()).encodeBID((T) this)
			: encodedBIP;
	}
	
}
