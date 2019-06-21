package example

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, ActorSelection, Identify}
import akka.cluster.{ClusterEvent, Member}
import akka.cluster.ClusterEvent.CurrentClusterState

trait ActorBase extends Actor with ActorLogging {
  var members = Set.empty[Member]
  var actors = Set.empty[ActorRef]

  def receive: Receive = {
    case state: CurrentClusterState =>
      log.info( s"Current cluster state: leader: ${state.leader} , members: ${state.members}" )

    case ClusterEvent.MemberUp( member ) =>
      log.info( s"Cluster joined by member ${member}" )
      members += member

      val path = member.address + "/user/" + classOf[ClusterNodeActor].getName + "*"
      log.info( s"Sending Identify to all remote actors on path: ${path}" )
      val remoteActors: ActorSelection = context.actorSelection( path )
      remoteActors ! Identify( path )

    // in this example application removal of ActorRefs when a node member leaves the cluster is not implemented
    case ClusterEvent.MemberLeft( member ) =>
      log.info( s"Cluster left by member ${member}" )
      members -= member

    // in this example application removal of ActorRefs when a node member leaves the cluster is not implemented
    case ClusterEvent.MemberRemoved( member, status ) =>
      log.info( s"Member ${member} removed from cluster, previous status ${status}" )
      members -= member

    case ActorIdentity( id, result ) =>
      result match {
        case Some( ref ) =>
          log.info( s"Received identification from actor: ${ref} on ${id}" )
          actors += ref
        case None =>
          log.info( s"No remote actors identified on ${id}" )
        }
  }
}
