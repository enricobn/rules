package org.rules;

import java.util.List;

/**
 * Created by enrico on 5/28/15.
 */
public interface JavaUI {

    <T> T choose(String title, String message, List<T> items);

}
