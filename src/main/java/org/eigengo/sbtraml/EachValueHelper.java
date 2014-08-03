package org.eigengo.sbtraml;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.helper.EachHelper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * You can iterate over a list using the built-in each helper. Inside the
 * block, you can use <code>this</code> to reference the element being
 * iterated over.
 *
 * @author janm399
 * @since 0.3.0
 */
public class EachValueHelper implements Helper<Object> {

    /**
     * A singleton instance of this helper.
     */
    public static final Helper<Object> INSTANCE = new EachHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "each";

    @SuppressWarnings({"rawtypes", "unchecked" })
    @Override
    public CharSequence apply(final Object context, final Options options)
            throws IOException {
        if (context == null) {
            return StringUtils.EMPTY;
        }
        if (context instanceof Map) {
            return mapContext((Map<?, ?>) context, options);
        }
        return hashContext(context, options);
    }

    /**
     * Iterate over a hash like object.
     *
     * @param context The context object.
     * @param options The helper options.
     * @return The string output.
     * @throws IOException If something goes wrong.
     */
    private CharSequence hashContext(final Object context, final Options options)
            throws IOException {
        Set<Map.Entry<String, Object>> propertySet = options.propertySet(context);
        StringBuilder buffer = new StringBuilder();
        Context parent = options.context;
        for (Map.Entry<String, Object> entry : propertySet) {
            Context current = Context.newContext(parent, entry.getValue())
                    .data("key", entry.getKey());
            buffer.append(options.fn(current));
        }
        return buffer.toString();
    }

    /**
     * Iterate over an iterable object.
     *
     * @param context The context object.
     * @param options The helper options.
     * @return The string output.
     * @throws IOException If something goes wrong.
     */
    private CharSequence mapContext(final Map<?, ?> context, final Options options)
            throws IOException {
        StringBuilder buffer = new StringBuilder();
        if (options.isFalsy(context)) {
            buffer.append(options.inverse());
        } else {
            Iterator<?> iterator = context.values().iterator();
            int index = -1;
            Context parent = options.context;
            while (iterator.hasNext()) {
                index += 1;
                Object element = iterator.next();
                boolean first = index == 0;
                boolean even = index % 2 == 0;
                boolean last = !iterator.hasNext();
                Context current = Context.newBuilder(parent, element)
                        .combine("@index", index)
                        .combine("@first", first ? "first" : "")
                        .combine("@last", last ? "last" : "")
                        .combine("@odd", even ? "" : "odd")
                        .combine("@even", even ? "even" : "")
                        .build();
                buffer.append(options.fn(current));
                current.destroy();
            }
        }
        return buffer.toString();
    }

}
