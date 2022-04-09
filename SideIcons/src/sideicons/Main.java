package sideicons;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public class Main {

  public static void main(String[] args) throws Exception {
    String tag = "\"image_url\":";
    String pkg = "\"packagename\":";
    String title = "\"name\":";
    String image = "";
    String desc = "";
    FileOutputStream fos = new FileOutputStream("applab.info");
    Scanner sc = new Scanner(new FileInputStream("localdb.json"));
    while (sc.hasNext()) {
        String line = sc.nextLine();
        if (line.contains(title)) {
          int begin = line.indexOf(title) + title.length() + 1;
          desc = line.substring(begin);
          if (desc.length() > 1) {
            desc = desc.substring(0, desc.length() - 2);
          } else {
            desc = "";
          }
      } else if (line.contains(tag)) {
          int begin = line.indexOf(tag) + tag.length() + 1;
          image = line.substring(begin);
          if (image.length() > 1) {
            image = image.substring(0, image.length() - 2);
          } else {
            image = "";
          }
      } else if (line.contains(pkg)) {
        int begin = line.indexOf(pkg) + pkg.length() + 1;
        String name = line.substring(begin);
        name = name.substring(0, name.length() - 2);
        name = name.toLowerCase();
        File file = new File("out", name + ".jpg");
        if (image.length() == 0 || !downloadFile(image, file)) {
          System.out.println(file.getAbsolutePath());
        } else {
          String command = "convert -geometry 450x";
          command += " " + file.getAbsolutePath() + "[0]";
          command += " " + file.getAbsolutePath();
          Runtime.getRuntime().exec(command);
          if (name.startsWith("com.autogen.")) {
            String value = name + " " + desc + "\n";
            fos.write(value.getBytes());
          }
        }
      }
    }
    fos.close();
    sc.close();
  }

  private static boolean downloadFile(String url, File outputFile) {
      try {
          InputStream is = new URL(url).openStream();
          DataInputStream dis = new DataInputStream(is);

          int length;
          byte[] buffer = new byte[65536];
          FileOutputStream fos = new FileOutputStream(outputFile);
          while ((length = dis.read(buffer))>0) {
              fos.write(buffer, 0, length);
          }
          fos.flush();
          fos.close();
          return true;
      } catch (Exception e) {
          return false;
      }
  }
}
