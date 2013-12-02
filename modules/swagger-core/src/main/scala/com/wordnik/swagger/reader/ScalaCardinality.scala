package com.wordnik.swagger.reader

import com.wordnik.swagger.interfaces.Cardinality

/** ============================================================================

   Name     : ScalaCardinality.java
   System   : com.wordnik.swagger.jaxrs.reader
   Language : Java
   Purpose  : < put class description here >
   Author               Date            Changes made
   ------------------   ------------    -------------------------------
   JNeufeld     27-11-2013         Original definition

                  Copyright (c) 1994 - 2013, Safe Software Inc.
                              All Rights Reserved

    This software may not be copied or reproduced, in all or in part,
    without the prior written consent of Safe Software Inc.

    The entire risk as to the results and performance of the software,
    supporting text and other information contained in this file
    (collectively called the "Software") is with the user.
    In no event will Safe Software Incorporated be liable for damages,
    including loss of profits or consequential damages, arising out of
    the use of the Software.
// ============================================================================ **/

object ScalaCardinality
{
  final val auto = Cardinality.auto.name()
  final val singular = Cardinality.singular.name()
  final val multiple = Cardinality.multiple.name()
}
