package noe.byteman

import noe.common.utils.PathHelper
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BytemanTest {

  String defaultJavaagentProperty
  String expectedDefaultJarPath
  String defaultListeningProperties
  String defaultJavaagentPrefix
  List<File> scriptPathList
  List<File> jarPathList
  File testingDir

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder()

  @Before
  void beforeTest() {
    testingDir = testFolder.newFolder("testingByteman")
    expectedDefaultJarPath = PathHelper.join(System.getProperty("java.io.tmpdir"), "noe", "byteman", Byteman.BYTEMAN_JAR_NAME)
    defaultListeningProperties = ",listener:true,port:9091"
    defaultJavaagentPrefix = "-javaagent:" + expectedDefaultJarPath + "=boot:" + expectedDefaultJarPath
    defaultJavaagentProperty = defaultJavaagentPrefix + defaultListeningProperties
    scriptPathList = [testFolder.newFile("first.btm")]
    jarPathList = [testFolder.newFile("first.jar")]
  }

  @Test
  void getDefaultBytemanJarPathTest() {
    String actualResult = new Byteman()
        .generateBytemanJavaOpts()
    Assert.assertEquals(defaultJavaagentProperty, actualResult)
  }

  @Test
  void getModyfiedEnvBytemanJarPathTest() {
    File modifiedBytemanJarPath = testFolder.newFile("byteman.jar")
    String actualResult = new Byteman(modifiedBytemanJarPath)
        .generateBytemanJavaOpts()

    Assert.assertEquals("-javaagent:" + modifiedBytemanJarPath + "=boot:" + modifiedBytemanJarPath +
        defaultListeningProperties, actualResult)
  }

  @Test(expected = FileNotFoundException.class)
  void getNotExistingModyfiedEnvBytemanJarPathTest() {
    File modifiedBytemanJarPath = new File("non_existing_byteman.jar")

    new Byteman(modifiedBytemanJarPath)
  }

  @Test
  void generateJavaAgentPropertyWithoutListenerTest() {
    String expectedJavaAgentProperty = "-javaagent:" + expectedDefaultJarPath + "=boot:" + expectedDefaultJarPath
    String actualResult = new Byteman()
        .listener(false)
        .generateBytemanJavaOpts()

    Assert.assertEquals(expectedJavaAgentProperty, actualResult)
  }

  @Test
  void generateJavaAgentPropertyWithoutListenerWithPortTest() {
    String actualResult = new Byteman()
        .listener(false)
        .port(9091)
        .generateBytemanJavaOpts()

    Assert.assertEquals(defaultJavaagentPrefix, actualResult)
  }

  @Test
  void generateJavaAgentPropertyWithPropTest() {
    String expectedJavaAgentProperty = defaultJavaagentProperty + ",prop:key1=value1,prop:key2=value2"
    Map<String, String> propValues = ["key1": "value1", "key2": "value2"]
    String actualResult = new Byteman()
        .prop(propValues)
        .generateBytemanJavaOpts()

    Assert.assertEquals(expectedJavaAgentProperty, actualResult)
  }

  @Test
  void generateJavaAgentPropertyWithPolicyTest() {
    String expectedJavaAgentProperty = defaultJavaagentProperty + ",policy:true," +
        "script:" + scriptPathList.first().getAbsolutePath()

    String actualResult = new Byteman()
        .script(scriptPathList)
        .policy(true)
        .generateBytemanJavaOpts()

    Assert.assertEquals(expectedJavaAgentProperty, actualResult)
  }

  @Test
  void generateJavaAgentPropertyWithProperPortTest() {
    String expectedJavaAgentProperty = defaultJavaagentPrefix + ",listener:true,port:1000," +
        "script:" + scriptPathList.first().getAbsolutePath()

    String actualResult = new Byteman()
        .script(scriptPathList)
        .port(1000)
        .generateBytemanJavaOpts()

    Assert.assertEquals(expectedJavaAgentProperty, actualResult)
  }

  @Test(expected = IllegalArgumentException.class)
  void generateJavaAgentPropertyWithWrongPortTest() {
    new Byteman()
        .port(-1000)
  }

  @Test
  void generateJavaAgentPropertyWithBootJarTest() {
    String expectedJavaAgentProperty = defaultJavaagentProperty + ",boot:" + jarPathList.first().getAbsolutePath()
    String actualResult = new Byteman()
        .boot(jarPathList)
        .generateBytemanJavaOpts()

    Assert.assertEquals(expectedJavaAgentProperty, actualResult)
  }

  @Test
  void generateJavaAgentPropertyWithSingleScriptTest() {
    String expectedJavaAgentProperty = defaultJavaagentProperty + ",script:" + scriptPathList.first().getAbsolutePath()
    String actualResult = new Byteman()
        .script([scriptPathList.first()])
        .generateBytemanJavaOpts()

    Assert.assertEquals(expectedJavaAgentProperty, actualResult)
  }

  @Test
  void generateJavaAgentPropertyWithMultipleScriptsTest() {
    List<File> scriptPathLinkedList = scriptPathList
    scriptPathLinkedList.add(new File("second.btm"))
    String expectedJavaAgentProperty = defaultJavaagentProperty + ",script:" + scriptPathLinkedList.first().getAbsolutePath() +
        ",script:" + scriptPathLinkedList.last().getAbsolutePath()

    String actualResult = new Byteman()
        .script(scriptPathLinkedList)
        .generateBytemanJavaOpts()

    Assert.assertEquals(expectedJavaAgentProperty, actualResult)
  }

  @Test
  void generateJavaAgentPropertyWithSingleJarPathTest() {
    List<File> scriptPathLinkedList = scriptPathList
    scriptPathLinkedList.add(new File("second.btm"))
    String expectedJavaAgentProperty = defaultJavaagentProperty + ",script:" + scriptPathLinkedList.first().getAbsolutePath() +
        ",script:" + scriptPathLinkedList.last().getAbsolutePath() + ",sys:" + expectedDefaultJarPath

    String actualResult = new Byteman()
        .script(scriptPathLinkedList)
        .sys([new File(expectedDefaultJarPath)])
        .generateBytemanJavaOpts()

    Assert.assertEquals(expectedJavaAgentProperty, actualResult)
  }

  @Test
  void generateJavaAgentPropertyWithMultipleJarPathTest() {
    List<File> scriptPathLinkedList = scriptPathList
    scriptPathLinkedList.add(new File("second.btm"))
    String expectedJavaAgentProperty = defaultJavaagentProperty + ",script:" + scriptPathLinkedList.first().getAbsolutePath() +
        ",script:" + scriptPathLinkedList.last().getAbsolutePath() + ",sys:" + expectedDefaultJarPath +
        ",sys:" + expectedDefaultJarPath

    String actualResult = new Byteman()
        .script(scriptPathLinkedList)
        .sys([new File(expectedDefaultJarPath), new File(expectedDefaultJarPath)])
        .generateBytemanJavaOpts()

    Assert.assertEquals(expectedJavaAgentProperty, actualResult)
  }
}
