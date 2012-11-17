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
package it.nibbles.javacoin.storage.jdbc;

import javax.sql.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

/**
 *
 * @author Alessandro Polverini
 */
public class DatasourceUtils
{

   public static DataSource getMysqlDatasource(String url, String user, String pw)
           throws ClassNotFoundException
   {
      PoolProperties p = new PoolProperties();
      p.setDriverClassName("com.mysql.jdbc.Driver");
      p.setUrl(url);
      p.setUsername(user);
      p.setPassword(pw);
      p.setJmxEnabled(false);
      p.setTestWhileIdle(false);
      p.setTestOnBorrow(true);
      p.setValidationQuery("SELECT 1");
      p.setTestOnReturn(false);
      p.setValidationInterval(30000);
      p.setTimeBetweenEvictionRunsMillis(30000);
      p.setMaxActive(100);
      p.setInitialSize(6);
      p.setMaxWait(10000);
      p.setRemoveAbandonedTimeout(60);
      p.setMinEvictableIdleTimeMillis(30000);
      p.setMinIdle(10);
      p.setLogAbandoned(true);
      p.setRemoveAbandoned(true);
      org.apache.tomcat.jdbc.pool.DataSource datasource = new org.apache.tomcat.jdbc.pool.DataSource(p);
      return datasource;
      /*
       try
       {
       ComboPooledDataSource cpds = new ComboPooledDataSource();
       cpds.setDriverClass("com.mysql.jdbc.Driver");
       cpds.setJdbcUrl(url);
       cpds.setUser(user);
       cpds.setPassword(pw);
       cpds.setMaxStatements(100);
       cpds.setMinPoolSize(4);
       cpds.setAcquireIncrement(2);
       cpds.setMaxPoolSize(64);
       return cpds;
       } catch (PropertyVetoException ex)
       {
       return null;
       }
       */
   }
//   public static DataSource getEmbeddedDerbyDatasource(String databaseName) throws ClassNotFoundException
//   {
//      Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
//      EmbeddedConnectionPoolDataSource40 ds = new EmbeddedConnectionPoolDataSource40();
//      ds.setDatabaseName(databaseName);
//      return ds;
//   }
}
