# CS 441 Project : 

### Description: Design and implement an Actor-based computational model for the Chord and CAN overlay network algorithms

## Team Members (Group 11)

Garima Gupta (ggupta22)

Avani Mande (amande6)

Samujjwaal Dey (sdey9)

## Overview
As part of this project a cloud simulation is implemented based on the Chord and CAN overlay network algorithms using the Akka cluster sharding actor model. 

The simulation has two main components. CAN and Chord algorithms take data read and data write requests and service them appropriately. Akka HTTP server is used to send read/write requests.

##Cluster Sharding

The project uses the cluster sharding model provided by Akka. In this, actors are distributed across shards. A group of shards forms a shard region where one shard can multiple entity actors. A shard region constitutes of a group of shards.
The entities have some state, i.e. data that each entity owns. In our project for the Chord algorithm, the Chord nodes act as entities which store the data as key value pairs. 
And for the CAN algorithm each entity represents a node which is in charge of a zone in the coordinate space.
When a read or a write request is received, the Chord/ CAN algorithm is used to determine which node is responsible for the data and then the request is routed using entity ID and shard ID to the correct entity on the specific shard hosting the entity.
This means that we do not have to worry about the physical location of the nodes and only need the logical identifier. Messages are sent to the entities using their identifiers. The shard region actor reference is used to route the message to the the final destination entity using the entity ID. 


##CAN Algorithm
The Content Addressable Network Algorithm makes use of a coordinate space. Nodes are in charge of zones in the space. Data is mapped to a coordinate point in the space and then routed through the network to reach the
node in charge of the zone in which the point lies in. Each node keeps a track of its neighbors which are used while routing request to the correct node. When a node joins the network, it does so by choosing a point in 
the coordinate space. The node which is in charge of the region in which the point lies is in then splits its zones and neighbors of both the nodes are updated.
When a node leaves the system zones are merged with a suitable neighbor. 


## Chord Algorithm

The Chord algorithm uses convergent hashing which when given a key returns a value. It is a peer to peer distributed hash table. 
Solves problem of locating a data item in a collection of distributed nodes, considering frequent node arrivals and departures.
It consists of a node ring where data lies on a node's successor. A successor is defined as the next node which is present in the system.
Each node keeps a finger table which is used for request routing.

## Akka HTTP

We use the [Akka HTTP](https://doc.akka.io/docs/akka-http/current/introduction.html) functionality which builds up on Akka actors and Akka streams to provide a complete client and server side functionality. We use DSL routes to define two requests that the server services. GET and POST.
The GET request is used to read a value given that the user provides a key. The GET request then sends a ask to the chord system to retrieve the given value. The POST request takes two arguments, key and value which are to be written to a node and sends a tell message to the chord system. The server responds to the user with the corresponding value for the read request key. For a write request, the server responds with a simple message saying that the operation is done.



## Application Design

### Some Important Files In CAN
- `CanMessageTypes` lists all the types types of messages that can be processed by the CAN node actor. 
- `CanNode` is the class for modelling a single node in the CAN system. Each node is an akka actor and can process messages.
- `Coordinate` class defines the coordinate point and functions associated for performing operations on the coordinate space.
- `Neighbor` defines an entry in the neighbor table kept by every node. 
- `CANmain` is the driver class for the CAN algorithm simulation. 

### Some Important Files In Chord

- The file `ClassicMessageTypes` lists all the types of messages that can be processed by the chord node actor.
- `ChordClassicNode` is the class for modelling a single node in the Chord ring. Each node is an akka actor and can process protocol messages of the type `NodeCommand` to alter it’s state or behavior.
  - The state of the `ChordNode` actor stores node IDs and node hash values of its successor & predecessor nodes, the current node’s fingertable and the data being stored on the node.
  - The messages are sent to the actor using the tell/ask actor interaction patterns to perform operation on the chord node like joining the chord network, updating node finger table and display finger table status. 
- `Finger` case class defines each finger entry in a node’s finger table. A case class is used as the compiler provides in-built getter, setter and other boilerplate code. Each finger table entry stores the hash value of the start index and node hash value of the successor node w.r.t the start index.
- `HTTPServer` defines an actor that will operate as the Akka HTTP server and the Chord node server. It can process protocol messages of type `ChordSystemCommand`. The server spawns actors for each chord node and adds it to a list storing node IDs. A node is selected from this list in random and added to the chord system using the ask actor interaction pattern.
- `ChordUtils` defines the `md5()` hashing function used to generate the hash values for chord node IDs and key positions in the chord ring.
- `Utils` defines the methods for generating random requests to the overlay network. The function `randomlySelectRequestType()` determines whether the next request generated by the user will be a data read or data write request. While the function `randomlySelectDataIndex()` selects random index of a data from the file to perform the read/write request.
- `ChordMain` is the driver class for the Chord simulation. It is responsible for instantiation of the `ChordHttpServer` and `UserSystem` actor systems.


##Analysis

As a part of our analysis we ran both the algorithms for a few predefined configurations. Their performance in terms of average request serviced per node and average hops per node. 
Hops here denote the number of times the request is passed from one node to the next when the current node is not responsible for the data key.


| Configuration | Chord         | CAN  |
| ------------- |:-------------:| -----:|
| Nodes= 10, <br/> Requests=40    | (4, 5.6) <br/> (4, 5.6) <br/> (4, 5.8) <br/> (4, 4.9) <br/> (4, 4.9) | (4, 2) <br/> (4, 1.5) <br/> (4, 1.8) <br/> (4, 1.857) <br/> (4, 1.628)|
| Nodes= 15, <br/> Requests=50      | (2.9, 9.2) <br/> (2.86, 7.62) <br/> (3.2, 7.35) <br/> (2.1, 6.46) <br/> (3.33, 6.67)     |  (2.8, 2.62) <br/> (3.33, 2.2) <br/> (3.33, 2.5) <br/> (3.3, 2.2) <br/> (3,2.27) |

We ran the simulation multiple times for each configuration to get an average approximation of network performance.
From the above observations, we can see that the average hops per request is higher for Chord than CAN. 
For Chord, the number of hops per request is approximately 5 for when there are 10 nodes in the network. 
Whereas in CAN, the number of hops per request is approximately 1.75 i.e. it varies between 1 and 2 for 10 nodes.
CAN consistently has lesser number of hops for same number of nodes and requests. This is due to the dimensionality of the key space. 
In Chord, the direction in which a request can be passed around is 1 dimensional, but in CAN a request is mapped to a multidimensional space due to which it has a higher probability to find the relevant node in lesser hops.
Latency increases as the number of hops increase. 
Still, both the algorithms reduce the average hops from a brute force linear search approach where a request goes to every node one by one to find the key.



## Instructions to Execute

1. Clone this repository from Bitcucket
2. Run `sbt clean compile test` to compile the project and run test cases
3. Import as sbt project in IntelliJ IDE
4. Run the `Main` scala class file to execute the simulation



### References

GitHub repositories: 

- [Link 1](https://github.com/pramo31/ChordSimulator)
- [Link 2](https://github.com/Swati32/Chord-Implementation-in-scala)
- [Link 3](https://github.com/softinio/pat)

Documentation:

[Akka Typed Actor Model](https://doc.akka.io/docs/akka/current/typed/index.html)

[Akka HTTP](https://doc.akka.io/docs/akka-http/current/introduction.html)

[Chord Algorithm](https://pdos.csail.mit.edu/papers/chord:sigcomm01/chord_sigcomm.pdf)



