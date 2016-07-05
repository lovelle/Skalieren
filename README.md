Skalieren
=========

A trip to conquer the big mountain of scala lang.

This is a collection of servers and examples written in scala that has been necessary 
or useful for some tasks.

### 1. Callbacks

Interface for communicate with external python scripts.
A tcp server, using [Akka](http://akka.io/) framework, listening for new connections,
and executing a python script as a callback module and returning the output to the client.

##### Build & Run

```sh
$ sbt compile
$ sbt run
```

### 2. Parking

A basic exercise.
