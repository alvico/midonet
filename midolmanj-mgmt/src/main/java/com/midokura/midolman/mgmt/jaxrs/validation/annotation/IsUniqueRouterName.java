/*
 * Copyright 2012 Midokura PTE LTD.
 */
package com.midokura.midolman.mgmt.jaxrs.validation.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.midokura.midolman.mgmt.jaxrs.validation.MessageProperty;
import com.midokura.midolman.mgmt.jaxrs.validation.constraint.RouterNameConstraintValidator;

@Target({ TYPE, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = RouterNameConstraintValidator.class)
@Documented
public @interface IsUniqueRouterName {

    String message() default MessageProperty.IS_UNIQUE_ROUTER_NAME;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}