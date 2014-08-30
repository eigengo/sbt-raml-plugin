package org.eigengo.sbtraml

import java.io.File

import akka.actor.{LightArrayRevolverScheduler, Actor, ActorSystem, Props}
import akka.io.IO
import com.typesafe.config.ConfigFactory
import org.raml.model._
import org.raml.model.parameter.UriParameter
import org.raml.parser.visitor.RamlDocumentBuilder
import sbt.Keys._
import spray.can.Http
import spray.http.Uri.Path
import spray.http.Uri.Path.{Segment, Slash}
import spray.http._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * RAML Mock task starts a server that mocks
 *
 * @param resourceLocation the resource location of the RAML files
 * @param settings the settings
 * @param s the TaskStreams
 */
class RamlMock(resourceLocation: File, settings: MockSettings, s: TaskStreams) extends RamlSources {
  private val resourceLocationPath: String = resourceLocation.getAbsolutePath
  private val ramlDefinitions = findFiles(resourceLocation).map(ramlFile => new RamlDocumentBuilder().build(load(ramlFile), resourceLocationPath)).toList

  def start(): Unit = {
    // Weird SBT class loading error fixed by the following:
    LightArrayRevolverScheduler.getClass
    implicit val system = ActorSystem("raml-mock",
      ConfigFactory.load(getClass.getClassLoader, "/raml-mocks.conf"), classLoader = getClass.getClassLoader)
    val mockServer = system.actorOf(Props(new MockServiceActor(ramlDefinitions, settings)))
    IO(Http) ! Http.Bind(mockServer, interface = settings.interface, port = settings.port)
    s.log.info("Mock server running using " + settings)
    s.log.info("Enter to quit")
    Console.readLine()
    system.shutdown()
  }

}

trait MockService {

  /**
   * Predicates wrap together matching functions
   */
  object predicates {

    /**
     * Match result ADT
     */
    sealed trait MatchResult {
      /**
       * An "and" operator
       * @param that the other match result
       * @return this "and" that
       */
      def &&(that: MatchResult): MatchResult

      /**
       * An "and" operator
       * @param that boolean that maps ``false`` to ``Unmatched`` and ``true`` to ``MatchedCompletely``
       * @return this "and" that
       */
      def &&(that: Boolean): MatchResult = if (that) this else Unmatched
    }

    /** Complete match */
    case object MatchedCompletely extends MatchResult {
      def &&(that: MatchResult): MatchResult = that
    }
    /** Partial / start match */
    case object MatchedStart extends MatchResult {
      def &&(that: MatchResult): MatchResult = that
    }
    /** No match */
    case object Unmatched extends MatchResult {
      def &&(that: MatchResult): MatchResult = Unmatched
    }

    /**
     * Adds the ``cata`` function to ``Try[A]``
     * @param t the ``Try[A]``
     * @tparam A the A
     */
    private implicit class RichTry[A](t: Try[A]) {
      /** catamorphism */
      def cata[B](left: => B, right: A => B): B = t match {
        case Success(a) => right(a)
        case Failure(_) => left
      }
    }

    /**
     * Matches the path parameter (in ``segment``) against the given ``value``, with additional information
     * in ``parameters``. For example, the matching will:
     *
     * - MatchedCompletely: {name} against "somesuch" with parameters [name -> String of any length, ...]
     * - MatchedCompletely: {id}   against 445        with parameters [id -> Integer in any range, ...]
     * - Unmatched:         {id}   against "one"      with parameters [id -> Integer in any range, ...]
     *
     * @param segment the segment to be matched, including the '{' and '}'
     * @param value the segment value
     * @param parameters parameters
     * @return
     */
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

    /**
     *
     * @param uri
     * @param r
     * @return
     */
    def uriPathPredicate(uri: Uri, basePath: String, r: Resource): MatchResult = {
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

      val segments = (basePath.split("/") ++ r.getUri.split("/")).filterNot(_.isEmpty).flatMap(Seq("/", _)).toList
      paths(segments, uri.path)
    }

  }

  def mock(ramls: List[Raml], settings: MockSettings, request: HttpRequest)(implicit e: ExecutionContext): Future[HttpResponse] = {
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

    def findResource(basePath: String)(resource: Resource): Option[Resource] = {
      uriPathPredicate(request.uri, basePath, resource) match {
        case Unmatched => None
        case MatchedCompletely => Some(resource)
        case MatchedStart =>
          resource.getResources.values().find(uriPathPredicate(request.uri, basePath, _) == MatchedCompletely).orElse(Some(resource))
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
        resource <- raml.getResources.values().flatMap(findResource(raml.getBasePath)).headOption
        action   <- Option(resource.getAction(request.method.toString()))
        response <- findResponse(action, responseCode)
      } yield mkHttpResponse(response)
    }

    val response = responses match {
      case Nil => HttpResponse(StatusCodes.NotFound)
      case r::Nil => r
      case r::rs => HttpResponse(StatusCodes.MultipleChoices)
    }

    Future {
      Thread.sleep(timeout)
      response
    }
  }

}

class MockServiceActor(definitions: List[Raml], settings: MockSettings) extends Actor with MockService {
  import akka.pattern.pipe
  import context.dispatcher

  def receive: Receive = {
    case Http.Connected(_, _) => sender ! Http.Register(self)
    case r@HttpRequest(_, _, _, _, _) => mock(definitions, settings, r) pipeTo sender()
  }
}