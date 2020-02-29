# Session Types Monitor
**Hybrid verification methodology for communication protocols in Scala built around the library [lchannels](https://github.com/alcestes/lchannels).**

A tool ([Synth](https://github.com/chrisbartoloburlo/stmonitor/blob/master/monitor/src/main/scala/monitor/Synth.scala)) that, given a session type _S_, can synthesise the Scala code of a type-checked monitor that verifies at runtime whether an interaction abides by _S_, and signatures usable to implement a process that interacts according to _S_. The generated monitors are embedded with runtime data checks as specified in the session types. More information about this approach can be found [here]().

### The login example

#### 1. Synthesising the monitor and CPSP classes.

It is recommended that the generation of `mon.scala` and `CPSPc.scala` is done first thing, i.e. before implementing anything else. However, in order to be able to proceed to the next step to start the server together with the monitor, the implementation of the other components must be present. Therefore, for the sake of this example follow the below steps in the [test](https://github.com/chrisbartoloburlo/stmonitor/tree/master/examples/src/main/scala/monitor/examples/test) directory, set up specifically for this demo containing only the files required to generate the monitor and the classes.

A server must follow the type found in `login.st`:
```
S_login = rec X.?Login(uname:Str, pwd:Str, token:Str)[validateAuth(uname, token)]. +{!Success(id:Str)[validateId(id,uname)].R , !Retry().X}
```

The functions `validateAuth()` and `validateId()` are present in the `util.scala` file. The package declaration (first line) must be temporarily removed from the file before proceeding.

To generate the monitor and the CPSP classes, run `Generate.scala` using the following command in a terminal inside the project root directory:
```
sbt "project examples" "runMain monitor.examples.test.Generate"
```
Once completed, the files `mon.scala` and `CPSPc.scala` should be present in the test directory.

#### 2. Starting the setup.
