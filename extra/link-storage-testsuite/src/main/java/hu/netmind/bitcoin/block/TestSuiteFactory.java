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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.LinkedList;

/**
 * This factory can be used to apply the test suite to an external module.
 * Use the following steps:
 * <ul>
 * <li>Import this module as a test dependency.</li>
 * <li>Implement the <code>StorageProvider</code> interface to generate your
 * implementation of the link storage.</li>
 * <li>Create a test class with a TestNG Factory annotated method to return
 * the test objects from this factory.</li>
 * </ul>
 * For an example look at the tests in this module.
 * @author Robert Brautigam
 */
public class TestSuiteFactory
{
   private static Logger logger = LoggerFactory.getLogger(TestSuiteFactory.class);

   public static <T extends BlockChainLinkStorage> Object[] getTestSuite(StorageProvider<T> provider)
   {
      List<StorageTests<T>> result = new LinkedList<StorageTests<T>>();
      result.add(new CleanTests<T>());
      // Add new tests before this line
      for ( StorageTests<T> tests : result )
         tests.init(provider);
      return result.toArray();
   }
}

