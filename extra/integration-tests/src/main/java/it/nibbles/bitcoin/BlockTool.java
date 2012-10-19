/**
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
package it.nibbles.bitcoin;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.BlockChain;
import hu.netmind.bitcoin.block.BitcoinFactory;
import hu.netmind.bitcoin.block.BlockChainImpl;
import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.block.BlockChainLinkStorage;
import hu.netmind.bitcoin.block.StandardBitcoinFactory;
import hu.netmind.bitcoin.block.Testnet2BitcoinFactory;
import hu.netmind.bitcoin.block.Testnet3BitcoinFactory;
import hu.netmind.bitcoin.block.TransactionOutputImpl;
import hu.netmind.bitcoin.block.jdbc.DatasourceUtils;
import hu.netmind.bitcoin.block.jdbc.JdbcChainLinkStorage;
import hu.netmind.bitcoin.keyfactory.ecc.KeyFactoryImpl;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import it.nibbles.bitcoin.utils.BtcUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple class to work on blockchain blocks, for testing purposes
 *
 * @author Alessandro Polverini
 */
public class BlockTool
{

   //private static Logger logger = LoggerFactory.getLogger(BlockTool.class);
   private static final long BC_PROTOCOL_VERSION = 32100;
   private static final String STORAGE_BDB = "bdb";
   private static final String STORAGE_JDBC = "jdbc";
   private static final String STORAGE_MEMORY = "memory";
   private static BlockChain chain;
   private static BlockChainLinkStorage storage;
   private static ScriptFactoryImpl scriptFactory;
   private static BitcoinFactory bitcoinFactory;
   private static final String HELP_TEXT =
      "BlockTool: Dump local BlockChain blocks for testint purposes\n\n"
      + "Usage:\n"
      + "  commands:\n"
      + "  --load               Read blocks from inputfile and store them to storage.\n"
      + "  --save               Read blocks from storage and write them to the specified outputfile.\n"
      + "  general options:\n"
      + "  --first=n            First block to operate.\n"
      + "  --last=n             Last block to operate.\n"
      + "  --testnet            Connect to Bitcoin test network.\n"
      + "  --prodnet            Connect to Bitcoin production network (default).\n"
      + "  --port=<port>        Listen for incoming connection on the provided port instead of the default.\n"
      + "  --inputfile=<file>   Name of the file used to read from.\n"
      + "  --outputfile=<file>  Name of the file used to write to.\n"
      + "  storage options:\n"
      + "  --storage=<engine>   Available storage engines: bdb, jdbc, memory.\n"
      + "  --bdb-path=path      Path to BDB storage files.\n"
      + "  --url=<url>          Specifies JDBC url.\n"
      + "  --driver=<class>     Specifies the class name of the JDBC driver. Defaults to 'com.mysql.jdbc.Driver'\n"
      + "  --dbuser=<username>  Specify database username. Defaults to 'bitcoinj'\n"
      + "  --dbpass=<password>  Specify database password to use for the connection.\n";
   private static int firstBlock, lastBlock;
   private static int listenPort = -1;
   private static String storageType = STORAGE_JDBC;
   private static boolean isProdnet = false;
   private static boolean isTestNet2 = false;
   private static boolean isTestNet3 = false;
   private static OptionSpec<String> optBdbPath;
   private static OptionSpec<String> optJdbcUrl;
   private static OptionSpec<String> optJdbcUser;
   private static OptionSpec<String> optJdbcPassword;
   private static OptionSpec<String> inputfile;
   private static OptionSpec<String> outputfile;
   private static OptionSpec<String> revalidateOption;
   private static boolean cmdSaveBlockchain = false;
   private static boolean cmdLoadBlockchain = false;

   public static void main(String[] args) throws Exception
   {
      OptionParser parser = new OptionParser();
      parser.accepts("help");
      parser.accepts("load");
      parser.accepts("save");
      parser.accepts("testnet2");
      parser.accepts("testnet3");
      parser.accepts("prodnet");
      parser.accepts("first").withRequiredArg().ofType(Integer.class);
      parser.accepts("port").withRequiredArg().ofType(Integer.class);
      optBdbPath = parser.accepts("bdbPath").withRequiredArg().defaultsTo("data");
      //optJdbcDriver = parser.accepts("driver").withRequiredArg().defaultsTo("com.mysql.jdbc.Driver");
      optJdbcUrl = parser.accepts("url").withRequiredArg().defaultsTo("jdbc:mysql://localhost/javacoin_testnet3");
      optJdbcUser = parser.accepts("dbuser").withRequiredArg().defaultsTo("javacoin");
      optJdbcPassword = parser.accepts("dbpass").withRequiredArg().defaultsTo("pw");
      inputfile = parser.accepts("inputfile").withRequiredArg();
      outputfile = parser.accepts("outputfile").withRequiredArg();
      OptionSet options = parser.parse(args);
      if (args.length == 0
         || options.hasArgument("help")
         || options.nonOptionArguments().size() > 0
         || (options.has("save") && options.has("load"))
         || (options.has("save") && !options.has("outputfile"))
         || (options.has("load") && !options.has("inputfile"))
         || (options.has("testnet2") && options.has("testnet3"))
         || (options.has("testnet2") && options.has("prodnet"))
         || (options.has("testnet3") && options.has("prodnet")))
      {
         println(HELP_TEXT);
         return;
      }
      cmdSaveBlockchain = options.has("save");
      cmdLoadBlockchain = options.has("load");
      isProdnet = options.has("prodnet");
      isTestNet2 = options.has("testnet2");
      isTestNet3 = options.has("testnet3");
      if (options.hasArgument("first"))
      {
         firstBlock = ((Integer) options.valueOf("first")).intValue();
      }
      if (options.hasArgument("port"))
      {
         listenPort = ((Integer) options.valueOf("port")).intValue();
      }

      println("save: " + cmdSaveBlockchain + " load: " + cmdLoadBlockchain + " prodnet: " + isProdnet + " testnet2: " + isTestNet2 + " testnet3: " + isTestNet3);
      BlockTool app = new BlockTool();
      try
      {
         println("FirstBlock: " + firstBlock + " lastBlock: " + lastBlock + " inputfile: " + inputfile.value(options) + " outputfile: " + outputfile.value(options));
         app.init(options);
         if (cmdLoadBlockchain)
         {
            app.readBlock(inputfile.value(options));
         } else if (cmdSaveBlockchain)
         {
            BlockChainLink blockLink = storage.getLastLink();
            app.writeBlock(outputfile.value(options), blockLink.getBlock());
         }
      } finally
      {
         println("close...");
         app.close();
      }
   }

   /**
    * Initialize and bind components together.
    */
   public void init(OptionSet options) throws BitCoinException, ClassNotFoundException
   {
      scriptFactory = new ScriptFactoryImpl(new KeyFactoryImpl(null));
      bitcoinFactory = isTestNet3 ? new Testnet3BitcoinFactory(scriptFactory)
         : isTestNet2 ? new Testnet2BitcoinFactory(scriptFactory)
         : new StandardBitcoinFactory(scriptFactory);
      // Initialize the correct storage engine
      if (STORAGE_BDB.equalsIgnoreCase(storageType))
      {
         println("BDB DISABLED");
         System.exit(33);
//         BDBChainLinkStorage engine = new BDBChainLinkStorage(scriptFactory);
//         engine.setDbPath(optBdbPath.value(options));
//         storage = engine;
      } else if (STORAGE_JDBC.equalsIgnoreCase(storageType))
      {
         JdbcChainLinkStorage engine = new JdbcChainLinkStorage(bitcoinFactory);
         engine.setDataSource(DatasourceUtils.getMysqlDatasource(
            optJdbcUrl.value(options), optJdbcUser.value(options), optJdbcPassword.value(options)));
         engine.init();
         storage = engine;
      }
      chain = new BlockChainImpl(bitcoinFactory, storage, false);
      // Introduce a small check here that we can read back the genesis block correctly
      storage.getGenesisLink().getBlock().validate();
      println("Storage initialized, last link height: " + storage.getLastLink().getHeight());
   }

   /**
    * Free used resources.
    */
   public void close()
   {
      if (storage != null)
      {
      }
   }

   public void readBlock(String fileName) throws FileNotFoundException, IOException
   {
      ObjectMapper mapper = new ObjectMapper();
      Map jsonObj = (Map) mapper.readValue(new File(fileName), Object.class);
      String hash = (String) jsonObj.get("hash");
      ArrayList<Map> txs = (ArrayList<Map>) jsonObj.get("tx");
      for (Map tx : txs)
      {
         println("Processing tx: " + tx.get("hash"));
         ArrayList<Map> inputs = (ArrayList<Map>) tx.get("inputs");
         ArrayList<Map> outs = (ArrayList<Map>) tx.get("out");
         for (Map input : inputs)
            println(" Input: " + input.get("prev_out"));
         for (Map output : outs)
         {
            long value = ((Long) output.get("value")).longValue();
            int type = ((Integer) output.get("type")).intValue();
            println(" Out -- addr: " + output.get("addr") + " value: " + value + " type: " + type);
            TransactionOutputImpl tout = new TransactionOutputImpl(value, scriptFactory.createFragment(null));
            println(" Tout: " + tout);
         }
      }
      println("Lettura da " + fileName + " hash: " + hash + " txs: " + txs);
   }

   public void writeBlock(String filename, Block block) throws IOException
   {
      println("write to " + filename + " block: " + block);
      println("block " + BtcUtil.hexOut(block.getHash()) + " "
         + BtcUtil.hexOut(block.getMerkleRoot()) + " "
         + BtcUtil.hexOut(block.getPreviousBlockHash()) + " "
         + Long.toHexString(block.getCompressedTarget()) + " "
         + block.getCreationTime() + " "
         + block.getNonce() + " "
         + block.getVersion());
   }

   public static void println(String s)
   {
      System.out.println(s);
   }
}
