package naming;

import storage.Command;
import storage.Storage;

import java.util.ArrayList;
import java.util.List;

/*
    Node class to be used for DFS tree implementation
 */
public class Node {
    String name = null;
    Lock lock;
    boolean isDirectory;
    List<Node> children;
    List<Command> commandStubs = new ArrayList<Command>();
    Storage storageStub;

    Node(String name, boolean isDirectory, Command commandStub, Storage storageStub) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.lock = new Lock();
        if(commandStub != null) {
        	this.commandStubs.add(commandStub);
        }
        if(storageStub != null) {
        	this.storageStub = storageStub;
        }
        children = new ArrayList<Node>();
    }
}
