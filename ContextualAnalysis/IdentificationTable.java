package miniJava.ContextualAnalysis;

import java.util.ArrayList;
import java.util.HashMap;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;

public class IdentificationTable {

	public ArrayList<HashMap<String, Declaration>> allScope;
	private ErrorReporter reporter;

	public IdentificationTable(ErrorReporter reporter) {
		allScope = new ArrayList<HashMap<String, Declaration>>();
		this.reporter = reporter;
	}
	
	
	public int getLength() {
		return allScope.size() - 1;
	}
	
	
	public Declaration findClass(String id) {
		
		if (allScope.get(0).get(id) != null) {
			return allScope.get(0).get(id);
		}
		
		return null;
	}
	
	
	
	public Declaration retrieve(String id) {
		int length = getLength();
		
		for (int i = length; i >= 0; i--) {
			HashMap<String, Declaration> idTable = allScope.get(i);
			
			if (idTable.get(id) != null) {
				return idTable.get(id);
			}
		}
		return null; 
	}
	
	
	public Declaration retrieve(Identifier id, MethodDecl md) {
		Declaration d = null;
		
		for (int i = getLength(); i >= 0; i--) {
			Declaration decl = allScope.get(i).get(id.spelling);
		
			if (decl != null) {
				d = decl;
				break;
			}
		}
		
		if (d == null) {
		
			reporter.reportError("*** line " + id.posn + ": error, id not declared");
		}
		if (d instanceof MemberDecl && md != null && md.isStatic && !((MemberDecl)d).isStatic) {
			reporter.reportError("*** line " + id.posn + ": error, cannot reference non-static member in static method");
		}
		
		return d;
	}
	
	
	public void openScope() {
		allScope.add(new HashMap<String, Declaration>());
	}
	
	
	public void closeScope() {
		allScope.remove(getLength());
	}
	
	public void enter(Declaration cd) {
		if (allScope.get(getLength()).containsKey(cd.name)) {
			reporter.reportError("*** line " + cd.posn.start + ": invalid multiple declaration on same id in same scope");
		
		} else if (getLength() > 2){		
			for (int i = 2; i < getLength(); i++) {
				if (allScope.get(i).containsKey(cd.name)) {
					reporter.reportError("*** line " + cd.posn + ": invalid multiple declaration on same id in scope 4+");
				}
			}
			
		} 	
		allScope.get(getLength()).put(cd.name, cd);
	
	}
	
	public void toPrint() {
		for (HashMap<String, Declaration> scope : allScope) {
			System.out.println(scope.values());
		}
	}


}


