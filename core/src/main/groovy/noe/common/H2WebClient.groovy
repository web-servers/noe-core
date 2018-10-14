package noe.common

import com.gargoylesoftware.htmlunit.HttpMethod
import com.gargoylesoftware.htmlunit.Page
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.WebResponse
import com.gargoylesoftware.htmlunit.WebResponseData
import com.gargoylesoftware.htmlunit.WebWindow
import com.gargoylesoftware.htmlunit.util.Cookie
import com.gargoylesoftware.htmlunit.util.NameValuePair
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.TlsVersion
import org.apache.commons.lang3.StringUtils

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

/**
 *
 * This is a wrapper around OkHttpClient. Its purpose
 * is to mimic Gargoyle software HTMLUnit WebClient
 * so as most of our code doesn't need to be migrated.
 *
 * I haven't found much sense in extending OkHttpClient
 * as the logic of constructing a Gargoyle software HTMLUnit
 * WebClient instance and OkHTTP3 WebClient instances is
 * so much different. Mostly, OkHTTP3 uses a Builder to
 * create an immutable WebClient instance with all settings
 * set, whereas Gargoyle software HTMLUnit WebClient creates
 * a vanilla instance and _then_ configures it as needed.
 *
 * In my opinion, OkHTTP3 WebClient aims at being a "cURL",
 * whereas Gargoyle software HTMLUnit WebClient aims at
 * approaching a human-oriented web browser.
 *
 * @author Michal Karm Babacek <karm@redhat.com>
 *
 */
public class H2WebClient extends WebClient {

  /*
    Undermentioned Section represents an unused path of creating
    a WebClientOptions-like approach. Could be used and expanded
    or removed.

    class Options extends WebClientOptions {
    boolean throwExceptionOnFailingStatusCode = true
    boolean redirectEnabled = true
    boolean printContentOnFailingStatusCode = true
    boolean useInsecureSSL = true
    URL certificateUrl = null
    String certificatePassword = null
    String certificateType
    // TODO: Unused
    String[] sslClientProtocols
    // TODO: Unused
    String[] sslClientCipherSuites

    @Override
    void setSSLClientCertificate(final URL certificateUrl, final String certificatePassword, final String certificateType) {
      this.certificateUrl = certificateUrl
      this.certificatePassword = certificatePassword
      this.certificateType = certificateType
    }
    }
  */

  class Cache extends com.gargoylesoftware.htmlunit.Cache {
    @Override
    void clear() {
      okHttpClient?.cache()?.evictAll()
    }
  }

  class CookieManager extends com.gargoylesoftware.htmlunit.CookieManager {
    @Override
    synchronized void addCookie(Cookie cookie) {
      final HttpCookie httpCookie = new HttpCookie(cookie?.name, cookie?.value)
      httpCookie.setDomain(cookie.domain)
      httpCookie.setHttpOnly(cookie.httpOnly)
      // TODO: This is a highly suspicious conversion
      httpCookie.setMaxAge(cookie.expires.time)
      httpCookie.setPath(cookie.path)
      httpCookie.setSecure(cookie.secure)
      myCookieManager?.cookieStore?.cookies?.add(httpCookie)
    }

    @Override
    synchronized void clearCookies() {
      myCookieManager?.cookieStore?.cookies?.clear()
    }

    @Override
    synchronized Cookie getCookie(String name) {
      final HttpCookie cookie = myCookieManager?.cookieStore?.cookies?.find { it.name?.equals(name) }
      return new Cookie(cookie.domain, cookie.name, cookie.value, cookie.path, new Date(cookie.maxAge), cookie.secure, cookie.httpOnly)
    }

    @Override
    synchronized Set<Cookie> getCookies() {
      final Set<Cookie> cookies = new HashSet<>()
      myCookieManager?.cookieStore?.cookies?.each { cookie ->
        // TODO: This is highly suspicious: new Date(cookie.maxAge)
        cookies.add(new Cookie(cookie.domain, cookie.name, cookie.value, cookie.path, new Date(cookie.maxAge), cookie.secure, cookie.httpOnly))
      }
      return cookies
    }

    @Override
    synchronized boolean isCookiesEnabled() {
      //TODO: If we need it more fine-grained based on URL, it would need reimplementation.
      return myCookieManager.policyCallback.equals(CookiePolicy.ACCEPT_ALL)
    }

    @Override
    synchronized void removeCookie(Cookie cookie) {
      for (int i = 0; i < myCookieManager?.cookieStore?.cookies?.size(); i++) {
        if (myCookieManager.cookieStore.cookies.get(i).getName().equals(cookie.getName())) {
          myCookieManager.cookieStore.cookies.remove(i)
          break
        }
      }
    }

    @Override
    synchronized void setCookiesEnabled(boolean enabled) {
      if (enabled) {
        myCookieManager?.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
      }
    }
  }

  OkHttpClient okHttpClient = null
  String myCurrentJSessionID = null

  final java.net.CookieManager myCookieManager = new java.net.CookieManager()
  final CookieManager cookieManager = new CookieManager()
  // See the beginning of this Class for a comment on WebClientOptions approach.
  //final Options myOptions = new Options()
  final Cache cache = new Cache()
  final HashMap<String, String> requestHeaders = new HashMap<>()

  public H2WebClient() {
    super()
    /*
    We don't create okHttpClient instance just yet, because we need all Options attributes set.
    Users of Gargoyle software HTMLUnit WebClient are used to calling new WebClient() and then
    setting Options attributes, whereas OkHTTP3 WebClient's Builder needs these attributes
    upfront on creation.

    We also don't inherit constructor from the parent class.
    super."${methods[0]}"('mrhaki')
    */
  }

  /*
  See the beginning of this Class for a comment on WebClientOptions approach.
  @Override
  public WebClientOptions getOptions() {
    return myOptions;
  }
  */

  /*
  See the beginning of this Class for a comment on WebClientOptions approach.
  groovy.lang.MissingPropertyException: No such property: certificateUrl for class: com.gargoylesoftware.htmlunit.WebClientOptions
  */

  void addRequestHeader(final String name, final String value) {
    requestHeaders.put(name, value)
  }

  @Override
  Page getPage(final URL url) {
    return new Page() {

      WebResponse webResponse

      @Override
      void initialize() throws IOException {
        // Silence is golden
      }

      @Override
      void cleanUp() {
        // Silence is golden
      }

      @Override
      WebResponse getWebResponse() {

        /*
        How HTMLUnit does it:
        WebResponse webResponse = page.webResponse
        respCode = webResponse.statusCode
        webResponse.getResponseHeaderValue("Set-Cookie")
        webResponse.getContentAsString()
        */

        final String keyStoreBackup = System.getProperty("javax.net.ssl.keyStore")
        final String keyStorePasswordBackup = System.getProperty("javax.net.ssl.keyStorePassword")

        if (options?.getSSLClientCertificateUrl()) {
          System.setProperty("javax.net.ssl.keyStore", options.getSSLClientCertificateUrl().getPath())
          System.setProperty("javax.net.ssl.keyStorePassword", options.getSSLClientCertificatePassword())
          //options.certificateType not used
        }
        try {
          final ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
          /*
          See the beginning of this Class for a comment on WebClientOptions approach.
          TODO: use Options.sslClientProtocols
          */
              .tlsVersions(TlsVersion.TLS_1_2)
              .cipherSuites(
              /*
              As above, TODO: use Options.sslClientCipherSuites
              */
              CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
              CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
              CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
              CipherSuite.SSL_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
              CipherSuite.SSL_DHE_RSA_WITH_AES_128_GCM_SHA256,
              CipherSuite.SSL_DHE_RSA_WITH_AES_128_GCM_SHA256)
              .build()

          final HostnameVerifier fakeHostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
              return true;
            }
          }

          final OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
          if (options?.useInsecureSSL) {
            okHttpClientBuilder.hostnameVerifier(fakeHostnameVerifier)
          }

          okHttpClientBuilder
              .connectionSpecs([spec])
              .cookieJar(new JavaNetCookieJar(myCookieManager))
              .followRedirects(options?.redirectEnabled)
              .followSslRedirects(options?.redirectEnabled)
              .protocols([Protocol.HTTP_2, Protocol.HTTP_1_1])

          okHttpClient = okHttpClientBuilder.build()

          final Request.Builder requestBuilder = new Request.Builder()
              .url(url)
          // TODO: Not used now, might be needed...
          //.method(HttpMethod.GET.toString(), requestBody)
              .header("User-Agent", "NOE Test Suite - OKHttp3")

          requestHeaders.each { k, v ->
            requestBuilder.addHeader(k, v)
          }

          final Request request = requestBuilder.build()

          // Yes, we throw IOExceptions. It is O.K. @see noe.common.utils.VerifyURLBuilder
          final Response response
          try {
            response = okHttpClient.newCall(request).execute()
          } catch (NoSuchMethodError e) {
            throw new IllegalStateException(
                "Handshaker utterly failed. The server probably doesn't run HTTPS nor HTTP/2 at all. " +
                    "Wrong protocol configuration? CHECK ALPN BOOT and your Java SDK version!!!.", e)
          }

          if (!response.isSuccessful() && options?.throwExceptionOnFailingStatusCode) {
            throw new IOException("Unexpected code " + response)
          }
          // TODO: printContentOnFailingStatusCode ?
          /*
          How does HTMLUnit do it?
          public WebResponseData(final byte[] body, final int statusCode, final String statusMessage,
                     final List<NameValuePair> responseHeaders)*/
          final List<NameValuePair> responseHeaders = new ArrayList<>()
          response.headers().toMultimap().each { headerName, headerValuesList ->
            // TODO: Somewhat sketchy, HttpOK3 presents header name as key and numerous header values, Gargoyle HTMLUnit only key:value.
            responseHeaders.add(new NameValuePair(headerName, headerValuesList.join(", ")))
          }

          final WebResponseData webResponseData = new WebResponseData(response.body().bytes(), response.code(), response.message(), responseHeaders)

          //TODO: Hardcoded fake HttpMethod, see .method(HttpMethod above...
          webResponse = new WebResponse(webResponseData, url, HttpMethod.GET, response.receivedResponseAtMillis())
        } finally {
          if (options?.getSSLClientCertificateUrl()) {
            StringUtils.isNotEmpty(keyStoreBackup) ? System.setProperty("javax.net.ssl.keyStore", keyStoreBackup) : System.clearProperty("javax.net.ssl.keyStore")
            StringUtils.isNotEmpty(keyStorePasswordBackup) ? System.setProperty("javax.net.ssl.keyStorePassword", keyStorePasswordBackup) : System.clearProperty("javax.net.ssl.keyStorePassword")
          }
        }

        return webResponse
      }

      @Override
      WebWindow getEnclosingWindow() {
        // Silence is golden
        return null
      }

      @Override
      URL getUrl() {
        return url
      }

      @Override
      boolean isHtmlPage() {
        // Silence is golden
        return false
      }

    }
  }

  def String getMyCurrentJSessionID() {
    final List<HttpCookie> cookies = myCookieManager.getCookieStore().getCookies()
    final int cookieIndex = cookies.indexOf(DefaultProperties.JSESSIONID)
    if (cookieIndex > -1) {
      final HttpCookie cookie = cookies.get(cookieIndex)
      return cookie?.getValue()
    } else {
      return myCurrentJSessionID
    }
  }

}
