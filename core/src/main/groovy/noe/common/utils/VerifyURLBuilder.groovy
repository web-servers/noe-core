package noe.common.utils

import com.gargoylesoftware.htmlunit.Page
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.WebResponse
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils

import java.util.regex.Pattern

/**
 * @author Michal Karm Babacek <karm@redhat.com>
 */
@Slf4j
public final class VerifyURLBuilder {
  private URL url = null
  private int code = 200
  private List<Integer> allowedCodes = null
  private List<Integer> notAllowedCodes = null
  private String content = ""
  private long timeout = 30000
  private boolean allowRedirects = true
  private boolean setReqProp = false
  private String reqKey = ""
  private String reqValue = ""
  private WebClient webClient = null
  private boolean clearWebClientCache = false
  private boolean reThrowAnyException = false
  private boolean contentAsRegex = false
  private boolean useFindInsteadOfMatch = false
  private boolean swallowIOExceptions = false
  private boolean tryOnlyOnce = false
  private boolean logResponse = true
  private boolean loggingOn = true
  private Integer webConnectionTimeout = null

  private VerifyURLBuilder() {}

  /**
   * MANDATORY
   * The URL to be accessed.
   */
  VerifyURLBuilder url(URL url) {
    this.url = url
    return this
  }

  /**
   * Expected return code.
   */
  VerifyURLBuilder code(int code) {
    this.code = code
    return this
  }

  /**
   *  Not expected return code.
   */
  VerifyURLBuilder notAllowedCodes(List<Integer> httpResponseCodes) {
    this.notAllowedCodes = httpResponseCodes
    return this
  }

  /**
   *  Expected response codes.
   */
  VerifyURLBuilder allowedCodes(List<Integer> httpResponseCodes) {
    this.allowedCodes = httpResponseCodes
    return this
  }

  /**
   * Defines timeout used for single connection, the timeout is in milliseconds.
   * 0 means indefinite timeout.
   * The timeout value is in the essence defined twice, once for connection establishment and second for retrieving the data.
   */
  VerifyURLBuilder webConnectionTimeout(int timeout) {
    this.webConnectionTimeout = webConnectionTimeout
    return this
  }

  /**
   * Expected content.
   */
  VerifyURLBuilder content(String content) {
    this.content = content
    return this
  }

  /**
   * 1 trial = wait for 1000 milliseconds
   */
  VerifyURLBuilder timeout(long timeout) {
    this.timeout = timeout
    return this
  }

  /**
   * Allow the client to be redirected?
   */
  VerifyURLBuilder allowRedirects(boolean allowRedirects) {
    this.allowRedirects = allowRedirects
    return this
  }

  /**
   * Request property to be set
   */
  VerifyURLBuilder setReqProp(boolean setReqProp) {
    this.setReqProp = setReqProp
    return this
  }

  /**
   * Remove all logging? Logging is put on debug level mostly.
   */
  VerifyURLBuilder loggingOn(boolean loggingOn) {
    this.loggingOn = loggingOn
    return this
  }

  /**
   * Request header key
   */
  VerifyURLBuilder reqKey(String reqKey) {
    this.reqKey = reqKey
    return this
  }

  /**
   * Request header value
   */
  VerifyURLBuilder reqValue(String reqValue) {
    this.reqValue = reqValue
    return this
  }

  /**
   * Your own WebClient instance
   */
  VerifyURLBuilder webClient(WebClient webClient) {
    this.webClient = webClient
    return this
  }

  /**
   * Explicitly clear WebClient's cache?
   */
  VerifyURLBuilder clearWebClientCache(boolean clearWebClientCache) {
    this.clearWebClientCache = clearWebClientCache
    return this
  }

  /**
   * Should we re-throw caught web client exceptions?
   */
  VerifyURLBuilder reThrowAnyException(boolean reThrowAnyException) {
    this.reThrowAnyException = reThrowAnyException
    return this
  }

  /**
   * Should we treat the content attribute as a regular expression?
   */
  VerifyURLBuilder contentAsRegex(boolean contentAsRegex) {
    this.contentAsRegex = contentAsRegex
    return this
  }

  /**
   * Use mather's find instead of match method.
   */
  VerifyURLBuilder useFindInsteadOfMatch(boolean useFindInsteadOfMatch) {
    this.useFindInsteadOfMatch = useFindInsteadOfMatch
    return this
  }

  /**
   * Should we silently swallow IOExceptions whrown by the web client network stack?
   */
  VerifyURLBuilder swallowIOExceptions(boolean swallowIOExceptions) {
    this.swallowIOExceptions = swallowIOExceptions
    return this
  }

  /**
   * Overrides all timeout settings - the web client asks only once and returns
   * either true or false.
   */
  VerifyURLBuilder tryOnlyOnce(boolean tryOnlyOnce) {
    this.tryOnlyOnce = tryOnlyOnce
    return this
  }

  /**
   * Whether response should be logged on response, by default it is logged on trace level.
   */
  VerifyURLBuilder logResponse(boolean logResponse) {
    this.logResponse = this.logResponse
    return this
  }

  /**
   * Verify page
   * @param block used to build VerifyUrlBuilder
   * @return true if code and content was found
   */
  public static boolean verifyURL(block) {
    final VerifyURLBuilder verifyURLBuilder = new VerifyURLBuilder().with block
    Map result = verifyURLBuilder.build()
    return (result.codeOk && result.contentOk)
  }

  /**
   * Get page content
   * @param block used to build VerifyUrlBuilder
   * @return Page response body as String, null if page verification failed(return code is not OK)
   */
  public static String getContent(block) {
    final VerifyURLBuilder verifyURLBuilder = new VerifyURLBuilder().with block
    Map result = verifyURLBuilder.build()
    return result.codeOk ? result.response : null
  }

  /**
   * Get whole response map
   * @param block used to build VerifyUrlBuilder
   * @return Map containing response details
   */
  public static Map getResponseMap(block) {
    final VerifyURLBuilder verifyURLBuilder = new VerifyURLBuilder().with block
    Map result = verifyURLBuilder.build()
    return result.codeOk ? result : null
  }

  /**
   * Get page information as a Map in format
   * contentOk - content was found
   * codeOk - response code is equal given code
   * response - response body
   * headers -  response headers
   * @return [contentOk:boolean, codeOk:boolean, response:String, headers:List<NameValuePair>]
   */
  private Map build() {
    if (url == null) throw new IllegalArgumentException("URL must not be null.")
    if (StringUtils.isBlank(reqKey) != StringUtils.isBlank(reqValue)) {
      throw new IllegalArgumentException("ReqKey and ReqValue have to have to be either both set or both empty.")
    }

    def codeOK = false
    def contentOK = false
    String response = null
    WebResponse webResponse = null
    def respCode
    if (webClient == null) {
      webClient = new WebClient()
    }
    // mbabacek: We need 404, 403 and others as well :-)
    webClient.getOptions().setThrowExceptionOnFailingStatusCode(false)
    webClient.getOptions().setRedirectEnabled(allowRedirects)
    if (webConnectionTimeout != null) {
      webClient.getOptions().setTimeout(webConnectionTimeout)
    }
    if (loggingOn) {
      log.debug("Verifying URL: ${url.toString()} for response code ${code} and content to: " +
          ((contentAsRegex) ? "match regexp " : "contain ") + "\"${content}\"")
    }
    try {
      while ((!codeOK || !contentOK) && (timeout > 0)) {
        timeout += new Date().getTime() // for exact, real time measurement
        contentOK = !content
        if (setReqProp) {
          webClient.addRequestHeader(reqKey, reqValue)
        }
        if (clearWebClientCache) {
          webClient.cache.clear()
        }
        Page page
        if (swallowIOExceptions) {
          try {
            page = webClient.getPage(url)
          } catch (IOException ex) {
            if (loggingOn) {
              log.error("Verify URL connection error, exception detected", ex)
            }
            Library.letsSleep(1000)
            timeout -= new Date().getTime()
            continue
          }
        } else {
          page = webClient.getPage(url)
        }

        webResponse = page.webResponse
        respCode = webResponse.statusCode

        if (allowedCodes) {
          codeOK = allowedCodes.contains(respCode)
        } else {
          codeOK = respCode == (code == -1 ? 200 : code)
        }

        if (!webClient.getCookieManager().isCookiesEnabled()) {
          def newJsessionId = webResponse.getResponseHeaderValue("Set-Cookie")
          if (loggingOn) {
            log.debug("Cookies disabled... newJsessionId:${newJsessionId}")
          }
          try {
            webClient.setMyCurrentJSessionID((newJsessionId =~ /^JSESSIONID=([^;]*);.*/)[0][1])
          } catch (IndexOutOfBoundsException ex) {
            if (loggingOn) {
              log.warn("I can't parse newJsessionId from this: {}", newJsessionId)
            }
          }
        }

        response = webResponse.getContentAsString()
        if (codeOK && content) {
          if (logResponse) {
            log.trace("RESPONSE: ${response}")
          }
          if (contentAsRegex) {
            Pattern regexPattern = Pattern.compile(content, Pattern.DOTALL)
            // "matches" returns true if the whole string matches the given pattern. "find" tries to find a substring that matches the pattern
            boolean regExMatchOrFound = (useFindInsteadOfMatch) ? regexPattern.matcher(response).find() : regexPattern.matcher(response).matches()
            contentOK = ((code != -1) && regExMatchOrFound) || ((code == -1) && (!regExMatchOrFound))
          } else {
            contentOK = ((code != -1) && (response.indexOf(content) >= 0)) || ((code == -1) && (response.indexOf(content) == -1))
          }
        } else {
          if (logResponse) {
            log.trace("RESPONSE: ${response}")
          }
        }
        if (loggingOn) {
          log.debug("Checking for ${url} response code: was ${respCode}, expected " +
              "${(allowedCodes) ? allowedCodes : code}")
        }
        if (codeOK && content) {
          if (loggingOn) {
            log.debug("Content \"${content}\" was " + ((contentOK && (code != -1)) ? "found" : "not found"))
          }
        }

        if (codeOK && contentOK) {
          break
        }
        if (tryOnlyOnce) {
          break
        }
        if (notAllowedCodes && notAllowedCodes.contains(respCode)) {
          if (loggingOn) {
            log.debug("Shouldn't get ${respCode} but got it," +
                " list of not allowed codes => ${notAllowedCodes}")
          }
          codeOK = false
          break
        }

        Library.letsSleep(1000)
        timeout -= new Date().getTime() // for exact, real time measurement
      }
    } catch (e) {
      contentOK = false
      log.error("verifyUrl() - problem with handling response text, exception detected", e)
      if (reThrowAnyException) {
        throw e
      }
    }
    return [contentOk: contentOK, codeOk: codeOK, response: response, headers: (webResponse == null) ? null : webResponse.responseHeaders]
  }
}
