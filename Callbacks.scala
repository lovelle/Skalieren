/* Callbacks.scala is a tcp server listening new  connections and 
 * redirects execution callbacks to python scripts executing python 
 * as callbacks modules
 */

import akka.actor._
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import akka.event.LoggingAdapter
import java.net.InetSocketAddress
import java.io.File
import scala.sys.process._
import Tcp._
import com.typesafe.config.ConfigFactory


trait Conf extends Actor with ActorLogging {
    private val projectdir = "/opt/Skalieren"
    private val serverconf = "%s/%s".format(projectdir, "conf/server.conf")

    if (new java.io.File(serverconf).exists == false)
        retError("File %s not found".format(serverconf))

    private val conf = ConfigFactory.parseFile(new File(serverconf))
    val config = Map(
        "host" -> conf.getString("host"),
        "port" -> conf.getString("port"),
        "encode" -> conf.getString("encode"),
        "loglevel" -> conf.getString("loglevel"),
        "server_dir" -> conf.getString("server_dir"),
        "python_bin" -> conf.getString("python_bin")
    )

    private def retError(err: String) = {
        log.error(err)
        sys.exit(-1)
    }
}


class PythonCallback() {

    val server_dir = "/opt/Skalieren"
    val python_bin = "/usr/bin/python"
    val pattern_re = """\((.*?)\)""".r

    case class CallbackNotFound() extends Exception

    def handle(data : String) = {
        try { main(data) }
        catch {
            case e: StringIndexOutOfBoundsException => Error("invalid syntax")
            case e: RuntimeException => Error("external callback has errors")
            case e: CallbackNotFound => Error("callback not found or invalid")
            case e: Exception => Error("general error %s".format(e.printStackTrace))
        }
    }

    def getCmd(cli: String) = Seq(python_bin, "-u", "-O", "-c", cli)
    def getProcess(cmd : Seq[String]) = Process(cmd, new java.io.File(server_dir))
    def getQuery(method: String, data : String) = "print %s%s".format(method, funcValues(data))
    def getCli(header: Array[String], query : String) = {
        """import %s.%s; %s""".format(header.head, header(1), query)
    }

    def main(data : String): String = {
        val method = data.substring(8).split('(').head
        val header = method.split('.')

        if (fileExists(getFile(header)) == false)
            throw new CallbackNotFound

        val qry = getQuery(method, data)
        val cli = getCli(header, qry)
        val cmd = getCmd(cli)
        val ret = getProcess(cmd).!!

        "%08d%s".format(ret.length, ret)
    }

    def fileExists(file : String): Boolean = return new java.io.File(file).exists
    def Error(reason : String) = "+ERR %s\r\n".format(reason)
    def getFile(p: Array[String]) = "%s/%s/%s.py".format(server_dir, p.head, p(1))
    def funcValues(data : String) = (pattern_re findFirstIn data).mkString
}


class Client(config : Map[String, String]) extends PythonCallback {

    override val server_dir = config{"server_dir"}
    override val python_bin = config{"python_bin"}
    private val encoding = config{"encode"}

    def response(data: ByteString) = {
        ByteString(process(data))
    }

    private def process(data: ByteString): String = data.decodeString(encoding).filter(_ >= ' ') match {
        //case x if x.toLowerCase().contains("ping") => pong
        case "ping" => pong
        case default => handle(default)
    }

    private def pong: String = "+PONG\r\n"
}


class Handler(connection: ActorRef, remote: InetSocketAddress) extends Conf {
    // sign death pact: this actor terminates when connection breaks
    context watch connection

    case object Ack extends Event

    def receive = {
        case Received(data) =>
            val client = new Client(config)
            log.debug("Writing to client %s".format(remote))
            //sender() ! Write(pong)
            connection ! Write(client.response(data), Ack)

        case PeerClosed => context stop self
    }
}


class Server() extends Conf {

    import context.system
    val host = config{"host"}
    val port = config{"port"}.toInt

    override def preStart(): Unit = {
        IO(Tcp) ! Bind(self, new InetSocketAddress(host, port))
    }

    override def postRestart(thr: Throwable): Unit = context stop self

    def receive = {
        case Bound(localAddress) => log.info("Server listening on %s:%s".format(host, port))
        case CommandFailed(_: Bind) =>
            log.warning("cannot bind on %s:%s".format(host, port))
            context stop self

        case Connected(remote, local) =>
            log.debug("received connection from: %s".format(remote))
            val connection = sender()
            val handler = context.actorOf(Props(classOf[Handler], connection, remote))
            connection ! Register(handler, keepOpenOnPeerClosed = true)
    }
}


object Callbacks extends App {
    val config = ConfigFactory.parseString("akka.loglevel = DEBUG")
    val sys = ActorSystem("Main", config)
    val act = sys.actorOf(Props[Server])
}
