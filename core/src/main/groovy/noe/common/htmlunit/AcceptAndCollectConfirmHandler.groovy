package noe.common.htmlunit

import com.gargoylesoftware.htmlunit.ConfirmHandler
import com.gargoylesoftware.htmlunit.Page

/**
 *
 * @author Aleksandar Kostadinov <akostadi@redhat.com>
 *
 */
public class AcceptAndCollectConfirmHandler implements ConfirmHandler {
  public Collection<String> confirms

  public AcceptAndCollectConfirmHandler(confirms) {
    this.confirms = confirms
  }

  boolean handleConfirm(Page page, String message) {
    confirms.add(message)
    return true
  }
}
