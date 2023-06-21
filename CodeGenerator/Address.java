package miniJava.CodeGenerator;

public class Address extends RuntimeEntity{
	
	public int offset;
	
	public Address(int size, int offset) {
		super(size);
		this.offset = offset;
	}
}
