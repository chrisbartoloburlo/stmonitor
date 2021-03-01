# Session Types Monitor
**Hybrid verification methodology for communication protocols in Scala built around the library [lchannels](https://github.com/alcestes/lchannels).** *(As seen in [FORTE 2020](https://link.springer.com/chapter/10.1007/978-3-030-50086-3_13) with [this](http://staff.um.edu.mt/afra1/papers/forte2020.pdf) paper.)*

A tool that, given a session type _S_, can synthesise the Scala code of a type-checked monitor that verifies at runtime whether an interaction abides by _S_, and signatures usable to implement a process that interacts according to _S_. The generated monitors are embedded with runtime data checks as specified in the session types. A presentation outlining our approach can be found [here](https://youtu.be/FL_teSjllSE).

These instructions are for recreating and executing the example found in the paper, namely, the login example. We assume a Unix-like operating system with Java 8 as default JRE/JDK which can be downloaded from [here](https://www.oracle.com/java/technologies/javase-jdk8-downloads.html).

### Compile the sources
This project uses the **`sbt`** build tool which can be downloaded from [here](https://www.scala-sbt.org/0.13/docs/Setup.html) (sbt v. 0.13.x). Once installed, open a terminal in `stmonitor/`* and execute the command `sbt compile`.

\* _All commands from this point forward must be executed from this location._

### The login example

#### 1. Synthesising the monitor and CPSP classes.

Consider the Login example, in which the server must follow the type found in `auth.st`:
```
S_auth=rec Y.( ?Auth(uname: String, pwd: String)[util.validateUname(uname)].+{!Succ(tok: String)[util.validateTok(tok, uname)], !Fail(Code: Int).Y} )
```

The functions `validateUname()` and `validateTok()` are present in the `util.scala` file. 

To generate the monitor and the CPSP classes, run `Generate.scala` using the following command in a terminal inside the project root directory:
```shell
sbt "project monitor" "runMain monitor.Generate $DIR $ST"
```
Replace `$DIR` with the directory in which the monitor and classes will be generated in: `[root]/stmonitor/examples/src/main/scala/examples/auth`

Replace `$ST` with the absolute path to the file containing the session type: `[root]/stmonitor/examples/src/main/scala/examples/auth/auth.st`

_(replace `[root]` to represent the absolute path to the directory containing the project)_

Once completed, the files `Mon.scala` and `CPSPc.scala` should be present in the provided directory. 

For a demo proceed to the next step.

#### 2. Setting up.
1. Add package declarations in the generated files `Mon.scala` and `CPSPc.scala`: `package examples.auth`

2. Uncomment all lines within the files [ConnectionManager.Scala](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/auth/ConnectionManager.scala), [MonitoredServer.scala](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/auth/MonitoredServer.scala) and [MonWrapper.scala](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/auth/MonWrapper.scala).

#### 3. Initialising a monitored setup.

For the sake of this example, we consider two different setups:

**SETUP 1** _Monitor and Scala server on the **same** JVM as **separate** threads._
1. Start the server together with the monitor using the following command:
    ```shell
    sbt "project examples" "runMain examples.auth.MonitoredServer"
    ```
   _The server listens on the TCP/IP socket 127.0.0.1:1330._

2. In a separate terminal navigate to the scripts directory `stmonitor/scripts/` and execute the following command to start a client which connects to the server and sends multiple requests:
   ```
   python3 auth-client.py 
   ```

**SETUP 2** _Monitor and Python server separately._

1. Navigate to the scripts directory `stmonitor/scripts/` and execute the following command to start a Python server using the following command:
    ```shell
    python3 auth-server.py
    ```
   The server listens on the TCP/IP socket 127.0.0.1:1335.

2. In a separate terminal, execute the following command to start a monitor:
   ```shell
   sbt "project examples" "runMain examples.demo.MonWrapper $LISTEN_PORT $FORWARDING_PORT 
   ```
   Replace `$LISTEN_PORT` with the port to expose for a client: _1330_, and `$FORWARDING_PORT` with the port for the monitor to connect to: _1335_. The monitor should connect to the Python server via the port _1335_ and wait for a connection from a client.

3. In a separate terminal, navigate to the scripts directory `stmonitor/scripts/` and execute the following command to start a Python client:
   ```shell
   python3 auth-client.py
   ```
   The client should send and receive messages via the port _1330_ which is handled by the monitor. In turn, the monitor analyses and forwards the messages to the server and client. 
