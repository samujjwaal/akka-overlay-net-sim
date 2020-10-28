import java.io.File
import com.typesafe.config.{Config, ConfigFactory}
import org.junit.Assert._
import org.junit.Test

class TestSuite {

  val config: Config = ConfigFactory.parseFile(new File("src/main/resources/application.conf"))

  @Test
  def validateConfig: Unit = {
    assertEquals(config.getString("test"), "test")
  }

}


