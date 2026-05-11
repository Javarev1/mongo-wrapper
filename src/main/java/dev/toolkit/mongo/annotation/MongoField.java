package dev.toolkit.mongo.annotation;

import java.lang.annotation.*;

/**
 * @author revqz
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface MongoField {

    // bson key, defaults to field name
    String value() default "";

    boolean index() default false;

    boolean unique() default false;

    // skips serialization entirely
    boolean ignore() default false;
}
