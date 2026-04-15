import java.sql.*;
public class QueryChannel {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:mysql://127.0.0.1:3306/loan_platform?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    try (Connection c = DriverManager.getConnection(url, "root", "123456");
         PreparedStatement ps = c.prepareStatement("select id, channel_code, channel_name, app_key, status from channel where channel_code = ?")) {
      ps.setString(1, "microsilver");
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          System.out.println(rs.getLong(1) + "|" + rs.getString(2) + "|" + rs.getString(3) + "|" + rs.getString(4) + "|" + rs.getInt(5));
        }
      }
    }
  }
}
