import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CompilerUI {
    private JFrame frame;
    private JTextArea bashInput;
    private JTextArea pythonOutput;
    private boolean isDarkMode = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CompilerUI().createAndShowGUI());
    }

    private void createAndShowGUI() {
        frame = new JFrame("Shell to Python Compiler");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new BorderLayout());

        // Split Pane for Bash and Python
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bashInput = new JTextArea();
        pythonOutput = new JTextArea();
        pythonOutput.setEditable(false);

        JScrollPane leftScroll = new JScrollPane(bashInput);
        JScrollPane rightScroll = new JScrollPane(pythonOutput);
        splitPane.setLeftComponent(leftScroll);
        splitPane.setRightComponent(rightScroll);
        splitPane.setDividerLocation(400);

        panel.add(splitPane, BorderLayout.CENTER);

        // Buttons
        JButton runButton = new JButton("Run");
        JButton manualButton = new JButton("Manual");
        JButton downloadButton = new JButton("Download");
        JButton toggleThemeButton = new JButton("Dark Mode");

        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(runButton);
        buttonPanel.add(manualButton);
        buttonPanel.add(downloadButton);
        buttonPanel.add(toggleThemeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add actions
        runButton.addActionListener(e -> runCompiler());
        manualButton.addActionListener(e -> openManual());
        downloadButton.addActionListener(e -> downloadPythonScript());
        toggleThemeButton.addActionListener(e -> toggleTheme(toggleThemeButton));

        frame.add(panel);
        updateTheme();
        frame.setVisible(true);
    }

    private void runCompiler() {
        try {
            Files.write(Paths.get("bash.sh"), bashInput.getText().getBytes(StandardCharsets.UTF_8));
            Main.main(new String[]{}); // Run your compiler
            String pythonCode = new String(Files.readAllBytes(Paths.get("output.py")), StandardCharsets.UTF_8);
            pythonOutput.setText(pythonCode);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openManual() {
        try {
            File manual = new File("manual.pdf");
            if (manual.exists()) {
                Desktop.getDesktop().open(manual);
            } else {
                JOptionPane.showMessageDialog(frame, "Manual not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error opening manual: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void downloadPythonScript() {
        try {
            String pythonCode = pythonOutput.getText();
            if (pythonCode.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No Python code to save!", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Files.write(Paths.get("converted_script.py"), pythonCode.getBytes(StandardCharsets.UTF_8));
            JOptionPane.showMessageDialog(frame, "Python script saved as 'converted_script.py'!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleTheme(JButton button) {
        isDarkMode = !isDarkMode;
        updateTheme();
        button.setText(isDarkMode ? "Light Mode" : "Dark Mode");
    }

    private void updateTheme() {
        Color backgroundColor = isDarkMode ? Color.DARK_GRAY : new Color(230, 230, 250);
        Color textColor = isDarkMode ? Color.WHITE : Color.BLACK;

        if (bashInput != null) {
            bashInput.setBackground(backgroundColor);
            bashInput.setForeground(textColor);
        }
        if (pythonOutput != null) {
            pythonOutput.setBackground(backgroundColor);
            pythonOutput.setForeground(textColor);
        }
        if (frame != null) {
            frame.getContentPane().setBackground(isDarkMode ? Color.GRAY : Color.LIGHT_GRAY);
        }
    }
}
