# PSTMonitor: Monitor Synthesis from Probabilistic Session Types

Given a _probabilistic session type S_, this tool synthesises the Scala code of a type-checked monitor (based on the library `lchannels`). The monitor can detect deviations of the current run-time execution of a system from the probabilistic behaviour specified by the type. 

<!-- The following instructions are for recreating and executing the lottery game example (from [here](https://link.springer.com/chapter/10.1007%2F978-3-030-78142-2_7)).  -->
<!-- We assume a Unix-like operating system with Java 8 as default JRE/JDK which can be downloaded from [here](https://www.oracle.com/java/technologies/javase-jdk8-downloads.html). -->

## Compiling the sources 
  1. You will need:
     * a Java SE Development Kit (recommended: version 11 LTS)
     * [`sbt`](https://www.scala-sbt.org/download.html) 1.5.0
  2. From the main folder containing this `README.md` file, execute the command:
     ```shell
     sbt compile
     ```

## Instructions
  * how to [invoke the monitor synthesis tool](#synthesising-a-monitor)
  * how to [run the examples](#examples)
  * how to [run the SMTP benchmark](#benchmarks)

### Synthesising a Monitor

Our tool generates session type monitors consisting of two main components: the monitoring logic, and the *Continuation Passing-Style Protocol classes* (CPSP classes) that describe the type and ordering of the messages being sent and received.

To generate the monitor code and the CPSP classes, run the following command:

```shell
sbt "project monitor" "runMain monitor.Generate $DIR $ST $PREAMBLE"
```
Where:

  * `$DIR` is the directory where the source code of the monitor and classes will be generated
  * `$ST` is the file containing the session type, and
  * `$PREAMBLE` _(optional)_ is a file containing a preamble that will be added to the top of the generated files (typically containing package declarations and imports, as in [this file](examples/src/main/scala/examples/http/preamble.txt)).

Once completed, the autogenerated files `Monitor.scala` and `CPSPc.scala` will be present in the directory `$DIR`. 

The type session type `$ST` must have the following syntax:

![](https://github.com/chrisbartoloburlo/stmonitor/blob/pstmonitor/doc/st-ebnf.png | width=100)

Where the the indexing set I for choice points (& and +) is finite and non-empty, choice labels _l<sub>i</sub>_ are pairwise distinct, and the types _T<sub>i</sub>_ range over basic data types for typing variables _x<sub>i</sub>_. We give a multinomial distribution interpretation to each choice point (& and +) in a PST: we require that the probabilities within a choice point sum to 1, where every _p<sub>i</sub>_ between 0 and 1 is the probability of selecting the branch labelled by _l<sub>i</sub>_. The probabilities prescribed at a choice point represent a behavioural obligation on the interacting party that has control over the selection at that choice point. As usual, we require that recursion is guarded, _i.e.,_ a recursion variable _X_ can only appear under an external or internal choice prefix.

## Examples

The source code of all examples is available under the [`examples/`](https://github.com/chrisbartoloburlo/stmonitor/tree/pstmonitor/examples/src/main/scala/examples) directory.

Here we discuss two examples: the [guessing game](#guessing-game) found in the COORDINATION'21 paper, and a [coin flip](#coin-flip) protocol.

**Notes:**

  * Some links below point to autogenerated monitor files (not present on the GitHub repository). Before continuing, you may want create such files by running:
    ```shell
    sbt examples/compile
    ```

  * [`Python 3`](https://www.python.org/downloads/) is required to run some components of the following examples.


### Guessing game

These instructions describe how to execute the running example in the COORDINATION 2021 paper: the guessing game.

The protocol is formalised as the session type `S_game` below, as found in the file [`game.st`](https://github.com/chrisbartoloburlo/stmonitor/blob/pstmonitor/examples/src/main/scala/examples/game/game.st): 
```
S_game=rec X.(+{!Guess(num: Int)[0.75].&{?Correct()[0.01].X, ?Incorrect()[0.99].X},
                !Help()[0.2].?Hint(info: String)[1].X,
                !Quit()[0.05].end})
```
The server waits for a client to either `Quit`, send a `Guess` message carrying a `num`ber or ask for `Help`. If the client attempts to guess, the server can answer `Correct` or `Incorrect`. If the client asks for help, the server should reply with a `Hint`. In both cases, the session repeats from the start (by looping on `X`). The probabilities constrict the client from asking for too many hints (not more than 2% of the time) and ensure that the server gives the client a fair chance of guessing correctly (1% of the time). 

The synthesised monitor `Monitor.scala` and CPSP classes `CPSPc.scala`  are generated automatically from the file [`game.st`](https://github.com/chrisbartoloburlo/stmonitor/blob/pstmonitor/examples/src/main/scala/examples/game/game.st), every time the example is compiled or executed via `sbt`.

We provide the implementation of two different client-server setups that use the autogenerated monitor.

#### Setup 1: Black-box setup, client and server written in Python

In this setup, the synthesised monitor `Monitor.scala` is used to verify the interaction between a client ([`game-client.py`](https://github.com/chrisbartoloburlo/stmonitor/blob/pstmonitor/scripts/game-client.py)) and a server ([`game-server.py`](https://github.com/chrisbartoloburlo/stmonitor/blob/pstmonitor/scripts/game-server.py)) both written in Python.

In this setup the monitor makes use of *two* connection managers:

  * [one](https://github.com/chrisbartoloburlo/stmonitor/blob/pstmonitor/examples/src/main/scala/examples/game/ClientConnectionManager.scala) sits between the client and the monitor; and
  * the [other](https://github.com/chrisbartoloburlo/stmonitor/blob/07d4361a67efb444fae5a1f286e24bc2e9810f36/examples/src/main/scala/examples/game/MonWrapper.scala#L13) sits between the monitor and the server. 
  
The purpose of the connection manages is to provide a suitable message transport for the `lchannels` library and translate messages from a text-based protocol to the types present in the generated `CPSPc.scala` file, and vice-versa.

1. Start the trusted server using the following command:
    ```shell
    python3 scripts/game-server.py 1335 0.1 0.99
    ```
    where `1335` is the port open for connections, `0.1` and `0.99` are the probability with which the server will send `Correct` or `Incorrect` respectively during the session.  

2. In a separate terminal, start the monitor:
   ```shell
   sbt "project examples" "runMain examples.game.MonWrapper 1330 1.9599 false"
   ```
   where `1330` is the port where the monitor listens for client connections, `1335` is the port where the server is running, `1.9599` is the 95% confidence level and `false` sets the boolean flag to not log the session.

3. In another terminal, start the client which connects to the server (with the monitor in between) and sends multiple requests:
   ```shell
   python3 scripts/game-client.py 1330 0.75 0.2 0.05
   ```
   where `1330` is the port to which it connects to, handled by the monitor, and `0.75`, `0.2`, `0.05` are the probabilities with which the client will send `Guess`, `Help` and `Quit` respectively during the session. 

The example will execute automatically until the client sends `Quit`. The python scripts will display what they are sending and receiving, whereas the monitor will issue or retract any warnings accordingly to the specified type. To repeat the experiment, simply re-execute steps 2 and 3. 

#### Setup 2: Grey-box setup, client written in Python and server written in Scala + `lchannels` 

The synthesised monitor `Monitor.scala` (the same used in the previous setup) verifies the interaction between a server ([`Server.scala`](https://github.com/chrisbartoloburlo/stmonitor/blob/pstmonitor/examples/src/main/scala/examples/game/Server.scala)) implemented in Scala using the `lchannels` library and a client written in Python. 
 
In this setup, the monitor makes use of only one [connection manager](https://github.com/chrisbartoloburlo/stmonitor/blob/pstmonitor/examples/src/main/scala/examples/game/ClientConnectionManager.scala) which sits between the client and the monitor (the same used in the previous setup). 
 
Since the generated monitor uses `lchannels` to interact with the server, in this setup we run the monitor and server on the same JVM (as separate threads), and let them interact through a fast local message transport (i.e., via `LocalChannel`s provided by `lchannels`).

1. Start the server together with the monitor using the following command:
    ```shell
    sbt "project examples" "runMain examples.game.MonitoredServer 1330 1.9599 false"
    ```
    where `1330` is the port open for connections, `1.9599` is the 95% confidence level and `false` sets the boolean flag to not log the session. 

2. In a separate terminal, start the client (the same from the previous setup):
   ```
   python3 scripts/game-client.py 1330 0.75 0.2 0.05
   ```


### Coin flip

   
<!-- The setup should now be running. If both the client and the server were started with the probabilities as specified within the type, the monitor should not issue any warnings. The server keeps executing until it is explicitly terminated. Therefore, one can try executing a different experiment by repeating steps 2 and 3. Apart from changing the specified `z-value` for the monitor, one can also try changing the probabilities of the client such that they violate those specified within the type, which would trigger the monitor to issue warnings. In a similar manner, the server can also be initialised in such a way the monitor issues warnings on its behaviour. -->
