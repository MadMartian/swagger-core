package com.wordnik.swagger.jaxrs.reader

import com.wordnik.swagger.jaxrs.{DefaultJaxrsResourceFactory, IJaxrsResourceFactory}

object ResourceFactory
{
  var factory : IJaxrsResourceFactory = new DefaultJaxrsResourceFactory();
}
