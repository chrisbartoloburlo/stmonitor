# Probabilistic Session Types Monitor

A tool ([Synth](https://github.com/chrisbartoloburlo/stmonitor/blob/master/monitor/src/main/scala/monitor/Synth.scala)) that, given a probabilistic session type _S_, can synthesise the Scala code of a type-checked monitor that verifies at runtime whether an interaction abides by _S_ according to the specified probabilistic behaviour, and signatures usable to implement a process that interacts according to _S_. 

These instructions are for recreating and executing the lottery game example. We assume a Unix-like operating system with Java 8 as default JRE/JDK which can be downloaded from [here](https://www.oracle.com/java/technologies/javase-jdk8-downloads.html).

### Compile the sources
This project uses the **`sbt`** build tool which can be downloaded from [here](https://www.scala-sbt.org/0.13/docs/Setup.html) (sbt v. 0.13.x). Once installed, open a terminal in `stmonitor/`* and execute the command `sbt compile`.

\* _All commands from this point forward must be executed from this location._

### The login example

#### 1. Synthesising the monitor and CPSP classes.

Consider the lottery game example, in which the server generates a number between 1 and 100 and should follow the type found in `S_game.st` in [`examples/demo/`](https://github.com/chrisbartoloburlo/stmonitor/tree/pstmonitor/examples/src/main/scala/examples/demo) :
```
S_login = rec X.&{?Guess(num: Int)[0.75].+{!Correct()[0.01].X, !Incorrect()[0.99].X}, ?Help()[0.2].!Hint()[1], ?Quit()[0.05].end}
```

To generate the monitor and the CPSP classes, run `Generate.scala` using the following command in a terminal inside the project root directory:
```
sbt "project monitor" "runMain monitor.Generate $DIRECTORY $SESSION_TYPE"
```
Replace `$DIRECTORY` with the absolute path to the directory in which the files should be generated. In this case, one can use the following directory (by replacing `[root]` accordingly):
```
[root]/stmonitor/examples/src/main/scala/examples/demo/
```
Similarly, replace `$SESSION_TYPE` with the absolute path to the file containing the session type:
```
[root]/stmonitor/examples/src/main/scala/examples/demo/S_game.st
```

Once completed, the files `mon.scala` and `CPSPc.scala` should be present in the demo directory. 


#### 2. Starting the setup.
**Before proceeding, uncomment all the lines within the `MonWrapper.scala` and `GameConnectionManager.scala` found in the `demo` directory.**

1. Start the provided python server `game_server.py` found in [`scripts/`](https://github.com/chrisbartoloburlo/stmonitor/tree/pstmonitor/scripts):
    ```
    python3 game-server.py $PORT $p_correct $p_incorrect
    ```
   Replace `$PORT` with the port for the server to listen on (_e.g.,_ 1335) and `p_correct` and `p_incorrect` with the desired probabilities for the server to send correct or incorrect. Respecting the type specified for the monitor, the probabilities should be: 0.01 and 0.99. 
   _Note: The server listens on 127.0.0.1_

2. In a separate terminal, start the generated monitor `Mon.scala` with the provided `MonWrapper` via the following command:
    ```
    sbt "project examples" "runMain examples.demo.MonWrapper $LISTEN_PORT $FORWARDING_PORT 1.9599"
    ```
    Replace `$LISTEN_PORT` with the port to expose for a client (_e.g.,_ 1330), and `$FORWARDING_PORT` with the port for the monitor to connect connect to (_e.g.,_ 1335). Replace `$z-value` with the _Z_ value for the desired confidence level (_e.g.,_ 1.9599 for 95%). More information about the _Z_ value and the confidence level can be found [here](https://en.wikipedia.org/wiki/Checking_whether_a_coin_is_fair).

3. In a separate terminal, start the provided python client `game_client.py` also found in [`scripts/`](https://github.com/chrisbartoloburlo/stmonitor/tree/pstmonitor/scripts): 
   ```
   python3 game-client.py $PORT $p_guess $p_help $p_quit 
   ```
   Replace `$PORT` with the port for the client to connect to (_e.g.,_ 1335). Similarly to the server, the parameters `$p_guess` `$p_help` `$p_quit` specify the probability for the client to send guess, help, or quit respectively. Respecting the type specified for the monitor, the probabilities should be: 0.75, 0.5 and 0.05. 
   
   
The setup should now be running. If both the client and the server were started with the probabilities as specified within the type, the monitor should not issue any warnings. The server keeps executing until it is explicitly terminated. Therefore, one can try executing a different experiment by repeating steps 2 and 3. Apart from changing the specified `z-value` for the monitor, one can also try changing the probabilities of the client such that they violate those specified within the type, which would trigger the monitor to issue warnings. In a similar manner, the server can also be initialised in such a way the monitor issues warnings on its behaviour.
