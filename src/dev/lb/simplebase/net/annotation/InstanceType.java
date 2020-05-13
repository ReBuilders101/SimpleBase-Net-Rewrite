package dev.lb.simplebase.net.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A class annotated with {@link InstanceType} repersents an object that holds one or more complex states,
 * and it is usually unique and not cloneable. The only equality between two InstanceType object is instance equality (== operator).
 */
@Retention(CLASS)
@Target(TYPE)
public @interface InstanceType {}
