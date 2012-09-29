/**
 * Copyright (C) 2012 nibbles.it This library is free software; you can
 * redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
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

import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.ScriptFactory;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.block.BitcoinFactory;
import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.block.BlockChainLinkStorage;
import hu.netmind.bitcoin.block.BlockImpl;
import hu.netmind.bitcoin.block.Difficulty;
import hu.netmind.bitcoin.block.TransactionImpl;
import hu.netmind.bitcoin.block.TransactionInputImpl;
import hu.netmind.bitcoin.block.TransactionOutputImpl;
import hu.netmind.bitcoin.net.HexUtil;
import hu.netmind.bitcoin.net.NodeAddress;
import hu.netmind.bitcoin.net.p2p.NodeStorage;
import it.nibbles.bitcoin.utils.BtcUtil;
import java.math.BigDecimal;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storing block link information using JDBC (MySql only for now).
 *
 * @author Alessandro Polverini
 */
public class JdbcChainLinkStorage implements BlockChainLinkStorage, NodeStorage
{

   private static final boolean DEFAULT_AUTOCREATE = true;
   private static final boolean DEFAULT_TRANSACTIONAL = true;
   private static final int DEFAULT_RESERVE_SIZE = 100;
   private static Logger logger = LoggerFactory.getLogger(JdbcChainLinkStorage.class);
   private boolean autoCreate = DEFAULT_AUTOCREATE;
   private boolean transactional = DEFAULT_TRANSACTIONAL;
   private int idReserveSize = DEFAULT_RESERVE_SIZE;
   private BitcoinFactory bitcoinFactory = null;
   //private ScriptFactory scriptFactory = null;
   //private boolean isTestnet = false;
   private DataSource dataSource;
   //
   // Id generators for rows inserted in the DB tables
   protected JdbcIdGenerator blockIdGen;
   protected JdbcIdGenerator transactionIdGen;
   protected JdbcIdGenerator txInputsIdGen;
   protected JdbcIdGenerator txOutputsIdGen;
   //
   // We keep cached the top of the chain for a faster response
   protected BlockChainLink topLink;
   //
   // Block Headers SQL statements and PS
   final private String blockSqlFields =
           "id, hash, hashMerkleRoot, nTime, nBits, nonce, height, prevBlockHash, chainWork";
   final private String sqlPutBlock =
           "INSERT INTO Block(" + blockSqlFields + ") VALUES(?,?,?,?,?,?,?,?,?)"; // ON DUPLICATE KEY UPDATE nTime=nTime";
   final private String sqlGetBlock =
           "SELECT " + blockSqlFields + " FROM Block WHERE hash=?";
   //
   // SQL statements used to write to the DB
   final private String sqlPutTransaction =
           "INSERT INTO Transaction(id,hash,lockTime) VALUES(?,?,?)";
   final private String sqlPutTxInput =
           "INSERT INTO TxInput(id,txId,referredTxHash,referredTxIndex,sequence,scriptBytes) VALUES(?,?,?,?,?,?)";
   final private String sqlPutTxOutput =
           "INSERT INTO TxOutput(id,txId,value,scriptBytes) VALUES(?,?,?,?)";
   //
   // SQL statements used to read from the DB
   final private String sqlGetSimplifiedBlockHeadersAtHeight =
           "SELECT hash,prevBlockHash,height FROM Block WHERE height=?";
   final private String sqlGetHigherWorkBlock =
           "SELECT hash, height FROM Block ORDER BY chainWork DESC LIMIT 1";
   final private String sqlGetSimplifiedBlocksWithPrevHash =
           "SELECT hash,prevBlockHash,height FROM Block WHERE prevBlockHash=?";
   final private String sqlGetNumBlocksWithPrevHash =
           "SELECT COUNT(*) FROM Block WHERE prevBlockHash=?";
   final private String sqlGetTransaction =
           "SELECT id,lockTime,hash,version FROM Transaction WHERE hash=?";
   final private String sqlGetBlockHashesWithTx =
           "SELECT Block.hash, Block.prevBlockHash, Block.height FROM Block "
           + "LEFT JOIN BlockTx ON (Block.id = BlockTx.blockId) LEFT JOIN Transaction ON (BlockTx.txId = Transaction.id) "
           + "WHERE Transaction.hash=?";
   final private String sqlGetBlockHashesWithReferredTx =
           "SELECT Block.hash, Block.prevBlockHash, Block.height FROM Block "
           + "LEFT JOIN BlockTx ON (Block.id = BlockTx.blockId) LEFT JOIN Transaction ON (BlockTx.txId = Transaction.id) "
           + "LEFT JOIN TxInput ON (Transaction.id=TxInput.txId) WHERE TxInput.referredTxHash=? AND TxInput.referredTxIndex=?";
   final private String sqlGetBlockTransactionsFromId =
           "SELECT BlockTx.txId,Transaction.hash,Transaction.lockTime,Transaction.version FROM BlockTx "
           + "LEFT JOIN Transaction ON (BlockTx.txId = Transaction.id) WHERE blkId=? ORDER BY BlockTx.pos";
   final private String sqlGetBlockTransactionsFromHash =
           "SELECT BlockTx.txId,Transaction.hash,Transaction.lockTime,Transaction.version FROM BlockTx "
           + "LEFT JOIN Transaction ON (BlockTx.txId = Transaction.id) LEFT JOIN Block ON (BlockTx.blockId = Block.id) "
           + "WHERE Block.hash=? ORDER BY BlockTx.pos";
   final private String sqlGetTxInputs =
           "SELECT id,referredTxHash,referredTxIndex,sequence,scriptBytes FROM TxInput WHERE txId=? ORDER BY id";
   final private String sqlGetTxOutputs =
           "SELECT id,value,scriptBytes FROM TxOutput WHERE txId=? ORDER BY id";
   final private String sqlPutBlkTxLink =
           "INSERT INTO BlockTx(blockId, txId, pos) VALUES(?,?,?)";
   final private String sqlGetNumBlockHeadersAtHeight =
           "SELECT count(*) AS num FROM Block WHERE height=?";
   //
   // Purge blocks and transactions (mainly used for debugging/testing, do not use, broken)
   final private String sqlPurgeBlocksUpToHeight =
           "DELETE FROM Block,BlockTx "
           + "USING Block LEFT JOIN BlockTx ON (Block.id=BlockTx.blockId) "
           + "WHERE height >= ?";
   final private String sqlPurgeOrphanedTransactions =
           "DELETE FROM Transaction,TxInput,TxOutput "
           + "USING Transaction LEFT JOIN TxInput ON (TxInput.txId=Transaction.id) "
           + "LEFT JOIN TxOutput ON (TxOutput.txId=Transaction.id) "
           + "WHERE Transaction.id NOT IN (SELECT BlockTx.txId FROM BlockTx)";
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

//   public JdbcChainLinkStorage(ScriptFactory scriptFactory, boolean isTestnet)
//   {
//      this.scriptFactory = scriptFactory;
//      this.isTestnet = isTestnet;
//      readConfiguration();
//   }

   public JdbcChainLinkStorage(BitcoinFactory bitcoinFactory)
   {
      this.bitcoinFactory = bitcoinFactory;
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
         idReserveSize = Integer.parseInt(config.getString(prefix + "idReserveSize"));
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
      commonInit();
      initializeDatabases();
      logger.debug(this.getClass() + " Initialized with "
              + (topLink == null ? "empty DB" : "top hash: " + BtcUtil.hexOut(topLink.getBlock().getHash())));
   }

   private void initializeDatabases()
   {
      logger.info("TODO: Initialize tables");
      // TODO: autocreate tables and indexes
   }

   private void commonInit()
   {
      blockIdGen = new JdbcIdGenerator(dataSource);
      transactionIdGen = new JdbcIdGenerator(dataSource);
      txInputsIdGen = new JdbcIdGenerator(dataSource);
      txOutputsIdGen = new JdbcIdGenerator(dataSource);
      blockIdGen.setIdName("Block").setIdReserveSize(idReserveSize);
      transactionIdGen.setIdName("Transaction").setIdReserveSize(idReserveSize);
      txInputsIdGen.setIdName("TxInput").setIdReserveSize(idReserveSize);
      txOutputsIdGen.setIdName("TxOutput").setIdReserveSize(idReserveSize);
      topLink = getLastLink();
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
         {
            "TxOutput", "TxInput", "Transaction", "Counter", "BlockTx", "Block"
         };

         for (String table : tables)
            st.execute("TRUNCATE TABLE " + table);
         logger.debug("Database tables truncated");
      } catch (SQLException e)
      {
         throw new JdbcStorageException("Error while truncating tables: " + e.getMessage(), e);
      }
   }

   @Override
   public synchronized void addLink(final BlockChainLink link)
   {
      long startTime = System.currentTimeMillis();
      //logger.debug("addLink: " + HexUtil.toSingleHexString(link.getBlock().getHash())
      //       + " height: " + link.getHeight() + " totalDifficulty: " + link.getTotalDifficulty() + " isOrphan: " + link.isOrphan());
      if (link.isOrphan())
      {
         logger.error("Requested to persist orphan block");
         throw new JdbcStorageException("Requested to persist orphan block");
      }
      Connection dbConnection = getDbConnection();
      try
      {
         if (transactional)
            dbConnection.setAutoCommit(false);
         long blockId = storeBlockHeader(dbConnection, link);

         List<Transaction> transactions = link.getBlock().getTransactions();
         int pos = 0;
         for (Transaction tx : transactions)
         {
            long txId = getTransactionId(dbConnection, tx.getHash());
            if (txId == -1)
               txId = storeTransaction(dbConnection, tx);
            storeBlkTxLink(dbConnection, blockId, txId, pos++);
         }

         if (topLink == null || link.getTotalDifficulty().compareTo(topLink.getTotalDifficulty()) > 0)
            topLink = link;

         if (transactional)
         {
            dbConnection.commit();
            dbConnection.setAutoCommit(true);
         }
      } catch (SQLException e)
      {
         try
         {
            if (transactional)
            {
               dbConnection.rollback();
               dbConnection.setAutoCommit(true);
            }
         } catch (SQLException ex)
         {
         }
         logger.error("AddLinkEx: " + e.getMessage(), e);
         throw new JdbcStorageException("Error while storing link: " + e.getMessage(), e);
      } finally
      {
         try
         {
            dbConnection.close();
         } catch (SQLException e)
         {
            logger.error("AddLinkEx: " + e.getMessage(), e);
            throw new JdbcStorageException("Error while closing connection: " + e.getMessage(), e);
         }
         long stopTime = System.currentTimeMillis();
         logger.debug("exec time: " + (stopTime - startTime) + " ms height: " + link.getHeight() + " total difficulty: " + link.getTotalDifficulty());
      }
   }

   // TODO: Handle correctly transactions so that if addLink fails the block is not removed from orphans
   @Override
   public void updateLink(BlockChainLink link)
   {
      logger.error("Requested to update link");
      throw new JdbcStorageException("Requested to update link");
   }

   @Override
   public BlockChainLink getLink(final byte[] hash)
   {
//      long startTime = System.currentTimeMillis();
      try (Connection dbConnection = getDbConnection())
      {
         // logger.debug("getLink: " + HexUtil.toSingleHexString(hash));
         return getCompleteBlock(dbConnection, hash, getBlockTransactions(dbConnection, hash));
      } catch (Exception e)
      {
         logger.error("getLinkEx: " + e.getMessage(), e);
         throw new JdbcStorageException("getLinkEx: " + e.getMessage(), e);
      } finally
      {
//         long stopTime = System.currentTimeMillis();
//         logger.debug("exec time: " + (stopTime - startTime) + " ms");
      }
   }

   @Override
   public BlockChainLink getGenesisLink()
   {
      try (Connection dbConnection = getDbConnection())
      {
         Map<byte[], SimplifiedStoredBlock> blocks = getBlocksAtHeight(dbConnection, BlockChainLink.ROOT_HEIGHT);
         if (blocks.isEmpty())
            return null;
         return getLink(blocks.keySet().iterator().next());
      } catch (SQLException ex)
      {
         throw new JdbcStorageException("getGenesisLinkEx: " + ex.getMessage(), ex);
      }
   }

   @Override
   public BlockChainLink getLastLink()
   {
      if (topLink != null)
         return topLink;
      try (Connection dbConnection = getDbConnection(); PreparedStatement ps = dbConnection.prepareStatement(sqlGetHigherWorkBlock))
      {
         ResultSet rs = ps.executeQuery();
         if (rs.next())
            return topLink = getLink(rs.getBytes("hash"));
         else
            return null;
      } catch (SQLException e)
      {
         logger.error("getLastLinkEx: " + e.getMessage(), e);
         throw new JdbcStorageException("getLastLinkEx: " + e.getMessage(), e);
      }
   }

   @Override
   public long getHeight()
   {
      if (topLink != null)
         return topLink.getHeight();
      try (Connection dbConnection = getDbConnection(); PreparedStatement ps = dbConnection.prepareStatement(sqlGetHigherWorkBlock))
      {
         ResultSet rs = ps.executeQuery();
         if (rs.next())
            return rs.getLong("height");
         else
            return 0;
      } catch (SQLException e)
      {
         logger.error("getHeight: " + e.getMessage(), e);
         throw new JdbcStorageException("getHeight: " + e.getMessage(), e);
      }
   }

   @Override
   public List<BlockChainLink> getNextLinks(final byte[] hash)
   {
      //long startTime = System.currentTimeMillis();
      try (Connection dbConnection = getDbConnection())
      {
         List<SimplifiedStoredBlock> blocks = getBlocksWithPrevHash(dbConnection, hash);
         List<BlockChainLink> storedLinks = new LinkedList<>();
         for (SimplifiedStoredBlock b : blocks)
            storedLinks.add(getLink(b.hash));
         return storedLinks;
      } catch (SQLException e)
      {
         logger.error("getNextLinks: " + e.getMessage(), e);
         throw new JdbcStorageException("getNextLinks: " + e.getMessage(), e);
      } finally
      {
         //long stopTime = System.currentTimeMillis();
         //logger.debug("exec time: " + (stopTime - startTime) + " ms");
      }
   }

   @Override
   public BlockChainLink getNextLink(final byte[] current, final byte[] target)
   {
      try (Connection dbConnection = getDbConnection())
      {
         SimplifiedStoredBlock targetBlock = getSimplifiedStoredBlock(dbConnection, target);
         if (targetBlock == null)
            return null;
         SimplifiedStoredBlock currentBlock = getSimplifiedStoredBlock(dbConnection, current);
         if (currentBlock == null || targetBlock.height < currentBlock.height)
            return null;
         for (SimplifiedStoredBlock candidate : getBlocksWithPrevHash(dbConnection, current))
            if (isReachable(dbConnection, targetBlock, candidate))
               return getLink(candidate.hash);
         return null;
      } catch (SQLException e)
      {
         logger.error("getNextLink: " + e.getMessage(), e);
         throw new JdbcStorageException("getNextLink: " + e.getMessage(), e);
      }
   }

   @Override
   public boolean isReachable(final byte[] target, final byte[] source)
   {
      long startTime = System.currentTimeMillis();
      try (Connection dbConnection = getDbConnection())
      {
         SimplifiedStoredBlock targetBlock = getSimplifiedStoredBlock(dbConnection, target);
         if (targetBlock == null)
            return false;
         SimplifiedStoredBlock sourceBlock = getSimplifiedStoredBlock(dbConnection, source);
         if (sourceBlock == null)
            return false;
         return isReachable(dbConnection, targetBlock, sourceBlock);
      } catch (SQLException e)
      {
         logger.error("isReacheble: " + e.getMessage(), e);
         throw new JdbcStorageException("isReachable: " + e.getMessage(), e);
      } finally
      {
         long stopTime = System.currentTimeMillis();
         logger.debug("exec time: " + (stopTime - startTime) + " ms");
      }
   }

   @Override
   public BlockChainLink getClaimedLink(final BlockChainLink link, final TransactionInput in)
   {
      long startTime = System.currentTimeMillis();
      try (Connection dbConnection = getDbConnection())
      {
         List<SimplifiedStoredBlock> potentialBlocks = getBlocksWithTx(dbConnection, in.getClaimedTransactionHash());
         SimplifiedStoredBlock linkBlock = new SimplifiedStoredBlock(link);
         for (SimplifiedStoredBlock b : potentialBlocks)
            if (b.height <= link.getHeight() && isReachable(dbConnection, linkBlock, b))
               return getLink(b.hash);
         return null;
      } catch (SQLException e)
      {
         logger.error("getClaimedLink: " + e.getMessage(), e);
         throw new JdbcStorageException("getClaimedLinks: " + e.getMessage(), e);
      } finally
      {
         long stopTime = System.currentTimeMillis();
         logger.debug(HexUtil.toSingleHexString(in.getClaimedTransactionHash()) + " exec time: " + (stopTime - startTime) + " ms");
      }
   }

   @Override
   public BlockChainLink getPartialClaimedLink(final BlockChainLink link, final TransactionInput in)
   {
      long startTime = System.currentTimeMillis();
      try (Connection dbConnection = getDbConnection())
      {
         List<SimplifiedStoredBlock> potentialBlocks = getBlocksWithTx(dbConnection, in.getClaimedTransactionHash());
         SimplifiedStoredBlock linkBlock = new SimplifiedStoredBlock(link);
         for (SimplifiedStoredBlock b : potentialBlocks)
            if (b.height <= link.getHeight() && isReachable(dbConnection, linkBlock, b))
            {
               List<TransactionImpl> txs = new LinkedList<>();
               txs.add(getTransaction(dbConnection, in.getClaimedTransactionHash()));
               return getCompleteBlock(dbConnection, b.hash, txs);
            }
         return null;
      } catch (SQLException | BitCoinException e)
      {
         logger.error("getClaimedLink: " + e.getMessage(), e);
         throw new JdbcStorageException("getClaimedLinks: " + e.getMessage(), e);
      } finally
      {
         long stopTime = System.currentTimeMillis();
         logger.debug(HexUtil.toSingleHexString(in.getClaimedTransactionHash()) + " /" + in.getClaimedOutputIndex() + " exec time: " + (stopTime - startTime) + " ms");
      }
   }

   @Override
   public BlockChainLink getClaimerLink(final BlockChainLink link, final TransactionInput in)
   {
      try (Connection dbConnection = getDbConnection())
      {
         byte[] hash = getClaimerHash(dbConnection, link, in);
         if (hash == null)
            return null;
         else
            return getLink(hash);
      } catch (SQLException e)
      {
         logger.error("getClaimerLink: " + e.getMessage(), e);
         throw new JdbcStorageException("getClaimerLinks: " + e.getMessage(), e);
      }
   }

   @Override
   public boolean outputClaimedInSameBranch(final BlockChainLink link, final TransactionInput in)
   {
      try (Connection dbConnection = getDbConnection())
      {
         return getClaimerHash(dbConnection, link, in) != null;
      } catch (SQLException e)
      {
         logger.error("outputClaimedInSameBranch: " + e.getMessage(), e);
         throw new JdbcStorageException("outputClaimedInSameBranch: " + e.getMessage(), e);
      }
   }

   @Override
   public BlockChainLink getCommonLink(final byte[] first, final byte[] second)
   {
      long startTime = System.currentTimeMillis();
      //logger.debug("getCommonLink " + HexUtil.toSingleHexString(first) + " " + HexUtil.toSingleHexString(second));
      try (Connection dbConnection = getDbConnection())
      {
         SimplifiedStoredBlock block1 = getSimplifiedStoredBlock(dbConnection, first);
         SimplifiedStoredBlock block2 = getSimplifiedStoredBlock(dbConnection, second);
         if (block1 == null || block2 == null)
            return null;

         if (block1.equals(block2))
            return getLink(block1.hash);

         // We need block1 height lower than block2 height
         if (block2.height < block1.height)
         {
            SimplifiedStoredBlock tmp = block1;
            block1 = block2;
            block2 = tmp;
         }

         // loop until we reach the genesis block
         while (true)
         {
            //logger.debug("getCommonLinkLoop block1: " + block1);
            // Genesis link reaches all non-orphan blocks
            if (block1.height == BlockChainLink.ROOT_HEIGHT)
               return getGenesisLink();

            // If we have no side branches there is only one possibility to have a common link
            // and it is the lowest block between the two: block1
            if (isReachable(dbConnection, block2, block1))
               return getLink(block1.hash);
            else if (getNumBlocksAtHeight(dbConnection, block1.height) == 1)
               return null;

            // If the two blocks are not reachable find a lower branch intersection
            do
               block1 = getSimplifiedStoredBlock(dbConnection, block1.prevBlockHash);
            while (getNumBlocksWithPrevHash(dbConnection, block1.hash) == 1);
         }

      } catch (SQLException ex)
      {
         throw new JdbcStorageException("getCommonLinkEx: " + ex.getMessage(), ex);
      } finally
      {
         long stopTime = System.currentTimeMillis();
         logger.debug("exec time: " + (stopTime - startTime) + " ms");
      }
   }

   @Override
   public byte[] getHashOfMainChainAtHeight(long height)
   {
      try (Connection dbConnection = getDbConnection())
      {
         Map<byte[], SimplifiedStoredBlock> blocks = getBlocksAtHeight(dbConnection, height);
         if (blocks.isEmpty())
            return null;
         if (blocks.size() == 1)
            return blocks.keySet().iterator().next();
         BlockChainLink top = getLastLink();       // TODO: Optimize that function, we can keep top cached
         SimplifiedStoredBlock topBlock = new SimplifiedStoredBlock(top);
         for (SimplifiedStoredBlock b : blocks.values())
            if (isReachable(dbConnection, b, topBlock))
               return b.hash;

         // We should never arrive here
         assert false;
         return null;
      } catch (SQLException e)
      {
         logger.error("getNextLink: " + e.getMessage(), e);
         throw new JdbcStorageException("getNextLink: " + e.getMessage(), e);
      }
   }

   /*
    * This check is complicated by BIP30, that a new tx can exist with same hash
    * if the other one fully spent We sort blocks on height because we need to
    * check only the last one in the given chain The problem arise from TX
    * a1d7c19f72ce5b24a1001bf9c5452babed6734eaa478642379f8c702a46d5e27 in block
    * 0000000013aa9f67da178005f9ced61c7064dd6e8464b35f6a8ca8fabc1ca2cf
    */
   protected byte[] getClaimerHash(final Connection dbConnection, final BlockChainLink link, final TransactionInput in)
   {
      if (link == null || in == null)
         return null;
      try
      {
         List<SimplifiedStoredBlock> potentialBlocks = getBlocksReferringTx(dbConnection, in);
         Collections.sort(potentialBlocks);
         SimplifiedStoredBlock linkBlock = new SimplifiedStoredBlock(link);
         for (SimplifiedStoredBlock b : potentialBlocks)
            if (b.height <= link.getHeight() && isReachable(dbConnection, linkBlock, b))
            {
               // Check if a tx with same hash has been created after being reclaimed
               List<SimplifiedStoredBlock> blocks = getBlocksWithTx(dbConnection, in.getClaimedTransactionHash());
               // We sort blocks on height to discard the ones below
               Collections.sort(blocks);
               for (SimplifiedStoredBlock block : blocks)
               {
                  // No potential good blocks left
                  if (block.height < b.height)
                     break;
                  // If we find a block higher in the chain with the same transaction we have found a not spent tx with the same hash
                  // So we need to return null, indicating that no block is claiming it
                  if (isReachable(dbConnection, new SimplifiedStoredBlock(link), block))
                     return null;
               }
               return b.hash;
            }
         return null;
      } catch (SQLException e)
      {
         logger.error("getClaimerHash: " + e.getMessage(), e);
         throw new JdbcStorageException("getClaimerHash: " + e.getMessage(), e);
      }
   }

   protected Map<byte[], SimplifiedStoredBlock> getBlocksAtHeight(final Connection dbConnection, long height) throws SQLException
   {
      Map<byte[], SimplifiedStoredBlock> blocks = new HashMap<>();
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetSimplifiedBlockHeadersAtHeight))
      {
         ps.setLong(1, height);
         ResultSet rs = ps.executeQuery();
         while (rs.next())
         {
            SimplifiedStoredBlock ssb = new SimplifiedStoredBlock(rs);
            blocks.put(ssb.hash, ssb);
         }
      }
      return blocks;
   }

   protected List<SimplifiedStoredBlock> getBlocksReferringTx(final Connection dbConnection, final TransactionInput in) throws SQLException
   {
      List<SimplifiedStoredBlock> blocks = new LinkedList<>();
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetBlockHashesWithReferredTx))
      {
         ps.setBytes(1, in.getClaimedTransactionHash());
         ps.setInt(2, in.getClaimedOutputIndex());
         ResultSet rs = ps.executeQuery();
         while (rs.next())
            blocks.add(new SimplifiedStoredBlock(rs));
      }
      return blocks;
   }

   protected List<SimplifiedStoredBlock> getBlocksWithTx(final Connection dbConnection, final byte[] hash) throws SQLException
   {
      List<SimplifiedStoredBlock> blocks = new LinkedList<>();
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetBlockHashesWithTx))
      {
         ps.setBytes(1, hash);
         ResultSet rs = ps.executeQuery();
         while (rs.next())
            blocks.add(new SimplifiedStoredBlock(rs));
      }
      return blocks;
   }

   protected List<SimplifiedStoredBlock> getBlocksWithPrevHash(final Connection dbConnection, final byte[] hash) throws SQLException
   {
      List<SimplifiedStoredBlock> blocks = new LinkedList<>();
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetSimplifiedBlocksWithPrevHash))
      {
         ps.setBytes(1, hash);
         ResultSet rs = ps.executeQuery();
         while (rs.next())
            blocks.add(new SimplifiedStoredBlock(rs));
      }
      return blocks;
   }

   protected int getNumBlocksAtHeight(final Connection dbConnection, long height) throws SQLException
   {
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetNumBlockHeadersAtHeight))
      {
         ps.setLong(1, height);
         ResultSet rs = ps.executeQuery();
         rs.next();
         return rs.getInt(1);
      }
   }

   protected int getNumBlocksWithPrevHash(final Connection dbConnection, byte[] hash) throws SQLException
   {
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetNumBlocksWithPrevHash))
      {
         ps.setBytes(1, hash);
         ResultSet rs = ps.executeQuery();
         rs.next();
         return rs.getInt(1);
      }
   }

   protected long storeBlockHeader(final Connection dbConnection, final BlockChainLink link)
   {
      Block block = link.getBlock();
      long blockId = blockIdGen.getNewId();
      //logger.debug("[storeBlock id= " + blockId + " " + HexUtil.toSingleHexString(block.getHash()) + "]");
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlPutBlock))
      {
         ps.setLong(1, blockId);
         ps.setBytes(2, block.getHash());
         ps.setBytes(3, block.getMerkleRoot());
         ps.setLong(4, block.getCreationTime());
         ps.setLong(5, block.getCompressedTarget());
         ps.setLong(6, block.getNonce());
         ps.setLong(7, link.getHeight());
         ps.setBytes(8, block.getPreviousBlockHash());
         ps.setLong(9, link.getTotalDifficulty().getDifficulty().longValue());
         ps.executeUpdate();
         return blockId;
      } catch (SQLException e)
      {
         logger.error("Error while storing block header: " + e.getMessage(), e);
         throw new JdbcStorageException("Error while storing block header: " + e.getMessage(), e);
      }
   }

   protected long storeTransaction(final Connection dbConnection, final Transaction tx)
   {
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlPutTransaction);
              PreparedStatement psPutTxInput = dbConnection.prepareStatement(sqlPutTxInput);
              PreparedStatement psPutTxOutput = dbConnection.prepareStatement(sqlPutTxOutput);)
      {
         long txId = transactionIdGen.getNewId();
         //logger.debug("Transazione con nuovo id: " + txId + " :" + HexUtil.toSingleHexString(tx.getHash()) + " txins: " + tx.getInputs().size() + " txouts: " + tx.getOutputs().size());
         ps.setLong(1, txId);
         ps.setBytes(2, tx.getHash());
         ps.setLong(3, tx.getLockTime());
         int n = ps.executeUpdate();
         //logger.debug("Numero record inseriti per la transazione: " + n);

         for (TransactionInput tinput : tx.getInputs())
         {
            long id = txInputsIdGen.getNewId();
            psPutTxInput.setLong(1, id);
            psPutTxInput.setLong(2, txId);
            psPutTxInput.setBytes(3, tinput.getClaimedTransactionHash());
            psPutTxInput.setLong(4, tinput.getClaimedOutputIndex());
            psPutTxInput.setLong(5, tinput.getSequence());
            psPutTxInput.setBytes(6, tinput.getSignatureScript().toByteArray());
            psPutTxInput.executeUpdate();
         }
         for (TransactionOutput tout : tx.getOutputs())
         {
            long id = txOutputsIdGen.getNewId();
            psPutTxOutput.setLong(1, id);
            psPutTxOutput.setLong(2, txId);
            psPutTxOutput.setLong(3, tout.getValue());
            psPutTxOutput.setBytes(4, tout.getScript().toByteArray());
            psPutTxOutput.executeUpdate();
         }
         return txId;
      } catch (SQLException e)
      {
         throw new JdbcStorageException("Error while storing transaction: " + e.getMessage(), e);
      }
   }

   protected void storeBlkTxLink(final Connection dbConnection, long blockId, long txId, int pos) throws SQLException
   {
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlPutBlkTxLink))
      {
         ps.setLong(1, blockId);
         ps.setLong(2, txId);
         ps.setLong(3, pos);
         ps.executeUpdate();
      }
   }

   protected List<TransactionInputImpl> loadTxInputs(final Connection dbConnection, long txId) throws SQLException
   {
      List<TransactionInputImpl> inputs = new LinkedList<>();
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetTxInputs))
      {
         ps.setLong(1, txId);
         ResultSet rs = ps.executeQuery();
         while (rs.next())
            inputs.add(new TransactionInputImpl(
                    rs.getBytes("referredTxHash"), rs.getInt("referredTxIndex"),
                    bitcoinFactory.getScriptFactory().createFragment(rs.getBytes("scriptBytes")),
                    rs.getLong("sequence")));
      }
      return inputs;
   }

   protected List<TransactionOutputImpl> loadTxOutputs(final Connection dbConnection, long txId) throws SQLException
   {
      List<TransactionOutputImpl> outputs = new LinkedList<>();
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetTxOutputs))
      {
         ps.setLong(1, txId);
         ResultSet rs = ps.executeQuery();
         while (rs.next())
            outputs.add(new TransactionOutputImpl(rs.getLong("value"),
                    bitcoinFactory.getScriptFactory().createFragment(rs.getBytes("scriptBytes"))));
      }
      return outputs;
   }

   protected long getTransactionId(final Connection dbConnection, byte[] hash) throws SQLException
   {
      long txId = -1;
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetTransaction))
      {
         ps.setBytes(1, hash);
         ResultSet rs = ps.executeQuery();
         if (rs.next())
            txId = rs.getLong("id");
         return txId;
      }
   }

   protected TransactionImpl getTransaction(final Connection dbConnection, byte[] hash) throws SQLException, BitCoinException
   {
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetTransaction))
      {
         ps.setBytes(1, hash);
         ResultSet rs = ps.executeQuery();
         if (rs.next())
         {
            long txId = rs.getLong("id");
            return new TransactionImpl(loadTxInputs(dbConnection, txId), loadTxOutputs(dbConnection, txId), rs.getLong("lockTime"), rs.getBytes("hash"), rs.getInt("version"));
         }
         return null;
      }
   }

   protected List<TransactionImpl> getBlockTransactions(final Connection dbConnection, ResultSet rs) throws SQLException, BitCoinException
   {
      List<TransactionImpl> res = new LinkedList<>();
      while (rs.next())
      {
         long txId = rs.getLong("txId");
         res.add(new TransactionImpl(loadTxInputs(dbConnection, txId), loadTxOutputs(dbConnection, txId), rs.getLong("lockTime"), rs.getBytes("hash"), rs.getInt("version")));
      }
      return res;
   }

   protected List<TransactionImpl> getBlockTransactions(final Connection dbConnection, byte[] hash)
   {
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetBlockTransactionsFromHash))
      {
         ps.setBytes(1, hash);
         return getBlockTransactions(dbConnection, ps.executeQuery());
      } catch (SQLException | BitCoinException e)
      {
         logger.error("getBlockByHashTransactionsEx: " + e.getMessage(), e);
         throw new JdbcStorageException("getBlockByHashTransactionsEx: " + e.getMessage(), e);
      }
   }

   protected List<TransactionImpl> getBlockTransactions(final Connection dbConnection, long blockId)
   {
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetBlockTransactionsFromId))
      {
         ps.setLong(1, blockId);
         return getBlockTransactions(dbConnection, ps.executeQuery());
      } catch (SQLException | BitCoinException e)
      {
         logger.error("getBlockByIdTransactionsEx: " + e.getMessage(), e);
         throw new JdbcStorageException("getBlockByIdTransactionsEx: " + e.getMessage(), e);
      }
   }

   protected BlockChainLink getBlockHeader(final Connection dbConnection, final byte[] hash)
   {
      return getCompleteBlock(dbConnection, hash, new LinkedList<TransactionImpl>());
   }

   protected BlockChainLink getCompleteBlock(final Connection dbConnection, final byte[] hash, List<TransactionImpl> transactions)
   {
      //logger.debug("[getCompleteBlock " + HexUtil.toSingleHexString(hash) + " ]");
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetBlock))
      {
         ps.setBytes(1, hash);
         ResultSet rs = ps.executeQuery();
         if (rs.next())
         {
            Block block = new BlockImpl(transactions, rs.getLong("nTime"), rs.getLong("nonce"), rs.getLong("nBits"),
                    rs.getBytes("prevBlockHash"), rs.getBytes("hashMerkleRoot"), rs.getBytes("hash"));
            return bitcoinFactory.newBlockChainLink(block, new BigDecimal(rs.getLong("chainWork")), rs.getLong("height"));
//            return new BlockChainLink(block,
//                    new Difficulty(new BigDecimal(rs.getLong("chainWork")), isTestnet),
//                    rs.getLong("height"), false);
         } else
            return null;
      } catch (SQLException | BitCoinException e)
      {
         logger.error("getCompleteBlockEx: " + e.getMessage(), e);
         throw new JdbcStorageException("getCompleteBlockEx: " + e.getMessage(), e);
      }
   }

   protected SimplifiedStoredBlock getSimplifiedStoredBlock(final Connection dbConnection, final byte[] hash) throws SQLException
   {
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetBlock))
      {
         ps.setBytes(1, hash);
         ResultSet rs = ps.executeQuery();
         if (rs.next())
            return new SimplifiedStoredBlock(rs);
         else
            return null;
      }
   }

   /**
    * Returns true if both blocks are in the same branch and b1 precedes b2
    *
    * @param source
    * @param target
    * @return
    */
   protected boolean isReachable(final Connection dbConnection, SimplifiedStoredBlock target, SimplifiedStoredBlock source) throws SQLException
   {
      // Sanity checks
      if (source == null || target == null)
         return false;

      // Handle special cases
      if (source.equals(target))
         return true;

      // If at same height and not same block we are in different branches
      if (target.height == source.height)
         return false;

      // We need source height lower than target height
      if (target.height < source.height)
      {
         SimplifiedStoredBlock tmp = source;
         source = target;
         target = tmp;
      }

      long currHeight = source.height;
      List<SimplifiedStoredBlock> chains = new LinkedList<>();
      chains.add(source);

      // Follow all the chains we discover from our starting point
      // until we are certain whether we are in the same chain of the target or not
      do
      {
         //logger.debug("isRechableLoop - currHeight: " + currHeight + " chains.size: " + chains.size());
         // Check if we are the only chain at this height
         // then we are in the same branch for sure because we know the target is connected
         if (chains.size() == 1)
            if (getNumBlocksAtHeight(dbConnection, currHeight) == 1)
               return true;
         List<SimplifiedStoredBlock> newChains = new LinkedList<>();
         for (SimplifiedStoredBlock block : chains)
            if (block.equals(target))
               return true;
            else
               newChains.addAll(getBlocksWithPrevHash(dbConnection, block.hash));
         chains = newChains;
         currHeight++;
      } while (!chains.isEmpty() && currHeight <= target.height);

      // We reached the top of all our chain without finding the target
      // so we are in different branches
      return false;
   }

   public void purgeBlocksUpToHeight(long height, boolean purgeTransactions) throws SQLException
   {
      long startTime = System.currentTimeMillis();
      Connection dbConnection = getDbConnection();
      int blocksDeleted=-1, txDeleted=-1;
      try
      {
         if (transactional)
            dbConnection.setAutoCommit(false);
         try (PreparedStatement ps = dbConnection.prepareStatement(sqlPurgeBlocksUpToHeight))
         {
            ps.setLong(1, height);
            blocksDeleted = ps.executeUpdate();
         }
         if (purgeTransactions)
            try (PreparedStatement ps = dbConnection.prepareStatement(sqlPurgeOrphanedTransactions))
            {
               txDeleted = ps.executeUpdate();
            }
         if (transactional)
         {
            dbConnection.commit();
            dbConnection.setAutoCommit(true);
         }
      } catch (SQLException e)
      {
         try
         {
            if (transactional)
            {
               dbConnection.rollback();
               dbConnection.setAutoCommit(true);
            }
         } catch (SQLException ex)
         {
         }
         logger.error("purgeBlocksUpToHeight: " + e.getMessage(), e);
         throw new JdbcStorageException("Error while purging blocks: " + e.getMessage(), e);
      } finally
      {
         try
         {
            dbConnection.close();
         } catch (SQLException e)
         {
            logger.error("purgeBlocksUpToHeight: " + e.getMessage(), e);
            throw new JdbcStorageException("Error while closing connection: " + e.getMessage(), e);
         }
         long stopTime = System.currentTimeMillis();
         logger.debug("exec time: " + (stopTime - startTime) + " ms blocksDeleted: "+blocksDeleted+" txDeleted: "+txDeleted);
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
