package hu.netmind.bitcoin.net;

import hu.netmind.bitcoin.net.AddrMessage.AddressEntry;
import java.io.IOException;
import java.util.List;

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

   public AddrMessage newAddrMessage(List<AddressEntry> entries)
      throws IOException
   {
      return new AddrMessage(messageMagic, entries);
   }

   public AlertMessage newAlertMessage(String message)
      throws IOException
   {
      return new AlertMessage(messageMagic, message);
   }

   public BlockMessage newBlockMessage(BlockHeader header, List<Tx> transactions)
      throws IOException
   {
      return new BlockMessage(messageMagic, header, transactions);
   }

   public GetAddrMessage newGetAddrMessage()
      throws IOException
   {
      return new GetAddrMessage(messageMagic);
   }

   public GetBlocksMessage newGetBlocksMessage(long messageVersion, List<byte[]> hashStarts, byte[] hashStop)
      throws IOException
   {
      return new GetBlocksMessage(messageMagic, messageVersion, hashStarts, hashStop);
   }

   public GetDataMessage newGetDataMessage(List<InventoryItem> items)
      throws IOException
   {
      return new GetDataMessage(messageMagic, items);
   }

   public GetHeadersMessage newGetHeadersMessage(long messageVersion, List<byte[]> hashStarts, byte[] hashStop)
      throws IOException
   {
      return new GetHeadersMessage(messageMagic, hashStarts, hashStop);
   }

   public InvMessage newInvMessage(List<InventoryItem> items)
      throws IOException
   {
      return new InvMessage(messageMagic, items);
   }

   public PingMessage newPingMessage()
      throws IOException
   {
      return new PingMessage(messageMagic);
   }

   public VerackMessage newVerackMessage()
      throws IOException
   {
      return new VerackMessage(messageMagic);
   }

   public VersionMessage newVersionMessage(long version, long services, long timestamp,
      NodeAddress receiverAddress, NodeAddress senderAddress, long nonce, String secondaryVersion, long startHeigth)
      throws IOException
   {
      return new VersionMessage(messageMagic, version, services, timestamp,
         receiverAddress, senderAddress, nonce, secondaryVersion, startHeigth);
   }
}
