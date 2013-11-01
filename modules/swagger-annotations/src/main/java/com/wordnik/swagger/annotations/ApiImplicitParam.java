/**
 *  Copyright 2013 Wordnik, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wordnik.swagger.annotations;

import com.wordnik.swagger.interfaces.ParamType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a single parameter in an Api Operation.  A parameter is an input
 * to the operation.  The difference with the ApiImplicitParam is that they are
 * not bound to a variable, and allow for more manually-defined descriptions.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiImplicitParam {
  /** Name of the parameter */
  String name() default "";

  /** Description of the parameter */
  String value() default "";

  /** Default value  - if e.g. no JAX-RS @DefaultValue is given */
  String defaultValue() default "";

  /** Description of values this endpoint accepts */
  ApiAllowableValues allowableValues() default @ApiAllowableValues();

  /** specifies if the parameter is required or not */
  boolean required() default false;

  /** 
   * specify an optional access value for filtering in a Filter 
   * implementation.  This
   * allows you to hide certain parameters if a user doesn't have access to them
   */
  String access() default "";

  /** specifies whether or not the parameter can have multiple values provided */
  boolean allowMultiple() default false;

  /** manually set the dataType */
  Class<?> dataType() default Object.class;

  /** manually set the param type, i.e. query, path, etc. */
  ParamType paramType() default ParamType.auto;
}
