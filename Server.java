import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    // Quản lý danh sách các phòng chat và các client trong mỗi phòng
    private static final Map<String, Set<Socket>> chatRooms = new HashMap<>();
    // Bản đồ để tra cứu phòng chat mà mỗi client đang tham gia
    private static final Map<Socket, String> clientRoomMap = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) { // Tạo server tại cổng 12345
            System.out.println("Server đang hoạt động...");

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Chờ và chấp nhận kết nối từ client
                new Thread(() -> handleClient(clientSocket)).start(); // Tạo luồng mới xử lý client
            }
        } catch (IOException e) {
            e.printStackTrace(); // In lỗi nếu xảy ra lỗi khi khởi động server
        }
    }

    // Hàm xử lý một client
    private static void handleClient(Socket clientSocket) {
        String username = null; // Lưu trữ tên người dùng
        String roomName = null; // Lưu trữ tên phòng chat

        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Nhận thông tin username từ client
            username = in.readLine();

            // Nhận thông tin tên phòng chat từ client
            roomName = in.readLine();

            // Thêm client vào phòng chat
            synchronized (chatRooms) {
                chatRooms.putIfAbsent(roomName, new HashSet<>()); // Nếu phòng chưa tồn tại, tạo mới
                chatRooms.get(roomName).add(clientSocket); // Thêm client vào phòng
                clientRoomMap.put(clientSocket, roomName); // Lưu thông tin phòng của client
            }

            System.out.println(username + " joined room: " + roomName); // Ghi log khi client tham gia phòng

            // Lắng nghe và xử lý tin nhắn từ client
            String message;
            while ((message = in.readLine()) != null) {
                String fullMessage = username + ":  " + message; // Gắn tên người dùng vào tin nhắn
                broadcast(roomName, fullMessage, clientSocket); // Phát tin nhắn đến các client trong phòng
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + username + " from room: " + roomName); // Log khi client ngắt kết nối
        } finally {
            removeClient(clientSocket, username, roomName); // Loại bỏ client khỏi phòng khi ngắt kết nối
        }
    }

    // Gửi tin nhắn đến tất cả client trong phòng
    private static void broadcast(String roomName, String message, Socket sender) {
        synchronized (chatRooms) {
            Set<Socket> roomClients = chatRooms.getOrDefault(roomName, new HashSet<>()); // Lấy danh sách client trong phòng

            Iterator<Socket> iterator = roomClients.iterator(); // Duyệt qua từng client trong phòng
            while (iterator.hasNext()) {
                Socket socket = iterator.next();
                if (socket != sender) { // Không gửi lại tin nhắn cho chính người gửi
                    try {
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println(message); // Gửi tin nhắn
                    } catch (IOException e) {
                        System.out.println("Failed to send message to a client in room: " + roomName);
                        iterator.remove(); // Loại bỏ client không còn hợp lệ
                    }
                }
            }
        }
    }

    // Loại bỏ client khỏi phòng chat và đóng kết nối
    private static void removeClient(Socket clientSocket, String username, String roomName) {
        if (roomName != null) {
            synchronized (chatRooms) {
                Set<Socket> roomClients = chatRooms.get(roomName); // Lấy danh sách client trong phòng
                if (roomClients != null) {
                    roomClients.remove(clientSocket); // Loại bỏ client khỏi phòng
                    System.out.println(username + " left room: " + roomName); // Log khi client rời phòng

                    if (roomClients.isEmpty()) { // Nếu phòng trống, xóa phòng
                        chatRooms.remove(roomName);
                        System.out.println("Room " + roomName + " is now empty and has been removed.");
                    }
                }
            }
        }
        clientRoomMap.remove(clientSocket); // Xóa client khỏi danh sách tra cứu phòng
        try {
            clientSocket.close(); // Đóng kết nối socket
        } catch (IOException e) {
            System.out.println("Error closing client socket for: " + username);
        }
    }
}
