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

To generate the monitor and the CPSP classes, run `Generate.scala` using the following command in a terminal inside the project root directory:
```shell
sbt "project monitor" "runMain monitor.Generate $DIR $ST $PREAMBLE"
```
Replace: 
1. `$DIR` with the absolute path to the directory in which the source code of the monitor and classes shall be generated in, 
2. `$ST` with the absolute path to the file containing the session type, and
3. `$PREAMBLE` _(optional argument)_ with the absolute path to a text file containing a preamble that will be added to the top of the generated files (typically consisting of the package declaration and specific imports required such as [this file](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/http/preamble.txt)).

Once completed, the files `Monitor.scala` and `CPSPc.scala` should be present in the provided directory in `$DIR`. 

In the case that the type contains bespoke functions as assertions, ensure that they are present in a file named `util.scala` located in the _same directory_ `$DIR` provided in the synthesis. The synthesis automatically type-checks the assertions using the Scala compiler, and ensures that they have a `Boolean` type. In any of these cases, the monitor would flag a violation if the expression evaluates to `false` at runtime. 

The following is a step-by-step example to generate the source code from a session type.

> _Example_: 
> The type `S_calc` below, taken from [`calc.st`](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/calc/calc.st), can be found in the directory [`calc/`](https://github.com/chrisbartoloburlo/stmonitor/tree/master/examples/src/main/scala/examples/calc) together with [`util.scala`](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/calc/util.scala) implementing the function `util.checkAdd()` and [`preamble.txt`](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/calc/preamble.txt).
> ```
> S_calc=rec X.(&{?Add(num1: Int, num2: Int).!Res(ans: Int)[util.checkAdd(num1, num2, ans)]})
> ```
> To generate the monitor and CPSP classes from this type, execute the following command from the root directory, replacing `[root]` accordingly:
> ```shell
> sbt "project monitor" "runMain monitor.Generate [root]/stmonitor/examples/src/main/scala/examples/calc [root]/stmonitor/examples/src/main/scala/examples/calc/calc.st [root]/stmonitor/examples/src/main/scala/examples/calc/preamble.txt"
> ```
> If successful, you should see the following in the terminal:
> ```shell
> $ INFO Synth - Input type calc.st parsed successfully
> $ INFO Synth - Successful synthesis for input type calc.st at [root]/stmonitor/examples/src/main/scala/examples/calc
> ```
> The files should be present in the provided directory [`calc/`](https://github.com/chrisbartoloburlo/stmonitor/tree/master/examples/src/main/scala/examples/calc).
>
> We encourage the reader to extend the type such as below, execute the synthesis once again and analyse the synthesised code.
> ```
> S_calc=rec X.(&{
>     ?Add(num1: Int, num2: Int).!Res(ans: Int)[util.checkAdd(num1, num2, ans)],
>     ?Sub(num1: Int, num2: Int).!Res(ans: Int)[ans==num2-num1]
> })
> ```

Proceed to the next section for instructions on how to launch a synthesised monitor between two interacting parties. 

## Examples

**Before attempting to launch any examples, ensure that the project [compiled](https://github.com/chrisbartoloburlo/stmonitor#compiling-the-sources) successfully.**

The following commands assume a Unix-like operating system and should all be executed from the project root directory `stmonitor/`. 

_Note_: [`Python 3.x`]() might be required to launch some of the following examples.

### 1. Authentication protocol

These instructions are for executing the an extended version of the running example in the accompanying paper: the authentication protocol. It is formalised as the session type `S_auth` below found in [`auth.st`](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/auth/auth.st):
```
S_auth=rec Y.(!Auth(uname: String, pwd: String)[util.validateUname(uname)].&{
	?Succ(origTok: String)[util.validateTok(origTok, uname)].rec X.(+{
		!Get(resource: String, reqTok: String).&{
			?Res(content: String)[origTok==reqTok].X,
			?Timeout().Y
		},
		!Rvk(rvkTok: String)[origTok==rvkTok].end}),
	?Fail(code: Int).end
})
```
After authenticating with the server, the client is granted exclusive access to a resource via a token `origTok`. The token might expire after a while and the server would send a `Timeout` message, allowing the client to request another token. Otherwise, the client can revoke the token prematurely by sending `Rvk` and the session terminates. 

In this type, the username communicated by the client would be validated using the function `validateUname()`. Similarly, the monitor will validate the token `origTok` issued upon successful authentication using `validateTok()`. The monitor would also check that when the server sends a resource in `Res`, the token included in the `Get` message (`reqTok`) was equivalent to the one issued earlier in the `Succ` message (`origTok`). 

The functions `validateUname()` and `validateTok()` can be found in [`util.scala`](). For the sake of the example, they will always return `true`; one can change their return value to `false` and see that the monitor indeed flags a violation. The monitor `Monitor.scala` and `CPSPc` found in the auth/ directory were generated automatically from this type upon compilation. 

We provide the implementation of two different client-server setups in which this monitor can be launched:

#### **SETUP 1** 

The synthesised monitor verifying the interaction between an _unsafe_ client implemented in Python and a _safe_ server implemented in Scala. The monitor and server are executed on the _same_ JVM as _separate_ threads and interact via `lchannels`.

1. Start the server together with the monitor using the following command:
    ```shell
    sbt "project examples" "runMain examples.auth.MonitoredServer"
    ```
   _The monitored server listens on the TCP/IP socket 127.0.0.1:1330._

2. In a separate terminal execute the following command to start a client which connects to the server and sends multiple requests:
   ```
   python3 auth-client.py 
   ```
   Alternatively, one can also interact with the monitored server using `telnet 127.0.0.1 1330` and follow a text based protocol. 

In this setup the monitor makes use of a [connection manager](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/auth/ConnectionManager.scala), which sits between the server and the monitor. This translates messages from a text based protocol to the types present in the generated `CPSPc.scala` file and vice-versa. 

#### **SETUP 2**  

The synthesised monitor verifying the interaction between a client and a server that are both _unsafe_ and implemented in Python. The monitor executes on its own JVM.

1. Start the server using the following command:
    ```shell
    python3 scripts/auth-server.py
    ```
   The server listens on the TCP/IP socket 127.0.0.1:1335.

2. In a separate terminal execute the following command to start a monitor:
   ```shell
   sbt "project examples" "runMain examples.auth.MonWrapper $LISTEN_PORT $FORWARDING_PORT"
   ```
   Replace `$LISTEN_PORT` with the port to expose for a client: _1330_, and `$FORWARDING_PORT` with the port for the monitor to connect to: _1335_. The monitor should connect to the Python server via the port _1335_ and wait for a connection from a client.

3. In another terminal execute the following command to start a Python client:
   ```shell
   python3 scripts/auth-client.py
   ```
   The client should send and receive messages via the port _1330_ which is handled by the monitor. In turn, the monitor analyses and forwards the messages to the server and client. 

In this setup the monitor makes use of two connection managers: [one](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/auth/ConnectionManager.scala) sits between the server and the monitor, as used in previous setup, and the [other](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/auth/MonWrapper.scala#L12) sits between the monitor and the client, also translating messages to and from the types found in `CPSPC.scala` and the text-based protocol.

### 2. Lottery game protocol

The type `S_game` below can be found in [`game.st`](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/game/game.st). In this case, the type describes the protocol from the server side.
```
S_game=rec X.(&{
  ?Guess(num: Int)[num > 1 && num < 100].+{
		!Correct(ans: Int)[ans==num],
		!Incorrect().X
	},
	?Quit()
})
```
We provide the implementation of a client in Scala respecting the dual of `S_game` in [`game/`](https://github.com/chrisbartoloburlo/stmonitor/tree/master/examples/src/main/scala/examples/game) and also implementation of a [server](https://github.com/chrisbartoloburlo/stmonitor/blob/master/scripts/game-server.py) implemented in Python. 

1. To start the server execute: 
   ```shell
   python3 scripts/game-server.py
   ```
   The server listens on the TCP/IP socket 127.0.0.1:1330 and generates a random number between 1 and 100 once a client connects. The client can recursively send guesses and if it guesses the number, the server replies with `Correct` and terminates the session. Otherwise, the server replies with `Incorrect` and the session recurses, giving the client another chance to guess correctly. 
2. The generated monitor can be launched on the same JVM with a [`client`](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/game/MonitoredClient.scala) (as a separate thread) by executing:
   ```shell
   sbt "project examples" "runMain examples.game.MonitoredClient"
   ```
   The monitored client automatically connects to the server on the port 1330, and asks for the user to input a guess which is sent to the server. The client recurses until the server replies with `Correct` or the user quits (in which case the client will send `Quit`). One can test that the monitor flags a violation and terminates the session if a number smaller than 1 or greater than 100 is sent as a guess. 

### 3. Fragment of the Simple Mail Transfer Protocol

[`smtp.st`](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/smtp/smtp.st)
```
S_smtp = !M220(msg: String).&{
	?Helo(hostname: String).!M250(msg: String).rec X.(&{
		?MailFrom(addr: String).!M250(msg: String).rec Y.(&{
			?RcptTo(addr: String).!M250(msg: String).Y,
			?Data().!M354(msg: String).?Content(txt: String).!M250(msg: String).X,
			?Quit().!M221(msg: String)}),
		?Quit().!M221(msg: String)}),
	?Quit().!M221(msg: String) }
```

### 4. Ping Pong protocol over HTTP

[`pingpong.st`](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/pingpong/pingpong.st)
```
S_ponger=rec X.(+{!Ping().?Pong().X, !Quit().end})
```

### 5. Fragment of the HTTP protocol

[`http.st`](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/http/http.st)
```
S_http=!Request(msg: RequestLine).rec X.(+{
	!AcceptEncodings(msg: String).X,
	!Accept(msg: String).X,
	!DoNotTrack(msg: Boolean).X,
	!UpgradeIR(msg: Boolean).X,
	!Connection(msg: String).X,
	!UserAgent(msg: String).X,
	!AcceptLanguage(msg: String).X,
	!Host(msg: String).X,
	!RequestBody(msg: Body).?HttpVersion(msg: Version).&{
		?Code404(msg: String).rec Y.(&{
			?ETag(msg: String).Y,
			...
		}),
		?Code200(msg: String).rec Z.(&{
			?ETag2(msg: String).Z,
			...
		})
	}
})
```


## Benchmarks

The benchmarks can _only_ be executed on a Linux-based machine since they rely on [`/usr/bin/time`](https://man7.org/linux/man-pages/man1/time.1.html) for the collection of data on CPU usage and memory consumption. The following dependencies are also required:
* [`screen`](https://man7.org/linux/man-pages/man1/screen.1.html)
* [`Python 3`]() and [`matplotlib`]()
* [`JMeter`](https://jmeter.apache.org)

To execute a _minimal_ version of the benchmarks and ensure that everything works execute: 
```shell
sh scripts/benchmarks.sh kickthetires
```

To execute the _full_ benchmarks execute:
```shell
sh scripts/benchmarks.sh $iterations $experiments
```
where: 
* `$iterations` is the number of repetitions to be executed per experiment (we recommend 5), and 
* `$experiments` is a space-separated list of the experiments to be conducted. The choices are: 
  * `smtp-python` consisting of a client that sends a number of emails to an untrusted SMTP python sever [`smtpd`](https://docs.python.org/3/library/smtpd.html) from its standard library following a fragment of SMTP formalised as the type [`smtp.st`](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/smtp/smtp.st);
  * `smtp-postfix` consisting of a similar setup to the previous experiment but uses [Postfix](http://www.postfix.org) as an SMTP server;
  * `pingpong` consisting of a server that interacts over HTTP and receives requests from JMeter which follows the type [`pingpong.st`](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/pingpong/pingpong.st); and
  * `http` consisting of a web server and a client following a fragment of the HTTP protocol formalised as the type [`http.st`](https://github.com/chrisbartoloburlo/stmonitor/blob/master/examples/src/main/scala/examples/http/http.st).
