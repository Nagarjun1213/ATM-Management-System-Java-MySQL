import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ATM extends JFrame {
    private JTextField cardNumberField, depositAmountField, withdrawAmountField;
    private JPasswordField pinField;
    private JLabel balanceLabel;
    private String currentUserCard;

    // Database Connection
    private Connection conn;

    public ATM() {
        // Connect to Database
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3307/atm_system", "root", "Nagarjun@25"); // Change password
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed!");
            System.exit(0);
        }

        setTitle("ATM System");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new CardLayout());

        // Panel for Login/Register
        JPanel authPanel = new JPanel(new GridLayout(3, 2));

        authPanel.add(new JLabel("Card Number:"));
        cardNumberField = new JTextField();
        authPanel.add(cardNumberField);

        authPanel.add(new JLabel("PIN:"));
        pinField = new JPasswordField();
        authPanel.add(pinField);

        JButton registerButton = new JButton("Register");
        JButton loginButton = new JButton("Login");
        authPanel.add(registerButton);
        authPanel.add(loginButton);

        // ATM Panel (After login)
        JPanel atmPanel = new JPanel(new GridLayout(5, 2));

        balanceLabel = new JLabel("Balance: $0.0");
        atmPanel.add(balanceLabel);

        atmPanel.add(new JLabel("Deposit Amount:"));
        depositAmountField = new JTextField();
        atmPanel.add(depositAmountField);
        JButton depositButton = new JButton("Deposit");
        atmPanel.add(depositButton);

        atmPanel.add(new JLabel("Withdraw Amount:"));
        withdrawAmountField = new JTextField();
        atmPanel.add(withdrawAmountField);
        JButton withdrawButton = new JButton("Withdraw");
        atmPanel.add(withdrawButton);

        JButton logoutButton = new JButton("Logout");
        atmPanel.add(logoutButton);

        add(authPanel, "auth");
        add(atmPanel, "atm");

        CardLayout cardLayout = (CardLayout) getContentPane().getLayout();

        // Register Button Action
        registerButton.addActionListener(e -> showRegistrationDialog());

        // Login Button Action
        loginButton.addActionListener(e -> {
            String cardNumber = cardNumberField.getText();
            String pin = new String(pinField.getPassword());

            if (authenticateUser(cardNumber, pin)) {
                currentUserCard = cardNumber;
                updateBalance();
                cardLayout.show(getContentPane(), "atm");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid card number or PIN!");
            }
        });

        // Deposit Money
        depositButton.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(depositAmountField.getText());
                if (amount <= 0) {
                    JOptionPane.showMessageDialog(this, "Enter a valid amount!");
                } else {
                    updateBalanceInDB(currentUserCard, amount);
                    updateBalance();
                    JOptionPane.showMessageDialog(this, "Deposit successful!");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid amount!");
            }
        });

        // Withdraw Money
        withdrawButton.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(withdrawAmountField.getText());
                if (amount <= 0) {
                    JOptionPane.showMessageDialog(this, "Enter a valid amount!");
                } else if (!withdrawMoney(currentUserCard, amount)) {
                    JOptionPane.showMessageDialog(this, "Insufficient funds!");
                } else {
                    updateBalance();
                    JOptionPane.showMessageDialog(this, "Withdrawal successful!");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid amount!");
            }
        });

        // Logout Button
        logoutButton.addActionListener(e -> {
            currentUserCard = null;
            cardNumberField.setText("");
            pinField.setText("");
            cardLayout.show(getContentPane(), "auth");
        });

        setVisible(true);
    }

    private void showRegistrationDialog() {
        JTextField nameField = new JTextField();
        JTextField cardNumberField = new JTextField();
        JPasswordField pinField = new JPasswordField();
        JPasswordField confirmPinField = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(4, 2));
        panel.add(new JLabel("Full Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Card Number:"));
        panel.add(cardNumberField);
        panel.add(new JLabel("PIN:"));
        panel.add(pinField);
        panel.add(new JLabel("Confirm PIN:"));
        panel.add(confirmPinField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Register", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText();
            String cardNumber = cardNumberField.getText();
            String pin = new String(pinField.getPassword());
            String confirmPin = new String(confirmPinField.getPassword());

            if (name.isEmpty() || cardNumber.isEmpty() || pin.isEmpty() || confirmPin.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required!");
            } else if (!pin.equals(confirmPin)) {
                JOptionPane.showMessageDialog(this, "PINs do not match!");
            } else if (registerUser(name, cardNumber, pin)) {
                JOptionPane.showMessageDialog(this, "Registration successful!");
            } else {
                JOptionPane.showMessageDialog(this, "Card Number already exists!");
            }
        }
    }

    private boolean registerUser(String name, String cardNumber, String pin) {
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (name, card_number, pin) VALUES (?, ?, ?)");
            stmt.setString(1, name);
            stmt.setString(2, cardNumber);
            stmt.setString(3, pin);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean authenticateUser(String cardNumber, String pin) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE card_number = ? AND pin = ?");
            stmt.setString(1, cardNumber);
            stmt.setString(2, pin);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private void updateBalance() {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT balance FROM users WHERE card_number = ?");
            stmt.setString(1, currentUserCard);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                balanceLabel.setText("Balance: $" + rs.getDouble("balance"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateBalanceInDB(String cardNumber, double amount) {
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE card_number = ?");
            stmt.setDouble(1, amount);
            stmt.setString(2, cardNumber);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean withdrawMoney(String cardNumber, double amount) {
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE users SET balance = balance - ? WHERE card_number = ? AND balance >= ?");
            stmt.setDouble(1, amount);
            stmt.setString(2, cardNumber);
            stmt.setDouble(3, amount);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        new ATM();
    }
}
