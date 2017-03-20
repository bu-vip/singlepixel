package edu.bu.vip.singlepixel.multikinect;

import edu.bu.vip.multikinect.controller.Controller;
import edu.bu.vip.multikinect.controller.webconsole.WebConsole;
import java.util.Scanner;

public class Main {

  public static void main(String[] args) throws Exception {
    Controller controller = new Controller("/home/doug/Desktop/multikinect");
    WebConsole webConsole = new WebConsole(controller);

    SinglePixelSensorPlugin sensorPlugin = new SinglePixelSensorPlugin("", "tcp://localhost:1883");
    controller.registerPlugin("singlepixel", sensorPlugin);

    // Must start the controller before the web console
    controller.start();
    webConsole.start();

    System.out.println("Press enter to stop");
    Scanner scanner = new Scanner(System.in);
    scanner.nextLine();
    scanner.close();

    webConsole.stop();
    controller.stop();
  }
}
