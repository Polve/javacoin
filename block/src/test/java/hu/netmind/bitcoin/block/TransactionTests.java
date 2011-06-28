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

package hu.netmind.bitcoin.block;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.easymock.EasyMock;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import java.util.List;
import java.util.ArrayList;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.TransactionInput;

/**
 * @author Robert Brautigam
 */
@Test
public class TransactionTests
{
   public void testTransactionHashing()
      throws Exception
   {
      // The data is taken from a real transaction, hash:
      // 4719e088cc1105e7aa636615a53f5e5b5082ec2201447e5d4e51449e6670a756

      ScriptFactoryImpl scriptFactory = new ScriptFactoryImpl(null);
      // First build the 2 outputs with script
      TransactionOutputImpl output1 = new TransactionOutputImpl(203000000,
         scriptFactory.createFragment(HexUtil.toByteArray(
            "76 A9 20 CA C8 9D 2F 1F C9 11 1B 38 BC 5F D7 27 8B E6 14 A7 89 C4 88 AC")));
      TransactionOutputImpl output2 = new TransactionOutputImpl(300000000,
         scriptFactory.createFragment(HexUtil.toByteArray(
            "76 A9 17 BE E5 04 89 99 BC 6D 7C CD B0 62 AE 06 C8 FD F8 E0 0B 17 88 AC")));
      List<TransactionOutputImpl> outputs = new ArrayList<TransactionOutputImpl>();
      outputs.add(output1);
      outputs.add(output2);
      // Before building the input, we have to simulate the claimed output
      Transaction claimedTransaction = EasyMock.createMock(Transaction.class);
      EasyMock.expect(claimedTransaction.getHash()).andReturn(HexUtil.toByteArray(
               "98 4F 59 A9 1A 14 C2 B5 11 81 57 4A 35 F8 6A F8 36 3D C4 80 F6 8B B2 84 7D 99 FB 49 C1 84 83 2B"));
      EasyMock.replay(claimedTransaction);
      TransactionOutput claimedOutput = EasyMock.createMock(TransactionOutput.class);
      EasyMock.expect(claimedOutput.getTransaction()).andReturn(claimedTransaction);
      EasyMock.expect(claimedOutput.getIndex()).andReturn(1);
      EasyMock.replay(claimedOutput);
      // Build the input
      TransactionInputImpl input = new TransactionInputImpl(claimedOutput,
         scriptFactory.createFragment(HexUtil.toByteArray(
            "47 "+ // Start of sig
            "30 44 02 20 15 78 04 17 3F 7E 25 82 65 7B 9C 40 "+
            "26 CC 72 9E F8 12 3E 2E 38 79 8D 3F 6A 9A 9B 54 "+
            "A0 C6 9A 28 02 20 49 EA EC 3E DD F5 FB 2B 4F 35 "+
            "77 F1 0F 9D 9A 3C 94 5B B0 CE 6C 1C 15 D1 4D 3B "+
            "03 23 B5 77 71 70 01 "+
            "41 "+ // Pub key
            "04 DE E1 6D 9F 0B 55 44 33 0C C8 00 C4 48 3F F9 "+
            "03 6F 5F B4 FF A6 F8 D7 36 77 17 DC E3 A6 F4 46 "+
            "17 FE E3 1F 19 59 37 2D 9C 56 AD F2 B3 DA FD 87 "+
            "37 2C D8 2D 21 8C D0 73 97 D7 F5 8D DF DE 9A 42 "+
            "F3")),
         0xFFFFFFFF); // We assume sequence number was UINT_MAX (not seen in block explorer)
      List<TransactionInputImpl> inputs = new ArrayList<TransactionInputImpl>();
      inputs.add(input);
      // Now build the transaction itself, the hash should be automaticall generated
      TransactionImpl transaction = new TransactionImpl(inputs,outputs,0); // We assume lock time was 0 (not seen in block explorer)
      // Check generated hash
      Assert.assertEquals(HexUtil.toHexString(transaction.getHash()), 
               "47 19 E0 88 CC 11 05 E7 AA 63 66 15 A5 3F 5E 5B 50 82 EC 22 01 44 7E 5D 4E 51 44 9E 66 70 A7 56");
   }
}

