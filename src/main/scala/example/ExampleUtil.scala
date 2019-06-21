package example

import akka.actor.ActorSystem
import akka.pattern.PromiseRef
import org.slf4j.{Logger, LoggerFactory}

object ExampleUtil {
  private val LOG: Logger = LoggerFactory.getLogger( this.getClass )

  /*
  Utility function to wait for a completion of a promise.
  Promise is completed once any message is sent to the reference associated with it.
   */
  def shutDown(promise: PromiseRef[Any], system: ActorSystem ) : Unit = {
    while( !promise.promise.isCompleted ) {
      Thread.sleep( 5000 )
    }

    LOG.info("All messages processed")
    val term = system.terminate()
    while( !term.isCompleted ) {
      LOG.info(" Waiting for system to shut down")
      Thread.sleep( 1000 )
    }
    LOG.info( "System is down")
  }
}
