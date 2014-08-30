package org.eigengo.sbtraml

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSystem, Props}
import akka.io.IO
import com.typesafe.config.{ConfigFactory, Config}
import org.raml.model._
import org.raml.model.parameter.UriParameter
import org.raml.parser.visitor.RamlDocumentBuilder
import spray.can.Http
import spray.http.Uri.Path
import spray.http.Uri.Path.{Segment, Slash}
import spray.http._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

class RamlMock(resourceLocation: File, settings: MockSettings) extends RamlSources {
  private val resourceLocationPath: String = resourceLocation.getAbsolutePath
  private val ramlDefinitions = findFiles(resourceLocation).map(ramlFile => new RamlDocumentBuilder().build(load(ramlFile), resourceLocationPath)).toList

  def start(): Unit = {
    implicit val system = ActorSystem("raml-mock", ConfigFactory.parseString(
      """
        |akka.version=2.3.5
        |akka.actor.guardian-supervisor-strategy = "akka.actor.DefaultSupervisorStrategy"
        |akka.actor.creation-timeout=30s
        |akka.actor.unstarted-push-timeout=30s
        |akka.actor.serialize-messages=off
        |akka.actor.serialize-creators=off
        |loglevel = "INFO"
        |akka.actor.provider = "akka.actor.LocalActorRefProvider"
        |akka.log-dead-letters = off
        |akka.log-dead-letters-during-shutdown = off
      """.stripMargin))
    val mockServer = system.actorOf(Props(new MockServiceActor(ramlDefinitions, settings)))
    IO(Http) ! Http.Bind(mockServer, interface = settings.interface, port = settings.port)
  }

}

trait MockService {

  object predicates {
    sealed trait MatchResult {
      def &&(that: MatchResult): MatchResult
      def &&(that: Boolean): MatchResult = if (that) this else Unmatched
    }
    case object MatchedCompletely extends MatchResult {
      def &&(that: MatchResult): MatchResult = that
    }
    case object MatchedStart extends MatchResult {
      def &&(that: MatchResult): MatchResult = that
    }
    case object Unmatched extends MatchResult {
      def &&(that: MatchResult): MatchResult = Unmatched
    }

    private implicit class RichTry[A](t: Try[A]) {
      def cata[B](left: => B, right: A => B): B = t match {
        case Success(a) => right(a)
        case Failure(_) => left
      }
    }

    def pathParameterPredicate(segment: String, value: String, parameters: Map[String, UriParameter]): MatchResult = {
      import scala.collection.JavaConversions._

      def matchParamType(p: UriParameter): MatchResult = p.getType match {
        case null => MatchedCompletely
        case ParamType.STRING =>
          // TODO: Should failing length be 400 or 404? (404 now)
          val min = Option(p.getMinLength).map(_.intValue()).getOrElse(0)
          val max = Option(p.getMaxLength).map(_.intValue()).getOrElse(Int.MaxValue)
          if (value.length >= min && value.length < max) MatchedCompletely else Unmatched
        case ParamType.BOOLEAN => Try(value.toBoolean).cata(Unmatched, _ => MatchedCompletely)
        case ParamType.NUMBER | ParamType.INTEGER =>
          // TODO: Should failing range be 400 or 404? (404 now)
          Try(BigDecimal(value)).cata(Unmatched, n =>
            Option(p.getMinimum).map(x => if (n >= x) MatchedCompletely else Unmatched).getOrElse(MatchedCompletely) &&
            Option(p.getMaximum).map(x => if (n <  x) MatchedCompletely else Unmatched).getOrElse(MatchedCompletely) &&
            MatchedCompletely
          )

        // TODO: What about these two?
        case ParamType.DATE => MatchedCompletely
        case ParamType.FILE => MatchedCompletely
      }

      (for {
        parameter   <- parameters.get(segment.substring(1, segment.length - 1))
        typeMatch   =  matchParamType(parameter)
        enumeration =  Option(parameter.getEnumeration).map(_.toList).getOrElse(Nil)
        enumMatch   =  enumeration.isEmpty || enumeration.contains(value)
      } yield typeMatch && enumMatch).getOrElse(MatchedCompletely)
    }

    def uriPathPredicate(uri: Uri, r: Resource): MatchResult = {
      import scala.collection.JavaConversions._

      def paths(segments: List[String], path: Path): MatchResult = (segments, path) match {
        case (seg::_, Path.Empty) => Unmatched
        case (Nil, Path.Empty) => MatchedCompletely
        case (Nil, p) => MatchedStart
        case ("/"::segs, Slash(tail)) => paths(segs, tail)
        case (seg::segs, Segment(p, tail)) =>
          if (seg.head == '{' && seg.last == '}') {
            pathParameterPredicate(seg, p, r.getUriParameters.toMap) && paths(segs, tail)
          } else if (seg == p) paths(segs, tail) else Unmatched
        case (s, p) =>
          throw new RuntimeException("Not implemented URI predicate for " + s + " -> " + p)
      }

      val segments = r.getUri.split("/").toList.flatMap { segment =>
        if (segment == "") Seq()
        else Seq("/", segment)
      }

      paths(segments, uri.path)
    }

  }

  def mock(ramls: List[Raml], settings: MockSettings, request: HttpRequest)(implicit system: ActorSystem): Future[HttpResponse] = {
    import predicates._
    import scala.collection.JavaConversions._

    def methodToActionType(method: HttpMethod): ActionType = method match {
      case HttpMethods.GET => ActionType.GET
      case HttpMethods.PUT => ActionType.PUT
      case HttpMethods.POST => ActionType.POST
      case HttpMethods.DELETE => ActionType.DELETE
      case HttpMethods.OPTIONS => ActionType.OPTIONS
      case HttpMethods.HEAD => ActionType.HEAD
      case HttpMethods.PATCH => ActionType.PATCH
      case HttpMethods.TRACE => ActionType.TRACE
    }

    def findResource(resource: Resource): Option[Resource] = {
      uriPathPredicate(request.uri, resource) match {
        case Unmatched => None
        case MatchedCompletely => Some(resource)
        case MatchedStart => resource.getResources.values().find(uriPathPredicate(request.uri, _) == MatchedCompletely)
      }
    }

    def findResponse(action: Action, responseCode: String): Option[Response] = {
      // TODO: Deal with content types
      Option(action.getResponses.get(responseCode))
    }

    def mkHttpResponse(response: Response): HttpResponse = {
      val r = for {
        ct  <- request.acceptableContentType(response.getBody.keySet().map(mt => ContentType(MediaType.custom(mt))).toSeq)
        (_, res) <- response.getBody.find { case (rct, _) => ct.mediaType.value == rct }
      } yield HttpResponse(entity = HttpEntity(ct, res.getExample))

      r.getOrElse(HttpResponse(StatusCodes.NotAcceptable))
    }

    val responseCode = request.headers.find(_.is(settings.responseCodeHeaderName.toLowerCase)).map(_.value).getOrElse("200")
    val timeout = request.headers.find(_.is(settings.responseTimeHeaderName.toLowerCase)).map(_.value.toInt).getOrElse(0)
    val fail = request.headers.exists(_.is(settings.failResponseHeaderName.toLowerCase))

    val responses = ramls.flatMap { raml =>
      for {
        resource <- raml.getResources.values().flatMap(findResource).headOption
        action   <- Option(resource.getAction(request.method.toString()))
        response <- findResponse(action, responseCode)
      } yield mkHttpResponse(response)
    }

    val response = responses match {
      case Nil => HttpResponse(StatusCodes.NotFound)
      case r::Nil => r
      case r::rs => HttpResponse(StatusCodes.MultipleChoices)
    }

    import akka.pattern.after
    import system.dispatcher
    after(FiniteDuration(timeout, TimeUnit.MILLISECONDS), system.scheduler)(Future(response))
  }

}

class MockServiceActor(definitions: List[Raml], settings: MockSettings) extends Actor with MockService {
  import akka.pattern.pipe
  import context.system
  import context.dispatcher

  def receive: Receive = {
    case Http.Connected(_, _) => sender ! Http.Register(self)
    case r@HttpRequest(_, _, _, _, _) => mock(definitions, settings, r) pipeTo sender()
  }
}