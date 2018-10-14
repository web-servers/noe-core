package noe.common.utils

import org.junit.Assert
import org.junit.Test
/**
 * @author Pavel Reichl <preichl@redhat.com>
 */
class PathHelperTest {

  static Platform platform = new Platform()

  @Test
  void AbsolutePathTest() {
    if (platform.isWindows()) {
      Assert.assertTrue(PathHelper.isAbsolute('c:\\a\\b\\c'))
      Assert.assertFalse(PathHelper.isAbsolute('a\\b\\c'))
    } else {
      Assert.assertTrue(PathHelper.isAbsolute('/a/b/c'))
      Assert.assertFalse(PathHelper.isAbsolute('a/b/c'))
    }
  }

  @Test
  void JoinTest() {
    // Platform independent cases
    Assert.assertEquals('', PathHelper.join('',))
    Assert.assertEquals('', PathHelper.join('', '', ''))
    Assert.assertEquals('foo', PathHelper.join('foo'))

    if (platform.isWindows()) {
      Assert.assertEquals('c:\\', PathHelper.join('', 'c:\\', ''))
      Assert.assertEquals('c:\\foo', PathHelper.join('c:\\foo'))
      Assert.assertEquals("a\\b\\c", PathHelper.join('a', 'b', 'c'))
      Assert.assertEquals("a\\b\\c", PathHelper.join('a\\', 'b\\', 'c'))
      Assert.assertEquals("a\\b\\c\\", PathHelper.join('a', 'b', 'c\\'))
      Assert.assertEquals("c:\\a\\b\\c", PathHelper.join('c:\\a', 'b', 'c'))
      Assert.assertEquals("d:\\c", PathHelper.join('c:\\a', 'b', 'd:\\c'))
    } else {
      Assert.assertEquals('/', PathHelper.join('', '/', ''))
      Assert.assertEquals('/foo', PathHelper.join('/foo'))
      Assert.assertEquals("a/b/c", PathHelper.join('a', 'b', 'c'))
      Assert.assertEquals("a/b/c", PathHelper.join('a/', 'b/', 'c'))
      Assert.assertEquals("a/b/c/", PathHelper.join('a', 'b', 'c/'))
      Assert.assertEquals("/a/b/c", PathHelper.join('/a', 'b', 'c'))
      Assert.assertEquals("/a/b/c", PathHelper.join('/a', '', 'b', 'c'))
      Assert.assertEquals("/a/b/c/", PathHelper.join('/a', 'b', 'c', ''))
      Assert.assertEquals("/c", PathHelper.join('/a', 'b', '/c'))
    }
  }

  @Test
  void AddSuffixTest() {
    if (platform.isWindows()) {
      Assert.assertEquals('run', PathHelper.join('run'))
      Assert.assertEquals('run', PathHelper.join(false, 'run'))
      Assert.assertEquals('run.bat', PathHelper.join(true, 'run'))

      Assert.assertEquals('A\\B\\run', PathHelper.join('A', 'B', 'run'))
      Assert.assertEquals('A\\B\\run', PathHelper.join(false, 'A', 'B', 'run'))
      Assert.assertEquals('A\\B\\run.bat', PathHelper.join(true, 'A', 'B', 'run'))
    } else {
      Assert.assertEquals('run', PathHelper.join('run'))
      Assert.assertEquals('run', PathHelper.join(false, 'run'))
      Assert.assertEquals('run.sh', PathHelper.join(true, 'run'))

      Assert.assertEquals('A/B/run', PathHelper.join('A', 'B', 'run'))
      Assert.assertEquals('A/B/run', PathHelper.join(false, 'A', 'B', 'run'))
      Assert.assertEquals('A/B/run.sh', PathHelper.join(true, 'A', 'B', 'run'))
    }
  }
}
