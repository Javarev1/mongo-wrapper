package dev.toolkit.mongo.annotation;

import java.lang.annotation.*;

/**
 * @author revqz
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface MongoDocument {

    String collection();

    // overrides MongoConfig database
    String database() default "";
}
