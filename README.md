# Session Types Monitor
**Hybrid verification methodology for communication protocols in Scala built around the library [lchannels](https://github.com/alcestes/lchannels).** *(As seen in [FORTE 2020](https://link.springer.com/chapter/10.1007/978-3-030-50086-3_13) with [this](http://staff.um.edu.mt/afra1/papers/forte2020.pdf) paper.)*

A tool ([Synth](https://github.com/chrisbartoloburlo/stmonitor/blob/master/monitor/src/main/scala/monitor/Synth.scala)) that, given a session type _S_, can synthesise the Scala code of a type-checked monitor that verifies at runtime whether an interaction abides by _S_, and signatures usable to implement a process that interacts according to _S_. The generated monitors are embedded with runtime data checks as specified in the session types. A presentation outlining our approach can be found [here](https://youtu.be/FL_teSjllSE).

These instructions are for recreating and executing the example found in the paper, namely, the login example. We assume a Unix-like operating system with Java 8 as default JRE/JDK which can be downloaded from [here](https://www.oracle.com/java/technologies/javase-jdk8-downloads.html).

### Compile the sources
This project uses the **`sbt`** build tool which can be downloaded from [here](https://www.scala-sbt.org/0.13/docs/Setup.html) (sbt v. 0.13.x). Once installed, open a terminal in `stmonitor/`* and execute the command `sbt compile`.

\* _All commands from this point forward must be executed from this location._

### The login example

#### 1. Synthesising the monitor and CPSP classes.

It is recommended that the generation of `mon.scala` and `CPSPc.scala` is done first thing, i.e. before implementing anything else. However, in order to be able to proceed to the next step to start the server together with the monitor, the implementation of the other components is required. Therefore, for the sake of this example, follow the below steps in the [test](https://github.com/chrisbartoloburlo/stmonitor/tree/master/examples/src/main/scala/monitor/examples/test) directory, set up specifically for this demo containing only the files required to generate the monitor and the classes.

The server must follow the type found in `login.st`:
```
S_login = rec X.?Login(uname:Str, pwd:Str, token:Str)[validateAuth(uname, token)]. +{!Success(id:Str)[validateId(id,uname)].R , !Retry().X}
```

The functions `validateAuth()` and `validateId()` are present in the `util.scala` file. The package declaration (first line) must be temporarily removed from the file before proceeding.

To generate the monitor and the CPSP classes, run `Generate.scala` using the following command in a terminal inside the project root directory (replace `[root]` accordingly to represent the absolute path to the test directory):
```
sbt "project examples" "runMain monitor.examples.test.Generate [root]/stmonitor/examples/src/main/scala/monitor/examples/test"
```
Once completed, the files `mon.scala` and `CPSPc.scala` should be present in the test directory. These files are the same as those found in the [login](https://github.com/chrisbartoloburlo/stmonitor/tree/master/examples/src/main/scala/monitor/examples/login) directory, with the exception handling and logging added where violations are expected. Moreover, the package declarations are also added accordingly. These generated files will not compile due to the lack of other sources (such as the connection manager). For a demo proceed to the next step.

#### 2. Starting the setup.
**Before proceeding, remove the files generated from the previous step to ensure that the project compiles.**
1. Start the server together with the monitor using `Demo.scala` found in [`tcp/`](https://github.com/chrisbartoloburlo/stmonitor/tree/master/examples/src/main/scala/monitor/examples/login/tcp) using the following command:
    ```
    sbt "project examples" "runMain monitor.examples.login.tcp.Demo"
    ```
   _Note: The server listens on the TCP/IP socket 127.0.0.1:1330._
   
2. In a separate terminal navigate to the scripts folder: `stmonitor/scripts/` and execute the following command to start a client which connects to the server and sends multiple requests:
   ```
   python login-client.py 
   ```
