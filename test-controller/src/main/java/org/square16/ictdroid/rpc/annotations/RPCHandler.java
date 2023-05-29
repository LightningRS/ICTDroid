package org.square16.ictdroid.rpc.annotations;

import java.lang.annotation.*;

/**
 * @author Zsx
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RPCHandler {
    String name();

    int priority() default 10;

    boolean autoRemove() default true;
}
