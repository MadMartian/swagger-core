package com.wordnik.swagger.jaxrs.config

import com.wordnik.swagger.core.{ SwaggerSpec, SwaggerContext }
import com.wordnik.swagger.config.{ SwaggerConfig, FilterFactory }
import com.wordnik.swagger.core.filter.SwaggerSpecFilter

import org.slf4j.LoggerFactory

import javax.servlet._
import com.wordnik.swagger.jaxrs.reader.ResourceFactory
import com.wordnik.swagger.interfaces.IJaxrsResourceFactory

class WebXMLReader(implicit servletConfig: ServletConfig) extends SwaggerConfig {
  private val LOGGER = LoggerFactory.getLogger(classOf[WebXMLReader])

  apiVersion = {
    servletConfig.getInitParameter("api.version") match {
      case e: String => {
        LOGGER.debug("set api.version to " + e); e
      }
      case _ => ""
    }
  }
  swaggerVersion = SwaggerSpec.version
  basePath = servletConfig.getInitParameter("swagger.api.basepath") match {
    case e: String => {
      LOGGER.debug("set swagger.api.basepath to " + e); e
    }
    case _ => ""
  }
  servletConfig.getInitParameter("swagger.filter") match {
    case e: String if(e != "") => {
      try {
        FilterFactory.filter = SwaggerContext.loadClass(e).newInstance.asInstanceOf[SwaggerSpecFilter]
        LOGGER.debug("set swagger.filter to " + e)
      }
      catch {
        case ex: Exception => LOGGER.error("failed to load filter " + e, ex)
      }
    }
    case _ =>
  }
  servletConfig.getInitParameter("swagger.jaxrs.factory") match {
    case e: String if !e.isEmpty => {
      try
      {
        ResourceFactory.factory = SwaggerContext.loadClass(e).newInstance.asInstanceOf[IJaxrsResourceFactory]
        LOGGER.debug("set swagger.context.factory to " + e);
      } catch {
        case ex: Exception => LOGGER.error("failed to load resource factory " + e, ex);
      }
    }
  }
}
