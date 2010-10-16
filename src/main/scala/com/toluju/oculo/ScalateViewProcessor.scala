package com.toluju.oculo

import com.sun.jersey.api.view.Viewable
import org.fusesource.scalate.DefaultRenderContext
import org.fusesource.scalate.Template
import org.fusesource.scalate.TemplateEngine
import scala.collection.JavaConversions._
import com.sun.jersey.spi.template.ViewProcessor
import java.io.File
import java.io.OutputStream
import java.io.PrintWriter
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.ext.Provider
import javax.ws.rs.core.Context

@Provider
class ScalateViewProcessor extends ViewProcessor[Template] {
  val engine = new TemplateEngine
  val baseDir = new File("src/main/webapp")

  @Context
  var request: HttpServletRequest = _

  def resolve(name:String):Template = {
    println("Resolving template: " + name)
    val file = new File(baseDir, name)
    println("Template file: " + file.getAbsolutePath)

    if (file.exists) {
      engine.load(file)
    }
    else {
      null
    }
  }

  def writeTo(template:Template, viewable:Viewable, out:OutputStream) = {
    val writer = new PrintWriter(out)
    val context = new DefaultRenderContext(request.getRequestURI, engine, writer)
    viewable.getModel match {
      case map:Map[_, _] => map.foreach(entry => {
        context.attributes(entry._1.toString) = entry._2
      })
      case obj:Any => context.attributes("it") = obj
    }
    template.render(context)
    writer.flush
  }
}
