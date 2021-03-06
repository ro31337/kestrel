package net.lag.kestrel

import java.util.concurrent.{CountDownLatch, ScheduledThreadPoolExecutor}
import com.twitter.util.{Duration, Future, JavaTimer, Promise, Return, Throw}
import java.nio.ByteBuffer

class BlockingContainer(blockPeriod: Duration) extends LocalDirectory("", new ScheduledThreadPoolExecutor(1)) {
  override def getStream(streamName: String, syncPeriod: Duration): PersistentStream = { new BlockingStream(blockPeriod) }
  override def listStreams(): Array[String] = {
    new Array[String](0)
  }
}

class BlockingStream(blockPeriod: Duration) extends PersistentStream {
  def getWriter(): PersistentStreamWriter = { new BlockingStreamWriter(blockPeriod) }
  def getReader(): PersistentStreamReader = { new BlockingStreamReader() }
  def length(): Long = { 0 }
  def recover() {}
}

class BlockingStreamWriter(blockPeriod: Duration) extends PersistentStreamWriter {
  implicit val timer = new JavaTimer(true)
  def write(data: ByteBuffer): Future[Unit] = {
    if (blockPeriod > Duration.Bottom) {
      sleep(blockPeriod)
    } else {
      Future.Done
    }
  }
  def sleep(blockPeriod: Duration): Future[Unit] = {
    if (blockPeriod <= Duration.Zero)
      return Future.Done
    val p = new Promise[Unit]
    val task = timer.schedule(blockPeriod.fromNow) { p.setValue(Unit) }
    p.setInterruptHandler {
      case e =>
        if (p.updateIfEmpty(Throw(e)))
          task.cancel()
    }
    p
  }
  def force(metadata: Boolean) {}
  def truncate(position: Long) {}
  def position(): Long = { 0 }
  def close() {}
}

private class BlockingStreamReader() extends PersistentStreamReader {
  override def read(dataBuffer: ByteBuffer): Int = { -1 }
  override def positionAt(newPosition: Long) {}
  override def position(): Long = { 0 }
  override def close() {}
}
