/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntaticAnalyzer.SourcePosition;
import miniJava.SyntaticAnalyzer.Token;

public class ClassDecl extends Declaration {

  public ClassDecl(String cn, FieldDeclList fdl, MethodDeclList mdl, SourcePosition posn) {
	  super(cn, null, posn);
	  fieldDeclList = fdl;
	  methodDeclList = mdl;
	  
	  Token t = new Token(4, cn, posn);
	  Identifier id = new Identifier(t);
	  type = new ClassType(id, posn);
  }
  
  public <A,R> R visit(Visitor<A, R> v, A o) {
      return v.visitClassDecl(this, o);
  }
      
  public FieldDeclList fieldDeclList;
  public MethodDeclList methodDeclList;
  public TypeDenoter type;
}
