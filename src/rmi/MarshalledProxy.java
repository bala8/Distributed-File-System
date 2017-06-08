package rmi;

import java.io.Serializable;

/**
 * 
 * @author nikhil;
 * @created 02/03
 */
public class MarshalledProxy implements Serializable{
	private static final long serialVersionUID = 1L;
	String methodName = null;
	Object[] args = null;
	Class[] parameterTypes = null;
	
	MarshalledProxy(String methodName, Object[] args, Class[] parameterTypes) {
		this.methodName = methodName;
		this.args = args;
		this.parameterTypes = parameterTypes;
	}
}
