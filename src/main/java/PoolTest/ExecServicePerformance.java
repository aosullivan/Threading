package PoolTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleFunction;

import com.google.common.base.Stopwatch;

public class ExecServicePerformance {

    private static final int repetitions = 120;
	private static int totalOperations = 250000;
    private static final int cpus = 8;
    private static final List<Batch> batches = batches(cpus);
    
    private static DoubleFunction<Double> performanceFunc = (double i) -> {return Math.sin(i * 100000 / Math.PI); };
    
    public static void main( String[] args ) throws InterruptedException {
    	
    	printExecutionTime("Synchronous", ExecServicePerformance::synchronous);
    	printExecutionTime("Synchronous batches", ExecServicePerformance::synchronousBatches);
    	printExecutionTime("Thread per batch", ExecServicePerformance::asynchronousBatches);
    	printExecutionTime("Executor pool", ExecServicePerformance::executorPool);
    	
    }
    
    private static void printExecutionTime(String msg, Runnable f) throws InterruptedException {
        long time = 0;
        for (int i = 0; i < repetitions; i++) {
        	Stopwatch stopwatch = Stopwatch.createStarted();
        	f.run(); //remember, this is a single-threaded synchronous execution since there is no explicit new thread
        	time += stopwatch.elapsed(TimeUnit.MILLISECONDS);
    	}
        System.out.println(msg + " exec time: " + time);
	}    

    private static void synchronous() {
    	for ( int i = 0; i < totalOperations; i++ ) {
    		performanceFunc.apply(i);
    	}
    }
    
    private static void synchronousBatches() {    	
    	for ( Batch batch : batches) {
    		batch.synchronously();
    	}
    }
    
    private static void asynchronousBatches() {
    	
    	CountDownLatch cb = new CountDownLatch(cpus);
    	
    	for ( Batch batch : batches) {
    		Runnable r = () ->  { batch.synchronously(); cb.countDown(); };
    		Thread t = new Thread(r);
    		t.start();
    	}
    	
    	try {
    		cb.await();
    	} catch (InterruptedException e) {
    		throw new RuntimeException(e);
    	}        
    }
    
    private static void executorPool() {
    	
    	final ExecutorService es = Executors.newFixedThreadPool(cpus);
    	
        for ( Batch batch : batches ) {
        	Runnable r = () ->  { batch.synchronously(); };
            es.submit(r);
        }

        es.shutdown();
        
    	try {
    		es.awaitTermination( 10, TimeUnit.SECONDS );
    	} catch (InterruptedException e) {
    		throw new RuntimeException(e);
    	} 

    }

    private static List<Batch> batches(final int cpus) {
    	List<Batch> list = new ArrayList<Batch>();
    	for ( int i = 0; i < cpus; i++ ) {
    		list.add( new Batch( totalOperations / cpus ) );
    	}
    	System.out.println("Batches: " + list.size());
    	return list;
    }
    
    private static class Batch {

        private final int operationsInBatch;

        public Batch( final int ops ) {
            this.operationsInBatch = ops;
        }

        public void synchronously() {
        	for ( int i = 0; i < operationsInBatch; i++ ) {
        		performanceFunc.apply(i);
        	}
        }
    }
    

}
