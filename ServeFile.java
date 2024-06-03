import java.io.*;

// This class reads the content of the a file and store it in a StringBuffer
// The StringBuffer can be converted into a String type 
public class ServeFile {
  StringBuffer sb = new StringBuffer();
  BufferedReader br = null;
  int status = 0;

  public ServeFile(String fileName) throws IOException {
    try {
      // try to read file in public folder and return 200 (success) status
      br = new BufferedReader(new FileReader("public/" + fileName));
      status = 200;
    } catch (FileNotFoundException e) {
      // Return the notfound file if there's no file with such name
      // with 404 (not found) status
      br = new BufferedReader(new FileReader("public/notfound.html"));
      status = 404;
    }
    while (true) {
      // Read and add each line of the file until there's a null line (no more lines)
      String line = br.readLine();
      if (line == null) {
        break;
      }
      sb.append(line).append("\n");
    }
  }

  // Convert StringBuffer to String type
  public String strVal() {
    return sb.toString();
  }

  // Literally in the name
  public int getStatusCode() {
    return status;
  }
}
