package assembler;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class Mips_Assembler
{
	private static final int ADDRESS_INIT = 0x00400000;
	private int lineNumber = 0, address = ADDRESS_INIT; //our start address and line (people count from 1)
	
	private boolean labels_made = false;
	
	private String fileString;
	private BufferedReader file;
	
	private FileOutputStream bin;
	
	private Map<String,Integer> labels;
	
	public Mips_Assembler(String filePath) throws IOException
	{
		fileString = filePath;
		file = new BufferedReader(new FileReader(filePath)); //open the file for ASSEMBLINGGGG!!!
	}
	
	public boolean createLabels() throws IOException
	{
		labels = new HashMap<String,Integer>();
		
		if(labels_made)
		{
			System.out.println("Labels already made");
			return false;
		}
		
		for(String line = file.readLine(); line != null; line = file.readLine()) //iterate through every line of the file
		{
			++lineNumber; //there is at least 1 line!
			
			if(line.charAt(line.length() - 1) == ':') //if the last character is a colon
			{
				//this is a label!!!!
				line = line.substring(0, line.length() - 1); //get rid of the colon
				
				labels.put(line, address - ADDRESS_INIT); //offset from base, when implementing remember to subtract from current address
			}
			else
			{
				++address; //only increment if this wasn't a label
			}
		}
		
		file.close();
		
		if(lineNumber >= 1)
		{
			labels_made = true;
			System.out.println("There are " + lineNumber + " lines in this file and the output will contain " + (address - ADDRESS_INIT) + " words");
			return true;
		}
		else
		{
			System.out.println("Label making error, 0 lines parsed in file");
			return false;
		}
	}
	
	public void assemble() throws IOException
	{
		if(!labels_made)
		{
			System.out.println("Must first create labels!");
			System.exit(1);
		}
		
		int opcode;
		
		file = new BufferedReader(new FileReader(fileString)); //reset to the top!
		
		Mips_Operation op;
		
		bin = new FileOutputStream(this.outName(),false);
		
		address = ADDRESS_INIT;
		
		for(String line = file.readLine(); line != null; line = file.readLine())
		{
			if(line.contains(":")) //was a label
			{
				continue; //skip
			}
			
			op = new Mips_Operation(line);
			
			op.decode(labels,address,ADDRESS_INIT);
			
			opcode = op.produceOpcode();
			
			bin.write(intToBytes(opcode));
			
			++address;
		}
		
		bin.close();
	}

	private byte[] intToBytes(int value)
	{
		byte[] ret = new byte[] {
	            (byte)(value >>> 24),
	            (byte)(value >>> 16),
	            (byte)(value >>> 8),
	            (byte)value};
		return ret;
	}

	private String outName()
	{
		String[] temp = fileString.split("[.]"); //needed the square brackets to deal with the period being a reserved regex expression
		return temp[0] + ".bin";
	}
	
}
