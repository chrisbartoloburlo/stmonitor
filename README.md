# STMonitor - Session Types Monitor Synthesiser

Given a session type _S_, this tool synthesises the Scala code of a type-checked monitor (based on the library [lchannels](https://github.com/alcestes/lchannels)). The monitor verifies at runtime whether the interactions between a client and server follow the protocol _S_. 

The approach and its underlying theory are described in the following papers:

* Christian Bartolo Burlò, Adrian Francalanza and Alcese Scalas. *[Towards a Hybrid Verification Methodology for Communication Protocols (Short Paper)](https://doi.org/10.1007/978-3-030-50086-3_13)*. FORTE 2020. ([Pesentation video](https://youtu.be/FL_teSjllSE))

* Christian Bartolo Burlò, Adrian Francalanza and Alcese Scalas. *On the Monitorability of Session Types, in Theory and Practice*. ECOOP 2021 (to appear),

## Compiling the sources

  1. You will need:
     * a Java SE Development Kit (recommended: version 11 LTS)
     * [`sbt`](https://www.scala-sbt.org/download.html) 1.5.0
  2. From the main folder containing this `README.md` file, execute the command:
     ```shell
     sbt compile
     ```

## Instructions

The remainder of this file documents:

  * how to [invoke the monitor synthesi tool](#synthesising-a-monitor)
  * how to [run the examples](#examples)
  * how to [run the benchmarks](#benchmarks)

All the commands below are executed from a shell in the main folder of the `STMonitor` source code (i.e., where this `README.md` file is located).

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

Once completed, the files `Monitor.scala` and `CPSPc.scala` will be present in the directory in `$DIR`. 

The type session type `$ST` can use boolean Scala functions as assertions, to perform run-time checks on the values being transmitted or received. If this feature is used, the monitor synthesiser expects that:

  1. all assertion functions begin with `util...` (for example: `util.assertNonNegative(x)`), and
  2. all assertion functions are defined in a file named `util.scala` located in the directory `$DIR` provided to the synthesiser.
  
The synthesis type-checks the assertion functions using the Scala compiler, and ensures that they have a `Boolean` type. 

## Examples

We discuss two examples: the [authentication protocol](#authentication-protocol) in the ECOOP'21 paper, and a [guessing game](#guessing-game).

We also discuss an (incomplete) ([calculator example](#calculator)) to observe the monitor generation output.

**Note:** [`Python 3`](https://www.python.org/downloads/) is required to run some components of the following examples.

### Authentication protocol

These instructions describe how to execute an extended version of the running example in the ECOOP 2021 paper: the authentication protocol.

The protocol is formalised as the session type `S_auth` below, as found in the file [`auth.st`](examples/src/main/scala/examples/auth/auth.st):

```
S_auth = rec Y.(!Auth(uname: String, pwd: String)[util.validateUname(uname)].&{
	?Succ(origTok: String)[util.validateTok(origTok, uname)].rec X.(+{
		!Get(resource: String, reqTok: String).&{
			?Res(content: String)[origTok==reqTok].X,
			?Timeout().Y
		},
		!Rvk(rvkTok: String)[origTok==rvkTok].end}),
	?Fail(code: Int).end
})
```

The protocol begins with the client sending an `Auth`entication request to the server.
If the authentication is `Succ`essful, the client is given a token `origTok`, and is allowed to request resources by sending a `Get` message. The token might expire after a while, hence the server might either:
  * provide the requested `Res`ource and allow for more `Get` requests (through the recursion on `X`), or
  * answer `Timeout`, letting the client re-authenticate and obtain another token (through the recursion on `Y`).
The client can also revoke the token (by sending the message `Rvk`), which ends the session.

The session type above specifies various predicates that the synthetised monitor checks at run-time:
  * the username sent by the client is validated using the function `util.validateUname()`
  * the token `origTok` issued upon successful authentication is validated using the function `util.validateTok()`
  * whenever the server sends a `Res`ource, the token included in the `Get` message (`reqTok`) must be equal to the token issued in the previous `Succ` message(`origTok`)
  * whenever the message `Rvk` is sent, its token `rvkTok` must be the same as `origTok`.

The functions `util.validateUname()` and `util.validateTok()` can be found in the file [`util.scala`](examples/src/main/scala/examples/auth/util.scala). For the sake of the example, their implementation always returns `true`; one can change their return value to `false` and see that the monitor indeed flags assertion violations.

The monitor [`Monitor.scala`](examples/src/main/scala/examples/auth/Monitor.scala) and the CPSP classes [`CPSPc.scala`](examples/src/main/scala/examples/auth/CPSPc.scala) found in the [`auth/`](examples/src/main/scala/examples/auth/) directory are generated automatically from the file [`auth.st`](examples/src/main/scala/examples/auth/auth.st). 

We provide the implementation of two different client-server setups that use the autogenerated monitor.

#### Setup 1: untrusted client, trusted server written in Scala+lchannels

 The synthesised monitor ([`Monitor.scala`](examples/src/main/scala/examples/auth/Monitor.scala)) verifies the interaction between an untrusted client and a trusted server ([`Server.scala`](examples/src/main/scala/examples/auth/Server.scala)) implemented in Scala using the `lchannels` library.
 
 In this setup the monitor makes use of a [connection manager](examples/src/main/scala/examples/auth/ConnectionManager.scala), which sits between the untrusted client and the monitor: its purpose is to translate messages from a text-based protocol to the types present in the generated [`CPSPc.scala`](examples/src/main/scala/examples/auth/CPSPc.scala) file, and vice-versa.
 
 Since the generated monitor uses `lchannels` to interact with the trusted side, we in this setup run the monitor and server on the same JVM (as separate threads), and let them interact through a fast local message transport (i.e., via `LocalChannel`s provided by `lchannels`).

1. Start the server together with the monitor using the following command:
    ```shell
    sbt "project examples" "runMain examples.auth.MonitoredServer"
    ```

2. In a separate terminal, start an untrusted client (a Python script) which connects to the server (with the monitor in between) and sends multiple requests:
   ```
   python3 scripts/auth-client.py 
   ```

The Python script above will print the sent/received messages on screen. You can also interact with the monitored server manually, by executing, e.g.:

```shell
netcat 127.0.0.1 1330
```

By altering the messages sent to the server, you can observe the monitor flag violations and close the ongoing session. You can try, e.g., sending wrong messages (such as `AUTHXXX` instead of `AUTH`), or invalid tokens.

#### Setup 2

The synthesised monitor ([`Monitor.scala`](examples/src/main/scala/examples/auth/Monitor.scala), the same used in the previous setup) verifies the interaction between an untrusted client and a trusted server --- this type, written in Python ([`auth-server.py`](scripts/auth-server.py)), hence *not* using `lchannels`.

In this setup the monitor makes use of *two* connection managers:

  * [one](examples/src/main/scala/examples/auth/ConnectionManager.scala) sits between the client and the monitor (it is the same used in the previous setup);
  * the [other](examples/src/main/scala/examples/auth/MonWrapper.scala#L12) sits between the monitor and the server, and is used by the `lchannels` library. This connection manager also translates messages to/from the types found in the [`CPSPc.scala`](examples/src/main/scala/examples/auth/CPSPc.scala) file --- but it performs less run-time checks, since it is facing the trusted server.


1. Start the trusted server using the following command:
    ```shell
    python3 scripts/auth-server.py
    ```

2. In a separate terminal, start the monitor:
   ```shell
   sbt "project examples" "runMain examples.auth.MonWrapper 1330 1335"
   ```
   where `1330` is the port where the monitor listens for client connections, and `1335` is the port where the trusted server is running.

3. In another terminal, start an untrusted client (a Python script):
   ```shell
   python3 scripts/auth-client.py
   ```
   The client sends/receives messages via port 1330, which is handled by the monitor. In turn, the monitor analyses and forwards the messages to the server, and forwards its responses to the client. 

### Guessing game

The type `S_game` below can be found in [`game.st`](examples/src/main/scala/examples/game/game.st). In this case, the type describes the protocol from the server side.
```
S_game = rec X.(&{
  ?Guess(num: Int)[num > 1 && num < 100].+{
		!Correct(ans: Int)[ans==num],
		!Incorrect().X
	},
	?Quit()
})
```

The server waits for the client to either `Quit`, or send a `Guess` message carrying a `num`ber; the assertion checks that `num` is between 1 and 100. The server can answer `Correct` (carryng the same number guessed by the client) or `Incorrect` --- and in this case, the session repeats from the start (by looping on `X`).

We provide the implementation of a [client written in Scala](examples/src/main/scala/examples/game/MonitoredClient.scala), and a [server](scripts/game-server.py) written in Python. 

1. To start the server, execute: 
   ```shell
   python3 scripts/game-server.py
   ```
   The server generates a random number between 1 and 100 once a client connects. The client can recursively send guesses and if it guesses the number, the server replies with `Correct` and terminates the session. Otherwise, the server replies with `Incorrect` and the session recurses, giving the client another chance to guess correctly.

2. In a separate terminal, you can launch a [`client`](examples/src/main/scala/examples/game/MonitoredClient.scala) that also spawns the [monitor synthesised from `S_game`](examples/src/main/scala/examples/game/Monitor.scala) (as a separate thread), by executing:
   ```shell
   sbt "project examples" "runMain examples.game.MonitoredClient"
   ```
   The monitored client connects to the server on the port 1330, and asks the user to input a guess which is sent to the server. The client iterates until the server replies with `Correct`, or the user quits. To test that the monitor flags a violation and terminates the session, you can try providing a number smaller than 1 or greater than 100. 

### Calculator

This example shows how to experiment with the monitor synthesis. Unlike the previous examples, here we do not provide an implementation of a client or server using the generated monitor.

The session type `S_calc` below (taken from [`calc.st`](examples/src/main/scala/examples/calc/calc.st)) describes a simple calculator, that (recursively) lets user ask to `Add` two numbers, and answers with their sum.
```
S_calc = rec X.(&{?Add(num1: Int, num2: Int).!Res(ans: Int)[util.checkAdd(num1, num2, ans)]})

```
To generate the monitor and CPSP classes from this type, execute the following command from the root directory, replacing `[root]` accordingly:

```shell
sbt "project monitor" "runMain monitor.Generate examples/src/main/scala/examples/calc examples/src/main/scala/examples/calc/calc.st examples/src/main/scala/examples/calc/preamble.txt"
```

The generated monitor uses a [`preamble`](examples/src/main/scala/examples/calc/preamble.txt) and the file [`util.scala`](examples/src/main/scala/examples/calc/util.scala) plementing the function `util.checkAdd()`, where the assertion function `util.checkAdd` is defined.

If successful, the monitor synthesis produces the following output:

```shell
INFO Synth - Input type calc.st parsed successfully
INFO Synth - Successful synthesis for input type calc.st at stmonitor/examples/src/main/scala/examples/calc
```

The generated files are [`Monitor.scala`](examples/src/main/scala/examples/calc/Monitor.scala) and [`CPSPc.scala`](examples/src/main/scala/examples/calc/CPSPc.scala).

We encourage the reader to extend the session type `S_calc`, re-execute the monitor synthesis, and observe the changes in the generated code.
For example, this variation of the type adds the option to perform a subtraction, and checks that the result is correct:

```
S_calc = rec X.(&{
    ?Add(num1: Int, num2: Int).!Res(ans: Int)[util.checkAdd(num1, num2, ans)],
    ?Sub(num1: Int, num2: Int).!Res(ans: Int)[ans==num2-num1]
})
```

## Benchmarks

The benchmarks can _only_ be executed on Linux, or other Unix-like systems providing [`/usr/bin/time`](https://man7.org/linux/man-pages/man1/time.1.html) --- which we use for observing CPU usage and memory consumption. The following dependencies are also required:
* [GNU screen](https://www.gnu.org/software/screen/)
* Python 3 and [Matplotlib](https://matplotlib.org/)
* [JMeter](https://jmeter.apache.org)

To execute a _kick-the-tires_ version of the benchmarks (taking around 5 minutes), you can run:

```shell
sh scripts/benchmarks.sh kickthetires
```

When it finishes, the benchmarking script above prints the directories containing the generated plots (in PDF format). If such PDF files are available, then the full benchmarks below should work without issues (note, however, that the generated kick-the-tires PDFs are not very informative).

To run the _full_ benchmarks, and generate complete plots, you can execute:

```shell
sh scripts/benchmarks.sh $REPETITIONS $EXPERIMENTS
```

where: 
* `$REPETITIONS` is the number of repetitions of each experiment:
  * we recommend a minimum of 5 repetitions (taking around 3 hours on a virtual machine running on a dual-core Intel Core i5, 8 GB RAM, macOS 11.2.3);
  * the plots in the ECOOP'21 paper have been generated with 30 repetitions.
* `$EXPERIMENTS` is a space-separated list of the experiments to run. The choices are one or more of: 
  * `smtp-python`: a trusted client sends a number of emails to an untrusted SMTP server, implemented using [`smtpd`](https://docs.python.org/3/library/smtpd.html) from the Python standard library.  The monitor for this experiment is generated from a fragment of the SMTP protocol, formalised as the session type [`smtp.st`](examples/src/main/scala/examples/smtp/smtp.st);
  * `smtp-postfix`: similar to the previous experiment, except that it uses an untrusted SMTP server listening on port `127.0.0.1:25`.  The plots of the ECOOP'21 paper are genarated using the [Postfix](http://www.postfix.org) SMTP server;
  * `pingpong`: a trusted server accepts 'ping' requests over HTTP, and answers 'pong', according to the session type [`pingpong.st`](examples/src/main/scala/examples/pingpong/pingpong.st). The untrusted client is JMeter, configured to send an increasing number of requests and measure the server performance;
  * `http`: a trusted web server implements a fragment of the HTTP protocol formalised as the session type [`http.st`](examples/src/main/scala/examples/http/http.st). The untrusted client is JMeter, configured to send an increasing number of requests and measure the server performance.

When it finishes, the benchmarking script above prints the directories containing the generated plots (in PDF format).
