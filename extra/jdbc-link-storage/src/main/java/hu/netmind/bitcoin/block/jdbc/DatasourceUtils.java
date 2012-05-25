/**
 * Copyright (C) 2012 nibbles.it
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTAB ILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 0211 1-13 07 USA
 */
package hu.netmind.bitcoin.block.jdbc;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import javax.sql.DataSource;
import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource40;

/**
 *
 * @author Alessandro Polverini
 */
public class DatasourceUtils
{

   public static DataSource getMysqlDatasource(String url, String user, String pw) throws ClassNotFoundException
   {
      Class.forName("com.mysql.jdbc.Driver");
      MysqlConnectionPoolDataSource ds = new MysqlConnectionPoolDataSource();
      ds.setURL(url);
      ds.setUser(user);
      ds.setPassword(pw);
      return ds;
   }

   public static DataSource getEmbeddedDerbyDatasource(String databaseName) throws ClassNotFoundException
   {
      Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
      EmbeddedConnectionPoolDataSource40 ds = new EmbeddedConnectionPoolDataSource40();
      ds.setDatabaseName(databaseName);
      return ds;
   }
}
