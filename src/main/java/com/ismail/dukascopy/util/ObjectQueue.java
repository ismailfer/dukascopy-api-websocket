package com.ismail.dukascopy.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Object queue for jobs to be sent to websocket clients
 * 
 * this is essential so the dukas server does not wait for client slow
 * connection
 * 
 * @author ismail
 * @since 20220617
 * @param <T>
 */
public class ObjectQueue<T> {
    private static int sDefaultInitialSize = 1000;

    private BlockingQueue<T> mQueueSync = null;

    private ArrayList<T> mQueue = null;

    private Comparator<T> mSorter = null;

    /**
     * Indicates if it is asynchronous queue; or synchronous queue
     */
    private boolean mAsync = true;

    /**
     * default constructor
     */
    public ObjectQueue(boolean pAsync) {
        this(true, pAsync, sDefaultInitialSize);
    }

    /**
     * @param pCapacity
     */
    public ObjectQueue(boolean pLinkedList, boolean pAsync, int pCapacity) {
        mAsync = pAsync;

        if (mAsync) {
            if (pLinkedList)
                mQueueSync = new LinkedBlockingQueue<>();
            else
                mQueueSync = new ArrayBlockingQueue<>(pCapacity);
        } else {
            mQueue = new ArrayList<>(pCapacity);
        }

    }

    public void put(T pObject) {
        if (mAsync) {
            try {
                mQueueSync.put(pObject);
            } catch (InterruptedException ie) {

            }
        } else {
            mQueue.add(pObject);
        }
    }

    public T receive() throws InterruptedException {
        if (mAsync) {
            return mQueueSync.poll();
        } else {
            if (mQueue.size() > 0)
                return mQueue.remove(0);
            else
                return null;
        }
    }

    public T receiveWithWait(final long pMaxWait) throws InterruptedException {
        if (mAsync) {
            T st = null;

            if (pMaxWait > 0)
                st = mQueueSync.poll(pMaxWait, TimeUnit.MILLISECONDS);
            else
                st = mQueueSync.poll();

            return st;
        } else {
            if (mQueue.size() > 0)
                return mQueue.remove(0);
            else
                return null;
        }
    }

    public int Size() {
        if (mAsync) {
            return mQueueSync.size();
        } else {
            return mQueue.size();
        }
    }

    public boolean isEmpty() {
        if (mAsync) {
            return mQueueSync.isEmpty();
        } else {
            return mQueue.size() == 0;
        }
    }

    public void clear() {
        if (mAsync) {
            mQueueSync.clear();
        } else {
            mQueue.clear();
        }
    }

    @Override
    public String toString() {
        String v = null;

        if (mAsync) {

            if (mQueueSync != null) {
                v = "Async:" + mQueueSync.getClass().getSimpleName() + ":" + mQueueSync.size();
            }
        } else {
            if (mQueue != null) {
                v = "Sync:" + mQueue.getClass().getSimpleName() + ":" + mQueue.size();
            }
        }

        return v;
    }
}