package hu.netmind.bitcoin.net.p2p;

import hu.netmind.bitcoin.net.p2p.source.IrcAddressSource;
import java.net.InetSocketAddress;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author dusty
 */
public class IrcAddressSourceTests
{

   private final Logger logger = LoggerFactory.getLogger(this.getClass());

   public IrcAddressSourceTests()
   {
      logger.debug(this.getClass() + " initialing");
   }
   // TODO add test methods here.
   // The methods must be annotated with annotation @Test. For example:
   //

   @Test
   public void testUserList()
   {
      IrcAddressSource addressSource = new IrcAddressSource("#bitcoinTEST3");
      logger.debug("connected" + addressSource);
      List<InetSocketAddress> l = addressSource.getAddresses();
      logger.debug("numero indirizzi: " + l.size());
      for (InetSocketAddress add : l)
      {
         logger.debug("testnet3 client:" + add);
      }
      assertTrue(l.size() > 0);
   }

   @BeforeClass
   public static void setUpClass() throws Exception
   {
   }

   @AfterClass
   public static void tearDownClass() throws Exception
   {
   }

   @BeforeMethod
   public void setUpMethod() throws Exception
   {
   }

   @AfterMethod
   public void tearDownMethod() throws Exception
   {
   }
}
