/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntaticAnalyzer.SourcePosition;

public class BaseType extends TypeDenoter
{
    public BaseType(TypeKind t, SourcePosition posn){
        super(t, posn);
    }
    
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitBaseType(this, o);
    }
    
    @Override
    public boolean equals(Object obj) {
    	
    	if (this.typeKind == TypeKind.UNSUPPORTED || this.typeKind == TypeKind.ERROR) {
    		return false;
    	}
    	if (this.typeKind == TypeKind.NULL && obj instanceof ClassType) {
    		return true;
    	}
    	
    	if (obj == null) {
    		return false;
    	} else if (obj instanceof BaseType) {
    		if (((BaseType) obj).typeKind == this.typeKind) {
    			return true;
    		}
    	}
    	
    	return false;
    }
}
