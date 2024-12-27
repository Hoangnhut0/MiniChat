import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Client {
    private Socket socket; // Socket kết nối đến server
    private PrintWriter out; // Để gửi tin nhắn đến server
    private BufferedReader in; // Để nhận tin nhắn từ server
    private String username; // Tên người dùng
    private String roomName; // Tên phòng chat

    public Client() {
        // Tạo cửa sổ chính
        JFrame frame = new JFrame("Chat của Nhựt");
        frame.setSize(500, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Đóng ứng dụng khi tắt cửa sổ

        // Tạo panel chính để chứa các thành phần giao diện
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Panel chat hiển thị tin nhắn
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS)); // Sắp xếp tin nhắn theo chiều dọc
        chatPanel.setBackground(new Color(250, 250, 250)); // Màu nền của khu vực hiển thị tin nhắn

        JScrollPane chatScrollPane = new JScrollPane(chatPanel); // Thêm cuộn khi tin nhắn vượt quá kích thước
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED); // Hiển thị thanh cuộn khi cần
        mainPanel.add(chatScrollPane, BorderLayout.CENTER); // Thêm vào vùng trung tâm của giao diện

        // Panel nhập tin nhắn
        JTextField inputField = new JTextField(); // Trường nhập tin nhắn
        inputField.setFont(new Font("Arial", Font.PLAIN, 14)); // Đặt font chữ
        inputField.setBackground(new Color(240, 248, 255)); // Màu nền của ô nhập liệu
        inputField.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1)); // Đường viền của ô nhập

        JButton sendButton = new JButton("Gửi"); // Nút gửi tin nhắn
        sendButton.setFont(new Font("Arial", Font.BOLD, 14)); // Đặt font chữ của nút
        sendButton.setBackground(new Color(70, 130, 180)); // Màu nền nút gửi
        sendButton.setForeground(Color.WHITE); // Màu chữ trên nút gửi
        sendButton.setFocusPainted(false); // Xóa đường viền khi nút được chọn

        JPanel inputPanel = new JPanel(new BorderLayout()); // Tạo panel để chứa ô nhập và nút gửi
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Căn lề xung quanh panel
        inputPanel.add(inputField, BorderLayout.CENTER); // Thêm ô nhập vào giữa
        inputPanel.add(sendButton, BorderLayout.EAST); // Thêm nút gửi vào bên phải
        mainPanel.add(inputPanel, BorderLayout.SOUTH); // Thêm panel nhập vào phía dưới

        // Header của giao diện
        JLabel headerLabel = new JLabel("Chat của Nhựt", SwingConstants.CENTER); // Tiêu đề
        headerLabel.setFont(new Font("Arial", Font.BOLD, 18)); // Font chữ tiêu đề
        headerLabel.setOpaque(true); // Để hiển thị màu nền
        headerLabel.setBackground(new Color(70, 130, 180)); // Màu nền tiêu đề
        headerLabel.setForeground(Color.WHITE); // Màu chữ tiêu đề
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // Căn lề tiêu đề
        mainPanel.add(headerLabel, BorderLayout.NORTH); // Thêm tiêu đề vào phía trên

        frame.add(mainPanel); // Thêm panel chính vào frame
        frame.setVisible(true); // Hiển thị giao diện

        // Kết nối tới server
        try {
            socket = new Socket("localhost", 12345); // Kết nối đến server tại localhost và cổng 12345
            out = new PrintWriter(socket.getOutputStream(), true); // Tạo luồng gửi tin nhắn
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Tạo luồng nhận tin nhắn

            // Nhập tên người dùng
            username = JOptionPane.showInputDialog(frame, "Nhập tên của bạn:", "Chat của Nhựt", JOptionPane.PLAIN_MESSAGE);
            if (username == null || username.trim().isEmpty()) { // Kiểm tra tên không được để trống
                JOptionPane.showMessageDialog(frame, "Tên không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            out.println(username); // Gửi tên người dùng đến server

            // Nhập tên phòng chat
            roomName = JOptionPane.showInputDialog(frame, "Nhập tên phòng chat:", "Chat của Nhựt", JOptionPane.PLAIN_MESSAGE);
            if (roomName == null || roomName.trim().isEmpty()) { // Kiểm tra phòng không được để trống
                JOptionPane.showMessageDialog(frame, "Tên phòng không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            out.println(roomName); // Gửi tên phòng đến server

            // Thread để nhận tin nhắn
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) { // Lắng nghe tin nhắn từ server
                        if (message.startsWith(username + ":")) { // Tin nhắn từ chính người dùng
                            addMessage(chatPanel, message, true);
                        } else { // Tin nhắn từ người dùng khác
                            addMessage(chatPanel, message, false);
                        }
                    }
                } catch (IOException e) {
                    addMessage(chatPanel, "Mất kết nối.", false); // Hiển thị thông báo mất kết nối
                }
            }).start(); // Bắt đầu thread

            // Xử lý nút gửi tin nhắn
            sendButton.addActionListener(e -> {
                String message = inputField.getText(); // Lấy nội dung tin nhắn từ ô nhập
                if (!message.isEmpty()) { // Nếu tin nhắn không trống
                    addMessage(chatPanel, username + ": " + message, true); // Hiển thị tin nhắn của chính mình
                    out.println(message); // Gửi tin nhắn đến server
                    inputField.setText(""); // Xóa nội dung ô nhập sau khi gửi
                }
            });

            inputField.addActionListener(e -> sendButton.doClick()); // Cho phép nhấn Enter để gửi

        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Không thể kết nối tới server.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            System.exit(0); // Thoát ứng dụng nếu không kết nối được
        }
    }

    // Thêm tin nhắn vào giao diện
    private void addMessage(JPanel chatPanel, String message, boolean isMine) {
        JPanel messagePanel = new JPanel(); // Panel chứa từng tin nhắn
        messagePanel.setLayout(new FlowLayout(isMine ? FlowLayout.RIGHT : FlowLayout.LEFT)); // Canh phải cho tin nhắn của mình, trái cho người khác
        messagePanel.setOpaque(false); // Không nền

        JLabel messageLabel = new JLabel("<html><p style=\"width: 300px; word-wrap: break-word;\">" + message + "</p></html>"); // Nội dung tin nhắn
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14)); // Font chữ của tin nhắn
        messageLabel.setOpaque(true); // Hiển thị màu nền
        messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Căn lề tin nhắn

        if (isMine) { // Nếu là tin nhắn của chính người dùng
            messageLabel.setBackground(new Color(135, 206, 235)); // Màu nền xanh nhạt
            messageLabel.setForeground(Color.BLACK); // Màu chữ đen
        } else { // Nếu là tin nhắn từ người khác
            messageLabel.setBackground(new Color(245, 245, 245)); // Màu nền xám nhạt
            messageLabel.setForeground(Color.DARK_GRAY); // Màu chữ xám đậm
        }

        messagePanel.add(messageLabel); // Thêm label tin nhắn vào panel
        chatPanel.add(messagePanel); // Thêm panel tin nhắn vào khu vực chat
        chatPanel.revalidate(); // Làm mới giao diện
        chatPanel.repaint(); // Vẽ lại giao diện
    }

    public static void main(String[] args) {
        new Client(); // Khởi chạy client
    }
}
