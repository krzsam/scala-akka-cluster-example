# scala-akka-cluster-example

### Architecture
This example application is designed to run and form a cluster of nodes consisting of one *Seed* node actor system, and any number of regular *Cluster*
node systems. All the actor systems can be started up in a ny order.

### Main classes
* **SeedNodeMain** - starts up an actor system containing the seed node of the cluster. The process does not require any runtime parameters. 
  Only one jar, only one jar? yes only one jar! (see **Build** section below)
  
  `krzsam@pi1:~$ java -cp ./scala-akka-cluster-example-assembly-0.1.jar example.SeedNodeMain`
  
* **ClusterNodeMain** - starts up an actor system containing a regular (non-seed) cluster node. The process requires parameter denoting host name of the seed node.
  There can be any number of cluster node actor systems started on any number of hosts.
  
  `krzsam@pi2:~ $ java -cp ./scala-akka-cluster-example-assembly-0.1.jar example.ClusterNodeMain pi1`
  
  `krzsam@pi3:~ $ java -cp ./scala-akka-cluster-example-assembly-0.1.jar example.ClusterNodeMain pi1`
  
  Seed-node and regular cluster node actor systems can be started in any order - if regular cluster node systems are started first, they will periodically try to reach the seed node system and 
  will connect to it and join the cluster once the seed node system is up and running. 
  
### Actors
There are two classes of actors in the project:
* **SeedNodeActor** - main actor which can be understood as a client actor to the cluster node actors running on regular cluster nodes. The seed node actor system binds on a well-known port number 2552.
* **ClusterNodeActor** - actors which provide an echo-like service to the actor running on the seed node. There may one or more of cluster node actor systems running on a number of hosts. 
  There may be any number (limited by memory size of course) of regular cluster node systems running on one physical host (cluster node systems bind on a random port). 
  Also, to ease debugging and make logs more clear, an instance of the ClusterNodeActor contains the name of the host in its name.
  
From the perspective of the actual deployment as used during the tests, the actor instances were as follows:
* Host: **pi1** , **SeedNodeActor** - *akka.tcp://ClusterSystem@PI1:2552/user/example.SeedNodeActor*
* Host: **pi2** , **ClusterNodeActor** - *akka.tcp://ClusterSystem@PI2:33971/user/example.ClusterNodeActorPI2*
* Host: **pi3** , **ClusterNodeActor** - *akka.tcp://ClusterSystem@PI3:34113/user/example.ClusterNodeActorPI3]*
  
### Messages
* **akka.actor.Identify** : sent from **SeedNodeActor** to **ClusterNodeActor**.
  *SeedNodeActor* creates *ActorSelection* reference when it is notified of a new member node joining the cluster. For simplicity, it uses wild card selection to query actor identity.
  For such selection, a message of type *Identify* is sent so *ActorRef* references can be captured once *ActorIdentity* message
  is received back by SeedNodeActor. This message Identify is handled by Akka itself. *correlationId* field is used to log *ActorSelection* path queried.
* **akka.actor.ActorIdentity** : sent from **ClusterNodeActor** to **SeedNodeActor**. *SeedNodeActor* upon reception of this message, stores the associated *ActorRef*
  references in the set which is later used to send messages to all cluster node actors in the cluster and to implement graceful shut down of the actor systems. 
  *correlationId* is used to log *ActorSelection* path queried.
* **example.SeedNodeActor.SendTo** : sent to **SeedNodeActor**
  This message instructs SeedNodeActor where to send a given message. The message class has two fields:
   * **message** - any object of any serializable class - this will be passed onto *ClusterNodeActor(s)*
   * **host** - indicates which of the known *ClusterNodeActor* (which hosts) should the message be sent to
     * **all** - in the example application, only sending a message to all known *ClusterNodeActors* is implemented
* **example.ClusterNodeActor.RemoteTerminateRequest** : sent from **SeedNodeActor** to **ClusterNodeActor(s)**. The message is sent to support graceful shut down of *Seed* and all *Cluster* node actor systems.
  Reception of *RemoteTerminateRequest* by a *Cluster* node system causes a message to be sent to a promise which is used as a semaphore
  to guarding whether the system can be shut down.
* **example.SeedNodeActor.RemoteTerminateResponse** : sent from **ClusterNodeActor** to **SeedNodeActor**. The message is sent to support graceful shut down of *Seed* and all *Cluster* node actor systems.
  Reception of *RemoteTerminateResponse* by the *Seed* system indicated that the sender of this message (a *Cluster* node actor system) confirmed *RemoteTerminateRequest* and is being shut down. *ClusterNodeActor* keeps track
   of which *Cluster* node systems replied with *RemoteTerminateResponse* in a map. Once all known *Cluster* node systems confirmed their shutdown, the *SeedNodeActor* actor sends a message to its own promise to trigger a semaphore 
   guarding whether the *Seed* node system can be shut down.
   
### Configuration files

* **resources/seed_node.conf** - loaded when the *Seed* node actor system is started to enable cluster functionality for Akka. Parameters:
  * **%HOSTNAME%**- placeholder for actual hostname on which *Seed* node actor system is started.
  * Port is hardcoded to a well known number 2552 to enable cluster node systems to connect to the seed node.
* **resources/cluster_node.conf** - loaded when any of *Cluster* node actor system is started to enable cluster functionality for Akka. Parameters:
  * **%HOSTNAME%** - placeholder for actual hostname on which *Cluster* node actor system is started.
  * Port is set to *0* to make Akka bind on a random port number.

Both files are contained inside the *fat jar* file and are read from there.

### Build

As one of the main main reason to implement this example project was to try Akka in a multi-host environment when nodes are organized into a cluster.
The requirement was to enable *Seed* and *Cluster* node actor systems
to be deployed flexibly across network without tying them up to particular hosts - the simplicity and ease of deployment was 
one of the major factors to consider, and the need to deploy main application accompanied with separate dependency libraries was ruled out at the very beginning.
Hence, the sbt-plugin was chosen as a way to produce a fat jar which would comprise of the examples classes pus all the classes from dependencies,
all bundled up together.

The deployment is done as below:
* Run *abt assembly* task, either via IntelliJ sbt panel, or sbt shell, or from the console directly
* The *fat jar* will be available in *./target/scala-2.12/scala-akka-cluster-example-assembly-0.1.jar*
* Use a tool of your choice to copy over the jar from your dev machine to your target runtime host(s) in the desired location. 
  In my case I used *scp* command

### Running

#### Locally

You can run one *Seed* node actor system and any number of *Cluster* node actor system collocated on your dev machine (via simple configurations in your IDE) for quick testing and/or testing changes. 
It is possible as the *Seed* node actor system will bind on well known port 2552, and all *Cluster* node actor system will bind on a randomly assigned port.

#### On multiple hosts

As mentioned in the **Build** section, you can copy the fat jar to a number of hosts - one of them will serve as your *Seed* node actor system, the others can host any number
of *Cluster* node actor systems.
The way how to start them was provided in the **Main classes** section. The nodes should join up and form a cluster automatically as supported by Akka cluster functionality.

#### Example message passing scenario
In this example application, a message is scheduled in the *Seed* node actor system to be sent after **20 seconds** to **SeedNodeActor** which will then 
send a message to every **ClusterNodeActor** actor in the *Cluster* nodes. After further **10 seconds** another message is scheduled to be sent to **SeedNodeActor**
which in turn will request all **ClusterNodeActor** to shut down their respective actor systems, which once complete via reply message, will cause shut down
of **SeedNodeActor** as well.

### Development environment

* Java: 1.8
* Scala: 2.12.8
* Akka: 2.5.23
* Sbt: 1.2.7
* Sbt-assembly: 0.14.9 - [sbt-assembly](https://github.com/sbt/sbt-assembly)

### Runtime environment used for testing

* Java: 1.8
* pi1, pi2, pi3: *Pi 3 Model B* running Raspbian 8 Jessie

### TODO

* Tests, tests, more tests
* Test if all works well on hosts where *InetAddress.getLocalHost().getHostName* returns FQDN, e.g. pi1.some-domain.org. 
  It may work already, I just did not have chance to test it.