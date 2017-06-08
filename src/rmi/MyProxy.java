package rmi;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 
 * @author nikhil;
 * @created 02/03
 */
public class MyProxy<T> implements InvocationHandler, Serializable{

	private static final long serialVersionUID = 1L;
	private InetSocketAddress address = null;
	private Class<T> c = null;
	
	public MyProxy(InetSocketAddress address) {
		this.setAddress(address);
	}
	
	public MyProxy(InetSocketAddress address, Class<T> c) {
		this.setAddress(address);
		this.c = c;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object returnValue = null;
		if(methodContainsRMIException(method)) {
			returnValue = invokeRemoteMethod(proxy, method, args);
		} else {
			returnValue = invokeLocalMethod(proxy, method, args);
		}
		return returnValue;
	}
	
	public Object invokeRemoteMethod(Object proxy, Method method, Object[] args) throws Throwable {
		Socket s = null;
		Object returnValue = null;
		boolean containsException = false;
		try {
			s = new Socket();
			s.connect(address);			
			Class[] parameterTypes = method.getParameterTypes();
			MarshalledProxy mProxy = new MarshalledProxy(method.getName(), args, parameterTypes); 
			ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
			oos.flush();
			ObjectInputStream ois = new ObjectInputStream(s.getInputStream());

			oos.writeObject(mProxy);
			oos.flush();

			containsException = ois.readBoolean();
			returnValue = ois.readObject();
		} catch(Exception e) {
			throw new RMIException(e);
		} finally {
			if(s != null) {
				s.close();
			}
		}
		if(containsException) {
			if(returnValue != null) {
				throw (Throwable) returnValue;
			} else {
				throw new RMIException("Null exception returned");
			}
		} 
		return returnValue;
	}
	
	public Object invokeLocalMethod(Object proxy, Method method, Object[] args) {
		Object returnValue = null;
		switch(method.getName()) {
			case "equals" : {
				returnValue = equalsCheck(proxy, method, args);
				break;
			}
			case "toString" : {
				returnValue = toString(proxy, method, args);
				break;
			}
			case "hashCode" : {
				returnValue = hashcode(proxy, method, args);
				break;
			}
		}
		return returnValue;
	}
		
	public Object equalsCheck(Object proxy, Method method, Object[] args) {
		boolean isEquals = false;
		if(args == null || args.length == 0) {
			isEquals = false;
		} else {
			try { 
				Object toCheck = args[0];
				if(toCheck == null) {
					isEquals = false;
				} else {
					if(proxy.getClass().equals(toCheck.getClass())) {
						MyProxy<T> proxyToCheck = (MyProxy<T>)Proxy.getInvocationHandler(toCheck);
						if(this.getAddress().toString().equals(proxyToCheck.getAddress().toString())) {
							isEquals = true;
						} else {
							isEquals = false;
						}
					} else {
						isEquals = false;
					}
				}
			} catch(Exception e) {
				isEquals = false;
			}
		}
		return isEquals;
	}
	
	public Object hashcode(Object proxy, Method method, Object[] args){
		int hashValue = 0;
		try { 
			hashValue = proxy.getClass().hashCode() + this.getAddress().hashCode();
		} catch(Exception e) {
			
		}
		return hashValue;
	}
	
	public Object toString(Object proxy, Method method, Object[] args){
		StringBuilder stringConvert = new StringBuilder();
		try { 
			stringConvert.append(c.getSimpleName());
			stringConvert.append(" - ");
			stringConvert.append(this.address.getHostName());
			stringConvert.append(" - ");
			stringConvert.append(this.address.getPort());
		} catch(Exception e) {
			
		}
		return stringConvert.toString();
	}

	public InetSocketAddress getAddress() {
		return address;
	}

	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}

	private static <T> boolean methodContainsRMIException(Method method) {
    	boolean containsRMIException = true;
    	Class[] exceptions =  method.getExceptionTypes();
		if(exceptions.length > 0) {
			for(Class exception : exceptions) {
				if(exception.getName().equals(RMIException.class.getName())) {
					containsRMIException = true;
				}
			}
		} else {
			containsRMIException = false;
		}
		return containsRMIException;
    }
}
