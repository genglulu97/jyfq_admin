import java.sql.*;
public class QueryProducts {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:mysql://127.0.0.1:3306/loan_platform?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    try (Connection conn = DriverManager.getConnection(url, "root", "123456");
         PreparedStatement ps = conn.prepareStatement(
           "SELECT ip.id, ip.product_name, ip.inst_id, i.inst_name, i.merchant_alias, ip.status, i.status AS inst_status FROM institution_product ip JOIN institution i ON i.id = ip.inst_id ORDER BY ip.id DESC LIMIT 50")) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          System.out.println("id=" + rs.getLong("id") + ", product_name=" + rs.getString("product_name") + ", inst_id=" + rs.getLong("inst_id") + ", inst_name=" + rs.getString("inst_name") + ", merchant_alias=" + rs.getString("merchant_alias") + ", product_status=" + rs.getInt("status") + ", inst_status=" + rs.getInt("inst_status"));
        }
      }
    }
  }
}