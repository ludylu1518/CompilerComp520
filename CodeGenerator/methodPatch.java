package miniJava.CodeGenerator;

import miniJava.AbstractSyntaxTrees.MethodDecl;

public class methodPatch {

	int line;
	MethodDecl md;
	
	public methodPatch(int line, MethodDecl md) {
		this.line = line;
		this.md = md;
	}
	
}
