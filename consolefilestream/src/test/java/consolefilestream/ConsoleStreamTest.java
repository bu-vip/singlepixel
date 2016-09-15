package consolefilestream;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;
import com.roeper.bu.consolefilestream.ConsoleStream;

public class ConsoleStreamTest {
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

  @Before
  public void setUpStreams() {
      System.setOut(new PrintStream(outContent));
  }

  @After
  public void cleanUpStreams() {
      System.setOut(null);
  }
  
  @Test
  public void csvTest() throws IOException {
    String fileName = getClass().getClassLoader().getResource("no_labels.csv").getFile();
    String[] args = {"--type", "csv", "--file", fileName };
    ConsoleStream.main(args);
    byte[] expected = Files.toByteArray(new File(fileName));
    assertArrayEquals(expected, outContent.toByteArray());
  }
}
