package common;
import java.io.IOException;
import java.util.Arrays;

import disassembler.Mips_Disassembler;
import assembler.Mips_Assembler;
import mifTools.MIF_creator;
import mifTools.MifToBinCreator;

public class MipsAssembler
{

	public static void main(String[] args) throws IOException
	{		
		if(args.length == 0)
		{
			System.out.println("Usage: MipsAssembler -asm \"input.asm\" (-mif) (\"output.bin\")");
			System.out.println("MipsAssembler -dsm \"input.mif\" (\"output.asm\"");
			System.exit(0);
		}
		if(args[0].equals("-asm")) //assemble
		{
			Mips_Assembler assembler = new Mips_Assembler(args[1]);
			
			assembler.createLabels();
			assembler.assemble();
			
			if(Arrays.asList(args).contains("-mif"))
			{
				System.out.println("Generating memory initialization file...");
				String outName;
				if(args.length > 3)
				{
					outName = args[3];
				}
				else
				{
					outName = "output.mif";
				}
				
				MIF_creator mifCreator = new MIF_creator(outName,args[1].split("[.]")[0] + ".bin"); //args0 is the input name
				
				mifCreator.convertAndClose();
			}
		
			System.out.println("Complete!");
		}
		else if(args[0].equals("-dsm")) //disassemble
		{
			MifToBinCreator binMaker = new MifToBinCreator(args[1].toLowerCase());
			
			String outName;
			
			if(args.length > 3)
			{
				outName = args[3];
			}
			else
			{
				outName = "output.mif";
			}
			
			try
			{
				binMaker.convertAndClose();
			} 
			catch (Exception e)
			{
				System.out.println(e.getMessage());
			}
			
			Mips_Disassembler disassembler = new Mips_Disassembler(outName,args[1].split("[.]")[0] + ".bin");
			
			try
			{
				disassembler.disassembleAndClose();
			} 
			catch (Exception e)
			{
				System.out.println(e.getMessage());
			}
			
			System.out.println("Complete!");
		}
		
	}

}
