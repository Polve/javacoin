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

package hu.netmind.bitcoin.chaintester;

import hu.netmind.bitcoin.block.BlockChainLinkStorage;
import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.block.TransactionInputImpl;
import hu.netmind.bitcoin.block.TransactionOutputImpl;
import hu.netmind.bitcoin.block.TransactionImpl;
import hu.netmind.bitcoin.block.BlockImpl;
import hu.netmind.bitcoin.block.Difficulty;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigInteger;

/**
 * A storage implemented as simple as possible to hold links.
 * @author Robert Brautigam
 */
public class SimpleSqlStorage implements BlockChainLinkStorage
{
   private static Logger logger = LoggerFactory.getLogger(SimpleSqlStorage.class);
   private Connection connection = null;
   private ScriptFactoryImpl scriptFactory = null;

   private BlockChainLink lastAddedLink = null;
   private long lastAddedLinkLeftMarker = 0;
   private long lastAddedLinkRightMarker = 0;

   public SimpleSqlStorage(ScriptFactoryImpl scriptFactory)
   {
      this.scriptFactory=scriptFactory;
      try
      {
         // Initialize the sql storage to the path given
         connection = DriverManager.getConnection("jdbc:h2:data/bitcoin;CACHE_SIZE=500000");
         connection.setAutoCommit(false);
         logger.debug("get connection to database: "+connection.getMetaData().getDatabaseProductName());
         // Create the schema if not already present
         ResultSet tables = connection.getMetaData().getTables(null,null,"LINK",new String[] {"TABLE"});
         if ( ! tables.next() )
         {
            logger.debug("schema doesn't exists yet in database, creating...");
            connection.prepareStatement(
                  "create table link ( hash bytea not null, creationtime bigint, "+
                  "nonce bigint, compressedtarget bigint, previoushash bytea, "+
                  "merkleroot bytea, orphan boolean, height bigint, "+
                  "totaldifficulty numeric(80,0), "+
                  "leftmarker bigint, rightmarker bigint, "+ // Stores structure (tree)
                  "primary key ( hash ))").executeUpdate();
            connection.prepareStatement(
                  "create table tx ( hash bytea not null, linkhash bytea, index integer, locktime bigint, "+
                  "primary key ( linkhash, hash ), "+
                  "foreign key ( linkhash ) references link ( hash ) )").executeUpdate();
            connection.prepareStatement(
                  "create table txout ( linkhash bytea not null, txhash bytea not null, index integer not null, value bigint not null, script bytea, "+
                  "primary key ( txhash, index ), "+
                  "foreign key ( linkhash, txhash ) references tx ( linkhash, hash ), "+
                  "foreign key ( linkhash ) references link ( hash ) )").executeUpdate();
            connection.prepareStatement(
                  "create table txin ( linkhash bytea, txhash bytea not null, index integer not null, claimedhash bytea, "+
                  "claimedindex integer, script bytea, sequence bigint, "+
                  "primary key ( txhash, index ), "+
                  "foreign key ( linkhash, txhash ) references tx ( linkhash, hash ), "+
                  "foreign key ( linkhash ) references link ( hash ) )").executeUpdate();
            connection.commit();
            logger.debug("changed commited.");
         }
      } catch ( SQLException e ) {
         throw new RuntimeException("error while initializing database for chain links",e);
      }
   }

   public void updateLink(BlockChainLink link)
   {
      logger.debug("updating link at depth: "+link.getHeight());
      lastAddedLink = null; // May be a child of an inserted link
      try
      {
         try
         {
            PreparedStatement pstmt = connection.prepareStatement(
                  "update link set orphan = ?, height = ?, totaldifficulty = ? where hash = ?");
            pstmt.setBoolean(1,link.isOrphan());
            pstmt.setLong(2,link.getHeight());
            pstmt.setObject(3,link.getTotalDifficulty().getDifficulty());
            pstmt.setBytes(4,link.getBlock().getHash());
            pstmt.executeUpdate();
         } finally {
            connection.commit();
         }
      } catch ( SQLException e ) {
         throw new RuntimeException("error while adding link: "+link,e);
      }
   }

   public void addLink(BlockChainLink link)
   {
      logger.debug("inserting link at depth: "+link.getHeight());
      try
      {
         PreparedStatement pstmt = null;
         ResultSet rs = null;
         long left = 0;
         long right = 0;
         try
         {
            // First calculate where in the hierarchy this node should go. If
            // the parent is the previously newly inserted link, then use that information,
            // otherwise find out.
            if ( (lastAddedLink!=null) && (Arrays.equals(lastAddedLink.getBlock().getHash(),
                        link.getBlock().getPreviousBlockHash())) )
            {
               left = lastAddedLinkLeftMarker;
               right = lastAddedLinkRightMarker;
            } else {
               pstmt = connection.prepareStatement(
                     "select leftmarker, rightmarker from link where hash = ?");
               pstmt.setBytes(1,link.getBlock().getPreviousBlockHash());
               rs = pstmt.executeQuery();
               if ( rs.next() )
               {
                  // We found the previous link
                  left = rs.getLong(1);
                  right = rs.getLong(2);
                  // Let's see what is still free
                  rs.close();
                  pstmt.close();
                  pstmt = connection.prepareStatement(
                        "select max(rightmarker) from link where previoushash = ?");
                  pstmt.setBytes(1,link.getBlock().getPreviousBlockHash());
                  rs = pstmt.executeQuery();
                  if ( rs.next() )
                     if ( rs.getLong(1) > left )
                        left = rs.getLong(1);
               }
               rs.close();
               pstmt.close();
            }
            if ( (left>0) && (right>0) )
            {
               // Now advance
               left++;
               right--;
               // If we don't have enough space left, extend
               if ( left >= right )
               {
                  pstmt = connection.prepareStatement(
                        "update link set rightmarker = rightmarker+1000 where rightmarker > ?");
                  pstmt.setLong(1,right);
                  pstmt.executeUpdate();
                  pstmt.close();
                  right += 1000;
               }
            } else {
               // We didn't found the parent, so link should be orphan
               if ( link.isOrphan() )
               {
                  // Link is orphan, so it's ok that it doesn't have any parents
                  left = 0;
                  right = 0;
               } else {
                  // Link is not orphan which means it's a genesis block,
                  // so put some initial values up.
                  left = 0;
                  right = 1000;
               }
            }
            logger.debug("calculated link hierarchy position as ("+left+","+right+")");
            // First add the link so it can be referenced
            pstmt = connection.prepareStatement(
                  "insert into link ( hash, creationtime, "+
                  "nonce, compressedtarget, previoushash, merkleroot, orphan, height, "+
                  "totaldifficulty, leftmarker, rightmarker ) values "+
                  "(?,?,?,?,?,?,?,?,?,?,?)");
            pstmt.setBytes(1,link.getBlock().getHash());
            pstmt.setLong(2,link.getBlock().getCreationTime());
            pstmt.setLong(3,link.getBlock().getNonce());
            pstmt.setLong(4,link.getBlock().getCompressedTarget());
            pstmt.setBytes(5,link.getBlock().getPreviousBlockHash());
            pstmt.setBytes(6,link.getBlock().getMerkleRoot());
            pstmt.setBoolean(7,link.isOrphan());
            pstmt.setLong(8,link.getHeight());
            pstmt.setObject(9,link.getTotalDifficulty().getDifficulty());
            pstmt.setLong(10,left);
            pstmt.setLong(11,right);
            pstmt.executeUpdate();
            pstmt.close();
            // Add transactions, input, outputs
            int txCounter = 0;
            for ( Transaction tx : link.getBlock().getTransactions() )
            {
               // First add transaction
               pstmt = connection.prepareStatement("insert into tx "+
                     "( hash, linkhash, locktime, index ) values ( ?,?,?,? )");
               pstmt.setBytes(1,tx.getHash());
               pstmt.setBytes(2,link.getBlock().getHash());
               pstmt.setLong(3,tx.getLockTime());
               pstmt.setInt(4,txCounter++);
               pstmt.executeUpdate();
               pstmt.close();
               // Add all inputs
               int counter = 0;
               for ( TransactionInput in : tx.getInputs() )
               {
                  pstmt = connection.prepareStatement("insert into txin "+
                        "( txhash, index, claimedhash, claimedindex, script, sequence, linkhash ) "+
                        "values ( ?,?,?,?,?,?,? )");
                  pstmt.setBytes(1,tx.getHash());
                  pstmt.setInt(2,counter++);
                  pstmt.setBytes(3,in.getClaimedTransactionHash());
                  pstmt.setInt(4,in.getClaimedOutputIndex());
                  pstmt.setBytes(5,in.getSignatureScript().toByteArray());
                  pstmt.setLong(6,in.getSequence());
                  pstmt.setBytes(7,link.getBlock().getHash());
                  pstmt.executeUpdate();
                  pstmt.close();
               }
               // Add all outputs
               for ( TransactionOutput out : tx.getOutputs() )
               {
                  pstmt = connection.prepareStatement("insert into txout "+
                        "( txhash, index, value, script, linkhash ) values ( ?,?,?,?,? )");
                  pstmt.setBytes(1,tx.getHash());
                  pstmt.setInt(2,out.getIndex());
                  pstmt.setLong(3,out.getValue());
                  pstmt.setBytes(4,out.getScript().toByteArray());
                  pstmt.setBytes(5,link.getBlock().getHash());
                  pstmt.executeUpdate();
                  pstmt.close();
               }
            }
         } finally {
            connection.commit();
            // Remember link if successful add
            lastAddedLink = link;
            lastAddedLinkLeftMarker = left;
            lastAddedLinkRightMarker = right;
         }
      } catch ( SQLException e ) {
         throw new RuntimeException("error while adding link: "+link,e);
      }
   }

   public void close()
   {
      try
      {
         if ( connection != null )
            connection.close();
      } catch ( SQLException e ) {
         throw new RuntimeException("error closing connection",e);
      }
   }

   public BlockChainLink getGenesisLink()
   {
      try
      {
         PreparedStatement pstmt = connection.prepareStatement(
               "select hash from link where height = 0 and orphan = false");
         ResultSet rs = pstmt.executeQuery();
         if ( ! rs.next() )
         {
            rs.close();
            pstmt.close();
            return null;
         }
         byte[] hash = rs.getBytes(1);
         rs.close();
         pstmt.close();
         return getLink(hash);
      } catch ( SQLException e ) {
         throw new RuntimeException("could not read genesis link",e);
      }
   }

   public BlockChainLink getLastLink()
   {
      try
      {
         PreparedStatement pstmt = connection.prepareStatement(
               "select hash from link order by height desc limit 1");
         ResultSet rs = pstmt.executeQuery();
         if ( ! rs.next() )
         {
            rs.close();
            pstmt.close();
            return null;
         }
         byte[] hash = rs.getBytes(1);
         rs.close();
         pstmt.close();
         return getLink(hash);
      } catch ( SQLException e ) {
         throw new RuntimeException("could not read last link",e);
      }
   }

   public BlockChainLink getLink(byte[] hash)
   {
      try
      {
         // First read tx ins
         Map<BigInteger,List<TransactionInputImpl>> insMap = new HashMap<BigInteger,List<TransactionInputImpl>>();
         PreparedStatement pstmt = connection.prepareStatement(
               "select txhash, index, claimedhash, claimedindex, script, sequence "+
               "from txin where linkhash = ? order by index asc");
         pstmt.setBytes(1,hash);
         ResultSet rs = pstmt.executeQuery();
         while ( rs.next() )
         {
            TransactionInputImpl in = new TransactionInputImpl(
                  rs.getBytes(3),rs.getInt(4),scriptFactory.createFragment(rs.getBytes(5)),
                  rs.getLong(6));
            BigInteger hashInt = new BigInteger(1,rs.getBytes(1));
            // Insert to map
            List<TransactionInputImpl> ins = insMap.get(hashInt);
            if ( ins == null )
            {
               ins = new LinkedList<TransactionInputImpl>();
               insMap.put(hashInt,ins);
            }
            ins.add(in);
         }
         rs.close();
         pstmt.close();
         // Read tx outs
         Map<BigInteger,List<TransactionOutputImpl>> outsMap = new HashMap<BigInteger,List<TransactionOutputImpl>>();
         pstmt = connection.prepareStatement(
               "select txhash, index, value, script "+
               "from txout where linkhash = ? order by index asc");
         pstmt.setBytes(1,hash);
         rs = pstmt.executeQuery();
         while ( rs.next() )
         {
            TransactionOutputImpl out = new TransactionOutputImpl(
                  rs.getLong(3),scriptFactory.createFragment(rs.getBytes(4)));
            BigInteger hashInt = new BigInteger(1,rs.getBytes(1));
            // Insert to map
            List<TransactionOutputImpl> outs = outsMap.get(hashInt);
            if ( outs == null )
            {
               outs = new LinkedList<TransactionOutputImpl>();
               outsMap.put(hashInt,outs);
            }
            outs.add(out);
         }
         rs.close();
         pstmt.close();
         // Read txs
         List<Transaction> txs = new LinkedList<Transaction>();
         pstmt = connection.prepareStatement(
               "select hash, index, locktime "+
               "from tx where linkhash = ? order by index asc");
         pstmt.setBytes(1,hash);
         rs = pstmt.executeQuery();
         while ( rs.next() ) 
         {
            BigInteger hashInt = new BigInteger(1,rs.getBytes(1));
            List<TransactionInputImpl> ins = insMap.get(hashInt);
            if ( ins == null )
               ins = new LinkedList<TransactionInputImpl>();
            List<TransactionOutputImpl> outs = outsMap.get(hashInt);
            if ( outs == null )
               outs = new LinkedList<TransactionOutputImpl>();
            TransactionImpl tx = new TransactionImpl(ins,outs,rs.getLong(3));
            txs.add(tx);
         }
         rs.close();
         pstmt.close();
         // Read link
         pstmt = connection.prepareStatement(
               "select creationtime, nonce, compressedtarget, previoushash, merkleroot, orphan, "+
               "height, totaldifficulty from link where hash = ?");
         pstmt.setBytes(1,hash);
         rs = pstmt.executeQuery();
         if ( ! rs.next() )
         {
            rs.close();
            pstmt.close();
            return null; // No link found with that hash
         }
         BlockImpl block = new BlockImpl(txs,rs.getLong(1),rs.getLong(2),rs.getLong(3),
               rs.getBytes(4),rs.getBytes(5),hash);
         BlockChainLink link = new BlockChainLink(block,new Difficulty(rs.getBigDecimal(8)),
               rs.getInt(7),rs.getBoolean(6));
         rs.close();
         pstmt.close();
         // Return link
         return link;
      } catch ( BitCoinException e ) {
         throw new RuntimeException("error unmarshalling link",e);
      } catch ( SQLException e ) {
         throw new RuntimeException("could not read link per hash",e);
      }
   }

   public List<BlockChainLink> getNextLinks(byte[] hash)
   {
      try
      {
         PreparedStatement pstmt = connection.prepareStatement(
               "select hash from link where previoushash = ?");
         pstmt.setBytes(1,hash);
         ResultSet rs = pstmt.executeQuery();
         List<BlockChainLink> links = new LinkedList<BlockChainLink>();
         while ( rs.next() )
            links.add(getLink(rs.getBytes(1)));
         rs.close();
         pstmt.close();
         return links;
      } catch ( SQLException e ) {
         throw new RuntimeException("could not read next links",e);
      }
   }

   public BlockChainLink getClaimedLink(BlockChainLink link, TransactionInput in)
   {
      try
      {
         // First get the left-right values
         PreparedStatement pstmt = connection.prepareStatement(
               "select leftmarker,rightmarker from link where hash = ?");
         pstmt.setBytes(1,link.getBlock().getHash());
         ResultSet rs = pstmt.executeQuery();
         if ( ! rs.next() )
         {
            rs.close();
            pstmt.close();
            return null; // No such branch
         }
         long leftmarker = rs.getLong(1);
         long rightmarker = rs.getLong(2);
         rs.close();
         pstmt.close();
         // Now do the actual query, this needs a join
         pstmt = connection.prepareStatement(
               "select link.hash from link, txout where "+
               "link.hash = txout.linkhash and link.leftmarker <= ? and link.rightmarker >= ? and "+
               "txout.txhash = ? and txout.index = ?");
         pstmt.setLong(1,leftmarker);
         pstmt.setLong(2,rightmarker);
         pstmt.setBytes(3,in.getClaimedTransactionHash());
         pstmt.setInt(4,in.getClaimedOutputIndex());
         rs = pstmt.executeQuery();
         if ( ! rs.next() )
         {
            rs.close();
            pstmt.close();
            return null;
         }
         byte[] hash = rs.getBytes(1);
         rs.close();
         pstmt.close();
         return getLink(hash);
      } catch ( SQLException e ) {
         throw new RuntimeException("could not get claimed link",e);
      }
   }

   public BlockChainLink getClaimerLink(BlockChainLink link, TransactionInput in)
   {
      try
      {
         // First get the left-right values
         PreparedStatement pstmt = connection.prepareStatement(
               "select leftmarker,rightmarker from link where hash = ?");
         pstmt.setBytes(1,link.getBlock().getHash());
         ResultSet rs = pstmt.executeQuery();
         if ( ! rs.next() )
         {
            rs.close();
            pstmt.close();
            return null; // No such branch
         }
         long leftmarker = rs.getLong(1);
         long rightmarker = rs.getLong(2);
         rs.close();
         pstmt.close();
         // Now do the actual query, this needs a join
         pstmt = connection.prepareStatement(
               "select link.hash from link, txin where "+
               "link.hash = txin.linkhash and link.leftmarker <= ? and link.rightmarker >= ? and "+
               "txin.claimedhash = ? and txin.claimedindex = ?");
         pstmt.setLong(1,leftmarker);
         pstmt.setLong(2,rightmarker);
         pstmt.setBytes(3,in.getClaimedTransactionHash());
         pstmt.setInt(4,in.getClaimedOutputIndex());
         rs = pstmt.executeQuery();
         if ( ! rs.next() )
         {
            rs.close();
            pstmt.close();
            return null;
         }
         byte[] hash = rs.getBytes(1);
         rs.close();
         pstmt.close();
         return getLink(hash);
      } catch ( SQLException e ) {
         throw new RuntimeException("could not get claimer link",e);
      }
   }
}

