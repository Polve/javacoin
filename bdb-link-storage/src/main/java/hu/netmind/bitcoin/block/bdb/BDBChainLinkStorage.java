/**
 * Copyright (C) 2011 NetMind Consulting Bt.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package hu.netmind.bitcoin.block.bdb;

import hu.netmind.bitcoin.block.BlockChainLinkStorage;
import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.ScriptFactory;
import hu.netmind.bitcoin.TransactionInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.collections.TransactionRunner;
import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

/**
 * Storing block link information using Berkeley DB. Link will be stored in denormalized
 * "dump" format as there is no need to make advanced ad-hoc queries on the structure. Secondary
 * indexes are defined for the small number of query use-cases defined by the storage interface.
 * @author Robert Brautigam
 */
public class BDBChainLinkStorage implements BlockChainLinkStorage
{
   private static final boolean DEFAULT_AUTOCREATE = true;
   private static final boolean DEFAULT_TRANSACTIONAL = true;
   private static final String DEFAULT_DB_PATH = "./bitcoin-db";
   private static final String LINK_DB_NAME = "link";
   private static Logger logger = LoggerFactory.getLogger(BDBChainLinkStorage.class);

   private boolean autoCreate = DEFAULT_AUTOCREATE;
   private boolean transactional = DEFAULT_TRANSACTIONAL;
   private String dbPath = DEFAULT_DB_PATH;

   private ScriptFactory scriptFactory = null;
   private Environment environment = null;
   private Database linkDatabase = null;
   private TransactionRunner runner = null;

   private StoredMap<byte[],BlockChainLink> links = null;

   public BDBChainLinkStorage(ScriptFactory scriptFactory)
   {
      this.scriptFactory=scriptFactory;
      readConfiguration();
   }

   private void readConfiguration()
   {
      try
      {
         ResourceBundle config = ResourceBundle.getBundle("bdb-link-storage");
         autoCreate = Boolean.valueOf(config.getString("storage.bdb.autocreate"));
         transactional = Boolean.valueOf(config.getString("storage.bdb.transactional"));
         dbPath = config.getString("storage.bdb.db_path");
      } catch ( MissingResourceException e ) {
         logger.warn("could not read configuration for bdb link storage, using some default values",e);
      }
   }

   /**
    * To use the storage, it must be first initialized by calling this method.
    */
   public void init()
   {
      initializeEnvironment();
      initializeDatabases();
      initializeViews();
   }

   private void initializeEnvironment()
   {
      EnvironmentConfig environmentConfig = new EnvironmentConfig();
      environmentConfig.setAllowCreate(autoCreate);
      environmentConfig.setTransactional(transactional);
      environment = new Environment(new File(dbPath), environmentConfig);
      runner = new TransactionRunner(environment);
   }

   private void initializeDatabases()
   {
      DatabaseConfig databaseConfig = new DatabaseConfig();
      databaseConfig.setAllowCreate(autoCreate);
      databaseConfig.setTransactional(transactional);
      linkDatabase = environment.openDatabase(null, LINK_DB_NAME, databaseConfig);
   }

   private void initializeViews()
   {
      LinkBinding linkBinding = new LinkBinding(scriptFactory);
      BytesBinding bytesBinding = new BytesBinding();
      links = new StoredMap(linkDatabase,bytesBinding,linkBinding,true);
   }

   /**
    * Close the connection to BDB.
    */
   public void close()
   {
      if ( linkDatabase != null )
         linkDatabase.close();
      if ( environment != null )
         environment.close();
   }

   private <T> T executeWork(ReturnTransactionWorker<T> worker)
   {
      try
      {
         runner.run(worker);
         return worker.getReturnValue();
      } catch ( RuntimeException e ) {
         throw e;
      } catch ( Exception e ) {
         throw new BDBStorageException("unexpected error running work",e);
      }
   }

   public BlockChainLink getGenesisLink()
   {
      throw new BDBStorageException("not implemented");
   }

   public BlockChainLink getLastLink()
   {
      throw new BDBStorageException("not implemented");
   }

   public BlockChainLink getLink(final byte[] hash)
   {
      return executeWork(new ReturnTransactionWorker<BlockChainLink>() {
            public BlockChainLink doReturnWork()
            {
               return links.get(hash);
            }
         });
   }

   public List<BlockChainLink> getNextLinks(byte[] hash)
   {
      throw new BDBStorageException("not implemented");
   }

   public BlockChainLink getClaimedLink(BlockChainLink link, TransactionInput in)
   {
      throw new BDBStorageException("not implemented");
   }

   public BlockChainLink getClaimerLink(BlockChainLink link, TransactionInput in)
   {
      throw new BDBStorageException("not implemented");
   }

   public void addLink(final BlockChainLink link)
   {
      executeWork(new ReturnTransactionWorker() {
            public void doWork()
            {
               links.put(link.getBlock().getHash(),link);
            }
         });
   }

   public void updateLink(BlockChainLink link)
   {
      addLink(link);
   }

   public boolean getAutoCreate()
   {
      return autoCreate;
   }
   public void setAutoCreate(boolean autoCreate)
   {
      this.autoCreate=autoCreate;
   }

   public boolean getTransactional()
   {
      return transactional;
   }
   public void setTransactional(boolean transactional)
   {
      this.transactional=transactional;
   }

   public String getDbPath()
   {
      return dbPath;
   }
   public void setDbPath(String dbPath)
   {
      this.dbPath=dbPath;
   }

}

