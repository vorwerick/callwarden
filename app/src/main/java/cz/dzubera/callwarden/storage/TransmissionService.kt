package cz.dzubera.callwarden.storage

import java.util.concurrent.locks.ReentrantReadWriteLock

class TransmissionService<T> {

    private val reentrantLock: ReentrantReadWriteLock = ReentrantReadWriteLock()
    private val readLock: ReentrantReadWriteLock.ReadLock = reentrantLock.readLock()
    private val writeLock: ReentrantReadWriteLock.WriteLock = reentrantLock.writeLock()

    private val pendingItems = mutableListOf<T>()
    private var transmissionQueueTask: ((T) -> Unit)? = null

    fun insertItem(item: T) {
        try {
            writeLock.lock()
            pendingItems.add(item)
            print("ITEMS IS: " + pendingItems.size)
        } finally {
            writeLock.unlock()
        }

    }


    fun getAndRemovePendingItems(): MutableList<T> {
        val pendingList = mutableListOf<T>()
        try {
            readLock.lock()
            println("pending items lasts: " + pendingItems.size)
            pendingList.addAll(pendingItems)
        } finally {
            readLock.unlock()
        }

        try {
            writeLock.lock()
            pendingItems.clear()
            println("pending items: " + pendingItems.size)
        } finally {
            writeLock.unlock()
        }
        println("wtf: " + pendingList.size)
        return pendingList
    }


    fun setTransmissionQueueTask(task: (T) -> Unit) {
        this.transmissionQueueTask = task
    }

}