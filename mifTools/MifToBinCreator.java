package mifTools;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class MifToBinCreator
{
	BufferedReader mif;
	FileOutputStream bin;
	
	String filePath;
	
	Radix radix;
	
	public MifToBinCreator(String filePath) throws FileNotFoundException
	{
		this.filePath = filePath;
		mif = new BufferedReader(new FileReader(filePath));
		bin = new FileOutputStream(outName());
	}
	
	public void convertAndClose() throws Exception
	{
		String line = null;
		do
		{
			line = mif.readLine();
			
			if(line != null && line.toLowerCase().contains("data_radix")) //set the radix for conversion later
			{
				String[] temp = line.toLowerCase().split("=");
				line = temp[temp.length - 1]; //right after the only equals in this line
				line = line.trim();
				line = line.replace(";", ""); //remove semicolon
				line = line.toLowerCase();//not case sensitive
				
				switch(line)
				{
				case "hex":
					radix = Radix.hex;
					break;
				case "oct":
					radix = Radix.octal;
					break;
				case "bin":
					radix = Radix.binary;
					break;
				case "dec":
					radix = Radix.decimal;
					break;
				default:
					System.out.println("Not a supported radix, exiting bin creation...");
					throw new Exception("radix error");
				}
			}	
			else if(line == null)
			{
				System.out.println("Incorrectly formatted MIF file, exiting binary creation...");
				throw new IOException("bad mif");
			}
		}while(!line.contains(":"));
		
		//now we are on a line that has the first instruction (every line with a : has one)
		int currentWord;
		
		do
		{
			line = line.split("\\s+")[3]; //the hex value is always after the second tab/space
			
			line = line.replace(";",""); //remove the semicolon
			
			currentWord = parseWord(line);
			
			bin.write(intToBytes(currentWord));
			
			line = mif.readLine();
					
		}while(!line.toLowerCase().contains("end")); //do this until the end
		
		mif.close();
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
	private int parseWord(String line)
	{
		switch(radix)
		{
		case hex:
			return (int) Long.parseLong(line, 16);
		case octal:
			return (int) Long.parseLong(line, 8);
		case binary:
			return (int) Long.parseLong(line, 2);
		case decimal:
			return (int) Long.parseLong(line, 10);
		default:
			return -1;
		}
	}

	private String outName()
	{
		String[] temp = filePath.split("[.]"); //needed the square brackets to deal with the period being a reserved regex expression
		return temp[0] + ".bin";
	}
}

enum Radix
{
	hex,octal,binary,decimal;
}
