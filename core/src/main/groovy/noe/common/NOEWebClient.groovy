package noe.common

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.util.Cookie

/**
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 * "OMG, why?!" The purpose of this class is to encapsulate a property
 *  that the parenting WebClient does not have, it's the JSESSIONID passed
 *  by URL, in case the COOKIE session tracking is disabled on Client.
 *
 *  Note that for sake of sanity, we are not switching COOKIE session tracking
 *  off on the server side, so one is still able to retrieve it from the response
 *  header. We however, in some tests, use a client that does not support cookies,
 *  which is a likely-to-happen scenario. If you are under an impression
 *  that this class is a bullshit, ping me :-)
 */
class NOEWebClient extends WebClient {

  String myCurrentJSessionID = null

  NOEWebClient() {
    super()
  }

  NOEWebClient(BrowserVersion browserVersion) {
    super(browserVersion)
  }

  NOEWebClient(BrowserVersion browserVersion, String proxyHost, int proxyPort) {
    super(browserVersion, proxyHost, proxyPort)
  }

  String getMyCurrentJSessionID() {
    com.gargoylesoftware.htmlunit.CookieManager cookieManager = this.getCookieManager()
    if (cookieManager.isCookiesEnabled()) {
      Cookie cookie = cookieManager.getCookie(DefaultProperties.JSESSIONID)
      if (cookie != null) {
        return cookie.getValue()
      } else {
        return myCurrentJSessionID
      }
    }
    return myCurrentJSessionID
  }
}
