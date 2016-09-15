package com.roeper.bu.consolefilestream;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

public class CSVReader {

  private final String fileName;
  private final boolean hasLabels;
  
  public CSVReader(String fileName, boolean hasLabels) {
    this.fileName = fileName;
    this.hasLabels = hasLabels;
  }
  
  public void read(OutputStream output) throws IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(this.fileName))) {
      // Skip labeled line
      if (this.hasLabels) {
        br.readLine();
      }

      // Print lines
      for (String line; (line = br.readLine()) != null;) {
        output.write(line.getBytes());
        output.write('\n');
      }
    }
  }
}
