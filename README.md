# Turmas

Distributed Systems Project 2021/2022

## Authors

**Group A03**

### Code Identification

In all source files (namely in the *groupId*s of the POMs), replace __GXX__ with your group identifier. The group
identifier consists of a G and the group number - always two digits. This change is important for code dependency
management, to ensure your code runs using the correct components and not someone else's.

### Team Members

| Number | Name             | User                                   | Email                                         |
|--------|------------------|----------------------------------------|-----------------------------------------------|
| 95560  | Diogo Dionísio   | <https://github.com/diogodionisio29>   | <mailto:diogodionisio29@tecnico.ulisboa.pt>   |
| 95617  | Juliana Yang     | <https://github.com/julianayang777>    | <mailto:juliana.yang@tecnico.ulisboa.pt>      |
| 95618  | Leonor Barreiros | <https://github.com/leonormmbarreiros> | <mailto:leonorbarreiros@tecnico.ulisboa.pt>   |

## Getting Started

The overall system is made up of several modules. The main server is the _ClassServer_. The clients are the _Student_,
the _Professor_ and the _Admin_. The definition of messages and services is in the _Contract_. The future naming server
is the _NamingServer_.

See the [Project Statement](https://github.com/tecnico-distsys/Turmas) or a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too, just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation

To compile and install all modules:

```s
mvn clean install
```

### Running the program

#### Naming Server
To compile and run the NamingServer:

[Note: the NamingServer should be running before running ClassServer ans client]

```s
mvn compile exec:java
```

To compile and run the NamingServer in the debug mode:

```s
mvn compile exec:java -Dexec.args="localhost 5000 -debug"
```

#### Class Server
To compile and run the ClassServer:

[Note: the ClassServer should be running before running each client]
[Note: To successfully run the server, you need to run both primary Server and secondary Server]

```s
mvn compile exec:java -Dexec.args="<host> <port> <P|S> [-debug]"
```

#### Admin

To compile and run the client Admin:
```s
mvn compile exec:java -Dexec.args="[-debug]"
```

#### Professor

To compile and run the client Professor:
```s
mvn compile exec:java -Dexec.args="[-debug]"
```

#### Student

To compile and run the client Student:
```s
mvn compile exec:java -Dexec.args="alunoXXXX name[ -debug]"
```



## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
