import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class Server {
    private ServerSocket serverSocket;
    final private int port = 12346;
    private static Hashtable<Socket, OutputStream> clients;
    private static Hashtable<String, Hashtable<Socket, OutputStream>> groups;
    private static Connection mysql;

    Server() {
        try {
            serverSocket = new ServerSocket(port);
            clients = new Hashtable<Socket, OutputStream>();
            groups = new Hashtable<String, Hashtable<Socket, OutputStream>>();
            System.out.println("Server started.");

            // 連接資料庫
            try {
                String URL = "jdbc:mysql://localhost:3306/androidserver?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC";
                String USER = "root";
                String PWD = "xu35p4jo6";
                mysql = DriverManager.getConnection(URL, USER, PWD);
                System.out.println("Successfully connect to MySQL.");
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Failed to connect MySQL.");
            }

            // 找出目前有哪些群組
            try {
                String query = "SHOW TABLES";
                PreparedStatement preparedStatement = mysql.prepareStatement(query);
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    String tableName = resultSet.getString(1);
                    if(tableName.equals("user_account")){
                        continue;
                    }
                    groups.put(tableName, new Hashtable<Socket, OutputStream>());
                    System.out.println(tableName);
                }
            } catch (SQLException e) {
                e.printStackTrace();
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
                                String query = "CREATE TABLE "+grupname+" (Name VARCHAR(20), Time TIMESTAMP,Message TEXT)";
                                Statement statement = mysql.createStatement();
                                statement.executeUpdate(query);
                                // bufferedWriter.write("Successfully create grup");
                                // bufferedWriter.newLine();
                                // bufferedWriter.flush();
                                groups.put(grupname, new Hashtable<Socket, OutputStream>());
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
                        } else if (option.equals("ENTERGROUP")) {

                            String groupName = data;
                            groups.get(groupName).put(socket, socket.getOutputStream());
                            // 傳送歷史訊息
                            try {
                                String query = "SELECT * FROM " + groupName;
                                PreparedStatement preparedStatement = mysql.prepareStatement(query);
                                ResultSet resultSet = preparedStatement.executeQuery();
                                System.out.println("Sending chat history...");
                                while (resultSet.next() && login) {
                                    String username = resultSet.getString("Name");
                                    String message = resultSet.getString("Message");
                                    String time  = resultSet.getTimestamp("Time").toString();
                                    bufferedWriter.write(username + "\n");
                                    bufferedWriter.write(message + "\n");
                                    bufferedWriter.write(time + "\n");
                                    bufferedWriter.flush();
                                    System.out.println(username + " " + message + " " + time);
                                }
                                // 送出歷史訊息傳送完畢的 signal
                                bufferedWriter.write("end" + "\n");
                                bufferedWriter.write("end" + "\n");
                                bufferedWriter.write("end" + "\n");
                                bufferedWriter.flush();
                                System.out.println("Done");

                            } catch (SQLException e) {
                                e.printStackTrace();
                            }

                            // 開始接收訊息、廣播
                            try {
                                while (true) {
                                    String name = bufferedReader.readLine();
                                    String msg2 = bufferedReader.readLine();
                                    String time = bufferedReader.readLine();
                                    System.out.println(name+" "+msg2+" "+time);
                                    if (time.equals("1970-01-01 00:00:01.0") || time == null) {
                                        System.out.println(time);
                                        break;
                                    }
                                    saveMsgToDB(name, msg2, time, groupName);
                                    broadcastMsg(name, msg2, time, groupName);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                groups.get(groupName).remove(socket);
                                System.out.println("leave " + groupName);
                                // clients.remove(socket);
                            }
                        }
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

    public static void broadcastMsg(String name, String msg, String time, String groupName) throws IOException {

        // 廣播訊息給與用戶同群組的其他用戶
        for (Enumeration e = groups.get(groupName).elements(); e.hasMoreElements(); ) {

            BufferedWriter outstream = new BufferedWriter(
                    new OutputStreamWriter((OutputStream) e.nextElement()));

            outstream.write(name + "\n");
            outstream.write(msg + "\n");
            outstream.write(time + "\n");
            outstream.flush();
        }
    }

    public static void saveMsgToDB(String name, String msg, String time, String groupName) {
        // 轉換時間為 Timestamp 型態
        Timestamp timestamp = Timestamp.valueOf(time);;

        String sql = "INSERT INTO " + groupName + "(Name, Time, Message) VALUES (?, ?, ?)";
        try {
            PreparedStatement preparedStatement = mysql.prepareStatement(sql);
            preparedStatement.setString(1, name);
            preparedStatement.setTimestamp(2, timestamp);
            preparedStatement.setString(3, msg);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}