package example

import akka.actor.{Actor, ActorLogging, Address, Props}
import akka.cluster.{Cluster, ClusterEvent}
import akka.pattern.PromiseRef
import example.LocalActor.{RemoteTerminateResponse, SendTo}

import scala.language.postfixOps

object LocalActor {
  def props( promise: PromiseRef[Any] ) : Props = Props( new LocalActor( promise ) )

  abstract sealed class Messages
  case class RemoteTerminateResponse(msg: String = "" ) extends Messages
  case class SendTo( message: Any, host: String ) extends Messages

  val ClusterPort = 2552
}

class LocalActor( promise: PromiseRef[Any] ) extends Actor with ActorLogging with ActorBase {
  val cluster = Cluster( context.system )
  cluster.subscribe( self, classOf[ClusterEvent.MemberUp], classOf[ClusterEvent.MemberLeft], classOf[ClusterEvent.MemberRemoved] )
  val main: Address = cluster.selfAddress.copy( port = Some(LocalActor.ClusterPort) )
  cluster.joinSeedNodes( main :: Nil )

  override def postStop = {
    cluster.unsubscribe( self )
  }

  override def receive: Receive = super[ActorBase].receive orElse {
    case SendTo( message, "all" ) =>
      log.info( s"Sending message to: ${actors}" )
      actors foreach ( _ ! message )

    case s: String => log.info( s"Remote actor $sender replied with $s" )

    case RemoteTerminateResponse( msg ) =>
      log.info( s"Received terminate reply $msg from $sender" )
      actors -= sender
      if( actors.size == 0 )
        promise.ref ! "Done!"

    case other: Any => log.info( s"Received something else: $other of type ${other.getClass.getCanonicalName}")
  }
}

