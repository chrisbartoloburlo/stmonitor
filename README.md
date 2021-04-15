# Session Types Monitor

**A runtime verification tool for communication protocols.** 

Given a session type _S_, the tool synthesises the Scala code of a type-checked monitor using the library [lchannels](https://github.com/alcestes/lchannels) that verifies at runtime whether the interaction abides by _S_. 


## Documentation and publications

* Christian Bartolo Burlò, Adrian Francalanza and Alcese Scalas. *[Towards a Hybrid Verification Methodology for Communication Protocols (Short Paper)](https://link.springer.com/chapter/10.1007/978-3-030-50086-3_13) at [FORTE 2020](https://link.springer.com/book/10.1007/978-3-030-50086-3).* A presentation outlining our approach can be found [here](https://youtu.be/FL_teSjllSE).

## Compiling the sources

1. You will need:
   * Java 11, available [here](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html), and
   * **`sbt` 1.5.0** build tool available [here](https://www.scala-sbt.org/download.html).
2. Open a terminal in the `stmonitor/` directory. 
3. Execute the command 
   ```
   sbt compile
   ```

The compilation will _automatically_ generate the files required for all examples. 

>The remainder of this readme consists of instructions on how to:
>* [_manually generate a monitor from a session type_](https://github.com/chrisbartoloburlo/stmonitor#synthesising-a-monitor-and-cpsp-classes)
>* [_launch existing examples_](https://github.com/chrisbartoloburlo/stmonitor#examples)
>* [_run the benchmarks_](https://github.com/chrisbartoloburlo/stmonitor#benchmarks)

## Synthesising a Monitor (and CPSP classes)

## Examples

### The auth example

These instructions are for recreating and executing the running example of the accompanying paper: the authentication protocol. We assume a Unix-like operating system, and all commands must be executed from the `stmonitor/` directory. 

The session type `S_auth` below found in [`auth.st`]() formalises an extended version of the authentication protocol from the client side:
```
S_auth=rec Y.(!Auth(uname: String, pwd: String).&{
	?Succ(origTok: String).rec X.(+{
		!Get(resource: String, reqTok: String).&{
			?Res(content: String).X,
			?Timeout().Y
		},
		!Rvk(rvkTok: String).end}),
	?Fail(code: Int).end
})
```
After authenticating with the server, the client is granted exclusive access to a resource via a token `origTok`. The token might expire after a while and the server would send a `Timeout` message, allowing the client to request another token. Otherwise, the client can revoke the token prematurely by sending `Rvk` and the session terminates. 

The monitor [Monitor.scala]() was generated from this type upon compilation. To launch a client-server setup with this monitor, skip to [these]() instructions; here we explain how to manually generate a monitor from a given session type.   

As we discuss in Section 5.1, the tool also offers the ability to enrich the session types with assertions that are predicates over the named payload variables. The type `SA_auth` extends `S_auth` with assertions to check the validity of the data being transmitted. 
```
SA_auth=rec Y.(!Auth(uname: String, pwd: String)[util.validateUname(uname)].&{
	?Succ(origTok: String)[util.validateTok(origTok, uname)].rec X.(+{
		!Get(resource: String, reqTok: String).&{
			?Res(content: String)[origTok==reqTok].X,
			?Timeout().Y
		},
		!Rvk(rvkTok: String)[origTok==rvkTok].end}),
	?Fail(code: Int).end
})
```
In this type, the username communicated by the client that becomes bound to `uname` would be validated using the function `validateUname()`. Similarly, the monitor will validate the token `origTok` issued upon successful authentication using `validateTok()`. The monitor would also check that when the server sends a resource in `Res`, the token included in the `Get` message (`reqTok`) was equivalent to the one issued earlier in the `Succ` message (`origTok`). 

The synthesis automatically type-checks the assertions using the Scala compiler, and ensures that they have a `Boolean` type. In any of these cases, the monitor would flag a violation if the expression evaluates to `false` at runtime. 

We encourage the reader to copy `SA_auth` and paste it in [`auth.st`](), and proceed with the following instructions to generate a monitor performing these checks. 

For your convenience, the functions `validateUname()` and `validateTok()` can be found in [`util.scala`](). For the sake of the example, they will always return `true`; one can change their return value to `false` and see that the monitor indeed flags a violation. 

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
   sbt "project examples" "runMain examples.auth.MonWrapper $LISTEN_PORT $FORWARDING_PORT"
   ```
   Replace `$LISTEN_PORT` with the port to expose for a client: _1330_, and `$FORWARDING_PORT` with the port for the monitor to connect to: _1335_. The monitor should connect to the Python server via the port _1335_ and wait for a connection from a client.

3. In a separate terminal, navigate to the scripts directory `stmonitor/scripts/` and execute the following command to start a Python client:
   ```shell
   python3 auth-client.py
   ```
   The client should send and receive messages via the port _1330_ which is handled by the monitor. In turn, the monitor analyses and forwards the messages to the server and client. Alternatively, one can also interact with the monitored server using `telnet 127.0.0.1 1335` and follow a text based protocol. 


## Benchmarks
