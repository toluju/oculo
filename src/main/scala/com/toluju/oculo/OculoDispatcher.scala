package com.toluju.oculo

import java.util.Date
import org.collectd.protocol.Dispatcher
import org.collectd.protocol.Notification
import org.collectd.protocol.ValueList
import com.google.inject.Singleton

@Singleton
class OculoDispatcher extends Dispatcher {
  private val _values = new CircularBuffer[ValueList](25)
  private val _notifications = new CircularBuffer[Notification](25)
  private var lastTime = 0l

  def dispatch(valueList:ValueList) = {
    if (valueList.getTime - lastTime > 1000) {
      println("Value received: " + new Date(valueList.getTime))
    }
    lastTime = valueList.getTime
    _values += valueList
  }

  def dispatch(notification:Notification) = {
    if (notification.getTime - lastTime > 1000) {
      println("Notification received: " + new Date(notification.getTime))
    }
    lastTime = notification.getTime
    _notifications += notification
  }

  def values = _values.view
  def notifications = _notifications.view
}

class CircularBuffer[T <: AnyRef : ClassManifest](val size:Int) {
  private val array = Array.ofDim[T](size)
  private var index = 0
  private val lock = new AnyRef

  def +=(item:T) = {
    lock.synchronized {
      index = (index + 1) % size
      array(index) = item
    }
  }

  def view = (array.view(index, size) ++ array.view(0, index)).filter(_ != null)
}
