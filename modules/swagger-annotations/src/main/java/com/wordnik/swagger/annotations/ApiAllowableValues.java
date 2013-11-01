package com.wordnik.swagger.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * ============================================================================
 * <p/>
 * Name     : ValueRange.java
 * System   : com.wordnik.swagger.annotations
 * Language : Java
 * Purpose  : < put class description here >
 * Author               Date            Changes made
 * ------------------   ------------    -------------------------------
 * JNeufeld     31-10-2013         Original definition
 * <p/>
 * Copyright (c) 1994 - 2013, Safe Software Inc.
 * All Rights Reserved
 * <p/>
 * This software may not be copied or reproduced, in all or in part,
 * without the prior written consent of Safe Software Inc.
 * <p/>
 * The entire risk as to the results and performance of the software,
 * supporting text and other information contained in this file
 * (collectively called the "Software") is with the user.
 * In no event will Safe Software Incorporated be liable for damages,
 * including loss of profits or consequential damages, arising out of
 * the use of the Software.
 * <p/>
 * ============================================================================ *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiAllowableValues
{
    String min() default "##default";
    String max() default "##default";
    String[] list() default {};
}
