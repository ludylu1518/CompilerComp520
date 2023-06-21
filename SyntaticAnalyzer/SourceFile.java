package miniJava.SyntaticAnalyzer;

import java.io.*;

import miniJava.ErrorReporter;

public class SourceFile {
	
	  static boolean EOT = false;        // end of file
	  static int currentLine;
	  
	  
	  FileInputStream reader;
	  ErrorReporter reporter;
	  
	  public SourceFile(String filename, ErrorReporter reporter) {
		  this.reporter = reporter;
		  currentLine = 1;
		  
		  try {
		 
		     reader = new FileInputStream(filename);
		  
		  } catch (FileNotFoundException e) {
		    System.out.println("input file not found.");
		    System.exit(3);
		    }
		  }
	

	  char getChar() {
	    try {
	    	
	      int c = reader.read();

	      if (c == -1) {
	        EOT = true;	        
	      } else if (c == '\n') {
	    	  currentLine++;
	      }
	      
	      return (char) c;
	      
	    } catch (IOException e) {	
	      reporter.reportError("Scan error, IOexception");
	      EOT = true;
	      return (char) -1;
	    }
	  }
	  
	  static int getCurrentLine() {
		  return currentLine;
	  }
}

