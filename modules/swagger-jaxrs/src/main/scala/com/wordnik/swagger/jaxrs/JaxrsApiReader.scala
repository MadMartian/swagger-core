package com.wordnik.swagger.jaxrs

import com.wordnik.swagger.annotations._
import com.wordnik.swagger.config._
import com.wordnik.swagger.reader.{ScalaCardinality, ClassReader, ClassReaderUtils}
import com.wordnik.swagger.core._
import com.wordnik.swagger.core.util._
import com.wordnik.swagger.core.ApiValues._
import com.wordnik.swagger.model._

import org.slf4j.LoggerFactory

import java.lang.reflect.{ Method, Type, Field }
import java.lang.annotation.Annotation

import javax.ws.rs._
import javax.ws.rs.core.Context

import reader.ResourceFactory
import scala.collection.JavaConverters._
import scala.collection.mutable.{ ListBuffer, HashMap, HashSet }
import java.util
import com.wordnik.swagger.interfaces.{Cardinality, ExampleInfo, IRequestEntityExampleGenerator}
import collection.{immutable, mutable}
import java.lang.ClassNotFoundException

import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.reflect.api.Types

import com.wordnik.swagger.interfaces.Cardinality.{auto, singular, multiple}

trait JaxrsApiReader extends ClassReader with ClassReaderUtils {
  private val LOGGER = LoggerFactory.getLogger(classOf[JaxrsApiReader])
  val GenericTypeMapper = "([a-zA-Z\\.]*)<([a-zA-Z0-9\\.\\,\\s]*)>".r

  var topparams : List[Parameter] = List.empty

  // decorates a Parameter based on annotations, returns None if param should be ignored
  def processParamAnnotations(mutable: MutableParameter, paramAnnotations: Array[Annotation]): Option[Parameter]

  def processDataType(paramType: Class[_], genericParamType: Type) = {
    paramType.getName match {
      case "[I" => "Array[int]"
      case "[Z" => "Array[boolean]"
      case "[D" => "Array[double]"
      case "[F" => "Array[float]"
      case "[J" => "Array[long]"
      case _ => {
        if(paramType.isArray) {
          "Array[%s]".format(paramType.getComponentType.getName)
        }
        else {
          genericParamType.toString match {
            case GenericTypeMapper(container, base) => {
              val qt = SwaggerTypes(base.split("\\.").last) match {
                case "object" => base
                case e: String => e
              }
              val b = ModelUtil.modelFromString(qt) match {
                case Some(e) => e._2.qualifiedType
                case None => qt
              }
              "%s[%s]".format(normalizeContainer(container), b)
            }
            case _ => paramType.getName
          }
        }
      }
    }
  }

  def normalizeContainer(str: String) = {
    if(str.indexOf(".List") >= 0) "List"
    else if(str.indexOf(".Set") >= 0) "Set"
    else {
      println("UNKNOWN TYPE: " + str)
      "UNKNOWN"
    }
  }

  def accountForEnum (param : MutableParameter, cls : Class[_]) =
  {
    if (cls.isEnum)
    {
      param.allowableValues = AllowableListValues(cls.getEnumConstants.map(_.toString).toList)
      param.dataType = "enum"
    }

    cls.isEnum
  }

  def parseOperation(
    method: Method,
    apiOperation: ApiOperation,
    apiResponses: List[ResponseMessage],
    isDeprecated: String,
    parentParams: List[Parameter],
    parentMethods: ListBuffer[Method]
  ) = {
    val api = method.getAnnotation(classOf[Api])
    val responseClass = {
      val baseName = apiOperation.response.getName
      val output = apiOperation.responseContainer match {
        case "" => baseName
        case e: String => "%s[%s]".format(e, baseName)
      }
      output
    }

    var paramAnnotations: Array[Array[java.lang.annotation.Annotation]] = null
    var paramTypes: Array[java.lang.Class[_]] = null
    var genericParamTypes: Array[java.lang.reflect.Type] = null

    if (parentMethods.isEmpty) {
      paramAnnotations = method.getParameterAnnotations
      paramTypes = method.getParameterTypes
      genericParamTypes = method.getGenericParameterTypes
    } else {
      paramAnnotations = parentMethods.map(pm => pm.getParameterAnnotations).reduceRight(_ ++ _) ++ method.getParameterAnnotations
      paramTypes = parentMethods.map(pm => pm.getParameterTypes).reduceRight(_ ++ _) ++ method.getParameterTypes
      genericParamTypes = parentMethods.map(pm => pm.getGenericParameterTypes).reduceRight(_ ++ _) ++ method.getGenericParameterTypes
    }

    val produces: List[String] = apiOperation.produces match {
      case Array(x, y @ _*) => (x +: y).toList
      case _ => method.getAnnotation(classOf[Produces]) match {
        case e: Produces => e.value.toList
        case _ => List()
      }
    }
    val consumes: List[String] = apiOperation.consumes match {
      case Array(x, y @ _*) => (x +: y).toList
      case _ => method.getAnnotation(classOf[Consumes]) match {
        case e: Consumes => e.value.toList
        case _ => List()
      }
    }
    val protocols: List[String] = apiOperation.protocols match {
      case Array(x, y @ _*) => (x +: y).toList
      case _ => List()
    }
    val authorizations: List[String] = apiOperation.authorizations match {
      case Array(x, y @ _*) => (x +: y).toList
      case _ => List()
    }

    val exgen: Option[Class[_ <: IRequestEntityExampleGenerator]]
    =
      (parentMethods :+ method).filter(m => m.getAnnotation(classOf[ApiOperation]) match {
        case null => false
        case x if x.exampleGenerator() != classOf[IRequestEntityExampleGenerator] => true
        case _ => false
      }) match {
        case x if !x.isEmpty => Some(x.head.getAnnotation(classOf[ApiOperation]).exampleGenerator())
        case _ => None
      }

    val exparamples : Map[String,String] = {
      exgen match
      {
        case Some(generator) => {
          try
          {
            val factory: IRequestEntityExampleGenerator = ResourceFactory.factory.acquireResource(generator)

            val h = new immutable.HashMap[String,String]

            h ++ (for (a <- factory.parameters(method.getDeclaringClass, method)) yield (a(0), a(1)))
          } catch {
            case e: Exception => {
              LOGGER.debug("Error! could not generate example! ", e)
              Map.empty
            }
          }
        }
        case _ => Map.empty
      }
    }

    val params = parentParams ++ (for((annotations, paramType, genericParamType) <- (paramAnnotations, paramTypes, genericParamTypes).zipped.toList) yield {
      if(annotations.length > 0) {
        val param = new MutableParameter

        if (!accountForEnum(param, paramType))
          param.dataType = processDataType(paramType, genericParamType)
        param.allowMultiple = Cardinality.isMultiple(paramType)
        processParamAnnotations(param, annotations, exparamples)
      }
      else /* If it doesn't have annotations, it must be a body parameter, and it's safe to assume that there will only
              ever be one of these in the sequence according to JSR-339 JAX-RS 2.0 section 3.3.2.1. */
      {
        val param = new MutableParameter
        param.dataType = paramType.getName
        param.name = TYPE_BODY
        param.paramType = TYPE_BODY

        Some(param.asParameter)
      }
    }).flatten.toList

    val implicitParams = {
      LOGGER.debug("checking for implicits")
      Option(method.getAnnotation(classOf[ApiImplicitParams])) match {
        case Some(e) => {
          (for(param <- e.value) yield {
            LOGGER.debug("processing " + param)
            val allowableValues = toAllowableValues(param.allowableValues)
            Parameter(
              param.name,
              Option(readString(param.value)),
              exparamples.get(param.name) orElse Option(param.defaultValue) filter(_.trim.nonEmpty),
              param.required,
              param.cardinality.name() match {
                case ScalaCardinality.auto => Cardinality.isMultiple(param.dataType)
                case ScalaCardinality.singular => false
                case ScalaCardinality.multiple => true
              },
              param.dataType.getName,
              allowableValues,
              param.paramType.name(),
              Option(param.access).filter(_.trim.nonEmpty))
          }).toList
        }
        case _ => List()
      }
    }

    // Implicit parameter definitions may override regular ones
    val combinedParams = (params :\ implicitParams)((x,y) => {
      y exists (p2 => x.name == p2.name) match {
        case true => y
        case false => x +: y
      }
    })

    val sortedParams = combinedParams.sortWith((x, y) => {
      if (x.paramType != y.paramType)
        x.paramType < y.paramType
      else
        x.name < y.name
    })

    val examples : List[Example] =
      (combinedParams.find(p => p.paramType == TYPE_BODY), exgen) match
      {
         case (Some(param), Some(generator)) if ! param.allowMultiple => {
           try
           {
            acquireEntityExample(generator, method.getDeclaringClass, method, Class.forName(param.dataType))
           } catch
           {
             case e: Exception => {
               LOGGER.debug("Error! could not generate example! ", e)
               List.empty
             }
           }
         }
         case _ => List.empty
      }

    Operation(
      parseHttpMethod(method, apiOperation),
      apiOperation.value,
      apiOperation.notes,
      responseClass,
      method.getName,
      apiOperation.position,
      produces,
      consumes,
      protocols,
      authorizations,
      sortedParams,
      apiResponses,
      examples,
      Option(isDeprecated))
  }

  def processParamAnnotations(param: MutableParameter, annotations: Array[Annotation], exparamples : Map[String, String]): Option[Parameter] = {
    processParamAnnotations(param, annotations)
      .map(e =>
      exparamples.get(param.name) match {
        case Some(j) if !j.isEmpty => {
          param.defaultValue = Some(j)
          param.asParameter
        }
        case _ => e
      }
    )
  }

  def acquireEntityExample(generator: Class[_ <: IRequestEntityExampleGenerator], cResourceClass: Class[_], mMethod: Method, cDataType: Class[_]): List[Example] = {
    if (generator != classOf[IRequestEntityExampleGenerator]) {
      val factory: IRequestEntityExampleGenerator = ResourceFactory.factory.acquireResource(generator)
      (
        for (e <- factory.entity(cResourceClass, mMethod, cDataType))
        yield Example(e.mediatype, e.example)
        ).toList
    } else
      List.empty
  }

  def readMethod(method: Method, parentParams: List[Parameter], parentMethods: ListBuffer[Method]) = {
    val apiOperation = method.getAnnotation(classOf[ApiOperation])
    val responseAnnotation = method.getAnnotation(classOf[ApiResponses])
    val apiResponses = {
      if(responseAnnotation == null) List()
      else (for(response <- responseAnnotation.value) yield {
        val apiResponseClass = {
          if(response.response != classOf[Void])
            Some(response.response.getName)
          else None
        }
        ResponseMessage(response.code, response.message, apiResponseClass)}
      ).toList
    }
    val isDeprecated = Option(method.getAnnotation(classOf[Deprecated])).map(m => "true").getOrElse(null)

    parseOperation(method, apiOperation, apiResponses, isDeprecated, parentParams, parentMethods)
  }

  def appendOperation(endpoint: String, path: String, op: Operation, operations: ListBuffer[Tuple3[String, String, ListBuffer[Operation]]]) = {
    operations.filter(op => op._1 == endpoint) match {
      case e: ListBuffer[Tuple3[String, String, ListBuffer[Operation]]] if(e.size > 0) => e.head._3 += op
      case _ => operations += Tuple3(endpoint, path, new ListBuffer[Operation]() ++= List(op))
    }
  }

  def read(docRoot: String, cls: Class[_], config: SwaggerConfig): Option[ApiListing] = {
    readRecursive(docRoot, "", cls, config, new ListBuffer[Tuple3[String, String, ListBuffer[Operation]]], new ListBuffer[Method])
  }

  def readRecursive(
    docRoot: String, 
    parentPath: String, cls: Class[_], 
    config: SwaggerConfig,
    operations: ListBuffer[Tuple3[String, String, ListBuffer[Operation]]],
    parentMethods: ListBuffer[Method]): Option[ApiListing] = {
    val api = cls.getAnnotation(classOf[Api])

    // must have @Api annotation to process!
    if(api != null) {
      val consumes = Option(api.consumes) match {
        case Some(e) if(e != "") => e.split(",").map(_.trim).toList
        case _ => cls.getAnnotation(classOf[Consumes]) match {
          case e: Consumes => e.value.toList
          case _ => List()
        }
      }
      val produces = Option(api.produces) match {
        case Some(e) if(e != "") => e.split(",").map(_.trim).toList
        case _ => cls.getAnnotation(classOf[Produces]) match {
          case e: Produces => e.value.toList
          case _ => List()
        }
      }
      val protocols = Option(api.protocols) match {
        case Some(e) if(e != "") => e.split(",").map(_.trim).toList
        case _ => List()
      }
      val description = api.description match {
        case e: String if(e != "") => Some(e)
        case _ => None
      }
      // look for method-level annotated properties
      val parentParams: List[Parameter] = extractClassLevelParams(cls)

      for(method <- cls.getMethods) {
        val path = method.getAnnotation(classOf[Path]) match {
          case e: Path => e.value()
          case _ => ""
        }
        val endpoint = parentPath + api.value + pathFromMethod(method)
        val aApiOp: ApiOperation = method.getAnnotation(classOf[ApiOperation])
        val returnType = aApiOp match {
          case null => method.getReturnType
          case _ => aApiOp.response()
        }
        Option(returnType.getAnnotation(classOf[Api])) match {
          case Some(e) => {
            val root = docRoot + api.value + pathFromMethod(method)
            parentMethods += method
            readRecursive(root, endpoint, returnType, config, operations, parentMethods)
            parentMethods -= method
          }
          case _ => {
            if(aApiOp != null) {
              val op = readMethod(method, topparams ++ parentParams, parentMethods)
              appendOperation(endpoint, path, op, operations)
            }
          }
        }
      }
      // sort them by min position in the operations
      val s = (for(op <- operations) yield {
        (op, op._3.map(_.position).toList.min)
      }).sortWith((x, y) => {
        if (x._2 != y._2)
          x._2 < y._2
        else
          x._1._1 < y._1._1
      }).toList
      val orderedOperations = new ListBuffer[Tuple3[String, String, ListBuffer[Operation]]]
      s.foreach(op => {
        val ops = op._1._3.sortWith((x, y) => {

          val fnMethRank = (x: String) => x toUpperCase match {
            case "GET" => 0
            case "POST" => 1
            case "PATCH" => 2
            case "PUT" => 3
            case "DELETE" => 4
          }

          if (x.position != y.position)
            x.position < y.position
          else if (x.method != y.method)
            fnMethRank(x.method) < fnMethRank(y.method)
          else
            x.nickname < y.nickname
        })
        orderedOperations += Tuple3(op._1._1, op._1._2, ops)
      })
      val apis = (for ((endpoint, resourcePath, operationList) <- orderedOperations) yield {
        val orderedOperations = new ListBuffer[Operation]
        operationList.sortWith(_.position < _.position).foreach(e => orderedOperations += e)
        ApiDescription(
          addLeadingSlash(endpoint),
          None,
          orderedOperations.toList)
      }).toList
      val models = ModelUtil.modelsFromApis(apis)
      Some(ApiListing (
        apiVersion = config.apiVersion,
        swaggerVersion = config.swaggerVersion,
        basePath = config.basePath,
        resourcePath = addLeadingSlash(api.value),
        apis = ModelUtil.stripPackages(apis),
        models = models,
        description = description,
        produces = produces,
        consumes = consumes,
        protocols = protocols,
        position = api.position)
      )
    }
    else None
  }

  def extractClassLevelParams(cls: Class[_]): List[Parameter] =
  {
    (for (field <- getAllFields(cls))
    yield {
      // only process fields with @ApiParam, @QueryParam, @HeaderParam, @PathParam
      if (field.getAnnotation(classOf[QueryParam]) != null || field.getAnnotation(classOf[HeaderParam]) != null ||
        field.getAnnotation(classOf[HeaderParam]) != null || field.getAnnotation(classOf[PathParam]) != null ||
        field.getAnnotation(classOf[ApiParam]) != null) {
        val param = new MutableParameter

        if (!accountForEnum(param, field.getType)) {
          param.dataType = field.getType.getName
          Option(field.getAnnotation(classOf[ApiParam])) match {
            case Some(annotation) => toAllowableValues(annotation.allowableValues)
            case _ =>
          }
        }
        val annotations = field.getAnnotations
        processParamAnnotations(param, annotations) // TODO: Support example values for parent parameters
      }
      else None
    }
      ).flatten.toList
  }

  def getAllFields(cls: Class[_]): List[Field] = {
    var fields = cls.getDeclaredFields().toList;                 
    if (cls.getSuperclass() != null) {
        fields = getAllFields(cls.getSuperclass()) ++ fields;
    }   
    return fields;
  }
  
  def pathFromMethod(method: Method): String = {
    val path = method.getAnnotation(classOf[javax.ws.rs.Path])
    if(path == null) ""
    else path.value
  }

  def parseApiParamAnnotation(param: MutableParameter, annotation: ApiParam) {
    param.name = readString(annotation.name, param.name)
    param.description = Option(readString(annotation.value))
    param.defaultValue = Option(readString(annotation.defaultValue, param.defaultValue.getOrElse(null)))

    try {
      param.allowableValues = toAllowableValues(annotation.allowableValues, param.allowableValues)
    } catch {
      case e: Exception =>
        LOGGER.error("Allowable values annotation problem in method for parameter " + param.name)
    }
    param.required = annotation.required
    param.allowMultiple = annotation.cardinality.name() match
    {
      case ScalaCardinality.auto => param.allowMultiple
      case ScalaCardinality.singular => false
      case ScalaCardinality.multiple => true
    }
    param.paramAccess = Option(readString(annotation.access))
  }

  def readString(value: String, defaultValue: String = null, ignoreValue: String = null): String = {
    if (defaultValue != null && defaultValue.trim.length > 0) defaultValue
    else if (value == null) null
    else if (value.trim.length == 0) null
    else if (ignoreValue != null && value.equals(ignoreValue)) null
    else value.trim
  }

  def parseHttpMethod(method: Method, op: ApiOperation): String = {
    if (op.httpMethod() != null && op.httpMethod().trim().length() > 0)
      op.httpMethod().trim
    else {
      if(method.getAnnotation(classOf[GET]) != null) "GET"
      else if(method.getAnnotation(classOf[DELETE]) != null) "DELETE"
      else if(method.getAnnotation(classOf[PATCH]) != null) "PATCH"
      else if(method.getAnnotation(classOf[POST]) != null) "POST"
      else if(method.getAnnotation(classOf[PUT]) != null) "PUT"
      else if(method.getAnnotation(classOf[HEAD]) != null) "HEAD"
      else if(method.getAnnotation(classOf[OPTIONS]) != null) "OPTIONS"
      else null
    }
  }

  def addLeadingSlash(e: String): String = {
    e.startsWith("/") match {
      case true => e
      case false => "/" + e
    }
  }
}


class MutableParameter(param: Parameter) {
  var name: String = _
  var description: Option[String] = None
  var defaultValue: Option[String] = None
  var required: Boolean = _
  var allowMultiple: Boolean = false
  var dataType: String = _
  var allowableValues: AllowableValues = AnyAllowableValues
  var paramType: String = _
  var paramAccess: Option[String] = None

  if(param != null) {
    this.name = param.name
    this.description = param.description
    this.defaultValue = param.defaultValue
    this.required = param.required
    this.allowMultiple = param.allowMultiple
    this.dataType = param.dataType
    this.allowableValues = param.allowableValues
    this.paramType = param.paramType
    this.paramAccess = param.paramAccess
  }

  def this() = this(null)

  def asParameter() = {
    Parameter(name,
      description,
      defaultValue,
      required,
      allowMultiple,
      dataType,
      allowableValues,
      paramType,
      paramAccess)
  }
}