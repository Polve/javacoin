package hu.netmind.bitcoin.net;

import hu.netmind.bitcoin.net.AddrMessage.AddressEntry;
import java.io.IOException;
import java.util.List;
import sun.security.krb5.internal.crypto.Nonce;

/**
 *
 * @author dusty
 */
public class NetworkMessageFactory
{

   private long messageMagic;

   public NetworkMessageFactory(long messageMagic)
   {
      this.messageMagic = messageMagic;
   }

   AddrMessage newAddrMessage(List<AddressEntry> entries)
      throws IOException
   {
      return new AddrMessage(messageMagic, entries);
   }

   AlertMessage newAlertMessage(String message)
      throws IOException
   {
      return new AlertMessage(messageMagic, message);
   }

   BlockMessage newBlockMessage(BlockHeader header, List<Tx> transactions)
      throws IOException
   {
      return new BlockMessage(messageMagic, header, transactions);
   }

   GetAddrMessage newGetAddrMessage()
      throws IOException
   {
      return new GetAddrMessage(messageMagic);
   }

   GetBlocksMessage newGetBlocksMessage(long messageVersion, List<byte[]> hashStarts, byte[] hashStop)
      throws IOException
   {
      return new GetBlocksMessage(messageMagic, messageVersion, hashStarts, hashStop);
   }

   GetHeadersMessage newGetHeadersMessage(long messageVersion, List<byte[]> hashStarts, byte[] hashStop)
      throws IOException
   {
      return new GetHeadersMessage(messageMagic, hashStarts, hashStop);
   }

   InvMessage newInvMessage(List<InventoryItem> items)
      throws IOException
   {
      return new InvMessage(messageMagic, items);
   }

   PingMessage newPingMessage()
      throws IOException
   {
      return new PingMessage(messageMagic);
   }

   VerackMessage newVerackMessage()
      throws IOException
   {
      return new VerackMessage(messageMagic);
   }

   VersionMessage newVersionMessage(long version, long services, long timestamp,
      NodeAddress receiverAddress, NodeAddress senderAddress, long nonce, String secondaryVersion, long startHeigth)
      throws IOException
   {
      return new VersionMessage(messageMagic, version, services, timestamp,
         receiverAddress, senderAddress, nonce, secondaryVersion, startHeigth);
   }
}
