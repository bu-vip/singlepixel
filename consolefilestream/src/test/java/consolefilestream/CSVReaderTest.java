package consolefilestream;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.OutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.roeper.bu.consolefilestream.CSVReader;

@RunWith(MockitoJUnitRunner.class)
public class CSVReaderTest {
  @Mock
  private OutputStream stream;

  private int countLines(String fileName) throws Exception {
    LineNumberReader lnr = new LineNumberReader(new FileReader(new File(fileName)));
    lnr.skip(Long.MAX_VALUE);
    int count = lnr.getLineNumber();
    lnr.close();
    return count;
  }

  @Test
  public void labeledData() throws Exception {
    String fileName = getClass().getClassLoader().getResource("labeled.csv").getFile();
    CSVReader reader = new CSVReader(fileName, true);
    reader.read(stream);

    // Skip first line as labeled
    int lineCount = countLines(fileName) - 1;
    verify(stream, times(lineCount)).write(any(byte[].class));
  }

  @Test
  public void notLabledData() throws Exception {
    String fileName = getClass().getClassLoader().getResource("no_labels.csv").getFile();
    CSVReader reader = new CSVReader(fileName, false);
    reader.read(stream);

    // Skip first line as labeled
    int lineCount = countLines(fileName);
    verify(stream, times(lineCount)).write(any(byte[].class));
  }
}
