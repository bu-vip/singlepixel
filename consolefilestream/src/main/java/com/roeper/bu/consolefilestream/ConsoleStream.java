package com.roeper.bu.consolefilestream;

import com.beust.jcommander.JCommander;

public class ConsoleStream {

  public static void main(String[] args) {
    ConsoleStreamArgs config = new ConsoleStreamArgs();
    JCommander jCommander = new JCommander(config, args);
    jCommander.setProgramName("ConsoleFileStream");
    if (config.isHelp()) {
        jCommander.usage();
        return;
    }

    try {
      switch (config.getFileType()) {
        case CSV:
          CSVReader reader = new CSVReader(config.getFileName(), config.getHasLabels());
          reader.read(System.out);
          break;

        default:
          throw new RuntimeException("Unhandled file type: " + config.getFileType());
      }
    } catch (Exception e) {
      System.out.println("ERROR: " + e.getMessage());
    }
  }
}
