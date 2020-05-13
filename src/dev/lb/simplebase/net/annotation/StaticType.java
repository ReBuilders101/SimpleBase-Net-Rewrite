package dev.lb.simplebase.net.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A class annotated with {@link StaticType} only contains static methods. Instances can not or should not created.
 * They are equivalent to static classes in C#
 */
@Retention(CLASS)
@Target(TYPE)
public @interface StaticType {}
