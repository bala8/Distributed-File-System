package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 
 * @author nikhil;
 * @created 02/03
 */
public class TCPServerWorker<T> implements Runnable{

	protected Socket clientSocket = null;
	protected Class<T> c;
	protected T server;
	protected InetSocketAddress address;
	Skeleton<T> skeleton = null;

    public TCPServerWorker(Socket clientSocket, Class<T> c, T server, InetSocketAddress address, Skeleton<T> skeleton) {
        this.clientSocket = clientSocket;
        this.c = c;
        this.server = server;
        this.address = address;
        this.skeleton = skeleton;
    }

    public void run() {
    	ObjectOutputStream output = null;
    	ObjectInputStream input = null;
        try {
        	if(clientSocket != null) {
        		output = new ObjectOutputStream(clientSocket.getOutputStream());
                output.flush();
                input  = new ObjectInputStream(clientSocket.getInputStream());

                MarshalledProxy mProxy = (MarshalledProxy)input.readObject();
                String methodName = mProxy.methodName;
                Object[] args = mProxy.args;
                Class[] parameterTypes = mProxy.parameterTypes;
                Object returnValue = null;
                Class[] interfaces = c.getInterfaces();
                Method m = null;
                for(Class interfaceClass : interfaces) {
                	m = interfaceClass.getMethod(methodName, parameterTypes);
                	if(m != null) {
                		break;
                	}
                }
                
                if(m == null) {
                	m = c.getMethod(methodName, parameterTypes);
                }
                returnValue = m.invoke(server, args);
                output.writeBoolean(false);
                output.writeObject(returnValue);
                output.flush();
        	}
            
        } catch (InvocationTargetException e) {
        	try {        		
        		output.writeBoolean(true);
				output.writeObject(e.getCause());
			} catch (IOException e1) {
//				e1.printStackTrace();
			}
        	skeleton.stopped(e);
		} catch (Exception e) {
			try {
				output.writeBoolean(true);
	        	output.writeObject(e.getCause());
			} catch (IOException e1) {
//				e1.printStackTrace();
			}
			skeleton.service_error(new RMIException(e));
		} finally {
			if(output != null) {
				try {
					output.close();
				} catch (IOException e) {
//					e.printStackTrace();
				}
			}
			if(input != null) {
				try {
					input.close();
				} catch (IOException e) {
//					e.printStackTrace();
				}
			}
			if(clientSocket != null) {
				if(!clientSocket.isClosed()) {
					try {
						clientSocket.close();
					} catch (IOException e) {
//						e.printStackTrace();
					}
				}
			}
		}
    }

}
