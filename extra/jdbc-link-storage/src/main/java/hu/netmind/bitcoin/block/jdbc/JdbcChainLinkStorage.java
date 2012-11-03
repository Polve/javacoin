/**
 * Copyright (C) 2012 nibbles.it
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package hu.netmind.bitcoin.block.jdbc;

import hu.netmind.bitcoin.net.NodeAddress;
import hu.netmind.bitcoin.net.p2p.NodeStorage;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storing external node information using JDBC (MySql only for now).
 *
 * @author Alessandro Polverini
 */
public class JdbcChainLinkStorage implements NodeStorage
{

   private static final boolean DEFAULT_AUTOCREATE = true;
   private static final boolean DEFAULT_TRANSACTIONAL = true;
   private static Logger logger = LoggerFactory.getLogger(JdbcChainLinkStorage.class);
   private boolean autoCreate = DEFAULT_AUTOCREATE;
   private boolean transactional = DEFAULT_TRANSACTIONAL;
   private DataSource dataSource;
   //
   // Address handling
   final private String sqlPutNodeAddress =
           "INSERT INTO Node(address, port, services, discovered) VALUES (?,?,?,?)";
   final private String sqlUpdateNodeAddress =
           "UPDATE Node set services=?, discovered=? WHERE address=? AND port=?";
   final private String sqlGetNodeAddress =
           "SELECT * FROM Node WHERE address=? AND port=?";
   final private String sqlGetNodeAddresses =
           "SELECT * FROM Node ORDER BY discovered DESC";

   public JdbcChainLinkStorage()
   {
      readConfiguration();
   }

   private void readConfiguration()
   {
      try
      {
         final String prefix = "storage.jdbc.";
         ResourceBundle config = ResourceBundle.getBundle("jdbc-link-storage");
         autoCreate = Boolean.valueOf(config.getString(prefix + "autocreate"));
         transactional = Boolean.valueOf(config.getString(prefix + "transactional"));
      } catch (MissingResourceException e)
      {
         logger.warn("could not read configuration for JDBC link storage, using some default values", e);
      }
   }

   /**
    * To use the storage, it must be first initialized by calling this method.
    */
   public void init()
   {
      initializeDatabases();
   }

   private void initializeDatabases()
   {
      logger.info("TODO: Initialize tables");
      // TODO: autocreate tables and indexes
   }

   private Connection getDbConnection()
   {
      try
      {
         return dataSource.getConnection();
      } catch (SQLException e)
      {
         throw new JdbcStorageException("Unable to obtain a connection to database: " + e.getMessage(), e);
      }
   }

   /**
    * Removes the database. Used for unit testing
    */
   public void removeDatabase()
   {
      //orphanBlocks.clear();
      try (Connection dbConnection = getDbConnection(); Statement st = dbConnection.createStatement())
      {
         String[] tables =
         {         };

         for (String table : tables)
            st.execute("TRUNCATE TABLE " + table);
         logger.debug("Database tables truncated");
      } catch (SQLException e)
      {
         throw new JdbcStorageException("Error while truncating tables: " + e.getMessage(), e);
      }
   }


   public boolean getAutoCreate()
   {
      return autoCreate;
   }

   public void setAutoCreate(boolean autoCreate)
   {
      this.autoCreate = autoCreate;
   }

   public boolean getTransactional()
   {
      return transactional;
   }

   public void setTransactional(boolean transactional)
   {
      this.transactional = transactional;
   }

   public DataSource getDataSource()
   {
      return dataSource;
   }

   public void setDataSource(DataSource dataSource)
   {
      this.dataSource = dataSource;
   }

   //
   // Node address storage
   //
   @Override
   public void storeNodeAddress(NodeAddress node)
   {
      try (Connection dbConnection = getDbConnection(); PreparedStatement psReadOld = dbConnection.prepareStatement(sqlGetNodeAddress))
      {
         InetAddress addr = node.getAddress().getAddress();
         String textAddr;
         if (addr instanceof Inet4Address)
            textAddr = addr.toString().substring(1);
         else
            textAddr = addr.toString();
         psReadOld.setString(1, textAddr);
         psReadOld.setLong(2, node.getAddress().getPort());
         ResultSet rs = psReadOld.executeQuery();
         if (rs.next())
            try (PreparedStatement ps = dbConnection.prepareStatement(sqlUpdateNodeAddress))
            {
               ps.setLong(1, node.getServices());
               ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
               ps.setString(3, textAddr);
               ps.setInt(4, node.getAddress().getPort());
               ps.executeUpdate();
            }
         else
            try (PreparedStatement ps = dbConnection.prepareStatement(sqlPutNodeAddress))
            {
               ps.setString(1, textAddr);
               ps.setInt(2, node.getAddress().getPort());
               ps.setLong(3, node.getServices());
               ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
               ps.executeUpdate();
            }
      } catch (SQLException e)
      {
         logger.error("putNodeAddress: " + e.getMessage(), e);
         throw new JdbcStorageException("putNodeAddress: " + e.getMessage(), e);
      }
   }

   @Override
   public List<NodeAddress> loadNodeAddesses(int maxNum)
   {
      try (Connection dbConnection = getDbConnection(); PreparedStatement ps = dbConnection.prepareStatement(sqlGetNodeAddresses);
              ResultSet rs = ps.executeQuery())
      {
         List<NodeAddress> list = new LinkedList<>();
         while (rs.next())
            try
            {
               list.add(new NodeAddress(rs.getLong("services"),
                       new InetSocketAddress(InetAddress.getByName(rs.getString("address")), rs.getInt("port"))));
            } catch (UnknownHostException ex)
            {
               logger.info("Could not create node address from stored address: " + rs.getString("address") + " port: ", rs.getInt("port"));
            }
         return list;
      } catch (SQLException e)
      {
         logger.error("getNodeAddesses: " + e.getMessage(), e);
         throw new JdbcStorageException("getNodeAddesses: " + e.getMessage(), e);
      }
   }
}
