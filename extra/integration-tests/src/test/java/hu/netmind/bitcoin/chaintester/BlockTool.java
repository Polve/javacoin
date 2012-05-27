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
package hu.netmind.bitcoin.chaintester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import hu.netmind.bitcoin.net.p2p.Node;
import hu.netmind.bitcoin.BlockChain;
import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.block.BlockChainImpl;
import hu.netmind.bitcoin.block.BlockChainLinkStorage;
import hu.netmind.bitcoin.block.BlockImpl;
import hu.netmind.bitcoin.block.TransactionOutputImpl;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import hu.netmind.bitcoin.keyfactory.ecc.KeyFactoryImpl;
import hu.netmind.bitcoin.block.bdb.BDBChainLinkStorage;
import hu.netmind.bitcoin.block.jdbc.JdbcChainLinkStorage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Simple class to work on blockchain blocks, for testing purposes
 *
 * @author Alessandro Polverini
 */
public class BlockTool {

  private static Logger logger = LoggerFactory.getLogger(BlockTool.class);
  private static final long BC_PROTOCOL_VERSION = 32100;
  private static final String STORAGE_BDB = "bdb";
  private static final String STORAGE_JDBC = "jdbc";
  private static final String STORAGE_MEMORY = "memory";
  private Node node = null;
  private BlockChain chain = null;
  private BlockChainLinkStorage storage = null;
  private ScriptFactoryImpl scriptFactory = null;
  private static final String HELP_TEXT =
          "BlockTool: Dump local BlockChain blocks for testint purposes\n\n"
          + "Usage:\n"
          + "  general options:\n"
          + "  --first=n            First block to operate.\n"
          + "  --last=n             Last block to operate.\n"
          + "  --testnet            Connect to Bitcoin test network.\n"
          + "  --prodnet            Connect to Bitcoin production network (default).\n"
          + "  --port=<port>        Listen for incoming connection on the provided port instead of the default.\n"
          + "  storage options:\n"
          + "  --storage=<engine>   Available storage engines: bdb, jdbc, memory.\n"
          + "  --bdb-path=path      Path to BDB storage files.\n"
          + "  --url=<url>          Specifies JDBC url.\n"
          + "  --driver=<class>     Specifies the class name of the JDBC driver. Defaults to 'com.mysql.jdbc.Driver'\n"
          + "  --dbuser=<username>  Specify database username. Defaults to 'bitcoinj'\n"
          + "  --dbpass=<password>  Specify database password to use for the connection.\n";
  private static int firstBlock, lastBlock;
  private static int listenPort = -1;
  private static String storageType = STORAGE_BDB;
  private static boolean isTestNet = false;
  private static OptionSpec<String> optBdbPath;
  private static OptionSpec<String> optJdbcDriver;
  private static OptionSpec<String> optJdbcUrl;
  private static OptionSpec<String> optJdbcUser;
  private static OptionSpec<String> optJdbcPassword;
  private static OptionSpec<String> inputfile;

  public static void main(String[] args) throws Exception {
    OptionParser parser = new OptionParser();
    parser.accepts("help");
    parser.accepts("testnet");
    parser.accepts("prodnet");
    parser.accepts("first").withRequiredArg().ofType(Integer.class);
    parser.accepts("port").withRequiredArg().ofType(Integer.class);
    optBdbPath = parser.accepts("bdbPath").withRequiredArg().defaultsTo("data");
    optJdbcUrl = parser.accepts("url").withRequiredArg();
    optJdbcDriver = parser.accepts("driver").withRequiredArg().defaultsTo("com.mysql.jdbc.Driver");
    optJdbcUser = parser.accepts("dbuser").withRequiredArg().defaultsTo("bitcoinj");
    optJdbcPassword = parser.accepts("dbpass").withRequiredArg();
    inputfile = parser.accepts("inputfile").withRequiredArg();
    OptionSet options = parser.parse(args);
    if (args.length == 0 || options.hasArgument("help") || options.nonOptionArguments().size() > 0
            || (options.has("testnet") && options.has("prodnet"))) {
      System.out.println(HELP_TEXT);
      return;
    }
    isTestNet = !options.has("prodnet");
    if (options.hasArgument("first")) {
      firstBlock = ((Integer) options.valueOf("first")).intValue();
    }
    if (options.hasArgument("port")) {
      listenPort = ((Integer) options.valueOf("port")).intValue();
    }

    BlockTool app = new BlockTool();
    try {
      logger.debug("FirstBlock: " + firstBlock + " lastBlock: " + lastBlock + " inputfile: " + inputfile.value(options));
      app.init(options);
      if (inputfile.value(options) != null) {
        app.readBlock(inputfile.value(options));
      }
      //logger.debug("run...");
      //app.run();
    } finally {
      logger.debug("close...");
      app.close();
    }
  }

  /**
   * Initialize and bind components together.
   */
  public void init(OptionSet options) throws BitCoinException {
    scriptFactory = new ScriptFactoryImpl(new KeyFactoryImpl(null));
    // Initialize the correct storage engine
    if (STORAGE_BDB.equalsIgnoreCase(storageType)) {
      BDBChainLinkStorage engine = new BDBChainLinkStorage(scriptFactory);
      engine.setDbPath(optBdbPath.value(options));
      engine.init();
      storage = engine;
    } else if (STORAGE_JDBC.equalsIgnoreCase(storageType)) {
      JdbcChainLinkStorage engine = new JdbcChainLinkStorage(scriptFactory, isTestNet);
      //engine.setDriverClassName(optJdbcDriver.value(options));
      //engine.init();
    }
    chain = new BlockChainImpl(BlockImpl.MAIN_GENESIS, storage, scriptFactory, false);
    // Introduce a small check here that we can read back the genesis block correctly
    storage.getGenesisLink().getBlock().validate();
    logger.info("Storage initialized, last link height: " + storage.getLastLink().getHeight());
  }

  /**
   * Free used resources.
   */
  public void close() {
    if (storage != null) {
      //storage.close();    // TODO: Why no close method in the storage interface?
    }
  }

  /**
   * Run the client and listen for new blocks forever.
   */
  public void run() {
    try {
      // Start the node
      node.start();
      // Wait for keypress to end
      System.in.read();
    } catch (Exception e) {
      logger.error("error while starting node or waiting for enter", e);
    }
  }

  public void readBlock(String fileName) throws FileNotFoundException, IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map jsonObj = (Map) mapper.readValue(new File(fileName), Object.class);
    String hash = (String) jsonObj.get("hash");
    ArrayList<Map> txs = (ArrayList<Map>) jsonObj.get("tx");
    for (Map tx : txs) {
      logger.debug("Processing tx: " + tx.get("hash"));
      ArrayList<Map> inputs = (ArrayList<Map>) tx.get("inputs");
      ArrayList<Map> outs = (ArrayList<Map>) tx.get("out");
      for (Map input : inputs) {
        logger.debug(" Input: " + input.get("prev_out"));
      }
      for (Map output : outs) {
        long value = ((Long) output.get("value")).longValue();
        int type = ((Integer) output.get("type")).intValue();
        logger.debug(" Out -- addr: " + output.get("addr") + " value: " + value + " type: " + type);
        TransactionOutputImpl tout = new TransactionOutputImpl(value, scriptFactory.createFragment(null));
        logger.debug(" Tout: " + tout);
      }
    }
    logger.debug("Lettura da " + fileName + " hash: " + hash + " txs: " + txs);
  }
}
