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

package hu.netmind.bitcoin;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.easymock.EasyMock;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import hu.netmind.bitcoin.keyfactory.ecc.KeyFactoryImpl;
import java.util.List;
import java.util.ArrayList;
import hu.netmind.bitcoin.block.TransactionOutputImpl;
import hu.netmind.bitcoin.block.TransactionInputImpl;
import hu.netmind.bitcoin.block.TransactionImpl;
import hu.netmind.bitcoin.Script;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.TransactionInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Test
public class TransactionVerificationITTests
{
   private static Logger logger = LoggerFactory.getLogger(TransactionVerificationITTests.class);

   public void testTransactionVerification()
      throws Exception
   {
      // The data is taken from a real transaction, hash:
      // 4719e088cc1105e7aa636615a53f5e5b5082ec2201447e5d4e51449e6670a756
      // and from the transaction of the output referenced in the input, hash:
      // 984f59a91a14c2b51181574a35f86af8363dc480f68bb2847d99fb49c184832b

      ScriptFactoryImpl scriptFactory = new ScriptFactoryImpl(new KeyFactoryImpl(null));
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
      // Build the input
      TransactionInputImpl input = new TransactionInputImpl(
         HexUtil.toByteArray(
            "98 4F 59 A9 1A 14 C2 B5 11 81 57 4A 35 F8 6A F8 36 3D C4 80 F6 8B B2 84 7D 99 FB 49 C1 84 83 2B "),
         1,
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
      Script verificationScript = scriptFactory.createScript(input.getSignatureScript(),
         scriptFactory.createFragment(HexUtil.toByteArray( // The output's script
            "76 A9 14 34 28 B4 98 22 00 CE 46 53 2B 9C 54 23 08 65 44 AC 99 D3 69 88 AC")));
      Assert.assertTrue(verificationScript.execute(input));
   }

   public void testTransactionVerification2()
      throws Exception
   {
      // Data is taken from the forum thread:
      // http://forum.bitcoin.org/index.php?topic=2957.0
      // Transaction input is in:
      // ff954e099764d192c5bb531c9c14c18c230b0c0a63f02cd168a4ea94548c890f
      // The referenced output is in:
      // 945691940e0ccd9f526ee1edd57a77ce170804915749702f5564c49b1f70f330

      ScriptFactoryImpl scriptFactory = new ScriptFactoryImpl(new KeyFactoryImpl(null));
      // First build the output with script

      TransactionOutputImpl output = new TransactionOutputImpl(10200000000l,
         scriptFactory.createFragment(HexUtil.toByteArray(
            "76 A9 14 9E 35 D9 3C 77 92 BD CA AD 56 97 DD EB F0 43 53 D9 A5 E1 96 88 AC")));
      List<TransactionOutputImpl> outputs = new ArrayList<TransactionOutputImpl>();
      outputs.add(output);
      // Build the inputs
      TransactionInputImpl input1 = new TransactionInputImpl(
         HexUtil.toByteArray(
            "94 56 91 94 0E 0C CD 9F 52 6E E1 ED D5 7A 77 CE 17 08 04 91 57 49 70 2F 55 64 C4 9B 1F 70 F3 30 "),
         0,
         scriptFactory.createFragment(HexUtil.toByteArray(
            "49 "+ // Start of sig
            "30 46 02 21 00 F5 74 6B 0B 25 4F 5A 37 E7 52 51 "+
            "45 9C 7A 23 B6 DF CB 86 8A C7 46 7E DD 9A 6F DD "+
            "1D 96 98 71 BE 02 21 00 88 94 8A EA 29 B6 91 61 "+
            "CA 34 1C 49 C0 26 86 A8 1D 8C BB 73 94 0F 91 7F "+
            "A0 ED 71 54 68 6D 3E 5B 01 "+
            "41 "+ // Pub key
            "04 47 D4 90 56 1F 39 6C 8A 9E FC 14 48 6B C1 98 "+
            "88 4B A1 83 79 BC AC 2E 0B E2 D8 52 51 34 AB 74 "+
            "2F 30 1A 9A CA 36 60 6E 5D 29 AA 23 8A 9E 29 93 "+
            "00 31 50 42 3D F6 92 45 63 64 2D 4A FE 9B F4 FE "+
            "28")),
         0xFFFFFFFFl);
      TransactionInputImpl input2 = new TransactionInputImpl(
         HexUtil.toByteArray(
            "89 A6 8E B0 A3 E6 C1 58 59 2D 98 80 2C 0C B6 9B D5 B9 BF 65 33 B7 E5 6D 2C E9 6C 68 F7 2B 14 72 "),
         0,
         scriptFactory.createFragment(HexUtil.toByteArray(
            "49 "+ // Start of sig
            "30 46 02 21 00 BC E4 3A D3 AC BC 79 B0 24 7E 54 "+
            "C8 C9 1E AC 1C F9 03 75 05 00 0E 01 D1 FD 81 18 "+
            "54 D8 5B C2 1A 02 21 00 99 2A 6F 6F 2F EB 6F 62 "+
            "D3 70 6F 3B 9A AA B8 8D 9F 11 32 95 6A 1D FF A9 "+
            "26 CD 55 6E D5 53 60 DF 01")),
         0xFFFFFFFFl);
      TransactionInputImpl input3 = new TransactionInputImpl(
         HexUtil.toByteArray(
            "2C 63 1B FA 78 2C C6 27 B1 DA A1 19 AC 56 EA 7B 7E DC 19 C6 0C 63 0A 3D 1C 7C 20 B6 BB 28 81 D2 "),
         0,
         scriptFactory.createFragment(HexUtil.toByteArray(
            "49 "+ // Start of sig
            "30 45 02 20 20 97 57 36 81 61 53 77 08 FD 29 D8 "+
            "9B B1 E9 D6 48 00 79 49 EC FD ED 78 9B 51 A9 63 "+
            "24 CB 65 18 02 21 00 CD 0F 7C 30 21 39 16 48 2B "+
            "6E 16 6D 8A 4F 2B 98 1F 77 7E B1 84 CD 8A 49 5F "+
            "1B 3D 36 90 FB BF 2D 01")),
         0xFFFFFFFFl);
      List<TransactionInputImpl> inputs = new ArrayList<TransactionInputImpl>();
      inputs.add(input1);
      inputs.add(input2);
      inputs.add(input3);
      // Now build the transaction itself
      TransactionImpl transaction = new TransactionImpl(inputs,outputs,0);
      // Now try to validate the input with the provided script (note: we don't know
      // the private key belonging to the input we picked here)
      Script verificationScript = scriptFactory.createScript(input1.getSignatureScript(),
         scriptFactory.createFragment(HexUtil.toByteArray( // Claimed output 1 script
            "76 A9 14 02 BF 4B 28 89 C6 AD A8 19 0C 25 2E 70 BD E1 A1 90 9F 96 17 88 AC")));
      logger.debug("running verification script: "+verificationScript);
      Assert.assertTrue(verificationScript.execute(input1));
   }
}

