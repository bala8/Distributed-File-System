package naming;

import common.Path;
import storage.Command;
import storage.Storage;
import java.util.ArrayList;

public class Tree {
    private Node root;

    public Tree() {
        // initialization of tree with root node
        this.root = new Node("/", true, null, null);
    }

    public Node getRoot() {
        return this.root;
    }

    /*
        method to get node from DFS tree
        for given path object
     */
    public Node getNode(Path path) {

        if(path == null) {
            System.out.println("Path passed is null");
            return null;
        }

        if(path.isRoot()) {
            return this.root;
        }

        if(path.toString().startsWith(root.name)) {
            ArrayList<String> dirsList = path.getPathComponents();
            Node currentDir = root;
            int count = 0;
            for(String d : dirsList) {
                for(Node child : currentDir.children) {
                    if(d.equals(child.name)) {
                        currentDir = child;
                        count++;
                        break;
                    }
                }
            }

            if(count == dirsList.size()) {
                return  currentDir;
            } else {
                return  null;
            }
        } else {
            return null;
        }
    }

    /*
        method to insert node into DFS tree
        for given path with required settings as per parameters
     */
    public boolean insertNode(Path path, boolean isDirectory, Command commandStub, Storage storageStub) {
    	boolean inserted = false;
    	if(path == null) {
    		throw new NullPointerException("Null path sent");
    	}

    	if(path.toString().startsWith(root.name)) {
    		Path parent = path.parent();
    		ArrayList<String> dirsList = null;
    		if(parent.equals("")) {
    			String fileName = path.last();
    			Node child = new Node(fileName, isDirectory, commandStub, storageStub);
    			if(root.commandStubs.size() == 0) {
    				root.commandStubs.add(commandStub);
    				root.storageStub = storageStub;
    			}
    			root.children.add(child);
    		} else {
    			dirsList = parent.getPathComponents();
    			Node currentDir = root;
    			boolean dirFound = false;
    			for(String d : dirsList) {
    				for(Node child : currentDir.children) {
    					if(d.equals(child.name)) {
                            currentDir = child;
                            dirFound = true;
                            break;
                        }
    				}
    				if(!dirFound) {
    					Node dir = new Node(d, true, commandStub, storageStub);
    					currentDir.children.add(dir);
    					if(currentDir == root) {
    						if(root.commandStubs.size() == 0) {
    		    				root.commandStubs.add(commandStub);
    		    				root.storageStub = storageStub;
    		    			}
    					}
    					currentDir = dir;
    				} else {
    					currentDir.commandStubs.add(commandStub);
    				}
    			}
    			String fileName = path.last();
    			Node child = new Node(fileName, isDirectory, commandStub, storageStub);
    			currentDir.children.add(child);
    			inserted = true;
    		}
    	} 
    	return inserted;
    }
}
