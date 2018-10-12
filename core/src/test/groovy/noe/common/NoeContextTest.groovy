package noe.common

import org.junit.Assert
import org.junit.Test


class NoeContextTest {

  @Test
  void singleContextTest() {
    Assert.assertTrue(NoeContext.forContext("xxx_yyy").consistsOf(['xxx_yyy']))
    Assert.assertFalse(NoeContext.forContext("xxx_yyy").consistsOf(['xxx']))
  }

  @Test
  void singleGroupMatchingTest() {
    Assert.assertTrue(NoeContext.forContext("xxx+aaa,yyy,zzz-bbb").areInSingleGroup(['aaa', 'xxx']))
    Assert.assertTrue(NoeContext.forContext("xxx+aaa,yyy,zzz-bbb").areInSingleGroup(['yyy']))
    Assert.assertTrue(NoeContext.forContext("xxx+aaa,yyy,zzz-bbb").areInSingleGroup(['zzz']))
    Assert.assertFalse(NoeContext.forContext("xxx+aaa,yyy,zzz-bbb").areInSingleGroup(['aaa', 'bbb']))
    Assert.assertFalse(NoeContext.forContext("xxx+aaa,yyy,zzz-bbb").areInSingleGroup(['ccc']))
  }

  @Test
  void noGroupsMatchingTest() {
    Assert.assertTrue(NoeContext.forContext("xxx+aaa+rpm,yyy,zzz-bbb").consistsOf(['aaa', 'xxx', 'rpm']))
    Assert.assertTrue(NoeContext.forContext("xxx+aaa+rpm,yyy,zzz-bbb").consistsOf(['aaa', 'yyy', 'rpm']))
    Assert.assertFalse(NoeContext.forContext("xxx+aaa+rpm,yyy,zzz-bbb").consistsOf(['aaa', 'ccc', 'rpm']))
    Assert.assertTrue(NoeContext.forContext("xxx+aaa+rpm,yyy,zzz-bbb").consistsOfAny(['aaa', 'xxx', 'rpm']))
    Assert.assertTrue(NoeContext.forContext("xxx+aaa+rpm,yyy,zzz-bbb").consistsOfAny(['aaa', 'ccc', 'rpm']))
    Assert.assertTrue(NoeContext.forContext("xxx+aaa+rpm,yyy,zzz-bbb").consistsOfAny(['aaa']))
    Assert.assertTrue(NoeContext.forContext("xxx+aaa+rpm,yyy,zzz-bbb").consistsOfAny(['yyy']))
    Assert.assertFalse(NoeContext.forContext("xxx+aaa+rpm,yyy,zzz-bbb").consistsOfAny(['ccc']))
  }

  @Test
  void currentContextTest() {
    String origContext = System.getProperty('context')
    try {
      System.setProperty('context', "eap+ews+rpm")
      def context = NoeContext.forCurrentContext()
      Assert.assertTrue(context.areInSingleGroup(['ews', 'rpm']))
    } finally {
      if (origContext != null) {
        System.setProperty('context', origContext)
      } else {
        System.clearProperty('context')
      }
    }
  }
}
