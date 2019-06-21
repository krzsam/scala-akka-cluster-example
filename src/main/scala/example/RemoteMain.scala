package example

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.PromiseRef
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.FiniteDuration
import scala.io.Source

object RemoteMain {
  private val LOG: Logger = LoggerFactory.getLogger( this.getClass )

  def main(args: Array[String]): Unit = {
    val hostname = InetAddress.getLocalHost().getHostName().toUpperCase()
    LOG.info( s"Starting REMOTE actor system on $hostname" )

    val configFile = getClass.getClassLoader.getResourceAsStream("remote_system.conf")
    val configContent = Source.fromInputStream( configFile ).mkString.replaceAll( "%HOSTNAME%", hostname )
    LOG.info( s"REMOTE config: $configContent")

    val config = ConfigFactory.parseString( configContent )

    val system = ActorSystem( "ClusterSystem", config )

    // promise - to be notified with any message that the system can shut down
    val promise = PromiseRef( system, Timeout( FiniteDuration( 600, TimeUnit.SECONDS ) ))

    // this actor will reply to 'local' actor
    val remoteLocal = system.actorOf( RemoteActor.props( clusterSeed = args(0).toUpperCase, promise), name = s"RemoteActor$hostname" )
    LOG.info( s"Started actor: $remoteLocal on host: $hostname" )

    ExampleUtil.shutDown( promise, system )
  }
}
