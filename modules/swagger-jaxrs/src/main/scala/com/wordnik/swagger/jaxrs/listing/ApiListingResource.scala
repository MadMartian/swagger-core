package com.wordnik.swagger.jaxrs.listing

import com.wordnik.swagger.annotations._
import com.wordnik.swagger.jaxrs._

import javax.ws.rs._
import javax.ws.rs.core.MediaType

@Path("/model.sw")
@Api("/model.sw")
@Produces(Array(MediaType.APPLICATION_JSON))
class ApiListingResourceJSON extends ApiListingResource
