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
      LOG.info("Waiting for signal to shut down")
      Thread.sleep( 5000 )
    }

    LOG.info("Shut down starting ...")
    val term = system.terminate()
    while( !term.isCompleted ) {
      LOG.info(" Waiting for system to shut down")
      Thread.sleep( 1000 )
    }
    LOG.info( "System is down")
  }
}
