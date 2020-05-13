package dev.lb.simplebase.net.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotated elements are used internally by the API and should not be called/instantiated by API users.
 * Internal elements may have incomplete/missing Javadoc as they are not supposed to be used.
 */
@Retention(CLASS)
@Target({ TYPE, METHOD, CONSTRUCTOR })
public @interface Internal {

}
