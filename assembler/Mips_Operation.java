package assembler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import common.InstructionType;

public class Mips_Operation
{
	private String line;
	private InstructionType type;
	private int operation;
	private int rs, rt, rd, shamt = 0, funct; // R and I type fields
	private int immediate; // needed for I type fields only
	private int address; // used for J type instructions
	private boolean decoded = false;

	private static final HashMap<String, Integer> registers = new HashMap<String, Integer>();
	private static final int MAX_IMMEDIATE = 32767;
	private static final int MIN_IMMEDIATE = -32768;
	private static final int MAX_JUMP = 33554431;
	private static final int MIN_JUMP = -33554432;
	
	static
	{
		registers.put("$zero", 0);
		registers.put("$at", 1);

		registers.put("$v0", 2);
		registers.put("$v1", 3);

		registers.put("$a0", 4);
		registers.put("$a1", 5);
		registers.put("$a2", 6);
		registers.put("$a3", 7);

		registers.put("$t0", 8);
		registers.put("$t1", 9);
		registers.put("$t2", 10);
		registers.put("$t3", 11);
		registers.put("$t4", 12);
		registers.put("$t5", 13);
		registers.put("$t6", 14);
		registers.put("$t7", 15);
		registers.put("$t8", 24);
		registers.put("$t9", 25);

		registers.put("$s0", 16);
		registers.put("$s1", 17);
		registers.put("$s2", 18);
		registers.put("$s3", 19);
		registers.put("$s4", 20);
		registers.put("$s5", 21);
		registers.put("$s6", 22);
		registers.put("$s7", 23);

		registers.put("$k0", 26);
		registers.put("$k1", 27);

		registers.put("$gp", 28);
		registers.put("$sp", 29);
		registers.put("$fp", 30);
		registers.put("$ra", 31);
		
		registers.put("$0", 0);
		registers.put("$1", 1);

		registers.put("$2", 2);
		registers.put("$3", 3);

		registers.put("$4", 4);
		registers.put("$5", 5);
		registers.put("$6", 6);
		registers.put("$7", 7);

		registers.put("$8", 8);
		registers.put("$9", 9);
		registers.put("$10", 10);
		registers.put("$11", 11);
		registers.put("$12", 12);
		registers.put("$13", 13);
		registers.put("$14", 14);
		registers.put("$15", 15);
		registers.put("$16", 16);
		registers.put("$17", 17);

		registers.put("$18", 18);
		registers.put("$19", 19);
		registers.put("$20", 20);
		registers.put("$21", 21);
		registers.put("$22", 22);
		registers.put("$23", 23);
		registers.put("$24", 24);
		registers.put("$25", 25);
		
		registers.put("$26", 26);
		registers.put("$27", 27);

		registers.put("$28", 28);
		registers.put("$29", 29);
		registers.put("$30", 30);
		registers.put("$31", 31);
	}

	public Mips_Operation(String line)
	{
		this.line = line;
	}

	public boolean decode(Map<String, Integer> labels, int currentAddress, int BASE_ADDRESS)
	{
		ArrayList<String> parameters = new ArrayList<String>(Arrays.asList(line.split("[ (]"))); // split along spaces
		parameters.remove(""); // removes all empty strings from this list, just in case

		// next iterate through list and remove commas (purify the strings)
		ListIterator<String> it = parameters.listIterator();
		while (it.hasNext())
		{
			String temp = it.next();
			temp = temp.replace(",", "");
			temp = temp.replace(")", ""); //remove the trailing commas and close parenthases
			it.set(temp);
		}

		String operation_string = parameters.get(0).toLowerCase(); // ignore
																	// case and
																	// store
																	// operation
																	// for
																	// switching

		boolean is_R = true;
		boolean isShift = false;
		switch (operation_string)
		{
		// R type instructions
		case "add": // add
			this.funct = 0x20;
			break;
		case "addu": // add unsigned
			this.funct = 0x21;
			break;
		case "and": // bitwise and between two registers
			this.funct = 0x24;
			break;
		case "jr": // jump register, very special case, just set rs and return
			this.funct = 0x08;
			this.rs = registers.get(parameters.get(1).toLowerCase());
			decoded = true;
			return true;
		case "nor": // bitwise nor between two registers
			this.funct = 0x27;
			break;
		case "or": // bitwise or between two registers
			this.funct = 0x25;
			break;
		case "slt": // set less than
			this.funct = 0x2A;
			break;
		case "sltu": // set less than unsigned
			this.funct = 0x2B;
			break;
		case "sll": // shift left logical
			this.funct = 0x00;
			isShift = true;
			break;
		case "srl": // shift right logical
			this.funct = 0x02;
			isShift = true;
			break;
		case "sub": // subtract
			this.funct = 0x22;
			break;
		case "subu": // subtract unsigned
			this.funct = 0x23;
			break;
		default:
			is_R = false; // none of these, so not an R operation
		}

		if (is_R) // if it was an R operation, fill in all of the other information, including that it was
		{
			this.operation = 0;
			
			this.rd = registers.get(parameters.get(1).toLowerCase()); // first arg is rd
			if(!isShift)
			{
				this.rs = registers.get(parameters.get(2).toLowerCase()); // second arg is rs
				this.rt = registers.get(parameters.get(3).toLowerCase()); // last is rt
			}
			else
			{
				this.rt = registers.get(parameters.get(2).toLowerCase());
				this.shamt = Integer.parseInt(parameters.get(3));
			}
			
			this.type = InstructionType.R;

			decoded = true;

			return true;
		}

		// was not that type, so either I or J, let's continue for I
		int immediateValueIndex = 3; //usually 3 for arithmetic ops and branches, 2 for load and store ops
		int rsValueIndex = 2; //usually 2 for arithmetic ops and branches, 3 for load and store
		
		boolean is_I = true;
		switch (operation_string)
		{
		case "addi": // add immediate
			this.operation = 0x08;
			break;
		case "addiu": // add immediate unsigned
			this.operation = 0x09;;
		case "andi": // and immediate
			this.operation = 0x0C;
			break;
		case "beq": // branch if equal to zero
			this.operation = 0x04;
			break;
		case "bne": // branch if not equal to zero
			this.operation = 0x05;
			break;
		case "lbu": // load byte unsigned
			this.operation = 0x24;
			immediateValueIndex = 2;
			rsValueIndex = 3;
			break;
		case "lhu": // load halfword (two bytes) unsigned
			this.operation = 0x25;
			immediateValueIndex = 2;
			rsValueIndex = 3;
			break;
		case "lui": // load upper immediate
			this.operation = 0x0F;
			immediateValueIndex = 2;
			rsValueIndex = 3;
			break;
		case "lw": // load word
			this.operation = 0x23;
			immediateValueIndex = 2;
			rsValueIndex = 3;
			break;
		case "ori": // or immediate
			this.operation = 0x0D;
			break;
		case "slti": // set less than immediate
			this.operation = 0x0A;
			break;
		case "sltiu": // set less than immediate unsigned
			this.operation = 0x0B;
			break;
		case "sb": // store byte
			this.operation = 0x28;
			immediateValueIndex = 2;
			rsValueIndex = 3;
			break;
		case "sh": // store halfword (2 bytes)
			this.operation = 0x29;
			immediateValueIndex = 2;
			rsValueIndex = 3;
			break;
		case "sw": // store word
			this.operation = 0x2B;
			immediateValueIndex = 2;
			rsValueIndex = 3;
			break;
		default:
			is_I = false;
		}

		if (is_I)
		{
			this.rt = registers.get(parameters.get(1).toLowerCase()); // first arg is rt, the destination
			
			this.immediate = this.unLabel(parameters.get(immediateValueIndex).toLowerCase(), currentAddress, BASE_ADDRESS, labels, InstructionType.I);

			if(parameters.size() >= 4) this.rs = registers.get(parameters.get(rsValueIndex).toLowerCase()); //there is only an rs during a load or store exept LUI

			this.type = InstructionType.I;

			decoded = true;

			return true;
		}

		// well it wasn't those, so it must be a jump
		boolean success = true;
		switch (operation_string)
		{
		case "j": // jump
			this.operation = 0x02;
			break;
		case "jal": // jump and link
			this.operation = 0x03;
			break;
		default:
			success = false;
			System.out.println("Error: " + operation_string	+ " is not a recognized instruction");
		}

		if (success)
		{
			this.address = this.unLabel(parameters.get(1).toLowerCase(), currentAddress, BASE_ADDRESS, labels, InstructionType.J);

			this.type = InstructionType.J;

			decoded = true;
		}

		return success;
	}

	private int unLabel(String param, int currentAddress, int BASE_ADDRESS, Map<String,Integer> labels, InstructionType t)
	{
		int ret = 0;
		
		if(labels.containsKey(param))
		{
			ret =  labels.get(param) - (currentAddress - BASE_ADDRESS);
		}
		else if(isHexInteger(param))
		{
			ret = Integer.parseInt(param.substring(2, param.length()), 16);
		}
		else if(isInteger(param))
		{
			ret = Integer.parseInt(param);
		}		
		else
		{
			System.out.println("Error: " + param + " label does not exist");
			System.exit(1);
		}
			//if within allowable range
		if(t == InstructionType.I && (short) ret <= MAX_IMMEDIATE && (short) ret >= MIN_IMMEDIATE) //must cast as short since this is 16 bit
		{
			return ret;
		}
		else if(t == InstructionType.J && ret <= MAX_JUMP && (ret | 0xFF000000) >= MIN_JUMP) //ret is 24 bit, must sign extend to 32 bit
		{
			return ret;
		}
		else
		{
			System.out.println("Value " + ret + " is out of range");
		}
	
		return 0;
	}
	
	private boolean isHexInteger(String param)
	{
		String hex_prefix;
		
		if(param.length() >= 3)
		{
			hex_prefix = param.substring(0, 2);
		}
		else
		{
			return false;
		}
		
		if(hex_prefix.equals("0x") && param.length() <= 6)
		{
			int param_int = Integer.parseInt(param.substring(2, param.length()), 16);
			return isInteger(Integer.toString(param_int));
		}
		return false;
	}

	private boolean isInteger(String s)
	{
		if (s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++)
		{
			if (i == 0 && s.charAt(i) == '-')
			{
				if (s.length() == 1)
					return false;
				else
					continue;
			}
			if (Character.digit(s.charAt(i), 10) < 0)
				return false;
		}
		return true;
	}

	public int produceOpcode()
	{
		if (!decoded)
		{
			System.out
					.println("Must Decode Instruction before attempting to produce opcode! Attempting...");
			System.exit(1);
		}

		int result = 0;

		result |= operation << 26; // most significant 6 bits are always opcode

		if (type == InstructionType.R || type == InstructionType.I)
		{
			result |= rs << 21;
			result |= rt << 16;

			if (type == InstructionType.I)
			{
				result |= (immediate & 0xFFFF); //masked bottom 16 bits
			} 
			else
			{
				result |= rd << 11;
				result |= shamt << 6;
				result |= funct;
			}
		}
		if (type == InstructionType.J)
		{
			result |= (address & 0x00FFFFFF); //bottom 24 bits are masked
		}

		return result;
	}

}