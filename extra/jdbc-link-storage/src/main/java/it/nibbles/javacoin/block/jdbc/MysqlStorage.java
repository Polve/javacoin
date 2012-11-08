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
package it.nibbles.javacoin.block.jdbc;

import hu.netmind.bitcoin.BitcoinException;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.block.BaseChainLinkStorage;
import hu.netmind.bitcoin.block.BitcoinFactory;
import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.block.BlockImpl;
import hu.netmind.bitcoin.block.SimplifiedStoredBlock;
import hu.netmind.bitcoin.block.TransactionImpl;
import hu.netmind.bitcoin.block.TransactionInputImpl;
import hu.netmind.bitcoin.block.TransactionOutputImpl;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
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
public class MysqlStorage extends BaseChainLinkStorage
{

   private static Logger logger = LoggerFactory.getLogger(MysqlStorage.class);
   private static final int DEFAULT_RESERVE_SIZE = 100;
   private int idReserveSize = DEFAULT_RESERVE_SIZE;
   private BitcoinFactory bitcoinFactory = null;
   private DataSource dataSource;
   //
   // Id generators for rows inserted in the DB tables
   protected JdbcIdGenerator blockIdGen;
   protected JdbcIdGenerator transactionIdGen;
   protected JdbcIdGenerator txInputsIdGen;
   protected JdbcIdGenerator txOutputsIdGen;
   //
   // Block Headers SQL statements and PS
   final private String blockSqlFields =
           "id, height, nTime, nBits, nonce, version, hash, prevBlockHash, hashMerkleRoot, chainWork";
   final private String sqlPutBlock =
           "INSERT INTO Block(" + blockSqlFields + ") VALUES(?,?,?,?,?,?,?,?,?,?)"; // ON DUPLICATE KEY UPDATE nTime=nTime";
   final private String sqlGetBlockHeader =
           "SELECT " + blockSqlFields + " FROM Block WHERE hash=?";
   final private String sqlGetBlockId =
           "SELECT id FROM Block WHERE hash=?";
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
           "SELECT hash,prevBlockHash,height FROM Block ORDER BY chainWork DESC LIMIT 1";
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

   public MysqlStorage(BitcoinFactory bitcoinFactory)
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
         setAutoCreate(Boolean.valueOf(config.getString(prefix + "autocreate")));
         //setTransactional(Boolean.valueOf(config.getString(prefix + "transactional")));
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
      logger.debug(this.getClass() + " Initialized");
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
   }

   @Override
   protected Connection newConnection()
   {
      try
      {
         return dataSource.getConnection();
      } catch (SQLException ex)
      {
         throw new LowLevelStorageException("Can't get a connection to database: " + ex.getMessage(), ex.getCause());
      }
   }

   /**
    * Removes the database. Used for unit testing
    */
   public void removeDatabase()
   {
      try (Connection dbConnection = newConnection(); Statement st = dbConnection.createStatement())
      {
         dbConnection.setAutoCommit(true);
         String[] tables =
         {
            "TxOutput", "TxInput", "Transaction", "Counter", "BlockTx", "Block"
         };

         for (String table : tables)
            st.execute("TRUNCATE TABLE " + table);
         logger.info("Database tables truncated");
      } catch (SQLException e)
      {
         throw new JdbcStorageException("Error while truncating tables: " + e.getMessage(), e);
      }
   }

   @Override
   public boolean blockExists(final Connection dbConnection, byte[] hash) throws SQLException
   {
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetBlockId))
      {
         ps.setBytes(1, hash);
         ResultSet rs = ps.executeQuery();
         return rs.next();
      }
   }

   @Override
   protected List<SimplifiedStoredBlock> getBlocksAtHeight(final Connection dbConnection, long height) throws SQLException
   {
      List<SimplifiedStoredBlock> blocks = new ArrayList<>();
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetSimplifiedBlockHeadersAtHeight))
      {
         ps.setLong(1, height);
         ResultSet rs = ps.executeQuery();
         while (rs.next())
         {
            SimplifiedStoredBlock ssb = new SimplifiedStoredBlock(rs);
            blocks.add(ssb);
         }
      }
      return blocks;
   }

   @Override
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

   @Override
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

   @Override
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

   @Override
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

   @Override
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

   @Override
   protected long storeBlockHeader(final Connection dbConnection, final BlockChainLink link)
   {
      Block block = link.getBlock();
      long blockId = blockIdGen.getNewId();
      //logger.debug("[storeBlock id= " + blockId + " " + HexUtil.toSingleHexString(block.getHash()) + "]");
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlPutBlock))
      {
         ps.setLong(1, blockId);
         ps.setLong(2, link.getHeight());
         ps.setLong(3, block.getCreationTime());
         ps.setLong(4, block.getCompressedTarget());
         ps.setLong(5, block.getNonce());
         ps.setLong(6, block.getVersion());
         ps.setBytes(7, block.getHash());
         ps.setBytes(8, block.getPreviousBlockHash());
         ps.setBytes(9, block.getMerkleRoot());
         ps.setLong(10, link.getTotalDifficulty().getDifficulty().longValue());
         ps.executeUpdate();
         return blockId;
      } catch (SQLException e)
      {
         logger.error("Error while storing block header: " + e.getMessage(), e);
         throw new JdbcStorageException("Error while storing block header: " + e.getMessage(), e);
      }
   }

   @Override
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
            if (Arrays.equals(TransactionInput.ZERO_HASH, tinput.getClaimedTransactionHash()))
               psPutTxInput.setNull(3, java.sql.Types.BINARY);
            else
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

   @Override
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

   @Override
   protected List<TransactionInputImpl> loadTxInputs(final Connection dbConnection, long txId) throws SQLException
   {
      List<TransactionInputImpl> inputs = new LinkedList<>();
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetTxInputs))
      {
         ps.setLong(1, txId);
         ResultSet rs = ps.executeQuery();
         while (rs.next())
         {
            byte[] referredTxHash = rs.getBytes("referredTxHash");
            if (rs.wasNull())
               referredTxHash = TransactionInput.ZERO_HASH;
            inputs.add(new TransactionInputImpl(
                    referredTxHash, rs.getInt("referredTxIndex"),
                    bitcoinFactory.getScriptFactory().createFragment(rs.getBytes("scriptBytes")),
                    rs.getLong("sequence")));
         }
      }
      return inputs;
   }

   @Override
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

   @Override
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

   @Override
   protected TransactionImpl getTransaction(final Connection dbConnection, byte[] hash) throws SQLException, BitcoinException
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

   @Override
   protected List<TransactionImpl> getBlockTransactions(final Connection dbConnection, ResultSet rs) throws SQLException, BitcoinException
   {
      List<TransactionImpl> res = new LinkedList<>();
      while (rs.next())
      {
         long txId = rs.getLong("txId");
         res.add(new TransactionImpl(loadTxInputs(dbConnection, txId), loadTxOutputs(dbConnection, txId), rs.getLong("lockTime"), rs.getBytes("hash"), rs.getInt("version")));
      }
      return res;
   }

   @Override
   protected List<TransactionImpl> getBlockTransactions(final Connection dbConnection, byte[] hash)
   {
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetBlockTransactionsFromHash))
      {
         ps.setBytes(1, hash);
         return getBlockTransactions(dbConnection, ps.executeQuery());
      } catch (SQLException | BitcoinException e)
      {
         logger.error("getBlockByHashTransactionsEx: " + e.getMessage(), e);
         throw new JdbcStorageException("getBlockByHashTransactionsEx: " + e.getMessage(), e);
      }
   }

   @Override
   protected List<TransactionImpl> getBlockTransactions(final Connection dbConnection, long blockId)
   {
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetBlockTransactionsFromId))
      {
         ps.setLong(1, blockId);
         return getBlockTransactions(dbConnection, ps.executeQuery());
      } catch (SQLException | BitcoinException e)
      {
         logger.error("getBlockByIdTransactionsEx: " + e.getMessage(), e);
         throw new JdbcStorageException("getBlockByIdTransactionsEx: " + e.getMessage(), e);
      }
   }

   @Override
   protected BlockChainLink createBlockWithTxs(final Connection dbConnection, final byte[] hash, List<TransactionImpl> transactions)
   {
      //logger.debug("[createBlockWithTxs " + HexUtil.toSingleHexString(hash) + " ]");
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetBlockHeader))
      {
         ps.setBytes(1, hash);
         ResultSet rs = ps.executeQuery();
         if (rs.next())
            return bitcoinFactory.newBlockChainLink(
                    new BlockImpl(
                    transactions, rs.getLong("nTime"), rs.getLong("nonce"), rs.getLong("nBits"),
                    rs.getBytes("prevBlockHash"), rs.getBytes("hashMerkleRoot"), rs.getBytes("hash"), rs.getLong("version")),
                    new BigDecimal(rs.getLong("chainWork")),
                    rs.getLong("height"));
         else
            return null;
      } catch (SQLException | BitcoinException e)
      {
         logger.error("getCompleteBlockEx: " + e.getMessage(), e);
         throw new JdbcStorageException("getCompleteBlockEx: " + e.getMessage(), e);
      }
   }

   @Override
   protected SimplifiedStoredBlock getSimplifiedStoredBlock(final Connection dbConnection, final byte[] hash) throws SQLException
   {
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetBlockHeader))
      {
         ps.setBytes(1, hash);
         ResultSet rs = ps.executeQuery();
         if (rs.next())
            return new SimplifiedStoredBlock(rs);
         else
            return null;
      }
   }

   @Override
   protected SimplifiedStoredBlock getHigherWorkHash(final Connection dbConnection) throws SQLException
   {
      try (PreparedStatement ps = dbConnection.prepareStatement(sqlGetHigherWorkBlock))
      {
         ResultSet rs = ps.executeQuery();
         if (rs.next())
            return new SimplifiedStoredBlock(rs);
         else
            return null;
      }
   }

   public void purgeBlocksUpToHeight(long height, boolean purgeTransactions) throws SQLException
   {
      long startTime = System.currentTimeMillis();
      Connection dbConnection = newConnection();
      int blocksDeleted = -1, txDeleted = -1;
      try
      {
         if (getTransactional())
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
         if (getTransactional())
         {
            dbConnection.commit();
         }
      } catch (SQLException e)
      {
         try
         {
            if (getTransactional())
            {
               dbConnection.rollback();
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
         logger.debug("exec time: " + (stopTime - startTime) + " ms blocksDeleted: " + blocksDeleted + " txDeleted: " + txDeleted);
      }
   }

   public DataSource getDataSource()
   {
      return dataSource;
   }

   public void setDataSource(DataSource dataSource)
   {
      this.dataSource = dataSource;
   }
}
