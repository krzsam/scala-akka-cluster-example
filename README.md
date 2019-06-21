# scala-akka-cluster-example

### Main classes
* **LocalMain** - starts up an actor system containing an instance of the local actor. The process should be started
  with hosts for remote actor systems provided as parameters. Only one jar, only one jar? yes only one jar! (see **Build** section below)
  
  `krzsam@pi1:~$ java -cp ./scala-akka-remote-example-assembly-0.1.jar example.LocalMain pi2 pi3`
  
* **RemoteMain** - starts up an actor system containing an instance of a remote actor. The process does not require any paremeters.
  There can be any number of remote actor systems started (on any number of hosts, one per a host)
  
  `krzsam@pi2:~ $ java -cp ./scala-akka-remote-example-assembly-0.1.jar example.RemoteMain`
  
  `krzsam@pi3:~ $ java -cp ./scala-akka-remote-example-assembly-0.1.jar example.RemoteMain`
  
### Actors
There are two classes of actors in the project:
* **LocalActor** - main actor which can be understood as a client actor to the remote actors.
* **RemoteActor** - actors which provide a echo-like service to the local actor. There may one or more of remote actors running on a number of hosts. 
  For the simplicity of example, there may be run only one remote actor per host (remote actors bind on the same fixed port
  which is hardcoded in the code - *5150*). An instance of the RemoteActor contains the name of the host in its name (which is probably not necessary
  but makes debugging and investigation easier).
  
From the perspective of the actual deployment as used during the tests, the actor instances were as follows:
* Host: **pi1** , **LocalActor** - *akka.tcp://LocalActorSystem@PI1:35713/user/LocalActor*   (the port will be random here and likely different each time)
* Host: **pi2** , **RemoteActor** - *akka.tcp://RemoteActorSystem@PI2:5150/user/RemoteActorPI2*
* Host: **pi3** , **RemoteActor** - *akka.tcp://RemoteActorSystem@PI3:5150/user/RemoteActorPI3*
  
### Messages
* **akka.actor.Identify** : sent from **LocalActor** to **RemoteActor**.
  *LocalActor* creates *ActorSelection* references for each of the remote actors on the hosts as provided for the LocalMain class
  For each of these references, a message of type *Identify* is sent so the *ActorRef* reference can be captured once *ActorIdentity* message
  is received back by LocalActor. This message is handled by Akka itself. *messageId* field is not used in the example.
* **akka.actor.ActorIdentity** : sent from **RemoteActor** to **LocalActor**. *LocalActor* upon reception of this message, stores the associated *ActorRef*
  in the map which is used to implement graceful shut down of the Local actor system. *correlationId* and *ref* fields are not used in the example.
* **example.LocalActor.SendTo** : sent to **LocalActor**
  This message instructs LocalActor where to send a given message. The message class has two fields:
   * **message** - any object of any serializable class - this will be passed onto *RemoteActor(s)*
   * **host** - indicates which of the known *RemoteActors* (which hosts) should the message be sent to
     * **all** - the message will be sent to all known *RemoteActors*
     * **random** - the message will be sent to one randomly picked *RemoteActor*
     * anything else is treated as specific host name and should be the name passed as one of the parameters the *Local* actor system was started with
* **example.LocalActor.RemoteTerminateRequest** : sent from **LocalActor** to **RemoteActor(s)**. The message is sent to support graceful shut down of *Local* and all *Remote* actor systems.
  Reception of *RemoteTerminateRequest* by a *Remote* system causes a message to be sent to a promise which is used as a semaphore
  to guarding whether the system can be shut down.
* **example.LocalActor.RemoteTerminateResponse** : sent from **RemoteActor** to **LocalActor**. The message is sent to support graceful shut down of *Local* and all *Remote* actor systems.
  Reception of *RemoteTerminateResponse* by the *Local* system indicated that the sender of this message (a *Remote* actor system) confirmed *RemoteTerminateRequest* and is being shut down. *LocalActor* keeps track
   of which *Remote* systems replied with *RemoteTerminateResponse* in a map. Once all known *Remote* systems confirmed their shutdown, the *LocalActor* sends a message to its own promise to trigger a semaphore 
   guarding whether the *Local* system can be shut down.
   
### Configuration files

* **resources/local_system.conf** - loaded when Local actor system is started to enables remoting for Akka. Parameters:
  * **%HOSTNAME%**- placeholder for actual hostname on which *Local* actor system is started
  * port is hardcoded to 0 so Akka will bind to a randomly selected port - in the example it is not necessary for Local actor system to be available on a well-known port
* **resources/remote_system.conf**
  * **%HOSTNAME%** - placeholder for actual hostname on which *Remote* actor system is started
  * **%PORT%** - placeholder for well-known port to bind *Remote* actor to

Both files are contained inside the *fat jar* file and are read from there.

### Build

As one of the main main reason to implement this example project was to try Akka in a multi-host requirement where Local and Remote actor systems
could be deployed flexibly without tying them up to particular hosts, the simplicity and ease of deployment was 
one of the major factors to consider, and the need to deploy main application accompanied with separate dependency libraries was ruled out at the very beginning.
Hence, the sbt-plugin was chosen as a way to produce a fat jar which would comprise of the examples classes pus all the classes from dependencies,
all bundled up together.

The deployment is done as below:
* Run *abt assembly* task, either via IntelliJ sbt panel, or sbt shell, or from the console directly
* The *fat jar* will be available in *./target/scala-2.12/scala-akka-remote-example-assembly-0.1.jar*
* Use a tool of your choice to copy over the jar from your dev machine to your target runtime host(s) in the desired location. 
  In my case I used *scp* command

### Running

#### Locally

You can run one *Local* actor system and one *Remote* actor system collocated on your dev machine (via simple configurations in your IDE) for quick testing and/or testing changes. 
It is possible as the *Remote* actor system will bind on well known port 5150, and the *Local* actor system will bind on a randomly assigned port.
Just make sure to start *Remote* actor system first. 

#### On multiple hosts

As mentioned in the **Build** section, you can copy the fat jar to a number of hosts - one of them will serve as your Local actor system, the others as your Remote actor systems
The way how to start them was provided in the **Main classes** section. For simplicity, there is no retry or resiliency in the example in the *LocalActor* implementation,
so just make sure all *Remote* actor systems are started before *Local* actor system.

### Development environment

* Java: 1.8
* Scala: 2.12.8
* Akka: 2.5.22
* Sbt: 1.2.7
* Sbt-assembly: 0.14.9 - [sbt-assembly](https://github.com/sbt/sbt-assembly)

### Runtime environment used for testing

* Java: 1.8
* pi1, pi2, pi3: *Pi 3 Model B* running Raspbian 8 Jessie

### TODO

* Tests, tests, more tests
* Enable more then one *Remote* actor system to be run on a given host (enable to pass port as process 
  parameter both in *LocalMain* and *RemoteMain* classes 
* Test if all works well on hosts where *InetAddress.getLocalHost().getHostName* returns FQDN, e.g. pi1.some-domain.org. 
  It may work already, I just did not have chance to test it.