package examples.auth

import monitor.util.ConnectionManager

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}

class ClientConnectionManager(client: java.net.Socket) extends ConnectionManager {
  var outB: BufferedWriter = _
  var inB: BufferedReader = _

  private val authR = """AUTH (.+) (.+)""".r
  private val getR = """GET (.+) (.+)""".r
  private val rvkR = """RVK (.+)""".r

  def setup(): Unit = {
    outB = new BufferedWriter(new OutputStreamWriter(client.getOutputStream))
    inB = new BufferedReader(new InputStreamReader(client.getInputStream))
  }

  def receive(): Any = inB.readLine() match {
    case authR(uname, pwd) => Auth(uname, pwd)(null);
    case getR(resource, tok) => Get(resource, tok)(null);
    case rvkR(tok) => Rvk(tok);
    case e => e
  }

  def send(x: Any): Unit = x match {
    case Succ(tok) => outB.write(f"SUCC ${tok}\r\n"); outB.flush();
    case Res(content) => outB.write(f"RES $content\r\n"); outB.flush();
    case Timeout() => outB.write(f"Timeout\r\n"); outB.flush();
    case Fail(code) => outB.write(f"FAIL $code\r\n"); outB.flush();
    case _ => close(); throw new Exception("[CM] Error: Unexpected message by Mon");
  }

  def close(): Unit = {
    outB.flush();
    inB.close();
    outB.close();
  }
}