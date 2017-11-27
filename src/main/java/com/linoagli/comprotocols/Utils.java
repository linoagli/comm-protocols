/**
 * comm-protocols Project.
 * com.linoagli.comprotocols
 *
 * @author Olubusayo K. Faye-Lino Agli, username: linoagli
 */
package com.linoagli.comprotocols;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.ListIterator;

public class Utils {
    public static boolean isStringEmpty(String string) {
        return string == null || string.trim().isEmpty();
    }

    public static String listItemsToString(List<?> list, String separator) {
        if (list == null) return null;

        StringBuilder sb = new StringBuilder();
        ListIterator<?> iterator = list.listIterator();

        while (iterator.hasNext()) {
            Object item = iterator.next();
            sb.append(item);
            if (separator != null && iterator.hasNext()) sb.append(separator);
        }

        return sb.toString();
    }

    public static String encodeBase64(Object object) {
        String encodedObject = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.flush();
            oos.close();

            encodedObject = Base64.getEncoder().encodeToString(baos.toByteArray());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return encodedObject;
    }
}
