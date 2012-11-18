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
package it.nibbles.javacoin;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.nibbles.javacoin.block.BitcoinFactory;
import it.nibbles.javacoin.block.BlockChainImpl;
import it.nibbles.javacoin.block.BlockChainLink;
import it.nibbles.javacoin.block.BlockChainLinkStorage;
import it.nibbles.javacoin.block.BlockImpl;
import it.nibbles.javacoin.block.ProdnetBitcoinFactory;
import it.nibbles.javacoin.block.Testnet2BitcoinFactory;
import it.nibbles.javacoin.block.Testnet3BitcoinFactory;
import it.nibbles.javacoin.block.TransactionImpl;
import it.nibbles.javacoin.block.TransactionInputImpl;
import it.nibbles.javacoin.block.TransactionOutputImpl;
import it.nibbles.javacoin.storage.bdb.BDBStorage;
import it.nibbles.javacoin.keyfactory.ecc.KeyFactoryImpl;
import it.nibbles.javacoin.script.ScriptFactoryImpl;
import it.nibbles.javacoin.utils.BtcUtil;
import it.nibbles.javacoin.storage.jdbc.DatasourceUtils;
import it.nibbles.javacoin.storage.jdbc.MysqlStorage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Simple class to work on blockchain blocks, for testing purposes
 *
 * @author Alessandro Polverini
 */
public class BlockTool {

  private static final String STORAGE_BDB = "bdb";
  private static final String STORAGE_JDBC = "jdbc";
  private static final String STORAGE_MEMORY = "memory";
  private static BlockChainLinkStorage storage;
  private static ScriptFactoryImpl scriptFactory;
  private static BitcoinFactory bitcoinFactory;
  private static BlockChain blockChain;
  private static final String HELP_TEXT =
          "BlockTool: Dump local BlockChain blocks for testint purposes\n\n"
          + "Usage:\n"
          + "  commands:\n"
          + "  --import             Read blocks from inputfile and store them to storage.\n"
          + "  --export             Read blocks from storage and write them to the specified outputfile.\n"
          + "  general options:\n"
          + "  --hash=n             Hash of block to operate.\n"
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
  private static String blockHash;
  private static String storageType = STORAGE_BDB;
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
  private static boolean cmdExportBlockchain = false;
  private static boolean cmdImportBlockchain = false;

  public static void main(String[] args) throws Exception {
    OptionParser parser = new OptionParser();
    parser.accepts("help");
    parser.accepts("import");
    parser.accepts("export");
    parser.accepts("testnet2");
    parser.accepts("testnet3");
    parser.accepts("prodnet");
    parser.accepts("first").withRequiredArg().ofType(Integer.class);
    parser.accepts("last").withRequiredArg().ofType(Integer.class);
    parser.accepts("hash").withRequiredArg();
    parser.accepts("port").withRequiredArg().ofType(Integer.class);
    optBdbPath = parser.accepts("bdbPath").withRequiredArg().defaultsTo("data");
    //optJdbcDriver = parser.accepts("driver").withRequiredArg().defaultsTo("com.mysql.jdbc.Driver");
    optJdbcUrl = parser.accepts("url").withRequiredArg().defaultsTo("jdbc:mysql://localhost/javacoin_testnet3");
    optJdbcUser = parser.accepts("dbuser").withRequiredArg().defaultsTo("javacoin");
    optJdbcPassword = parser.accepts("dbpass").withRequiredArg().defaultsTo("pw");
    inputfile = parser.accepts("inputfile").withRequiredArg();
    outputfile = parser.accepts("outputfile").withRequiredArg();
//    String[] args = {
//      "--inputfile", "blocks-0-100000.txt", "--prodnet", "--load", "--url", "jdbc:mysql://localhost/javacoin_test"
//    };
    OptionSet options = parser.parse(args);
    if (args.length == 0
            || options.hasArgument("help")
            || options.nonOptionArguments().size() > 0
            || (options.has("export") && options.has("import"))
            || (options.has("export") && !options.has("outputfile"))
            || (options.has("import") && !options.has("inputfile"))
            || (options.has("testnet2") && options.has("testnet3"))
            || (options.has("testnet2") && options.has("prodnet"))
            || (options.has("testnet3") && options.has("prodnet"))) {
      println(HELP_TEXT);
      return;
    }
    if (options.hasArgument("port")) {
      //listenPort = ((Integer) options.valueOf("port")).intValue();
    }
    cmdExportBlockchain = options.has("export");
    cmdImportBlockchain = options.has("import");
    isProdnet = options.has("prodnet");
    isTestNet2 = options.has("testnet2");
    isTestNet3 = options.has("testnet3");
    if (!isProdnet && !isTestNet2 && !isTestNet3)
      isTestNet3 = true;
    if (options.hasArgument("first")) {
      firstBlock = ((Integer) options.valueOf("first")).intValue();
      if (!options.hasArgument("last"))
        lastBlock = firstBlock;
    }
    if (options.hasArgument("last")) {
      lastBlock = ((Integer) options.valueOf("last")).intValue();
      if (!options.hasArgument("first"))
        firstBlock = lastBlock;
    }
    if (options.hasArgument("hash"))
      blockHash = (String) options.valueOf("hash");
    if (cmdExportBlockchain && blockHash == null && firstBlock == 0 && lastBlock == 0) {
      println("To save blocks you have to specify a range or an hash");
      return;
    }

    //println("save: " + cmdSaveBlockchain + " load: " + cmdLoadBlockchain + " prodnet: " + isProdnet + " testnet2: " + isTestNet2 + " testnet3: " + isTestNet3);
    //println("FirstBlock: " + firstBlock + " lastBlock: " + lastBlock + " inputfile: " + inputfile.value(options) + " outputfile: " + outputfile.value(options));
    BlockTool app = new BlockTool();
    app.init(options);
    if (cmdImportBlockchain) {
      //System.out.println("Press return to start import blocks to blockchain");
      //System.in.read();
      BufferedReader reader;
      if ("-".equals(inputfile.value(options)))
        reader = new BufferedReader(new InputStreamReader(System.in));
      else
        reader = new BufferedReader(new FileReader(inputfile.value(options)));
      int numBlocks = 0;
      Block block = app.readBlock(reader, false);
      while (block != null) {
        numBlocks++;
        long startTime = System.currentTimeMillis();
        blockChain.addBlock(block);
        long insertTime = System.currentTimeMillis() - startTime;
        System.out.printf("%6d Block " + BtcUtil.hexOut(block.getHash()) + " #txs: %4d insertTime(ms): %d%n", block.getTransactions().size(), insertTime);
        block = app.readBlock(reader, false);
      }
      System.out.println("Numero blocchi letti: " + numBlocks);
    } else if (cmdExportBlockchain) {
      BlockChainLink blockLink;
      try (PrintWriter writer = new PrintWriter(new File(outputfile.value(options)))) {
        if (blockHash != null) {
          blockLink = storage.getLink(BtcUtil.hexIn(blockHash));
          app.writeBlock(writer, blockLink.getBlock());
        } else {
          for (int i = firstBlock; i <= lastBlock; i++) {
            blockLink = storage.getLinkAtHeight(i);
            app.writeBlock(writer, blockLink.getBlock());
          }
        }
      }
    }
    app.close();
  }

  /**
   * Initialize and bind components together.
   */
  public void init(OptionSet options) throws BitcoinException, ClassNotFoundException {
    scriptFactory = new ScriptFactoryImpl(new KeyFactoryImpl(null));
    bitcoinFactory = isTestNet3 ? new Testnet3BitcoinFactory(scriptFactory)
            : isTestNet2 ? new Testnet2BitcoinFactory(scriptFactory)
            : new ProdnetBitcoinFactory(scriptFactory);
    // Initialize the correct storage engine
    if (STORAGE_BDB.equalsIgnoreCase(storageType)) {
      BDBStorage engine = new BDBStorage(bitcoinFactory);
      String bdbPath = (isProdnet ? "prodnet" : isTestNet2 ? "testnet2" : "testnet3") + "-db";
      if (options.has(optBdbPath))
        bdbPath = optBdbPath.value(options);
      engine.setDbPath(bdbPath);
      engine.setUseExplicitTransactions(false);
      engine.setCachePercent(90);
      engine.setDeferredWrite(true);
      engine.init();
      println("BDB Storage initialized with path: " + bdbPath);
      storage = engine;
    } else if (STORAGE_JDBC.equalsIgnoreCase(storageType)) {
      MysqlStorage engine = new MysqlStorage(bitcoinFactory);
      engine.setDataSource(DatasourceUtils.getMysqlDatasource(
              optJdbcUrl.value(options), optJdbcUser.value(options), optJdbcPassword.value(options)));
      engine.init();
      storage = engine;
    }
    blockChain = new BlockChainImpl(bitcoinFactory, storage, false);
    println("Storage initialized, last link height: " + (storage.getLastLink() == null ? 0 : storage.getLastLink().getHeight()));
  }

  /**
   * Free used resources.
   */
  public void close() {
    if (storage instanceof BDBStorage) {
      long time = System.currentTimeMillis();
      ((BDBStorage) storage).close();
      println("Time to flush BDB database: " + (System.currentTimeMillis() - time));
    }
  }

  public void testBdb(OptionSet options) throws FileNotFoundException, IOException, BitcoinException, SQLException {
    scriptFactory = new ScriptFactoryImpl(new KeyFactoryImpl(null));
    bitcoinFactory = new ProdnetBitcoinFactory(scriptFactory);
    BDBStorage bdb = new BDBStorage(bitcoinFactory);
    bdb.setDbPath(optBdbPath.value(options));
    bdb.setUseExplicitTransactions(true);
    bdb.init();
    bdb.printBlockHeaders();
    String filename = (String) options.valueOf("inputfile");
    BufferedReader reader;
    if ("-".equals(filename))
      reader = new BufferedReader(new InputStreamReader(System.in));
    else
      reader = new BufferedReader(new FileReader(filename));
    int numBlocks = 0;
    System.out.println("Infile: " + filename);
    Block block = readBlock(reader, true);
    while (block != null) {
      numBlocks++;
      //System.out.println("Letto blocco con hash: " + BtcUtil.hexOut(block.getHash()));
      BlockChainLink chainLink = new BlockChainLink(block, bitcoinFactory.newDifficulty(new BigDecimal(numBlocks * 253)), numBlocks);
      //bdb.storeBlockLink(null, chainLink);
//      BlockChainLink storedBlock = bdb.getLink(block.getHash());
//      System.out.println("[reread] block: " + storedBlock+" with "+storedBlock.getBlock().getTransactions().size()+" txs");

      //
//      for (Transaction tx : block.getTransactions()) {
//        Transaction storedTx = bdb.getTransaction(tx.getHash());
//        System.out.println("[reread] tx: " + storedTx);
//      }

      block = readBlock(reader, true);
    }

//    System.out.println("Rileggo tutti i blocchi");
//    for (int i = 1; i <= numBlocks; i++) {
//      List<SimplifiedStoredBlock> l = bdb.getBlocksAtHeight(null, i);
//      for (SimplifiedStoredBlock b : l) {
//        System.out.println("HEIGHT " + i + ": " + b);
//      }
//    }
//    bdb.printClaims();
//    bdb.printPrevHashes();
//    bdb.printDifficulty();
    //bdb.printTxBlockIndex();
    bdb.close();
  }

  public Block readBlock(BufferedReader reader, boolean doHashCheck) throws IOException, BitcoinException {
    String line = reader.readLine();
    if (line == null) {
      return null;
    }
    if (line.startsWith("block ")) {
      String[] tokens = line.split("\\s");
      byte[] hash = BtcUtil.hexIn(tokens[1]);
      byte[] previousHash = BtcUtil.hexIn(tokens[2]);
      byte[] merkleRoot = BtcUtil.hexIn(tokens[3]);
      long compressedTarget = Long.parseLong(tokens[4]);
      long creationTime = Long.parseLong(tokens[5]);
      long nonce = Long.parseLong(tokens[6]);
      long version = Long.parseLong(tokens[7]);
      int numTransactions = Integer.parseInt(tokens[8]);
      //System.out.println("Input block " + BtcUtil.hexOut(hash) + " nonce: " + nonce + " numTxs: " + numTransactions);
      List<TransactionImpl> txs = new ArrayList<>(numTransactions);
      for (int i = 0; i < numTransactions; i++) {
        txs.add(readTransaction(reader, doHashCheck));
      }
      if (doHashCheck) {
        Block block = new BlockImpl(txs, creationTime, nonce, compressedTarget, previousHash, merkleRoot, null, version);
        if (!Arrays.equals(hash, block.getHash())) {
          System.out.println("Error: invalid input block hash: " + BtcUtil.hexOut(hash) + " vs " + BtcUtil.hexOut(block.getHash()) + " (calculated)");
          return null;
        }
        return block;
      } else {
        return new BlockImpl(txs, creationTime, nonce, compressedTarget, previousHash, merkleRoot, hash, version);
      }
    } else
      return null;
  }

  public TransactionImpl readTransaction(BufferedReader reader, boolean doHashCheck) throws IOException, BitcoinException {
    String line = reader.readLine();
    if (line == null || !line.startsWith(" tx ")) {
      System.out.println("Error: expecting TX and got: " + line);
      return null;
    }
    String[] tokens = line.substring(4).split("\\s");
    byte[] hash = BtcUtil.hexIn(tokens[0]);
    long lockTime = Long.parseLong(tokens[1]);
    long version = Long.parseLong(tokens[2]);
    int numInputs = Integer.parseInt(tokens[3]);
    int numOutputs = Integer.parseInt(tokens[4]);
    List<TransactionInputImpl> inputs = new ArrayList<>(numInputs);
    for (int i = 0; i < numInputs; i++) {
      String l = reader.readLine();
      if (l == null || !l.startsWith("  in ")) {
        System.out.println("Error: expecting TX IN and got: " + l);
        return null;
      }
      String[] tk = l.substring(5).split("\\s");
      byte[] refHash = BtcUtil.hexIn(tk[0]);
      int refIndex = Integer.parseInt(tk[1]);
      long sequence = Long.parseLong(tk[2]);
      byte[] sig = BtcUtil.hexIn(tk[3]);
      inputs.add(new TransactionInputImpl(refHash, refIndex, bitcoinFactory.getScriptFactory().createFragment(sig), sequence));
    }
    List<TransactionOutputImpl> outputs = new ArrayList<>(numOutputs);
    for (int i = 0; i < numOutputs; i++) {
      String l = reader.readLine();
      if (!l.startsWith("   out ")) {
        System.out.println("Error: expecting TX OUT and got: " + l);
        return null;
      }
      String[] tk = l.substring(7).split("\\s");
      long value = Long.parseLong(tk[0]);
      byte[] script = BtcUtil.hexIn(tk[1]);
      outputs.add(new TransactionOutputImpl(value, bitcoinFactory.getScriptFactory().createFragment(script)));
    }
    if (doHashCheck) {
      TransactionImpl tx = new TransactionImpl(inputs, outputs, lockTime, version);
      if (!Arrays.equals(hash, tx.getHash())) {
        System.out.println("Error: invalid input TX hash: " + BtcUtil.hexOut(hash) + " vs " + BtcUtil.hexOut(tx.getHash()) + " (calculated)");
        return null;
      }
      return tx;
    } else {
      return new TransactionImpl(inputs, outputs, lockTime, hash, version);
    }
  }

  public void writeBlock(PrintWriter writer, Block block) throws IOException {
    writer.println("block " + BtcUtil.hexOut(block.getHash()) + " "
            + BtcUtil.hexOut(block.getPreviousBlockHash()) + " "
            + BtcUtil.hexOut(block.getMerkleRoot()) + " "
            //+ Long.toHexString(block.getCompressedTarget()) + " "
            + block.getCompressedTarget() + " "
            + block.getCreationTime() + " "
            + block.getNonce() + " "
            + block.getVersion() + " "
            + block.getTransactions().size());
    for (Transaction t : block.getTransactions()) {
      writeTransaction(writer, t);
    }
  }

  public void writeTransaction(PrintWriter writer, Transaction t) {
    List<TransactionInput> inputs = t.getInputs();
    List<TransactionOutput> outputs = t.getOutputs();
    writer.println(" tx " + BtcUtil.hexOut(t.getHash()) + " "
            + t.getLockTime() + " " + t.getVersion() + " " + inputs.size() + " " + outputs.size());
    for (TransactionInput input : inputs) {
      writer.println("  in " + BtcUtil.hexOut(input.getClaimedTransactionHash()) + " "
              + input.getClaimedOutputIndex() + " "
              + input.getSequence() + " "
              + BtcUtil.hexOut(input.getSignatureScript().toByteArray()));
    }
    for (TransactionOutput output : outputs) {
      writer.println("   out " + output.getValue() + " "
              + BtcUtil.hexOut(output.getScript().toByteArray()));
    }
  }

  public static void println(String s) {
    System.out.println(s);
  }

  // TEST -- not used
  public void readJsonBlock(String fileName) throws FileNotFoundException, IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map jsonObj = (Map) mapper.readValue(new File(fileName), Object.class);
    String hash = (String) jsonObj.get("hash");
    ArrayList<Map> txs = (ArrayList<Map>) jsonObj.get("tx");
    for (Map tx : txs) {
      println("Processing tx: " + tx.get("hash"));
      ArrayList<Map> inputs = (ArrayList<Map>) tx.get("inputs");
      ArrayList<Map> outs = (ArrayList<Map>) tx.get("out");
      for (Map input : inputs)
        println(" Input: " + input.get("prev_out"));
      for (Map output : outs) {
        long value = ((Long) output.get("value")).longValue();
        int type = ((Integer) output.get("type")).intValue();
        println(" Out -- addr: " + output.get("addr") + " value: " + value + " type: " + type);
        TransactionOutputImpl tout = new TransactionOutputImpl(value, scriptFactory.createFragment(null));
        println(" Tout: " + tout);
      }
    }
    println("Lettura da " + fileName + " hash: " + hash + " txs: " + txs);
  }
}
