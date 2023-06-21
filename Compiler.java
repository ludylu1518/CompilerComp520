package miniJava;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.CodeGenerator.CodeGenerator;
import miniJava.ContextualAnalysis.Identification;
import miniJava.ContextualAnalysis.TypeChecking;
import miniJava.SyntaticAnalyzer.Parser;
import miniJava.SyntaticAnalyzer.Scanner;
import miniJava.SyntaticAnalyzer.SourceFile;
import miniJava.mJAM.Disassembler;
import miniJava.mJAM.Interpreter;
import miniJava.mJAM.ObjectFile;

public class Compiler {
	
	public static void main(String[] args) {
		
		boolean check;		
	
		ErrorReporter reporter = new ErrorReporter();
		SourceFile file = new SourceFile("test.java", reporter);
		Scanner scanner = new Scanner(file, reporter);
	    Parser parser = new Parser(scanner, reporter);
	    
	    
	    AST ast = parser.parse();
		
		check = !reporter.hasErrors();
		
		if (!check) {
			
			System.out.println("FAILED SyntaticAnalysis");
			System.exit(4);
		}
		
		
		Identification id = new Identification(ast, reporter);
		id.startIdentification();
		
		check = !reporter.hasErrors();
		
		if (!check) {
			
			System.out.println("*** FAILED contextual analysis: identification");
			System.exit(4);
		}
	
		TypeChecking type = new TypeChecking(ast, reporter);
		type.startTypeChecking();
		
		check = !reporter.hasErrors();
		
		if (!check) {
			
			System.out.println("*** FAILED contextual analysis: typeChecking");
			System.exit(4);
		}
		
		CodeGenerator code = new CodeGenerator(ast, reporter);
		code.Start();
		
		if (reporter.hasErrors()) {
			System.out.println("*** FAILED codeGeneration");
			System.exit(4);
		}
		
		
		
		String s = args[0].substring(0, args[0].indexOf('.')) + ".mJAM";
		ObjectFile obj = new ObjectFile(s);
			
		
		if (obj.write()) {
             System.out.println("*** Failed writing code");
             System.exit(4);
         }
				        
		 System.out.println("Successfully written code");
		
		String asmCodeFileName = s.replace(".mJAM",".asm");
        System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
        Disassembler d = new Disassembler(s);
        if (d.disassemble()) {
                System.out.println("FAILED!");
                return;
        }
        else
                System.out.println("SUCCEEDED");
		
        //Interpreter.debug(s, asmCodeFileName);
        
        Interpreter.interpret(s);
        
			     
		 System.out.println("Valid miniJava Program");
         System.exit(0);
		
		
	}
	
}
