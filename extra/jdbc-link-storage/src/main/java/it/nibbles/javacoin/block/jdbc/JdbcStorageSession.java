package it.nibbles.javacoin.block.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alessandro Polverini <alex@nibbles.it>
 */
public class JdbcStorageSession implements StorageSession
{

   // TODO
   
   private static Logger logger = LoggerFactory.getLogger(JdbcStorageSession.class);
   private Connection connection;
   private boolean closeWithCommit;

   public JdbcStorageSession(Connection connection, boolean closeWithCommit)
   {
      this.connection = connection;
      this.closeWithCommit = closeWithCommit;
   }

   public Connection getConnection()
   {
      return connection;
   }

   @Override
   public void close() throws IOException
   {
      try
      {
         if (closeWithCommit)
            connection.commit();
         connection.close();
      } catch (SQLException e)
      {
         try
         {
            if (closeWithCommit)
               connection.rollback();
         } catch (SQLException ex)
         {
         }
         logger.error("Ex while closing JdbcStorageSession: " + e.getMessage(), e);
         throw new JdbcStorageException("Ex while closing JdbcStorageSession: " + e.getMessage(), e);
      }
   }
}
