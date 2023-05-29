package org.square16.ictdroid.logcat.annotations;

import java.lang.annotation.*;

/**
 * @author Zsx
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogcatParser {
    String verbosity();
}
