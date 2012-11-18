package it.nibbles.javacoin.block;

import it.nibbles.javacoin.BitcoinException;
import it.nibbles.javacoin.Block;
import it.nibbles.javacoin.ScriptFactory;

/**
 *
 * @author Alessandro Polverini
 */
public class TesterBitcoinFactory extends ProdnetBitcoinFactory
{
   private Block genesisBlock;

   public TesterBitcoinFactory(Block genesisBlock, ScriptFactory scriptFactory) throws BitcoinException
   {
      super(scriptFactory);
      this.genesisBlock = genesisBlock;
   }

   @Override
   public Block getGenesisBlock()
   {
      return genesisBlock;
   }

   
}
