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

import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.Transaction;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Merkle tree is a binary tree of hashes. The representation inside this class
 * is capable of reconstructing the root hash with minimal information, which is the
 * leaf hashes at first. If leafs are removed from the tree, whole branches might be 
 * removed with it, and only a summary hash stays. For example:
 * <pre>
 *        A
 *      /   \
 *     B     C
 *    / \   / \
 *   D   E F   G
 * </pre>
 * If "D" and "E" are removed from the tree, "B" is sufficient to re-calculate the root while
 * being also able to prove that "F" and "G" are in the tree.
 * @author Robert Brautigam
 */
public class MerkleTree
{
   private static Logger logger = LoggerFactory.getLogger(MerkleTree.class);

   private MerkleNode root;

   private List<Transaction> transactions;
   private List<MerkleNode> outerNodes;
   private MessageDigest digest = null;

   public MerkleTree(List<Transaction> transactions)
      throws BitCoinException
   {
      this(null,transactions);
   }

   /**
    * @param outerNodes The outer nodes need only include the hash value
    * and the start and end indices.
    * @param transactions Transactions to build the tree with.
    */
   public MerkleTree(List<MerkleNode> outerNodes, List<Transaction> transactions)
      throws BitCoinException
   {
      this.outerNodes=outerNodes;
      if ( this.outerNodes == null )
         this.outerNodes = new LinkedList<MerkleNode>();
      this.transactions=transactions;
      try
      {
         digest = MessageDigest.getInstance("SHA-256");
      } catch ( NoSuchAlgorithmException e ) {
         throw new BitCoinException("can not find sha-256 algorithm for merkle hash calculation",e);
      }
   }

   /**
    * Get the list of outer nodes (those nodes which have all children already
    * removed). This has to be kept in sync with the list of transactions still
    * in the tree (and this is done with the <code>removeTransaction()</code> method).
    */
   public List<MerkleNode> getOuterNodes()
   {
      return outerNodes;
   }

   private MerkleNode createParent(MerkleNode node1, MerkleNode node2, int startIndex, int endIndex)
   {
      // Generate new hash
      digest.reset();
      digest.update(node1.getHash());
      digest.update(node2.getHash());
      byte[] firstHash = digest.digest();
      digest.reset();
      MerkleNode parent = new MerkleNode(digest.digest(firstHash), startIndex, endIndex,
            new MerkleNode[] { node1, node2 },false);
      return parent;
   }

   /**
    * Build the merkle tree from the specified outer nodes and the supplied transactions.
    */
   private void buildTree()
   {
      if ( root != null )
         return; // Already there
      // First create the leaf nodes
      List<MerkleNode> openLeafNodes = new LinkedList<MerkleNode>();
      for ( Transaction tx : transactions )
         openLeafNodes.add(new MerkleNode(tx.getHash(),-1,-1,null,false));
      // Copy other nodes
      SortedSet<MerkleNode> openNodes = new TreeSet<MerkleNode>(outerNodes);
      logger.debug("starting to build tree, open leafs: {}, open nodes: {}",openLeafNodes,openNodes);
      // Go through all open leafs and position them into the sorted
      // open nodes list. Find spots from the open nodes where there is
      // place for new nodes.
      while ( ! openLeafNodes.isEmpty() )
      {
         // Find the first spot the open nodes don't cover
         int startIndex = 0;
         Iterator<MerkleNode> openIterator = openNodes.iterator();
         while ( openIterator.hasNext() )
         {
            MerkleNode openNode = openIterator.next();
            if ( openNode.getStartIndex() > startIndex )
               break; // Next node starts later
            startIndex = openNode.getEndIndex(); // Next slot possibility
         }
         // Now merge the next two open lead nodes together to create
         // a node here
         MerkleNode node1 = openLeafNodes.remove(0);
         MerkleNode node2 = null;
         if ( openLeafNodes.isEmpty() )
            node2 = node1;
         else
            node2 = openLeafNodes.remove(0);
         MerkleNode parent = createParent(node1,node2,startIndex,startIndex+2);
         openNodes.add(parent);
         logger.debug("merging open leafs at index: {}, open leafs left: {}",startIndex,openLeafNodes);
      }
      // After processing all leafs, the open nodes should contain non-leaf nodes
      // in-order, but on separate levels. Merge them until only the root is left
      while ( openNodes.size() > 1 )
      {
         logger.debug("merging open nodes: {}",openNodes);
         // Find two nodes that are next to eachother and on the same level
         MerkleNode node1 = null;
         MerkleNode node2 = null;
         Iterator<MerkleNode> openIterator = openNodes.iterator();
         while ( (openIterator.hasNext()) && ((node1==null) || (node2==null)) )
         {
            MerkleNode nextNode = openIterator.next();
            if ( node1 == null )
            {
               node1 = nextNode;
            } else {
               if ( (node1.getEndIndex()==nextNode.getStartIndex()) &&
                    (node1.getEndIndex()-node1.getStartIndex()==nextNode.getEndIndex()-nextNode.getStartIndex()) )
               {
                  // Found two nodes next to eachother who cover the same range
                  node2 = nextNode;
               } else {
                  node1 = nextNode; // Continue looking
               }
            }
         }
         if ( node2 == null )
         {
            // If there was no pair, just duplicate the last node, and also double its range
            node2 = node1; 
            node2.setEndIndex((node2.getEndIndex()-node2.getStartIndex())+node2.getEndIndex());
         }
         // Unify the two nodes
         openNodes.remove(node1);
         openNodes.remove(node2);
         openNodes.add(createParent(node1,node2,node1.getStartIndex(),node2.getEndIndex()));
      }
      // When over, the nodes will only contain one entry (the root)
      root = openNodes.iterator().next();
   }

   private MerkleNode getParentNode(byte[] hash)
   {
      List<MerkleNode> opens = new LinkedList<MerkleNode>();
      opens.add(root);
      while ( ! opens.isEmpty() )
      {
         MerkleNode parent = opens.remove(0);
         if ( parent.getChildren() == null )
            continue;
         // Check children
         if ( Arrays.equals(parent.getChildren()[0].getHash(),hash) )
            return parent;
         if ( Arrays.equals(parent.getChildren()[1].getHash(),hash) )
            return parent;
         opens.add(parent.getChildren()[0]);
         opens.add(parent.getChildren()[1]);
      }
      return null;
   }

   /**
    * Remove a hash from the merkle tree. Removing the hash potentially also compacts
    * the tree somewhat if a whole branch can be then removed.
    */
   public void removeTransaction(Transaction tx)
   {
      // Make sure tree is build
      buildTree();
      // Remove from the list of transactions
      transactions.remove(tx);
      // Remove hash
      removeHash(tx.getHash());
   }

   private void removeHash(byte[] hash)
   {
      // Mark node as removed, and also remove sibling if it's also removed
      MerkleNode parent = getParentNode(hash);
      if ( parent == null )
         return; // This is root, don't remove
      MerkleNode thisNode = parent.getChildren()[0];
      MerkleNode otherNode = parent.getChildren()[1];
      if ( ! Arrays.equals(thisNode.getHash(),hash) )
      {
         // Swap
         thisNode = parent.getChildren()[1];
         otherNode = parent.getChildren()[0];
      }
      thisNode.setRemoved(true);
      if ( otherNode.isRemoved() )
      {
         // Both siblings are removed, so we can safely remove them altogether
         // just leave the parent hash in (but mark as removable)
         parent.setChildren(null);
         outerNodes.add(parent);
         outerNodes.remove(thisNode);
         outerNodes.remove(otherNode);
         // Now try to remove parent too, since there is nothing below it
         removeHash(parent.getHash());
      }
   }

   /**
    * Return the root hash of this tree.
    */
   public byte[] getRoot()
   {
      buildTree();
      return root.getHash();
   }

   public static class MerkleNode implements Comparable<MerkleNode>
   {
      private byte[] hash;
      private MerkleNode[] children;
      private boolean removed;
      private int startIndex = -1;
      private int endIndex = -1;

      public int compareTo(MerkleNode other)
      {
         return startIndex - other.startIndex;
      }

      public String toString()
      {
         return "Node "+startIndex+"-"+endIndex+" (removed: "+removed+")";
      }

      public MerkleNode(byte[] hash, int startIndex, int endIndex)
      {
         this(hash,startIndex,endIndex,null,true);
      }

      MerkleNode(byte[] hash, int startIndex, int endIndex, 
            MerkleNode[] children, boolean removed)
      {
         this.hash=hash;
         this.children=children;
         this.removed=removed;
         this.startIndex=startIndex;
         this.endIndex=endIndex;
      }

      public int getStartIndex()
      {
         return startIndex;
      }
      public void setStartIndex(int startIndex)
      {
         this.startIndex=startIndex;
      }

      public int getEndIndex()
      {
         return endIndex;
      }
      public void setEndIndex(int endIndex)
      {
         this.endIndex=endIndex;
      }

      public boolean isRemoved()
      {
         return removed;
      }
      public void setRemoved(boolean removed)
      {
         this.removed=removed;
      }
      public byte[] getHash()
      {
         return hash;
      }
      public void setChildren(MerkleNode[] children)
      {
         this.children=children;
      }
      public MerkleNode[] getChildren()
      {
         return children;
      }
   }
}


