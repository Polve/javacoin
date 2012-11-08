/**
 * Copyright (C) 2012 Alessandro Polverini
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
package it.nibbles.bitcoin;

import hu.netmind.bitcoin.BitcoinException;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.BlockChain;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.block.BitcoinFactory;
import hu.netmind.bitcoin.block.BlockChainImpl;
import hu.netmind.bitcoin.block.BlockImpl;
import hu.netmind.bitcoin.block.ProdnetBitcoinFactory;
import hu.netmind.bitcoin.block.Testnet2BitcoinFactory;
import hu.netmind.bitcoin.block.Testnet3BitcoinFactory;
import it.nibbles.javacoin.block.jdbc.DatasourceUtils;
import it.nibbles.javacoin.block.jdbc.JdbcChainLinkStorage;
import it.nibbles.javacoin.block.jdbc.MysqlStorage;
import hu.netmind.bitcoin.keyfactory.ecc.KeyFactoryImpl;
import hu.netmind.bitcoin.net.BitcoinInputStream;
import hu.netmind.bitcoin.net.BlockMessage;
import hu.netmind.bitcoin.net.MessageMarshaller;
import hu.netmind.bitcoin.net.p2p.AddressSource;
import hu.netmind.bitcoin.net.p2p.Node;
import hu.netmind.bitcoin.net.p2p.source.DNSFallbackNodesSource;
import hu.netmind.bitcoin.net.p2p.source.IrcAddressSource;
import hu.netmind.bitcoin.net.p2p.source.NodeSource;
import hu.netmind.bitcoin.net.p2p.source.RandomizedNodesSource;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import it.nibbles.bitcoin.utils.BtcUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Download all the blocks available on the Bitcoin network and adds them to the
 * chain.
 *
 */
public class ChainDownloader {

  private static Logger logger = LoggerFactory.getLogger(ChainDownloader.class);
  private static boolean isProdnet = false;
  private static boolean isTestnet2 = false;
  private static boolean isTestnet3 = false;
  private static AddressSource customAddressSource = null;
  private static String jdbcUrl;
  private static String jdbcUser;
  private static String jdbcPassword;
  private Node node = null;
  private StdNodeHandler nodeHandler;

  public static void main(String[] args)
          throws Exception {
    OptionSpec<String> optJdbcUrl;
    OptionSpec<String> optJdbcUser;
    OptionSpec<String> optJdbcPassword;
    OptionParser parser = new OptionParser();
    parser.accepts("help", "Show all options");
    parser.accepts("testnet2", "Connecto to Bitcoin old testnet2 network");
    parser.accepts("testnet3", "Connecto to Bitcoin testnet3 network (default)");
    parser.accepts("prodnet", "Connecto to Bitcoin production network");
    parser.accepts("connect", "Disable peer discovery and connect just to the give peer (host:port)").withRequiredArg();
    optJdbcUrl = parser.accepts("url").withRequiredArg();
    optJdbcUser = parser.accepts("dbuser").withRequiredArg().defaultsTo("javacoin");
    optJdbcPassword = parser.accepts("dbpass").withRequiredArg().defaultsTo("pw");
    OptionSet options = parser.parse(args);
    if (options.has("help")) {
      parser.printHelpOn(System.out);
      System.exit(1);
    }
    isProdnet = options.has("prodnet");
    isTestnet2 = options.has("testnet2");
    isTestnet3 = options.has("testnet3");
    if (!isProdnet && !isTestnet2 && !isTestnet3)
      isTestnet3 = true;

    if (options.has("url")) {
      jdbcUrl = optJdbcUrl.value(options);
    } else {
      jdbcUrl = "jdbc:mysql://localhost/javacoin_" + (isTestnet2 ? "testnet2" : isTestnet3 ? "testnet3" : "prodnet");
    }
    jdbcUser = optJdbcUser.value(options);
    jdbcPassword = optJdbcPassword.value(options);

    if (options.has("connect")) {
      customAddressSource = new NodeSource((List<String>) options.valuesOf("connect"),
              isProdnet ? 8333 : 18333);

    }
    List<String> peers = (List<String>) options.valuesOf("connect");
    for (String peer : peers) {
      System.out.println("OPTIONS Connect: " + peer);
    }

    ChainDownloader app = new ChainDownloader();
    logger.debug("init...");
    app.init();
    try {
      logger.debug("run...");
      app.run();
    } catch (RuntimeException e) {
      logger.error("Exception: " + e.getMessage(), e);
      throw e;
    } finally {
      logger.debug("close...");
      app.close();
    }
  }

  /**
   * Free used resources.
   */
  public void close() {
    nodeHandler.stop();
//      if (storage != null)
//      {
//         storage.close();
//      }
  }

  /**
   * Run the client and listen for new blocks forever.
   */
  public void run() {
    try {
//         // Start the node
//         node.start();
      nodeHandler.run();
      // Wait for keypress to end
      System.in.read();
    } catch (Exception e) {
      logger.error("error while starting node or waiting for enter", e);
    }
  }

  /**
   * Initialize and bind components together.
   */
  public void init()
          throws BitcoinException, ClassNotFoundException {
    // Initialize the chain
    ScriptFactoryImpl scriptFactory = new ScriptFactoryImpl(new KeyFactoryImpl(null));
    BitcoinFactory bitcoinFactory = isTestnet2
            ? new Testnet2BitcoinFactory(scriptFactory)
            : isTestnet3
            ? new Testnet3BitcoinFactory(scriptFactory)
            : new ProdnetBitcoinFactory(scriptFactory);
    logger.debug("bitcoin factory initialized");
    MysqlStorage storage = new MysqlStorage(bitcoinFactory);
    storage.setDataSource(DatasourceUtils.getMysqlDatasource(jdbcUrl, jdbcUser, jdbcPassword));
    /*
     storage.setDataSource(DatasourceUtils.getMysqlDatasource("jdbc:mysql://localhost/javacoin_"
     + (isTestnet2 ? "testnet2" : isTestnet3 ? "testnet3" : "prodnet"), "javacoin", "pw"));
     */
    storage.init();
    logger.debug("block storage initialized");
    JdbcChainLinkStorage nodeStorage = new JdbcChainLinkStorage();
    nodeStorage.setDataSource(DatasourceUtils.getMysqlDatasource(jdbcUrl, jdbcUser, jdbcPassword));
    /*
     nodeStorage.setDataSource(DatasourceUtils.getMysqlDatasource("jdbc:mysql://localhost/javacoin_"
     + (isTestnet2 ? "testnet2" : isTestnet3 ? "testnet3" : "prodnet"), "javacoin", "pw"));
     */
    nodeStorage.init();
    logger.debug("node storage initialized");
    BlockChain chain = new BlockChainImpl(bitcoinFactory, storage, false);
    logger.debug("blockchain initialized");
    // Introduce a small check here that we can read back the genesis block correctly
    Block genesisBlock = storage.getGenesisLink().getBlock();
    logger.debug("Genesis block hash: " + BtcUtil.hexOut(genesisBlock.getHash()) + " nonce: " + genesisBlock.getNonce());
    genesisBlock.validate();
    logger.info((isTestnet2 ? "[TESTNET2]" : isTestnet3 ? "[TESTNET3]" : "[PRODNET]") + " initialized chain, last link height: " + chain.getHeight());
    // Initialize p2p node
    node = new Node(bitcoinFactory.getMessageMagic());
    node.setPort((isTestnet2 || isTestnet3) ? 18733 : 7333);
    node.setMinConnections(10);
    node.setMaxConnections(100);
    AddressSource defaultAddressSource;
    if (isTestnet2) {
      // addressSource = new LocalhostTestnetNodeSource();
      StorageFallbackNodesSource source = new StorageFallbackNodesSource(nodeStorage);
      //source.setFallbackSource(new LocalhostTestnetNodeSource());
      source.setFallbackSource(new IrcAddressSource("#bitcoinTEST"));
      defaultAddressSource = source;
    } else if (isTestnet3) {
      StorageFallbackNodesSource source = new StorageFallbackNodesSource(nodeStorage);
      source.setFallbackSource(new IrcAddressSource("#bitcoinTEST3"));
      defaultAddressSource = source;
    } else {
      // node.setAddressSource(new LocalhostNodeSource());
      // addressSource = new DNSFallbackNodesSource();
      StorageFallbackNodesSource source = new StorageFallbackNodesSource(nodeStorage);
      source.setFallbackSource(new DNSFallbackNodesSource());
      defaultAddressSource = source;
    }
    if (customAddressSource != null)
      node.setAddressSource(customAddressSource);
    else
      node.setAddressSource(defaultAddressSource);
    //node.addHandler(new DownloaderHandler());
    logger.debug(defaultAddressSource.toString());
    nodeHandler = new StdNodeHandler(node, bitcoinFactory, chain, storage, nodeStorage);
    logger.info("Node handler: " + nodeHandler);
  }

  public void testBlock41980OfTestnet2() throws BitcoinException {
    BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(BtcUtil.hexIn(
            "FABFB5DA626C6F636B00000000000000550600008F8306AC0100000017467951795F6"
            + "230920B28555A78DBA1DF352BF74CA98CFC2A00250200000000F1889B2E7D20AF6515"
            + "E3880E2E63951814BD96ECB8389FE37342CC6B7045D796DBE0BD4E56D8031CB3287D2"
            + "806010000000100000000000000000000000000000000000000000000000000000000"
            + "00000000FFFFFFFF070456D8031C011BFFFFFFFF01A074B4300100000043410441FE2"
            + "0B7922ED8CCFB8B2788A10B59C4ED22DCD09D74E9D67C8714523BD56BFC5E3C8F7961"
            + "5391B14028108AB1950BC324268F40905993163232A72AF6A8C13BAC00000000DF490"
            + "71601D79295A9BB2F45FAC01B4783656374DC1AD04CFF67CD39483BDABCE027DF0293"
            + "010000008C493046022100832B8F07E11E7CFA2ABD5A47488B304E5BB1A04BA33F956"
            + "DF0F5819484E20E7D022100C9DECB08E7739CBCAC60FE5D60D746BA7A7E15E22EBF4C"
            + "EB472618FAA0334837014104E22790E6CE548E26E2B69DAC9627C20DF544163679C5A"
            + "7F371DACECCD1169455F7FA2685650F01DD45BD3D53789D08B0A6541158B399AEADD7"
            + "44150366A635ACFFFFFFFF02007AD285050000001976A914CE7CE0716D473FC8F6100"
            + "480D1A9546FF2F6F2FC88AC804F5049000000001976A9145682781E9AFA6C0B039E32"
            + "D469D5212A61D8A8FA88AC000000000100000001FCCF757E72FC63FE4C883005BC51D"
            + "EE3FD33155681126ABEA69BE4026FAE710600000000494830450221008B8A725578AF"
            + "50623878A75E528FEA37CA04A43D683F3C9CF01295F4690326DB02203E391FF6FD4BD"
            + "60D39C8C30B1E54216552E92367BE29397FC8E95DC1A2FB6E1501FFFFFFFF0271AEEB"
            + "02000000001976A914DB5F9A274DFF673F6A9E515EE50368DD9732DFE688AC4F010B2"
            + "7010000001976A9141E4019C9B18F640C9FBB6FC621261EABD0D505DF88AC00000000"
            + "01000000021E27E5E8F55F58809EFB4F6D9BC4E1DA944544A14AC1D0B648E20CEC2B4"
            + "D8F40000000008C4930460221008491ED4F5B8DA08D98FB9861A50973C054F1C0E8EC"
            + "708B92DCF5C88950243435022100E495F7E68A60CC56276D3D2DDBF4B0D1D6CF7FD83"
            + "B9554D15CB53550EA3806F401410492AD765E8E807392FCB3ADF07FE3677932EB045D"
            + "5D1DFDF512F8F17B5B65DE712A0471A266E103B8E2154EF3E9E026401891720F10C61"
            + "51504572489ED1119FEFFFFFFFFF18BDA96E7B9D29B868090B38EE8052F7B1D6F6344"
            + "87606C9C1B7F525681E01F010000008B483045022100E90174A696032B9FEACFCBDF8"
            + "DD8D827923088373B49397B3C2EC9443DB98A0E0220543C859F5B7E1322869EA4655C"
            + "09D9B6DA1AEDF1A7F864E73EC2A236347AAE7801410492AD765E8E807392FCB3ADF07"
            + "FE3677932EB045D5D1DFDF512F8F17B5B65DE712A0471A266E103B8E2154EF3E9E026"
            + "401891720F10C6151504572489ED1119FEFFFFFFFF0200E40B54020000001976A9146"
            + "5C23351F353869F38B93E4F82BA7D43842B33AF88AC602522FC060000001976A91431"
            + "05910BCBBACA30A9F610D4DCCD4893FB271A4688AC0000000001000000011E27E5E8F"
            + "55F58809EFB4F6D9BC4E1DA944544A14AC1D0B648E20CEC2B4D8F40010000008C4930"
            + "46022100B1E9696E819027E5C96AC71EAFC5DF0AECD4556286E2715DD03DEABF9BA53"
            + "787022100C907BAAC4374EAEAC5D77ADA4005AAE6FEEF6C5DDEB9140D5F9044DA241C"
            + "26C0014104CE6BE923F961E6E6A4164161F692A36A72055C3244180243EBF870EAF08"
            + "BEE1BDD9CB691D7E0E4F077DCBA3DFF1220FB733BC8583D2FCC176D0EB12D21B0EF7A"
            + "FFFFFFFF0200F2052A010000001976A9143105910BCBBACA30A9F610D4DCCD4893FB2"
            + "71A4688AC608A0E24010000001976A91465C23351F353869F38B93E4F82BA7D43842B"
            + "33AF88AC00000000CAF8E73901B7684A7A4E383637FA2C59F0A0FB96E6A766B9D43E7"
            + "CADF5EEC0B89694D8598A000000008A4730440220186596FC3108E8CC704E2883DD2C"
            + "9FAF60F6D48122D67B8216089F30E7684F6302200EE842F8BF98446002DCB63744181"
            + "DD79CEBDA54EE03A60F17E65C98C6CE9E8DA04104B3E7BDC43812EA4369FE71ED1431"
            + "A8F1582E188AEF4C868554F345D224E2B2508FECE494E0754F719C71B43DCEEE08DCE"
            + "FFCCA755624487CDA56A74801654749FFFFFFFF02C0B72CED040000001976A914426F"
            + "8309EC847B40DC00C66DA3B9D3799684072188AC00809698000000001976A91456827"
            + "81E9AFA6C0B039E32D469D5212A61D8A8FA88AC00000000")));
    ScriptFactoryImpl scriptFactory = new ScriptFactoryImpl(new KeyFactoryImpl(null));
    BitcoinFactory bitcoinFactory = new Testnet2BitcoinFactory(scriptFactory);
    // Unmarshall
    MessageMarshaller marshal = new MessageMarshaller(bitcoinFactory.getMessageMagic());
    BlockMessage message = null;
    try {
      message = (BlockMessage) marshal.read(input);
    } catch (IOException ex) {
      logger.error("Marshaller IO err: " + ex.getMessage());
      System.exit(0);
    }
    // Check
    assert message.getMagic() == bitcoinFactory.getMessageMagic();
    assert message.getCommand().equals("block");
    assert message.verify();
    assert message.getHeader().getVersion() == 1;
    assert message.getHeader().getTimestamp() == 1321066715000L;
    assert message.getHeader().getDifficulty() == 470014038;
    assert message.getHeader().getNonce() == 679291059;
    assert message.getTransactions().size() == 6;
    BlockImpl block = BlockImpl.createBlock(scriptFactory, (BlockMessage) message);
    for (Transaction tx : block.getTransactions()) {
      logger.info(tx.toString());
      tx.validate();
    }
    block.validate();
    Transaction tx = block.getTransactions().get(1);
    logger.info("Details for transaction " + BtcUtil.hexOut(tx.getHash()) + " isCoinbase: " + tx.isCoinbase() + " locktime: " + tx.getLockTime());
    for (TransactionInput in : tx.getInputs()) {
      logger.info("IN: " + BtcUtil.hexOut(in.getClaimedTransactionHash()) + "/" + in.getClaimedOutputIndex()
              + " " + BtcUtil.hexOut(in.getSignatureScript().toByteArray()));
    }
    for (TransactionOutput out : tx.getOutputs()) {
      logger.info("OUT value: " + out.getValue() + " script: " + out.getScript() + " index: " + out.getIndex());
    }
    logger.info("All checks OK on block " + message.getHeader());
  }

  public class LocalhostNodeSource extends RandomizedNodesSource {

    @Override
    protected List<InetSocketAddress> getInitialAddresses() {
      List<InetSocketAddress> list = new ArrayList<>();
      list.add(new InetSocketAddress("127.0.0.1", 8333));
      return list;
    }
  }

  public class LocalhostTestnetNodeSource extends RandomizedNodesSource {

    @Override
    protected List<InetSocketAddress> getInitialAddresses() {
      List<InetSocketAddress> list = new ArrayList<>();
      list.add(new InetSocketAddress("127.0.0.1", 18333));
      return list;
    }
  }
}
