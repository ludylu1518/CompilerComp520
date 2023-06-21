/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntaticAnalyzer.SourcePosition;

public class ClassType extends TypeDenoter
{
    public ClassType(Identifier cn, SourcePosition posn){
        super(TypeKind.CLASS, posn);
        className = cn;
    }
    
            
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitClassType(this, o);
    }

    public Identifier className;


	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		if (obj == null) {
			return false;
			
		} else if (((TypeDenoter)obj).typeKind == TypeKind.NULL) {
			return true;
			
		} else if (obj instanceof ClassType) {
			return this.className.spelling.equals(((ClassType) obj).className.spelling);
		}
		
		
		return false;
	}
}
