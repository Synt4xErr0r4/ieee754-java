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
package at.syntaxerror.ieee754;

import java.math.BigDecimal;

/**
 * This class is used to create new Binary objects
 * 
 * @author Thomas Kasper
 * 
 */
public interface BinaryFactory<T extends Binary<T>> {

	/**
	 * Creates a new {@link Binary}
	 * 
	 * @param signum the signum (either -1, 0, or 1)
	 * @param value the value
	 * @return the new Binary
	 * @deprecated internal use only
	 */
	@Deprecated
	T createUnchecked(int signum, BigDecimal value);

	/**
	 * Creates a new {@link Binary}
	 * 
	 * @param signum the signum (either -1, 0, or 1)
	 * @param type the type
	 * @return the new Binary
	 */
	T create(int signum, BinaryType type);

	/**
	 * Creates a new {@link Binary}.
	 * <p>
	 * The primary use of this function is for creating signed zeros.
	 * In all other cases, {@link #create(BigDecimal)} should be preferred.
	 * 
	 * @param signum the signum (either -1, 0, or 1)
	 * @param value the value
	 * @return the new Binary
	 */
	T create(int signum, BigDecimal value);

	/**
	 * Creates a new {@link Binary}
	 * 
	 * @param value the value
	 * @return the new Binary
	 */
	default T create(BigDecimal value) {
		int signum = value.signum();
		return create(signum == 0 ? 1 : signum, value);
	}

	/**
	 * Creates a new {@link Binary}.
	 * 
	 * @param value the value
	 * @return the new Binary
	 */
	default T create(Number value) {
		return create(new BigDecimal(value.doubleValue()));
	}
	
}
