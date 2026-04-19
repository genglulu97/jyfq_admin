import java.sql.*;
public class QueryProductConfig {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:mysql://127.0.0.1:3306/loan_platform?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    try (Connection conn = DriverManager.getConnection(url, "root", "123456");
         PreparedStatement ps = conn.prepareStatement(
           "SELECT ip.id, ip.inst_id, ip.product_name, ip.status, ip.min_age, ip.max_age, ip.min_amount, ip.max_amount, ip.weight, ip.priority, " +
           "ip.city_list, ip.excluded_city_codes, ip.specified_channels, ip.excluded_channels, ip.working_hours, ip.qualification_config, " +
           "i.inst_code, i.inst_name, i.merchant_alias, i.status AS inst_status " +
           "FROM institution_product ip JOIN institution i ON i.id = ip.inst_id WHERE ip.product_name LIKE ? ORDER BY ip.id DESC")) {
      ps.setString(1, "%??????A%");
      try (ResultSet rs = ps.executeQuery()) {
        ResultSetMetaData md = rs.getMetaData();
        while (rs.next()) {
          for (int i = 1; i <= md.getColumnCount(); i++) {
            System.out.println(md.getColumnLabel(i) + "=" + rs.getString(i));
          }
          System.out.println("---ROW---");
        }
      }
    }
  }
}