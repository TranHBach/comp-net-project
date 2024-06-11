import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class httpClient extends JFrame {
    private JComboBox<String> methodComboBox;
    private JTextField pathTextField;
    private JPanel requestBodyPanel;
    private List<JPanel> keyValuePanels;
    private JButton submitButton;
    private JEditorPane responseTextArea;
    private JTextField statusCodeField;
    private JButton addKeyValueButton;

    public httpClient() {
        setTitle("HTTP Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Method and Path Panel
        JPanel methodPathPanel = new JPanel(new FlowLayout());
        methodPathPanel.add(new JLabel("Method:"));
        methodComboBox = new JComboBox<>(new String[]{"GET", "POST", "PUT"});
        methodPathPanel.add(methodComboBox);
        methodPathPanel.add(new JLabel("Path:"));
        pathTextField = new JTextField("/", 30);
        methodPathPanel.add(pathTextField);
        add(methodPathPanel, BorderLayout.NORTH);

        // Request Body Panel
        requestBodyPanel = new JPanel();
        requestBodyPanel.setLayout(new BoxLayout(requestBodyPanel, BoxLayout.Y_AXIS));
        keyValuePanels = new ArrayList<>();
        addKeyValuePanel();
        JScrollPane requestBodyScrollPane = new JScrollPane(requestBodyPanel);

        //Add key-value button
        addKeyValueButton = new JButton("Add Key-Value Pair");
        addKeyValueButton.addActionListener(e -> addKeyValuePanel());

        // Submit Button
        submitButton = new JButton("Submit Request");
        submitButton.addActionListener(new SubmitButtonListener());

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(addKeyValueButton);
        buttonPanel.add(submitButton);

        // Request Panel
        JPanel requestPanel = new JPanel();
        requestPanel.setLayout(new BorderLayout());
        requestPanel.add(requestBodyScrollPane, BorderLayout.CENTER);
        requestPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(requestPanel, BorderLayout.CENTER);

        // Response Text Area
        JPanel responsePanel = new JPanel();
        responsePanel.setLayout(new BorderLayout());

        // HTTP Status Code Label
        JPanel statusPanel = new JPanel(new GridLayout(1,2));
        statusPanel.add(new JLabel("HTTT Status Code:"));
        statusCodeField = new JTextField("");
        statusCodeField.setEditable(false);
        statusPanel.add(statusCodeField);
        responsePanel.add(statusPanel, BorderLayout.NORTH);

        // Response Text Area
        responseTextArea = new JEditorPane();
        responseTextArea.setContentType("text/html");
        responseTextArea.setEditable(false);
        Font font = new Font("Monospaced", Font.PLAIN, 12);
        responseTextArea.setFont(font);
        JScrollPane responseScrollPane = new JScrollPane(responseTextArea);

        // Set Size of the response area
        FontMetrics metrics = responseTextArea.getFontMetrics(font);
        int charWidth = metrics.charWidth('M');  // Use 'M' as it is usually the widest character
        int charHeight = metrics.getHeight();
        int preferredWidth = charWidth * 50;
        int preferredHeight = charHeight * 10;
        responseScrollPane.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        responsePanel.add(responseScrollPane, BorderLayout.CENTER);

        add(responsePanel, BorderLayout.SOUTH);

        pack(); // Adjust frame size to fit components
        setLocationRelativeTo(null); // Center the frame on the screen
    }

    private void addKeyValuePanel() {
        JPanel keyValuePanel = new JPanel(new FlowLayout());
        JTextField keyField = new JTextField("", 15);
        JTextField valueField = new JTextField("", 15);
        keyValuePanel.add(new JLabel("Key:"));
        keyValuePanel.add(keyField);
        keyValuePanel.add(new JLabel("Value:"));
        keyValuePanel.add(valueField);
        keyValuePanels.add(keyValuePanel);
        requestBodyPanel.add(keyValuePanel);
        requestBodyPanel.revalidate();
        requestBodyPanel.repaint();
    }

    private class SubmitButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String method = (String) methodComboBox.getSelectedItem();
            String path = pathTextField.getText();
            StringBuilder requestBody = new StringBuilder();
            for (JPanel keyValuePanel : keyValuePanels) {
                JTextField keyField = (JTextField) keyValuePanel.getComponent(1);
                JTextField valueField = (JTextField) keyValuePanel.getComponent(3);
                String key = keyField.getText();
                String value = valueField.getText();
                if (!key.isEmpty() && !value.isEmpty()) {
                    if (requestBody.length() > 0) {
                        requestBody.append("&");
                    }
                    requestBody.append(key).append("=").append(value);
                }
            }
            System.out.println(requestBody);
            StringBuilder response  = new StringBuilder();

            try {
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                // Construct the full URL
                String urlStr = "http://localhost:8080" + path;
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(10000);
                connection.setConnectTimeout(10000);
                connection.setUseCaches(false);
                connection.setAllowUserInteraction(false);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestMethod(method);

                if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                    connection.setDoOutput(true);
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = requestBody.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }
                }
                else{
                    connection.setDoOutput(false);
                }

                int status = connection.getResponseCode();

                BufferedReader br;
                if (status >= 200 && status < 300) {
                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                }
    
                String inputLine;
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine).append("\n");
                }
                br.close();
    
                responseTextArea.setText(response.toString());
                statusCodeField.setText(String.valueOf(status));
                
            } catch (Exception ex) {
                responseTextArea.setText("Error: " + ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            httpClient clientGUI = new httpClient();
            clientGUI.setVisible(true);
        });
    }
}
