/*
 * Copyright 2012 Midokura PTE LTD.
 */
package org.midonet.api.network.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import org.midonet.api.validation.MessageProperty;

@Target({ TYPE, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = RouteNextHopPortConstraintValidator.class)
@Documented
public @interface NextHopPortValid {

    String message() default MessageProperty.ROUTE_NEXT_HOP_PORT_NOT_NULL;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}