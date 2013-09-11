package org.sg.ehcache.pounder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.search.Attribute;

import org.ho.yaml.Yaml;


public class EhcachePounderV2 {

	private final int batchCount;
	private final int maxValueSize;
	private final int minValueSize;
	private final int hotSetPercentage;
	private final int rounds;
	private final int updatePercentage;
	private final int searchPercentage;
	private static final Random random = new Random();

	private volatile boolean isWarmup = true;
	private volatile AtomicLong maxBatchTimeMillis = new AtomicLong();

	private CacheManager cacheManager;

	private Ehcache cache;
	private final long entryCount;
	private final int threadCount;
	private final Results results;
	private final AtomicLong maxGetTime = new AtomicLong(0);
	private final PrintWriter csvOut;

	
	public EhcachePounderV2(int threadCount,
			long entryCount, int batchCount, int maxValueSize, int minValueSize,
			int hotSetPercentage, int rounds, int updatePercentage, 
			String ehcacheFileURL, String ehcacheFileCacheName,
			int searchPercentage
			) throws InterruptedException, IOException {
		
		this.entryCount = entryCount;
		this.threadCount = threadCount;
		this.batchCount = batchCount;
		this.maxValueSize = maxValueSize;
		this.minValueSize = minValueSize;
		this.hotSetPercentage = hotSetPercentage;
		this.rounds = rounds;
		this.updatePercentage = updatePercentage;
		this.searchPercentage = searchPercentage;
				
		// If a ehcache file was provided, create the cache using the parameters from the file. 
		// If not, create your own cache ...
		initializeCacheByFile(ehcacheFileURL, ehcacheFileCacheName);
		
		this.results = new Results(String.valueOf(System.currentTimeMillis()));
		this.csvOut = new PrintWriter(new FileWriter("results.csv"));
	}

	/**
	 * Kicks off the run of the test for this node
	 * 
	 * NOTE: It will wait for nodeCount nodes before actually running
	 * 
	 * @throws InterruptedException
	 */
	public void start() throws InterruptedException {

		System.out.println("Starting Pounder with " + threadCount + " threads,"
						+ " inserting: " + entryCount + " objects, each with a max Length of: "
						+ maxValueSize);
		
		for (int i = 0; i < rounds; i++) {
			final long totalTime = performCacheOperationsInThreads(i, isWarmup);
			final int tps = (int) (entryCount / (totalTime / 1000d));
			outputRoundData(i, cache.getSize(), totalTime, tps);
			results.addRound(totalTime, cache.getSize(), tps);
			if (isWarmup) {
				this.maxBatchTimeMillis.set(0);
			}
			isWarmup = false;
		}
		results.setMaxGetTime(maxGetTime.get());
		results.printResults(System.out);
		cacheManager.shutdown();
	}

	private void outputRoundData(int round, int cacheSize,
			final long totalTime, final int tps) {
		System.out.println(System.currentTimeMillis() + " ROUND " + round
				+ " size: " + cacheSize);
		System.out.println(System.currentTimeMillis() + " Took: " + totalTime
				+ " final size was " + cacheSize + " TPS: " + tps);
		// csvOut.println(round + "," + cacheSize + ',' + totalTime + ',' +
		// tps);
		// csvOut.flush();
	}

	private long performCacheOperationsInThreads(final int round,
			final boolean warmup) throws InterruptedException {
		long t1 = System.currentTimeMillis();

		Thread[] threads = new Thread[threadCount];
		for (int i = 0; i < threads.length; i++) {

			final int current = i;
			threads[i] = new Thread() {

				public void run() {
					try {
						executeLoad(round, warmup, (entryCount / threadCount)
								* current, (entryCount / threadCount)
								* (current + 1));
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			};
			threads[i].start();
		}
		for (int i = 0; i < threads.length; i++) {
			threads[i].join();
		}
		long totalTime = (System.currentTimeMillis() - t1);
		return totalTime;
	}

	private void executeLoad(int round, final boolean warmup, final long start,
			final long entryCount) throws InterruptedException {

		int readCount = 0;
		int writeCount = 0;
		int searchCount = 0;
		long t = System.currentTimeMillis();
		int currentSize = 1;
		List<String> ids = getListOfIDs();
		
		SampleObject so = new SampleObject(minValueSize, maxValueSize);

		for (long i = start; i < entryCount; i++) {

			if ((i + 1) % batchCount == 0) {

				long batchTimeMillis = (System.currentTimeMillis() - t);
				synchronized (maxBatchTimeMillis) {
					maxBatchTimeMillis
							.set(batchTimeMillis > maxBatchTimeMillis.get() ? batchTimeMillis
									: maxBatchTimeMillis.get());
				}
				currentSize = cache.getSize();

				outputBatchData(round, System.currentTimeMillis(), warmup,
						so.getContent().length, readCount, writeCount, searchCount, currentSize,
						batchTimeMillis);
				t = System.currentTimeMillis();
				readCount = 0;
				writeCount = 0;
				searchCount = 0;

			}
			
			if (!warmup && isSearch()) {
				Attribute<String> ID = cache.getSearchAttribute("ID");
				
				net.sf.ehcache.search.Results results = cache.createQuery().includeKeys().addCriteria(ID.eq(getRandomID(null))).execute();
				searchCount++;
			}
			
			if (warmup || isWrite()) {
				so = new SampleObject(minValueSize, maxValueSize);
				cache.put(new Element(so.getID(), so));
				writeCount++;
			}
			if (!isWrite() && !warmup) {
				long getTime = 0;
				if (readCount % threadCount == 0) {
					getTime = System.currentTimeMillis();
				}
				readEntry(getRandomID(ids));
				if (getTime > 0) {
					long ct = System.currentTimeMillis() - getTime;
					if (maxGetTime.get() < ct) {
						maxGetTime.set(ct);
					}
				}
				readCount++;
			}
		}

	}
	
	@SuppressWarnings("unchecked")
	private List<String> getListOfIDs() {
		
		return cache.getKeys();
	}
	
	private String getRandomID(List<String> ids) {
		
		if (ids == null) {
			ids = getListOfIDs();
		}

		Random rand = new Random();
		int choice = rand.nextInt(ids.size());
		
		//System.out.println("IDs Size " + ids.size() + " Choice: " + choice);
		
		if (!ids.contains(choice)) {
			choice = 1;
		}
		
		return ids.get(choice);
		
	}
	
	

	private void outputBatchData(int round, long timeMillis,
			final boolean warmup, int entrySize, int readCount, int writeCount, int searchCount,
			int currentSize, long batchTimeMillis) {
		System.out.println("*** " + timeMillis  
				+ " Cache size: " + (currentSize)
				+ " - Batch time: " + batchTimeMillis
				+ "; Object size:" + entrySize 
				+ "; READ Count: " + readCount
				+ " - WRITE Count: " + writeCount 
				+ " - SEARCH Count: " + searchCount
				+ " ***"
				);
		csvOut.println(round + "," + timeMillis + "," + currentSize + ","
				+ batchTimeMillis + "," + warmup + "," + entrySize + ","
				+ readCount + "," + writeCount + "," + hotSetPercentage);
		csvOut.flush();

	}

	private boolean isWrite() {

		return random.nextInt(100) < updatePercentage;
	}
	
	private boolean isSearch() {

		return random.nextInt(100) < searchPercentage;
	}

	private void readEntry(Object key) {

		Element e = cache.get(key);
/*		if (e == null) {
			return;
		}
		SampleObject so = (SampleObject) e.getObjectValue();
		byte[] value = so.getContent();

		if (!validateValue(value))
			throw new RuntimeException("Invalid Value:");*/
	}

	

	/**
	 * 
	 * @param bytes
	 *            - make sure the checksum is still legit when the entry is
	 *            retrieved
	 * @return boolean as to whether the entry is valid
	 */
	private boolean validateValue(byte[] bytes) {
		for (byte i = 0; i < 5; i++) {
			if (i != bytes[i]) {
				System.out.println(System.currentTimeMillis()
						+ " First Expected: " + i + " got: " + bytes[i]);
				return false;
			}
		}

		for (byte i = 1; i < 5; i++) {
			if (i != bytes[bytes.length - i]) {
				System.out.println(System.currentTimeMillis()
						+ " Last Expected: " + i + " got: "
						+ bytes[bytes.length - i]);
				return false;
			}
		}
		return true;
	}

	
	private void initializeCacheByFile(String ehcacheFileURL, String ehcacheFileCacheName) {

		// Create a new cachemanager from the ehcache.xml file provided. 
		this.cacheManager = new CacheManager(ehcacheFileURL);
		System.out.println("Printing Ehchache configuration:");
		
		this.cache = this.cacheManager.getCache(ehcacheFileCacheName);
		System.out.println(cacheManager.getActiveConfigurationText(ehcacheFileCacheName));
	}
	
	

	@SuppressWarnings("unchecked")
	public static final void main(String[] args) throws Exception {
		Map<String, Object> config = (Map<String, Object>) Yaml.load(new FileReader("config.yml"));

		System.out.println(" Printing Pounder YAML config values:");
		for (String k : config.keySet()) {
			System.out.println(k + ": " + config.get(k));
		}

		Integer entryCount = (Integer) config.get("entryCount");
		int threadCount = (Integer) config.get("threadCount");
		int batchCount = (Integer) config.get("batchCount");
		int maxValueSize = (Integer) config.get("maxValueSize");
		int minValueSize = (Integer) config.get("minValueSize");
		int hotSetPercentage = (Integer) config.get("hotSetPercentage");
		int rounds = (Integer) config.get("rounds");
		int updatePercentage = (Integer) config.get("updatePercentage");
		int searchPercentage = (Integer) config.get("searchPercentage");
		String ehcacheFileURL = (String) config.get("ehcacheFileURL");
		String ehcacheFileCacheName = (String) config.get("ehcacheFileCacheName");
		
		new EhcachePounderV2(threadCount, entryCount, batchCount, maxValueSize, minValueSize,
				hotSetPercentage, rounds, updatePercentage, ehcacheFileURL, ehcacheFileCacheName, searchPercentage)
				.start();
	}

	private static final class Results {
		private final List<Round> rounds = new LinkedList<Round>();
		private final String testID;
		private long maxGetTime;

		public Results(String TestID) {
			this.testID = TestID;
		}

		public void addRound(final long elapsedTime, final int finalSize,
				final int tps) {
			rounds.add(new Round(elapsedTime, finalSize, tps));
		}

		public void setMaxGetTime(long maxGetTime) {
			this.maxGetTime = maxGetTime;
		}

		public void printResults(final PrintStream out) {
			out.println("All Rounds:");
			float tpsSum = 0;
			long timeSum = 0;
			for (int i = 0; i < rounds.size(); i++) {
				final Round round = rounds.get(i);
				if (i > 0) {
					// exclude round 1 from the averages, since it's always an
					// outlier.
					tpsSum += round.getThroughputTPS();
					timeSum += round.getElapsedTimeMillis();
				}
				out.println("Round " + (i + 1) + ": elapsed time: "
						+ round.getElapsedTimeMillis() + ", final cache size: "
						+ round.getFinalCacheSize() + ", tps: "
						+ round.getThroughputTPS());
			}
			//out.println((StoreType.OFFHEAP.equals(storeType) ? "BigMemory"
			//		: storeType.toString()) + " Pounder Final Results");
			out.println("TOTAL TIME: " + timeSum
					+ "ms, AVG TPS (excluding round 1): " + tpsSum
					/ (rounds.size() - 1) + " MAX GET LATENCY: " + maxGetTime
					);
			
		}
	}

	private static final class Round {
		private final long elapsedTime;
		private final int finalSize;
		private final int tps;

		public Round(final long elapsedTime, final int finalSize, final int tps) {
			this.elapsedTime = elapsedTime;
			this.finalSize = finalSize;
			this.tps = tps;
		}

		public long getElapsedTimeMillis() {
			return elapsedTime;
		}

		public int getFinalCacheSize() {
			return finalSize;
		}

		public int getThroughputTPS() {
			return tps;
		}
	}
}
