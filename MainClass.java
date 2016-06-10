package automata.nsa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;


/*
 * NSA to DNA converter.
 * Written by Niv Hoffman and Maor Prital
 */

public class MainClass {
			
	public static void main(String[] args) throws NumberFormatException, IOException {
		
		boolean printToFile = true;
		
		String inputPath, outputPath, outputDNA;	
		inputPath = args[0];
		outputPath = args[1];		
		
		NSA b = new NSA(inputPath);	
		
		outputDNA = b.convertToDNA();
		
		System.out.println(outputDNA);
				
	    if(printToFile)
	    {
	    	try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputPath))))
	    	{
	    		bw.write(outputDNA);
	    		bw.close();
	    	}
	    	
	    	catch (FileNotFoundException ex)
	    	{
	    		System.out.println(ex.toString());
	    	}
		}
	    
	    
	}	
	
}
