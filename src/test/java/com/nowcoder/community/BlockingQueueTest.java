package com.nowcoder.community;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BlockingQueueTest {

    public static void main(String[] args) {
        BlockingQueue queue = new ArrayBlockingQueue(10); // 队列的容量10
        new Thread(new Producer(queue)).start(); // 实例化生产者线程,并启动
        new Thread(new Consumer(queue)).start(); // 实例化消费者线程,并启动
        new Thread(new Consumer(queue)).start();
        new Thread(new Consumer(queue)).start(); // 3个消费者同时消费
    }
}

// 继承Runnable接口来实现多线程
// 生产者
class  Producer implements  Runnable{

    // 把阻塞队列传进来
    private BlockingQueue<Integer> queue;

    public Producer(BlockingQueue<Integer> queue){
        this.queue = queue;
    }
    @Override
    public void run() {
            try {
                // 传数据
                for (int i=0; i < 100; i++){
                    Thread.sleep(20);
                    // 把i交给队列管理
                    queue.put(i);
                    System.out.println(Thread.currentThread().getName() + "生产:" + queue.size());
                }
            }catch (Exception e){
                e.printStackTrace();
            }
    }
}

// 消费者
class  Consumer implements  Runnable{

    // 把阻塞队列传进来
    private BlockingQueue<Integer> queue;

    public Consumer(BlockingQueue<Integer> queue){
        this.queue = queue;
    }
    @Override
    public void run() {
            try {
                // 取数据
                while(true){
                        Thread.sleep(new Random().nextInt(1000)); // 0-1000随机一个数
                        queue.take();
                    System.out.println(Thread.currentThread().getName() + "消费:" + queue.size());
                }
            }catch (Exception e){
                e.printStackTrace();
            }
    }
}