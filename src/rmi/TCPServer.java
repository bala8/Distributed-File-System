package rmi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 
 * @author nikhil;
 * @created 02/03
 */
public class TCPServer<T> implements Runnable{
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    protected InetSocketAddress address = null;
    protected Class<T> c;
	protected T server;
	Thread workerThread = null;
	Skeleton<T> skeleton = null;

    public TCPServer(InetSocketAddress address){
        this.address = address;
    }
    
    public TCPServer(Class<T> c, T server, InetSocketAddress address, ServerSocket serverSocket, Skeleton<T> skeleton){
    	this.c = c;
    	this.server = server;
        this.address = address;
        this.serverSocket = serverSocket;
        this.skeleton = skeleton;
    }

    public void run(){
        while(!isStopped){
            Socket clientSocket = null;
            try {
               clientSocket = serverSocket.accept();
            } catch (IOException e) {
            	 skeleton.stopped(e);
                 e.printStackTrace();
            }
            workerThread = new Thread(new TCPServerWorker<T>(clientSocket, c, server, address, skeleton));
            workerThread.start();
        }
    }


    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
			this.serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

//    private void openServerSocket() {
//
//        System.out.println("Inside TCP Server Open Socket method : "+this.address);
//        try {
//            this.serverSocket.bind(this.address);
//            System.out.println("Inside TCP Server Open Socket method : "+this.address);
//        } catch (IOException e) {
//            throw new RuntimeException("Cannot open port 8080", e);
//        }
//    }
}
