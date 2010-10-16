package com.toluju.oculo.resources

import com.google.inject.name.Named
import com.google.inject.Inject
import com.toluju.oculo.OculoDispatcher
import java.io.File
import javax.ws.rs._
import javax.ws.rs.core.Response
import Helpers._
import org.codehaus.jackson.node.JsonNodeFactory.instance._

@Path("/") 
class Root @Inject()(@Named("filedir") fileDir:String, dispatcher:OculoDispatcher) {
  @GET @Produces(Array("text/html"))
  def index = new File(fileDir, "index.html")

  @GET @Path("hosts") @Produces(Array("application/json"))
  def hosts = {
    val node = arrayNode
    dispatcher.values.foreach(vl => node.add(vl.getHost))
    node
  }

  @Path("hosts/{name}")
  def host(@PathParam("name") name:String) =
    dispatcher.values.find(_.getHost == name).getOrElse(notFound)
}

class Host(val name:String, val metrics:Seq[Metric]) {
  @GET @Produces(Array("application/json"))
  def json = {
    val node = objectNode
    node.put("name", name)
    node
  }

  @Path("metrics/{name}")
  def metric(@PathParam("name") name:String) =
    metrics.find(_.name == name).getOrElse(notFound)
}

class Metric(val name:String) {
  @GET @Produces(Array("application/json"))
  def json = {
    val node = objectNode
    node.put("name", name)
    node
  }
}

object Helpers {
  def notFound = throw new WebApplicationException(
    Response.status(Response.Status.NOT_FOUND).build)
}