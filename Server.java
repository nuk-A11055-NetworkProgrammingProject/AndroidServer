import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class Server {
    private ServerSocket serverSocket;
    final private int port = 12346;
    private static Hashtable<Socket, OutputStream> clients;
    private static Connection mysql;

    Server() {
        try {
            serverSocket = new ServerSocket(port);
            clients = new Hashtable<Socket, OutputStream>();
            System.out.println("Server started.");

            // 連接資料庫
            try {
                String URL = "jdbc:mysql://localhost:3306/java?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC";
                String USER = "root";
                String PWD = "goodboy20000";
                mysql = DriverManager.getConnection(URL, USER, PWD);
                System.out.println("Successfully connect to MySQL.");
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Failed to connect MySQL.");
            }

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept(); // Waiting for client to connect
                System.out.println("connected from " + socket.getInetAddress().getHostAddress());
                newClient(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void newClient(Socket socket) throws IOException {
        clients.put(socket, socket.getOutputStream());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                    boolean login = false;

                    // 用戶端尚未登入
                    while (!login) {

                        String msg = bufferedReader.readLine();
                        String option = msg.substring(0, 10);
                        String data = msg.substring(10);
                        // System.out.println(option);
                        
                        // 嘗試登入
                        if (option.equals("CHECKLOGIN")) {
                            try {
                                String[] tokens = data.split(":");
                                String username = tokens[0], password = tokens[1];

                                String query = "SELECT * from user_account WHERE Name = ?";
                                PreparedStatement preparedStatement = mysql.prepareStatement(query);
                                preparedStatement.setString(1, username);
                                ResultSet resultSet = preparedStatement.executeQuery();

                                if (resultSet.next()) {
                                    String correct_password = resultSet.getString("password");
                                    if (password.equals(correct_password)) {
                                        System.out.println(username + " login success from " + socket.getInetAddress().getHostAddress());
                                        bufferedWriter.write("Login success");
                                        login = true;
                                    } else {
                                        System.out.println(socket.getInetAddress().getHostAddress() + " attempting to login " + username + " with wrong password");
                                        bufferedWriter.write("Wrong password");
                                    }
                                } else {
                                    System.out.println(socket.getInetAddress().getHostAddress() + " attempting to login with a non-existent username.");
                                    bufferedWriter.write("Username not found");
                                }

                                bufferedWriter.newLine();
                                bufferedWriter.flush();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            // 嘗試註冊
                        } else if (option.equals("INSERTUSER")) {
                            try {
                                String[] tokens = data.split(":");
                                String username = tokens[0], password = tokens[1];

                                String query = "SELECT * from user_account WHERE Name = ?";
                                PreparedStatement preparedStatement = mysql.prepareStatement(query);
                                preparedStatement.setString(1, username);
                                ResultSet resultSet = preparedStatement.executeQuery();

                                if (resultSet.next()) {
                                    // username 重複
                                    bufferedWriter.write("Username already exist");
                                } else {
                                    query = "INSERT INTO user_account (Name, Password) VALUES (?, ?)";
                                    preparedStatement = mysql.prepareStatement(query);

                                    preparedStatement.setString(1, username);
                                    preparedStatement.setString(2, password);
                                    int rowsAffected = preparedStatement.executeUpdate();

                                    if (rowsAffected > 0) {
                                        System.out.println("Insert successful. " + rowsAffected + " affected");
                                        bufferedWriter.write("Successfully register");
                                    } else {
                                        System.out.println("Insert failed. No rows affected");
                                        bufferedWriter.write("Failed to register");
                                    }
                                }

                                bufferedWriter.newLine();
                                bufferedWriter.flush();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // 用戶端已登入
                    while (login) {
                        
                        String msg = bufferedReader.readLine();
                        if (msg == null) {
                            break;
                        }
                        // 12/7 新增create table
                        String option = msg.substring(0, 10);
                        String data = msg.substring(10);
                        if(option.equals("CREATEGRUP")){
                            try {
                                String[] tokens = data.split(":");
                                String username = tokens[0], grupname = tokens[1];
                                System.out.println(username + " : create grup :" + grupname);
                                String query = "CREATE TABLE "+grupname+" (Name VARCHAR(20), Time TIMESTAMP,Message VARCHAR(1000))";
                                Statement statement = mysql.createStatement();
                                statement.executeUpdate(query);
                                // bufferedWriter.write("Successfully create grup");
                                // bufferedWriter.newLine();
                                // bufferedWriter.flush();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }// 12/7 新增create table 結束
                        else if(option.equals("HAVINGROUP")){
                            // 接 having
                            // String AA = bufferedReader.readLine();
                            String return_now_table = "";
                            try {
                                String query = "SHOW TABLES";
                                Statement statement = mysql.createStatement();
                                ResultSet resultSet = statement.executeQuery(query);
                                while (resultSet.next()) {
                                    String tableName = resultSet.getString(1);
                                    if(tableName.equals("user_account")){
                                        continue;
                                    }
                                    return_now_table += ":" + tableName;
                                    System.out.println(tableName);
                                }
                                bufferedWriter.write(return_now_table);
                                bufferedWriter.newLine();
                                bufferedWriter.flush();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
//                        saveMsgToDB(msg);
                        System.out.println(msg);
                        broadcastMsg(msg);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println(socket.getInetAddress().getHostAddress() + " leave");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    clients.remove(socket);
                }
            }
        }).start();
    }

    public static void broadcastMsg(String msg) throws IOException {
        for (Enumeration e = clients.elements(); e.hasMoreElements(); ) {
            BufferedWriter outstream = new BufferedWriter(
                    new OutputStreamWriter((OutputStream) e.nextElement()));

            outstream.write(msg + "\n");
            outstream.flush();
        }
    }

    public static void saveMsgToDB(String msg) {
        String[] tokens = msg.split(":");
        String name = tokens[0], message = tokens[1];

        // 取得目前時間
        Date currentDate = new Date();
        Timestamp timestamp = new Timestamp(currentDate.getTime());

        String sql = "INSERT INTO chat_history(Name, Time, Message) VALUES (?, ?, ?)";
        try {
            PreparedStatement preparedStatement = mysql.prepareStatement(sql);
            preparedStatement.setString(1, name);
            preparedStatement.setTimestamp(2, timestamp);
            preparedStatement.setString(3, message);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}