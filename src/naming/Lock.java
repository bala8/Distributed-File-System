package naming;
import java.util.LinkedList;
import java.util.Queue;

public class Lock {

    boolean isExclusive = false;
    Queue<Lock> queue = new LinkedList<>();
    Queue<Boolean> lockType = new LinkedList<>();
    int numOfReaders = 0;
    int numOfReadRequests = 0;

    /**
     * One can acquire a shared lock on this node if no one is currently holding an exclusive lock
     * on it and if no one is waiting for an exclusive lock on it.
     * @return
     */
    public synchronized boolean acquireSharedLock()
    {
        while(isExclusive || queue.size() != 0)
        {
            queue.add(this);
            lockType.add(false);
            try {
                this.wait();
            } catch (InterruptedException e) {
                return false;
                //e.printStackTrace();
            }
        }
        numOfReaders++;
        numOfReadRequests++;
        return true;
    }

    /**
     * If this is the last reader on the node, it should notify the head of the queue (which has to be an exclusive lock)
     * @return
     */
    public synchronized boolean releaseSharedLock()
    {
        if(numOfReaders > 0) {
        	numOfReaders--;
        }
        if(!queue.isEmpty() && numOfReaders == 0)
        {
            Lock headLock = queue.remove();
            lockType.remove();
            headLock.notify();
        }
        return true;
    }

    /**
     * if the object is already locked for exclusive purpose or if there are readers on it, client should wait
     * @return
     */
    public synchronized boolean acquireExclusiveLock()
    {
        while(isExclusive || numOfReaders > 0)
        {
            queue.add(this);
            lockType.add(true);
            try {
                this.wait();
            } catch (InterruptedException e) {
                return false;
            }
            
        }
        isExclusive = true;
        return true;
    }

    /**
     *
     * @return
     */
    public synchronized boolean releaseExclusiveLock()
    {
    	if(!isExclusive) {
    		return true;
    	}
    	isExclusive = false;
        if(!queue.isEmpty())
        {
            Lock headLock;
            boolean lock_type = lockType.peek();
            if(lock_type == true)
            {
                headLock = queue.remove();
                lockType.remove();
                headLock.notify();
            }
            else {
                while (lockType.size() > 0 && lockType.peek() == false)
                {
                    headLock = queue.remove();
                    lockType.remove();
                    headLock.notify();
                }
            }
        }
        
        return true;
    }

}
