package top.zibin.luban;

/**
 * Created on 2018/1/3 19:43
 *
 * @author andy
 * <p>
 * A functional interface (callback) that returns true or false for the given input path should be compressed.
 */

public interface CompressionPredicate {

    /**
     * Determine the given input path should be compressed and return a boolean.
     *
     * @param mimeType input mimeType
     * @return the boolean result
     */
    boolean apply(String mimeType);
}
