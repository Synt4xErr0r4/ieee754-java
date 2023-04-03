/**
 * 
 */
package at.syntaxerror.ieee754;

import java.math.BigDecimal;

/**
 * This class is used to create new Binary objects
 * 
 * @author SyntaxError404
 * 
 */
public interface BinaryFactory<T extends Binary<T>> {

	/**
	 * Creates a new Binary
	 * 
	 * @param signum the signum
	 * @param type the type
	 * @return the new Binary
	 */
	T create(int signum, BinaryType type);

	/**
	 * Creates a new Binary
	 * 
	 * @param signum the signum
	 * @param value the value
	 * @return the new Binary
	 */
	T create(int signum, BigDecimal value);

	/**
	 * Creates a new Binary, with the signum derived from the BigDecimal
	 * 
	 * @param value the value
	 * @return the new Binary
	 */
	default T create(BigDecimal value) {
		int signum = value.signum();
		return create(signum == 0 ? 1 : signum, value);
	}
	
}
