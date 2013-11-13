package com.wordnik.swagger.jaxrs.reader

import com.wordnik.swagger.jaxrs.{DefaultJaxrsResourceFactory}
import com.wordnik.swagger.interfaces.IJaxrsResourceFactory

object ResourceFactory
{
  var factory : IJaxrsResourceFactory = new DefaultJaxrsResourceFactory();
}
