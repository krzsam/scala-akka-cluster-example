package example

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.{Cluster, ClusterEvent}
import akka.pattern.PromiseRef
import example.SeedNodeActor.RemoteTerminateResponse
import example.ClusterNodeActor.RemoteTerminateRequest

object ClusterNodeActor {
  def props( clusterSeed: String, promise: PromiseRef[Any] ) : Props = Props( new ClusterNodeActor( clusterSeed, promise ) )

  abstract sealed class Messages
  case class RemoteTerminateRequest( msg: String = "" ) extends Messages
}

class ClusterNodeActor(clusterSeed:String, promise: PromiseRef[Any] ) extends Actor with ActorLogging with ActorBase {
  val cluster = Cluster( context.system )
  cluster.subscribe( self, classOf[ClusterEvent.MemberUp], classOf[ClusterEvent.MemberLeft], classOf[ClusterEvent.MemberRemoved] )
  val main = cluster.selfAddress
  val seed = main.copy( port = Some(SeedNodeActor.ClusterPort), host = Some(clusterSeed) )
  cluster.join(seed)

  override def postStop = {
    cluster.unsubscribe( self )
  }

  override def receive: Receive = super[ActorBase].receive orElse {
    case msg: String =>
      log.info( s"Received message: $msg from sender: $sender")
      sender() ! s"All right buddy, I got from you: '$msg'"

    case RemoteTerminateRequest( msg ) =>
      log.info( s"Received terminate request $msg from $sender")
      sender() ! RemoteTerminateResponse( "ok" )
      promise.ref ! "Done!"

    case other: Any => log.info( s"Received something else: $other of type ${other.getClass.getCanonicalName}")
  }
}
