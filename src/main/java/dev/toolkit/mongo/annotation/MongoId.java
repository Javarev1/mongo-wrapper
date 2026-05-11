package dev.toolkit.mongo.annotation;

import java.lang.annotation.*;

/**
 * @author revqz
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface MongoId {
}
