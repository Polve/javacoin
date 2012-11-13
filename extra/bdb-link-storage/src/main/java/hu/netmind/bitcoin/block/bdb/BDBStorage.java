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

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.collections.CurrentTransaction;
import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.TransactionInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.collections.TransactionRunner;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.TransactionConfig;
import hu.netmind.bitcoin.BitcoinException;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.block.BaseChainLinkStorage;
import hu.netmind.bitcoin.block.BitcoinFactory;
import hu.netmind.bitcoin.block.BlockImpl;
import hu.netmind.bitcoin.block.Difficulty;
import hu.netmind.bitcoin.block.SimplifiedStoredBlock;
import hu.netmind.bitcoin.block.StorageSession;
import hu.netmind.bitcoin.block.TransactionImpl;
import it.nibbles.bitcoin.utils.BtcUtil;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
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
  //private static final boolean DEFAULT_TRANSACTIONAL = true;
  private static final String DEFAULT_DB_PATH = "./test-db";
  private static Logger logger = LoggerFactory.getLogger(BDBStorage.class);
  private boolean autoCreate = DEFAULT_AUTOCREATE;
  private String dbPath = DEFAULT_DB_PATH;
  private BitcoinFactory bitcoinFactory = null;
  private Environment environment = null;
  private TransactionRunner runner = null;
  private Database blockHeadersDatabase = null;
  private Database txDatabase = null;
  private Database blockTxDatabase = null;
  private Database claimDatabase = null;
  private Database txBlockDatabase = null;
  private SecondaryDatabase heightDatabase = null;
  private SecondaryDatabase prevHashDatabase = null;
  private SecondaryDatabase difficultyDatabase = null;
  private StoredMap<byte[], BlockChainLink> blockHeaders = null;
  private StoredMap<byte[], Transaction> transactions = null;
  private StoredMap<byte[], List<byte[]>> blockTxRelationship = null;
  private StoredMap<Integer, BlockChainLink> heightIndex = null;
  private StoredMap<Claim, byte[]> claimedTxToBlockHash = null;
  private StoredMap<byte[], BlockChainLink> prevHashIndex = null;
  private StoredSortedMap<Difficulty, BlockChainLink> difficultyIndex = null;
  private StoredMap<byte[], byte[]> txBlockRelationship = null;

  public BDBStorage(BitcoinFactory bitcoinFactory) {
    this.bitcoinFactory = bitcoinFactory;
    readConfiguration();
  }

  private void readConfiguration() {
    try {
      ResourceBundle config = ResourceBundle.getBundle("bdb-link-storage");
      autoCreate = Boolean.valueOf(config.getString("storage.bdb.autocreate"));
      //transactional = Boolean.valueOf(config.getString("storage.bdb.transactional"));
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
    environmentConfig.setTransactional(getTransactional());
    File dbFile = new File(dbPath);
    if (autoCreate)
      dbFile.mkdirs();
    environment = new Environment(dbFile, environmentConfig);
    runner = new TransactionRunner(environment);
  }

  private void initializeDatabases() {
    // Main
    DatabaseConfig nodupsDbConfig = new DatabaseConfig();
    nodupsDbConfig.setAllowCreate(autoCreate);
    nodupsDbConfig.setTransactional(getTransactional());

    // Primary databases
    blockHeadersDatabase = environment.openDatabase(null, "blockHeader-db", nodupsDbConfig);
    txDatabase = environment.openDatabase(null, "tx-db", nodupsDbConfig);
    blockTxDatabase = environment.openDatabase(null, "blockHeader-tx-relation", nodupsDbConfig);

    DatabaseConfig dupsAllowedDbConfig = new DatabaseConfig();
    dupsAllowedDbConfig.setAllowCreate(autoCreate);
    dupsAllowedDbConfig.setSortedDuplicates(true);
    dupsAllowedDbConfig.setTransactional(getTransactional());
    claimDatabase = environment.openDatabase(null, "claim-blockHash-relation", dupsAllowedDbConfig);
    txBlockDatabase = environment.openDatabase(null, "tx-blockHash-relation", dupsAllowedDbConfig);

    // Secondary databases (indexes)

    // Height index
    SecondaryConfig secondaryConfig = new SecondaryConfig();
    secondaryConfig.setAllowCreate(autoCreate);
    secondaryConfig.setTransactional(getTransactional());
    secondaryConfig.setSortedDuplicates(true);
    secondaryConfig.setKeyCreator(new HeightIndexCreator(bitcoinFactory));
    heightDatabase = environment.openSecondaryDatabase(null, "height-index", blockHeadersDatabase, secondaryConfig);

    // Prev hash index
    secondaryConfig = new SecondaryConfig();
    secondaryConfig.setAllowCreate(autoCreate);
    secondaryConfig.setTransactional(getTransactional());
    secondaryConfig.setSortedDuplicates(true);
    secondaryConfig.setKeyCreator(new PrevHashIndexCreator(bitcoinFactory));
    prevHashDatabase = environment.openSecondaryDatabase(null, "prevhash-index", blockHeadersDatabase, secondaryConfig);

    // Difficulty index
    secondaryConfig = new SecondaryConfig();
    secondaryConfig.setAllowCreate(autoCreate);
    secondaryConfig.setTransactional(getTransactional());
    secondaryConfig.setSortedDuplicates(true);
    secondaryConfig.setKeyCreator(new DifficultyIndexCreator(bitcoinFactory));
    //secondaryConfig.setBtreeComparator(new DifficultyComparator(bitcoinFactory));
    difficultyDatabase = environment.openSecondaryDatabase(null, "difficulty-index", blockHeadersDatabase, secondaryConfig);

  }

  private void initializeViews() {
    BlockChainHeaderBinding blockHeaderBinding = new BlockChainHeaderBinding(bitcoinFactory);
    BytesBinding hashBinding = new BytesBinding();
    blockHeaders = new StoredMap(blockHeadersDatabase, hashBinding, blockHeaderBinding, true);
    transactions = new StoredMap(txDatabase, hashBinding, new TxBinding(bitcoinFactory), true);
    blockTxRelationship = new StoredMap(blockTxDatabase, hashBinding, new BlockTxBinding(), true);
    txBlockRelationship = new StoredSortedMap(txBlockDatabase, hashBinding, hashBinding, true);
    claimedTxToBlockHash = new StoredMap(claimDatabase, new ClaimBinding(), hashBinding, true);
    heightIndex = new StoredMap(heightDatabase, new IntegerBinding(), blockHeaderBinding, false);
    prevHashIndex = new StoredMap(prevHashDatabase, hashBinding, blockHeaderBinding, false);
    difficultyIndex = new StoredSortedMap(difficultyDatabase, new DifficultyBinding(bitcoinFactory), blockHeaderBinding, false);
  }

  /**
   * Close the connection to BDB.
   */
  public void close() {
    if (txBlockDatabase != null)
      txBlockDatabase.close();
    if (claimDatabase != null)
      claimDatabase.close();
    if (txDatabase != null)
      txDatabase.close();
    if (difficultyDatabase != null)
      difficultyDatabase.close();
    if (prevHashDatabase != null)
      prevHashDatabase.close();
    if (heightDatabase != null)
      heightDatabase.close();
    if (blockTxDatabase != null)
      blockTxDatabase.close();
    if (blockHeadersDatabase != null)
      blockHeadersDatabase.close();
    if (environment != null)
      environment.close();
  }

  public class StorageSessionImpl implements StorageSession {
    com.sleepycat.je.Transaction transaction;

    public StorageSessionImpl(com.sleepycat.je.Transaction transaction) {
      this.transaction = transaction;
    }

    @Override
    public void commit() {
      if (transaction != null)
        transaction.commit();
    }

    @Override
    public void rollback() {
      transaction.abort();
    }
  }

  public StorageSession newStorageSession(boolean forWriting) {
    return new StorageSessionImpl(forWriting ? environment.beginTransaction(null, TransactionConfig.DEFAULT) : null);
  }

  // TODO eliminare
  public void storeTransaction(Transaction tx) {
    transactions.put(tx.getHash(), tx);
  }

  // TODO eliminare
  public Transaction getTransaction(byte[] hash) {
    return transactions.get(hash);
  }

  // TODO eliminare
  public void printClaims() {
    System.out.println("Numero claims: " + claimedTxToBlockHash.size());
    for (Claim claim : claimedTxToBlockHash.keySet()) {
      BlockChainLink block = blockHeaders.get(claimedTxToBlockHash.get(claim));
      System.out.println("[CLAIMS] block " + BtcUtil.hexOut(block.getBlock().getHash()) + " claims tx " + BtcUtil.hexOut(claim.getClaimedTransactionHash()) + "/" + claim.getClaimedOutputIndex());
    }
  }

  // TODO eliminare
  public void printBlockHeaders() {
    for (byte[] hash : blockHeaders.keySet()) {
      System.out.println("[BLOCKS] Stored hash: " + BtcUtil.hexOut(hash) + " block: " + blockHeaders.get(hash));
    }
  }

  // TODO eliminare
  public void printPrevHashes() {
    System.out.println("Numero prevHashes: " + prevHashIndex.size());
    for (byte[] hash : prevHashIndex.keySet()) {
      BlockChainLink storedBlock = prevHashIndex.get(hash);
      System.out.println("[PREVHASHES] block with prev hash: " + BtcUtil.hexOut(hash) + ": " + BtcUtil.hexOut(storedBlock.getBlock().getHash()));
    }
  }

  // TODO eliminare
  public void printDifficulty() throws SQLException {
    for (Entry<Difficulty, BlockChainLink> entrySet : difficultyIndex.entrySet()) {
      System.out.println("Difficulty: " + entrySet.getKey().getDifficulty().longValue() + " hash: " + BtcUtil.hexOut(entrySet.getValue().getBlock().getHash()));
    }
    System.out.println("Higher work: " + BtcUtil.hexOut(getHigherWorkHash(null).hash));
  }

  // TODO eliminare
  public void printTxBlockIndex() {
    txBlockRelationship.put(BtcUtil.hexIn("4A5E1E4BAAB89F3A32518A88C31BC87F618F76673E2CC77AB2127B7AFDEDA33B"), BtcUtil.hexIn("000000004EBADB55EE9096C9A2F8880E09DA59C0D68B1C228DA88E48844A1485"));
    System.out.println("Numero transazioni in txBlockIndex: " + txBlockRelationship.size());
    for (byte[] hash : txBlockRelationship.keySet()) {
      Collection<byte[]> blockHashes = txBlockRelationship.duplicates(hash);
      for (byte[] blockHash : blockHashes) {
        BlockChainLink storedBlock = blockHeaders.get(blockHash);
        System.out.println("[TX_HASH_INDEX] tx: " + BtcUtil.hexOut(hash) + ": " + BtcUtil.hexOut(storedBlock.getBlock().getHash()));
      }
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
    return null;
  }

  // TODO: Convertire a protected
  @Override
  public void storeBlockLink(Connection connection, BlockChainLink link) throws SQLException {
    blockHeaders.put(link.getBlock().getHash(), link);
    for (Transaction tx : link.getBlock().getTransactions()) {
      transactions.put(tx.getHash(), tx);
      txBlockRelationship.put(tx.getHash(), link.getBlock().getHash());
      if (!tx.isCoinbase())
        for (TransactionInput in : tx.getInputs()) {
          claimedTxToBlockHash.put(new Claim(in.getClaimedTransactionHash(), in.getClaimedOutputIndex()), link.getBlock().getHash());
        }
    }
    storeBlockTxRelation(link.getBlock());
  }

//  protected long storeBlockHeader(Connection connection, BlockChainLink link) throws SQLException {
//    DatabaseEntry key = new DatabaseEntry(link.getBlock().getHash());
//    DatabaseEntry val = new DatabaseEntry();
//    new BlockChainHeaderBinding(bitcoinFactory).objectToEntry(link, val);
//    blockHeadersDatabase.put(null, key, val);
//    return 0;
//  }
  protected void storeBlockTxRelation(Block block) {
    List<Transaction> txs = block.getTransactions();
    List<byte[]> txHashes = new ArrayList<>(txs.size());
    for (Transaction tx : txs) {
      txHashes.add(tx.getHash());
    }
    blockTxRelationship.put(block.getHash(), txHashes);
  }

  @Override
  protected boolean blockExists(Connection dbConnection, byte[] hash) throws SQLException {
    return blockHeaders.containsKey(hash);
  }

  @Override
  public BlockChainLink getLinkBlockHeader(byte[] hash) {
    return blockHeaders.get(hash);
  }

  @Override
  protected SimplifiedStoredBlock getSimplifiedStoredBlock(Connection connection, byte[] hash) throws SQLException {
    return new SimplifiedStoredBlock(blockHeaders.get(hash));
  }

  // TODO: Cambiare a protected
  // TODO: Eliminare del tutto i SimplifiedStoredBlock
  @Override
  public List<SimplifiedStoredBlock> getBlocksAtHeight(Connection connection, long height) throws SQLException {
    Collection<BlockChainLink> headers = heightIndex.duplicates((int) height);
    if (headers == null)
      return new ArrayList<>(0);
    List<SimplifiedStoredBlock> res = new ArrayList<>(headers.size());
    for (BlockChainLink b : headers)
      res.add(new SimplifiedStoredBlock(b));
    return res;
  }

  // Con BDB questo metodo non è più efficiente, verificare se cambiare l'implementazione base
  @Override
  protected int getNumBlocksAtHeight(Connection connection, long height) throws SQLException {
    return heightIndex.duplicates((int) height).size();
  }

  @Override
  protected BlockChainLink createBlockWithTxs(Connection connection, byte[] hash, List<TransactionImpl> transactions) {
    BlockChainLink storedBlock = blockHeaders.get(hash);
    if (storedBlock != null) {
      Block block = storedBlock.getBlock();
      try {
        return bitcoinFactory.newBlockChainLink(new BlockImpl(
                transactions, block.getCreationTime(), block.getNonce(), block.getCompressedTarget(),
                block.getPreviousBlockHash(), block.getMerkleRoot(), block.getHash(), block.getVersion()),
                new BigDecimal(storedBlock.getTotalDifficulty().getDifficulty().longValue()),
                storedBlock.getHeight());
      } catch (BitcoinException ex) {
        logger.error("Unexpected error while duplicating block");
      }
    }
    return null;
  }

  @Override
  protected List<TransactionImpl> getBlockTransactions(Connection connection, byte[] hash) {
    List<byte[]> hashes = blockTxRelationship.get(hash);
    if (hashes == null)
      return new ArrayList<>(0);
    List<TransactionImpl> txs = new ArrayList<>(hashes.size());
    for (byte[] h : hashes) {
      txs.add((TransactionImpl) transactions.get(h));
    }
    return txs;
  }

  @Override
  protected TransactionImpl getTransaction(Connection connection, byte[] hash) throws SQLException, BitcoinException {
    return (TransactionImpl) transactions.get(hash);
  }

  @Override
  protected List<SimplifiedStoredBlock> getBlocksReferringTx(Connection connection, TransactionInput in) throws SQLException {
    Collection<byte[]> hashes = claimedTxToBlockHash.duplicates(new Claim(in.getClaimedTransactionHash(), in.getClaimedOutputIndex()));
    if (hashes == null)
      return new ArrayList<>(0);
    List<SimplifiedStoredBlock> res = new ArrayList<>(hashes.size());
    for (byte[] h : hashes)
      res.add(new SimplifiedStoredBlock(blockHeaders.get(h)));
    return res;
  }

  @Override
  protected List<SimplifiedStoredBlock> getBlocksWithPrevHash(Connection connection, byte[] hash) throws SQLException {
    Collection<BlockChainLink> headers = prevHashIndex.duplicates(hash);
    if (headers == null)
      return new ArrayList<>(0);
    List<SimplifiedStoredBlock> res = new ArrayList<>(headers.size());
    for (BlockChainLink b : headers)
      res.add(new SimplifiedStoredBlock(b));
    return res;
  }

  @Override
  protected int getNumBlocksWithPrevHash(Connection connection, byte[] hash) throws SQLException {
    return prevHashIndex.duplicates(hash).size();
  }

  @Override
  protected SimplifiedStoredBlock getHigherWorkHash(Connection connection) throws SQLException {
    Difficulty lastKey = difficultyIndex.lastKey();
    if (lastKey == null)
      return null;
    return new SimplifiedStoredBlock(difficultyIndex.get(lastKey));
  }

  @Override
  protected List<SimplifiedStoredBlock> getBlocksWithTx(Connection connection, byte[] hash) throws SQLException {
    Collection<byte[]> blockHashes = txBlockRelationship.duplicates(hash);
    if (blockHashes == null)
      return new ArrayList<>(0);
    List<SimplifiedStoredBlock> res = new ArrayList<>(blockHashes.size());
    for (byte[] blockHash : blockHashes) {
      res.add(new SimplifiedStoredBlock(blockHeaders.get(blockHash)));
    }
    return res;
  }

  public String getDbPath() {
    return dbPath;
  }

  public void setDbPath(String dbPath) {
    this.dbPath = dbPath;
  }
}
