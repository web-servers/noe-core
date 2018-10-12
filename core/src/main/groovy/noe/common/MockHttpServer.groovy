package noe.common

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import groovy.util.logging.Slf4j
/**
 * MockHttpServer to be used either as a standalone server
 * or as an embedded one.
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@Slf4j
class MockHttpServer implements HttpHandler {
  static final UTF_8 = "UTF-8"
  HttpServer server
  static MockHttpServer mockHttpServer
  Closure handleParams
  static String currentSTATUSmessage = null

  @Override
  void handle(HttpExchange httpExchange) throws IOException {
    try {
      def method = httpExchange.getRequestMethod()
      InputStream bodyIn = httpExchange.getRequestBody()
      StringBuilder body = new StringBuilder()
      bodyIn.eachLine(UTF_8) { line ->
        body.append(line)
      }
      def uri = httpExchange.getRequestURI()
      def query = uri.getRawQuery()
      def path = uri.getRawPath()
      def params = parseQuery(query)
      if (method == "STATUS") {
        currentSTATUSmessage = body
      }
      showInfo(
          method: method, body: body.toString(), uri: uri, protocol: httpExchange.getProtocol(),
          query: query, path: path, params: params)
      if (handleParams) {
        handleParams(this, httpExchange, params)
      } else {
        sendString(httpExchange, ":-)")
      }
    } catch (Exception ex) {
      log.error("Handling HttpExchange failed", ex)
      stopServer()
      throw ex
    }
  }

  void createHTTPServer(String address, int port) {
    server = HttpServer.create(new InetSocketAddress(address, port), 0)
    server.createContext("/", this)
  }

  void showInfo(Map info) {
    StringBuilder sb = new StringBuilder("")
    info.each { k, v ->
      sb.append(k + (v ? ': [' + v + ']' : ": []")).append(DefaultProperties.NL)
    }
    log.info('Received request:\n{}', sb.toString())
  }

  static sendString(httpExchange, answer) throws IOException {
    httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, answer.length())
    OutputStream os = httpExchange.getResponseBody()
    os.write(answer.getBytes())
    os.close()
  }

  void start() {
    server.start()
  }

  void stopServer() {
    log.info("Stopping MockHttpServer")
    ((HttpServer) server).stop(3)
  }

  static Map parseQuery(final String query) {
    Map parameters = new HashMap()
    if (!query || query.length() == 0) {
      return parameters
    }
    def key, value
    for (String parameter : query.split("&")) {
      String[] pair = parameter.split("=")
      if (pair.length > 0) {
        key = URLDecoder.decode(pair[0], UTF_8)
      }
      value = ""
      if (pair.length > 1) {
        value = URLDecoder.decode(pair[1], UTF_8)
      }
      List<String> values = parameters.get(key)
      if (values == null) {
        values = new ArrayList<String>()
        parameters.put(key, values)
      }
      values.add(!value ? "" : value)
    }
    return parameters
  }
}
