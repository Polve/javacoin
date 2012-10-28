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
import hu.netmind.bitcoin.block.Difficulty;
import hu.netmind.bitcoin.ScriptFactory;
import hu.netmind.bitcoin.TransactionInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.collections.TransactionRunner;
import hu.netmind.bitcoin.block.BitcoinFactory;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
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
   private static final String NEXTHASH_DB_NAME = "nexthash-index";
   private static final String DIFFICULTY_DB_NAME = "difficulty-index";
   private static final String TXHASH_DB_NAME = "txhash-index";
   private static final String CLAIM_DB_NAME = "claim-index";
   private static final String HEIGHT_DB_NAME = "height-index";
   private static Logger logger = LoggerFactory.getLogger(BDBChainLinkStorage.class);

   private boolean autoCreate = DEFAULT_AUTOCREATE;
   private boolean transactional = DEFAULT_TRANSACTIONAL;
   private String dbPath = DEFAULT_DB_PATH;

   private BitcoinFactory bitcoinFactory = null;
   private Environment environment = null;
   private Database linkDatabase = null;
   private SecondaryDatabase nexthashDatabase = null;
   private SecondaryDatabase difficultyDatabase = null;
   private SecondaryDatabase txhashDatabase = null;
   private SecondaryDatabase claimDatabase = null;
   private SecondaryDatabase heightDatabase = null;
   private TransactionRunner runner = null;

   private StoredMap<byte[],StoredLink> links = null;
   private StoredMap<byte[],StoredLink> nextLinks = null;
   private StoredSortedMap<Difficulty,StoredLink> difficultyLinks = null;
   private StoredMap<byte[],StoredLink> txhashLinks = null;
   private StoredMap<Claim,StoredLink> claimLinks = null;
   private StoredMap<Long,StoredLink> heightLinks = null;

   public BDBChainLinkStorage(BitcoinFactory bitcoinFactory)
   {
      this.bitcoinFactory = bitcoinFactory;
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
      File dbFile = new File(dbPath);
      if ( autoCreate )
         dbFile.mkdirs();
      environment = new Environment(dbFile, environmentConfig);
      runner = new TransactionRunner(environment);
   }

   private void initializeDatabases()
   {
      // Main
      DatabaseConfig databaseConfig = new DatabaseConfig();
      databaseConfig.setAllowCreate(autoCreate);
      databaseConfig.setTransactional(transactional);
      linkDatabase = environment.openDatabase(null, LINK_DB_NAME, databaseConfig);
      // Next hash index
      SecondaryConfig secondaryConfig = new SecondaryConfig();
      secondaryConfig.setAllowCreate(autoCreate);
      secondaryConfig.setTransactional(transactional);
      secondaryConfig.setSortedDuplicates(true);
      secondaryConfig.setKeyCreator(new NextHashIndexCreator(bitcoinFactory));
      nexthashDatabase = environment.openSecondaryDatabase(null, NEXTHASH_DB_NAME, linkDatabase, secondaryConfig);
      // Difficulty index
      secondaryConfig = new SecondaryConfig();
      secondaryConfig.setAllowCreate(autoCreate);
      secondaryConfig.setTransactional(transactional);
      secondaryConfig.setSortedDuplicates(true);
      secondaryConfig.setKeyCreator(new DifficultyIndexCreator(bitcoinFactory));
      secondaryConfig.setBtreeComparator(new DifficultyComparator(bitcoinFactory));
      difficultyDatabase = environment.openSecondaryDatabase(null, DIFFICULTY_DB_NAME, linkDatabase, secondaryConfig);
      // Txhash index
      secondaryConfig = new SecondaryConfig();
      secondaryConfig.setAllowCreate(autoCreate);
      secondaryConfig.setTransactional(transactional);
      secondaryConfig.setSortedDuplicates(true);
      secondaryConfig.setMultiKeyCreator(new TxHashIndexCreator(bitcoinFactory));
      txhashDatabase = environment.openSecondaryDatabase(null, TXHASH_DB_NAME, linkDatabase, secondaryConfig);
      // Claims index
      secondaryConfig = new SecondaryConfig();
      secondaryConfig.setAllowCreate(autoCreate);
      secondaryConfig.setTransactional(transactional);
      secondaryConfig.setSortedDuplicates(true);
      secondaryConfig.setMultiKeyCreator(new ClaimIndexCreator(bitcoinFactory));
      claimDatabase = environment.openSecondaryDatabase(null, CLAIM_DB_NAME, linkDatabase, secondaryConfig);
      // Hight index
      secondaryConfig = new SecondaryConfig();
      secondaryConfig.setAllowCreate(autoCreate);
      secondaryConfig.setTransactional(transactional);
      secondaryConfig.setSortedDuplicates(true);
      secondaryConfig.setKeyCreator(new HeightIndexCreator(bitcoinFactory));
      heightDatabase = environment.openSecondaryDatabase(null, HEIGHT_DB_NAME, linkDatabase, secondaryConfig);
   }

   private void initializeViews()
   {
      LinkBinding linkBinding = new LinkBinding(bitcoinFactory);
      BytesBinding bytesBinding = new BytesBinding();
      links = new StoredMap(linkDatabase,bytesBinding,linkBinding,true);
      nextLinks = new StoredMap(nexthashDatabase,bytesBinding,linkBinding,false);
      difficultyLinks = new StoredSortedMap(difficultyDatabase,new DifficultyBinding(bitcoinFactory),linkBinding,false);
      txhashLinks = new StoredMap(txhashDatabase,bytesBinding,linkBinding,false);
      claimLinks = new StoredMap(claimDatabase,new ClaimBinding(),linkBinding,false);
      heightLinks = new StoredMap(heightDatabase,new LongBinding(),linkBinding,false);
   }

   /**
    * Close the connection to BDB.
    */
   public void close()
   {
      if ( claimDatabase != null )
         claimDatabase.close();
      if ( txhashDatabase != null )
         txhashDatabase.close();
      if ( difficultyDatabase != null )
         difficultyDatabase.close();
      if ( nexthashDatabase != null )
         nexthashDatabase.close();
      if ( linkDatabase != null )
         linkDatabase.close();
      if ( heightDatabase != null )
         heightDatabase.close();
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
      return executeWork(new ReturnTransactionWorker<BlockChainLink>() {
            public BlockChainLink doReturnWork()
            {
               if ( difficultyLinks.isEmpty() )
                  return null;
               StoredLink link = difficultyLinks.get(difficultyLinks.firstKey());
               if ( link == null )
                  return null;
               if ( link.getLink().getHeight() != BlockChainLink.ROOT_HEIGHT )
                  throw new BDBStorageException("genesis link in database did not have height of "+BlockChainLink.ROOT_HEIGHT);
               return link.getLink();
            }
         });
   }

   public BlockChainLink getLastLink()
   {
      return executeWork(new ReturnTransactionWorker<BlockChainLink>() {
            public BlockChainLink doReturnWork()
            {
               if ( difficultyLinks.isEmpty() )
                  return null;
               StoredLink link = difficultyLinks.get(difficultyLinks.lastKey());
               if ( link == null )
                  return null;
               return link.getLink();
            }
         });
   }

   public long getHeight()
   {
      return getLastLink().getHeight();
   }

   public StoredLink getStoredLink(final byte[] hash)
   {
      return executeWork(new ReturnTransactionWorker<StoredLink>() {
            public StoredLink doReturnWork()
            {
               return links.get(hash);
            }
         });
   }

   public BlockChainLink getLink(final byte[] hash)
   {
      StoredLink link = getStoredLink(hash);
      if ( link == null )
         return null;
      return link.getLink();
   }

   private List<StoredLink> getNextStoredLinks(final byte[] hash)
   {
      return executeWork(new ReturnTransactionWorker<List<StoredLink>>() {
            public List<StoredLink> doReturnWork()
            {
               return new LinkedList(nextLinks.duplicates(hash));
            }
         });
   }

   public List<BlockChainLink> getNextLinks(final byte[] hash)
   {
      List<BlockChainLink> chainLinks = new LinkedList<BlockChainLink>();
      for ( StoredLink link : getNextStoredLinks(hash) )
         chainLinks.add(link.getLink());
      return chainLinks;
   }

   public BlockChainLink getNextLink(final byte[] current, final byte[] target)
   {
      StoredLink targetLink = getStoredLink(target);
      if ( (targetLink == null) || (targetLink.getLink().isOrphan()) )
         return null;
      for ( StoredLink candidate : getNextStoredLinks(current) )
         if (candidate.getPath().isPrefix(targetLink.getPath()))
            return candidate.getLink();
      return null;
   }

   public boolean isReachable(final byte[] target, final byte[] source)
   {
      return executeWork(new ReturnTransactionWorker<Boolean>() {
            public Boolean doReturnWork()
            {
               StoredLink targetLink = getStoredLink(target);
               StoredLink sourceLink = getStoredLink(source);
               if ( (targetLink==null) || (sourceLink==null) || 
                  (targetLink.getLink().isOrphan()) || (sourceLink.getLink().isOrphan()) )
                  return false;
               return targetLink.getPath().isPrefix(sourceLink.getPath());
            }
         });
   }

   public BlockChainLink getCommonLink(final byte[] first, final byte[] second)
   {
      return executeWork(new ReturnTransactionWorker<BlockChainLink>() {
            public BlockChainLink doReturnWork()
            {
               StoredLink firstLink = getStoredLink(first);
               StoredLink secondLink = getStoredLink(second);
               if ( (firstLink==null) || (secondLink==null) || 
                  (firstLink.getLink().isOrphan()) || (secondLink.getLink().isOrphan()) )
                  return null;
               // Determine when the paths cross
               Path commonPath = Path.getCommonPath(firstLink.getPath(),secondLink.getPath());
               if ( commonPath == null )
                  return null;
               // Get the links at the specified height
               Collection<StoredLink> potentialLinks = heightLinks.duplicates(commonPath.getHeight());
               for ( StoredLink potentialLink : potentialLinks )
                  if ( firstLink.getPath().isPrefix(potentialLink.getPath()) )
                     return potentialLink.getLink();
               return null;
            }
         });
   }

   public BlockChainLink getClaimedLink(final BlockChainLink link, final TransactionInput in)
   {
      return executeWork(new ReturnTransactionWorker<BlockChainLink>() {
            public BlockChainLink doReturnWork()
            {
               StoredLink branchLink = getStoredLink(link.getBlock().getHash());
               if ( branchLink == null )
                  throw new BDBStorageException("branch link not found in database: "+link);
               Collection<StoredLink> potentialLinks = txhashLinks.duplicates(in.getClaimedTransactionHash());
               for ( StoredLink potentialLink : potentialLinks )
                  if ( branchLink.getPath().isPrefix(potentialLink.getPath()) )
                     return potentialLink.getLink();
               return null;
            }
         });
   }

   public BlockChainLink getClaimerLink(final BlockChainLink link, final TransactionInput in)
   {
      return executeWork(new ReturnTransactionWorker<BlockChainLink>() {
            public BlockChainLink doReturnWork()
            {
               StoredLink branchLink = getStoredLink(link.getBlock().getHash());
               if ( branchLink == null )
                  throw new BDBStorageException("branch link not found in database: "+link);
               Collection<StoredLink> potentialLinks = claimLinks.duplicates(
                  new Claim(in.getClaimedTransactionHash(),in.getClaimedOutputIndex()));
               for ( StoredLink potentialLink : potentialLinks )
                  if ( branchLink.getPath().isPrefix(potentialLink.getPath()) )
                     return potentialLink.getLink();
               return null;
            }
         });
   }

   public synchronized void addLink(final BlockChainLink link)
   {
      executeWork(new ReturnTransactionWorker() {
            public void doWork()
            {
               StoredLink previousLink = getStoredLink(link.getBlock().getPreviousBlockHash());
               if ( (previousLink==null) && (!link.isOrphan()) && (link.getHeight()!=BlockChainLink.ROOT_HEIGHT) )
                  throw new BDBStorageException("could not find previous link on add, but link added is not marked as orphan: "+link);
               Path path = new Path();
               if ( previousLink != null )
               {
                  List<BlockChainLink> nextLinks = getNextLinks(previousLink.getLink().getBlock().getHash());
                  path = previousLink.getPath().createPath(nextLinks.size());
               }
               links.put(link.getBlock().getHash(),new StoredLink(link,path));
            }
         });
   }

  @Override
  public BlockChainLink getLinkAtHeight(long height) {
    throw new UnsupportedOperationException("Not supported yet.");
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

   @Override
   public BlockChainLink getPartialClaimedLink(BlockChainLink link, TransactionInput in)
   {
      return getClaimedLink(link, in);
   }

   @Override
   public boolean outputClaimedInSameBranch(BlockChainLink link, TransactionInput in)
   {
      return getClaimerLink(link, in) != null;
   }

   @Override
   public byte[] getHashOfMainChainAtHeight(long height)
   {
      BlockChainLink lastLink = getLastLink();
      if (lastLink == null)
         return null;
      return lastLink.getBlock().getHash();
   }

}
