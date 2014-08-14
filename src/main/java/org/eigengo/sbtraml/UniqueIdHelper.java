package org.eigengo.sbtraml;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * You can iterate over a list using the built-in each helper. Inside the
 * block, you can use <code>this</code> to reference the element being
 * iterated over.
 *
 * @author janm399
 * @since 0.3.0
 */
public class UniqueIdHelper implements Helper<Object> {

    private static MessageDigest md;
    static {
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ignored) {

        }
    }

    /**
     * A singleton instance of this helper.
     */
    public static final Helper<Object> INSTANCE = new UniqueIdHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "uniqueId";

    @SuppressWarnings({"rawtypes", "unchecked" })
    @Override
    public CharSequence apply(final Object context, final Options options)
            throws IOException {
        if (context == null) {
            return StringUtils.EMPTY;
        }

        byte[] mdbytes = md.digest(context.toString().getBytes());
        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (byte mdbyte : mdbytes) {
            sb.append(Integer.toString((mdbyte & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

}
