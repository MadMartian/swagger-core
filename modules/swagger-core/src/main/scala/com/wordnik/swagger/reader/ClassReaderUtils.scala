package com.wordnik.swagger.reader

import com.wordnik.swagger.model._
import com.wordnik.swagger.annotations.ApiAllowableValues

trait ClassReaderUtils {
  final val POSITIVE_INFINITY_STRING = "Infinity"
  final val NEGATIVE_INFINITY_STRING = "-Infinity"
  final val `def`: String = "##default"

  def toAllowableValues(annotation: ApiAllowableValues, defaults: AllowableValues = AnyAllowableValues): AllowableValues =
  {
    (annotation.min(), annotation.max(), annotation.list()) match
    {
      case (`def`, `def`,   Array()) => defaults
      case (_,      _,      Array(_, _*)) => AllowableListValues(annotation.list().toList)
      case (_,      `def`,  Array()) => AllowableRangeValues(annotation.min(), Float.MaxValue.toString)
      case (`def`,  _,      Array()) => AllowableRangeValues(Float.MinValue.toString, annotation.max())
      case (_,      _,      Array()) => AllowableRangeValues(annotation.min(), annotation.max())
    }
  }
}