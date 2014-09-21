package disassembler;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

import common.InstructionType;


public class Mips_Disassembler //converts from a binary to an ASM
{
	FileInputStream bin;
	BufferedWriter asm;
	
	public Mips_Disassembler() throws IOException
	{
		this("output.asm", "out.bin");
	}
	
	public Mips_Disassembler(String out, String in) throws IOException
	{
		bin = new FileInputStream(in);
		asm = new BufferedWriter(new FileWriter(out,false));
	}
	
	public void disassembleAndClose() throws Exception
	{
		byte[] b = new byte[4];
		
		int bytesRead = 0, currentWord = 0;
		
		do		
		{
			bytesRead = bin.read(b);
			
			currentWord = ByteBuffer.wrap(b).getInt();
			
			if(bytesRead >= 0)
			{
				asm.write(disassembleInstruction(currentWord) + "\n");
			}
		}while(bytesRead >= 0);
		
		asm.close();
		bin.close();
	}
	
	private String disassembleInstruction(int currentWord) throws Exception
	{
		int operation = parseOperation(currentWord);
		int function; //for use in R type instructions
		
		InstructionType it;
		
		String retVal = "";
		
		switch(operation) //first determine instruction type
		{
		case 0: //this is an R type operation
			it = InstructionType.R;
			break;
		case 0x08:				
		case 0x09:
		case 0x0C:
		case 0x04:
		case 0x05:
		case 0x24:
		case 0x25:
		case 0x0F:
		case 0x23:
		case 0x0D:
		case 0x0A:
		case 0x0B:
		case 0x28:
		case 0x29:
		case 0x2B:
			it = InstructionType.I;
			break;
		case 0x02:
		case 0x03:
			it = InstructionType.J;
			break;
		default: //what? this isn't an instruction
			System.out.println("No such opcode, exiting bin creation...");
			throw new Exception("no such opcode");
		}
		
		//next we determine the first part of the return string, the instruction
		
		if(it == InstructionType.R) //if R type, op code is 0, and the instruction is based on the function
		{
			function = parseFunction(currentWord);
			switch(function)
			{
			case 0x20:
				retVal += "add ";
				break;
			case 0x21:
				retVal += "addu ";
				break;
			case 0x24:
				retVal += "and ";
				break;
			case 0x08:
				retVal += "jr " + "$" + Integer.toString(parseRS(currentWord)); //special case for jump reg operations
				return retVal;
			case 0x27:
				retVal += "nor ";
				break;
			case 0x25:
				retVal += "or ";
				break;
			case 0x2A:
				retVal += "slt ";
				break;
			case 0x2B:
				retVal += "sltu ";
				break;
			case 0x00:
				retVal += "sll " + "$" + Integer.toString(parseRD(currentWord)) + ", $" + Integer.toString(parseRT(currentWord)) + ", " + Integer.toString(parseShamt(currentWord)); //special cases for shift operations
				return retVal;
			case 0x02:
				retVal += "srl " + "$" + Integer.toString(parseRD(currentWord)) + ", $" + Integer.toString(parseRT(currentWord)) + ", " + Integer.toString(parseShamt(currentWord));
				return retVal;
			case 0x22:
				retVal += "sub ";
				break;
			case 0x23:
				retVal += "subu ";
				break;
			}
			
			retVal += "$" + Integer.toString(parseRD(currentWord)) + ", $" + Integer.toString(parseRS(currentWord)) + ", $" + Integer.toString(parseRT(currentWord));
			
			return retVal;
		}
		else if(it == InstructionType.I) //immediate type
		{
			switch(operation) //Next fill in the instruction string
			{
			case 0x08:
				retVal += "addi ";
				break;
			case 0x09:
				retVal += "addiu ";
				break;
			case 0x0C:
				retVal += "andi ";
				break;
			case 0x04: //technically by leaving these thge same as the others I am switching RT and RS but it is symmetrical so who cares
				retVal += "beq ";
				break;
			case 0x05:
				retVal += "bne ";
				break;
			case 0x24: //load operations are special, so we'll return from here
				retVal += "lbu $" + Integer.toString(parseRT(currentWord)) + ", " + /*immediate value*/ Integer.toString(0xFFFF & currentWord) + "($" + Integer.toString(parseRS(currentWord)) + ")";
				return retVal;
			case 0x25:
				retVal += "lhu $" + Integer.toString(parseRT(currentWord)) + ", " + /*immediate value*/ Integer.toString(0xFFFF & currentWord) + "($" + Integer.toString(parseRS(currentWord)) + ")";
				return retVal;
			case 0x0F:
				retVal += "lui $" + Integer.toString(parseRT(currentWord)) + ", " + /*immediate value*/ Integer.toString(0xFFFF & currentWord);
				return retVal;
			case 0x23:
				retVal += "lw $" + Integer.toString(parseRT(currentWord)) + ", $" + /*immediate value*/ Integer.toString(0xFFFF & currentWord) + "($" + Integer.toString(parseRS(currentWord)) + ")";
				return retVal;
			case 0x0D:
				retVal += "ori ";
				break;
			case 0x0A:
				retVal += "slti ";
				break;
			case 0x0B:
				retVal += "sltiu ";
				break;
			case 0x28:
				retVal += "sb $" + Integer.toString(parseRT(currentWord)) + ", $" + /*immediate value*/ Integer.toString(0xFFFF & currentWord) + "($" + Integer.toString(parseRS(currentWord)) + ")";
				return retVal;
			case 0x29:
				retVal += "sh $" + Integer.toString(parseRT(currentWord)) + ", $" + /*immediate value*/ Integer.toString(0xFFFF & currentWord) + "($" + Integer.toString(parseRS(currentWord)) + ")";
				return retVal;
			case 0x2B:
				retVal += "sw $" + Integer.toString(parseRT(currentWord)) + ", $" + /*immediate value*/ Integer.toString(0xFFFF & currentWord) + "($" + Integer.toString(parseRS(currentWord)) + ")";
				return retVal;
			}
			
			retVal += "$" + Integer.toString(parseRT(currentWord)) + ", $" + Integer.toString(parseRS(currentWord)) + ", " + /*immediate value*/ Integer.toString(0xFFFF & currentWord);
			
			return retVal;
		}
		else //jump type
		{
			switch(operation)
			{
			case 0x02:
				retVal += "j $";
				break;
			case 0x03:
				retVal += "jal $";
				break;
			}
			
			retVal += Integer.toString(0xFFFF & currentWord);
			
			return retVal;
		}
	}

	private int parseRD(int currentWord)
	{
		return ((0xF800 & currentWord) >>> 11);
	}

	private int parseRT(int currentWord)
	{
		return ((0x1F0000 & currentWord) >>> 16);
	}

	private int parseShamt(int currentWord)
	{
		return ((0x7C0 & currentWord) >>> 6);
	}

	private int parseRS(int currentWord)
	{
		return ((0x3E00000 & currentWord) >>> 21);
	}

	private int parseFunction(int currentWord)
	{
		return (0x1F & currentWord); //least sig 5 bits are the function
	}

	private int parseOperation(int currentWord)
	{
		return ((currentWord & 0xFC000000) >>> 26); //get operation and mask it
	}
}
