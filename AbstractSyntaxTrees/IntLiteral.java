/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntaticAnalyzer.Token;

public class IntLiteral extends Terminal {

  public IntLiteral(Token t) {
    super(t);
  }
 
  public <A,R> R visit(Visitor<A,R> v, A o) {
      return v.visitIntLiteral(this, o);
  }
}