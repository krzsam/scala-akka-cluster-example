package example

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.PromiseRef
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import example.LocalActor.SendTo
import example.RemoteActor.RemoteTerminateRequest
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.io.Source
import scala.concurrent.duration._

object LocalMain {
  private val LOG: Logger = LoggerFactory.getLogger( this.getClass )

  def main(args: Array[String]): Unit = {
    val hostname = InetAddress.getLocalHost().getHostName().toUpperCase()
    val parameters = args.mkString( "," )

    LOG.info( s"Starting LOCAL actor system on: $hostname parameters: $parameters" )

    val configFile = getClass.getClassLoader.getResourceAsStream("local_system.conf")
    val configContent = Source.fromInputStream( configFile ).mkString.replaceAll( "%HOSTNAME%", hostname )
    LOG.info( s"LOCAL Config: $configContent")

    val config = ConfigFactory.parseString( configContent )

    val system = ActorSystem( "ClusterSystem", config )

    // promise - to be notified with any message that the system can shut down
    val promise = PromiseRef( system, Timeout( FiniteDuration( 600, TimeUnit.SECONDS ) ))

    // this actor will send messages to remote actors
    if( args.length > 0 ) {
      val localActor = system.actorOf( LocalActor.props( promise ), name = "LocalActor" )

      implicit val executor = ExecutionContext.global
      system.scheduler.scheduleOnce( 10 seconds, localActor, SendTo( "Hello everyone", "all" ) )
      system.scheduler.scheduleOnce( 20 seconds, localActor, SendTo( RemoteTerminateRequest( "We are done" ), "all" ) )
    }
    else
      promise.ref ! "Done!"

    ExampleUtil.shutDown( promise, system )
  }
}
