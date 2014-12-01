package disassembler;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import common.InstructionType;


public class Mips_Disassembler //converts from a binary to an ASM
{
	FileInputStream bin;
	BufferedWriter asm;
	String inFile;
	Map<Integer,String> addressMap;
	
	public Mips_Disassembler() throws IOException
	{
		this("output.asm", "out.bin");
	}
	
	public Mips_Disassembler(String out, String in) throws IOException
	{
		inFile = in;
		bin = new FileInputStream(inFile);
		asm = new BufferedWriter(new FileWriter(out,false));
		this.addressMap = new HashMap<Integer,String>();
	}
	
	public void disassembleAndClose() throws Exception
	{
		byte[] b = new byte[4];
		
		int bytesRead = 0, currentWord = 0;
		
		int address = 0;
		
		int labelNum = 0;
		
		//Map<Integer,String> addressMap = new HashMap<Integer,String>();
		
		//first create labels
		for(bytesRead = bin.read(b); bytesRead > 0; bytesRead = bin.read(b))
		{
			int key = 0;
			currentWord = ByteBuffer.wrap(b).getInt();
			if(isBranch(currentWord))
			{
				key = currentWord & 0xFFFF;
				if(!addressMap.containsKey(address + (short)key)) //don't place a key already there
				{
					addressMap.put((address + 1 + (short)key), "label" + Integer.toString(labelNum++) + ":");
				}
			}
			else if(isJump(currentWord))
			{
				key = (currentWord & ((0b11 << 22) | 0xFFFFF) | ((address+1) & 0xFF000000)); //jump includes what would be the next upper 8 bits and the 24 bit immediate value, who's lsb 2 bits are ignored
				if(!addressMap.containsKey(key))
				{
					addressMap.put(key, "label" + Integer.toString(labelNum++) + ":");
				}
			}
			address++;
		}

		bin = new FileInputStream(inFile); //reset stream
		
		address = 0;
		
		do		
		{			
			if(addressMap.containsKey(address)) //check if contains label
			{
				asm.write(addressMap.get(address) + "-- 0x" + Integer.toString(address,16) + "\n"); //print label and next line
			}	
			//now go ahead and print the instruction
			bytesRead = bin.read(b);
			
			currentWord = ByteBuffer.wrap(b).getInt();
			
			if(bytesRead > 0)
			{
				asm.write(disassembleInstruction(currentWord, address) + "\n");
				address++;
			}
		}while(bytesRead >= 0);
		
		asm.close();
		bin.close();
	}
	
	private boolean isBranch(int currentWord)
	{
		int operation = parseOperation(currentWord);
		if(operation == 0x4 || operation == 0x5) //j and jal
		{
			return true;
		}
		return false;
	}

	private boolean isJump(int currentWord)
	{
		int operation = parseOperation(currentWord);
		switch(operation)
		{
		case 0x2:
		case 0x3:
			return true;
		default:
			return false;
		}
	}

	private String disassembleInstruction(int currentWord, int address) throws Exception
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
				retVal += "lbu $" + Integer.toString(parseRT(currentWord)) + ", 0x" + /*immediate value*/ Integer.toString(0xFFFF & currentWord,16) + "($" + Integer.toString(parseRS(currentWord)) + ")";
				return retVal;
			case 0x25:
				retVal += "lhu $" + Integer.toString(parseRT(currentWord)) + ", 0x" + /*immediate value*/ Integer.toString(0xFFFF & currentWord,16) + "($" + Integer.toString(parseRS(currentWord)) + ")";
				return retVal;
			case 0x0F:
				retVal += "lui $" + Integer.toString(parseRT(currentWord)) + ", 0x" + /*immediate value*/ Integer.toString(0xFFFF & currentWord,16);
				return retVal;
			case 0x23:
				retVal += "lw $" + Integer.toString(parseRT(currentWord)) + ", 0x" + /*immediate value*/ Integer.toString(0xFFFF & currentWord,16) + "($" + Integer.toString(parseRS(currentWord)) + ")";
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
				retVal += "sb $" + Integer.toString(parseRT(currentWord)) + ", 0x" + /*immediate value*/ Integer.toString(0xFFFF & currentWord,16) + "($" + Integer.toString(parseRS(currentWord)) + ")";
				return retVal;
			case 0x29:
				retVal += "sh $" + Integer.toString(parseRT(currentWord)) + ", 0x" + /*immediate value*/ Integer.toString(0xFFFF & currentWord,16) + "($" + Integer.toString(parseRS(currentWord)) + ")";
				return retVal;
			case 0x2B:
				retVal += "sw $" + Integer.toString(parseRT(currentWord)) + ", 0x" + /*immediate value*/ Integer.toString(0xFFFF & currentWord,16) + "($" + Integer.toString(parseRS(currentWord)) + ")";
				return retVal;
			}
			
			String branchAddress_str;
			int branchAddress = (short) (0xFFFF & currentWord) + address + 1;
			
			if(addressMap.containsKey(branchAddress) && (operation == 0x04 || operation == 0x05))
			{
				branchAddress_str = addressMap.get(branchAddress) + "-- 0x" + Integer.toString(address,16);
				branchAddress_str = branchAddress_str.replace(':', ' ');
			}
			else
			{
				branchAddress_str = "0x" + Integer.toString(0xFFFF & currentWord,16);
			}
			
			retVal += "$" + Integer.toString(parseRT(currentWord)) + ", $" + Integer.toString(parseRS(currentWord)) + ", " + /*immediate value*/ branchAddress_str;
			
			return retVal;
		}
		else //jump type
		{
			switch(operation)
			{
			case 0x02:
				retVal += "j ";
				break;
			case 0x03:
				retVal += "jal ";
				break;
			}
			
			int jumpAddress = (currentWord & ((0b11 << 22) | 0xFFFFF) | ((address+1) & 0xFF000000));
			
			if(addressMap.containsKey(jumpAddress))
			{
				retVal += addressMap.get(jumpAddress).replace(':', ' ') + "-- 0x" + Integer.toString(address,16);
			}
			else
			{
				retVal += "0x" + Integer.toHexString((0xFFFFFF & currentWord) << 2);
			}
			
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
		return (0x3F & currentWord); //least sig 5 bits are the function
	}

	private int parseOperation(int currentWord)
	{
		return ((currentWord & 0xFC000000) >>> 26); //get operation and mask it
	}
}
