import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
public class ServerListenerThread extends Thread {
    // Each request is separate by a line => use split("\r\n") to split each line
    private static final String HTTP_NEW_LINE_SEPARATOR = "\r\n";
    // Body and Header is separated by an extra line, like
    // Header
    // ____
    // Body
    private static final String HTTP_HEAD_BODY_SEPARATOR = HTTP_NEW_LINE_SEPARATOR + HTTP_NEW_LINE_SEPARATOR;

    // Byte length is 4
    private static final int HTTP_HEAD_BODY_SEPARATOR_BYTES = HTTP_HEAD_BODY_SEPARATOR
            .getBytes().length;
    // private static final int DEFAULT_PACKET_SIZE = 10_000;

    // This is used to determind body length
    private static final String CONTENT_LENGTH_HEADER = "content-length";
    Socket s;

    public ServerListenerThread(Socket connection) throws IOException {
        this.s = connection;
    }

    @Override
    public void run() {
        try {
            // Optional type is used to provide optional values instead of null references
            Optional<HttpReq> optionalRequest = readRequest(s);
            // If the request is empty, then there's something wrong and just break;
            if (optionalRequest.isEmpty()) {
                return;
            }
            // If a value is present
            optionalRequest.ifPresent(request -> {

                try (OutputStream os = s.getOutputStream()) {
                    // Check if method is POST and the url is /submit_form
                    // This is to create a seperate function for this route
                    if (request.method.equals("POST")) {
                        if (request.url.equals("/search_db")) {
                            // Get the value "name" from the body
                            String receivedBody = new String(request.body);
                            String studentName = receivedBody.split("=")[1];
                            System.out.println(studentName);
                            ResultSet rs = null;
                            // SQL Query to Postgres
                            String SQL = "SELECT * FROM\"students\" WHERE name = \'" + studentName + "\'";
                            postgresAdapter adapter = new postgresAdapter();
                            // Connect to postgres
                            Connection conn = adapter.connect();
                            PreparedStatement pstmt = conn.prepareStatement(SQL);
                            // Execute query statement and store it to resultset
                            rs = pstmt.executeQuery();
                            String body = new String("");
                            while (rs.next()) {
                                body = new String("name=" + rs.getString("name") + HTTP_NEW_LINE_SEPARATOR
                                        + "class=" + rs.getString("class") + HTTP_NEW_LINE_SEPARATOR
                                        + "major=" + rs.getString("major") + HTTP_NEW_LINE_SEPARATOR
                                        + "intake=" + rs.getString("intake"));
                            }
                            String statusStr = "";
                            // Check if any student was found
                            if (body.getBytes().length == 0 || body.equals("")) {
                                statusStr = "HTTP/1.1 404 Not Found";
                                body = new String("No data found");
                            } else {
                                statusStr = "HTTP/1.1 200 OK";
                            }
                            String response = "%s\nContent-Length: %d\n\n%s".formatted(statusStr,
                                    body.getBytes().length,
                                    body);
                            // System.out.println(response);
                            os.write(response.getBytes());
                        } else {
                            ServeFile file = new ServeFile("notfound.html");
                            String body = file.strVal();
                            String statusStr = "HTTP/1.1 404 Not Found";
                            String response = "%s\nContent-Length: %d\n\n%s".formatted(statusStr, body.getBytes().length,
                                    body);
                            os.write(response.getBytes());    
                        }
                    } else if (request.method.equals("GET")) {
                        // request.url: /index.html
                        // Need to remove the first character "/"
                        String fileName = request.url.substring(1);
                        // Read file
                        ServeFile file = new ServeFile(fileName);
                        // Covert the ServeFile Object to String
                        String body = file.strVal();
                        int statusCode = file.status;
                        String statusStr = "";
                        // Check if error when read file
                        if (statusCode == 200) {
                            // Found file
                            statusStr = "HTTP/1.1 200 OK";
                        } else if (statusCode == 404) {
                            // Not found file
                            statusStr = "HTTP/1.1 404 Not Found";
                        }
                        // Return response with status code and content-length in header
                        // The Header and body must be separated by an extra line
                        // Hence the \n\n
                        String response = "%s\nContent-Length: %d\n\n%s".formatted(statusStr, body.getBytes().length,
                                body);
                        System.out.println(response);
                        os.write(response.getBytes());

                    } else if (request.method.equals("PUT")) {
                        if (request.url.equals("/submit_form")) {
                            String receivedBody = new String(request.body);
                            String clientName = receivedBody.split("=")[1];
                            try {
                                // Write to the clientName file with the option APPEND
                                // instead of overwriting the entire file
                                // Need to convert to byte[] to write to file
                                Files.write(Paths.get("clientName.txt"), "%s\n".formatted(clientName).getBytes(),
                                        StandardOpenOption.APPEND);
                            } catch (IOException e) {
                            }
                            // Read file
                            ServeFile file = new ServeFile("success.html");
                            String body = file.strVal();
                            int statusCode = file.status;
                            // Check if error when reading file
                            String statusStr = "";
                            if (statusCode == 200) {
                                statusStr = "HTTP/1.1 200 OK";
                            } else if (statusCode == 404) {
                                statusStr = "HTTP/1.1 404 Not Found";
                            }
                            // Return response with status code and content-length in header
                            // The Header and body must be separated by an extra line
                            // Hence the \n\n
                            String response = "%s\nContent-Length: %d\n\n%s".formatted(statusStr,
                                    body.getBytes().length,
                                    body);
                            // Return response (byte[])
                            os.write(response.getBytes());
                        } else {
                            ServeFile file = new ServeFile("notfound.html");
                            String body = file.strVal();
                            String statusStr = "HTTP/1.1 404 Not Found";
                            String response = "%s\nContent-Length: %d\n\n%s".formatted(statusStr, body.getBytes().length,
                                    body);
                            os.write(response.getBytes());    
                        }
                    } else if (request.method.equals("DELETE")) {
                        if (request.url.equals("/delete_student")) {
                            // Get the value "name" from the body
                            String receivedBody = new String(request.body);
                            String studentName = receivedBody.split("=")[1];
                            // SQL Query to Postgres
                            String SQL = "DELETE FROM\"students\" WHERE name = \'" + studentName + "\'";
                            postgresAdapter adapter = new postgresAdapter();
                            // Connect to postgres
                            Connection conn = adapter.connect();
                            PreparedStatement pstmt = conn.prepareStatement(SQL);
                            // Execute delete statement
                            pstmt.executeUpdate();
                            String body = new String("Student deleted successfully");
                            String statusStr = "HTTP/1.1 200 OK";
                            String response = "%s\nContent-Length: %d\n\n%s".formatted(statusStr,
                                    body.getBytes().length,
                                    body);
                            os.write(response.getBytes());
                        } else {
                            // If client request a method that is not supported
                            // Return not found html
                            ServeFile file = new ServeFile("notfound.html");
                            String body = file.strVal();
                            String statusStr = "HTTP/1.1 404 Not Found";
                            String response = "%s\nContent-Length: %d\n\n%s".formatted(statusStr, body.getBytes().length,
                                    body);
                            os.write(response.getBytes());    
                        }

                    }
                     else {
                        // If none of the path is correct => wrong path
                        ServeFile file = new ServeFile("notfound.html");
                        String body = file.strVal();
                        String statusStr = "HTTP/1.1 404 Not Found";
                        String response = "%s\nContent-Length: %d\n\n%s".formatted(statusStr, body.getBytes().length,
                                body);
                        os.write(response.getBytes());
                    }
                } catch (Exception e) {
                }
                // Uncomment this line to view the entire request
                printRequest(request);
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Optional<HttpReq> readRequest(Socket connection) throws IOException, Exception {
        InputStream stream = connection.getInputStream();
        byte[] rawRequestHead = readRawRequestHead(stream);
        if (rawRequestHead.length == 0) {
            return Optional.empty();
        }

        // requestHead should have the structure like this
        // GET /index.html HTTP/1.1
        // Host: localhost:8080
        // Connection: keep-alive
        // Cache-Control: max-age=0
        String requestHead = new String(rawRequestHead);
        // System.out.println(requestHead);
        // So need to use "\r\n" to split the line
        String[] lines = requestHead.split(HTTP_NEW_LINE_SEPARATOR);

        // The first line is the method with URL
        String line = lines[0];
        String[] methodUrl = line.split(" ");
        String method = methodUrl[0];
        String url = methodUrl[1];

        // Pass all request as parameter
        var headers = readHeaders(lines);

        // Get bodylength from content-type header
        int bodyLength = getExpectedBodyLength(headers);
        byte[] body = new byte[0];
        // Check if there's a body
        if (bodyLength > 0) {
            // get start index of body by find the index of HTTP_HEAD_BODY_SEPARATOR
            // because the header and body is separated by
            int bodyStartIndex = requestHead.indexOf(HTTP_HEAD_BODY_SEPARATOR);

            if (bodyStartIndex > 0) {
                // Copy the array of request from the beginning of the body to end of body
                byte[] readBody = Arrays.copyOfRange(rawRequestHead, bodyStartIndex + HTTP_HEAD_BODY_SEPARATOR_BYTES,
                        rawRequestHead.length);
                body = readBody;
            } else {
                body = new byte[0];
            }
        } else {
            body = new byte[0];
        }

        // need Optional.of because this is optional type
        return Optional.of(new HttpReq(method, url, headers, body));
    }

    // Read the entirely of the request
    private static byte[] readRawRequestHead(InputStream stream) throws Exception {
        // Check number of byte to read
        int toRead = stream.available();
        byte[] buffer = new byte[toRead];
        // Read and store in a buffer
        stream.read(buffer);
        return buffer;
    }

    // Function to print method, url, header and body
    private static void printRequest(HttpReq req) {
        System.out.println("Method: " + req.method);
        System.out.println("Url: " + req.url);
        req.headers.forEach((k, v) -> {
            System.out.println("Header: %s - %s".formatted(k, v));
        });
        System.out.println("Body: ");
        if (req.body.length > 0) {
            System.out.println(new String(req.body));
        } else {
            System.out.println("Body is empty");
        }
    }

    // This function read headers of the request
    private static Map<String, List<String>> readHeaders(String[] lines) throws Exception {
        HashMap<String, List<String>> headers = new HashMap<>();
        // The first line is already read, so i starts at 1, not 0
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            // If line is empty, meaning that it has reach end of the request
            // or read the body of the request
            if (line.isEmpty()) {
                break;
            }

            String[] keyValue = line.split(":", 2);
            String key = keyValue[0].toLowerCase().strip();
            String value = keyValue[1].strip();
            // computeIfAbsent Take 2 parameter, key and function,
            // used to add a key into the hashMap header if the key does not exist already
            // use function to create value with input key if key not exists.
            headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return headers;
    }

    // This function use the header "Content-length" to get the body length
    // If no content-length => return 0
    private static int getExpectedBodyLength(Map<String, List<String>> headers) {
        try {
            return Integer.parseInt(headers.getOrDefault(CONTENT_LENGTH_HEADER, List.of("0")).get(0));
        } catch (Exception ignored) {
            return 0;
        }
    }

    // The keyword "record" is used to create an immutable (constant) class to ONLY
    // hold data
    // This case it hold a http request
    private record HttpReq(String method, String url, Map<String, List<String>> headers, byte[] body) {
    }
}
