package com.toluju.oculo

import com.google.inject.servlet.ServletModule
import com.sun.jersey.api.core.ResourceConfig
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Inject
import com.google.inject.Scopes
import com.google.inject.Singleton
import com.google.inject.name.Names
import com.google.inject.servlet.GuiceFilter
import com.google.inject.servlet.GuiceServletContextListener
import javax.ws.rs.ext.MessageBodyWriter
import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import org.collectd.protocol.UdpReceiver
import org.mortbay.jetty.servlet.{Context => JettyContext}
import org.mortbay.jetty.{Server => JettyServer}
import scala.collection.JavaConversions._
import com.sun.jersey.spi.container.ContainerRequest
import com.sun.jersey.spi.container.ContainerResponse
import com.sun.jersey.spi.container.ContainerResponseFilter
import java.io.File
import java.lang.reflect.Type
import javax.activation.MimetypesFileTypeMap
import javax.servlet.GenericServlet
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider
import javax.ws.rs.Produces
import java.io.OutputStream
import java.lang.annotation.Annotation
import org.codehaus.jackson.JsonEncoding

import com.toluju.oculo.resources._

object Server {
  def main(args:Array[String]) = {
    val injector = Guice.createInjector(new OculoModule)

    startListener(injector)
    startServer(injector)
  }
  
  def startListener(injector:Injector) = {
    val thread = new Thread() {
      override def run = {
        new UdpReceiver(injector.getInstance(classOf[OculoDispatcher])).listen()
      }
    }
    thread.setDaemon(true)
    thread.start
    
    println("Collectd listener started")
  }

  def startServer(injector:Injector) = {
    val server = new JettyServer(8080)
    val context = new JettyContext(server, "/", JettyContext.SESSIONS)
    context.addEventListener(new GuiceServletContextListener {
      override protected def getInjector = injector
    })
    context.addFilter(classOf[GuiceFilter], "/*", 0)
    context.addServlet(classOf[EmptyServlet], "/*")
    server.start
    println("Server started")
  }
}

class EmptyServlet extends GenericServlet {
  override def service(request:ServletRequest, response:ServletResponse) = {
    throw new IllegalStateException("Shouldn't get here")
  }
}

class OculoModule extends ServletModule {
  override def configureServlets = {
    bind(classOf[Root])
    bind(classOf[OculoDispatcher])

    bind(classOf[ObjectMapper]).in(Scopes.SINGLETON)
    bind(classOf[JsonNodeMessageBodyWriter])

    bindConstant.annotatedWith(Names.named("filedir")).to("src/main/webapp")

    val requestFilters = List.empty[Class[_]]// ++ List(classOf[LoggingFilter])
    val responseFilters = List(classOf[StaticContentResponseFilter]) //++ List(classOf[LoggingFilter])

    filter("*").through(classOf[GuiceContainer], Map(
      ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS -> requestFilters.map(_.getName).mkString(","),
      ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS -> responseFilters.map(_.getName).mkString(",")
    ))
  }
}

class StaticContentResponseFilter extends ContainerResponseFilter {
  val mimeTypes = new MimetypesFileTypeMap
  val baseDir = new File("src/main/webapp")

  def filter(request:ContainerRequest, response:ContainerResponse) = {
    if (response.getStatus == Response.Status.NOT_FOUND.getStatusCode) {
      val file = new File(baseDir, request.getPath)
      if (file.exists) {
        val mimeType = MediaType.valueOf(mimeTypes.getContentType(file))
        response.setResponse(Response.ok(file, mimeType).build)
      }
    }
    response
  }
}

@Provider @Produces(Array("application/json")) @Singleton
class JsonNodeMessageBodyWriter @Inject()(val om:ObjectMapper) extends MessageBodyWriter[JsonNode] {
  def getSize(node:JsonNode, clazz:Class[_], genericType:Type,
              annotations:Array[Annotation], mediaType:MediaType) = 0l

  def isWriteable(clazz:Class[_], genericType:Type,
                  annotations:Array[Annotation], mediaType:MediaType) = {
    classOf[JsonNode].isAssignableFrom(clazz) && mediaType.equals(MediaType.APPLICATION_JSON_TYPE)
  }

  def writeTo(node:JsonNode, clazz:Class[_], genericType:Type,
              annotations:Array[Annotation], mediaType:MediaType,
              headers:MultivaluedMap[String,Object], out:OutputStream) = {
    out.write(om.writeValueAsBytes(node))
  }
}
