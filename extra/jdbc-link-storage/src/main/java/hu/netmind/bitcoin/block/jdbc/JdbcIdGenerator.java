package hu.netmind.bitcoin.block.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 *
 * @author Alessandro Polverini
 */
public class JdbcIdGenerator {

  private int idReserveSize = 100;
  private long currentId, idPoolSizeLeft;
  private String idName;
  private String jdbcUrl, dbUser, dbPassword;
  private DataSource dataSource;

  public JdbcIdGenerator(String driverClassName, String url, String user, String pw) {
    this.jdbcUrl = url;
    this.dbUser = user;
    this.dbPassword = pw;
    try {
      Class.forName(driverClassName);
    } catch (ClassNotFoundException e) {
      throw new JdbcStorageException("JDBC driver class not found: " + driverClassName);
    }
  }

  public JdbcIdGenerator(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public JdbcIdGenerator setIdReserveSize(int idReserveSize) {
    this.idReserveSize = idReserveSize;
    return this;
  }

  public JdbcIdGenerator setIdName(String idName) {
    this.idName = idName;
    return this;
  }

  private Connection getConnection() {
    try {
      if (dataSource != null) {
        return dataSource.getConnection();
      } else {
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
      }
    } catch (SQLException e) {
      throw new JdbcStorageException("Can't get a connection for idGen: " + e.getMessage(), e);
    }
  }

  synchronized public long getNewId() {
    if (idPoolSizeLeft-- > 0) {
      return ++currentId;
    } else {
      Connection con = null;
      PreparedStatement psInsertId = null;
      PreparedStatement psGetId = null;
      PreparedStatement psUpdateId = null;
      ResultSet rs = null;
      long lastId = 0;
      try {
        con = getConnection();
        con.setAutoCommit(false);
        psGetId = con.prepareStatement("SELECT value FROM Counter WHERE name = ?");
        psGetId.setString(1, idName);
        rs = psGetId.executeQuery();
        if (rs.next()) {
          lastId = rs.getLong(1);
          psUpdateId = con.prepareStatement("UPDATE Counter set value = ? WHERE name = ?");
          psUpdateId.setLong(1, lastId + idReserveSize);
          psUpdateId.setString(2, idName);
          psUpdateId.executeUpdate();
        } else {
          psInsertId = con.prepareStatement("INSERT INTO Counter(name,value) VALUES (?,?)");
          psInsertId.setString(1, idName);
          psInsertId.setLong(2, idReserveSize);
          psInsertId.executeUpdate();
        }
        con.commit();
        idPoolSizeLeft = idReserveSize - 1;
        return (currentId = lastId + 1);
      } catch (SQLException e) {
        try {
          con.rollback();
        } catch (SQLException ex) {
        }
        throw new JdbcStorageException("Exception getting new Id: " + e.getMessage(), e);
      } finally {
        try {
          if (rs != null) {
            rs.close();
          }
          if (psInsertId != null) {
            psInsertId.close();
          }
          if (psGetId != null) {
            psGetId.close();
          }
          if (psUpdateId != null) {
            psUpdateId.close();
          }
          if (con != null) {
            con.close();
          }
        } catch (SQLException ex) {
        }
      }
    }
  }
}
