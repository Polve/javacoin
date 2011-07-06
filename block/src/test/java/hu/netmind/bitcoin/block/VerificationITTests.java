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
import hu.netmind.bitcoin.keyfactory.ecc.KeyFactoryImpl;
import java.util.List;
import java.util.ArrayList;
import hu.netmind.bitcoin.Script;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.TransactionInput;


@Test
public class VerificationITTests
{
   public void testTransactionVerification()
      throws Exception
   {
      // The data is taken from a real transaction, hash:
      // 4719e088cc1105e7aa636615a53f5e5b5082ec2201447e5d4e51449e6670a756
      // and from the transaction of the output referenced in the input, hash:
      // 984f59a91a14c2b51181574a35f86af8363dc480f68bb2847d99fb49c184832b

      ScriptFactoryImpl scriptFactory = new ScriptFactoryImpl(new KeyFactoryImpl(null,null));
      // First build the 2 outputs with script
      TransactionOutputImpl output1 = new TransactionOutputImpl(203000000,
         scriptFactory.createFragment(HexUtil.toByteArray(
            "76 A9 14 20 CA C8 9D 2F 1F C9 11 1B 38 BC 5F D7 27 8B E6 14 A7 89 C4 88 AC")));
      TransactionOutputImpl output2 = new TransactionOutputImpl(300000000,
         scriptFactory.createFragment(HexUtil.toByteArray(
            "76 A9 14 17 BE E5 04 89 99 BC 6D 7C CD B0 62 AE 06 C8 FD F8 E0 0B 17 88 AC")));
      List<TransactionOutputImpl> outputs = new ArrayList<TransactionOutputImpl>();
      outputs.add(output1);
      outputs.add(output2);
      // Before building the input, we have to build the claimed output
      Transaction claimedTransaction = EasyMock.createMock(Transaction.class);
      EasyMock.expect(claimedTransaction.getHash()).andReturn(HexUtil.toByteArray(
               "2B 83 84 C1 49 FB 99 7D 84 B2 8B F6 80 C4 3D 36 F8 6A F8 35 4A 57 81 11 B5 C2 14 1A A9 59 4F 98")).anyTimes();
      EasyMock.replay(claimedTransaction);
      TransactionOutputImpl claimedOutput = new TransactionOutputImpl(503000000,
         scriptFactory.createFragment(HexUtil.toByteArray(
            "76 A9 14 34 28 B4 98 22 00 CE 46 53 2B 9C 54 23 08 65 44 AC 99 D3 69 88 AC")));
      claimedOutput.setTransaction(claimedTransaction);
      claimedOutput.setIndex(1);
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
         0xFFFFFFFFl); // We assume sequence number was UINT_MAX (not seen in block explorer)
      List<TransactionInputImpl> inputs = new ArrayList<TransactionInputImpl>();
      inputs.add(input);
      // Now build the transaction itself
      TransactionImpl transaction = new TransactionImpl(inputs,outputs,0);
      // Now try to validate the input with the provided script (note: we don't know
      // the private key belonging to the input we picked here)
      Script verificationScript = scriptFactory.createScript(input.getSignatureScript(),claimedOutput.getScript());
      Assert.assertTrue(verificationScript.execute(input));
   }
}

