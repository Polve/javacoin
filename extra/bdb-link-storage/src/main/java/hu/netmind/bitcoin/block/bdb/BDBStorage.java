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
package hu.netmind.bitcoin.block.bdb;

import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.block.Difficulty;
import hu.netmind.bitcoin.TransactionInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.collections.TransactionRunner;
import hu.netmind.bitcoin.BitcoinException;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.block.BaseChainLinkStorage;
import hu.netmind.bitcoin.block.BitcoinFactory;
import hu.netmind.bitcoin.block.SimplifiedStoredBlock;
import hu.netmind.bitcoin.block.TransactionImpl;
import hu.netmind.bitcoin.block.TransactionInputImpl;
import hu.netmind.bitcoin.block.TransactionOutputImpl;
import it.nibbles.bitcoin.utils.BtcUtil;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

/**
 * Storing block and transaction information using Berkeley DB. The storage is
 * not completely denormalized because we have a limited use case. Secondary
 * indexes are defined for the small number of query use-cases defined by the
 * storage interface.
 *
 * @author Alessandro Polverini
 */
public class BDBStorage extends BaseChainLinkStorage {

  private static final boolean DEFAULT_AUTOCREATE = true;
  private static final boolean DEFAULT_TRANSACTIONAL = true;
  private static final String DEFAULT_DB_PATH = "./test-db";
  private static final String LINK_DB_NAME = "link";
  private static final String NEXTHASH_DB_NAME = "nexthash-index";
  private static final String DIFFICULTY_DB_NAME = "difficulty-index";
  private static final String TXHASH_DB_NAME = "txhash-index";
  private static final String CLAIM_DB_NAME = "claim-index";
  private static final String HEIGHT_DB_NAME = "height-index";
  private static Logger logger = LoggerFactory.getLogger(BDBStorage.class);
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
  private StoredMap<byte[], BlockChainLink> links = null;
  private StoredMap<byte[], StoredLink> nextLinks = null;
  private StoredSortedMap<Difficulty, StoredLink> difficultyLinks = null;
  private StoredMap<byte[], StoredLink> txhashLinks = null;
  private StoredMap<Claim, StoredLink> claimLinks = null;
  private StoredMap<Long, StoredLink> heightLinks = null;

  public BDBStorage(BitcoinFactory bitcoinFactory) {
    this.bitcoinFactory = bitcoinFactory;
    readConfiguration();
  }

  private void readConfiguration() {
    try {
      ResourceBundle config = ResourceBundle.getBundle("bdb-link-storage");
      autoCreate = Boolean.valueOf(config.getString("storage.bdb.autocreate"));
      transactional = Boolean.valueOf(config.getString("storage.bdb.transactional"));
      dbPath = config.getString("storage.bdb.db_path");
    } catch (MissingResourceException e) {
      logger.warn("could not read configuration for bdb link storage, using some default values", e);
    }
  }

  /**
   * To use the storage, it must be first initialized by calling this method.
   */
  public void init() {
    initializeEnvironment();
    initializeDatabases();
    initializeViews();
  }

  private void initializeEnvironment() {
    EnvironmentConfig environmentConfig = new EnvironmentConfig();
    environmentConfig.setAllowCreate(autoCreate);
    environmentConfig.setTransactional(transactional);
    File dbFile = new File(dbPath);
    if (autoCreate)
      dbFile.mkdirs();
    environment = new Environment(dbFile, environmentConfig);
    runner = new TransactionRunner(environment);
  }

  private void initializeDatabases() {
    // Main
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setAllowCreate(autoCreate);
    databaseConfig.setTransactional(transactional);
    linkDatabase = environment.openDatabase(null, LINK_DB_NAME, databaseConfig);
//    // Next hash index
//    SecondaryConfig secondaryConfig = new SecondaryConfig();
//    secondaryConfig.setAllowCreate(autoCreate);
//    secondaryConfig.setTransactional(transactional);
//    secondaryConfig.setSortedDuplicates(true);
//    secondaryConfig.setKeyCreator(new NextHashIndexCreator(bitcoinFactory));
//    nexthashDatabase = environment.openSecondaryDatabase(null, NEXTHASH_DB_NAME, linkDatabase, secondaryConfig);
//    // Difficulty index
//    secondaryConfig = new SecondaryConfig();
//    secondaryConfig.setAllowCreate(autoCreate);
//    secondaryConfig.setTransactional(transactional);
//    secondaryConfig.setSortedDuplicates(true);
//    secondaryConfig.setKeyCreator(new DifficultyIndexCreator(bitcoinFactory));
//    secondaryConfig.setBtreeComparator(new DifficultyComparator(bitcoinFactory));
//    difficultyDatabase = environment.openSecondaryDatabase(null, DIFFICULTY_DB_NAME, linkDatabase, secondaryConfig);
//    // Txhash index
//    secondaryConfig = new SecondaryConfig();
//    secondaryConfig.setAllowCreate(autoCreate);
//    secondaryConfig.setTransactional(transactional);
//    secondaryConfig.setSortedDuplicates(true);
//    secondaryConfig.setMultiKeyCreator(new TxHashIndexCreator(bitcoinFactory));
//    txhashDatabase = environment.openSecondaryDatabase(null, TXHASH_DB_NAME, linkDatabase, secondaryConfig);
//    // Claims index
//    secondaryConfig = new SecondaryConfig();
//    secondaryConfig.setAllowCreate(autoCreate);
//    secondaryConfig.setTransactional(transactional);
//    secondaryConfig.setSortedDuplicates(true);
//    secondaryConfig.setMultiKeyCreator(new ClaimIndexCreator(bitcoinFactory));
//    claimDatabase = environment.openSecondaryDatabase(null, CLAIM_DB_NAME, linkDatabase, secondaryConfig);
//    // Hight index
//    secondaryConfig = new SecondaryConfig();
//    secondaryConfig.setAllowCreate(autoCreate);
//    secondaryConfig.setTransactional(transactional);
//    secondaryConfig.setSortedDuplicates(true);
//    secondaryConfig.setKeyCreator(new HeightIndexCreator(bitcoinFactory));
//    heightDatabase = environment.openSecondaryDatabase(null, HEIGHT_DB_NAME, linkDatabase, secondaryConfig);
  }

  private void initializeViews() {
    BlockChainHeaderBinding blocksBinding = new BlockChainHeaderBinding(bitcoinFactory);
    BytesBinding hashBinding = new BytesBinding();
    links = new StoredMap(linkDatabase, hashBinding, blocksBinding, true);
//    nextLinks = new StoredMap(nexthashDatabase, bytesBinding, linkBinding, false);
//    difficultyLinks = new StoredSortedMap(difficultyDatabase, new DifficultyBinding(bitcoinFactory), linkBinding, false);
//    txhashLinks = new StoredMap(txhashDatabase, bytesBinding, linkBinding, false);
//    claimLinks = new StoredMap(claimDatabase, new ClaimBinding(), linkBinding, false);
//    heightLinks = new StoredMap(heightDatabase, new LongBinding(), linkBinding, false);
  }

  /**
   * Close the connection to BDB.
   */
  public void close() {
//    if (claimDatabase != null)
//      claimDatabase.close();
//    if (txhashDatabase != null)
//      txhashDatabase.close();
//    if (difficultyDatabase != null)
//      difficultyDatabase.close();
//    if (nexthashDatabase != null)
//      nexthashDatabase.close();
//    if (heightDatabase != null)
//      heightDatabase.close();
    if (linkDatabase != null)
      linkDatabase.close();
    if (environment != null)
      environment.close();
  }

  public void storeBlockHeader(BlockChainLink link) throws SQLException {
    links.put(link.getBlock().getHash(), link);
  }

  public void printBlockHeaders() {
    for (byte[] hash : links.keySet()) {
      System.out.println("Stored hash: "+BtcUtil.hexOut(hash));
      System.out.println("block: "+links.get(hash));
    }
  }

  private <T> T executeWork(ReturnTransactionWorker<T> worker) {
    try {
      runner.run(worker);
      return worker.getReturnValue();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new BDBStorageException("unexpected error running work", e);
    }
  }

  @Override
  protected Connection newConnection() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected long storeBlockHeader(Connection connection, BlockChainLink link) throws SQLException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public BlockChainLink getLinkBlockHeader(byte[] hash) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public BlockChainLink getLinkAtHeight(long height) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public BlockChainLink getPartialClaimedLink(BlockChainLink link, TransactionInput in) {
    return getClaimedLink(link, in);
  }

  @Override
  public boolean outputClaimedInSameBranch(BlockChainLink link, TransactionInput in) {
    return getClaimerLink(link, in) != null;
  }

  @Override
  public byte[] getHashOfMainChainAtHeight(long height) {
    BlockChainLink lastLink = getLastLink();
    if (lastLink == null)
      return null;
    return lastLink.getBlock().getHash();
  }

  @Override
  public boolean blockExists(byte[] hash) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected boolean blockExists(Connection dbConnection, byte[] hash) throws SQLException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected List<SimplifiedStoredBlock> getBlocksAtHeight(Connection connection, long height) throws SQLException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected List<SimplifiedStoredBlock> getBlocksReferringTx(Connection connection, TransactionInput in) throws SQLException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected List<SimplifiedStoredBlock> getBlocksWithTx(Connection connection, byte[] hash) throws SQLException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected List<SimplifiedStoredBlock> getBlocksWithPrevHash(Connection connection, byte[] hash) throws SQLException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected int getNumBlocksAtHeight(Connection connection, long height) throws SQLException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected int getNumBlocksWithPrevHash(Connection connection, byte[] hash) throws SQLException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected long storeTransaction(Connection connection, Transaction tx) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected void storeBlkTxLink(Connection connection, long blockId, long txId, int pos) throws SQLException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected List<TransactionInputImpl> loadTxInputs(Connection connection, long txId) throws SQLException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected List<TransactionOutputImpl> loadTxOutputs(Connection connection, long txId) throws SQLException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected long getTransactionId(Connection connection, byte[] hash) throws SQLException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected TransactionImpl getTransaction(Connection connection, byte[] hash) throws SQLException, BitcoinException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected List<TransactionImpl> getBlockTransactions(Connection connection, ResultSet rs) throws SQLException, BitcoinException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected List<TransactionImpl> getBlockTransactions(Connection connection, byte[] hash) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected List<TransactionImpl> getBlockTransactions(Connection connection, long blockId) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected BlockChainLink createBlockWithTxs(Connection connection, byte[] hash, List<TransactionImpl> transactions) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected SimplifiedStoredBlock getSimplifiedStoredBlock(Connection connection, byte[] hash) throws SQLException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected SimplifiedStoredBlock getHigherWorkHash(Connection connection) throws SQLException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public boolean getAutoCreate() {
    return autoCreate;
  }

  public void setAutoCreate(boolean autoCreate) {
    this.autoCreate = autoCreate;
  }

  public boolean getTransactional() {
    return transactional;
  }

  public void setTransactional(boolean transactional) {
    this.transactional = transactional;
  }

  public String getDbPath() {
    return dbPath;
  }

  public void setDbPath(String dbPath) {
    this.dbPath = dbPath;
  }
}
