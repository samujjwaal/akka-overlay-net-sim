# Homework 3 


## Chord
The Chord algorithm uses convergent hashing which when given a key returns a value. It is a peer to peer distributed hash table. 
Solves problem of locating a data item in a collection of distributed nodes, considering frequent node arrivals and departures
We implement a simpler version where fault tolerance is not taken into consideration.
## Akka HTTP
We use the Akka HTTP functionality which builds up on Akka actors and Akka streams to provide a complete client and server side functionality. We use DSL routes to define two requests that the server services. GET and POST.
The GET request is used to read a value given that the user provides a key. The GET request then sends a ask to the chord system to retrieve the given value. The POST request takes two arguments, key and value which are to be written to a node and sends a tell message to the chord system. The server responds to the user with the corresponding value for the read request key. For a write request, the server responds with a simple message saying that the operation is done.

##Data Used 
The data that we use here for this simulation is available in resourses/listfile.txt and contains comma-separated-values of a title song and its release year.
The user uses this data to query for a song year given a song title. The link from which the dataset is obtained is given here. http://millionsongdataset.com/pages/getting-dataset/


A small subset of the dataset is chosen. The data is preprocessed in python to retrieve just two characteristics namely the song name and song year. 

##Overall functioning 
The simulation has three main components. The user system which generates user actors and sends read or write requests to the http server. The http server is another component which forwards these requests to the chord system. 

